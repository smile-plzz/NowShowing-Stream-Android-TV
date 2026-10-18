# Current playback contract — 1.5.0

Selected movies and series use existing provider HTML/iframe players in Android System WebView. Media3 remains available for direct diagnostic demo streams; choosing a demo does not play the selected movie.

## Startup and remote input
Pressing Play enters the provider page immediately, with pointer focus. There is no app-owned Enter Player/Start gate. Some providers still require an OK click on their own play button. `mediaPlaybackRequiresUserGesture=false` does not guarantee provider autoplay; no arbitrary DOM/pixel clicks or unverified autoplay URL parameters are injected.

Arrows move the pointer; held arrows accelerate; OK performs one pointer click. First-use hints appear without blocking playback. Back exits provider fullscreen first, then opens app controls, then returns to the title; submenus return to controls. Menu opens controls directly. Source changes/retry replace and destroy the old WebView. Controls include prominent Next episode for series, episode selection, retry, source switching and repeatable remote help.

## Episode progression
Autoplay is ON by default and persists in Settings. **Current web providers have no verified completion event integration. Automatic advancement is unavailable for them.** Neither iframe existence, page finished, elapsed time nor an apparently paused image proves video completion. There is no JavaScript-to-native bridge or accepted cross-origin message protocol.

Use manual Next episode. The app resolves known episode numbers, fetches the next season at a known boundary and recognizes the last known episode. Missing metadata yields a recovery message rather than selecting invented episode numbers. Metadata can lag the provider catalog; use explicit season/episode selection when necessary. Automatic post-movie recommendations likewise require a completion signal, so cached related titles appear on Details instead.

Native playback uses actual player events. A supported native series completion can show a ten-second countdown, Next episode and Dismiss; OFF leaves advancement manual. Events are tied to the current native source/session. Currently native catalog entries are fixed diagnostic videos, so they do not establish feature support for movies/series providers.

## Protection and lifecycle
HTTPS only; no mixed content, file/content access, downloads, permissions, external navigation intents or privileged JavaScript bridges. Popup windows are denied. Main-frame host isolation and request filtering follow settings/source exceptions. Default SSL rejection is preserved. HTTPS provider child frames remain possible.

WebView pauses/resumes with the screen and is stopped/destroyed on source replacement or leaving. Filtering covers only requests WebView exposes and the rule formats the engine supports. It does not promise full Brave Shields compatibility or complete ad removal.

HTML loading does not establish playback success. A 25-second page-loading timeout reopens controls without declaring video failure/success. Provider-required clicks, subtitles, seeking, fullscreen quirks and availability must be checked on the TV. See TV_ACCEPTANCE.md.
