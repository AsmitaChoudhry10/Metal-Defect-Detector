"""
Automated Morphological Defect Severity Metrology for NEU-DET
Solves the unlabelled severity problem by extracting deterministic
pixel-level surface defect density and contour topology directly from images.
"""
import os
import glob
import json
import cv2
import numpy as np

def analyze_dataset(test_dir):
    classes = ['crazing', 'inclusion', 'patches', 'pitted_surface', 'rolled-in_scale', 'scratches']
    stats = {c: [] for c in classes}

    for f in glob.glob(os.path.join(test_dir, '*.jpg')):
        basename = os.path.basename(f)
        cls = next((c for c in classes if basename.startswith(c)), None)
        if not cls:
            continue
        img = cv2.imread(f, cv2.IMREAD_GRAYSCALE)
        if img is None:
            continue
        resized = cv2.resize(img, (200, 200))
        blurred = cv2.GaussianBlur(resized, (5, 5), 0)
        thresh = cv2.adaptiveThreshold(blurred, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY_INV, 21, 5)
        defect_pixels = cv2.countNonZero(thresh)
        area_pct = (defect_pixels / 40000.0) * 100.0

        contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        stats[cls].append({
            'file': basename,
            'area_pct': round(float(area_pct), 2),
            'contours': len(contours),
            'severity': 'Low' if area_pct < 2.5 else ('Medium' if area_pct < 8.0 else 'High')
        })

    summary = {}
    for c in classes:
        items = stats[c]
        areas = [x['area_pct'] for x in items]
        summary[c] = {
            'count': len(items),
            'mean_defect_density_pct': round(float(np.mean(areas)), 2),
            'median_defect_density_pct': round(float(np.median(areas)), 2),
            'low_severity_count': sum(1 for x in items if x['severity'] == 'Low'),
            'medium_severity_count': sum(1 for x in items if x['severity'] == 'Medium'),
            'high_severity_count': sum(1 for x in items if x['severity'] == 'High'),
        }

    return summary, stats

if __name__ == '__main__':
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    test_dir = os.path.join(base_dir, 'android-app', 'app', 'src', 'androidTest', 'assets', 'neu_test')
    summary, full_stats = analyze_dataset(test_dir)
    print(json.dumps(summary, indent=2))
    out_path = os.path.join(base_dir, 'reports', 'neu-morphological-severity-analysis.json')
    with open(out_path, 'w') as f:
        json.dump({'summary': summary, 'samples': full_stats}, f, indent=2)
    print(f"Report saved to {out_path}")
