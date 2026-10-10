# Nature / mushroom detection

FungiRadar supports TensorFlow Lite Task Vision detectors and raw YOLOv8 TFLite detection exports.

Open Images V7 has 601 classes in this app's label map. Important labels include Mushroom and Tree.

## macOS export environment

Use Python 3.11 and a fresh virtual environment. The export stack is pinned because Ultralytics 8.3.229 otherwise auto-installs newer TensorFlow helper packages and may silently replace TensorFlow/NumPy with incompatible releases.

Important pinned versions:
- Ultralytics 8.3.229
- NumPy 1.26.4
- TensorFlow 2.19.0
- tf_keras 2.19.0
- ONNX 1.17.0
- onnx2tf 1.28.8
- ai-edge-litert 1.3.0

The script sets `YOLO_AUTOINSTALL=false` so the exporter cannot mutate the environment at runtime.

```bash
deactivate 2>/dev/null || true
rm -rf .venv-model
python3.11 -m venv .venv-model
source .venv-model/bin/activate
python -m pip install --upgrade pip setuptools wheel
pip install -r tools/export-requirements.txt
python -m pip check
python tools/export_openimages_tflite.py
```

The resulting models are written to `exports/`. Import `exports/yolov8n-oiv7_float32.tflite` first.

Detection is not an edibility decision.
