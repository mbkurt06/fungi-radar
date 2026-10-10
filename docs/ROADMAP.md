# Roadmap

## Phase 0 — Android foundation
- [x] Camera preview, permissions, frame analysis
- [x] TFLite Task Vision integration scaffold
- [x] Overlay, threshold slider, sound, vibration
- [x] Optional location read
- [ ] Device build + Samsung S24 camera verification

## Phase 1 — Actual mushroom detection
- [ ] Find and license a mushroom bounding-box dataset
- [ ] Train/validate a general mushroom detector
- [ ] Choose compatible TFLite export OR implement YOLO decoding
- [ ] Package model and run on S24
- [ ] Correct crop/rotation/box coordinate mapping

## Phase 2 — Field readiness
- [ ] Background-safe throttling and battery profiling
- [ ] Save geotagged finds to local Room database
- [ ] Offline map / GPX export
- [ ] Adjustable sound/vibration and flashlight
- [ ] Multi-scale detection for small distant mushrooms
- [ ] Field test under autumn forest conditions

## Safety
Only detects visible mushroom-like objects. Does not identify edibility, does not detect hidden mushrooms, and cannot guarantee finding all mushrooms.
