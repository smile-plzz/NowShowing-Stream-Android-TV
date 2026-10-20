# App health and browsing measurements

Open **Settings → App health**. Live heap/system-memory readings update every two seconds while this section is visible, stop on background/navigation, and pause while a frozen report is open. Heap is app Java memory, not total device RAM.

**Record browsing performance** attaches Android `Window.OnFrameMetricsAvailableListener` only after an explicit action. Navigate through Home, Movies, TV, Watchlist, Search and Details; stop manually or wait for the 60-second limit. Backgrounding also stops recording. Settings/player frames are excluded. Routes, title identifiers and input events are not retained. Recording is capped at 12,000 samples, then the listener and timeout are removed.

The recording uses `FrameMetrics.TOTAL_DURATION`, nearest-rank median/p95/p99 and a slow-frame count above one refresh interval at recording start. Missing metric callbacks are reported separately; they are **not** dropped display frames. Display refresh changes are not tracked. Zero samples means unmeasured, not healthy. Input latency, video FPS, CPU/GPU load and codec health remain unavailable. Instrumentation overhead has not yet been compared on the owner's TV.

**Run health check / Freeze report** stops recording and captures local app/build/device/WebView/certificate, metadata/cache/process counters and memory information on IO. It does not scan image folders, make a network probe, test video or upload data. Last successful metadata network time/latency is measured during actual requests; cache hits do not prove connectivity. Counters are process-local; detail-hit counters exclude progressive cache prepasses.

**View last frozen report** uses manual Previous/Next/Done controls with a repeated ID, version and original timestamp on every page. Long fields continue on extra pages without ellipsis. One report is stored locally and can be reopened after process restart or deleted. Capturing again replaces it. Live readings cannot change the open report. Copy remains secondary.

The report uses allowlisted fields: no API credentials, viewing history, provider URLs, account/device identifiers, raw exception text or private local paths. The public signing certificate is shown solely for comparison. Report classes retain their serialization fields in minified builds.

## Repeatable owner TV check

1. Record installed version/variant/certificate. Use the same network, cache state, refresh rate and thermal conditions.
2. Run a fixed route: Home shelves → Movies grid → Details → Back → Watchlist → Search → playback → return. Record cold and warm shelf population separately from frame measurements.
3. Repeat at least three times on baseline and candidate; compare median/tail distributions and sample counts. Repeat once with measurement disabled to assess overhead externally.
4. Verify focus/selection/scroll restoration, absence of freezes and sustained heap growth. Photograph all report pages and include report IDs in feedback.

No numeric improvement target is asserted before a real-TV baseline exists. Rendering changes, cache-limit changes, memory promotion and database rewrites remain evidence-dependent.

API references checked 19 October 2026: [Android FrameMetrics](https://developer.android.com/reference/android/view/FrameMetrics), [OMDb parameters](https://www.omdbapi.com/). OMDb supports `y` with title search; its documented search endpoint does not supply live trending/popularity or genre/plot discovery. No scraping or additional paid/key-dependent service was added.
