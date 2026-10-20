# Product direction and publication status

Reviewed 20 October 2026.

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

## Side-by-side TV testing
The `-debug.apk` download installs as **NowShowing Preview** (`com.nowshowing.tv.preview`), with separate local storage. Use this candidate to test alongside the existing NowShowing TV installation. Preview starts with its own empty watchlist/history/settings and does not migrate the installed app's data. The `-release.apk` retains `com.nowshowing.tv` and requires a matching certificate for an in-place upgrade. Preview is a debug test build; its performance is not evidence of minified release performance.

## Latest owner review and next milestone

The owner reports substantially smoother browsing and manageable current ads. Preserve that experience. The latest feedback did not identify an exact installed version/variant/certificate or complete every acceptance check, so stable-signing and remaining TV gates are not automatically closed.

The current next-step specification is [Final product refinement handoff](FINAL-PRODUCT-REFINEMENT-HANDOFF.md): clearer emotion/scenario mood phrases and specific additions, a compact year picker, refreshable IMDb-ID collections while retaining OMDb, more relevant More Like This, and creator information/social links in About. Automatic ID-feed sourcing remains unresolved. Cast photos, new movie APIs and ad/player rewrites are excluded. **This is a documentation plan; application development remains on hold until Ismail explicitly authorizes it.**
