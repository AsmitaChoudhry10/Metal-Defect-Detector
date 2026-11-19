# NEU-DET data check

## Result

- Extracted layout: `data/raw/NEU-DET/NEU-DET/IMAGES/` and `data/raw/NEU-DET/NEU-DET/ANNOTATIONS/`.
- 1,800 JPG images and 1,800 XML annotation files.
- Every image is 200 x 200 pixels.
- Every image has a matching XML file; no unmatched files were found.
- The XML annotations use the six expected labels: crazing, inclusion, patches, pitted_surface, rolled-in_scale, and scratches.

## Object annotation counts

These are bounding-box counts, not image counts. An image may contain more than one annotated defect.

| Defect label | Annotated boxes |
|---|---:|
| crazing | 689 |
| inclusion | 1,011 |
| patches | 881 |
| pitted_surface | 432 |
| rolled-in_scale | 628 |
| scratches | 548 |

## Important limitation

This is a defect-only dataset; it does not provide defect-free steel images for learning or measuring a `PASS` result. The first model can learn to locate and classify the six known defect types. Before presenting it as a pass/fail inspector, add a separate, representative set of good-part images and evaluate false alarms on them. Until then, unknown or low-confidence images should be marked `RECHECK`, not `PASS`.

## Next data step

Create a fixed, reproducible train/validation/test split while keeping each image and its XML annotation together. Do not augment or copy images across splits.
