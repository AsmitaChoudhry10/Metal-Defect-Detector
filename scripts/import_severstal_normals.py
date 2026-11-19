"""Copy defect-free images from the Kaggle Severstal training set.

After downloading and extracting the official competition data into
data/raw/Severstal/, run: python scripts/import_severstal_normals.py
This does not redistribute data or alter the source archive.
"""
from __future__ import annotations
import csv
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data" / "raw" / "Severstal"
CSV = DATA / "train.csv"
IMAGES = DATA / "train_images"
OUT = ROOT / "data" / "processed" / "normal_steel"

def main() -> None:
    if not CSV.is_file() or not IMAGES.is_dir():
        raise SystemExit(f"Expected train.csv and train_images under {DATA}")
    masks: dict[str, list[str]] = {}
    with CSV.open(newline="", encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            masks.setdefault(row["ImageId"], []).append((row.get("EncodedPixels") or "").strip())
    normal = sorted(name for name, values in masks.items() if values and all(not v for v in values))
    OUT.mkdir(parents=True, exist_ok=True)
    copied = []
    for name in normal:
        src = IMAGES / name
        if src.is_file():
            shutil.copy2(src, OUT / name)
            copied.append(name)
    manifest = OUT / "manifest.csv"
    with manifest.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["image", "label", "source"])
        writer.writerows([[name, "normal", "Severstal competition train set"] for name in copied])
    print(f"Copied {len(copied)} verified no-defect images to {OUT}")
    print("Check competition terms before using or sharing these images.")

if __name__ == "__main__":
    main()
