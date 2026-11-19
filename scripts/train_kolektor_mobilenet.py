"""Image-level normal/defect experiment on the official KolektorSDD2 split.

Threshold selection uses a stratified holdout from train only. The official
test split is evaluated once after selection and must not be used for tuning.
"""
from __future__ import annotations

import json
import random
from pathlib import Path

import numpy as np
import torch
from PIL import Image
from torch import nn
from torch.utils.data import DataLoader, Dataset
from torchvision import models, transforms


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "raw" / "KolektorSDD2"
OUT = ROOT / "models" / "kolektor_mobilenet_v3_small.pth"
METRICS = ROOT / "reports" / "kolektor-mobilenet-results.json"
SEED = 2026
IMAGE_SIZE = (192, 512)  # preserve the elongated part view better than square crops
BATCH_SIZE = 8
MAX_EPOCHS = 18
PATIENCE = 4


def scan_split(name: str) -> list[tuple[Path, int]]:
    directory = DATA / name
    rows: list[tuple[Path, int]] = []
    skipped: list[str] = []
    for image in sorted(directory.glob("*.png")):
        if image.stem.endswith("_GT"):
            continue
        mask = image.with_name(image.stem + "_GT.png")
        if not mask.is_file():
            skipped.append(image.name)
            continue
        with Image.open(mask) as im:
            label = int(np.asarray(im.convert("L")).max() > 0)
        rows.append((image, label))
    if skipped:
        print(f"{name}: skipped {len(skipped)} images without masks: {skipped[:5]}")
    if not rows:
        raise RuntimeError(f"No labeled images found under {directory}")
    return rows


def stratified_holdout(rows: list[tuple[Path, int]], fraction: float = 0.2):
    rng = random.Random(SEED)
    groups = {0: [], 1: []}
    for row in rows:
        groups[row[1]].append(row)
    fit, validation = [], []
    for label in (0, 1):
        group = groups[label]
        rng.shuffle(group)
        count = max(1, round(len(group) * fraction))
        validation.extend(group[:count])
        fit.extend(group[count:])
    rng.shuffle(fit)
    rng.shuffle(validation)
    return fit, validation


class KSDD(Dataset):
    def __init__(self, rows, training: bool):
        self.rows = rows
        operations = [transforms.Grayscale(num_output_channels=3)]
        if training:
            operations.extend([
                transforms.RandomHorizontalFlip(),
                transforms.RandomVerticalFlip(),
                transforms.RandomAffine(degrees=0, translate=(0.025, 0.025)),
                transforms.ColorJitter(brightness=0.12, contrast=0.15),
            ])
        operations.extend([
            transforms.Resize(IMAGE_SIZE, antialias=True),
            transforms.ToTensor(),
            transforms.Normalize(mean=(0.485, 0.456, 0.406), std=(0.229, 0.224, 0.225)),
        ])
        self.transform = transforms.Compose(operations)

    def __len__(self):
        return len(self.rows)

    def __getitem__(self, index):
        path, label = self.rows[index]
        with Image.open(path) as image:
            tensor = self.transform(image.convert("RGB"))
        return tensor, torch.tensor(label, dtype=torch.float32)


def predict(model, loader, device, criterion=None):
    model.eval()
    scores, labels = [], []
    loss_sum = count = 0
    with torch.inference_mode():
        for images, target in loader:
            logits = model(images.to(device)).flatten()
            if criterion is not None:
                target_device = target.to(device)
                loss_sum += float(criterion(logits, target_device)) * len(target)
                count += len(target)
            scores.extend(torch.sigmoid(logits).cpu().tolist())
            labels.extend(target.tolist())
    mean_loss = loss_sum / max(count, 1) if criterion is not None else None
    return np.asarray(labels, dtype=np.int32), np.asarray(scores, dtype=np.float64), mean_loss


def choose_f2_threshold(labels, scores):
    candidates = np.unique(np.concatenate(([0.0, 0.5, 1.0], scores)))
    best = (-1.0, 0.5)
    for threshold in candidates:
        predicted = (scores >= threshold).astype(np.int32)
        tp = int(np.sum((labels == 1) & (predicted == 1)))
        fp = int(np.sum((labels == 0) & (predicted == 1)))
        fn = int(np.sum((labels == 1) & (predicted == 0)))
        precision = tp / max(tp + fp, 1)
        recall = tp / max(tp + fn, 1)
        f2 = (5 * precision * recall / (4 * precision + recall)) if (4 * precision + recall) else 0.0
        if f2 > best[0]:
            best = (float(f2), float(threshold))
    return best


def metrics(labels, scores, threshold):
    predicted = (scores >= threshold).astype(np.int32)
    tp = int(np.sum((labels == 1) & (predicted == 1)))
    fp = int(np.sum((labels == 0) & (predicted == 1)))
    fn = int(np.sum((labels == 1) & (predicted == 0)))
    tn = int(np.sum((labels == 0) & (predicted == 0)))
    precision = tp / max(tp + fp, 1)
    recall = tp / max(tp + fn, 1)
    order = np.argsort(scores)
    positive_ranks = np.flatnonzero(labels[order] == 1) + 1
    positives = int(np.sum(labels == 1))
    negatives = int(np.sum(labels == 0))
    roc_auc = ((positive_ranks.sum() - positives * (positives + 1) / 2) /
               max(positives * negatives, 1))
    descending = np.argsort(-scores)
    sorted_labels = labels[descending]
    cumulative_tp = np.cumsum(sorted_labels == 1)
    precision_at_each = cumulative_tp / np.arange(1, len(sorted_labels) + 1)
    average_precision = float(np.sum(precision_at_each * (sorted_labels == 1)) / max(positives, 1))
    return {
        "threshold": float(threshold),
        "accuracy": float((tp + tn) / max(len(labels), 1)),
        "precision_defect": float(precision),
        "recall_defect": float(recall),
        "f1_defect": float(2 * precision * recall / max(precision + recall, 1e-12)),
        "roc_auc": float(roc_auc),
        "average_precision": average_precision,
        "confusion_matrix_labels": ["normal", "defective"],
        "confusion_matrix": [[tn, fp], [fn, tp]],
    }


