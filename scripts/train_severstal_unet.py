from __future__ import annotations

import argparse
import csv
import json
import random
from pathlib import Path

import numpy as np
from PIL import Image
import torch
from torch import nn
from torch.utils.data import DataLoader, Dataset


CLASSES = [1, 2, 3, 4]
CLASS_NAMES = ["defect_1", "defect_2", "defect_3", "defect_4"]
HEIGHT, WIDTH = 128, 512


def decode_rle(encoded: str, height: int, width: int) -> np.ndarray:
    mask = np.zeros(height * width, dtype=np.uint8)
    if encoded:
        values = np.asarray([int(v) for v in encoded.split()], dtype=np.int64)
        starts = values[0::2] - 1
        lengths = values[1::2]
        for start, length in zip(starts, lengths):
            mask[start : start + length] = 1
    return mask.reshape((width, height)).T


class SteelDataset(Dataset):
    def __init__(self, root: Path, names: list[str], annotations: dict[str, list[tuple[int, str]]], augment: bool = False):
        self.root, self.names, self.annotations, self.augment = root, names, annotations, augment

    def __len__(self):
        return len(self.names)

    def __getitem__(self, index):
        name = self.names[index]
        with Image.open(self.root / "train_images" / name) as source:
            image = np.asarray(source.convert("L").resize((WIDTH, HEIGHT), Image.Resampling.BILINEAR), dtype=np.uint8)
        mask = np.zeros((256, 1600), dtype=np.uint8)
        for class_id, rle in self.annotations.get(name, []):
            region = decode_rle(rle, 256, 1600)
            mask[region > 0] = class_id
        mask = np.asarray(Image.fromarray(mask).resize((WIDTH, HEIGHT), Image.Resampling.NEAREST), dtype=np.uint8)
        if self.augment and random.random() < 0.5:
            image, mask = np.fliplr(image).copy(), np.fliplr(mask).copy()
        if self.augment and random.random() < 0.5:
            image, mask = np.flipud(image).copy(), np.flipud(mask).copy()
        return torch.from_numpy(image.astype(np.float32)[None] / 255.0), torch.from_numpy(mask.astype(np.int64))


class Block(nn.Module):
    def __init__(self, a: int, b: int):
        super().__init__()
        self.net = nn.Sequential(
            nn.Conv2d(a, b, 3, padding=1, bias=False), nn.BatchNorm2d(b), nn.ReLU(inplace=True),
            nn.Conv2d(b, b, 3, padding=1, bias=False), nn.BatchNorm2d(b), nn.ReLU(inplace=True),
        )

    def forward(self, x):
        return self.net(x)


