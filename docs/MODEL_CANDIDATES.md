# Model catalog and target classes

## Downloadable in the app now
All four presets below are official TensorFlow Lite Task Vision compatible COCO detectors and can be switched without rebuilding:
- SSD MobileNet V1 — fast baseline
- EfficientDet Lite0 — balanced
- EfficientDet Lite1 — medium
- EfficientDet Lite2 — higher quality / slower

COCO has 80 object classes. It includes person, chair, potted plant, car, dog, cat, etc. It does not include generic mushroom or tree classes.

The app has a target filter. With COCO models you can test Person, Chair, Potted plant and other supported labels. Mushroom and Tree are already present as filter targets so a future/imported detector with those labels works without redesigning the UI.

## Mushroom + tree candidate
Open Images V7 has roughly 600 detection classes and includes a much broader vocabulary than COCO, including classes useful to this project. YOLOv8n Open Images weights are available, but their TFLite export uses YOLO raw output and is not compatible with the current Task Vision decoder.

Next engineering milestone:
1. Add a dedicated YOLO TFLite decoder.
2. Export/download YOLOv8n Open Images V7 TFLite.
3. Bundle/associate the 600-class label map.
4. Validate Mushroom and Tree labels on Samsung S24.
5. Compare latency/recall against the custom one-class mushroom detector.

Do not silently treat potted plant as tree or claim COCO finds mushrooms.
