# Metal Inspection Assistant

An offline Android app for inspecting metal parts, detecting surface defects, estimating severity, and giving shop-floor operators clear next-step advice using **Llama 3.2 1B on ExecuTorch**.

Built for the **Bharat AI-SoC Challenge (PS5: Real-Time Industrial Defect Detection & Quality-Control Assistant)**. Everything runs locally on the phone with zero internet or cloud access (`INTERNET` permission is not declared in `AndroidManifest.xml`).

---

## Quick File Links

Open any key file directly without clicking through nested folders:

- **Main App & ExecuTorch Llama Integration:** [MainActivity.kt](source-code/app/src/main/java/org/bharatai/ps5/metalinspection/MainActivity.kt)
- **Welcome, Tutorial, About & History UI:** [AppScreens.kt](source-code/app/src/main/java/org/bharatai/ps5/metalinspection/AppScreens.kt)
- **OpenCV Defect Classifier & Clean-Part Metrology:** [OpenCvDefectClassifier.kt](source-code/app/src/main/java/org/bharatai/ps5/metalinspection/OpenCvDefectClassifier.kt)
- **Local SQLite Inspection Log:** [InspectionHistoryStore.kt](source-code/app/src/main/java/org/bharatai/ps5/metalinspection/InspectionHistoryStore.kt)
- **Visual Triage & Severity Rubric:** [SeverityRubric.kt](source-code/app/src/main/java/org/bharatai/ps5/metalinspection/SeverityRubric.kt)
- **SVM Training Script:** [tune_neu_opencv_svm.py](scripts/tune_neu_opencv_svm.py)
- **Technical Report (PDF):** [Final_Technical_Report.pdf](reports/Final_Technical_Report.pdf)
- **Technical Report (Word):** [Final_Technical_Report.docx](reports/Final_Technical_Report.docx)

---

## Features

- **Offline Defect Detection:** Classifies steel surface defects across six NEU-DET classes (`crazing`, `inclusion`, `patches`, `pitted_surface`, `rolled-in_scale`, `scratches`) using OpenCV HOG + linear SVM.
- **Clean-Part Screening & Severity Grading:** Measures surface defect area percentage (`Nominal < 1.0%`, `Low < 2.5%`, `Medium 2.5%–8.0%`, `High >= 8.0%`) and screens clean 3D parts (washers, nuts, bolts) so outer edges, shadows, and threads do not trigger false alarms.
- **On-Device Operator Advice:** Sends the structured defect summary (not raw pixels) to a quantized **Llama 3.2 1B Instruct** model via **ExecuTorch** to generate short, practical Standard Operating Procedure (SOP) steps.
- **Local Inspection Log:** Saves every scan, latency, severity tier, and optional inspector override to an on-device SQLite database.

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
