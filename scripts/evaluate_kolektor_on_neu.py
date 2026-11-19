"""Cross-domain smoke evaluation of the frozen KSDD2 binary model on NEU-DET test defects.

This is a positive-only transfer check: NEU-DET has no normal images, so only
defect recall at the already-deployed KSDD2 threshold is measured. Do not tune
the threshold with this dataset or interpret this as general anomaly accuracy.
"""
import csv
import json
from collections import defaultdict
from pathlib import Path

import cv2
import numpy as np


ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / "android-app" / "app" / "src" / "main" / "assets" / "kolektor_binary_mobilenet.onnx"
SPLIT = ROOT / "data" / "processed" / "splits" / "test.csv"
KSDD_REPORT = ROOT / "reports" / "kolektor-onnx-opencv-results.json"


def score(net, path):
    image = cv2.imread(str(path), cv2.IMREAD_COLOR)
    if image is None:
        raise RuntimeError(f"Could not read {path}")
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    rgb = cv2.cvtColor(gray, cv2.COLOR_GRAY2RGB)
    rgb = cv2.resize(rgb, (512, 192), interpolation=cv2.INTER_LINEAR)
    blob = cv2.dnn.blobFromImage(rgb, 1 / 255.0, (512, 192), mean=(0, 0, 0), swapRB=False, crop=False)
    net.setInput(blob)
    logit = float(net.forward().reshape(-1)[0])
    return float(1.0 / (1.0 + np.exp(-logit)))


def main():
    cv2.setNumThreads(2)
    threshold = json.loads(KSDD_REPORT.read_text(encoding="utf-8"))[
        "threshold_selected_for_f2_on_training_validation_only"
    ]
    with SPLIT.open(newline="", encoding="utf-8-sig") as f:
        rows = list(csv.DictReader(f))
    net = cv2.dnn.readNetFromONNX(str(MODEL))
    by_class = defaultdict(list)
    all_scores = []
    for row in rows:
        confidence = score(net, ROOT / row["image"])
        by_class[row["filename_class"]].append(confidence)
        all_scores.append(confidence)
    total = len(all_scores)
    detected = sum(p >= threshold for p in all_scores)
    report = {
        "evaluation": "frozen KSDD2 detector transferred to NEU-DET official project test split",
        "runtime": f"OpenCV DNN {cv2.__version__} CPU",
        "sample_count": total,
        "label_scope": "All NEU-DET test images are defective; no normal or unrelated examples are present.",
        "threshold_source": "KolektorSDD2 training-only validation split; unchanged for this evaluation.",
        "threshold": float(threshold),
        "defect_recall_at_frozen_threshold": float(detected / total),
        "detected_defects": int(detected),
        "missed_defects": int(total - detected),
        "score_min": float(min(all_scores)),
        "score_median": float(np.median(all_scores)),
        "score_max": float(max(all_scores)),
        "per_class": {
            label: {
                "count": len(scores),
                "detected": sum(p >= threshold for p in scores),
                "defect_recall": float(sum(p >= threshold for p in scores) / len(scores)),
                "median_score": float(np.median(scores)),
            }
            for label, scores in sorted(by_class.items())
        },
        "warning": "Positive-only cross-domain check; cannot measure specificity, normal rejection, unrelated-image rejection, or production performance.",
    }
    out = ROOT / "reports" / "kolektor-on-neu-transfer.json"
    out.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
