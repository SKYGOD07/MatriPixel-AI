# MatriPixel AI 🔬

> **Offline-First Anemia Detection** using Edge AI

A native Android app that detects anemia using smartphone camera images of the eye/nail bed. Built for the **Health in Pixels 2025 Hackathon**.

---

## ✨ Features

| Feature | Status |
|---------|--------|
| 📱 Offline-First | ✅ Works in Airplane Mode |
| 🔒 HIPAA Compliant | ✅ SQLCipher + Keystore |
| 🧠 Edge AI | ✅ LiteRT (TensorFlow Lite) |
| 📸 CameraX | ✅ Real-time analysis |
| 🔄 Federated Sync | ✅ WorkManager + WiFi-only |
| 🎨 Modern UI | ✅ Jetpack Compose |

---

## 🏗️ Architecture

```
MatriPixelAI/
├── data/           # Room DB + Repositories
│   ├── model/      # Patient, DiagnosisScan
│   ├── dao/        # PatientDao, DiagnosisDao
│   ├── db/         # SQLCipher Database
│   └── repository/ # Clean Architecture
├── ml/             # LiteRT Inference
│   ├── AnemiaDetector.kt
│   └── DiagnosisUseCase.kt
├── camera/         # CameraX Integration
│   ├── CameraManager.kt
│   └── ConjunctivaAnalyzer.kt
├── sync/           # WorkManager
│   └── SyncWorker.kt
├── ui/             # Jetpack Compose
│   ├── screens/    # Home, Capture, Result
│   ├── components/ # RiskGauge, Overlay
│   └── theme/      # Material 3
└── di/             # Hilt Modules
```

---

## 🔧 Tech Stack

| Layer | Technology |
|-------|------------|
| **Language** | Kotlin 1.9 |
| **UI** | Jetpack Compose |
| **Camera** | CameraX 1.4 |
| **ML** | LiteRT (TensorFlow Lite 2.14) |
| **Database** | Room + SQLCipher |
| **Sync** | WorkManager |
| **DI** | Hilt |

---

## 📋 Clinical Logic

```
Input: Eye Image + Vitals
  ↓
ROI Extraction (conjunctiva)
  ↓
Pallor Analysis (color)
  ↓
LiteRT Inference
  ↓
Output: Risk Score
  ├── 🔴 RED (≥0.7) → Immediate consultation
  ├── 🟡 AMBER (0.4-0.7) → Schedule blood test
  └── 🟢 GREEN (<0.4) → No concern
```

---

## 🚀 Quick Start

1. **Clone & Open in Android Studio**
   ```bash
   git clone <repo>
   cd MatriPixelAI
   ```

2. **Build**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Run on Device**
   - Connect Android device (API 26+)
   - Enable USB debugging
   - Run from Android Studio

---

## 🔐 Privacy by Design

| What's Synced | What's Never Synced |
|---------------|---------------------|
| ✅ Anonymized risk scores | ❌ Raw images |
| ✅ Feature vectors | ❌ Patient names |
| ✅ Model gradients | ❌ Device identifiers |

---

## 📁 Project Structure

```
MatriPixel AI/
├── app/
│   ├── src/main/
│   │   ├── java/com/matripixel/ai/
│   │   ├── res/
│   │   └── assets/ml/  # TFLite model
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🧪 Testing

```bash
# Unit Tests
./gradlew testDebugUnitTest

# Instrumented Tests
./gradlew connectedDebugAndroidTest
```

---

## 📄 License

MIT License - Health in Pixels 2025

---

**Built with ❤️ by MatriPixel AI Team**
