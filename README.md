# NowShowing TV

A standalone movie and series discovery app for Android TV / Google TV, by Ismail Hossain.

**Sideload release: 1.5.0 · Android version code 15003**

[Published APK releases](https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases) · [Build runs and candidate artifacts](https://github.com/smile-plzz/NowShowing-Stream-Android-TV/actions/workflows/build-apk.yml) · [Installation](docs/INSTALLATION.md)

Download the APK from the versioned GitHub Release. Releases include both APK variants, checksums, public certificate details and exact build information. Development-signed downloads are explicitly marked as prereleases; they are usable sideload builds, not a claim of permanent signing.

## Final UI polish
- Scrollable remote navigation keeps every destination reachable on narrower TV layouts.
- Settings is grouped into Playback, Metadata, Privacy, Help and About.
- Readable, scrollable diagnostic reports and disabled busy actions provide clear feedback.
- Personal keys save explicitly; switching back to the demo key is a separate action.
- Browse/Mood recovery states explain failures. First-use player hints disappear after eight seconds; remote help remains available.

## Daily TV use
- D-pad browsing, search, watchlist, three genre shelves and curated featured/movie/series picks.
- Title details initially focus Play; returning restores the selected action/card and saved list position.
- Play enters the web provider directly. Arrows move the pointer; OK clicks; Back exits provider fullscreen, then opens app controls, then returns to the title.
- Next episode is prominent in series controls. Season boundaries use known episode metadata; missing lists offer recovery rather than inventing episode numbers.
- Autoplay defaults on and persists in Settings. Current web providers have no verified completion integration, so **web episodes still require manual Next episode**. Native completion supports a cancelable countdown where applicable; native sources currently contain diagnostic demos only.
- Recently opened remembers episode/source. Manual Watched is a title-level label and survives reopening; it does not measure playback.
- Related titles reuse cached candidates sharing genres, with no recommendation backend or extra metadata requests.
- About, device/WebView/build diagnostics, copy/share report, and scoped cache recovery are in Settings.

## Metadata and privacy
OMDb provides metadata using a bundled public demo credential or one user-supplied authorized key stored locally on the TV. No key pooling is implemented. The APK cannot keep embedded credentials secret. API URL logging is disabled.

Quota, invalid key, network, service failure and no-results are distinguished. Retry is bounded; successful metadata remains cached. Expired metadata may be used for up to 30 days after expiry during service failure and is identified in Home/Diagnostics. Metadata storage is bounded independently of saved user data.

Movies/series use **Android System WebView**. **Media3 serves direct diagnostic demo streams**, not native versions of selected movies. Page loading does not establish video playback, progress or completion. Request filtering and denied popup windows reduce some intrusions; filtering is not Brave Shields and does not remove every ad.

## Build and test
JDK 21, Android SDK 35, minimum device API 26.

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug assembleRelease
python3 -m unittest discover -s scripts -p 'test_*.py'
```

Kotlin, Compose, ViewModels/StateFlow, Retrofit/OkHttp, Coil, SQLiteOpenHelper, WebView and Media3. JVM tests include Robolectric storage/repository/filter recovery tests. CI packages both APKs, checksums, public certificate information and exact build commit. PR builds use development signing and do not publish releases.

## Documentation
- [Architecture](docs/ARCHITECTURE.md) · [Web playback](docs/WEB_PLAYBACK.md)
- [Remote controls](docs/REMOTE_NAVIGATION.md) · [Troubleshooting](docs/TROUBLESHOOTING.md)
- [Known limitations](docs/KNOWN_LIMITATIONS.md) · [Privacy and blocking](docs/PRIVACY_AND_BLOCKING.md)
- [Release notes](docs/releases/v1.5.0.md) · [Signing](docs/RELEASE_SIGNING.md)
- [Release checklist](docs/RELEASE_CHECKLIST.md) · [Real-TV acceptance](docs/TV_ACCEPTANCE.md)

Historical design documents are retained for context and are not the current implementation specification.
