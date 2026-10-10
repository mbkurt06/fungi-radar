# Model candidates

## 1. TensorFlow SSD MobileNet V1 COCO — integrated test preset
Purpose: verify the complete Android inference pipeline (download, TFLite loading, boxes, confidence, beep/vibration). It is **not a mushroom detector** because COCO has no mushroom class.

Source: TensorFlow Lite Support official example / TensorFlow Hub metadata model.
License: Apache-2.0 according to the TensorFlow model listing.

The app can download this preset itself.

## 2. Open Images V4 SSD MobileNet V2 — primary general-mushroom research candidate
Open Images includes a **Mushroom** boxable category. Google's SSD MobileNet V2 model is trained on all 600 Open Images V4 boxable classes and is Apache-2.0. The published artifact is a TensorFlow SavedModel, not currently a drop-in Task Vision TFLite detector. We need a verified conversion/export before shipping it on Android.

Why it is promising: it directly has a generic Mushroom class, matching FungiRadar's first goal.

## 3. FungiTastic DINOv3 — species classifier, secondary stage
A recent mobile TFLite classifier covers thousands of fungi species. It is classification, not detection, so it cannot replace the detector. Later it can classify a crop after the detector finds a candidate.

## Rule
Do not label a model as production-ready until:
- its redistributable license is verified,
- Android input/output compatibility is tested,
- false positives on leaves, pinecones, bark and rocks are measured,
- it is field-tested on independent forest videos.
