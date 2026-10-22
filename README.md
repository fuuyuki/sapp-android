# Smart Adherence Pillbox (SAP) – Android App

Android client for the **Smart Adherence Pillbox (SAP)**. It connects to the FastAPI backend and adds an on-device camera proof flow for medication doses.

> **Source of truth:** the ESP32 pillbox decides `taken` / `missed`. The Android app never creates or updates medlogs. It can only attach a photo + confidence to an existing medlog as additional proof.

## Features

- JWT authentication (login / registration)
- Patient and caretaker dashboards
- Device management
- Medication schedules
- Medlog history (read-only; pillbox-owned status)
- **Camera mouth-open proof**
  - CameraX front-camera preview + `ImageAnalysis`
  - MediaPipe `FaceLandmarker` (`LIVE_STREAM`, 1 face, GPU with CPU fallback)
  - Mouth Aspect Ratio trigger over 8 consecutive frames
  - One JPEG capture, multipart upload to the existing medlog
  - WorkManager offline queue with exponential backoff
  - Local cooldown + one verified upload per medlog
- Indonesian and English UI strings

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Retrofit + OkHttp (existing JWT interceptor)
- Kotlin Coroutines / StateFlow
- CameraX 1.4
- MediaPipe Tasks Vision 0.10.14
- WorkManager
- Firebase Cloud Messaging
- DataStore

## Requirements

- Android Studio (latest stable)
- JDK 17
- Android SDK platform 36
- `minSdk 24`, `targetSdk 36`, `compileSdk 36`

## Build and run

1. Clone:
   ```bash
   git clone https://github.com/fuuyuki/sapp-android.git
   cd sapp-android
   ```
2. Open in Android Studio or build from the CLI:
   ```bash
   ./gradlew :app:assembleDebug
   ```
3. Install:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. The backend base URL is in
   `app/src/main/java/com/example/sapp/data/network/NetworkModule.kt`.
   `app/google-services.json` must match the Firebase project and application id.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

Covered:

- MAR math with synthetic landmarks
- 8-frame consecutive debounce
- Confidence bounds
- WIB ±60-minute medlog matching
- Due-dose resolution
- Multipart `photo` / `confidence` / `Authorization` assertions
- `404 → 200` retry

## Camera proof: how it works

1. The patient opens **Confirm Meds** or taps **Verify with camera** on a medlog row.
2. The app asks for the camera permission at the point of use.
3. Per frame, MediaPipe returns normalized landmarks. The app computes:
   `MAR = |y13 - y14| / max(ε, |x61 - x291|)` after scaling to the image size.
4. The trigger requires:
   - exactly one face
   - face width ≥ 25% of the frame
   - |yaw| and |pitch| ≤ 20°
   - `MAR ≥ 0.35` for 8 consecutive analyzed frames
5. The app captures the triggering frame as one JPEG (max 1280 px, quality 80).
6. It selects the ESP32-created medlog by:
   - trimmed, case-insensitive `pillname`
   - `scheduled_time` within ±60 minutes of now in **Asia/Jakarta**
   - `evidence_key == null`
   - latest match
7. If no medlog matches, it does **not** create one; it shows *“Waiting for the pillbox to report this dose.”*
8. Upload `PUT medlogs/{medlog_id}/evidence` with:
   - `photo` (required)
   - `confidence` (required)
   - `captured_at` (optional)
   - `method=camera_mouth_open` (optional)
9. Offline: the JPEG is queued with WorkManager (`NetworkType.CONNECTED`, exponential backoff, max 8 runs).

No frames or video are uploaded; only the single JPEG.

## GitHub Releases

The repository includes:

- `.github/workflows/ci.yml` — unit tests + debug APK on pushes/PRs
- `.github/workflows/release.yml` — signed APK + `latest.json` on `v*` tags

### 1. Create a release keystore (one time)

```bash
keytool -genkeypair -v \
  -keystore release.jks \
  -alias sapp \
  -keyalg RSA -keysize 2048 -validity 10000
```

Do **not** commit `release.jks`. It is covered by `.gitignore`.

Base64-encode it for GitHub:

```bash
# Linux
base64 -w0 release.jks > release.jks.b64
# macOS
base64 -i release.jks -o release.jks.b64
```

### 2. Add repository secrets

Settings → Secrets and variables → Actions:

