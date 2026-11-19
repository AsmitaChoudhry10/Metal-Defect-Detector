"""Select an OpenCV-compatible NEU HOG/SVM using validation only, then evaluate once on test."""
from pathlib import Path
import csv
import json

import cv2
import numpy as np
from sklearn.metrics import accuracy_score, f1_score, classification_report, confusion_matrix


ROOT = Path(__file__).resolve().parents[1]
SPLITS = ROOT / "data" / "processed" / "splits"
CLASSES = ["crazing", "inclusion", "patches", "pitted_surface", "rolled-in_scale", "scratches"]
HOG = cv2.HOGDescriptor((200, 200), (40, 40), (20, 20), (20, 20), 9)


def load(split):
    features, labels = [], []
    with (SPLITS / f"{split}.csv").open(encoding="utf-8-sig", newline="") as f:
        for row in csv.DictReader(f):
            image = cv2.imread(str(ROOT / row["image"]), cv2.IMREAD_GRAYSCALE)
            if image is None:
                raise RuntimeError(f"Could not read {row['image']}")
            # Match OpenCvDefectClassifier.kt and the original Android training path.
            image = cv2.resize(image, (200, 200), interpolation=cv2.INTER_LINEAR)
            features.append(HOG.compute(image).reshape(-1))
            labels.append(CLASSES.index(row["filename_class"]))
    return np.asarray(features, np.float32), np.asarray(labels, np.int32)


def fit_predict(train_x, train_y, predict_x, kernel, c, gamma=None):
    model = cv2.ml.SVM_create()
    model.setType(cv2.ml.SVM_C_SVC)
    model.setKernel(kernel)
    model.setC(float(c))
    if gamma is not None:
        model.setGamma(float(gamma))
    if not model.train(train_x, cv2.ml.ROW_SAMPLE, train_y):
        raise RuntimeError("OpenCV SVM training failed")
    _, result = model.predict(predict_x)
    return model, result.reshape(-1).astype(np.int32)


def main():
    train_x, train_y = load("train")
    val_x, val_y = load("validation")
    test_x, test_y = load("test")

    candidates = [(cv2.ml.SVM_LINEAR, c, None) for c in (0.01, 0.1, 1, 10, 100)]
    candidates += [(cv2.ml.SVM_RBF, c, g) for c in (1, 10, 100) for g in (0.0001, 0.0005, 0.001)]
    trials = []
    for kernel, c, gamma in candidates:
        _, val_pred = fit_predict(train_x, train_y, val_x, kernel, c, gamma)
        trials.append({
            "kernel": "linear" if kernel == cv2.ml.SVM_LINEAR else "rbf",
            "C": c,
            "gamma": gamma,
            "validation_accuracy": float(accuracy_score(val_y, val_pred)),
            "validation_macro_f1": float(f1_score(val_y, val_pred, average="macro")),
        })
        print(trials[-1], flush=True)

    best = max(trials, key=lambda r: (r["validation_macro_f1"], r["validation_accuracy"]))
    kernel = cv2.ml.SVM_LINEAR if best["kernel"] == "linear" else cv2.ml.SVM_RBF
    # The fixed validation set chooses the hyperparameters; retrain on train+validation.
    fit_x = np.concatenate([train_x, val_x])
    fit_y = np.concatenate([train_y, val_y])
    final_model, test_pred = fit_predict(fit_x, fit_y, test_x, kernel, best["C"], best["gamma"])
    previous = json.loads((ROOT / "reports" / "opencv-mobile-model.json").read_text(encoding="utf-8"))
    report = {
        "model": "OpenCV HOG + tuned C-SVC",
        "selection": "Highest macro-F1 on fixed validation split; test split used once after selection.",
        "preprocessing": "OpenCV grayscale resize to 200x200 with INTER_LINEAR, matching Android Imgproc.resize default",
        "train_count": len(train_y),
        "validation_count": len(val_y),
        "final_fit_count": len(fit_y),
        "test_count": len(test_y),
        "validation_trials": trials,
        "selected": best,
        "test_accuracy": float(accuracy_score(test_y, test_pred)),
        "test_macro_f1": float(f1_score(test_y, test_pred, average="macro")),
        "previous_installed_model": {
            "test_accuracy": previous["test_accuracy"],
            "test_macro_f1": previous["test_report"]["macro avg"]["f1-score"],
        },
        "test_per_class": classification_report(test_y, test_pred, labels=list(range(6)), target_names=CLASSES, output_dict=True, zero_division=0),
        "test_confusion_matrix": confusion_matrix(test_y, test_pred, labels=list(range(6))).tolist(),
        "warning": "Closed-set NEU-DET defect classes only; no normal or unrelated examples. Not validated on plumbing photos.",
    }
    out = ROOT / "reports" / "neu-opencv-svm-tuning.json"
    out.write_text(json.dumps(report, indent=2), encoding="utf-8")
    candidate_model = ROOT / "models" / "neu_hog_svm_tuned.xml"
    final_model.save(str(candidate_model))
    print(json.dumps({"selected": best, "test_accuracy": report["test_accuracy"], "test_macro_f1": report["test_macro_f1"], "candidate_model": str(candidate_model)}, indent=2))


if __name__ == "__main__":
    main()
