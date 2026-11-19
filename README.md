# Metal Inspection Assistant

Android edge-AI prototype for classifying six steel-surface defect types from the NEU-DET dataset. The app runs the vision model on an Android phone, shows a class-specific operator checklist, and records the class, time, elapsed analysis duration, and any operator-entered visual severity tier in local SQLite history. It has no network permission and does not upload images or inspection records.

This is a research demonstration, not a certified quality-control instrument. Its classifier is closed-set: it always selects one of six defects and cannot identify a good part or reliably reject an unrelated image. It does not produce validated severity or defect-location predictions. A person may record a team-defined visual tier; it is not an AI result or product acceptance decision.

## Challenge requirements and status

| Requirement | Current status | Evidence and limits |
|---|---|---|
| Detect/classify defects | Partial prototype | Six NEU-DET classes; 83.70% accuracy and 0.837 macro F1 on the untouched 270-image test split. No normal class or general out-of-domain rejection. NEU-DET is flat steel surface imagery, not plumbing fittings. |
| Classify severity | Manual visual triage only | The operator may record Low/Medium/High with class-specific visual cues. No AI severity model is available. NEU-DET has no severity labels or physical measurements. |
| Alert with actionable guidance | Implemented as fixed checklist; Llama optional/experimental | Class-specific recheck steps appear immediately. One local Llama generation completed in 86.35 seconds, so it is not a real-time alert. Llama receives text labels/checklist only; it never examines pixels. Neither output is an acceptance standard. |
| Log inspections | Implemented | SQLite history on the phone stores predicted label, timestamp, processing time, and optional operator tier. Images are not stored. |
| Run at the edge/offline | Implemented for tested phone flow | OpenCV inference and Llama run locally. The manifest has no `INTERNET` permission. A sustained FPS/power benchmark is not available; Llama is slow. |
| Defect localization | Not integrated/reliable | The NEU bounding-box experiment reached 54.6% recall at IoU 0.3 on 634 held-out boxes. Do not use it as a dependable location output. |

## Evaluation

### NEU-DET

- Dataset: 1,800 200×200 grayscale steel-surface images, six defect classes, with XML bounding boxes.
- Held-out test: 270 images, grouped split, 226 correct.
- Accuracy: **83.70%**; macro F1: **0.837**.
- These values describe only the NEU-DET test split and do not establish performance on normal parts, unrelated images, plumbing fittings, or physical production parts.
- The test loop was previously reproduced on the OPPO CPH2757 by Android instrumentation: 226/270 correct; 2.698 seconds for the full loop. This is not per-image latency.

The official dataset source is [NEU-DET](https://faculty.neu.edu.cn/songkc/en/zdylm/263270/list/index.htm). The page provides images and annotations but does not state a redistribution license. Do not redistribute the raw dataset; obtain it from the source and cite the associated papers.

### Other experiments

- KolektorSDD2's separate binary model scored 96.71% accuracy, 86.67% defect precision, 82.73% recall, F1 0.847, ROC-AUC 0.956 on its 1,004-image held-out test set. This is a different product domain. At its frozen threshold it detected only 40/270 (14.8%) NEU-DET defects, so it is not a general normal/defect gate.
- Severstal segmentation and mask-area severity experiments were weak (best mean defect IoU 0.117; severity proxy accuracy 42.7%) and are not integrated.
- Synthetic imagery and its metrics are excluded from the challenge demo and claims, per mentor instruction.

Details and source evaluation reports are under [`reports/`](reports/). The severity policy is in [`reports/severity-rubric.md`](reports/severity-rubric.md).

## Build and install

Requirements: Android Studio with its bundled JDK, Android SDK/platform tools, and a Windows PowerShell terminal. Open [`android-app/`](android-app/) in Android Studio and build the `debug` variant, or run:

```powershell
cd android-app
.\gradlew.bat --offline testDebugUnitTest assembleDebug
```

The APK is generated at `android-app/app/build/outputs/apk/debug/app-debug.apk`. To install to a USB-debugging-authorized phone:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

The build packages the NEU classifier and the separate optional KolektorSDD2 model. The synthetic fitting flow and unused legacy Kolektor SVM asset are excluded from the APK. The optional Llama panel requires the matching `.pte` model and `tokenizer.model` to be selected on the device; those large licensed/runtime files are not included in this source tree.

## Four submission deliverables

1. **Runnable documented source:** this repository, Android project, scripts, evaluation reports, and build instructions.
2. **Working mobile application:** debug APK builds and has been installed on the OPPO CPH2757. A held-out `crazing_10.jpg` sample returned `crazing` in 904 ms; the operator-entered Medium tier appeared in history and persisted after app restart. This single flow check does not validate severity or broader model performance.
3. **Demo video:** use a real NEU-DET sample and show analysis, fixed checklist, optional human-entered severity, and saved history. Never use synthetic data in the challenge video. See [`reports/first-round-demo-run-sheet.md`](reports/first-round-demo-run-sheet.md).
4. **Technical report:** [`reports/phase1-technical-report.md`](reports/phase1-technical-report.md).

## Reproducibility and dataset handling

Raw datasets, generated synthetic data, private Llama files, local environments, training checkpoints, backups, Android build products, and machine-specific SDK files are excluded by [`.gitignore`](.gitignore). They are preserved on the development machine and are not required for reviewers to clone the source. Follow the scripts and official dataset source links to prepare data locally. Do not commit third-party datasets without permission to redistribute them.

## Safety and limitations

- Model prediction is a class label, not proof that a defect exists.
- The app has no good/normal class in its primary NEU model and no calibrated reject threshold.
- The severity selector records an operator's visual opinion; it does not infer severity or measure crack/pit depth, strength, or fit.
- The localization experiment is not part of the phone result.
- Llama can only rephrase the fixed checklist from text; its only measured response took 86.35 seconds.
- A qualified inspector and the applicable product drawing, gauge, and acceptance procedure must make any real-part decision.

