"""Evaluate the exported ONNX model using OpenCV DNN on the official test set."""
import json
import random
from pathlib import Path

import cv2
import numpy as np
from sklearn.metrics import accuracy_score, average_precision_score, confusion_matrix, precision_score, recall_score, roc_auc_score, f1_score


ROOT = Path(__file__).resolve().parents[1]
DATA_ROOT = ROOT / "data" / "raw" / "KolektorSDD2"
MODEL = ROOT / "android-app" / "app" / "src" / "main" / "assets" / "kolektor_binary_mobilenet.onnx"
SEED = 2026


def rows_for(split):
    rows = []
    for path in sorted((DATA_ROOT / split).glob("*.png")):
        if path.stem.endswith("_GT"):
            continue
        mask_path = path.with_name(path.stem + "_GT.png")
        if not mask_path.is_file():
            continue
        mask = cv2.imread(str(mask_path), cv2.IMREAD_GRAYSCALE)
        rows.append((path, int(mask is not None and cv2.countNonZero(mask) > 0)))
    return rows


def stratified_holdout(rows):
    rng = random.Random(SEED)
    groups = {0: [], 1: []}
    for row in rows:
        groups[row[1]].append(row)
    validation = []
    for label in (0, 1):
        group = groups[label]
        rng.shuffle(group)
        validation.extend(group[:max(1, round(len(group) * 0.2))])
    return validation


def score_rows(net, rows, name):
    labels, scores = [], []
    for i, (path, label) in enumerate(rows, 1):
        image = cv2.imread(str(path), cv2.IMREAD_COLOR)
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        rgb = cv2.cvtColor(gray, cv2.COLOR_GRAY2RGB)
        rgb = cv2.resize(rgb, (512, 192), interpolation=cv2.INTER_LINEAR)
        blob = cv2.dnn.blobFromImage(rgb, 1 / 255.0, (512, 192), mean=(0, 0, 0), swapRB=False, crop=False)
        net.setInput(blob)
        logit = float(net.forward().reshape(-1)[0])
        labels.append(label)
        scores.append(1.0 / (1.0 + np.exp(-logit)))
        if i % 100 == 0:
            print(f"{name}: processed {i}/{len(rows)}", flush=True)
    return np.asarray(labels, dtype=np.int32), np.asarray(scores, dtype=np.float64)


def choose_f2_threshold(labels, scores):
    candidates = np.unique(np.concatenate(([0.0, 0.5, 1.0], scores)))
    best = (-1.0, 0.5)
    for threshold in candidates:
        predicted = (scores >= threshold).astype(np.int32)
        precision = precision_score(labels, predicted, zero_division=0)
        recall = recall_score(labels, predicted, zero_division=0)
        f2 = 5 * precision * recall / (4 * precision + recall) if (4 * precision + recall) else 0.0
        if f2 > best[0]:
            best = (float(f2), float(threshold))
    return best[1]


def evaluate(labels, scores, threshold):
    y = np.asarray(labels, dtype=np.int32)
    probability = np.asarray(scores, dtype=np.float64)
    predicted = (probability >= threshold).astype(np.int32)
    return {
        "threshold": float(threshold),
        "accuracy": float(accuracy_score(y, predicted)),
        "precision_defect": float(precision_score(y, predicted, zero_division=0)),
        "recall_defect": float(recall_score(y, predicted, zero_division=0)),
        "f1_defect": float(f1_score(y, predicted, zero_division=0)),
        "roc_auc": float(roc_auc_score(y, probability)),
        "average_precision": float(average_precision_score(y, probability)),
        "confusion_matrix_labels": ["normal", "defective"],
        "confusion_matrix": confusion_matrix(y, predicted, labels=[0, 1]).tolist(),
    }


def main():
    cv2.setNumThreads(2)
    net = cv2.dnn.readNetFromONNX(str(MODEL))
    validation_rows = stratified_holdout(rows_for("train"))
    official_test_rows = rows_for("test")
    y_val, p_val = score_rows(net, validation_rows, "validation")
    threshold = choose_f2_threshold(y_val, p_val)
    y_test, p_test = score_rows(net, official_test_rows, "official test")
    report = {
        "runtime": f"OpenCV DNN {cv2.__version__} CPU",
        "validation_count": len(validation_rows),
        "official_test_count": len(official_test_rows),
        "threshold_selected_for_f2_on_training_validation_only": threshold,
        "validation_metrics": evaluate(y_val, p_val, threshold),
        "official_test_metrics": evaluate(y_test, p_test, threshold),
        "scope_warning": "KolektorSDD2 only; not general evidence for NEU-DET or plumbing fittings.",
    }
    out = ROOT / "reports" / "kolektor-onnx-opencv-results.json"
    out.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
