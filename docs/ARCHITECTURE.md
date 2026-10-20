# Current architecture — 1.5.1

This describes implemented source behavior. It supersedes historical Room/native-only designs. Device performance and provider compatibility require separate TV validation.

## Application and navigation
`NowShowingApp` creates a Coil loader and initializes filter rules and metadata pruning on IO. `MainActivity` hosts Compose Navigation routes for Home, Browse, Mood, Search, Watchlist, Settings, Details and Player. Screen ViewModels expose StateFlow. There is no metadata backend, account service or cloud synchronization.

Compose lazy lists/grids retain saveable scroll state within their Navigation entries. `ReturnFocusScope` saves the selected card/action key and restores it on return/resume; Details defaults to the Play action once it exists. Restoration does not repeatedly request focus during ordinary recomposition. Removed items may fall back to normal Compose focus behavior. Physical remote testing remains necessary.

## Metadata repository
`MediaRepository` uses Retrofit/OkHttp against OMDb with one bundled demo key or up to five locally supplied keys. Invalid/inactive credentials fail over in order, with session rejection tracking and bounded attempts. Quota, network, service and no-results failures do not cycle credentials. Logging of request URLs is disabled because query strings include credentials.

- At most two concurrent metadata requests across repository instances.
- 64 striped mutexes bound synchronization memory and coalesce matching detail/search/season requests.
- Successful details are aliased by IMDb ID. Memory cache: access ordered, 128 entries, 24-hour TTL.
- SQLite payload TTLs: details 24 hours, seasons seven days, searches one hour.
- Service/key/network failures may fall back to payloads no older than 30 days after expiry. No-results does not use stale fallback. Stale metadata is identified in the session status.
- Persistent metadata: maximum 500 payloads, maximum 64 KiB each; expiry-based pruning on app initialization and writes. This bounds logical payloads to roughly 32 MiB; SQLite file pages/overhead may remain allocated after deletion.
- `MetadataHealth` publishes safe error categories and retains at most 12 timestamp/category records in memory. Quota/key failures pause new calls for 15 minutes; network/service failures for ten seconds. Explicit Retry resets the circuit. No-results never opens it. Already-running requests may finish. No raw exception messages, URLs, keys or title history enter this log.
- Home preserves existing shelf content if a refresh fails. Related titles use genre overlap among at most 100 fresh cached IMDb detail candidates; no network recommendations.

## Local storage
`NowShowingDatabase` is **SQLiteOpenHelper**, database `nowshowing_tv.db`, schema version 2. It is not Room.

| Table | Meaning and retention |
|---|---|
| watchlist | User-saved titles |
| recently_opened | Last 30 titles opened in a provider, episode/source, manual title-level watched label |
| continue_watching | Measured native playback progress, separate from page history |
| search_history | Last eight retained queries |
| metadata_cache | Rebuildable JSON metadata payloads |

Watchlist/history/progress/cache access occurs on IO through the repository. Cache clearing removes metadata and Coil image caches only; it preserves saved data and preferences. Screens can retain already-loaded models until reopened. No schema migration is required for these retention fixes.

## Playback
`SourceRegistry` builds existing provider URLs for IMDb title/season/episode. Web sources use `WebEmbedScreen` and `PointerWebView`; direct streams use `PlayerScreen` and Media3. The legacy `UNSUPPORTED_WEB_EMBED` name means unsupported by the native resolver, not by the app.

Web playback enters directly with pointer input focused. App controls are available through Back/Menu. HTTPS child frames are permitted; main-frame confinement, filtering, popup denial, certificate validation and lifecycle cleanup remain in place. No privileged JavaScript bridge, DOM click simulation or stream extraction is added.

No current provider exposes a verified completion/progress contract to this app. Web opening history is recorded, but web timestamp resume and automatic next episode are not claimed. Series controls expose manual Next episode and season/episode selection. Next resolves actual episode numbers and known season boundaries; missing metadata yields an explanation. Source changes cancel pending advancement.

`PlaybackPreferences` persists default-on autoplay. For supported native series, a genuine `STATE_ENDED` event bound to the current source instance prepares a ten-second cancelable countdown; OFF requires manual activation. Duplicate/stale events are ignored per session. No native sources are registered after demo removal, so this is not advertised as web autoplay. There is no post-movie recommendation overlay for web completion; related titles appear on Details.

## Performance bounds
Coil decoded image memory is capped at 16 MiB, disk at 50 MiB. Poster requests remain 300×450; backdrops 1280×720. Poster crossfades and unfocused card shadows are removed. Lazy item keys remain stable. Home loads shelves sequentially, with metadata calls bounded to two, and displays three genre rows rather than five. These are engineering limits, not measured frame-rate claims. Navigation-heavy and long-session behavior needs the TV workload in TV_ACCEPTANCE.md.

## Filtering and diagnostics
`FilterListManager` fetches the maintained Android repository's raw bundled list paths. Updates serialize against initialization, cap each response at 512 KiB, validate supported rule-shaped text, close responses and use AtomicFile replacement with rollback. Each list updates independently; result is COMPLETE, PARTIAL or FAILED. Unchanged/failed lists retain known-good files or bundled fallback.

Settings shows installed version/code/commit, build variant, device model, Android/API, WebView, public signing certificate SHA-256, cache payload/image sizes, heap snapshot and safe metadata error categories. Reports exclude viewing history, provider URLs, keys and device serials. Copy/share uses the Android clipboard/share chooser; TVs without targets can photograph the visible report. A heap snapshot is not a memory-leak diagnosis. Certificate permanence cannot be inferred from the APK; compare it with the previous install.

## Final TV UI pass
Primary navigation is a bounded LazyRow rather than an overflowing fixed Row. Tabs expose selection semantics and one focus target. Settings keeps five section tabs above a keyed scrolling body; Metadata opens initially when a metadata failure is recorded. Busy actions are disabled and diagnostic exports require a completed report. The report dialog has focusable text chunks and restores focus on close.

Poster cards use AsyncImage with stable dimensions and static loading/error text instead of SubcomposeAsyncImage per lazy item. Browse has retry/settings recovery actions; Mood cancels superseded work and distinguishes service failure from no matching recommendations. First-use pointer hints hide after eight seconds; app controls/help remain available. These changes are covered by JVM Compose layout/interaction tests, not a claim of physical-TV profiling.

Before public publication, the publisher independently rechecks APK checksums, both variants, public certificate report presence, version/code, exact commit and required prerelease status. It never overwrites an existing version.

## 1.6 browsing and health additions

Progressive collection loading reads fresh cached seeds on IO before resolving missing seeds in pairs. Home retains visible shelf priority. Browse retains cards during forced reload and separates loading from sorting. Genre/start-year filters use loaded details only; OMDb title search includes year in cache and pagination identity. Subscriptions in Home/Browse/Search/Watchlist/Settings are lifecycle-aware.

`diagnostics/BrowsingRecorder` owns an opt-in bounded window listener; MainActivity stops it on pause. `MetadataMetrics` retains anonymous process counters only. Settings health models freeze on IO and retain one local report with `@Keep` serialization fields. Photo pages have manual focusable navigation. See [App health](APP_HEALTH.md) for methods and limits. Existing database tables and saved user data are unchanged.
