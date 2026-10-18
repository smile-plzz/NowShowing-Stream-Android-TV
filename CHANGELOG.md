# 1.5.0 — final UI and distribution polish

Scrollable TV navigation; five clear Settings sections; readable diagnostic dialog; explicit personal/demo-key actions; disabled busy controls; Browse/Mood recovery; cancellation of superseded Mood requests; disappearing first-use hints; static AsyncImage artwork states. Adds Compose UI regressions and publication bundle integrity checks. The gated workflow ships versioned APKs with checksums and public certificate/build details. Development signing is explicitly distributed as a prerelease. Web autoplay/progress and physical-TV validation remain limited as documented.

# 1.5.0-rc.2 — daily-use candidate

Metadata recovery/caching, watched-label preservation, safe filter replacement, direct player entry, default Play focus, cached related titles, bounded image/network work, manual episode controls and persisted autoplay preference, About and redacted Diagnostics. Web completion remains unsupported. Tests and release build evidence belong to the final CI run; TV checks remain pending. See docs/releases/v1.5.0-rc.2.md.

# 1.5.0-rc.1 — 2026-10-18

- Refined TV player menu, first-use help, precise/accelerated pointer movement, fullscreen pointer forwarding, and predictable Back navigation.
- Added current episode metadata and manual episode selection; kept provider-managed video controls.
- Moved native diagnostic streams to Settings and added visible app/build versions.
- Included local Recently opened, shared metadata caching, progressive shelves, and navigation-state restoration from the preceding polish pass.
- Added versioned GitHub release packaging, APK signature verification, checksums, build metadata, optional permanent signing, and honest prerelease status.
- Rewrote installation/update instructions and added known limitations, troubleshooting, and release-signing guidance.

---

# Changelog

All notable changes to the NowShowing project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.0] - 2026-10-18

