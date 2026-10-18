# Known limitations — 1.5.1

- Movie/series sources are external web/iframe providers inside WebView. Native diagnostic demos are independent of selected IMDb titles. No stream extraction or DRM bypass is implemented.
- Web completion/progress is not integrated. Default-on autoplay does not automatically advance these providers; use manual Next episode. No web timestamp resume or completion-driven related panel is claimed.
- Recently opened means a page was opened. Manual Watched is a title-level label, preserved across reopening any episode. Measured native Continue Watching is separate.
- Provider play buttons, seeking, subtitles, catalog, availability and fullscreen quirks remain provider-dependent. Page loading does not prove video playback.
- Next episode and season boundaries need OMDb metadata. When unavailable, the app explains the limitation and supports retry/explicit selection; it cannot verify the provider's catalog or future episode availability.
- Up to five locally supplied OMDb keys are supported. Invalid/inactive keys fall back in order; quota, network, service and no-results failures do not rotate credentials. Additional supplied keys are not published in source/APKs and require local configuration. Public/demo credentials can reach quota.
- Expired cached metadata is a temporary service-failure fallback, up to 30 days after expiry. Cached related titles may be absent on a fresh device. Shelves are curated picks rather than real-time popularity rankings.
- Filtering handles exposed WebView requests and supported rule syntax; it is not Brave Shields and cannot eliminate every intrusion. Failed updates keep known-good rules.
- Cache clearing preserves user data. SQLite payload sizes differ from database file allocation; no forced VACUUM runs during navigation. Loaded screens and in-flight image work can retain or repopulate content.
- No accounts, cloud sync, downloads or history backup/restore. Diagnostic reports exclude viewing history and secrets but include device model and public certificate; review before sharing. Some TVs have no clipboard consumer or share target; the report remains visible.
- PR builds use development signing. Permanent-signing secrets and same-certificate upgrades are not proven by source inspection or CI success. Certificate mismatch prevents in-place upgrades; do not uninstall without understanding data loss.
- Memory/image/network bounds are implemented; no real-TV before/after profiler measurements are available. Device-wide slowness may remain. The previously successful owner playback test predates this candidate.
- Google Play publication, Fire TV certification, Tizen and webOS are outside this release.
