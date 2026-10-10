# Nature / mushroom detection

FungiRadar supports TensorFlow Lite Task Vision detectors and raw YOLOv8 TFLite detection exports.

Open Images V7 has 601 classes in this app's label map. Important labels:
- Mushroom: 347
- Tree: 553
- Plant: 393
- Flower: 195
- Palm tree: 364
- Maple: 324
- Willow: 586

## Export on macOS / PC

Use **Python 3.11**. Newer Python releases may not have compatible TensorFlow wheels.

On macOS with Homebrew:

```bash
brew install python@3.11
rm -rf .venv-model
/opt/homebrew/bin/python3.11 -m venv .venv-model
source .venv-model/bin/activate
python -m pip install --upgrade pip setuptools wheel
pip install -r tools/export-requirements.txt
python tools/export_openimages_tflite.py
```

On Intel Macs, the Python path may be `/usr/local/bin/python3.11`. You can use `which python3.11` to find it.

The resulting models are written to `exports/`. Import `exports/yolov8n-oiv7_float32.tflite` first.

You can also run the manual GitHub Actions workflow "Export Nature TFLite models".

When Mushroom is detected, the app shows a special banner, distinct alert and (when enabled) saves a JPEG plus CSV metadata. GPS is included when permission/fix is available. Auto-saves are throttled to once per 10 seconds.

Model/software licensing from Ultralytics applies to exported weights. The repository does not redistribute those model binaries.

Detection is not an edibility decision.
