# NowShowing TV

A dedicated, TV-native movie and television discovery and streaming application designed specifically for **Android TV** and **Google TV** (API 26 to 35).

Built with **Jetpack Compose for TV**, **AndroidX Media3 (ExoPlayer)**, **Room Database**, and **Retrofit/OkHttp**.

See [current playback behavior and TV test steps](docs/WEB_PLAYBACK.md). Provider availability is external; an accessible web page does not guarantee playback. Web-player progress and subtitles are managed by the provider.

---

## Features

- **TV polish and local history**: Progressive home shelves, shared bounded metadata caching, remembered browsing position/focus, and a Recently opened shelf with per-title source/episode memory and manual history controls. See [polish and history notes](docs/POLISH_AND_HISTORY.md).

- **Cinematic 10-Foot UI**: Remote-controlled interface optimized for standard D-pad television remotes, Google TV voice remotes, and HDMI-CEC controllers.
- **29-Mood Recommendation Engine**: Precision recommendation system ported from the web client. Explores multi-genre candidate seeds, verifies real genre tags against cached OMDb lookups, and ranks candidates using composite score heuristics.
- **Dual Playback Engines**: Existing movie and series embed providers open in an embedded WebView with D-pad pointer controls. Direct HLS, DASH, and MP4 streams retain Media3. Demo streams are diagnostics, not the selected movie.
- **Media Source Resolution & Validation**: Dedicated resolver layer that identifies direct playable streams, validates HTTPS transport, isolates redirect chains (max 5 hops), and keeps web-only embeds out of the native player.
- **Native Brave-Inspired Privacy Shields**: Network request filtering against Adblock Plus and EasyPrivacy rule syntax with a thread-safe 1,000-entry LRU decision cache , live security metrics, and a dedicated TV settings screen.
- **Local Persistence**: Watchlist, Continue Watching (with genuine playback progress tracking), Search History, and OMDb request caching stored securely in SQLite via Room.
- **Launcher Integration**: Declares `android.intent.category.LEANBACK_LAUNCHER` with a 320x180 TV banner (`@drawable/banner`) for seamless discovery on Google TV, standard Android TV, and the Nook TV launcher.

---

## Project Structure

```
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml       # Leanback launcher declaration & permissions
│   │   ├── assets/filters/           # Bundled ABP ad and tracker blocklists
│   │   ├── java/com/nowshowing/tv/
│   │   │   ├── data/                 # Room database, OMDb Retrofit client, repository
│   │   │   ├── domain/               # 29-mood engine, video source registry
│   │   │   ├── playback/             # MediaSourceResolver, SourceValidation, PlaybackCoordinator
│   │   │   ├── privacy/              # ContentBlockEngine, FilterListManager, BlockingStatistics
│   │   │   └── ui/                   # Jetpack Compose for TV screens (Home, Browse, Mood, Player, Search, Settings)
│   │   └── res/                      # TV banner, launcher icons, theme colors, strings
│   ├── src/test/                     # JVM regression tests
│   ├── build.gradle.kts              # App dependencies, Compose TV, Media3, signing config
│   └── proguard-rules.pro            # R8 obfuscation & keep rules for Room and Media3
├── docs/                             # Complete architecture & engineering documentation
│   ├── ANDROID_TV_PRD.md             # Product requirements & TV ergonomics
│   ├── ARCHITECTURE.md               # Clean architecture & subsystem boundaries
│   ├── REMOTE_NAVIGATION.md          # Remote key matrix & spatial focus guide
│   ├── PRIVACY_AND_BLOCKING.md       # Network isolation & redirect guard specification
│   ├── PLAYBACK_COMPATIBILITY.md     # 4-tier provider audit matrix
│   ├── QA_TEST_PLAN.md               # Test matrix & automated test baseline
│   ├── INSTALLATION.md               # Sideloading via USB, ADB, and Nook TV
│   ├── RELEASE_CHECKLIST.md          # Distribution verification checklist
│   └── CLAUDE_HANDOFF.md             # Detailed engineering handoff guide
├── build.gradle.kts                  # Root build script
├── settings.gradle.kts               # Root settings (NowShowingTV)
├── gradle.properties                 # JVM heap & AndroidX settings
├── gradlew / gradlew.bat             # Gradle wrapper
└── README.md
```

---

## Building and Testing

### Prerequisites
- JDK 17 or JDK 21
- Android SDK (API 26 minimum, API 35 target)

### Commands
```bash
# Run unit test suite (existing tests and web-navigation policy tests)
./gradlew testDebugUnitTest

# Assemble Debug APK (development & testing)
./gradlew assembleDebug
# Generated at: app/build/outputs/apk/debug/app-debug.apk

# Assemble Signed Release APK (production & sideloading)
./gradlew assembleRelease
# Generated at: app/build/outputs/apk/release/app-release.apk
```

---

## Installation & Sideloading

### Method A: Wireless ADB (Recommended)
```bash
# 1. Connect to Android TV via IP
adb connect <TV_IP_ADDRESS>:5555

# 2. Sideload the release APK
adb install -r app/build/outputs/apk/release/app-release.apk

# 3. Launch application directly
adb shell am start -n com.nowshowing.tv/.ui.MainActivity
```

### Method B: USB Flash Drive
1. Copy `app-release.apk` to a FAT32 or NTFS formatted USB flash drive.
2. Insert into television / streaming box USB port.
3. Open any TV file manager (*File Commander*, *X-plore*), enable *Install unknown apps*, and install.

---

## Data Source & API Keys

Uses the OMDb API for movie and series metadata. A public demo key is included for immediate testing. For production deployments with higher request limits, supply your own key in `ApiClient.kt` or via local configuration.

---

## License & Attribution

Built by Ismail Hossain.  
Target repository: [smile-plzz/NowShowing-Stream-Android-TV](https://github.com/smile-plzz/NowShowing-Stream-Android-TV)
