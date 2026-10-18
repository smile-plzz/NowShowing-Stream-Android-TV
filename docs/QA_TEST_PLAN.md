> Historical planning/reference document. Current implemented behavior is specified in [README](../README.md), [Architecture](ARCHITECTURE.md) and [Web playback](WEB_PLAYBACK.md). Native-only, Room, automatic iframe completion and demo-as-movie claims below are superseded. Candidate device checks are in TV_ACCEPTANCE.md.

> **October 17, 2026 playback update:** See [WEB_PLAYBACK.md](WEB_PLAYBACK.md) for the current dual-engine playback contract. Earlier native-only / zero-WebView requirements below are historical and superseded. Real-provider TV testing remains required.

# QA Test Plan: NowShowing TV

## 1. Scope & Test Environment
- **Target Hardware**: Android TV emulator (API 33/35, 1080p TV), Google TV Chromecast HD/4K, Nvidia Shield TV.
- **Input Devices**: Standard Android TV D-pad remote (Up, Down, Left, Right, OK/Select, Back, Play/Pause).
- **Core Objectives**: Guarantee 100% remote-navigable 10-foot UI, rock-solid OMDb metadata handling, accurate 29-mood recommendation engine, resilient multi-source playback, and persistent library state.

## 2. Test Execution Matrix

### 2.1. TV Launcher & Manifest Compliance
| Test Case ID | Objective | Expected Result | Pass/Fail Criteria |
|---|---|---|---|
| **TC-MAN-01** | Verify Android TV Banner | App displays 320x180 leanback banner on Android TV home screen. | Banner renders sharply without distortion or clipping. |
| **TC-MAN-02** | Sideload Discovery | App appears in Nook TV and default Android TV app drawer. | Icon/Banner visible; one-click launch from launcher. |
| **TC-MAN-03** | Touchscreen Independence | `android.hardware.touchscreen` marked `false` in manifest. | App installs on devices with zero touch hardware. |
| **TC-MAN-04** | Fixed Orientation | Verify application forces landscape mode. | Screen stays landscape (`16:9`) regardless of sensor events. |

### 2.2. D-pad Remote Navigation & Focus Management
| Test Case ID | Objective | Expected Result | Pass/Fail Criteria |
|---|---|---|---|
| **TC-NAV-01** | Initial Focus Acquisition | On initial launch, focus lands predictably on the Hero "Play" button or Top Nav. | No invisible focus; cyan/gold indicator clearly visible. |
| **TC-NAV-02** | D-pad Directional Matrix | Pressing Up, Down, Left, Right moves cleanly between elements. | Focus never jumps erratically or gets trapped. |
| **TC-NAV-03** | Rapid D-pad Navigation | Rapidly press Left/Right/Down across large carousel rows. | UI does not stutter or crash; images load smoothly via Coil. |
| **TC-NAV-04** | Back Key Focus Restoration | Open Detail screen from Home carousel item #5, press Back. | Focus restores precisely to item #5; scroll position maintained. |
| **TC-NAV-05** | No Off-Screen Traps | Navigate to ends of horizontal carousels. | Focus stops cleanly at row boundary; does not disappear into void. |

### 2.3. Catalog Discovery & Metadata
| Test Case ID | Objective | Expected Result | Pass/Fail Criteria |
|---|---|---|---|
| **TC-CAT-01** | Home Rows Loading | Verify Trending, Genre rows (Action, Sci-Fi, Drama, Comedy, Thriller), Popular Movies/TV. | All rows populate with posters and titles from OMDb seeds. |
| **TC-CAT-02** | Missing Metadata Grace | Load titles with missing posters (`N/A`) or missing plot summaries. | Fallback poster placeholder rendered; no crash. |
| **TC-CAT-03** | Catalog Sorting | In Browse Movies/TV, sort by Title A-Z, Newest, Top Rated. | Grid items reorder instantly according to selected criterion. |

### 2.4. Precision 29-Mood Recommendation Engine
| Test Case ID | Objective | Expected Result | Pass/Fail Criteria |
|---|---|---|---|
| **TC-MOD-01** | 29 Mood Palette Display | Navigate to Mood screen; verify all 29 moods rendered with emojis and labels. | 29 tiles present in grid with D-pad navigation. |
| **TC-MOD-02** | Precision Ranking Logic | Select mood "Happy". | Candidates fetched from mapped genres (Comedy, Musical, Family), scored by genre match count, and ranked by score + rating. |
| **TC-MOD-03** | Mood Detail Caching | Run "Happy", exit screen, run "Happy" again. | Second run uses cached detail lookups from Room; rapid result display without extra API calls. |
| **TC-MOD-04** | Surprise Me Button | Click "Surprise Me" with remote OK. | Random mood selected; recommendation pipeline executes automatically. |

### 2.5. 10-Foot Search
| Test Case ID | Objective | Expected Result | Pass/Fail Criteria |
|---|---|---|---|
| **TC-SCH-01** | On-Screen Keyboard | Use D-pad to type search term (e.g. "Dune"). | Letters input accurately; clear highlight on each virtual key. |
| **TC-SCH-02** | Search Results & Type Filter | Switch filter pill between "All", "Movies", "TV Series". | Query results update to reflect type filter. |
| **TC-SCH-03** | Search History | Enter search, exit screen, reopen Search. | Previous search query appears in recent search row; clickable via OK. |

