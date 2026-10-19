> Implementation authorized by Ismail on 19 October 2026. This handoff remains the historical planning baseline; current candidate behavior and open device gates are documented in [v1.6.0-rc.1](releases/v1.6.0-rc.1.md) and [App health](APP_HEALTH.md).

# NowShowing TV - Post-release optimization handoff

Prepared: 19 October 2026, Asia/Dhaka  
Owner: Ismail Hossain � GitHub: https://github.com/smile-plzz  
Repository: https://github.com/smile-plzz/NowShowing-Stream-Android-TV  
Reviewed source baseline: `84771263cebda9b5f08fdf93e841190268cae7aa` (`main`)  
Source version: **1.5.1**, version code **15004**

## 1. Outcome and authorization

The app is shipped and functional. The owner reports that **browsing shelves, the library, and navigating through the app are the main laggy areas**. Previously successful playback should be preserved. Hardware limitations are a hypothesis, not an established cause.

This milestone is analysis and documentation only. Do not implement application changes, rebuild, or publish a new APK until the owner reviews this handoff and authorizes implementation. The current task authorizes a repository documentation update only.

The next development milestone should make browsing measurably smoother, add useful TV-friendly performance reporting, remove demo media playback, and improve About. Discovery enhancements follow once browsing performance is understood and stable.

### Confirmed owner requirements

1. Prioritize browsing/navigation performance on the owner's TV.
2. Remove demo media playback from the product.
3. Expand About with owner information, GitHub links, app/build information, and support links.
4. Add on-demand app health/performance audits with live measurements where available.
5. Make reports easy to photograph directly from the television; copying is inconvenient.
6. Assess dynamic top/popular/new-release shelves, release-year and keyword search, genres, and useful sorting/filtering. These discovery additions are conditional on feasibility and data availability.
7. Refine UI/UX while preserving the current visual identity and working functionality.
8. Assess phone layouts, but Android TV remains the primary platform.

## 2. Evidence and limits

This document combines the owner's supplied post-release audit brief, this conversation, the repository tree, and static inspection of the implementation at the baseline above. Repository files were retrieved through GitHub after a local clone failed because of environment networking. No app code was modified during this review.

The browse/home/library/search/repository/cache/settings/diagnostic paths were inspected directly, alongside the source registry, playback state management, manifest, build workflow, existing tests, and current README/limitations. Playback UI and privacy paths were also inspected for relevant lifecycle, source selection, and diagnostic integration points. This is a source-grounded planning audit, not a runtime performance certification or an exhaustive security review.

No APK was installed, benchmarked, or tested in this session. No Gradle build or test suite was run. CPU/GPU load, frame timings, input latency, total device RAM, thermal behavior, and a before/after baseline are unknown. Do not present suspected bottlenecks as measured root causes.

The earlier file `NowShowing-TV-Development-Handoff.md` currently reads only `# `; it supplies no usable detailed requirements. The supplied `Pasted text(20261018-195037).txt` and current code are the useful planning inputs. This document is the consolidated post-release handoff.

### Owner-provided device snapshot

| Field | Value / interpretation |
| --- | --- |
| Device | Hisilicon SMART_TV |
| Android | 14; API 34 |
| WebView | 133.0.6876.3 |
| Installed certificate SHA-256 | `22:AA:2A:F5:98:20:E0:03:E4:51:FF:B3:0F:26:42:50:66:76:41:FC:21:00:40:98:79:8E:B6:93:44:0E:B3:52` |
| Metadata | "No failure recorded; not a connectivity test" |
| Remaining text | Truncated/uninterpretable fragment: `story, provider L`; stale-cache label has no supplied value |
| Installed app version | Not supplied; source version above must not be assumed to identify the installed APK |
| Main symptom | Browsing shelves/library/general navigation lag; no new playback-lag complaint |

The fingerprint is a public certificate identifier, not a performance measurement. Compare with the next APK's certificate before claiming in-place update compatibility; permanence cannot be inferred from a single fingerprint. Missing fields are unknown, not zero or healthy.

## 3. Architecture and source map

Paths below are relative to the repository. Kotlin paths are under `app/src/main/java/com/nowshowing/tv/` unless otherwise stated.

