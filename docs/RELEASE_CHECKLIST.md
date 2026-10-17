# Release checklist — 1.5.0-rc.1

## Automated gates
- JVM tests, including pointer acceleration, navigation policy, metadata cache, and history labels.
- Python release-version parsing tests.
- Debug and minified release APK assembly.
- apksigner verification for both packaged APKs.
- Versioned names, SHA-256 checksums, certificate report, exact source commit, and Android version code.
- Publication only after a successful build; existing release versions cannot be overwritten.
- No private keystore/password in release assets.

## Honest release status
This version is a prerelease sideload candidate. No permanent signing secrets were configured when prepared. Configuring a permanent key and verifying an upgrade on a real TV remain owner release tasks; see RELEASE_SIGNING.md. CI success is recorded by its workflow run, not assumed by this document.

## Real-device acceptance
Prior web playback has been owner-tested. Verify the new changes before promoting to stable:
- Movie and series playback on the owner's TV, including one known-working source.
- Short pointer taps, held acceleration, OK without repeat clicks, and provider fullscreen.
- Fullscreen -> Back -> player -> Back -> app controls -> Back -> title.
- First-use tips, dismissal, reset, and help.
- Source/episode checkmarks versus remote focus.
- Episode metadata, season selection, and handling missing metadata.
- Returning to the selected browsing card and episode.
- Recent history/watchlist persistence on same-certificate upgrade.
- Settings diagnostics, Home, search, mood, and watchlist regression.
- Background/foreground, slow network, unavailable providers, and longer viewing.

Do not claim permanent-signing readiness, universal provider compatibility, or measured iframe progress until those capabilities are actually established.
