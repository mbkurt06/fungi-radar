# FungiRadar 🍄

Offline-first Android camera application for finding **visible** mushrooms in the field. Built for Samsung Galaxy S24 and other Android phones.

## Current implementation
- CameraX live camera preview and frame analysis.
- TensorFlow Lite object detection with on-device inference (no cloud/API key).
- Bounding-box overlay, confidence score, vibration and beep on detection.
- Adjustable confidence threshold; local GPS coordinates can be shown.
- Safe behavior when no model is installed: camera works, **detection disabled** (no fabricated results).

**Important:** This is an experimental visual detector, not a mushroom edibility classifier. Never eat a mushroom based on its output. It cannot see through leaves or soil.

## Quick start
1. Install Android Studio (current stable) and Android SDK 35, JDK 17.
2. Clone this repository, open the root folder in Android Studio, sync Gradle.
3. Connect Samsung S24 with USB debugging, click Run.
4. To enable detections, train/export a compatible model and place it at `app/src/main/assets/mushroom_detector.tflite`.
5. Rebuild and run. See [MODEL.md](MODEL.md) and [SETUP.md](SETUP.md).

The app intentionally ships **without pretrained model weights**: generic COCO object detectors do not have a mushroom class. A model must be trained/evaluated on mushroom images first.

## Structure
- `app/`: native Kotlin Android app
- `ml/`: dataset config and model training/export instructions
- `docs/`: milestones, safety, and evaluation guidance

## License
Apache-2.0 for this repository's original source code. **Third-party training libraries and model weights have their own licenses.** Ultralytics YOLO models can carry AGPL-3.0 requirements; check licensing before distributing a combined application.