| Area | Implementation | Current behavior |
| --- | --- | --- |
| App/bootstrap | `NowShowingApp.kt`, `ui/MainActivity.kt` | Compose navigation; asynchronous metadata pruning/filter initialization; shared Coil loader |
| Home | `ui/home/HomeScreen.kt`, `HomeViewModel.kt` | Lazy vertical list with lazy horizontal shelves; recent/history/hero/curated content |
| Browse | `ui/browse/BrowseScreen.kt`, `BrowseViewModel.kt` | Lazy adaptive grid; curated movies/TV; four existing sort modes |
| Cards/focus | `ui/common/TvFocusableCard.kt`, `TvContentRow.kt`, `ReturnFocus.kt`, `TvTopBar.kt` | Stable card keys, focus scale/shadow/border; per-entry selection and lazy list state |
| Library | `ui/library/WatchlistScreen.kt`, `WatchlistViewModel.kt` | Local watchlist, lazy grid; reads run through repository IO dispatch |
| Search | `ui/search/SearchScreen.kt`, `SearchViewModel.kt` | Remote keyboard, recent queries, type filter, explicit search and pagination |
| Metadata | `data/api/OmdbApiService.kt`, `ApiClient.kt`, `data/repository/MediaRepository.kt` | OMDb requests; shared two-request semaphore; striped locks and local cache fallback |
| Persistence | `data/db/NowShowingDatabase.kt`, `data/repository/MetadataCache.kt` | SQLiteOpenHelper database v2; bounded metadata and local user data |
| Details/recommendations | `ui/details/DetailsViewModel.kt`, `domain/mood/MoodRecommendationEngine.kt` | Episodes, watchlist and source choice; cached genre-related candidates; mood recommendations |
| Playback | `ui/player/PlayerViewModel.kt`, `PlayerScreen.kt`, `WebEmbedScreen.kt`, `playback/` | Movie/series providers use WebView; native Media3 path currently serves reference/demo streams |
| Sources | `domain/source/SourceRegistry.kt` | Eleven web providers plus four native demo/reference sources |
| Privacy | `privacy/ContentBlockEngine.kt`, `FilterListManager.kt`, `BlockingStatistics.kt` | Exposed-request filtering; bounded update reads; retained known-good rules |
| Settings/report | `ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`, `DiagnosticReport.kt` | Grouped settings, refreshed text report, scrollable dialog, copy/share |
| Release | `app/build.gradle.kts`, `.github/workflows/build-apk.yml`, `scripts/` | Debug/release builds, tests, versioned packaging, signing details, conditional publish |

The stack is Kotlin/Compose, ViewModel/StateFlow, Retrofit/OkHttp/Gson, Coil, SQLiteOpenHelper, WebView and Media3. Preserve these boundaries; this milestone does not justify a rewrite.

## 4. Existing features and maturity

| Feature | Verified implementation | Next action |
| --- | --- | --- |
| Lazy shelves/grids | Already implemented on Home, Browse, Watchlist and Search | Profile and refine; do not propose adding basic virtualization as though absent |
| Image/cache bounds | 16 MiB Coil memory cache, 50 MiB image disk cache; card requests 300x450; global crossfade off | Measure hit rate, decode/allocation churn and actual screen density before changing limits |
| Metadata resilience | Two network slots, coalescing locks, memory/disk cache, stale fallback, failure categories | Preserve bounds and user feedback; measure warm/cold behavior |
| Featured/popular content | Fixed seed lists in `MediaRepository`; Home displays three of five genre seeds | Treat as curated, not live popularity or a complete catalog |
| Sorting | Featured, Title A-Z, Newest, Top Rated already exist | Fix year handling; make scope and missing-value behavior clear |
| Search | Title text, movie/series/all filter, recent queries, load-more and cancellation | Extend query model instead of building a parallel search flow |
| Watchlist/history | Local watchlist; Recently Opened with episode/source; manual watched label; separate native progress | Preserve data and distinguish opened pages from measured watching |
| Episodes/autoplay | Manual episode controls and metadata-based progression; persisted default-on autoplay | Web completion is not integrated; do not promise automatic advancement for web providers |
| About/diagnostics | Already implemented, including owner name and basic source URL | Expand links and convert reporting into actionable health/performance views |
| Blocking/recovery | WebView request filtering and source controls; partial-update fallback | Regression-test; no Brave-equivalence claim |
| Phone behavior | Owner reports functional but awkward layout; manifest requires leanback and forces landscape | Keep TV-first positioning; assess narrow layout/touch improvements separately |

