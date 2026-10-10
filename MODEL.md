# Model contract

The Android application uses **TensorFlow Lite Task Vision ObjectDetector**. It requires a compatible TFLite detection model with metadata and labels. Merely renaming a YOLO export to `mushroom_detector.tflite` will **not** work: many YOLO models have raw tensors requiring preprocessing, non-max suppression, and decoding.

## Recommended fastest working route
1. Obtain a properly licensed, mushroom-specific labeled detection dataset (bounding boxes, not image-level labels).
2. Train a model with a **documented Task Vision compatible export path**, or implement a custom YOLO TFLite output parser.
3. Evaluate on independent forest videos (small/occluded mushrooms, leaves, brown caps, poor light).
4. Put the verified compatible file in `app/src/main/assets/mushroom_detector.tflite`.
5. Confirm inference outputs and overlay alignment on Samsung S24.

Until step 4, the app displays a camera feed and clearly reports that AI detection is unavailable. It does not claim to find mushrooms.

## Dataset labeling
Use one class: `mushroom`. Label every visible fruiting body with a bounding box. Include negative images: rocks, leaves, pinecones, tree stumps, plastic, bark. Split by **location and recording session**, not random frames, to avoid data leakage.

## Metrics
Measure precision, recall, mAP50, false alarms per minute, latency and battery drain on-device. Optimize for recall while keeping false alarms manageable.

## License
Ultralytics has AGPL-3.0 / enterprise terms. Dataset and pretrained model licenses may differ from this repository's Apache-2.0 license.
