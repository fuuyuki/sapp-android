# Smart Adherence Pillbox (SAP) – Android App

This repository contains the Android application source code for **Smart Adherence Pillbox (SAP)**.  
The app integrates with a FastAPI backend to provide medication scheduling, device management, and adherence logging features.

---

## 🚀 Features
- **User Authentication**: Register and login with JWT-based authentication.
- **Device Management**: Add, update, and delete pillbox devices.
- **Medication Schedules**: Create and manage schedules for medication doses.
- **Medlogs**: Record whether a dose was taken or missed.
- **Notifications**: Push reminders to Android via system notifications.
- **Modern Android Stack**:
  - Kotlin
  - Retrofit + OkHttp (with JWT interceptor)
  - Coroutines
  - Jetpack components (ViewModel, LiveData/StateFlow)
  - Material Design UI

---

## 📂 Project Structure
```code
sapp-android/
├── app/                  # Main Android app module
├── gradle/               # Gradle wrapper and build scripts
├── .idea/                # Android Studio project settings
├── build.gradle.kts      # Project-level Gradle config
├── settings.gradle.kts   # Module settings
└── README.md             # Project documentation
```

---

## ⚙️ Requirements
- **Android Studio** (latest stable version)
- **Minimum SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 34 (Android 14)
- **Backend**: FastAPI server with endpoints for users, devices, schedules, and medlogs

---

## 🔧 Setup
1. Clone the repository:
```bash
   git clone https://github.com/fuuyuki/sapp-android.git
   cd sapp-android
```
2. Open in Android Studio.
3. Sync Gradle to install dependencies.
4. Configure the backend base URL in ApiClient.kt:
```Kotlin
private const val BASE_URL = "https://sap.protofylabs.web.id/"
```
5. Run the app on an emulator or physical device.

## 📡 API Integration
The app communicates with the FastAPI backend via these endpoints:
- POST /register – User registration
- POST /login – JWT login
- GET /me – Fetch current user info
- CRUD /devices – Manage pillbox devices
- CRUD /schedules – Manage medication schedules
- CRUD /medlogs – Record adherence logs

## 🛠 Development Notes
- JWT tokens are stored locally (DataStore/SharedPreferences).
- Notifications use a fixed ID to avoid duplicates.
- Repository layer abstracts API calls for clean ViewModel usage.
- Example UI flow: Login → Device List → Add Schedule → Record Medlog.

## 📜 License
This project is currently unlicensed. Please contact the repository owner before reuse or distribution.

## 👤 Author
Developed by fuuyuki.
