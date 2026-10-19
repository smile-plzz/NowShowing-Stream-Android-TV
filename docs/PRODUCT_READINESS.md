# Product direction and publication status

Reviewed 19 October 2026.

NowShowing TV is a standalone, remote-first Android TV discovery and provider-viewing app for Ismail Hossain. Its primary job is helping someone find a title, save it, open its provider and return comfortably on a modest TV.

## Product decisions
1. Preserve the functioning provider playback path while making browsing lighter and recovery clearer.
2. Provide two title-entry methods and a navigable local watchlist rather than requiring long D-pad typing or scrolling.
3. Keep curated discovery labeled accurately. OMDb title/type/year search does not establish live popularity, newly released rankings or plot-keyword discovery.
4. Keep progress honest: web page loading is not measured video progress or completion.
5. Keep diagnostics opt-in, local and readable in photographs.
6. Publish exact build/version/checksum/certificate evidence with every downloadable candidate.

## Candidate scope
1.6.0-rc.2 carries forward the rc.1 performance and diagnostics work, adds system-keyboard search and local library organization, and retains the existing playback architecture. It makes no database schema change.

## Remaining publication gates
- Compatible signing: match the owner's installed certificate before claiming an in-place upgrade. Neither development signing nor merely configuring a permanent key satisfies this by itself.
- Real TV: confirm focus, keyboard behavior, browsing smoothness, photo pages and playback on the Hisilicon Android 14 device.
- Stable designation: automated tests/builds establish build health, not the above device results. Development-signed packages must remain prereleases.

Phone-to-TV communication is a separate TV Bridge product. It is not added as an unverified dependency here.
