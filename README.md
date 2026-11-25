# Metal Inspection Assistant

An offline Android app for inspecting metal parts, detecting surface defects, estimating severity, and giving shop-floor operators clear next-step advice using **Llama 3.2 1B on ExecuTorch**.

Built for the **Bharat AI-SoC Challenge (PS5: Real-Time Industrial Defect Detection & Quality-Control Assistant)**. Everything runs locally on the phone with zero internet or cloud access (`INTERNET` permission is not declared in `AndroidManifest.xml`).

---

## Features

- **Offline Defect Detection:** Classifies steel surface defects across six NEU-DET classes (`crazing`, `inclusion`, `patches`, `pitted_surface`, `rolled-in_scale`, `scratches`) using OpenCV HOG + linear SVM.
- **Clean-Part Screening & Severity Grading:** Measures surface defect area percentage (`Nominal < 1.0%`, `Low < 2.5%`, `Medium 2.5%–8.0%`, `High >= 8.0%`) and screens clean 3D parts (washers, nuts, bolts) so outer edges, shadows, and threads do not trigger false alarms.
- **On-Device Operator Advice:** Sends the structured defect summary (not raw pixels) to a quantized **Llama 3.2 1B Instruct** model via **ExecuTorch** to generate short, practical Standard Operating Procedure (SOP) steps.
- **Local Inspection Log:** Saves every scan, latency, severity tier, and optional inspector override to an on-device SQLite database.

---

## How It Works

1. **Acquire:** Capture a photo with the phone camera or import an image from storage.
2. **Pre-process & Classify:** Resize to `200x200` grayscale, extract `4,410` HOG features, and classify with the trained linear SVM (`neu_hog_svm.xml`).
3. **Measure Severity:** Run OpenCV morphological metrology to compute defect area density and filter out clean isolated parts.
4. **Advise & Log:** Display immediate SOP cues, optionally generate tailored advice with ExecuTorch Llama 3.2 1B, and log the record in SQLite.

---

## Verified Results (OPPO CPH2757, Android 16)

- **NEU-DET Held-Out Test Set (`270` images, `45` per class):** `83.70%` accuracy (`226 / 270`), `0.838` macro precision, `0.837` macro recall, `0.837` macro F1.
- **Speed on Phone:** `2 ms` median SVM prediction call (`1.433 s` for all `270` images in `NeuClassifierDeviceTest`); `25–103 ms` typical end-to-end UI inspection (`788–952 ms` on large uncached photo decodes).
- **Clean vs. Defective Part Checks:**
  - Clean Metal Washer: `0.0%` defect density -> `Nominal Surface (No Flaws Detected)`
  - Clean Hex Nut: `0.8%` defect density -> `Nominal Surface (No Flaws Detected)`
  - Clean Nut & Bolt: `0.2%` defect density -> `Nominal Surface (No Flaws Detected)`
  - Corroded / Rusty Bolt & Nut: `25.0%` defect density (`36` contours) -> `Flagged: High Severity`

---

## Repository Layout

```text
├── source-code/                   # Runnable Android app (Kotlin, Jetpack Compose, OpenCV, ExecuTorch)
│   ├── app/src/main/java/         # App screens, OpenCV classifier, SQLite store, theme
│   ├── app/src/main/assets/       # Trained neu_hog_svm.xml model
│   └── app/src/test/              # Unit tests
├── reports/
│   ├── Final_Technical_Report.pdf # Technical Report (PDF)
│   ├── Final_Technical_Report.docx# Technical Report (Word)
│   ├── neu-opencv-svm-tuning.json # Held-out NEU-DET test metrics
│   └── neu-device-evaluation.md   # OPPO CPH2757 instrumentation log
├── scripts/                       # Python training and evaluation scripts
└── data/                          # Train/validation/test split CSVs
```

---

## Build and Run

1. Open a terminal in `source-code/` (requires Android Studio JDK 17/21 and Android SDK):

   ```powershell
   cd source-code
   .\gradlew.bat testDebugUnitTest assembleDebug
   ```

2. Install the generated APK on a connected Android phone:

   ```powershell
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

3. *(Optional for Llama advisory)* Copy your quantized Llama 3.2 1B `.pte` model and `tokenizer.model` onto the phone and select them in the **AI Advisory Copilot** section of the inspection screen.

---

## Technical Report

- **PDF:** [reports/Final_Technical_Report.pdf](reports/Final_Technical_Report.pdf)
- **Word:** [reports/Final_Technical_Report.docx](reports/Final_Technical_Report.docx)
