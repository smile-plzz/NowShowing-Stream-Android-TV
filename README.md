# NowShowing TV

A standalone movie and series discovery app for Android TV / Google TV, by Ismail Hossain.

**Sideload prerelease: 1.6.0-rc.2 · Android version code 16001**

Version 1.6.0-rc.2 is the current sideload candidate. Stable upgrade acceptance remains pending. See [App health and TV measurements](docs/APP_HEALTH.md).

[Published APK releases](https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases) · [Build runs and candidate artifacts](https://github.com/smile-plzz/NowShowing-Stream-Android-TV/actions/workflows/build-apk.yml) · [Installation](docs/INSTALLATION.md)

Download the APK from the versioned GitHub Release. Releases include both APK variants, checksums, public certificate details and exact build information. Development-signed downloads are explicitly marked as prereleases; they are usable sideload builds, not a claim of permanent signing.

## Final UI polish
- Scrollable remote navigation keeps every destination reachable on narrower TV layouts.
- Settings is grouped into Playback, Metadata, Privacy, Help, App health and About.
- Readable, scrollable diagnostic reports and disabled busy actions provide clear feedback.
- Personal keys save explicitly; switching back to the demo key is a separate action.
- Browse/Mood recovery states explain failures. First-use player hints disappear after eight seconds; remote help remains available.

## Browsing and discovery

Home/Browse show fresh cached cards first and publish incremental results with two global network slots. Refresh retains populated grids. Browse sorts and genre/start-year filters apply only to the loaded curated collection, not a complete catalog. Search supports title words, type and optional release year with Load more. Live popularity/new-release rankings and plot-keyword discovery are deferred; curated seeds are not live rankings.

## Daily TV use
- System-keyboard title entry alongside the D-pad keyboard; scrollable search controls and result counts.
- Watchlist filters for movies/series, sorting by saved date/title/release year, and a shortcut to find titles. All operations stay local.
- D-pad browsing, search, watchlist, three genre shelves and curated featured/movie/series picks.
- Title details initially focus Play; returning restores the selected action/card and saved list position.
- Play enters the web provider directly. Arrows move the pointer; OK clicks; Back exits provider fullscreen, then opens app controls, then returns to the title.
- Next episode is prominent in series controls. Season boundaries use known episode metadata; missing lists offer recovery rather than inventing episode numbers.
- Autoplay defaults on and persists in Settings. Current web providers have no verified completion integration, so **web episodes still require manual Next episode**. No native demo streams are exposed.
- Recently opened remembers episode/source. Manual Watched is a title-level label and survives reopening; it does not measure playback.
- Related titles reuse cached candidates sharing genres, with no recommendation backend or extra metadata requests.
- About has owner/source/releases/issues links. App health has opt-in browsing recording, frozen numbered photo pages, live memory and one reopenable/deletable local report; scoped cache recovery remains in Metadata.

## Metadata and privacy
OMDb provides metadata using a bundled public demo credential or up to five user-supplied authorized keys stored locally on the TV. Invalid/inactive credentials fall back in order. Quota, network, service and no-results responses do not rotate keys; cached content remains available. The APK cannot keep embedded credentials secret. API URL logging is disabled.

Quota, invalid key, network, service failure and no-results are distinguished. Retry is bounded; successful metadata remains cached. Expired metadata may be used for up to 30 days after expiry during service failure and is identified in Home/Diagnostics. Metadata storage is bounded independently of saved user data.

Movies/series use **Android System WebView**. All demo media playback has been removed. Media3/resolver infrastructure remains internally tested, without a registered native source. Page loading does not establish video playback, progress or completion. Request filtering and denied popup windows reduce some intrusions; filtering is not Brave Shields and does not remove every ad.

## Build and test
JDK 21, Android SDK 35, minimum device API 26.

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug assembleRelease
python3 -m unittest discover -s scripts -p 'test_*.py'
```

Kotlin, Compose, ViewModels/StateFlow, Retrofit/OkHttp, Coil, SQLiteOpenHelper, WebView and Media3. JVM tests include Robolectric storage/repository/filter recovery tests. CI packages both APKs, checksums, public certificate information and exact build commit. PR builds use development signing and do not publish releases.

## Documentation
- [Next development handoff](docs/FINAL-PRODUCT-REFINEMENT-HANDOFF.md) — latest owner feedback and final refinement plan; implementation remains on hold until explicitly authorized.
- [Architecture](docs/ARCHITECTURE.md) · [Web playback](docs/WEB_PLAYBACK.md)
- [Remote controls](docs/REMOTE_NAVIGATION.md) · [Troubleshooting](docs/TROUBLESHOOTING.md)
- [Known limitations](docs/KNOWN_LIMITATIONS.md) · [Privacy and blocking](docs/PRIVACY_AND_BLOCKING.md)
- [Candidate release notes](docs/releases/v1.6.0-rc.2.md) · [Signing](docs/RELEASE_SIGNING.md)
- [Release checklist](docs/RELEASE_CHECKLIST.md) · [Real-TV acceptance](docs/TV_ACCEPTANCE.md)

Historical design documents are retained for context and are not the current implementation specification.

## Side-by-side TV testing
The `-debug.apk` download installs as **NowShowing Preview** (`com.nowshowing.tv.preview`), with separate local storage. Use this candidate to test alongside the existing NowShowing TV installation. Preview starts with its own empty watchlist/history/settings and does not migrate the installed app's data. The `-release.apk` retains `com.nowshowing.tv` and requires a matching certificate for an in-place upgrade. Preview is a debug test build; its performance is not evidence of minified release performance.

## Refinement in progress

The local refinement source targets **1.6.0-rc.3 / 16002**; it is not a published download. See [implementation status](docs/REFINEMENT_IMPLEMENTATION_STATUS.md) and [candidate notes](docs/RELEASE_NOTES_1.6.0-rc.3.md). The existing published candidate remains the installation baseline until a verified replacement is available.