Current source rotates locally configured metadata credentials only after invalid/inactive-key responses, not quota/network/service/no-results failures. Existing tests assert this behavior. Do not promise seamless quota fallback or publish credentials in reports or documentation.

## 5. Confirmed findings and optimization hypotheses

### F1 - Incorrect year-range sorting (confirmed code defect)

`BrowseViewModel.applySort` removes all non-digits from `year` before converting to an integer. A series year such as `2011-2019` becomes `20112019`, giving incorrect newest ordering versus single-year entries.

Proposed change: parse an explicit first/start year, or a verified release date when available. Define handling for ongoing series, missing dates and ties. This is a small, low-risk correction; verify examples with ranges, `N/A`, single years and equal years.

### F2 - Browse retry can be suppressed (confirmed control-flow issue)

`retryBrowse(kind)` calls `loadBrowse(kind)`, whose same-kind guard returns when `rawItems` is nonempty. This prevents refreshing an already-populated/partially populated collection through that path.

Proposed change: separate forced refresh from initial-load reuse; keep existing cards during refresh, publish failures and allow another bounded retry. Also disable or coordinate sorting while the initial load is in progress, since `setSortMode` calls `applySort`, which clears `isLoading` before loading necessarily finishes. Verify the visible race on-device before classifying its severity.

### F3 - Home shelf loading is sequential at the shelf boundary (confirmed behavior)

`HomeViewModel.loadHomeData` launches then joins featured, each of three genre shelves, and popular movies before the final TV shelf. Within each shelf, `lookupTitles` starts lookups concurrently but waits for all results. Network concurrency remains capped at two globally.

Impact: a slow early shelf can delay later shelves. This explains potential population delay, not proven D-pad/rendering lag. Profile separately. Consider cached-first snapshots, visible-shelf priority, per-shelf status and batched incremental results. Do not remove the global request bound or start an unbounded catalog fetch.

### F4 - Browse waits for the complete seed lookup (confirmed behavior)

Browse resolves the full fixed movie/TV list before publishing its grid and holds a full-screen spinner while loading. Progressive cached-first cards and a retained grid during refresh could improve perceived speed. Loading time and scroll jank require different measurements.

### F5 - Warm disk metadata is deserialized repeatedly (confirmed implementation; impact unmeasured)

`cachedDetail` returns fresh disk entries without admitting them into the memory tier, explicitly preserving disk expiry. On a cold process with warm disk cache, repeated lookups can repeat SQLite/JSON work. If profiling justifies a change, promote entries with their original remaining expiry; do not silently reset TTL or extend stale metadata validity.

### F6 - Existing visual costs need measurement (hypothesis)

Cards already use fixed-size artwork, quiet placeholders, modest focus scaling and only focused shadows. Hero artwork still requests 1280x720 with a 300 ms crossfade. Inspect frame timing during repeated D-pad movement, card focus transitions, shelf scrolling and hero visibility. Tune effects individually only when measured. Maintain a clear focus indicator even if animation is reduced.

### F7 - Navigation state/subscriptions deserve profiling (hypothesis)

Screens commonly use `collectAsState`; focus restoration creates a lifecycle-aware effect per card. Main navigation pops back to Home when switching destinations. Measure composition counts, recreated ViewModels, repeated fetches, focus restoration and list-position retention. Use lifecycle-aware subscriptions or narrower state only where this improves actual behavior; do not assume all recomposition is costly.

### F8 - Bounded storage still has write costs (hypothesis)

Metadata writes prune inside transactions and title lookups may write both title-key and IMDb-key payloads. Measure disk-write/lock contention and cache churn during browsing. Watchlist loading is triggered both by its ViewModel initialization and screen effect; inspect redundant reads and errors. Avoid speculative database replacement.

