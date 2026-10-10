#!/usr/bin/env python3
import sys
from pathlib import Path
import shutil

if sys.version_info[:2] != (3, 11):
    raise SystemExit(
        "FungiRadar model export requires Python 3.11. "
        f"Current Python is {sys.version.split()[0]}. "
        "Create the venv with python3.11 -m venv .venv-model."
    )

import numpy as np
import ultralytics
from ultralytics import YOLO

print(f"Python: {sys.version.split()[0]}")
print(f"Ultralytics: {ultralytics.__version__}")
print(f"NumPy: {np.__version__}")

if ultralytics.__version__ != "8.3.229":
    raise SystemExit(
        "Wrong Ultralytics version. Expected 8.3.229. "
        "Delete .venv-model and reinstall tools/export-requirements.txt."
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
