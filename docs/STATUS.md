# Verified status

Implemented in source: CameraX preview/analyzer, Task Vision object detector adapter, local model import/selection, overlay, audio/vibration, threshold and optional GPS.

NOT YET VERIFIED:
- Android CI compilation and installation on Samsung S24.
- Model import with a known compatible mushroom-specific model.
- Any mushroom detection accuracy.
- Camera crop/rotation box alignment.
- GPS waypoint saving (currently only reads coordinates).

Model files and external hosted inference services are not connected. No automatic downloading or API key storage is implemented. Do not present the app as a finished mushroom detector until weights and field tests are completed.
