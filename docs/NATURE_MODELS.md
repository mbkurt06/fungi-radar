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

Use **Python 3.11** and a fresh virtual environment.

The export tool is intentionally pinned to **Ultralytics 8.3.229**. Newer 8.4.x releases switched the TFLite path to LiteRT Torch and can currently pull a JAX/NumPy stack that conflicts with the TensorFlow export environment on macOS ARM64.

```bash
brew install python@3.11
deactivate 2>/dev/null || true
rm -rf .venv-model
python3.11 -m venv .venv-model
source .venv-model/bin/activate
python -m pip install --upgrade pip setuptools wheel
pip install -r tools/export-requirements.txt
python tools/export_openimages_tflite.py
```

Expected versions before export:
- Python 3.11.x
- Ultralytics 8.3.229
- NumPy 1.26.4
- TensorFlow 2.18.1

The resulting models are written to `exports/`. Import `exports/yolov8n-oiv7_float32.tflite` first.

When Mushroom is detected, the app shows a special banner, distinct alert and (when enabled) saves a JPEG plus CSV metadata. GPS is included when permission/fix is available. Auto-saves are throttled to once per 10 seconds.

Model/software licensing from Ultralytics applies to exported weights. The repository does not redistribute those model binaries.

Detection is not an edibility decision.
