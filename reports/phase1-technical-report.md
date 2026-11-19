# Metal Inspection Assistant — Phase 1 Technical Report

**Report date:** 19 November 2026  
**Target platform:** Android phone (OPPO CPH2757); no Raspberry Pi is required for this team's demo.  
**Status:** Mobile edge-AI research prototype; not a certified production inspector.

## 1. Summary

The Android app runs a classical OpenCV HOG/SVM image classifier locally, gives an immediate class-specific recheck checklist, and stores inspection metadata in SQLite on the phone. It has been installed on the OPPO and has no Internet permission. A separate optional Llama 3.2 1B Instruct model runs through ExecuTorch on the phone and receives the predicted label and checklist as text only. Its one verified response took 86.35 seconds, so the fixed checklist is the practical immediate operator guidance.

The primary model classifies six NEU-DET steel-surface defect labels. It always returns one of those labels: it cannot recognize a good part or reject an unrelated image. The app does not automatically grade severity or provide dependable defect localization. The operator can record a team-defined visual Low/Medium/High tier, which is clearly separate from AI output and physical acceptance criteria.

## 2. Requirement status

| Challenge requirement | Status | Evidence/remaining limit |
|---|---|---|
| Detect and classify surface defects | Partial | Six NEU-DET labels; 83.70% accuracy and 0.837 macro F1 on a 270-image held-out test set. No normal class, broad anomaly rejection, or validation on plumbing fittings. |
| Classify severity | Manual triage implemented; AI severity incomplete | Operator selects a team-defined visual tier after reviewing the image. NEU-DET provides neither severity labels nor physical measurements; no AI severity accuracy can be claimed. |
| Alert with actionable guidance | Fixed class-specific checklist implemented; Llama optional | Checklist appears immediately. One Llama response succeeded locally in 86.35 seconds; it is an experimental paraphrase, not image analysis or a real-time alert. |
| Log inspections | Implemented | Local SQLite history records predicted class, timestamp, elapsed analysis time, and optional operator-entered tier. Images are not retained. |
| Run at the edge without cloud | Implemented on the selected phone; sustained real-time performance unproven | Model inference is local and the app declares no Internet permission. A sustained FPS, power, and thermal benchmark has not been recorded. |
| Locate defects | Unavailable as a dependable app output | A non-integrated NEU box-localization baseline reached 54.6% recall at IoU 0.3 on 634 held-out boxes. It is not suitable for inspection decisions. |

## 3. Data and evaluation

### NEU-DET classifier

NEU-DET contains 1,800 grayscale 200×200 steel-surface images in six classes: crazing, inclusion, patches, pitted surface, rolled-in scale, and scratches. XML files provide defect boxes but do not label severity or normal/good parts. The model was selected using train/validation data and evaluated on a separate held-out test split.

| Test set | Correct | Accuracy | Macro F1 |
|---|---:|---:|---:|
| NEU-DET held-out images | 226/270 | 83.70% | 0.837 |

The same 270-image test was previously run on the OPPO with an instrumentation assertion; the full loop took 2.698 seconds. That is test-loop duration, not per-image latency. The current app produced an on-device result from the held-out `crazing_10.jpg` sample. A spot saved-image pipeline result recorded on 15 November took 751 ms; spot timings are not a sustained benchmark.

