#!/usr/bin/env python3
import os
import sys
from pathlib import Path
import shutil

if sys.version_info[:2] != (3, 11):
    raise SystemExit(
        "FungiRadar model export requires Python 3.11. "
        f"Current Python is {sys.version.split()[0]}."
    )

# Never let Ultralytics mutate this carefully pinned export environment.
os.environ["YOLO_AUTOINSTALL"] = "false"

import numpy as np
import tensorflow as tf
import tf_keras
import ultralytics
import onnx
import onnx2tf
from ultralytics import YOLO

EXPECTED = {
    "ultralytics": "8.3.229",
    "numpy": "1.26.4",
    "tensorflow": "2.19.0",
    "tf_keras": "2.19.0",
    "onnx": "1.17.0",
    "onnx2tf": "1.28.8",
}
ACTUAL = {
    "ultralytics": ultralytics.__version__,
    "numpy": np.__version__,
    "tensorflow": tf.__version__,
    "tf_keras": tf_keras.__version__,
    "onnx": onnx.__version__,
    "onnx2tf": onnx2tf.__version__,
}
print(f"Python: {sys.version.split()[0]}")
for key, value in ACTUAL.items():
    print(f"{key}: {value}")

bad = [f"{k}={ACTUAL[k]} (expected {v})" for k, v in EXPECTED.items() if ACTUAL[k] != v]
if bad:
    raise SystemExit(
        "Pinned export environment is not clean:\n  " + "\n  ".join(bad) +
        "\nDelete .venv-model and reinstall tools/export-requirements.txt."
    )

OUT = Path("exports")
OUT.mkdir(exist_ok=True)

for name in ("yolov8n-oiv7.pt", "yolov8s-oiv7.pt"):
    print(f"Loading {name}...")
    model = YOLO(name)
    exported = Path(model.export(format="tflite", imgsz=640, nms=False, half=False))
    candidates = list(exported.parent.rglob("*_float32.tflite"))
    if not candidates and exported.suffix == ".tflite":
        candidates = [exported]
    if not candidates:
        raise SystemExit(f"No float32 TFLite found under {exported.parent}")
    dst = OUT / (name.replace(".pt", "") + "_float32.tflite")
    shutil.copy2(candidates[0], dst)
    print(f"Ready: {dst}")

print("Import a model from exports/ in FungiRadar.")
