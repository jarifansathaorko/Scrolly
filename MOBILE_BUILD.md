# 📱 BrainRot Mobile App - Build Guide

BrainRot is now a full native mobile app built with **Capacitor 8** wrapping a high-performance React + Vite web app.

## 🏗️ Architecture

```
BrainRot/
├── src/                    # React web app (Vite + Tailwind)
├── dist/                   # Built web assets (generated)
├── android/                # Native Android project (Capacitor)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/brainrot/app/MainActivity.java
│   │   │   └── res/        # Icons, splash, colors
│   │   └── build.gradle
│   └── gradle/wrapper/
├── public/
│   ├── manifest.json       # PWA manifest
│   ├── icon-*.png          # App icons
│   └── ...
├── capacitor.config.ts     # Capacitor config
└── build.gradle.kts        # Root orchestrator (web + APK)
```

## 🚀 Quick Start

### Prerequisites
- Node.js 18+ and npm
- Android Studio (for Android builds)
- Java 17+ (for Gradle)
- Android SDK (via Android Studio)

### 1. Install & Build Web

```bash
npm ci
npm run build
```

### 2. Sync to Native

```bash
npx cap sync
# or
npm run cap:sync
```

### 3. Build Android APK

#### Via npm (recommended for dev):
```bash
npm run android:build
# Output: android/app/build/outputs/apk/debug/app-debug.apk
# Also copied to: ./brainrot-debug.apk
```

#### Via Gradle (root):
```bash
./gradlew assembleDebug          # Linux/Mac
gradlew.bat assembleDebug        # Windows

# Or using npm wrapper script:
npm run build
cd android && ./gradlew assembleDebug
```

#### Via Android Studio:
```bash
npx cap open android
# Then Build > Build APK(s) in Android Studio
```

### 4. Run on Device/Emulator

```bash
npm run android:run
# or
npx cap run android
# or with live reload:
npm run mobile:dev
```

## 📦 Build Outputs

- **Web**: `dist/` - Optimized Vite build (JS, CSS, HTML)
- **Android Debug APK**: `android/app/build/outputs/apk/debug/app-debug.apk`
- **Convenience copy**: `brainrot-debug.apk` in project root
- **Release APK**: `android/app/build/outputs/apk/release/app-release-unsigned.apk`

## 🔧 Capacitor Configuration

`capacitor.config.ts`:
- **appId**: `com.brainrot.app`
- **appName**: `BrainRot`
- **webDir**: `dist`
- **Plugins**: SplashScreen, StatusBar, Haptics, Keyboard, App

## 🎨 App Icons & Splash

Icons generated from `public/icon-512.png`:
- 48, 72, 96, 144, 192, 512, 1024 px variants
- Android mipmap: mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi
- Adaptive icons via `mipmap-anydpi-v26/ic_launcher.xml`
- Splash: `res/drawable/splash.png` + `splash_icon.xml`

To regenerate:
```bash
python3 -c "
from PIL import Image
im=Image.open('public/icon-512.png')
# ... resize logic in build scripts
"
```

## 🔐 Permissions (AndroidManifest.xml)

- `INTERNET` & `ACCESS_NETWORK_STATE` - WebView & API
- `SYSTEM_ALERT_WINDOW` - Floating Dynamic Island overlay
- `VIBRATE` - Haptic feedback
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Background service reliability
- `WAKE_LOCK` - Screen awareness

## 📱 Native Features

### Implemented:
- ✅ StatusBar customization (#FEF7FF light theme)
- ✅ SplashScreen with BrainRot branding
- ✅ Haptics on interactions (light/medium/heavy)
- ✅ Edge-to-edge display (Android 15+)
- ✅ Safe area insets (notch, gesture nav)
- ✅ Back button handling
- ✅ Keyboard handling
- ✅ Deep links (com.brainrot.app://, https://brainrot.app)
- ✅ PWA shortcuts (Stats, Battles, Block)

### Future (requires custom native plugin):
- Accessibility Service for real scroll detection
- Overlay Service for true floating Dynamic Island
- Background service for continuous tracking

## 🌐 PWA Features

- `manifest.json` with icons, shortcuts, screenshots
- Standalone display mode
- Theme color #6750A4
- Apple touch icon & meta tags
- Safe area viewport-fit=cover
- Service Worker ready (placeholder)

## 🛠️ Development Workflow

```bash
# 1. Dev web with hot reload
npm run dev
# Open http://localhost:3000

# 2. Dev mobile with live reload (requires device)
npm run mobile:dev

# 3. Build & test APK
npm run android:build
adb install brainrot-debug.apk
# or
adb install android/app/build/outputs/apk/debug/app-debug.apk

# 4. Logs
adb logcat | grep -i brainrot
# or in Android Studio: Logcat
```

## 📏 Build Optimization

- Vite chunks: vendor (react) + capacitor split
- ProGuard enabled for release (minify + shrinkResources)
- Java 17 target
- Hardware acceleration in WebView
- Over-scroll disabled for native feel

## 🚢 Release Process

1. Update version in:
   - `package.json` (version)
   - `android/app/build.gradle` (versionCode, versionName)
   - `capacitor.config.ts` if needed

2. Build release:
```bash
npm run android:build:release
# or
cd android && ./gradlew assembleRelease
```

3. Sign APK (requires keystore):
```bash
# Create keystore if not exists
keytool -genkey -v -keystore brainrot-release.keystore -alias brainrot -keyalg RSA -keysize 2048 -validity 10000

# Sign
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA256 -keystore brainrot-release.keystore android/app/build/outputs/apk/release/app-release-unsigned.apk brainrot

# Zipalign
zipalign -v 4 app-release-unsigned.apk brainrot-release.apk
```

4. Upload to Play Console or distribute

## 🐛 Troubleshooting

**Gradle sync failed:**
```bash
cd android && ./gradlew clean && cd .. && npx cap sync
```

**Web assets not updating:**
```bash
npm run build && npx cap copy android
```

**Icons not showing:**
- Ensure mipmap folders have ic_launcher.png
- Check adaptive icon XML references @color/backgroundLight
- Rebuild: cd android && ./gradlew clean assembleDebug

**Status bar wrong color:**
- Check MainActivity.java sets #FEF7FF
- Verify capacitor.config.ts StatusBar plugin config

**Haptics not working:**
- Only works on native device, not web
- Requires VIBRATE permission (already added)

## 📚 References

- [Capacitor Docs](https://capacitorjs.com/docs)
- [Android Developer](https://developer.android.com)
- [Vite](https://vitejs.dev)

---

Built with 💜 by BrainRot team - Take back your attention!
