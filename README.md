# NowShowing TV

A movie and series discovery app for Android TV and Google TV, built by Ismail Hossain.

**Current candidate: 1.5.0-rc.1 · Android build 15001**

[Download APKs from GitHub Releases](https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases/tag/v1.5.0-rc.1) · [Installation](docs/INSTALLATION.md) · [Troubleshooting](docs/TROUBLESHOOTING.md) · [Known limitations](docs/KNOWN_LIMITATIONS.md)

## TV experience
- D-pad browsing, curated shelves, search, watchlist, and mood discovery.
- Existing movie/series web providers play inside Android System WebView; direct playback diagnostics use Media3.
- Precise pointer taps, accelerated held movement, first-use hints, and repeatable remote help.
- Simple playback menu with source selection, episode selection, reload, and explicit title navigation.
- Recently opened remembers each title's episode/source. Manual watched labels and removal controls stay local to the TV.
- Progressive shelf loading, bounded shared metadata caching, and remembered list/grid position and selected-card focus.
- Embedded request filtering, denied popup windows, HTTPS-only transport, and default certificate validation.

Page loading does not prove that a provider video is playing. Web-player subtitles, seeking, and timestamp resume remain provider-controlled. Native diagnostic videos are fixed demo streams, not substitutes for selected movies.

## Install and update
Android TV / Google TV API 26+. Download the **release.apk** asset, not the source ZIP or debug build. ADB users can install with:
```sh
adb install -r NowShowing-TV-1.5.0-rc.1-release.apk
```

This candidate uses development signing unless its build-info.json explicitly says permanent. A signing mismatch prevents in-place upgrades; uninstalling clears local data. See [signing and upgrade guidance](docs/RELEASE_SIGNING.md).

## Development
JDK 21 and Android SDK 35 are required.
```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug assembleRelease
python3 -m unittest discover -s scripts -p 'test_*.py'
```
The app uses Jetpack Compose, Media3, SQLiteOpenHelper, Retrofit/OkHttp, and Coil. OMDb supplies metadata using an included demo key subject to its quota.

## Releases
GitHub Actions runs tests and builds both APK variants. Authorized main commits marked [publish], or a manual main workflow run with publication enabled, produce versioned APK assets, SHA-256 checksums, public signing certificates, build information, and release notes. Development-signed builds are always prereleases. See [release operations](docs/RELEASE_SIGNING.md).

## Documentation
- [Remote controls](docs/REMOTE_NAVIGATION.md)
- [Installation/update guide](docs/INSTALLATION.md)
- [Known limitations](docs/KNOWN_LIMITATIONS.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)
- [Current web playback contract](docs/WEB_PLAYBACK.md)
- [Polish and history](docs/POLISH_AND_HISTORY.md)
- [Release notes](docs/releases/v1.5.0-rc.1.md)
- [Release checks](docs/RELEASE_CHECKLIST.md)
