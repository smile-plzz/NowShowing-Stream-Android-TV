# TV polish and viewing history — October 18, 2026

## Scope
Keep the existing web-provider playback path and all discovery routes. This pass improves loading, remote navigation, and local viewing convenience. It does not extract provider streams or attempt native iframe playback.

## Viewing history
- Home has a Recently opened shelf with the last 30 opened web-provider titles.
- Reopen uses the remembered episode and per-title source. Opened means the provider page was requested, not confirmed video playback.
- Manage history offers remove, Mark watched, and Mark unwatched. Watched is an explicit manual title-level label, including for series.
- Per-title source preferences are shared by Details and Player. Invalid or removed source IDs fall back to the global preference.
- Native Continue Watching remains separate and removable. Web-player timestamps and subtitles remain provider-managed.
- Reopening a title or another episode preserves the manual watched label until explicitly changed.
- History stays on the TV. Removing a history entry clears its remembered episode; source preferences remain separate.
- SQLite schema v2 adds recently_opened without dropping or rewriting v1 tables.

## Loading and performance
- Home publishes each shelf independently and reads local history without waiting for metadata requests.
- Previously displayed content remains while refresh runs. Refresh shelves retries unavailable metadata.
- A shared 128-entry access-order metadata cache expires entries after 24 hours.
- Disk cache expiry remains authoritative on disk reads. Matching requests use bounded striped mutexes; metadata network requests are limited to two at a time.
- Title fetches also populate IMDb-keyed cache entries for navigation into Details.
- Shelf results deduplicate IMDb IDs. Superseded search, browse, title, and season jobs are cancelled where applicable; cancellation propagates through repository network operations.
- Details displays title metadata before season/recommendation work finishes.
- Poster placeholders are static instead of running one shimmer animation per loading poster; focus scale/shadow are softer.

## Remote UX
- Navigation entries save the selected card/action and lazy-list/grid position.
- Home, browse, search, watchlist, and title cards restore selection when returning from details/playback; title Play and episode actions are included.
- Invalid/removed targets fall back to regular D-pad focus behavior.
- Play now enters provider pointer input directly; series controls prominently expose Next episode. Provider-required interaction remains provider-dependent.

## Verification
Automated: existing JVM suites plus cache LRU/TTL/replacement/concurrency and history-label regression tests. GitHub Actions assembles debug and release APKs.

Real TV checks:
1. Cold launch: home remains navigable while shelves arrive; valid cached data works with network unavailable.
2. Open a movie, choose a source, return Home, reopen from Recently opened; source is retained.
3. Open S2 E3, then reopen from history; episode is retained. A manual selection of another episode still opens that episode.
4. Mark watched/unwatched and remove entries; relaunch and confirm persistence.
5. Navigate deep into a shelf/grid, enter Details, return; verify scroll and focus. Repeat Details -> Player -> Back.
6. Rapidly submit different searches and filters; the newest request wins.
7. Upgrade an existing v1 install with the same signing key and confirm watchlist/native progress remain intact.
8. Regression-test web playback, source switching, fullscreen/Back, safe rejection of removed diagnostic-stream routes, moods, and watchlist.

## Distribution
Use the PR's successful build artifacts. APK installation updates require the same signing certificate as the installed version. The current workflow uses a development signing fallback when no release keystore is configured; different runner-generated certificates can require reinstalling. A stable production signing key remains a separate release-operations task.