### F9 - Existing diagnostics are snapshots, not a performance audit (confirmed gap)

`DiagnosticReport.kt` reports build/device/WebView/certificate, metadata status, cache bytes, heap values and twelve recent metadata error categories. There is no frame/input timing audit or timestamped multi-page performance summary. The current dialog chunks arbitrary text into three-line focusable blocks; long certificates and messages can wrap awkwardly for photographs.

### F10 - Broader maintenance observations

The manifest allows cleartext traffic even though relevant protections enforce HTTPS elsewhere; evaluate actual provider compatibility before tightening it. Blocking statistics persist on each event and use per-instance counters; investigate accuracy and write frequency if privacy metrics are expanded. No measured browsing impact is established. Release builds can fall back to development signing; retain explicit signing labels and verify certificate continuity rather than assuming source configuration proves it.

No P0 crash, ANR or severe root cause has been demonstrated. Promote an issue to P0 only with evidence.

## 6. Required product changes for the next implementation phase

### A. Remove demo media playback

Remove release-facing reference/demo playback buttons, sample stream entries, diagnostic player routes and demo-related help/copy. Primary touchpoints: `SourceRegistry.NATIVE_SOURCES`, Settings Help, `PlayerViewModel.initialize(type == "diagnostic")`, `PlayerScreen` source selection, navigation/back handling, associated tests, README and playback documentation.

All four current native sources are reference streams, including the Mux item labeled `NATIVE_STREAM`. Do not leave that one behind merely because its type differs from `AUTHORIZED_SAMPLE`.

Audit all `.first()` defaults, resolver fallbacks and registry consumers before deleting the list. The default player/details state currently refers to native/demo-backed registry values. Replace unsafe defaults with valid production state or explicit unloaded/null state. Verify old diagnostic routes fail gracefully, rather than crashing or playing unrelated media.

Removing demos does not authorize replacing the working WebView providers or deleting unrelated progress/watchlist data. Evaluate remaining Media3/resolver dependencies after reachability analysis; remove dead code only where proven unused. Metadata's shared demo API credential is a separate concept: the owner requested removal of demo **media playback**, not removal of metadata access.

Acceptance: no sample streams, demo playback entry points, accidental demo fallback or broken registry defaults in the release. Normal movie/series playback and saved user data remain intact.

### B. Expand About

Keep owner attribution and add a clean remote-friendly presentation of:

- App name, short purpose, version name/code, build variant and commit.
- "Created by Ismail Hossain" and GitHub profile: https://github.com/smile-plzz
- Source: https://github.com/smile-plzz/NowShowing-Stream-Android-TV
- Releases: https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases
- Report an issue: https://github.com/smile-plzz/NowShowing-Stream-Android-TV/issues
- Short troubleshooting/known-limitations guidance and applicable dependency/content attribution verified from the project.

Use focused link buttons, readable URLs, and optionally QR codes for these public links. If no browser handler exists, leave the URL/QR visible with helpful feedback. Do not invent a personal website, email address or additional social profile. Keep certificate details in Diagnostics rather than overwhelming About.

### C. Health and performance audit, designed for photographs

Flow: **Settings  App health & diagnostics  Run health check  Summary  numbered detail pages**. Offer a separate short **Record browsing performance** session so browsing can be measured during real navigation.

Use two modes: lightweight live readings while the health screen is open, and **Freeze report** that captures one consistent, timestamped report for photographs. Live values must not move or reorder content while the user photographs a report. Stop sampling when leaving; no permanent background polling or automatic uploads.

Recommended report pages:

| Page | Content |
| --- | --- |
| 1 - Summary | Report ID, capture time/timezone, app/build, device, observed warning categories, recording duration and next actions |
| 2 - Browsing | Screen/scenario, collected frame duration/jank summary, dropped/slow-frame definitions, sample count and refresh-rate context; input timing only when actually instrumented |
| 3 - Memory/cache | App heap used/max, optional process memory where supported, system available memory/low-memory flag, image and metadata cache size, measured cache/request counters |
| 4 - Metadata/network | Last successful measured request, bounded probe result and latency if run, error categories, retries, cache/stale usage; distinguish no failure from connectivity tested |
| 5 - Environment/errors | OS/WebView, build variant, certificate on a dedicated subpage if needed, bounded sanitized recent errors, privacy-filter status and supported limitations |

