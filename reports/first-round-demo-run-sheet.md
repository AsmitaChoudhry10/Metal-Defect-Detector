# First-round demo run sheet — real NEU-DET flow

Use the real-data Android flow only. The mentor ruled synthetic data out for this challenge. The primary classifier is a closed-set model for six NEU-DET steel-surface classes; it cannot say “good,” reject unrelated inputs, estimate severity, or reliably locate a defect.

## Before recording

- Install `android-app/app/build/outputs/apk/debug/app-debug.apk` on the OPPO CPH2757.
- Keep the phone offline for the inference portion. The app has no Internet permission and uses local model assets.
- Use the held-out grayscale sample `neu_crazing_heldout.jpg` in the phone's `MetalInspectionDemo` folder or another real NEU-DET image. Identify it as a dataset sample, not a plumbing fitting or production part.
- Turn on Do Not Disturb if appropriate, close personal apps, and frame the phone so notifications or private information are not visible.
- Keep synthetic fitting images, synthetic model outputs, and generated-data metrics out of the video and narration.

## Walkthrough

1. Introduce this as a mobile on-device research prototype for six NEU-DET defect classes.
2. Select the grayscale NEU-DET sample and show its image preview.
3. Tap **Analyze image**. Show the class label and state that the score applies only to the held-out NEU-DET evaluation (83.70% accuracy; 0.837 macro F1 on 270 images).
4. Show the fixed class-specific recheck checklist. Explain it is immediate guidance, not an acceptance standard.
5. If you demonstrate severity, choose a tier only after looking at the image. Say clearly: “This Low/Medium/High value is entered by the operator using our team-defined visual rubric; the model does not predict severity, and this is not a physical measurement or acceptance limit.”
6. Show **Recent inspections** and the saved class, time, duration, and operator tier. State that the app stores inspection metadata locally and does not retain the image.
7. Mention optional Llama only if useful: ExecuTorch generation ran locally, but one short response took 86.35 seconds. It receives text labels/checklist, not pixels, so the fixed checklist is the immediate guidance.
8. Close with the key gaps: no good/unrelated-image rejection, no validated AI severity, and no dependable defect localization.

## Suggested narration

“This is our Android edge-AI research prototype. Its real-data classifier predicts one of six NEU-DET steel-surface defect classes. On the 270-image held-out test split it reached 83.70% accuracy and 0.837 macro F1. This dataset has no normal class, so the app cannot confirm a defect-free part or reject an unrelated image. The checklist is a prompt for manual reinspection. The Low/Medium/High field is selected by an operator, not predicted by AI, and it is not a physical severity measurement or acceptance rule. Class, time, analysis duration, and the optional operator tier are saved locally; the image is not. All inference is on the phone.”

## Verified results to cite

- NEU-DET held-out test: 226/270 correct; 83.70% accuracy; macro F1 0.837.
- OPPO CPH2757: held-out `crazing_10.jpg` returned `crazing` in 904 ms on 19 November 2026. The manually selected Medium visual tier appeared in local history and remained after force-stop/relaunch. This confirms the workflow only; one sample does not validate accuracy or the tier.
- Llama/ExecuTorch: one local generation completed in 86.35 seconds (70 prompt tokens, 9 generated tokens). It does not inspect the image.