def main():
    random.seed(SEED)
    np.random.seed(SEED)
    torch.manual_seed(SEED)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(SEED)
        torch.backends.cudnn.benchmark = True
    if not DATA.is_dir():
        raise FileNotFoundError(f"Expected dataset folder {DATA}")

    full_train = scan_split("train")
    official_test = scan_split("test")
    train_rows, val_rows = stratified_holdout(full_train)
    train_loader = DataLoader(KSDD(train_rows, True), batch_size=BATCH_SIZE, shuffle=True,
                              num_workers=0, pin_memory=torch.cuda.is_available())
    val_loader = DataLoader(KSDD(val_rows, False), batch_size=BATCH_SIZE, shuffle=False, num_workers=0)
    test_loader = DataLoader(KSDD(official_test, False), batch_size=BATCH_SIZE, shuffle=False, num_workers=0)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    weights = models.MobileNet_V3_Small_Weights.DEFAULT
    model = models.mobilenet_v3_small(weights=weights)
    model.classifier[3] = nn.Linear(model.classifier[3].in_features, 1)
    model.to(device)

    positive = sum(label for _, label in train_rows)
    negative = len(train_rows) - positive
    criterion = nn.BCEWithLogitsLoss(pos_weight=torch.tensor([negative / max(positive, 1)], device=device))
    optimizer = torch.optim.AdamW(model.parameters(), lr=2e-4, weight_decay=1e-4)
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=MAX_EPOCHS)
    best_loss, best_state, stale = float("inf"), None, 0
    history = []

    for epoch in range(1, MAX_EPOCHS + 1):
        model.train()
        loss_sum = count = 0
        for images, target in train_loader:
            images = images.to(device, non_blocking=True)
            target = target.to(device, non_blocking=True)
            optimizer.zero_grad(set_to_none=True)
            loss = criterion(model(images).flatten(), target)
            loss.backward()
            optimizer.step()
            loss_sum += float(loss.detach()) * len(target)
            count += len(target)
        scheduler.step()

        y_val, p_val, validation_loss = predict(model, val_loader, device, criterion)
        threshold = choose_f2_threshold(y_val, p_val)[1]
        predicted = (p_val >= threshold).astype(np.int32)
        train_loss = float(loss_sum / max(count, 1))
        val_precision = float(np.sum((y_val == 1) & (predicted == 1)) / max(np.sum(predicted == 1), 1))
        val_recall = float(np.sum((y_val == 1) & (predicted == 1)) / max(np.sum(y_val == 1), 1))
        row = {
            "epoch": epoch,
            "train_loss": train_loss,
            "validation_loss": validation_loss,
            "validation_f2": float(5 * val_precision * val_recall / max(4 * val_precision + val_recall, 1e-12)),
            "validation_recall": val_recall,
            "validation_threshold_f2": threshold,
        }
        history.append(row)
        print(json.dumps(row), flush=True)
        if validation_loss < best_loss:
            best_loss, best_state, stale = validation_loss, {k: v.detach().cpu().clone() for k, v in model.state_dict().items()}, 0
        else:
            stale += 1
            if stale >= PATIENCE:
                break

    if best_state is None:
        raise RuntimeError("No model checkpoint was selected")
    model.load_state_dict(best_state)
    y_val, p_val, _ = predict(model, val_loader, device)
    threshold = choose_f2_threshold(y_val, p_val)[1]
    y_test, p_test, _ = predict(model, test_loader, device)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    METRICS.parent.mkdir(parents=True, exist_ok=True)
    torch.save({"state_dict": best_state, "input_size": list(IMAGE_SIZE), "labels": ["normal", "defective"]}, OUT)
    summary = {
        "model": "ImageNet-initialized MobileNetV3-Small; supervised image-level binary classification",
        "dataset": "KolektorSDD2 official train/test split; training split further divided 80/20 by image for threshold selection",
        "seed": SEED,
        "device": str(device),
        "input_size_height_width": list(IMAGE_SIZE),
        "counts": {
            "training_fit": {"total": len(train_rows), "defective": positive, "normal": negative},
            "validation": {"total": len(val_rows), "defective": sum(y for _, y in val_rows), "normal": sum(1-y for _, y in val_rows)},
            "official_test": {"total": len(official_test), "defective": sum(y for _, y in official_test), "normal": sum(1-y for _, y in official_test)},
        },
        "validation_threshold_selected_for_f2": threshold,
        "validation_metrics": metrics(y_val, p_val, threshold),
        "official_test_metrics_at_validation_threshold": metrics(y_test, p_test, threshold),
        "epoch_history": history,
        "caution": "Dataset-specific result only; images show one controlled production-item domain. Not validated for NEU-DET steel strips, plumbing fittings, or other parts. CC BY-NC-SA 4.0 dataset license.",
        "checkpoint": str(OUT),
    }
    METRICS.write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print(json.dumps({k: summary[k] for k in ("device", "counts", "validation_threshold_selected_for_f2", "official_test_metrics_at_validation_threshold", "checkpoint")}, indent=2))


if __name__ == "__main__":
    main()