| Secret | Value |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | contents of `release.jks.b64` |
| `SIGNING_STORE_PASSWORD` | keystore password |
| `SIGNING_KEY_ALIAS` | `sapp` (or your alias) |
| `SIGNING_KEY_PASSWORD` | key password |

### 3. Publish a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow:

- derives `versionName` from the tag (`v1.0.0` → `1.0.0`)
- uses the GitHub run number as `versionCode`
- builds a signed `app-release.apk`
- publishes `sapp-1.0.0.apk` and `latest.json` to the GitHub Release

### 4. Local signed build (optional)

```bash
export SIGNING_KEYSTORE_PATH="$PWD/release.jks"
export SIGNING_STORE_PASSWORD="..."
export SIGNING_KEY_ALIAS="sapp"
export SIGNING_KEY_PASSWORD="..."
export VERSION_NAME="1.0.0"
export VERSION_CODE="1"
./gradlew :app:assembleRelease
```

Without those environment variables, `assembleRelease` produces an **unsigned** APK and exits successfully; only CI publishes signed artifacts.

## In-app update checker

The top bar of both dashboards has a **Check for updates** button. It uses the `latest.json` asset published by the release workflow:

```json
{
  "versionName": "1.0.0",
  "versionCode": 12,
  "tag": "v1.0.0",
  "apk": "sapp-1.0.0.apk",
  "sha256": "...",
  "publishedAt": "2026-10-22T00:00:00Z"
}
```

The flow:

1. Fetch `https://github.com/fuuyuki/sapp-android/releases/latest/download/latest.json`.
2. Compare `versionCode` with `BuildConfig.VERSION_CODE`.
3. Download the APK from `.../releases/latest/download/<apk>` with a progress dialog.
4. Verify the SHA-256 from `latest.json`.
5. Open Android's package installer via `FileProvider`.

`REQUEST_INSTALL_PACKAGES` is declared. On Android 8+, if “install unknown apps” is not allowed, the app opens the settings page. Android always shows the final installer confirmation; silent installation is not allowed for normal apps.

The **first install is still manual**: send the client to
`https://github.com/fuuyuki/sapp-android/releases/latest`, have them install the APK once, and subsequent updates can use the in-app button.

The release must contain both `sapp-<version>.apk` and `latest.json`; the release workflow does this automatically.

## API endpoints used

Base URL: `https://sap.protofylabs.web.id/`

- `POST /register`, `POST /login`, `GET /me`
- `GET/PUT /users/{user_id}`
- `GET /users/{user_id}/adherence-summary`
- `GET /caretakers/{caretaker_id}/patients`
- `GET /devices`
- `GET /schedules/by-user/{user_id}`
- `GET /schedules/by-caretaker/{caretaker_id}/{patient_id}`
- `POST/PUT/DELETE /schedules/...`
- `GET /medlogs/{user_id}`
- `GET /medlogs/{caretaker_id}/{patient_id}`
- `PUT /medlogs/{medlog_id}/evidence`
- `POST /register_token`

`GET /medlogs/{medlog_id}/evidence` and `DELETE .../evidence` exist server-side; the app currently only displays the evidence metadata, not an image viewer.

## Project structure

```text
app/
  src/main/java/com/example/sapp/
    data/                 # models, network, local storage
    ui/                   # Compose screens
    verification/         # camera proof, matching, upload queue
  src/main/assets/face_landmarker.task
  src/test/java/com/example/sapp/
    verification/         # JVM tests + MockWebServer
.github/workflows/
  ci.yml
  release.yml
```

## Release notes / limitations

- The application id is still `com.example.sapp`. Before a public release, consider changing `namespace`/`applicationId` and regenerating `google-services.json`; this changes the package identity and breaks upgrades from the old id.
- `google-services.json` is committed. Restrict the Firebase API key to the Android app in Google Cloud.
- `android:allowBackup="true"` is unchanged. Review backup handling for medical data.
- No license has been chosen yet. Add a `LICENSE` before distributing publicly.
- The MediaPipe model (`assets/face_landmarker.task`, ~3.6 MB) is vendored so the app works offline; it must remain in the repository.
- Debug and release builds use different signing keys. Uninstall a debug build before installing a release APK.
- The universal APK is large (~64 MB) because it bundles MediaPipe native libraries for all ABIs. Per-ABI splits are not enabled to keep the release asset and the update checker simple.
- `versionCode` comes from the GitHub Actions run number. A locally built release should pass `-PversionCode` explicitly if it must be higher than an installed release.
