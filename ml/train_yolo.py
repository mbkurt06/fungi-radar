"""Optional training experiment; YOLO TFLite export is NOT directly compatible with the Android Task Vision parser."""
from ultralytics import YOLO

model = YOLO("yolo11n.pt")
model.train(data="ml/dataset.yaml", imgsz=640, epochs=80, batch=16, project="ml/runs", name="mushrooms")
model.val()
model.export(format="tflite", imgsz=640)
print("NOTE: raw YOLO TFLite needs a matching Android decoder; see MODEL.md")
