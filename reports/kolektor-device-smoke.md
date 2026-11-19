# KolektorSDD2 on-device smoke check

Date: 2026-11-04  
Device: OPPO CPH2757, Android 16  
Runtime: packaged ONNX model through OpenCV DNN

## Results

The Android instrumentation run completed with `OK (2 tests)`.

| Official held-out sample | Model defect score | Deployed threshold | Time | Expected / actual |
|---|---:|---:|---:|---|
| Normal (`20000`) | 0.000495 | 0.342953 | 234 ms (first run) | normal / normal |
| Defective (`20042`) | 0.999747 | 0.342953 | 79 ms (second run) | defect / defect |

The timings are two smoke-test calls (first and second invocation), not a latency benchmark. The samples verify that the Android model loads and that its score direction and threshold agree with the labels. They do not estimate on-device accuracy.

The separate OpenCV DNN evaluation on all 1,004 official KSDD2 test images reported 96.71% accuracy, 86.67% defect precision, 82.73% defect recall, F1 0.847, ROC-AUC 0.956, with confusion matrix `[[880,14],[19,91]]`. Its threshold was selected on the training-only validation split. See `kolektor-onnx-opencv-results.json` for details.

## Cleanup and limits

Temporary sample files on shared phone storage were removed after the test. The main app remains installed; its private Llama model/tokenizer and inspection history were retained. ADB confirmed about 78 GB free on `/data`.

KSDD2 is a single production-item domain and has no unrelated-image labels. These results do not validate plumbing fittings, NEU-DET images, or a general-purpose normal/defect rejector. The user-facing branch remains marked experimental and domain-limited.

## Cross-domain warning

With the KSDD2 threshold frozen, the model detected only 40 of 270 held-out NEU-DET defects (14.8% recall). This was a positive-only transfer check; NEU-DET has no normal examples, so specificity and reject behavior cannot be calculated. Full class breakdown is in `kolektor-on-neu-transfer.json`. Do not use this binary branch as a general steel defect gate.