Choose frame instrumentation appropriate to this Android/Compose app during implementation; label what is measured (window frame time versus sampled main-thread delays, for example). App heap is not total TV memory. Avoid an unexplained universal "health score," invented FPS/CPU/GPU metrics, or a green success status for unavailable checks. A diagnostic probe does not verify video playback.

Photo-mode requirements:

- Large high-contrast text, safe margins and a small number of labeled fields per page.
- Manual Previous/Next using the remote, visible "Page X of Y," stable focus, Back/Done and an explicit rerun action.
- Report ID, app version and timestamp repeated on every page so photographs stay attributable.
- No auto-scrolling, auto-advance, marquee text or clipped values. Wrap long fields or split them into labeled continuation pages; never silently truncate evidence.
- Freeze all pages to the same snapshot. Allow reading/reopening the last completed local report with its original timestamp.
- Summary must remain useful without copying, exporting, a browser or a phone connection. Copy/share may remain secondary conveniences.
- Exclude API keys, viewing history/title identifiers, provider URLs, account identifiers, serial numbers, raw exception URLs and private local paths. No credentials in QR codes.

Heavy checks should run on demand, have timeouts, cancellation and visible progress, and avoid repeated cache-directory scans or network calls on every refresh. Retain a small bounded local report history only if needed; show its age and provide deletion. Compare audit-on versus audit-off overhead before claiming the instrumentation is lightweight.

### D. Discovery, search and sorting - conditional feasibility track

The current collection lists are static. Current `OmdbApiService.searchTitles` exposes query, page and type, without a release-year parameter. Search text is title matching, not a verified plot/theme keyword engine. Current sorts apply to the fetched curated list; "Top Rated" is not a global ranking.

Implement progressively after performance work:

1. Fix existing year sorting and improve clear sort labels/scope. Preserve selection and focus when sorting.
2. Add a year filter to the query model if verified provider support allows it; include every filter in cache keys, request identity, pagination and stale-response protection.
3. Add genre/type/year filtering of the loaded library/catalog with clear "loaded titles" scope. Fetching all details solely to filter every search page can multiply requests; avoid it.
4. Assess genuine new-release/popular/trending data. Before selecting a source, verify current official API capabilities, free-tier needs, attribution, regional/date semantics, IMDb mapping, quotas, caching permissions and failure behavior. No scraping or paid dependency should be assumed.
5. If a suitable source is available, add a bounded discovery adapter that maps identifiers to the existing detail/player routes, with deduplication and cached shelves. Keep it separate from playback.
6. If live rankings are unavailable, show honest curated/cached collections with freshness labels. Do not relabel seed lists as real-time trending or infer theatrical/streaming availability from a release year.

Use static artwork and progressively loaded content for "dynamic" discovery; the requirement does not imply animated backgrounds, video previews or rotating carousels. Persist chosen filters/sort when useful, add a visible Clear filters action and explain empty results.

Optional supporting refinements: filter/sort Watchlist; preserve shelf/filter position on return; show content freshness; clearer recovery when a provider is unavailable. Assess existing behavior before adding duplicate controls.

## 7. TV UI/UX and phone compatibility

Validate predictable D-pad focus through shelves, tabs, dialogs and settings; visible focused versus selected states; consistent Back behavior; focus restoration after details/playback; and stable scroll position when data refreshes. Loading/failure controls must be reachable without a touch screen. Preserve direct Play and keep hints brief and discoverable later.

Assess readability at normal viewing distance, overscan margins, poster fallback, clipped action rows and animation cost. Avoid replacing useful retained content with a full-screen spinner during refresh.

For phones, recommend **TV-first with limited sideload compatibility** for this milestone. The manifest's required leanback feature and forced landscape explain part of the current platform positioning. Inspect fixed 48 dp gutters, fixed card dimensions and custom remote keyboard on narrow screens. Low-risk responsive spacing/touch fixes can be proposed; full portrait/mobile product support is a separate decision and must not weaken the TV experience.

## 8. Prioritized roadmap

