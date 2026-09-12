# Environment Setup & Build Instructions

- **Build System**: Gradle with Version Catalogs (`gradle/libs.versions.toml`)
- **Android Gradle Plugin (AGP)**: `8.2.2`
- **Kotlin Version**: `1.9.22`
- **Minimum SDK**: `26` (Android 8.0 Oreo)
- **Target / Compile SDK**: `34` (Android 14)

---

## 1. Prerequisites

Before building the project, ensure your workstation has:
1. **JDK 17**: Oracle JDK, Eclipse Temurin 17, or Android Studio JBR.
2. **Android Studio**: Iguana (2023.2.1) or Hedgehog (2023.1.1).
3. **Android SDK Packages**:
   - Android SDK Platform 34
   - Android SDK Build-Tools 34.0.0
   - Android SDK Command-line Tools (latest)

Verify Java version in your shell:
```bash
java -version
# Expected: openjdk version "17.0.x"
```

---

## 2. Key Dependencies & Version Catalog

All dependency coordinates are declared in [`gradle/libs.versions.toml`](file:///d:/projects/mobile_projects/Notes/notes_app_native_android/gradle/libs.versions.toml#L1-L40):

| Library Group | Artifact | Version | Purpose |
|---|---|---|---|
| **AndroidX Graphics** | `androidx.graphics:graphics-core` | `1.0.0-rc01` | Front-buffered low latency rendering |
| **Motion Prediction** | `androidx.input:input-motionprediction` | `1.0.0-beta04` | Stylus trajectory prediction |
| **Jetpack Compose** | Compose BOM | `2023.10.01` | Material 3 Declarative UI |
| **Dependency Injection**| `com.google.dagger:hilt-android` | `2.50` | Compile-time dependency injection |
| **Local Database** | `androidx.room:room-runtime` | `2.6.1` | SQLite ORM |
| **ML Kit Scanner** | `play-services-mlkit-document-scanner` | `16.0.0-beta1` | Camera edge & perspective correction |
| **ML Kit Digital Ink** | `com.google.mlkit:digital-ink-recognition`| `18.1.0` | Geometric shape recognition |

---

## 3. CLI Build & Verification Commands

```powershell
# Clean build cache
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Run unit tests across modules
./gradlew testDebugUnitTest

# Generate release bundle (requires keystore signing config)
./gradlew bundleRelease
```
