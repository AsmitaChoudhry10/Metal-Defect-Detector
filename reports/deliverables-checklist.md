# Phase 1 Submission Deliverables Checklist
**Bharat AI-SoC Challenge PS5: Real-Time Industrial Defect Detection & Quality-Control Assistant**

**Submission Deadline:** 25 November 2026, 11:59 PM IST  
**Official Submission Guidelines:**
1. Source Code: Public GitHub repository with runnable code and documentation.
2. Application: Functional edge application deployed on target hardware (Arm Android).
3. Demo Video: 2-minute video on YouTube/Google Drive (Public visibility, 1080p, 16:9, MP4).
4. Final Project Report: Comprehensive report (.pdf / .docx format) outlining methodology, results, hardware utilization, and optimization techniques.

---

## Deliverables Status (23 November 2026)

| # | Deliverable | Repository / Artifact Location | Verification Status & Next Human Action |
|---|---|---|---|
| **1** | **Source Code** | Git Root & `android-app/`<br>GitHub: `https://github.com/AsmitaChoudhry10/Metal-Defect-Detector.git` | **READY & COMMITTED (`main` branch clean).**<br>*Human Action:* In GitHub repo Settings &gt; General &gt; Danger Zone &gt; Change visibility to **"Public"** (per challenge instructions). Push commits: `git push origin main`. |
| **2** | **Application** | `android-app/app/build/outputs/apk/debug/app-debug.apk` (197.8 MB) | **VERIFIED ON REAL HARDWARE.**<br>Tested live on OPPO CPH2757 (Android 16, Dimensity 6300 8-Core). Verified vision predict (2 ms), clean specimen screening (0.0% defect density), 1-tap quality disposition, and SQLite persistence. |
| **3** | **Demo Video** | Script & Run Sheet:<br>`reports/first-round-demo-run-sheet.md` | **READY FOR RECORDING.**<br>Complete 2-minute script with exact timestamps, camera angles, narration, and test samples (`crazing_10.jpg` & clean hex nut) prepared for the team to record. |
| **4** | **Technical Report** | Markdown: `reports/phase1-technical-report.md`<br>Printable HTML: `reports/phase1-final-project-report.html` | **COMPLETED & READY FOR SUBMISSION.**<br>Detailed 10-section report covering 6-stage architecture, NEU-DET 83.70% benchmark, mathematical severity metrology, Llama 3.2 ExecuTorch optimization, and Arm hardware profiles. One-click export to PDF or Word. |

---

## GitHub Packaging & Clean Repository Verification

- **Repository Hygiene:** All private model binaries (`.pte`, `tokenizer.model`), raw image datasets, intermediate build directories, and synthetic experiments are ignored via `.gitignore`.
- **Zero Synthetic Data Compliance:** All synthetic pipelines and models remain strictly excluded from the codebase and APK per mentor directives.
- **Air-Gapped Security:** Manifest declares **no network permissions** (`INTERNET` permission absent), guaranteeing 100% on-device edge execution.