Complexity is relative implementation scope, not a promised delivery time. Benefits are expected until measured.

| Priority/order | Item and present problem | Proposed result / user benefit | Feasibility, complexity, performance impact and risk |
| --- | --- | --- | --- |
| P0 - conditional | No critical defect established | Investigate any reproducible ANR/crash first | Severity requires evidence; do not invent blockers |
| P1 / 1 | Browsing lag lacks measurements | Record baseline, add bounded browsing instrumentation and photo reports | Feasible; medium-high; enables diagnosis; instrumentation itself can add overhead |
| P1 / 2 | Sequential shelf/batch loading | Cached-first, progressive visible content; retain cards on refresh | Feasible; medium; reduces perceived waits; guard request volume, focus and ordering |
| P1 / 3 | Potential artwork/render/cache churn | Tune only measured hotspots | Feasible; medium; expected smoother D-pad use; cache/effect changes may trade memory for network or readability |
| P1 / 4 | Retry guard and load/sort state coordination | Explicit forced refresh with stable populated UI | Feasible; small-medium; reliable recovery; test cancellation and repeated requests |
| P2 / 5 | Newest mishandles year ranges | Correct chronological sort | Feasible; small; negligible cost; define date semantics |
| P2 / 6 | Demo playback remains exposed | Remove sample playback and safe defaults/dead routes | Feasible; medium; simpler product; shared source/player references need careful cleanup |
| P2 / 7 | About has limited links | Owner/source/releases/issues and public-link QR where useful | Feasible; small; low runtime cost; handle missing browser |
| P2 / 8 | Remaining remote/focus/readability friction | Consistent navigation, return position, photo readability | Feasible; medium; avoids repeated steps; actual TV checks required |
| P3 / 9 | Search lacks year/genre controls | Verified filters and explicit result scope | Conditional; medium; useful discovery; protect cache keys, quotas and pagination |
| P3 / 10 | Popular/new content is static | Verified dynamic data adapter and cached shelves | Conditional on source; medium-high; background work must be bounded; attribution/ID mapping risks |
| P4 / later | Mobile layout awkward | Adaptive mobile support after explicit scope decision | Feasible in principle; medium-high; prevent TV regressions |

## 9. Implementation phases and acceptance gates

### Phase 0 - Reconcile baseline

Re-fetch current main and compare with the baseline commit. Read any applicable repository instructions. Identify installed app version and exact APK/build. Confirm completed features before carrying findings forward. Link this handoff to relevant existing docs; historical design docs are not the current specification.

Acceptance: source/build/device baseline is recorded; all changed requirements remain traceable; no app changes before owner authorization.

### Phase 1 - Measure and expose health

Implement report models, capture/freeze/photo pages and opt-in browsing measurement. Capture cold and warm startup, first shelves, repeated shelf movement, Browse, Search and Watchlist, including after returning from playback.

Acceptance: all report pages can be photographed without clipping or moving data; unknown/skipped/error/success states are distinct; recording duration/sample count and methods are stated; no secrets; cancellation and backgrounding stop work.

### Phase 2 - Optimize the measured browsing path

Fix refresh/sort defects; improve cached-first/progressive loading and only the rendering, image, memory or IO work shown to matter. Retain existing two-request bound unless evidence justifies a deliberate change.

Proposed comparison gate: at least three repeat runs of the same fixed navigation scenario on the owner's TV, same APK variant, similar network/cache conditions and thermal state. Record median and tail frame/input metrics and shelf-ready times. Seek a repeatable improvement with no new freezes, focus loss or sustained memory growth. Set numeric targets from the baseline before changing code rather than claiming arbitrary FPS or latency guarantees.

### Phase 3 - Product cleanup and polish

Remove demo media, expand About, validate focus/readability/Back behavior, update stale documentation and relevant tests.

Acceptance: sample media cannot be launched; all valid providers still route correctly; About links are useful without an installed browser; watchlist/history/settings survive the update; TV-only navigation completes all flows.

### Phase 4 - Discovery proof of feasibility

Verify official provider documentation and data terms, then propose and implement only approved discovery scope. Year/title/type filters and local sorting should be useful even if a live popularity source is deferred.

