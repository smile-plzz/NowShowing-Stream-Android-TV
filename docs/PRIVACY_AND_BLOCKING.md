# Privacy and request filtering — 1.5.1

## Network behavior
OMDb receives metadata queries/IMDb IDs and the selected API key. Poster hosts receive image requests. Movie/series web providers and their HTTPS frames run inside Android System WebView and may receive normal browser information such as IP address, cookies and user agent. Filtering reduces some requests; it does not make third-party playback private or guarantee removal of every advertisement.

There is no new app analytics service, viewing-history upload, metadata backend or account system. Watchlist, recently opened, measured native progress, manual watched labels and settings stay on the TV. Provider-side cookies/history remain subject to that provider and WebView behavior; clearing app metadata/image caches does not promise to erase browser site data.

## Credentials and diagnostic reports
One bundled public demo OMDb key or an ordered list of up to five authorized user keys is used. Personal keys are stored in local app preferences, excluded from Android cloud backup and device transfer; embedded APK keys are not secrets. Invalid/inactive keys fall back automatically; quota multiplication is not implemented. Additional owner-supplied keys are not published or embedded. HTTP metadata logging is disabled so query credentials do not enter request logs.

The in-app error log retains at most 12 metadata timestamp/category records in process memory, not raw exceptions or URLs. Settings diagnostic reports include version/code/commit, build variant, device model, Android/API, WebView version, public certificate fingerprint, cache sizes and an app heap snapshot. They exclude viewing history, title names, API keys, provider URLs, tokens and device serial numbers. Copy/share occurs only when the user activates it. The app opens Android's share chooser rather than uploading automatically. Review the visible report before sharing; it contains device/build information.

## Embedded provider protections
- HTTPS transport and no mixed content; certificate errors use default rejection.
- Popup windows and permission requests are denied. Downloads and external navigation intents are not enabled.
- File/content access is disabled. No privileged JavaScript-native bridge or browser extension engine is exposed.
- Main-frame navigation stays on the selected host when isolation is enabled; HTTPS child frames remain possible for playback.
- Intercepted requests pass through configured ad/tracker rules unless a user explicitly exempts that source.
- WebView pauses/resumes with its screen and is stopped/destroyed when replaced or left.

These controls apply within the app's supported WebView request surface. The app does not embed Brave, run a VPN, require root or provide full Brave Shields/uBlock/Adblock Plus compatibility. Rules not supported by ContentBlockEngine do not gain functionality merely by being present in a downloaded list. Provider playback can still need interaction or fail under filtering.

## Filter lists and updates
Bundled rules live in `app/src/main/assets/filters`. Manual online updates retrieve the maintained Android repository's raw files:

- `https://raw.githubusercontent.com/smile-plzz/NowShowing-Stream-Android-TV/main/app/src/main/assets/filters/filters_adblock.txt`
- `https://raw.githubusercontent.com/smile-plzz/NowShowing-Stream-Android-TV/main/app/src/main/assets/filters/filters_tracking.txt`

Both URLs were retrieved successfully during this development pass. Each response is closed and capped at 512 KiB. Validation rejects HTML, empty/short lists and unsupported line shapes before writing. AtomicFile replacement and read-back validation detect failures; initialization also validates downloaded files and can recover interrupted replacements or fall back to bundled rules. Initialization/update serialize; the engine replaces its rule snapshot safely.

Lists update independently. COMPLETE means both replacements succeeded; PARTIAL means one changed and the failed list retained its known-good rules; FAILED means no successful replacement. Failure is not presented as “already up to date.” No scheduled filter updater, signed-list distribution or remote checksum verification is claimed.

## Local data recovery
Metadata retention is bounded to 500 payloads of at most 64 KiB, with expiry plus up to 30-day stale fallback during service failure. Image caches have separate bounds. Clear metadata & image caches preserves watchlist, history, manual labels and settings. Clearing app storage or uninstalling erases local data; it is a different operation. See ARCHITECTURE.md and KNOWN_LIMITATIONS.md.
