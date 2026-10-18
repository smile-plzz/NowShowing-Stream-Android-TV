> Historical planning/reference document. Current implemented behavior is specified in [README](../README.md), [Architecture](ARCHITECTURE.md) and [Web playback](WEB_PLAYBACK.md). Native-only, Room, automatic iframe completion and demo-as-movie claims below are superseded. Candidate device checks are in TV_ACCEPTANCE.md.

> **October 17, 2026 playback update:** See [WEB_PLAYBACK.md](WEB_PLAYBACK.md) for the current dual-engine playback contract. Earlier native-only / zero-WebView requirements below are historical and superseded. Real-provider TV testing remains required.

# NowShowing TV — Handoff to Claude: Design Polish, Functionality Audit & Release Stabilization

**Target Platform:** Android TV / Google TV (API 26 to 35)  
**Primary Repository:** [smile-plzz/NowShowing-Stream-Android-TV](https://github.com/smile-plzz/NowShowing-Stream-Android-TV)  
**Secondary/Origin Repository:** [smile-plzz/NowShowing-Stream](https://github.com/smile-plzz/NowShowing-Stream)  
**Active Release Branch:** `feature/native-only-playback` (tracking `target/main`)  
**Current Commit:** `d3903a9`  
**Current Build Status:** All 33 automated unit tests pass (100%); `assembleDebug` and signed `assembleRelease` compile successfully.

---

## 1. Context & Mission

NowShowing TV is a standalone, TV-native streaming and discovery application built for Android TV and Google TV using **Jetpack Compose for TV**, **AndroidX Media3 (ExoPlayer)**, **Room Database**, and **Retrofit/OkHttp**.

The core architecture, 29-mood recommendation engine, local persistence, network privacy/redirect isolation, and mandatory native-only playback pipeline are fully implemented.

### Your Objectives:
1. **Design Polish & Cinematic Refinement**: Elevate the 10-foot user experience to rival Apple TV and Netflix TV interfaces — smooth focus transitions, elegant typography, shimmer placeholders, and cohesive dark-theme design tokens.
2. **Functionality Audit & Edge Case Resolution**: Audit remote navigation, media lifecycle, audio focus handling, subtitle selection, network disconnects, and TV series transitions.
3. **Performance Profiling & Release Stabilization**: Optimize memory footprints for constrained TV chipsets (1.5GB–2GB RAM), verify ProGuard/R8 rules, and prepare the definitive release candidate.

---

## 2. Codebase Quick Orientation

All Android TV source code resides at the root of the repository:

```
app/src/main/java/com/nowshowing/tv/
├── data/
│   ├── api/                   # Retrofit client & OMDb API service
│   ├── cache/                 # In-memory and TTL caching layers
│   ├── db/                    # Room SQLite database (Watchlist, Continue, Search, Metadata)
│   ├── model/                 # Core data entities (WatchlistItem, ContinueItem, GenreRowSeed)
│   └── repository/            # MediaRepository bridging remote OMDb and Room DB
├── domain/
│   ├── mood/                  # 29-mood precision recommendation engine (genre matching + ranking)
│   └── source/                # Video source registry (HLS/MP4 streams vs legacy web embeds)
├── playback/
│   ├── PlayableSource.kt      # Playable media representation & resolution result models
│   ├── SourceValidation.kt    # Protocol validation, HTTPS enforcement, redirect depth capping (<=5)
│   ├── MediaSourceResolver.kt # Source resolver: extracts direct streams, safely rejects web embeds
│   └── PlaybackCoordinator.kt # Fallbacks, episode sequencing (SxE), seek clamping, auto-next prompt
├── privacy/
│   ├── ContentBlockEngine.kt  # Pure-Kotlin ABP rule matcher with 1,000-entry LRU decision cache
│   ├── FilterListManager.kt   # Asset filter loading (EasyList/EasyPrivacy) & HTTPS updates
│   ├── ProtectionPreferences.kt # SharedPreferences toggles for ad/tracker/redirect rules
│   └── BlockingStatistics.kt  # Local atomic counters for threats, trackers, and redirects
└── ui/
    ├── common/                # TvFocusableCard, TvTopBar, TvButton, ShimmerPlaceholders
    ├── details/               # Title details, backdrop art, season/episode list, recs row
    ├── home/                  # Hero banner, Continue Watching, Trending, Genre carousels
    ├── mood/                  # 29-mood palette grid, "Surprise Me" generator, ranked results
    ├── player/                # Native Media3 PlayerScreen & PlayerViewModel (100% native HUD)
    ├── search/                # 10-foot on-screen QWERTY keyboard, history, category chips
    ├── settings/              # TV Settings screen: privacy toggles, live counters, list updater
    └── theme/                 # TV Material3 color palette, typography tokens, elevation
```

### Key Architectural Rules Already Enforced:
1. **Mandatory Native Playback**: Playback must **never** render inside an embedded WebView or iframe. All media is played through `androidx.media3.exoplayer.ExoPlayer`.
2. **No Browser Pop-ups or Redirects**: Web embeds are classified as `UNSUPPORTED_WEB_EMBED` and rejected by `MediaSourceResolver` before any network request occurs.
3. **Zero Remote Telemetry**: All blocking metrics and continue-watching positions are persisted locally.
4. **Leanback & D-pad First**: Every single interactive element must have explicit focus handling (`onFocusChanged`), visible border/scale feedback, and predictable D-pad directional traversal.

---

## 3. High-Priority Workstreams for Claude

### Workstream 1: Design Polish & Visual Refinement

#### A. Focus States & Spatial Navigation Feel
- **Current State**: Focus uses a basic 2dp cyan border and `1.05x` scale.
- **Goal**:
  - Add a soft radial elevation glow / drop shadow beneath focused cards (using `Modifier.shadow` or custom canvas drawing) to separate focused items from dark backgrounds.
  - Tune focus scale animation with `tween(durationMillis = 150, easing = FastOutSlowInEasing)`.
  - Prevent focus clipping when cards at carousel edges scale up. Check `LazyRow` content paddings (`padding(horizontal = 48.dp)`) and ensure `clipChildren = false`.

#### B. Hero Banner & Backdrop Transitions
- **File**: `android-tv/app/src/main/java/com/nowshowing/tv/ui/home/HomeScreen.kt` & `DetailsScreen.kt`
- **Goal**:
  - Implement smooth crossfades between hero backdrop images as the user scrolls between carousel items or changes selection.
  - Enhance vertical and horizontal gradient scrimps: a multi-stop gradient (pure black `#0D0E12` at the bottom and sides) to ensure 100% legible white text over bright movie backdrops.
  - Add shimmer skeleton loading states for movie posters while Coil fetches images from the network.

#### C. Native Player HUD Polish
- **File**: `android-tv/app/src/main/java/com/nowshowing/tv/ui/player/PlayerScreen.kt`
- **Goal**:
  - **Scrubber Timeline**: Replace the basic linear progress bar with a refined TV scrubber slider that displays buffered percentage in semi-transparent white, elapsed position in cyan, and remaining duration.
  - **Seek Feedback Pill**: Ensure the seek pill (`⏪ -10s`, `⏩ +10s`) uses a subtle frosted glass effect (`background(Color.Black.copy(alpha = 0.7f))` with rounded corners) and fades smoothly with `AnimatedVisibility`.
  - **Source Drawer**: Refine the side/modal drawer when switching sources: show clear provider latency, resolution badge (`1080p`, `4K`, `720p`), format badge (`HLS`, `MP4`), and direct selection.
  - **Auto-Next Episode Banner**: Position the 30-second series prompt elegantly in the bottom-right corner with a circular countdown ring or progress bar showing the remaining seconds before auto-advancing.

#### D. TV Settings Screen Polish
- **File**: `android-tv/app/src/main/java/com/nowshowing/tv/ui/settings/SettingsScreen.kt`
- **Goal**:
  - Group settings into distinct visual cards (e.g., *Network Privacy*, *Playback Preferences*, *Diagnostics & Storage*).
  - Use custom TV toggle switches that clearly indicate ON (Cyan glow) vs OFF (Muted Charcoal) from 10 feet away.
  - Add an "About / Version Info" card showing app version, build type, and active filter list date.

---

### Workstream 2: Functionality Audit & Bug Fixes

#### A. Audio Focus & TV Hardware Lifecycle
- **Files**: `android-tv/app/src/main/java/com/nowshowing/tv/ui/player/PlayerViewModel.kt` & `MainActivity.kt`
- **Audit Points**:
  - **Audio Focus**: Ensure `ExoPlayer.setAudioAttributes(AudioAttributes, handleAudioFocus = true)` is properly configured with `USAGE_MEDIA` and `CONTENT_TYPE_MOVIE`.
  - **TV Standby / HDMI-CEC Power Off**: When the user turns off the TV or switches input, Android TV sends `onPause()` or `onStop()`. Ensure ExoPlayer immediately pauses playback and persists the current position to Room database without holding wakelocks or continuing audio playback in the background.
  - **Remote Media Keys**: Audit handling of `KEYCODE_MEDIA_PLAY`, `KEYCODE_MEDIA_PAUSE`, `KEYCODE_MEDIA_STOP`, `KEYCODE_MEDIA_FAST_FORWARD`, `KEYCODE_MEDIA_REWIND`, and `KEYCODE_MEDIA_PLAY_PAUSE`.

#### B. Subtitle & Multi-Audio Track Selection
- **Files**: `PlayerScreen.kt` & `PlayerViewModel.kt`
- **Audit Points**:
  - For HLS and DASH streams containing WebVTT / CEA-608 subtitles or multi-language audio tracks, add a "Subtitles & Audio" dialogue/drawer accessible from the player HUD.
  - Support track selection via `TrackSelectionParameters` on ExoPlayer (`setPreferredTextLanguage`, etc.).

#### C. Network Disconnect & Error Recovery
- **Files**: `MediaSourceResolver.kt`, `PlayerViewModel.kt`, `MediaRepository.kt`
- **Audit Points**:
  - If the TV loses Wi-Fi or Ethernet during playback, ExoPlayer fires `Player.EVENT_PLAYER_ERROR` with `PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED`.
  - Verify that a clean, remote-friendly retry card appears (*"Network connection lost. Check Wi-Fi."* with `[Retry]` and `[Back to Details]` buttons).
  - If a stream encounters a 404 or corrupted manifest, automatically invoke `PlaybackCoordinator.resolveFallbackSource()` to attempt the next configured provider seamlessly.

#### D. TV Series Episode Progression Edge Cases
- **File**: `PlaybackCoordinator.kt`
- **Audit Points**:
  - Season roll-over: If the user finishes Season 1 Episode 10, verify whether `getNextEpisode` correctly navigates to Season 2 Episode 1 if metadata exists.
  - If the final episode of a series finishes, ensure playback returns cleanly to the Title Details screen with a completion toast instead of looping or crashing.

---

### Workstream 3: Performance Profiling & Release Stabilization

#### A. Memory & Bitmaps on Low-End Hardware
- **Context**: Budget Android TV sticks (e.g. Chromecast HD, Xiaomi TV Stick) have only 1.5GB of total RAM, with low per-app memory limits (~192MB-256MB).
- **Audit Points**:
  - Verify Coil image loading uses downsampling: `size(width = 300, height = 450)` for poster cards and `size(1280, 720)` for backdrops, rather than loading raw 4K posters into memory.
  - Ensure memory cache in Coil has a reasonable max size (e.g., 25% of app memory).
  - Confirm `ExoPlayer.release()` is called in `onCleared()` of `PlayerViewModel` and inside `DisposableEffect` when leaving `PlayerScreen`.

#### B. ProGuard / R8 Rules Verification
- **File**: `android-tv/app/proguard-rules.pro`
- **Audit Points**:
  - The current release build uses R8 (`isMinifyEnabled = true` with fallback).
  - Verify that Room SQLite entities (`WatchlistEntity`, `ContinueEntity`, etc.), Retrofit/Gson DTOs, and Media3 internal renderers are preserved and not obfuscated into crashing states.
  - Inspect `android-tv/app/proguard-rules.pro` and add explicit keep rules if missing:
    ```pro
    -keepclassmembers class * {
        @androidx.room.* *;
    }
    -keep class com.nowshowing.tv.data.model.** { *; }
    -keep class androidx.media3.exoplayer.** { *; }
    ```

#### C. Verification Suite Execution
Always verify changes with the complete test and build pipeline:
```bash
# 1. Run unit test suite
./gradlew testDebugUnitTest

# 2. Build debug APK
./gradlew assembleDebug

# 3. Build signed release APK
./gradlew assembleRelease
```

---

## 4. Current Automated Test Matrix (Reference Baseline)

| Test Class | Path | Test Count | Status |
| :--- | :--- | :--- | :--- |
| `DataModelsTest` | `app/src/test/java/com/nowshowing/tv/data/DataModelsTest.kt` | 3 | Passing |
| `MoodRecommendationEngineTest` | `app/src/test/java/com/nowshowing/tv/domain/mood/` | 3 | Passing |
| `SourceRegistryTest` | `app/src/test/java/com/nowshowing/tv/domain/source/` | 6 | Passing |
| `MediaSourceResolverTest` | `app/src/test/java/com/nowshowing/tv/playback/` | 5 | Passing |
| `PlaybackCoordinatorTest` | `app/src/test/java/com/nowshowing/tv/playback/` | 5 | Passing |
| `SourceValidationTest` | `app/src/test/java/com/nowshowing/tv/playback/` | 4 | Passing |
| `ContentBlockEngineTest` | `app/src/test/java/com/nowshowing/tv/privacy/` | 7 | Passing |
| **Total** | **All suites** | **33** | **100% Pass** |

---

## 5. Helpful Commands & Development Workflow

### Git & Remote Sync
```bash
# Verify current branch and remotes
git status
git remote -v

# Push to primary Android TV target repository
git push target feature/native-only-playback:main
git push target feature/native-only-playback:feature/native-only-playback
```

### Hardware Deployment
```bash
# Connect to physical TV
adb connect <TV_IP>:5555

# Install signed release APK
adb install -r app/build/outputs/apk/release/app-release.apk

# Launch App
adb shell am start -n com.nowshowing.tv/.ui.MainActivity

# Stream TV logcat
adb logcat -s NowShowingTV:V ExoPlayer:V ContentBlockEngine:V
```