class SmallUNet(nn.Module):
    def __init__(self):
        super().__init__()
        self.e1, self.e2, self.e3 = Block(1, 8), Block(8, 16), Block(16, 32)
        self.pool = nn.MaxPool2d(2)
        self.mid = Block(32, 64)
        self.u3, self.d3 = nn.ConvTranspose2d(64, 32, 2, 2), Block(64, 32)
        self.u2, self.d2 = nn.ConvTranspose2d(32, 16, 2, 2), Block(32, 16)
        self.u1, self.d1 = nn.ConvTranspose2d(16, 8, 2, 2), Block(16, 8)
        self.head = nn.Conv2d(8, 5, 1)

    def forward(self, x):
        a = self.e1(x)
        b = self.e2(self.pool(a))
        c = self.e3(self.pool(b))
        z = self.mid(self.pool(c))
        z = self.d3(torch.cat([self.u3(z), c], 1))
        z = self.d2(torch.cat([self.u2(z), b], 1))
        z = self.d1(torch.cat([self.u1(z), a], 1))
        return self.head(z)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--epochs", type=int, default=8)
    parser.add_argument("--batch", type=int, default=8)
    parser.add_argument("--limit", type=int, default=0, help="Optional image cap for quick smoke runs")
    args = parser.parse_args()
    random.seed(2026)
    np.random.seed(2026)
    torch.manual_seed(2026)
    torch.cuda.manual_seed_all(2026)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    csv_path = args.data / "train.csv"
    annotations: dict[str, list[tuple[int, str]]] = {}
    with csv_path.open(newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            if row["EncodedPixels"].strip():
                annotations.setdefault(row["ImageId"], []).append((int(row["ClassId"]), row["EncodedPixels"]))
    names = sorted(p.name for p in (args.data / "train_images").glob("*.jpg"))
    if args.limit:
        names = names[: args.limit]
    random.shuffle(names)
    val_count = max(1, int(round(len(names) * 0.2)))
    val_names, train_names = names[:val_count], names[val_count:]
    train_ds = SteelDataset(args.data, train_names, annotations, augment=True)
    val_ds = SteelDataset(args.data, val_names, annotations)
    train_loader = DataLoader(train_ds, batch_size=args.batch, shuffle=True, num_workers=0, pin_memory=device.type == "cuda")
    val_loader = DataLoader(val_ds, batch_size=args.batch, shuffle=False, num_workers=0, pin_memory=device.type == "cuda")
    model = SmallUNet().to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=0.001, weight_decay=1e-4)
    # Stronger positive-pixel weights help the thin/rare mask classes survive downsampling.
    weights = torch.tensor([0.15, 6.0, 12.0, 1.5, 5.0], device=device)
    best_score = -1.0
    history = []
    args.out.mkdir(parents=True, exist_ok=True)
    for epoch in range(args.epochs):
        model.train()
        losses = []
        for images, masks in train_loader:
            images, masks = images.to(device, non_blocking=True), masks.to(device, non_blocking=True)
            optimizer.zero_grad(set_to_none=True)
            logits = model(images)
            probabilities = torch.softmax(logits, dim=1)
            one_hot = nn.functional.one_hot(masks, num_classes=5).permute(0, 3, 1, 2).float()
            intersection = (probabilities * one_hot).sum(dim=(0, 2, 3))
            denominator = probabilities.sum(dim=(0, 2, 3)) + one_hot.sum(dim=(0, 2, 3))
            dice_loss = 1.0 - ((2.0 * intersection[1:] + 1.0) / (denominator[1:] + 1.0)).mean()
            loss = nn.functional.cross_entropy(logits, masks, weight=weights) + dice_loss
            loss.backward()
            optimizer.step()
            losses.append(float(loss.detach().cpu()))
        model.eval()
        intersections = np.zeros(5, np.int64)
        unions = np.zeros(5, np.int64)
        with torch.no_grad():
            for images, masks in val_loader:
                logits = model(images.to(device, non_blocking=True))
                pred = logits.argmax(1).cpu().numpy()
                truth = masks.numpy()
                for c in range(1, 5):
                    intersections[c] += np.logical_and(pred == c, truth == c).sum()
                    unions[c] += np.logical_or(pred == c, truth == c).sum()
        ious = [float(intersections[c] / max(1, unions[c])) for c in range(1, 5)]
        score = float(np.mean(ious))
        history.append({"epoch": epoch + 1, "train_loss": float(np.mean(losses)), "class_iou": ious, "class_dice": [float(2*intersections[c]/max(1, intersections[c]+unions[c])) for c in range(1,5)], "macro_defect_iou": score})
        print(json.dumps(history[-1]), flush=True)
        if score > best_score:
            best_score = score
            torch.save({"model": model.state_dict(), "classes": CLASS_NAMES, "input_hw": [HEIGHT, WIDTH], "seed": 2026}, args.out / "severstal_unet_best.pt")
    result = {
        "dataset": "Severstal Steel Defect Detection train split",
        "architecture": "SmallUNet base channels 8, grayscale 128x512, 5 classes including background, CE plus Dice loss",
        "device": str(device),
        "gpu": torch.cuda.get_device_name(0) if device.type == "cuda" else None,
        "train_images": len(train_ds), "validation_images": len(val_ds), "seed": 2026,
        "history": history, "best_macro_defect_iou": best_score,
        "limitations": "Downsampled exploratory baseline; proxy severity is mask coverage only; no official public test labels used.",
    }
    (args.out / "severstal_unet_metrics.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(json.dumps({"best_macro_defect_iou": best_score, "metrics": str(args.out / 'severstal_unet_metrics.json')}, indent=2), flush=True)


if __name__ == "__main__":
    main()
