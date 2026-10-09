# BrainRot 📱

> See how much you scroll. Take back control with playful gamified tracking, brain mascot states, app breakdown, and friend battles.

BrainRot has been rewritten as a modern, high-performance **React Native-ready** application on **Vite + Tailwind CSS + Capacitor**, preserving all core business logic and features from the original Android app, now with full **mobile app builds** for Android (and PWA).

![BrainRot Icon](./public/icon-192.png)

## 📱 Mobile App Build - NEW!

BrainRot now builds as a **native Android APK** via Capacitor 8!

### Quick Build

```bash
# 1. Install deps
npm ci

# 2. Build mobile (web + sync to native)
npm run build:mobile
# or
./scripts/build-mobile.sh

# 3. Build APK (requires JDK 17 + Android SDK)
npm run android:build
# Output: android/app/build/outputs/apk/debug/app-debug.apk
#         brainrot-debug.apk (convenience copy)

# 4. Run on device/emulator
npm run android:run
# or with live reload
npm run mobile:dev
```

**What you get:**
- ✅ Web build: `dist/` (PWA, optimized)
- ✅ Android project: `android/` (Gradle, ready for Android Studio)
- ✅ Debug APK: `brainrot-debug.apk` (when SDK available)
- ✅ PWA manifest + icons (72-1024px)
- ✅ Native plugins: Haptics, StatusBar, SplashScreen, Keyboard, App

See [MOBILE_BUILD.md](./MOBILE_BUILD.md) for full guide and CI workflow.

### GitHub Actions

Automated builds on every push:
- 🌐 Web build + lint
- 🤖 Android debug APK
- 🔧 PWA audit

Artifacts uploaded for 7-14 days.

## Features

- **Brain Mascot (`ScrollyCharacter`)**: 6 dynamic expressive states based on daily scrolling:
  - *Super Fresh* (0–20 reels): clear crystal mind, sparkling anime eyes
  - *Cruising* (21–50 reels): chill, mindful
  - *Dazed* (51–100 reels): wide-eyed, gentle reminder
  - *Brain Fried* (101–200 reels): dizzy spiral eyes, sweat drop
  - *Completely Cooked* (201–500 reels): X eyes, open tongue mouth
  - *Nuclear Brainrot* (501+ reels): emergency dopamine overload
- **Interactive Dynamic Island Overlay (`NotchBarPreview`)**:
  - Live floating capsule at top of display
  - Real-time scroll counter with bounce animation and status pulse
  - Expandable into full dynamic card with progress bar and remaining scroll allowances
- **Hardware Notch Finder & Auto-Adjuster**:
  - Notch profile configurations (iPhone Dynamic Island, Center Punch-Hole, Wide Notch, Waterdrop, Corner Punch-Holes, Below Status Bar)
  - Placement modes (*Wrap Around Notch* vs *Below Notch*)
  - Gap width and X/Y offset fine-tuning
- **Active Apps Tracking**:
  - Live breakdown across Instagram, YouTube Shorts, TikTok, Spotify, and Facebook
  - Real-time simulation tester (+1, +5, +20)
- **Daily App Limits & Interceptor (`BlockScreen`)**:
  - Custom scroll limit sliders (10 to 200 reels/day)
  - Full-screen intervention overlay when daily limits are exceeded
  - Earn extra scrolls by completing healthy offline resets (Touch Grass Walk, Box Breathing, Phone-Down Reset)
- **Friend Battles (`BattlesScreen`)**:
  - Head-to-head scroll duels (lower scroll count wins!)
  - Live duel card with status banner (Winning, Behind, Tied)
  - Challenge friends and send mindful nudges
  - Daily attention leaderboard
- **Analytics & History (`StatsScreen`)**:
  - Day, Week, Month, and Year timeframe breakdown
  - Quarter-hour daily segments, day-by-day weekly comparison
  - App distribution share and mindful insights
- **Profile & Gamification (`ProfileScreen`)**:
  - Multi-day streak tracking
  - All-time scroll counts and battle win rate
  - Achievements gallery with unlock criteria and progress bars
- **📱 Native Mobile Enhancements**:
  - Haptic feedback on scrolls, tab changes, achievements
  - StatusBar theming (#FEF7FF light)
  - SplashScreen with Scrolly brain mascot
  - Safe area insets for notches & gesture nav
  - Edge-to-edge on Android 15+
  - PWA with shortcuts (Stats, Battles, Block)
  - Offline-ready web assets

## Tech Stack

- **Web**: React 19 + TypeScript + Vite 6 + Tailwind CSS 4
- **Mobile**: Capacitor 8 (Android, PWA)
- **Native Plugins**: @capacitor/app, haptics, keyboard, status-bar, splash-screen
- **UI**: Lucide React & Canvas Confetti
- **Build**: Gradle 8 + Android SDK 36

## Project Structure

```
src/
├── components/          # Navigation, Mascot, Notch preview
├── screens/             # Home, Stats, Battles, Block, Profile
├── context/             # BrainRotContext (state + haptics)
├── utils/               # capacitor.ts, dateKeys.ts
├── types/               # TypeScript types
├── App.tsx              # Main with safe areas & deep links
├── main.tsx             # Native init + PWA
└── index.css            # Mobile-first styles

android/                 # Capacitor Android project
├── app/src/main/
│   ├── AndroidManifest.xml  # Permissions + deep links
│   ├── java/.../MainActivity.java  # Edge-to-edge + WebView perf
│   └── res/             # Icons (mipmap), splash, colors, styles
public/
├── manifest.json        # PWA manifest
├── icon-*.png           # 48-1024px icons
└── ...

capacitor.config.ts      # App ID com.brainrot.app
build.gradle.kts         # Root: web + APK orchestrator
```

## Development

```bash
# Web dev
npm run dev
# http://localhost:3000

# Mobile dev with live reload (device needed)
npm run mobile:dev

# Build web only
npm run build

# Build mobile web + sync
npm run build:mobile

# Android APK (requires Android SDK)
npm run android:build

# Open in Android Studio
npm run cap:open:android

# Lint
npm run lint
```

## Permissions (Android)

- `INTERNET`, `ACCESS_NETWORK_STATE` - WebView
- `SYSTEM_ALERT_WINDOW` - Floating Dynamic Island overlay
- `VIBRATE` - Haptics
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Background reliability

## PWA

Installable on Android/iOS/desktop:
- Standalone display
- Theme #6750A4, background #FEF7FF
- Icons maskable 192 & 512
- Shortcuts: Stats, Battles, Block
- Apple touch icon, safe area viewport-fit=cover

## License

MIT - Take back your attention! 🧠💜
