# Changelog

All notable changes to this project are documented here.

## [1.0.0] - 2026-10-22

### Added

- JWT login and registration.
- Patient and caretaker dashboards.
- Device management.
- Medication schedules.
- Read-only medication log history.
- Camera mouth-open verification:
  - CameraX front-camera preview and `ImageAnalysis`.
  - MediaPipe `FaceLandmarker` with `LIVE_STREAM`, one face, and GPU → CPU fallback.
  - MAR (Mouth Aspect Ratio) trigger over 8 consecutive frames.
  - One-shot JPEG capture and multipart evidence upload.
  - WIB ±60-minute medlog matching.
  - WorkManager offline queue with exponential backoff.
  - Five-minute cooldown and one verified upload per medlog.
- Evidence metadata in the medlog DTO and history UI.
- In-app GitHub update checker:
  - Checks the latest `latest.json` release asset.
  - Downloads the APK with progress.
  - Verifies the SHA-256 from the release metadata.
  - Opens Android's package installer via a `FileProvider`.
  - Opens the “install unknown apps” settings when required.
- Responsive UI refresh:
  - New design system with shared cards, gradient headers, pills, and empty states.
  - Adaptive card grids and two-pane layouts for tablets and landscape screens.
  - Navigation rail on large screens, bottom navigation on phones.
  - Responsive authentication and medication-form layouts.
  - Light/dark theme support with transparent system bars.
- GitHub Actions CI and tag-based signed release workflow.
- `latest.json` release asset for a future in-app update checker.
