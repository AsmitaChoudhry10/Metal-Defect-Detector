# Metal Inspection Assistant

An offline Android application for inspecting metal parts, detecting surface defects, estimating defect severity, and guiding shop-floor operators with on-device Llama 3.2 1B advice via ExecuTorch.

Built for the **Bharat AI-SoC Challenge (PS5: Real-Time Industrial Defect Detection & Quality-Control Assistant)** and verified on an **OPPO CPH2757 (Android 16)** device with zero cloud or internet dependence (`android.permission.INTERNET` is not declared in `AndroidManifest.xml`).

---

## How It Works

Instead of feeding raw camera frames into a large language model, the app splits inspection into two lightweight stages:

1. **Vision and Surface Metrology (OpenCV on Arm CPU)**  
   Resizes the captured or imported photo to `200x200` grayscale, extracts HOG descriptors (`4,410` features), and classifies steel surface defects across six NEU-DET classes (`crazing`, `inclusion`, `patches`, `pitted_surface`, `rolled-in_scale`, `scratches`) using a linear SVM.  
   A deterministic OpenCV metrology pass measures defect area percentage (`Nominal < 1.0%`, `Low < 2.5%`, `Medium 2.5%–8.0%`, `High >= 8.0%`) and screens clean isolated 3D parts (washers, nuts, bolts) so part silhouettes, shadows, and normal threads do not trigger false alarms.

2. **On-Device Advisory Copilot and Logging (ExecuTorch + Llama 3.2 1B + SQLite)**  
   Passes the structured finding (defect class, severity tier, defect area percentage, and contour count) to a quantized **Llama 3.2 1B Instruct** `.pte` model running locally through **ExecuTorch** (`seqLen = 128`, `maxNewTokens = 50`, `temperature = 0.1`) to produce short, practical shop-floor SOP steps. Every inspection and optional inspector override is saved in a local SQLite database.

---

## Key Verified Results

- **NEU-DET Held-Out Test Split (`270` images, `45` per class):** `83.70%` accuracy (`226 / 270` correct), `0.838` macro precision, `0.837` macro recall, `0.837` macro F1.
- **On-Device Speed (OPPO CPH2757, Arm CPU):** `2 ms` median SVM prediction call (`1.433 s` total for all 270 images in `NeuClassifierDeviceTest`); `25–103 ms` typical end-to-end UI inspection (`788–952 ms` on large uncached photo decodes).
- **Clean vs. Defective Part Checks on Phone:**
  - Clean Metal Washer: `0.0%` defect density -> `Nominal Surface (No Flaws Detected)`
  - Clean Hex Nut: `0.8%` defect density -> `Nominal Surface (No Flaws Detected)`
  - Clean Nut & Bolt: `0.2%` defect density -> `Nominal Surface (No Flaws Detected)`
  - Corroded / Rusty Bolt & Nut: `25.0%` defect density (`36` contours) -> `Flagged: High Severity`

---

## Repository Structure

```text
metal-inspection-mvp/
├── android-app/                   # Kotlin + Jetpack Compose + OpenCV + ExecuTorch Android app
│   └── app/src/main/
│       ├── assets/                # neu_hog_svm.xml (trained linear HOG-SVM model)
│       ├── java/org/bharatai/ps5/metalinspection/
│       │   ├── MainActivity.kt            # Inspection workflow, camera/gallery, ExecuTorch Llama copilot
│       │   ├── AppScreens.kt              # Welcome, 5-step Tutorial, About, and History screens
│       │   ├── OpenCvDefectClassifier.kt  # HOG-SVM classification + morphological metrology & clean-part gate
│       │   ├── InspectionHistoryStore.kt  # Local SQLite inspection ledger
│       │   └── SeverityRubric.kt          # Visual triage cues and tier helpers
│       └── java/.../ui/theme/             # Brushed-metal & graphite Material 3 theme
├── reports/
│   ├── Final_Technical_Report.pdf         # Final Technical Report (PDF)
│   ├── Final_Technical_Report.docx        # Final Technical Report (Word)
│   ├── neu-opencv-svm-tuning.json         # Grid search & held-out test metrics
│   └── neu-device-evaluation.md           # On-device OPPO CPH2757 instrumentation log
└── scripts/                               # Python training, evaluation, and validation scripts
```

---

## Build and Run

### Prerequisites

- Android Studio (with bundled JDK 17/21) and Android SDK Platform Tools (`adb`).
- An Android device (or emulator) connected over ADB.

### Compile and Run Unit Tests

```powershell
cd android-app
.\gradlew.bat testDebugUnitTest assembleDebug
```

The debug APK is generated at:
`android-app/app/build/outputs/apk/debug/app-debug.apk`

### Install on Device

```powershell
adb install -r android-app\app\build\outputs\apk\debug\app-debug.apk
```

To enable on-device Llama 3.2 1B advisory generation, copy your quantized Llama 3.2 1B `.pte` file and `tokenizer.model` onto the phone and select them in the **AI Advisory Copilot** section of the inspection screen.

---

## Technical Documentation

- **Technical Report (PDF):** [reports/Final_Technical_Report.pdf](reports/Final_Technical_Report.pdf)
- **Technical Report (DOCX):** [reports/Final_Technical_Report.docx](reports/Final_Technical_Report.docx)