### 2.6. Title Details & TV Series Navigation
| Test Case ID | Objective | Expected Result | Pass/Fail Criteria |
|---|---|---|---|
| **TC-DET-01** | Full Metadata Display | Open detail for a title (e.g., "Inception"). | Title, Year, Rating, Runtime, Director, Cast, Plot tags correctly displayed. |
| **TC-DET-02** | TV Series Seasons & Episodes | Open a TV show (e.g., "Breaking Bad"). | Season selector pills appear; selecting Season 2 loads Season 2 episode list. |
| **TC-DET-03** | Watchlist Toggle | Click "Add to Watchlist" button. | State toggles to "In Watchlist"; entry persists in Room database. |
| **TC-DET-04** | Recommended Titles | Verify recommendations row at bottom of Detail screen. | Similar titles in primary genre appear; clicking opens their detail. |

### 2.7. Native-Only Playback & Redirect Isolation
| Test Case ID | Objective | Expected Result | Pass/Fail Criteria |
|---|---|---|---|
| **TC-PLY-01** | Native Media3 Playback | Launch verified HLS/MP4 native stream (Mux HLS, Tears of Steel, Big Buck Bunny). | Video decodes smoothly in ExoPlayer; 10-foot HUD operates; D-pad Left/Right seeks with visual feedback pill; Play/Pause toggles. |
| **TC-PLY-02** | Unsupported Web Embed Rejection | Select any legacy web embed provider (e.g. `VidSrc.to`, `AutoEmbed`). | Source is immediately recognized as unsupported; player refuses to render a WebView; displays clean TV card: *"Native playback is unavailable for this source. Try another source."* |
| **TC-PLY-03** | In-Player Native Source Switching | From error card or HUD, click "Switch to Compatible Native Source" or open Source Dialog. | Player transitions directly to a compatible HLS/MP4 stream without leaving the native player. |
| **TC-PLY-04** | Redirect & Protocol Isolation | Attempt loading media stream redirecting to unencrypted HTTP, unknown schemes (`intent:`, `market:`), or exceeding 5 hops. | `SourceValidation` rejects request; error logged; player aborts dangerous redirect safely without launching external browser. |
| **TC-PLY-05** | TV Series Episode Sequencing | Play TV series episode; navigate via "Next Episode" / "Previous Episode" buttons in HUD. | Episode index updates; new episode resolves and plays seamlessly; Continue Watching record updates. |
| **TC-PLY-06** | Next Episode Auto-Prompt | Reach the final 30 seconds of a TV show episode. | Clean prompt overlay appears in lower right: *"Next Episode starting soon"*; clicking "Play Now" advances episode. |
| **TC-PLY-07** | Playback Progress & Resume | Watch media for 5 minutes, press Back, relaunch from "Continue Watching". | Player resumes playback at the exact 5-minute saved timestamp from Room database. |


### 2.8. Persistence & Memory Stability
| Test Case ID | Objective | Expected Result | Pass/Fail Criteria |
|---|---|---|---|
| **TC-PER-01** | Cold App Restart | Add items to Watchlist, kill app process, relaunch app. | Watchlist items load instantly from local Room SQLite database. |
| **TC-PER-02** | Low-Memory Stability | Run app continuously for 30 minutes across multiple browsing screens. | Heap remains bounded (<120MB); no `OutOfMemoryError` due to Coil bitmap pooling. |

### 2.9. Native Privacy & Content Blocking
| Test Case ID | Objective | Expected Result | Pass/Fail Criteria |
|---|---|---|---|
| **TC-PRIV-01** | Ad Network Interception | Launch embed player on source with aggressive banner/pop ads (e.g. `doubleclick.net`, `popads.net`). | Ad network HTTP/HTTPS requests intercepted via `shouldInterceptRequest`; empty response returned. No ad overlays or broken layout. |
| **TC-PRIV-02** | Analytics & Tracker Interception | Verify telemetry domains (`google-analytics.com`, `hotjar.com`) during playback. | Requests blocked; blocked tracker counter in `BlockingStatistics` increments accordingly. |
| **TC-PRIV-03** | Legitimate Media Stream Bypass | Play HLS (.m3u8, .ts), DASH (.mpd), and MP4 streams through WebView player. | Media stream fragments and manifests unconditionally bypass rule evaluator. Playback starts smoothly without CDN chunk drops. |
| **TC-PRIV-04** | Pop-up & Unsolicited Window Suppression | Trigger embed player button that attempts `window.open` or deceptive redirect. | Pop-up suppressed by `PopupProtection` WebChromeClient; `onCreateWindow` returns false; blocked popup counter increments. |
| **TC-PRIV-05** | Per-Source Exemption Override | Open Player HUD -> click Shields button or disable shields in Settings for current source. | WebView reloads with filtering disabled for that source domain. Shields HUD indicates "OFF (Disabled for this source)". |
| **TC-PRIV-06** | Privacy Settings & Statistics Persistence | In Settings screen, toggle Ad/Tracker/Popup blocking or click "Reset Statistics". | Toggle states persist across app restarts in SharedPreferences; local counters reset to zero cleanly. |

