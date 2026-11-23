# Metal Inspection Assistant
**Bharat AI-SoC Challenge PS5: Real-Time Industrial Defect Detection & Quality-Control Assistant**

Edge-native Android application for industrial defect detection, automated severity metrology, and on-device operator advisory reasoning. Built for an Arm Cortex-A mobile platform, the application operates **100% offline in a strictly air-gapped environment** (declaring zero network permissions in `AndroidManifest.xml`).

The system integrates:
1. **Real-time OpenCV Vision & Severity Metrology Engine:** Runs on-device in under 10 ms, classifying steel surface defects across six standardized NEU-DET classes with **83.70% accuracy on held-out test data**, and automatically screening clean, non-defective specimens with **0.0% defect density**.
2. **On-Device Advisory Reasoning Copilot (Llama 3.2 1B on ExecuTorch):** Mandatory edge reasoning engine that ingests structured defect records and generates concrete, shop-floor Standard Operating Procedure (SOP) guidance for machine operators.
3. **Local SQLite Audit Ledger:** Logs all inspections, timestamps, severity tiers, and quality disposition statuses (`PASSED`, `FLAGGED`) for zero-cloud traceability.

---

## Challenge Requirements and Status Matrix

| PS5 Requirement | Implementation Status | Evidence and Measured Performance |
|---|---|---|
| **Detect defects & cracks** | **Verified** | Six NEU-DET classes; **83.70% accuracy and 0.837 macro F1** on untouched 270-image held-out test split. Clean specimen screening identifies defect-free parts at **0.0% defect density**. |
| **Classify defect type & severity** | **Verified** | Deterministic morphological surface density metrology ($\delta = \frac{A_{\text{defect}}}{A_{\text{ROI}}} \times 100\%$) mapped to calibrated tiers: Low (<2.5%), Medium (2.5%–8.0%), High ($\ge 8.0\%$), and Pass (<1.0%). Executed on-device in **~5 ms**. 1-tap quality disposition bar provides human-in-the-loop audit. |
| **Alert with actionable recommendations** | **Verified** | Immediate class-specific SOP action cues (quarantine Red Tag, descaler nozzle pressure checks, roll cooling sprays) + on-device **Llama 3.2 1B Instruct on ExecuTorch** generating structured clearance/quarantine advice. |
| **Log inspection results** | **Verified** | Local SQLite database on the phone stores record ID, defect label, severity, processing duration, and quality disposition. Real-time logging latency **< 1 ms**. |
| **Run at edge without cloud** | **Verified** | Complete vision pipeline and Llama reasoning run on-device on Arm Cortex-A. Manifest has **no `android.permission.INTERNET`**. Fully verified in Airplane Mode. |

---

## Evaluation & Benchmarks

### 1. NEU-DET Surface Defect Classifier
- **Dataset:** 1,800 grayscale 200×200 steel-surface images across six classes: `crazing`, `inclusion`, `patches`, `pitted_surface`, `rolled-in_scale`, and `scratches`.
- **Held-Out Test Set:** 270 images (45 per class, grouped split never seen during training).
- **Accuracy:** **83.70%** (226/270 correct).
- **Macro F1 Score:** **0.837** (Macro Precision: 0.842, Macro Recall: 0.837).
- **Inference Latency:** **2.1 ms median predict time** on Arm Cortex-A (OPPO CPH2757).

### 2. Automated Morphological Severity Metrology
- Evaluates local contrast against Gaussian neighborhoods to segment defect contours.
- Computes surface density index: $\delta = \left( \frac{A_{\text{defect}}}{A_{\text{ROI}}} \right) \times 100\%$.
- Tiers: Pass (<1.0%), Low (1.0%–2.5%), Medium (2.5%–8.0%), High ($\ge 8.0\%$).
- Verified on hardware: `crazing_10.jpg` yielded **30.2% defect density (HIGH)**; clean hex nut yielded **0.0% defect density (PASS)**.

### 3. Non-Defective Metal Screening
- OpenCV evaluates boundary corner luminance (>235). Catalog parts on neutral backgrounds are automatically screened at 0.0% defect density.
- 1-tap **Quality Disposition Action Bar** allows instant operator confirmation (`[● DEFECT DETECTED]` or `[✔ PASS / NO DEFECT]`), dynamically applying Green Tag assembly clearance.

### 4. Zero Synthetic Data Policy
- Per mentor directives, all synthetic fitting pipelines and models are completely excluded from the competition codebase and APK.

---

## Build and Install

### Prerequisites
- Android Studio with bundled JDK (JDK 17/21).
- Android SDK platform tools with ADB.
- Windows PowerShell terminal.

### Compile from Source
```powershell
cd android-app
.\gradlew.bat --offline testDebugUnitTest assembleDebug
```

The APK is generated at:
`android-app/app/build/outputs/apk/debug/app-debug.apk` (~197.8 MB)

### Install to Target Device via ADB
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

To run on-device Llama 3.2 advisory inference, select the matching quantized `.pte` model file and `tokenizer.model` within Section 04 of the app.

---

## Four Phase 1 Submission Deliverables

1. **Source Code (Deliverable 1):**
   - GitHub Repository: [https://github.com/AsmitaChoudhry10/Metal-Defect-Detector.git](https://github.com/AsmitaChoudhry10/Metal-Defect-Detector.git)
   - Runnable, fully documented, clean `main` branch.
   - *Submission Note:* Change repository visibility to **"Public"** in GitHub repository settings before submitting the link.
2. **Working Application (Deliverable 2):**
   - Debug APK: `android-app/app/build/outputs/apk/debug/app-debug.apk`
   - Verified on target device: OPPO CPH2757 (MediaTek Dimensity 6300, 8-Core Arm Cortex-A76/A55, Android 16).
3. **Demo Video (Deliverable 3):**
   - 2-Minute Video Run Sheet & Script: [`reports/first-round-demo-run-sheet.md`](reports/first-round-demo-run-sheet.md).
   - Demonstrates air-gapped HUD, defective sample inspection (crazing), clean part screening (hex nut), on-device Llama 3.2 advisory, and SQLite audit log.
4. **Final Technical Report (Deliverable 4):**
   - Technical Report (Markdown): [`reports/phase1-technical-report.md`](reports/phase1-technical-report.md).
   - Printable Report (HTML / PDF export ready): [`reports/phase1-final-project-report.html`](reports/phase1-final-project-report.html).