These metrics apply only to the NEU-DET split. The dataset has no normal samples and its images are flat steel crops, so it cannot establish performance on good parts, unrelated images, fittings, or real production components. The [official NEU-DET page](https://faculty.neu.edu.cn/songkc/en/zdylm/263270/list/index.htm) supplies the dataset and annotation information; it does not state a redistribution license. Obtain the dataset from its official source and cite its papers rather than redistributing it.

### Separate experiments (not merged into the primary claim)

- **KolektorSDD2:** a separate model reached 96.71% accuracy, 86.67% defect precision, 82.73% defect recall, F1 0.847, and ROC-AUC 0.956 on its 1,004-image held-out test set. At its frozen threshold it detected 40/270 (14.8%) NEU-DET defects, so it is not a general-purpose reject gate. Dataset licensing is CC BY-NC-SA 4.0; keep the images out of this repository.
- **Severstal:** the best segmentation experiment reached 0.117 mean defect IoU. A mask-area severity proxy reached 42.7% accuracy and 0.421 macro F1. Neither result supports an integrated feature.
- **AI-generated synthetic data:** excluded from the challenge demo and claims following mentor guidance. Historical synthetic files remain local and are ignored by Git.

## 4. Severity, recommendations, and safety

### Operator visual tier

After a class result, the app offers class-specific visual descriptions for Low, Medium, and High. The operator, not the model, chooses a tier. It is saved as `Operator visual tier: <tier> (team-defined rubric)` alongside the inspection record. The [rubric](severity-rubric.md) is only for consistent demo triage; it is not a physical measurement, engineering standard, or accept/reject criterion.

NEU-DET cannot support severity model training because it has no severity labels. A meaningful AI severity model needs permitted real images of the target product, reviewer-assigned labels using the applicable product specification, part-level grouped splits, and a held-out evaluation reporting per-tier precision/recall, confusion matrix, and ordinal performance. Until then, automatic severity remains incomplete.

### Operator guidance and Llama

The fixed per-class checklist is displayed immediately and recommends visual rechecks, such as retaking a focused image or having an inspector compare the finding with the applicable product specification. These are prompts, not factory standards or acceptance rules.

The optional Llama 3.2 1B Instruct component uses ExecuTorch locally. It receives only a predicted text label and the fixed checklist; it cannot inspect pixels, confirm a defect, identify a location, assign severity, or decide pass/fail. A prompt overflow was fixed and one generation completed with a 70-token prompt and 9 generated tokens in 86.35 seconds (73.538 seconds to first token; 12.812 seconds generation). Report this as a successful local generation, not real-time performance. If the class is wrong, Llama's advice may also be wrong.

No model output is a certified acceptance standard. A qualified inspector and the applicable drawing, gauge, and inspection procedure must decide the disposition of a real part.

## 5. Mobile implementation and verification

- **Vision:** OpenCV inference on the phone; the NEU HOG/SVM model is bundled in the app. An optional separate KolektorSDD2 MobileNet check is visibly labeled experimental and domain-limited.
- **Image input:** Android camera capture and saved-image selection are available.
- **Logging:** SQLite stores local inspection metadata; images are not stored.
- **Network:** Android manifest has no `INTERNET` permission; inference code uses local assets.
- **Phone:** Updated debug APK built, installed in place, and launched on OPPO CPH2757 (Android 16). A held-out `crazing_10.jpg` image returned `crazing` on-device in 904 ms, then a human-entered Medium visual tier appeared in local history. The same result/tier remained after force-stop and relaunch. This one image confirms the flow only; it does not validate the classifier or severity tier.
- **Build checks:** `testDebugUnitTest` and `assembleDebug` succeeded on 19 November; all three unit-test classes passed. The APK excludes the synthetic fitting model and an unused legacy Kolektor SVM asset.
- **Llama resources:** The `.pte` and tokenizer are user-provided phone files and are not included in the repository or APK. Model loading and one local generation were verified separately.

Airplane-mode inference, repeated camera-to-result timing, and sustained FPS/thermal/power behavior have not been measured on this exact build. The no-Internet-permission manifest and local-asset path support offline design, but should not be presented as a full stress benchmark.

## 6. Four submission deliverables

1. **Runnable documented source:** Android project, permitted training/evaluation scripts, reports, and root build instructions; committed locally. The existing private GitHub repository is identified, but the first push remains pending because the session's automatic approval gate blocked outbound publishing.
2. **Working mobile application:** debug APK builds and was installed on the OPPO. APK path: `android-app/app/build/outputs/apk/debug/app-debug.apk`.
3. **Demo video:** record the primary flow using a real NEU-DET sample; show the label, fixed checklist, optional operator tier, and local history. Do not use synthetic images or synthetic metrics. See [`first-round-demo-run-sheet.md`](first-round-demo-run-sheet.md).
4. **Technical report:** this document reports the dataset, held-out results, device evidence, architecture, limitations, and severity plan.

## 7. Reproduction

Open `android-app/` in Android Studio and build the debug variant, or run from PowerShell:

```powershell
cd android-app
.\gradlew.bat --offline testDebugUnitTest assembleDebug
```

The generated APK is at `android-app/app/build/outputs/apk/debug/app-debug.apk`. The environment-specific Android SDK path is not committed. Raw datasets, generated experiments, private Llama files, backups, training checkpoints, and build outputs are excluded by the root `.gitignore`.

