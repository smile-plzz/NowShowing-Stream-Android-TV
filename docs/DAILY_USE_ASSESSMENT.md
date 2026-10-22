# Daily-use assessment — 22 October 2026

Baseline: main at dd28ca214f9d60b35921127ee3d8ac7578a3741a. Its Android CI passed. The latest versioned prerelease at assessment time was v1.6.0-rc.2; rc.3 refinement was already merged. This pass targets 1.6.0-rc.4 / 16003.

## Findings and changes

| Area | Observed source behavior | Change |
| --- | --- | --- |
| Search recovery | Retry always restarted page one after a Load more failure. | Retry the failed next page while preserving earlier results. |
| Search responsiveness | Busy state was set after a database write; backspace retained obsolete errors; changing type with an empty draft retained old results. | Publish request state before IO and clear inconsistent filter/error state. |
| Series details | Failed or empty episode metadata silently removed the episode row. | Show loading, an explanatory empty state and Retry episodes. Sort/deduplicate positive episode numbers and reconcile saved selections with the available list. |
| Player lifecycle | Reinitialization copied prior state and did not cancel the old episode-list request. | Start a fresh title state, cancel episode loading and verify title/season before publishing. |
| Watchlist | A removal refresh could be dropped while an older load was active; rapid toggles could overlap. | Supersede old snapshots and allow one toggle at a time, updating only the matching title. |
| Return focus | Search and watchlist relied on title/poster-derived focus keys. | Use stable IMDb-ID keys. |

The existing mood/recommendation work, WebView providers, privacy filtering and metadata credential behavior are preserved. No speculative feed, new service, background polling or dependency upgrade was added.

## Validation

- Eight release packaging/integrity Python tests pass locally.
- Seven new Robolectric regression tests cover pagination retry, immediate busy state, blank-draft filter changes, error clearing, episode failure/retry, season loading/validation and series-to-movie reset.
- Android tests and both APK variants must pass the existing PR workflow before merging. CI evidence and artifacts are linked from the PR and Actions pages.
- Local Android execution is unavailable in this environment; no physical-TV or live-provider playback pass is claimed.

## Candidate TV checks

1. Install NowShowing Preview alongside the existing app. Search, load another page, temporarily disconnect, retry, reconnect and confirm earlier cards remain.
2. Open a series, change seasons quickly and confirm the selected season owns the displayed list. With unavailable metadata, confirm Retry episodes is visible and works after recovery.
3. Return from details to the selected search/watchlist card. Check two titles with identical display names if available.
4. Toggle Watchlist quickly, leave and return; confirm the saved state agrees between screens.
5. Play a series, return, then open a movie. Confirm no old episode title/list or stale provider page appears. Verify remote controls, source switching and manual Next episode on the real TV.

This is a prerelease candidate. Existing signing-certificate continuity and real-TV acceptance requirements still apply; a successful build is not proof of smooth playback on the owner's device. Web provider completion detection and exact progress remain unsupported as documented.
