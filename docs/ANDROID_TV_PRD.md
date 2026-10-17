> **October 17, 2026 playback update:** See [WEB_PLAYBACK.md](WEB_PLAYBACK.md) for the current dual-engine playback contract. Earlier native-only / zero-WebView requirements below are historical and superseded. Real-provider TV testing remains required.

# Product Requirement Document (PRD): NowShowing TV

## 1. Vision & Purpose
NowShowing TV is a standalone, native Android TV / Google TV application providing a cinematic, 10-foot discovery and streaming experience for movies and television shows. Built on top of the established concepts of the NowShowing web platform, it provides remote-controlled navigation, rich OMDb metadata, mood-based recommendations, and multi-tiered media playback.

## 2. Target Platform & Hardware Constraints
- **Target OS**: Android TV and Google TV
- **Minimum SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 35 (Android 15)
- **Input Method**: TV Remote Control (D-pad: Up, Down, Left, Right, OK/Center, Back, Play/Pause). No touch screen or mouse pointer.
- **Orientation**: Fixed Landscape (16:9 widescreen: 1080p, 4K UHD, 720p).
- **Form Factor**: Television screens viewed from 6–10 feet distance.
- **Hardware Profile**: Low-to-mid-range TV SoCs (quad-core ARM, 1.5GB–3GB RAM). High emphasis on memory efficiency, bounded caches, and lazy rendering.

## 3. Core Functional Requirements

### 3.1. Home & Catalog Discovery
- **Hero Banner**: Highlights a featured trending or popular title with high-res backdrop, title, tags, synopsis, and direct "Play" and "More Info" actions.
- **Continue Watching Row**: Displays titles in active progress with episode markers and percentage progress bar.
- **Trending Now Row**: Horizontal carousel seeded from curated popular titles (`TRENDING_SEED`).
- **Genre Rows**: Curated genre discovery rows (Action & Adventure, Sci-Fi & Fantasy, Acclaimed Drama, Comedy, Thrillers).
- **Popular Movies & TV Rows**: Seeded carousels matching the web app catalog (`POPULAR_MOVIES`, `POPULAR_TV`).
- **Mood Discovery Entry**: Prominent interactive banner linking to the Mood Discovery screen.

### 3.2. Dedicated Browsing (Movies & TV)
- Dedicated catalog browsing grids with remote pagination.
- Remote-accessible filter and sort bar:
  - Featured (default)
  - Title A–Z
  - Newest / Oldest
  - Top Rated (IMDb rating)

### 3.3. Precision 29-Mood Recommendation Engine
Direct port of the web application's recommendation engine:
- Full 29-mood palette (`Happy`, `Sad`, `Excited`, `Relaxed`, `Angry`, `Scared`, `Inspired`, `Nostalgic`, `Curious`, `Romantic`, `Adventurous`, `Thoughtful`, `Anxious`, `Hopeful`, `Playful`, `Adult`, `Wired`, `Vengeful`, `Goofy`, `Eerie`, `Empowered`, `Jaded`, `Awestruck`, `Challenged`, `Giddy`, `Focused`, `Escapist`, `Intrigued`, `Tense`).
- Each mood maps to canonical OMDb genres and curated keywords.
- Algorithm executes multi-query candidate generation, merges & deduplicates, performs cached detail lookups for candidate titles, computes genre-match intersection score, and ranks by match score then IMDb rating.
- Includes a "Surprise Me" random mood selector and mood keyword search.

### 3.4. Search Experience
- 10-foot optimized search interface.
- On-screen D-pad alphanumeric keyboard + voice search trigger (when available on device).
- Filter pills: All, Movies, TV Series.
- Search history row with one-click re-query and clear functionality.
- Paginated results grid with empty, loading, and error states.

### 3.5. Title Details & Series Navigation
- Full cinematic presentation: backdrop artwork, poster, title, plot, ratings, release year, runtime, age certification, director, cast, and country.
- Primary Action buttons: "Play / Resume", "Add to Watchlist" (toggle), "Select Source".
- **Series Episode Picker**:
  - Displays available seasons derived from OMDb `totalSeasons`.
  - Horizontally browsable season pills.
  - Episode list showing Episode number, Title, and synopsis.
  - Remembers last selected season and episode.
- **Recommended Titles Row**: Dynamically queries similar titles based on primary genre.

### 3.6. Mandatory Native-Only Playback Architecture
- **Exclusive Native Engine (AndroidX Media3 / ExoPlayer)**:
  - All video playback takes place exclusively inside the native Media3 player.
  - Zero WebViews, zero HTML/DOM rendering, and zero browser pop-ups in the playback flow.
  - Plays standards-compliant HLS (.m3u8), DASH (.mpd), and progressive MP4 media streams.
  - Full remote control: Play/Pause, 10s Skip Forward/Backward (with visual feedback), Scrubber timeline, Audio & Subtitle track selector.
  - Tracks true playback progress in milliseconds to local Room database for resume watching.
- **Dedicated Media Source Resolution Layer**:
  - `MediaSourceResolver` evaluates requested sources prior to playback.
  - Supported direct media endpoints proceed to native playback.
  - Legacy iframe web embeds (e.g. VidSrc, AutoEmbed) are classified as incompatible and rejected without exposing the user to third-party web pages.
- **Safe Failure & Source Switching**:
  - Clear, user-friendly TV error card when an incompatible web source is selected: *"Native playback is unavailable for this source. Try another source."*
  - Remote-friendly action buttons to switch to a compatible native stream or select from the source dialog.
- **Redirect & Protocol Isolation**:
  - Enforces HTTPS for all remote media streams (allowing RFC-1918 private IPs for local home media servers).
  - Rejects arbitrary external intent launches, app store redirects, and URL scheme hijacking.
- **Series Playback Progression**:
  - Previous and Next Episode buttons in the TV player HUD.
  - Unobtrusive auto-next episode suggestion banner in the lower-right during the final 30 seconds of an episode.

### 3.7. Persistence & Library
- **Watchlist**: Persisted in Room database. Direct remote access screen with one-click remove.
- **Continue Watching**: Tracks genuinely initiated playback, current playback timestamp, total duration, and season/episode for series.
- **Metadata Cache**: SQLite/Room-backed LRU cache for OMDb responses to minimize API calls and respect rate limits.

## 4. Acceptance Criteria
1. APK builds cleanly via Gradle without manual file copying.
2. App installs and boots on Android TV / Google TV hardware and emulators.
3. Every UI element is reachable and operable solely using a TV remote control.
4. Back button returns predictably without losing focus or exiting prematurely.
5. All 29 moods produce ranked recommendations matching web logic.
6. Season and episode selection works for television shows.
7. Playback handles valid streams natively and provides robust fallback for embed sources.
8. Watchlist and viewing progress survive application restarts.
