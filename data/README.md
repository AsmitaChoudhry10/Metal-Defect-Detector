# Data handling

- `raw/` contains locally downloaded source datasets. It is excluded from Git; reviewers should download NEU-DET from the official source linked in the root README and comply with its terms.
- `processed/splits/` contains the part/image-level CSV split manifests used for the reported NEU evaluation. The referenced images and annotations remain local under `raw/` and are not committed.
- Synthetic example images and derived synthetic metrics are excluded from the challenge demo and are ignored by Git under mentor guidance.
- KolektorSDD2 is a separate-domain experiment and is licensed CC BY-NC-SA 4.0. Keep its images local and do not present its result as general-purpose normal/defect performance.

The app bundles the NEU-trained classifier asset and a separate KolektorSDD2 model because they are needed by the mobile app. The primary NEU-DET evaluation is described in `reports/phase1-technical-report.md`.
