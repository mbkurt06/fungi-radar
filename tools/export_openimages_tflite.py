#!/usr/bin/env python3
from pathlib import Path
import shutil
from ultralytics import YOLO
OUT=Path("exports");OUT.mkdir(exist_ok=True)
for name in ("yolov8n-oiv7.pt","yolov8s-oiv7.pt"):
    model=YOLO(name)
    exported=Path(model.export(format="tflite",imgsz=640,nms=False,half=False))
    candidates=list(exported.parent.rglob("*_float32.tflite"))
    if not candidates: raise SystemExit(f"No float32 TFLite found under {exported.parent}")
    dst=OUT/(name.replace(".pt","")+"_float32.tflite")
    shutil.copy2(candidates[0],dst);print(f"Ready: {dst}")
print("Import a model from exports/ in FungiRadar.")
