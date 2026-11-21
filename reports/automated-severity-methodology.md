# Automated Morphological Defect Severity Methodology

## 1. Problem Statement
In real-world manufacturing and academic datasets (such as NEU-DET), defect images are categorized by type (`crazing`, `scratches`, etc.) or annotated with bounding boxes, but **lack human-graded severity annotations** (e.g. Low, Medium, High). Manually labeling thousands of training images for severity is:
- **Cost-prohibitive** and labor-intensive.
- **Subjective**, suffering from severe inter-annotator variance.
- **Disconnected from physical metrology** without calibrated depth/dimensional gauges.

Attempting to train a supervised deep neural network to predict categorical severity on unlabelled or arbitrarily assigned labels produces ungrounded hallucinations.

---

## 2. The Multi-Tier Engineering Solution

To satisfy PS5 defect severity requirements with scientific and engineering rigor, our edge solution uses a **Deterministic Computer-Vision Metrology & Calibration Framework**:

```
[Raw Optical Specimen]
          │
          ├──> 1. Closed-Set Defect Classification (OpenCV HOG-SVM, 83.70% Accuracy)
          │
          ├──> 2. Automated Morphological Metrology Engine (OpenCV Edge Pipeline, 5ms)
          │       ├── Adaptive Local Thresholding (Defect Segmentation)
          │       ├── Defect Area Coverage Density (% surface affected)
          │       └── Contour Topology & Micro-Fissure Count
          │
          ├──> 3. Calibrated Industrial Severity Tier
          │       ├── Low Severity    (< 2.5% area coverage)
          │       ├── Medium Severity (2.5% - 8.0% area coverage)
          │       └── High Severity   (> 8.0% area coverage / propagating fissures)
          │
          └──> 4. Human-in-the-Loop Visual Triage (Recorded in Local SQLite History)
```

---

## 3. Mathematical Formulation of Surface Defect Density

For an inspected Region of Interest (ROI) with dimensions $W \times H$ (standardized to $200 \times 200 = 40,000$ pixels):
1. **Local Contrast Normalization:**
   Gaussian blur filter $G_\sigma(x, y)$ removes high-frequency sensor noise while preserving surface defect contours:
   $$I_{\text{smooth}}(x, y) = I(x, y) * G_\sigma(x, y)$$
2. **Adaptive Defect Extraction:**
   Each pixel is evaluated against its local $21 \times 21$ neighborhood mean $T(x, y)$:
   $$B(x, y) = \begin{cases} 1 & \text{if } I_{\text{smooth}}(x, y) < T(x, y) - C \\ 0 & \text{otherwise} \end{cases}$$
3. **Defect Area Density Index ($\delta$):**
   $$\delta = \frac{1}{W \cdot H} \sum_{x=1}^{W} \sum_{y=1}^{H} B(x, y) \times 100\%$$
4. **Morphological Severity Mapping:**
   $$\text{Severity}(\delta) = \begin{cases}
   \text{Low} & \text{if } \delta < 2.5\% \\
   \text{Medium} & \text{if } 2.5\% \le \delta < 8.0\% \\
   \text{High} & \text{if } \delta \ge 8.0\%
   \end{cases}$$

---

## 4. Empirical Evaluation on the 270 Held-Out NEU-DET Test Set

Running the automated morphological engine on the untouched held-out 270-image test set reveals distinct physical defect signatures:

| Defect Class | Mean Defect Density ($\delta$) | Median Density | Low ($<2.5\%$) | Medium ($2.5-8\%$) | High ($\ge 8\%$) | Physical Interpretation |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **Inclusion** | **2.20%** | **2.04%** | 29 | 16 | 0 | Discrete, localized non-metallic particles. |
| **Scratches** | **8.04%** | **6.83%** | 2 | 24 | 19 | Balanced distribution across fine marks and heavy gouges. |
| **Pitted Surface** | **14.43%** | **12.27%** | 1 | 10 | 34 | Dense localized depression clusters. |
| **Rolled-in Scale**| **13.64%** | **13.75%** | 0 | 1 | 44 | Continuous longitudinal scale entrapment. |
| **Patches** | **25.50%** | **27.50%** | 0 | 0 | 45 | Large surface oxidation and discolored areas. |
| **Crazing** | **31.10%** | **32.06%** | 0 | 0 | 45 | Extensive interconnected micro-fissure networks. |

---

## 5. Key Advantages for Edge Deployment & Challenge Scoring

1. **Zero Training Labels Required:** Solves the unlabelled severity dilemma completely by calculating deterministic physical properties.
2. **Instant Edge Execution:** Runs in **5 ms** on an Arm Cortex-A CPU inside OpenCV, requiring zero GPU and zero cloud connectivity.
3. **Traceable & Auditable:** Unlike black-box neural networks, every percentage point of defect density is directly inspectable by a human operator.
4. **Few-Shot Golden Calibration:** If a plant specifies tighter tolerances, only **15–30 reference physical parts** (Golden Samples) are needed to calibrate the $\delta$ thresholds without retraining any model.
