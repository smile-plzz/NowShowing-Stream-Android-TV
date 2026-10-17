# Playback restoration — October 17, 2026

## Why selected movies did not play
The native-only implementation classified all eleven existing movie providers as unsupported. Its native alternatives are fixed demo URLs, independent of the selected IMDb title. Changing to a demo cannot resolve a selected movie.

## Current playback contract
- Existing web providers use an embedded Android System WebView, using the existing IMDb, season, and episode URL builder.
- Direct HLS, DASH, and MP4 sources retain Media3 and its existing native controls.
- Fresh installs and missing saved source names default to the first existing web provider. Explicit saved demo preferences remain available.
- Demo streams are labeled separately; they are playback diagnostics, not a movie catalog.
- No stream extraction, DRM bypass, external app launch, or new provider service is introduced.
- The legacy UNSUPPORTED_WEB_EMBED enum name is retained for compatibility with the native resolver. It means incompatible with Media3, not unavailable to the application.

## Remote control
Select Enter player to focus the player. D-pad moves a temporary pointer; OK clicks at the pointer, including inside cross-origin frames. Back or Menu reveals the app controls; Back again leaves. Provider fullscreen views use Android's custom-view callback. Native source selection returns to the existing native player. Web controls offer reload, source selection, and previous/next episode.

## Protection and lifecycle
HTTPS only; no mixed content, file access, content access, JavaScript-native bridge, downloads, permission grants, or external intents. New windows are denied. Main-frame navigation stays on the selected host while redirect isolation is enabled. HTTPS child frames remain possible for provider playback hosts. Existing source exceptions and filtering preferences apply to intercepted requests. Certificate errors retain WebView's default rejection. Filter evaluation and rule replacement are synchronized, and cached decisions include protection modes.

WebView pauses/resumes with the screen lifecycle and is destroyed on source changes or leaving. Filtering is limited to requests exposed by WebView; it is not equivalent to Brave Shields or a guarantee of removing every advertisement.

## Honest limitations
Provider pages can fail, change endpoints, be blocked by a network, require unsupported browser features, or have no selected title. Successful HTML loading does not prove that video plays. A 25-second timer brings controls back without claiming video success. Try another provider when needed.

Cross-origin provider video state is not exposed to the app: web playback does not create native Continue Watching records, claim measured resume, or trigger automatic next-episode countdowns. Use the provider's own subtitles and playback controls; manual episode navigation remains available. Native progress/resume is unchanged except that saved positions must match the requested season and episode.

## Validation
Navigation policy tests cover provider host confinement, HTTPS iframe compatibility, misleading host suffixes, unsafe schemes, user-info URLs, and isolation preference behavior. GitHub Actions runs existing tests and assembles debug/release APKs for the PR.

Before release, test on a real Android TV: at least one movie and two series episodes; D-pad pointer; provider play/seek/subtitle controls; fullscreen and Back; app background/foreground; source change during load; unreachable source; popup blocking; protection preferences and exceptions; native demo playback; home/search/watchlist/mood navigation.

## Product direction
This document supersedes earlier native-only requirements in historical handoffs. The UI keeps its routes and features, with warm charcoal surfaces, ivory typography, muted terracotta accents, and clear remote focus.