Acceptance: freshness/source and result scope are honest; requests bounded; cold-cache/no-network behavior useful; filters compose with pagination and caching; metadata discovery never implies playback availability.

### Phase 5 - Release verification after implementation approval

Run relevant existing tests plus focused regressions, build both variants, validate minified release, compare installed certificate, then produce versioned APKs/release notes/checksums according to the existing workflow. Do not use `[publish]` or dispatch publishing during this documentation-only task.

## 10. Test and regression plan

Existing tests cover metadata errors/cache/coalescing/credential fallback, database retention, source routing, episode progression, preferences, privacy updates, pointer behavior and selected Compose settings/navigation behavior. They are valuable but do not establish real-TV smoothness. No existing instrumentation/benchmark source set appears in the reviewed tree.

During implementation, add behavior-focused tests where code changes warrant them:

- Year ranges/unknown dates, refresh after partial success, sort during load, cancellation and stale-response rejection.
- Filter-aware cache identity and pagination; no duplicated requests on repeated remote actions.
- Report freeze consistency, page splitting for long values, redaction, unavailable metrics, timeouts and sampler lifecycle.
- Removal of every demo entry/route/default; preservation of normal provider selection.
- Existing metadata failure and user-data retention guarantees.

Repository commands for the implementation phase (not executed in this audit):

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug assembleRelease
python3 -m unittest discover -s scripts -p 'test_*.py'
```

Real-device scenarios:

| Scenario | Evidence to capture |
| --- | --- |
| Owner's Hisilicon TV, cold/warm cache | Build/version, recording method, shelf-ready time, frame timing, memory, input behavior |
| Hold D-pad across shelves/grid, reverse direction | Focus remains visible/reachable; frame/input tails; no queued-input burst |
| Open details and return repeatedly | Same selected item/scroll position; no repeated full reload or growing memory |
| Browse/library with many saved titles | Stable scroll/focus and bounded artwork work; no UI freeze |
| Slow network/offline/quota | Cached content retained, accurate error state, bounded recovery |
| Playback then return to browse | Existing playback unchanged; WebView cleanup; browsing stays responsive |
| Photograph every report page | Readable from viewing distance; full values, stable snapshot, page identity |
| Reduced-width phone and second TV where available | Layout/touch sanity; remote behavior unaffected; document limited mobile scope |
| Longer browsing/viewing session | No unbounded memory rise, leaked player resources or degraded navigation |

Use debug builds for diagnosis and the minified release for final performance comparison. Report simulated/emulator results separately from actual TV results.

## 11. Release risks and checklist

- [ ] Baseline commit and installed version reconciled; changes limited to approved scope.
- [ ] Before/after browsing evidence recorded on the owner's TV; no hardware-blame assumption.
- [ ] No playback, pointer/fullscreen, provider switching, episode-control or privacy regressions.
- [ ] Web progress/completion/autoplay limitations remain explicit; no false timestamp-resume promises.
- [ ] Demo routes/defaults/help/tests cleaned up without wiping user data.
- [ ] Report pages freeze consistently and redact secrets; live recording stops correctly.
- [ ] Search/sort/filter state and result scope correct; missing data handled.
- [ ] Cache expiration, network concurrency and recovery remain bounded.
- [ ] Actual release certificate compared against the installed fingerprint; data-preserving upgrade checked.
- [ ] Tests/build/minified smoke test completed and results recorded, with any limitations.
- [ ] README, architecture, troubleshooting, known limitations, QA and release notes agree with final behavior.
- [ ] Version/code incremented; versioned APKs/checksums/signing details published only in the implementation/release phase.

## 12. Start here in the next development thread

Read this document and reconcile current main. The first milestone is **smooth browsing plus useful photo-friendly diagnostics**, followed by demo removal and About polish. Preserve the working WebView movie/series playback. Treat dynamic discovery as a verified-data-source enhancement, not a reason to increase startup work or invent trending data.

Once the owner authorizes implementation, establish a repeatable baseline and add the minimum instrumentation needed to diagnose the browsing lag. Implement and verify in small phases, keeping confirmed code findings distinct from device measurements and unresolved hypotheses.

