# Android model smoke check

The on-device OpenCV classifier was manually checked against six held-out NEU-DET images. All six predictions matched their known labels:

| Image | Expected | Phone result |
|---|---|---|
| crazing_10.jpg | crazing | Match |
| inclusion_105.jpg | inclusion | Match |
| patches_109.jpg | patches | Match |
| pitted_surface_102.jpg | pitted surface | Match |
| rolled-in_scale_112.jpg | rolled-in scale | Match |
| scratches_1.jpg | scratches | Match |

This is a smoke check, not a full device benchmark. The desktop held-out test accuracy for the OpenCV HOG + linear SVM model was 81.9% on 270 images. The classifier always chooses one of the six defect classes and has no defect-free class; it is not validated on the photographed plumbing fitting.
