# Phase 1 submission deliverables

| Deliverable | Project artifact | Status on 19 Nov 2026 |
|---|---|---|
| Runnable, documented source code | Repository root, `android-app/`, `scripts/`, and `reports/` | Prepared for publication. Clean ignore rules exclude raw datasets, synthetic metrics, private Llama files, backups, and build output. Local Git repository initialized; the manual video can be added in a later commit. |
| Working application on chosen target device | `android-app/app/build/outputs/apk/debug/app-debug.apk` | **Verified.** Debug tests and build succeeded; APK installed on OPPO CPH2757 (Android 16). The app classified held-out `crazing_10.jpg` on-device in 904 ms. An operator-selected Medium visual tier appeared in history and remained after force-stop/relaunch. |
| Demo video: detection, alert, logging | `reports/metal-inspection-demo.mp4` | **Team-recorded manually.** Use only the held-out NEU-DET image. Show the predicted class, immediate checklist, operator severity choice clearly labeled as manual, and local history. Keep synthetic data out of the challenge video. |
| Technical report | `reports/phase1-technical-report.md` | **Prepared.** Includes exact NEU-DET held-out metrics, mobile architecture and checks, Llama timing, severity rubric, and limitations. |

## GitHub packaging

- Existing destination: private repository `AsmitaChoudhry10/Metal-Defect-Detector`.
- Do not commit raw NEU-DET/KolektorSDD2 data, generated synthetic imagery/metrics, personal phone screenshots, Llama PTE/tokenizer files, backups, local SDK files, or Android build outputs.
- The debug APK is about 198 MB. GitHub blocks files larger than 100 MiB in normal Git history; keep it out of commits. Build it from source or distribute it as a Release asset if the team needs a downloadable APK.
- NEU-DET's official page does not state a redistribution license. Link to the official source and keep the dataset local. KolektorSDD2 is CC BY-NC-SA 4.0 and remains local.

## Severity guidance

AI severity grading is incomplete because the approved NEU-DET dataset has no severity labels or calibrated physical measurements. The app now supports consistent human-recorded visual triage; its Low/Medium/High categories are not AI predictions or acceptance criteria. To train an AI severity model, obtain mentor-approved real target-part images with qualified severity labels and part-grouped train/validation/test splits. Do not tune severity using the synthetic dataset because the mentor disallowed synthetic data for this challenge.

