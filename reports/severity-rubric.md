# Operator visual severity rubric

## Purpose and status

This rubric lets an operator record a consistent visual triage tier after the app predicts a NEU-DET class. The app does not predict severity. The operator chooses Low, Medium, or High from the visible descriptions; the selected value is saved with the local inspection history.

These are team-defined visual cues for a research demonstration. They are not an engineering standard, a physical measurement, or acceptance/rejection criteria. A phone image cannot establish crack depth, remaining wall thickness, pit depth, dimensional tolerance, or fitness for service. When the part is safety-critical or the view is unclear, leave the tier unrecorded and refer it to a qualified inspector using the applicable drawing, gauge, and inspection procedure.

## Cues shown in the app

| Predicted class | Low | Medium | High |
|---|---|---|---|
| Crazing / crack-like pattern | Short, faint surface line; no visible opening; away from a critical edge or joint. | Clear continuous line, especially near an edge, transition, thread, or joint. | Wide/open line, branching pattern, or line crossing a functional or load-bearing area. |
| Scratches | Small isolated mark away from a sealing or contact surface. | Clearly visible groove or mark near a thread, fit, or contact surface. | Extensive gouge or mark crossing a sealing, thread, or functional surface. |
| Pitted surface / corrosion | Small localized discoloration; no obvious flaking or broad rough area. | Several visible rough or pitted spots, or a clearly affected local patch. | Widespread flaking, clustered deep-looking pits, or affected functional surfaces. |
| Patches / rolled-in scale / inclusion | Small isolated visible region, away from a functional edge or contact area. | Clearly visible region across part of the surface or near a functional area. | Large, multiple, or functionally located regions that need prompt inspector review. |
| Deformation | Small local contour change, away from a mating or sealing surface. | Visible contour change near a thread, fit, edge, or contact surface. | Major visible shape change at a bore, mating, sealing, or load-bearing area. |

The severity tier is separate from classifier confidence. A high confidence score does not mean high severity, and vice versa.

## Evidence and validation gap

NEU-DET contains six defect labels and bounding boxes, but no Low/Medium/High severity labels or physical measurements. Its images are 200 x 200 steel-strip crops, not complete plumbing fittings. The existing HOG bounding-box baseline recovered 54.6% of 634 held-out boxes at IoU 0.3 and is not integrated into the app. Therefore no model-based severity accuracy can be reported from the current data.

The previous Severstal mask-area proxy reached 42.7% accuracy and 0.421 macro F1 on its proxy task; it is not reliable and is excluded from the app. Synthetic severity metrics are excluded from competition evidence as instructed by the mentor.

## What would support an AI severity model

Use permitted photos of real target parts and have qualified reviewers assign severity from the applicable product requirements. Record the defect type, visible region, measurement method and value where available, location relative to functional features, reviewer rationale, and the acceptance document used. Keep all images of each physical part in a single train, validation, or test split. Resolve disagreements before training. Report per-tier precision/recall, confusion matrix, and a weighted ordinal metric; do not call a confidence score severity. Until that evidence exists, describe the app value as an operator-recorded visual tier only.
