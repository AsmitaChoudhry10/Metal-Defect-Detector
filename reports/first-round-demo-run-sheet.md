# 2-Minute Demo Video Script & Run Sheet (Phase 1 Submission)

**Project:** Bharat AI-SoC Challenge PS5 — Real-Time Industrial Defect Detection & Quality-Control Assistant  
**Target Video Specs (Per Guidelines):** Exactly 2 Minutes, 1080p (1920×1080), 16:9 Aspect Ratio, MP4 format, Public Visibility on YouTube/Google Drive.  
**Hardware Setup:** Arm-based Android Phone (OPPO CPH2757) on desktop tripod or clean workbench. Turn on **Airplane Mode** to visually emphasize 100% offline air-gapped operation.

---

## Shot List & 2-Minute Timeline

### Scene 1: Introduction & Air-Gapped Edge Architecture (0:00 – 0:25)
- **Visual:** Frame phone screen showing the industrial dark HUD. Point camera at status pills: `SYSTEM OPERATIONAL · AIR-GAPPED` and `ARM CORTEX-A · NPU ACCELERATED`.
- **Narration (Spoken by presenter):**
  > *"Hello, this is our submission for Problem Statement 5 of the Bharat AI-SoC Challenge: Real-Time Industrial Defect Detection and Quality-Control Assistant. Our system runs entirely at the edge on an Arm-powered smartphone in a strictly air-gapped environment with zero cloud connectivity. It pairs real-time OpenCV vision metrology with on-device Llama 3.2 advisory reasoning via ExecuTorch."*

### Scene 2: Real Defective Specimen & Automated Severity Metrology (0:25 – 0:55)
- **Visual:** Import or capture real defective steel sample (`crazing_10.jpg` from the NEU-DET held-out split). Tap **ANALYZE SPECIMEN**.
- **On Screen:** Shows `CRAZING` detected in **~2 ms**, `SEVERITY: HIGH` badge, Defect Density at **30.2%**, and the Red Tag quarantine SOP (`QUARANTINE COIL IMMEDIATELY`).
- **Narration:**
  > *"Here we inspect a real hot-rolled steel surface sample from our held-out NEU-DET test set, where our vision model achieved 83.70% accuracy. Within 2 milliseconds, our deterministic morphological metrology measures surface defect density at 30.2%, classifying it as High Severity. The operator is immediately alerted with actionable SOP steps to quarantine the coil with a Red Tag and inspect the roll cooling sprays."*

### Scene 3: Clean Specimen Screening & 1-Tap Quality Disposition (0:55 – 1:25)
- **Visual:** Load the clean, non-defective metal component (machined hex nut). Tap **ANALYZE SPECIMEN**.
- **On Screen:** Shows `Clean Specimen (Pass)` with `0.0%` defect density and Neon Emerald Green header: `STATUS: PASS · Clear for Production`. Point camera to the 1-tap Quality Disposition Action Bar `[✔ PASS / NO DEFECT]` and Part Clearance SOP (Green Tag).
- **Narration:**
  > *"Next, we inspect a clean, non-defective machined metal component. The edge pre-processor analyzes boundary luminance and surface density, correctly screening it at 0.0% defect density. The app confirms Green Tag clearance for production release. The operator can also use our 1-tap Quality Disposition bar to audit and confirm the pass decision."*

### Scene 4: On-Device Llama 3.2 1B Advisory via ExecuTorch (1:25 – 1:50)
- **Visual:** Scroll to Section 04: AI Advisory Copilot. Tap **GENERATE LLAMA ADVISORY**.
- **On Screen:** Displays ExecuTorch inference running on Arm CPU, outputting structured actionable steps: *"The operator should: 1. Affix the Green Tag to the component. 2. Release the component to production assembly. 3. Log batch clearance."*
- **Narration:**
  > *"For reasoning, the challenge-mandated Llama 3.2 model is deployed on-device using the reference ExecuTorch runtime on Arm CPU. Rather than analyzing raw pixels, Llama reasons over the structured defect record to generate concise, shop-floor action steps, advising the operator on exact lot clearance or quarantine protocols."*

### Scene 5: Local Audit Ledger & Conclusion (1:50 – 2:00)
- **Visual:** Scroll to Section 03: Local Audit Log. Show the permanent SQLite entries with timestamp, defect type, severity badges, and `PASSED` / `FLAGGED` status.
- **Narration:**
  > *"Every inspection event is permanently logged in a local SQLite ledger for zero-cloud traceability. Source code, APK, and technical report are available in our public repository. Thank you."*

---

## Pre-Recording Checklist
1. APK installed on phone: `app-debug.apk` (located at `android-app/app/build/outputs/apk/debug/app-debug.apk`).
2. Airplane Mode enabled (no Wi-Fi, no Mobile Data, no Bluetooth).
3. Sample images ready in phone gallery:
   - Defective sample: `neu_crazing_heldout.jpg` or `crazing_10.jpg`.
   - Clean sample: `clean_nut.jpg` (clean machined hex nut).
4. Audio recording clear, video framed horizontally at 16:9 / 1080p.
