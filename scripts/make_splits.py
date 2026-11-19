"""Create repeatable train/validation/test manifests for NEU-DET.

This script does not move or alter the original images or XML annotations.
Run it from any folder with: python scripts/make_splits.py
"""

from __future__ import annotations

import argparse
import csv
import random
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path


CLASSES = (
    "crazing",
    "inclusion",
    "patches",
    "pitted_surface",
    "rolled-in_scale",
    "scratches",
)
SPLIT_RATIOS = {"train": 0.70, "validation": 0.15}


def find_dataset_root(project_root: Path) -> Path:
    """Find the nested folder that contains both official dataset folders."""
    raw_root = project_root / "data" / "raw" / "NEU-DET"
    matches = [
        folder
        for folder in raw_root.rglob("*")
        if folder.is_dir()
        and (folder / "IMAGES").is_dir()
        and (folder / "ANNOTATIONS").is_dir()
    ]
    if len(matches) != 1:
        raise FileNotFoundError(
            f"Expected one NEU-DET folder containing IMAGES and ANNOTATIONS; found {len(matches)} under {raw_root}"
        )
    return matches[0]


def annotation_labels(xml_path: Path) -> list[str]:
    root = ET.parse(xml_path).getroot()
    labels = [node.findtext("name", default="").strip() for node in root.findall("object")]
    labels = [label for label in labels if label]
    unknown = sorted(set(labels) - set(CLASSES))
    if unknown:
        raise ValueError(f"Unknown labels in {xml_path.name}: {unknown}")
    if not labels:
        raise ValueError(f"No labeled objects in {xml_path.name}")
    return labels


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--seed", type=int, default=2026, help="Fixed shuffle seed (default: 2026).")
    args = parser.parse_args()

    project_root = Path(__file__).resolve().parents[1]
    dataset_root = find_dataset_root(project_root)
    images_dir = dataset_root / "IMAGES"
    annotations_dir = dataset_root / "ANNOTATIONS"
    images = sorted(path for path in images_dir.iterdir() if path.suffix.lower() in {".jpg", ".jpeg", ".png"})
    if not images:
        raise FileNotFoundError(f"No images found in {images_dir}")

    groups: dict[str, list[tuple[Path, Path, list[str]]]] = defaultdict(list)
    for image in images:
        annotation = annotations_dir / f"{image.stem}.xml"
        if not annotation.is_file():
            raise FileNotFoundError(f"Missing annotation for {image.name}: {annotation}")
        primary_label = image.stem.rsplit("_", 1)[0]
        if primary_label not in CLASSES:
            raise ValueError(f"Unexpected image class prefix in {image.name}: {primary_label}")
        groups[primary_label].append((image, annotation, annotation_labels(annotation)))

    rng = random.Random(args.seed)
    rows: dict[str, list[dict[str, str]]] = {"train": [], "validation": [], "test": []}
    for primary_label in CLASSES:
        samples = groups[primary_label]
        if not samples:
            raise ValueError(f"No images found for class {primary_label}")
        rng.shuffle(samples)
        train_end = round(len(samples) * SPLIT_RATIOS["train"])
        validation_end = train_end + round(len(samples) * SPLIT_RATIOS["validation"])
        partitions = {
            "train": samples[:train_end],
            "validation": samples[train_end:validation_end],
            "test": samples[validation_end:],
        }
        for split_name, split_samples in partitions.items():
            for image, annotation, labels in split_samples:
                rows[split_name].append(
                    {
                        "image": image.relative_to(project_root).as_posix(),
                        "annotation": annotation.relative_to(project_root).as_posix(),
                        "filename_class": primary_label,
                        "annotation_labels": ";".join(sorted(set(labels))),
                    }
                )

    output_dir = project_root / "data" / "processed" / "splits"
    output_dir.mkdir(parents=True, exist_ok=True)
    for split_name, split_rows in rows.items():
        output_path = output_dir / f"{split_name}.csv"
        with output_path.open("w", newline="", encoding="utf-8") as stream:
            writer = csv.DictWriter(stream, fieldnames=list(split_rows[0]))
            writer.writeheader()
            writer.writerows(sorted(split_rows, key=lambda row: row["image"]))

    print(f"Dataset: {dataset_root.relative_to(project_root).as_posix()}")
    print(f"Seed: {args.seed}")
    print("Split counts by filename class:")
    for split_name, split_rows in rows.items():
        counts = Counter(row["filename_class"] for row in split_rows)
        print(f"  {split_name:10} {len(split_rows):4}  " + ", ".join(f"{name}={counts[name]}" for name in CLASSES))
    print(f"Manifests saved to: {output_dir.relative_to(project_root).as_posix()}")


if __name__ == "__main__":
    main()