### Added
- **Web Provider Fallback & TV Pointer Controls (PR #1)**:
  - Added `WebEmbedScreen.kt` and `WebEmbedPolicy.kt` enabling TV pointer and D-pad controls for web provider streams.
  - Implemented return focus persistence (`ReturnFocus.kt`) restoring remote focus to provider controls upon exit.
  - Added `WebEmbedPolicyTest.kt` verifying embedding policy and fallback constraints.
  - Added documentation: `docs/WEB_PLAYBACK.md`.
- **Viewing History & TV Browsing Polish (PR #2)**:
  - Added local SQLite viewing history tracking (`RecentItem.kt`, `HistoryManager.kt`).
  - Added offline metadata recovery and warm upgrade cache preservation (`MetadataCache.kt`).
  - Added unit tests: `RecentItemTest.kt` and `MetadataCacheTest.kt`.
  - Added documentation: `docs/POLISH_AND_HISTORY.md`.

## [1.4.0] - 2026-10-17

### Added
- **Subtitles & Multi-Audio Track Selection**:
  - Remote-navigable "Audio & Subs" drawer modal in `PlayerScreen.kt` using ExoPlayer's `TrackSelectionParameters`.
  - D-pad toggles for embedded WebVTT subtitles and multi-language audio track preferences (Auto, English, Original).
- **Network Error Recovery & Fallback**:
  - `onPlayerError` listener in `PlayerScreen.kt` catching network timeout, bad HTTP status, and connection failure exceptions.
  - Cinematic TV Retry Dialog with instant retry, next-source fallback, and exit options.
- **Series Progression & Finale**:
  - Season boundary rollover support in `PlaybackCoordinator.kt` (`getNextEpisode` with `maxEpisodesInSeason`, e.g. S1E10 -> S2E1).
  - TV series/movie completion card overlay upon playback finish.
  - Dedicated unit tests for season rollover and fallback source resolution.
- **Design Polish & Cinematic Refinements**:
  - `TvFocusableCard.kt`: Smooth `FastOutSlowInEasing` focus scale animation (150ms), soft elevation drop shadow with `FocusGlow`, and animated `ShimmerBox` loading skeleton.
  - `HomeScreen.kt`: Multi-stop vertical bottom gradient scrim (`#0D0E12` fade) for 100% typography contrast, `ShimmerHomeScreen` skeleton carousel placeholders, and crossfade backdrops.
  - `DetailsScreen.kt`: Left-edge and bottom gradient scrims ensuring pristine title legibility against high-key artwork, plus crossfade backdrops.
  - `PlayerScreen.kt`: Custom timeline scrubber with buffered percentage segment and played duration, frosted glass seek pill (`⏪ -10s`, `⏩ +10s`), and live countdown timer on 30s auto-next prompt.
  - `SettingsScreen.kt`: 1.2x scaled TV toggle switches for 10-foot visibility.

### Optimized
- **Low-End Hardware Memory & APK Size (Chromecast HD & Xiaomi Mi Box)**:
  - Memory-bounded Coil downsampling on all images: `size(300, 450)` for poster cards and `size(1280, 720)` for hero backdrops.
  - Activated R8 code shrinking and resource optimization (`isMinifyEnabled = true`, `isShrinkResources = true`).
  - Hardened ProGuard rules preserving Room SQLite, Media3 ExoPlayer renderers, Retrofit, OkHttp, and Gson.
  - Release APK size reduced by 34% (from 15.8 MB down to **9.95 MB**).
- **Hardware Remote & Standby Lifecycle**:
  - Audio focus configured via `AudioAttributes(USAGE_MEDIA, CONTENT_TYPE_MOVIE)` and `handleAudioFocus = true`.
  - Handled `KEYCODE_MEDIA_STOP`, `PLAY`, `PAUSE`, `FAST_FORWARD`, `REWIND`.
  - TV standby / HDMI-CEC sleep audit: `onPause`/`onResume` manages `FLAG_KEEP_SCREEN_ON` to allow hardware sleep while releasing wakelocks and persisting playback positions.

## [1.3.0] - 2026-10-17

### Added
- **Signed Release Packaging (`app/build.gradle.kts`)**:
  - Configured `signingConfigs.release` with automated keystore fallback, ensuring `assembleRelease` produces a hardware-installable APK ready for sideloading onto physical Android TV and Google TV devices.
  - ProGuard / R8 code shrinking and resource optimization reducing release APK size to 15.1 MB (30% reduction from debug build).
- **Unit Test Suite Expansion**:
  - `PlaybackCoordinatorTest.kt`: Unit tests for fallback resolution, TV series episode calculations (`getNextEpisode`, `getPreviousEpisode`), seek boundary clamping, and 30s auto-next prompt eligibility.
  - `DataModelsTest.kt`: Unit tests verifying `ContinueItem` progressFraction calculation, `WatchlistItem`, and `GenreRowSeed` data model integrity.
  - Total automated test suite expanded to 33 unit tests across 7 suites with 100% pass rate.
- **TV UI Compatibility Indicators**:
  - `DetailsScreen.kt`: Added `SourceChip` compatibility badges displaying cyan `[NATIVE]` for direct HLS/MP4 feeds and coral `[EMBED]` for legacy web embeds.
  - `TvTopBar.kt`: Updated top bar navigation tab from "Shields" to "Settings" for intuitive 10-foot remote access.

### Changed
- **Privacy Subsystem Reconciliation (`com.nowshowing.tv.privacy`)**:
  - Eliminated obsolete WebView player interception classes (`ProtectedWebViewClient.kt` and `PopupProtection.kt`) in accordance with the mandatory native playback architecture.
  - Refactored `ContentBlockEngine.kt`, `ProtectionPreferences.kt`, and `BlockingStatistics.kt` to protect metadata API queries and source resolution network requests.
  - Connected `SourceValidation.kt` to live blocking metrics: recording isolated redirects, blocked tracker domains, and rejected unsafe URI schemes.
  - Modernized `SettingsScreen.kt` and `SettingsViewModel.kt` to display real-time network safety statistics and provide D-pad controls for ad blocking, tracker protection, and strict redirect validation.

### Repository Restructuring
- Promoted Android TV project from `android-tv/` to root directory (`app/`, `gradle/`, `build.gradle.kts`, `settings.gradle.kts`, `gradlew`).
- Removed legacy web application assets (`css/`, `js/`, `public/`, `index.html`) to maintain a 100% pure native Android TV codebase.
- Reconfigured `.gitignore` to standard Android specification.

### Documentation & Release Preparation
- Created `docs/CLAUDE_HANDOFF.md`: Comprehensive handoff guide detailing architecture, 3 targeted workstreams (Design Polish, Functionality & Lifecycle Audit, Performance & R8 Stabilization), test baselines, and hardware testing procedures.
- Hardened `app/proguard-rules.pro` with explicit keep rules for Room SQLite and Media3 ExoPlayer internal classes.
- Updated `docs/PRIVACY_AND_BLOCKING.md` and `docs/REMOTE_NAVIGATION.md` to reflect native ExoPlayer HUD, D-pad seek feedback, series sequencing, and network-level security.
- Updated `README.md` with complete Android TV instructions, build procedures, and handoff guide link.

## [1.2.0] - 2026-10-17

### Added
- **Mandatory Native Playback Engine (`com.nowshowing.tv.playback`)**:
  - Exclusive video playback powered by AndroidX Media3 / ExoPlayer with complete removal of embedded WebViews, iframe players, and browser pop-ups.
  - Native hardware-accelerated playback for HLS (`.m3u8`), MPEG-DASH (`.mpd`), and progressive MP4 streams.
  - Dedicated Media Source Resolution Layer (`MediaSourceResolver.kt` & `PlayableSource.kt`) evaluating configured providers prior to playback.
  - Incompatible Web Embed Rejection: All 11 legacy embed sources are recognized and safely rejected with an informative TV error card (*"Native playback is unavailable for this source. Try another source."*).
  - Protocol & Redirect Isolation (`SourceValidation.kt`): Enforces HTTPS (with local network RFC-1918 exemptions), blocks unauthorized URI schemes (`intent:`, `market:`, `javascript:`, `file:`), caps redirect chains at 5 hops, and filters redirect destinations against ad/tracker rules.
  - Playback Coordination (`PlaybackCoordinator.kt`): Handles source fallbacks, TV series previous/next episode sequencing, and 30-second next-episode prompt eligibility.
  - 10-Foot Remote-Controlled TV HUD (`PlayerScreen.kt`):
    - D-pad Left/Right seek (-10s / +10s) with animated visual feedback pills (`⏪ -10s`, `⏩ +10s`).
    - D-pad Center / OK / Media Play-Pause toggle.
    - Up/Down HUD overlay with auto-hide timer.
    - Scrubber timeline, elapsed/total duration, and series navigation.
    - TV Source Selection dialog displaying all 15 sources with `[NATIVE PLAYABLE]` or `[UNSUPPORTED WEB EMBED]` badges.
    - Automatic next-episode prompt in the lower right during the final 30 seconds of an episode.
- **Unit Test Suite Expansion**:
  - `SourceValidationTest.kt`: Testing protocol enforcement, local IP exemptions, media format detection, and redirect loop capping.
  - `MediaSourceResolverTest.kt`: Testing rejection of unsupported web embeds, resolution of HLS/MP4 streams, and episode sequencing.
  - `SourceRegistryTest.kt`: Updated with native source classification.
- **Documentation Suite Updates**:
  - Updated `docs/PLAYBACK_COMPATIBILITY.md` with complete 4-tier audit table (Native-playable vs Unsupported web-only embed).
  - Updated `docs/ANDROID_TV_PRD.md`, `docs/ARCHITECTURE.md`, `docs/QA_TEST_PLAN.md`, and `docs/RELEASE_CHECKLIST.md`.

## [1.1.0] - 2026-10-17

### Added
- **Native Brave-Inspired Privacy & Content Blocking (`android-tv/app/src/main/java/com/nowshowing/tv/privacy/`)**:
  - Native network request interception via `WebViewClient.shouldInterceptRequest` returning empty 200 OK responses for blocked requests.
  - High-performance Pure-Kotlin Adblock Plus (ABP) rule matcher (`ContentBlockEngine.kt`) supporting domain anchors (`||domain^`), path wildcards (`/ads/*`), and a 1000-entry LRU decision cache.
  - Automatic media bypass guaranteeing `.m3u8`, `.ts`, `.mpd`, `.m4s`, and `.mp4` video segments are never disrupted.
  - Unsolicited pop-up, background tab, and alert dialog suppression via `PopupProtection.kt` (`WebChromeClient`).
  - Bundled offline filter rules (`assets/filters/filters_adblock.txt`, `filters_tracking.txt`) with background HTTPS update capabilities.
  - Per-source shield exemption overrides with persistence in SharedPreferences.
  - Thread-safe local atomic telemetry counters (`BlockingStatistics.kt`) tracking blocked ads, trackers, and suppressed pop-ups without any remote data transmission.
- **TV Player HUD Shields Integration (`PlayerScreen.kt`)**:
  - Live HUD shield badge showing active protection state and blocked item counter (`🛡️ Shields: ON (N blocked)`).
  - Remote-friendly one-click toggle to disable shields for the active streaming source if playback fails.
  - Recovery prompt integration allowing users to disable shields or switch sources upon detecting a blank player.
- **Privacy & Content Blocking TV Settings Screen (`SettingsScreen.kt`)**:
  - Dedicated TV-first settings screen accessible via top navigation.
  - D-pad toggles for Ad Blocking, Tracker Blocking, Pop-up Blocking, and Third-Party Cookie restrictions.
  - Real-time blocking counters and one-click statistics reset.
  - Manual "Update Filter Rules" trigger for refreshing blocklists from remote repositories.
- **Privacy Documentation**:
  - Added `docs/PRIVACY_AND_BLOCKING.md` detailing architecture, network interception, rule syntax, and threat model.
  - Updated `docs/ARCHITECTURE.md`, `docs/PLAYBACK_COMPATIBILITY.md`, `docs/QA_TEST_PLAN.md`, and `docs/RELEASE_CHECKLIST.md`.

## [1.0.0] - 2026-10-17

### Added
- **Native Android TV Application (`android-tv/`)**:
  - Independent, sideloadable Android TV APK with `LEANBACK_LAUNCHER` and TV banner (`@drawable/banner`).
  - 10-foot remote-controlled UI designed with Jetpack Compose for TV & TV Material.
  - Alphanumeric D-pad on-screen keyboard with filter chips and search history.
  - Full port of the precision 29-mood recommendation engine (`MoodRecommendationEngine.kt`) with candidate deduplication, cached genre verification, and composite score ranking.
  - Multi-tier playback engine (`PlayerScreen.kt`): Native AndroidX Media3 (ExoPlayer) for direct media streams, TV-optimized hardware-accelerated WebView for third-party embed providers, and external player intent handoff (VLC / Just Player).
  - 10-second blank player detection and one-click source rotation.
  - Persistent storage in SQLite (`NowShowingDatabase.kt`) for Watchlist, Continue Watching with true playback progress, and OMDb request caching.
  - Complete test suite: `SourceRegistryTest.kt` and `MoodRecommendationEngineTest.kt` (100% pass).
- **Documentation Suite (`docs/`)**:
  - `ANDROID_TV_PRD.md` — Product requirements, TV ergonomics, and acceptance criteria.
  - `ARCHITECTURE.md` — Clean architecture, domain boundaries, and data flow.
  - `REMOTE_NAVIGATION.md` — D-pad navigation matrix, focus indicators, and state restoration.
  - `PLAYBACK_COMPATIBILITY.md` — Provider-by-provider compatibility matrix and playback tiers.
  - `QA_TEST_PLAN.md` — Comprehensive test execution matrix.
  - `INSTALLATION.md` — Sideloading via ADB (USB/Wi-Fi), flash drive, and launcher integration.
  - `RELEASE_CHECKLIST.md` — Pre-distribution readiness checklist.
- Project workflow tracking: `context.md`, `dev-log.md`, `CHANGELOG.md`.
