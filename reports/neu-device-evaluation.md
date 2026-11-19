# NEU-DET classifier on OPPO

Date: 2026-11-04  
Device: OPPO CPH2757, Android 16  
Runtime: app OpenCV SVM + HOG classifier

## Held-out result

The Android instrumentation run processed all 270 images from the existing NEU-DET held-out test split and passed (`OK (1 test)`). It classified 226 correctly: **83.70% accuracy**. This matched the desktop OpenCV evaluation exactly.

Confusion matrix, rows = true class and columns = predicted class; order is `crazing`, `inclusion`, `patches`, `pitted_surface`, `rolled-in_scale`, `scratches`:

```text
[[41, 0, 4, 0, 0, 0],
 [ 0,35, 1, 8, 0, 1],
 [ 3, 0,36, 4, 0, 2],
 [ 5, 2, 6,31, 0, 1],
 [ 0, 0, 0, 0,45, 0],
 [ 1, 5, 1, 0, 0,38]]
```

Median classifier-call time was 2 ms. The timer covered model prediction, not image decoding or camera capture. The full 270-image instrumentation loop took 1.433 seconds; this is not a full camera-to-screen user-flow benchmark.

## Cache fix

The first device run still scored like the old model because the app reused an earlier model copy from its cache directory after an APK update. The loader now refreshes that cached file from the APK on first use in each app process and keeps the parsed SVM in memory. After rebuilding and reinstalling, the full phone result matched the desktop result. The previous model remains at `models/neu_hog_svm_before_tuned.xml`.

The detector remains closed-set: its training data contains six defect classes but no normal or unrelated examples. These results do not establish good-part rejection or performance on plumbing fittings.
