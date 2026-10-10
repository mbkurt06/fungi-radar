# Model selection and compatibility

## Android interface
The app now has a model dropdown and **Import local .tflite model** button. Models are copied into private app storage, and selection persists. Switching models does not require recompiling. An incompatible model shows an error and detection stays disabled.

**No pretrained mushroom model is bundled.** The repository contains neither a verified freely redistributable mushroom detection model nor a remote Roboflow inference connection. It is not acceptable to silently substitute a generic COCO detector, because COCO has no mushroom class.

## Required model contract
Use a TensorFlow Lite Task Vision **ObjectDetector-compatible** model, including expected detection outputs and TFLite metadata. Generic YOLO TFLite exports often expose raw prediction tensors and cannot be imported directly without a custom YOLO decoder.

For mushroom-specific models from Roboflow or elsewhere:
1. Confirm license permits downloading and redistribution.
2. Download actual weights, not just an API demo.
3. Inspect input/output tensor shapes and preprocessing.
4. Convert to compatible Task Vision format, or add the appropriate model-specific interpreter/decoder.
5. Test detection accuracy on held-out forest video.
6. Import the verified model on-device with the button.

## Test cases
- Empty model list -> camera only, explicit warning
- Invalid .tflite -> unsupported model message
- Two valid detectors -> switch in dropdown, persist selection
- Real mushroom -> bounding boxes and alert
- Pinecones, leaves, rocks -> false-positive measurement
- Rotated phone -> overlay crop alignment (still needs calibration)

## Dataset
Train a single class `mushroom`, labeling visible fruiting bodies of any species. Add hard negatives and split train/validation/test by forest visit, not by adjacent video frames.
