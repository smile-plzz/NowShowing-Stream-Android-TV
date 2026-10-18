> Historical planning/reference document. Current implemented behavior is specified in [README](README.md), [Architecture](docs/ARCHITECTURE.md) and [Web playback](docs/WEB_PLAYBACK.md). Native-only, Room, automatic iframe completion and demo-as-movie claims below are superseded. Candidate device checks are in TV_ACCEPTANCE.md.

# Project Context: NowShowing TV

## 1. Project Goals
- Develop a standalone, TV-native version of NowShowing for Android TV / Google TV.
- Deliver an installable, native Android APK (.apk).
- Provide a cinematic, remote-controlled 10-foot movie and television discovery and viewing experience.
- Maintain independent installation while allowing discovery by Android TV launchers (including Nook TV).
- Port the complete feature set from the web repository (`NowShowing-Stream`):
  - Catalog browsing (Trending, Popular Movies, Popular TV, Genre rows)
  - 29-mood precision recommendation engine with genre verification & ranking
  - Search with on-screen keyboard & type filtering
  - Detailed metadata (plot, cast, director, rating, runtime, genre tags)
  - Season & episode picker for TV shows
  - Exclusive Native Media3 Playback: Standards-compliant HLS/DASH/MP4 streams with full 10-foot remote controls, timeline scrubber, and series sequencing
  - Local persistence for Watchlist, Continue Watching (with genuine playback progress), preferred source, and cached metadata
  - Dedicated Media Source Resolution & Redirect Isolation: Evaluates providers, safely rejects unsupported web embeds, enforces HTTPS, and limits redirect chains
  - Native Brave-inspired Privacy Shields: Built-in ad & tracker blocking, pop-up suppression, and local statistics

## 2. Technology Stack
- **Language**: Kotlin 2.0+
- **Platform**: Android TV / Google TV (Min SDK 26 / Android 8.0 Oreo, Target SDK 35 / Android 15)
- **UI Framework**: Android TV UI with Jetpack Compose for TV & TV Material3 / Leanback architecture
- **Media Playback**: AndroidX Media3 (ExoPlayer 1.4+) exclusively; zero WebViews in playback
- **Resolution & Security**: `MediaSourceResolver`, `SourceValidation`, protocol enforcement (HTTPS + private network allowance), redirect depth validation
- **Privacy & Content Blocking**: Native network interception, Adblock Plus rule matcher, compact bundled filter lists, pop-up & redirect interceptors, local statistics
- **Networking & Serialization**: OkHttp 4 / Retrofit 2 + Kotlinx Serialization / Gson
- **Image Loading**: Coil 2.x with aggressive bitmap downsampling and memory/disk cache
- **Persistence**: Room Database (SQLite) / SharedPreferences for Watchlist, Continue Watching, search history, and metadata cache
- **Build System**: Gradle 8.x with Android Gradle Plugin (AGP) 8.x

## 3. Constraints & Principles
- **10-foot remote navigation**: D-pad only (Up, Down, Left, Right, Center/OK, Back, Play/Pause). Zero pointer or touch dependency.
- **Predictable focus**: Explicit focus management, visible focus indicators, focus restoration on back navigation, horizontal scroll position retention.
- **Performance on low-end TV hardware**: Lazy loading of carousels/grids, memory-bounded image caches, no heavy background autoplay. Sub-millisecond rule evaluation.
- **Mandatory Native Playback**: All playable media must open directly in the native TV player. No WebView inside playback. If a provider only exposes a web embed, it is classified as incompatible with clear user guidance to select a native stream.
- **Legitimate media protection**: Never block video fragments (.m3u8, .ts, .mpd, .m4s, .mp4), subtitles, or authorized playback scripts.
- **Source of truth**: Web codebase in root; Android project located in `android-tv/` without breaking the web app.
