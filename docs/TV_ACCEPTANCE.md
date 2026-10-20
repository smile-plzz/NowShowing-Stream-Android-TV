# Real-TV acceptance — 1.6.0-rc.3 (pending build)

Record installed version/code/commit from Settings, device model/Android/WebView, signing certificate, and whether this is a fresh install or same-certificate update. Do not uninstall a valued installation just to test: a signing mismatch needs a deliberate data-loss decision.

1. Open Settings → App health → Run health check / Freeze report. Copy/share the report where possible; otherwise photograph it. Check actual device information rather than assuming the earlier provisional TV specification.
2. With normal metadata access, browse Home/Browse/Search/Mood/Watchlist, open a title, confirm Play initially focused, then Back. Repeat after scrolling far down and sideways; verify card and scroll restoration. Select a series episode, enter playback, return and verify episode/source selection.
3. Test a deliberately invalid personal metadata key, then a valid authorized key if available. Confirm actionable key recovery on a fresh test installation, existing cached titles remain visible, and Search distinguishes failure from genuine no-results. Test offline/network timeout and Retry. Do not publish keys/screenshots containing typed credentials.
4. Play one movie and two series episodes. Confirm no app Enter Player gate; record any provider-owned required click. Test held arrows, single OK, fullscreen/Back/Menu, source switching, retry and leaving/reentering. Do not interpret page loading as video success.
5. Open series controls with Back: Next episode should be prominent and focused. Test a middle episode, a known season boundary and final episode. When metadata is unavailable, verify the explanation and Retry episode list. Disable autoplay, reopen Settings and confirm OFF persists. Web completion is currently unsupported: manual progression is expected even with autoplay ON.
6. Browse many titles for 10–15 minutes, repeatedly open/back, change sections and return after a long video. Record lag, missing posters, focus failures and crashes. Compare the same workload/build conditions. No speedup or memory-leak fix is presumed.
7. Mark a recent title Watched, reopen it (and another episode), and confirm the title-level label remains. Save a watchlist entry. Clear metadata/artwork caches in Settings → Metadata; verify watchlist, watched labels, history and autoplay setting remain.
8. Try filter update online and offline. Failure must say failed/partial and keep protection active. Confirm popup denial and usable provider playback with normal protection and any explicit source exemption.
9. Background/foreground and TV standby during playback; leave the player and verify audio/resources do not linger. Verify obsolete diagnostic routes cannot launch sample media. Photograph the App health pages and compare matched browsing recordings.

For a failure, supply: exact action sequence, title/source/season/episode voluntarily if relevant, installed build, approximate time, and a redacted diagnostic report. Viewing history is not collected in reports automatically.

Final UI checks: on a narrower layout, reach Settings from the top navigation; visit all six sections; verify blank Save personal keys is disabled; verify View/Copy/Share waits for a generated report; page through the frozen photo report with Previous/Next and close it; verify busy controls do not accept repeated requests. Automated Compose tests cover narrow navigation, section switching and disabled/key-entry actions. Actual TV visual/remote validation remains an owner check.

## Refinement candidate checks (not yet performed on TV)

- Mood phrases retain stable selection; try the six specific additions. Expect sparse local-collection results rather than global coverage.
- Year starts at All Years. Select an exact year, reopen to the selected row, cancel with Back, then choose All Years. Switch mood and Surprise Me; year stays selected. Opening Details and returning should restore the grid and title-card focus.
- Rapidly switch moods and years, retry offline and background the app. Old responses must not overwrite the latest selection. Repeated year changes make no metadata requests.
- More Like This may show fewer than six results or be absent. Shared broad genres or a shared actor alone must not populate it. Details/Play must appear first.
- Open About, read the creator bio and exact LinkedIn/Facebook addresses, and check no-browser feedback.
- Compare cold/warm browsing on the same installed variant before considering stable promotion. Retest movie/series playback separately.
