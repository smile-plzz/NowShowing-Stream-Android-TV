> **October 17, 2026 playback update:** See [WEB_PLAYBACK.md](WEB_PLAYBACK.md) for the current dual-engine playback contract. Earlier native-only / zero-WebView requirements below are historical and superseded. Real-provider TV testing remains required.

# Playback Compatibility & Source Matrix: NowShowing TV

## 1. Native-Only Playback Architecture

NowShowing TV has eliminated embedded browser (`WebView`) video players in favor of a **100% native media playback engine** powered exclusively by **AndroidX Media3 / ExoPlayer**.

Third-party web embed interfaces commonly expose television viewers to intrusive pop-ups, arbitrary redirects, deceptive advertisements, and unstable browser navigation. NowShowing TV enforces a strict native-first experience:

```
[Select Movie/Episode] ──▶ [MediaSourceResolver] ──▶ [SourceValidation] ──▶ [ExoPlayer (Native TV)]
                                    │
                                    ├── If Native Direct Stream (HLS/DASH/MP4) ──▶ Native TV Playback
                                    │
                                    └── If Legacy Web Embed (iframe) ───────────▶ [Incompatible Error Screen]
                                                                                   "Native playback unavailable.
                                                                                    Try another source."
```

### Architectural Principles:
1. **Exclusive Native Engine**: Media3 / ExoPlayer is the sole in-app playback surface. No `WebView`, no HTML, and no JavaScript execution occurs in the player.
2. **Deterministic Resolution**: `MediaSourceResolver` evaluates candidate providers before playback. If a source only provides an unsupported web embed, it is immediately rejected without exposing the user to the web page.
3. **Redirect & Destination Isolation**: Network requests must use HTTPS (with explicit local-network IP exemptions for home NAS/DLNA media servers). Disallowed URI schemes (`intent:`, `market:`, `javascript:`, `file:`) and redirects to advertising or tracking hosts are blocked.
4. **TV-Native Ergonomics**: Complete D-pad remote control (Up/Down HUD toggle, Left/Right 10s seek with visual feedback pills, Center/OK Play/Pause, Next/Previous episode navigation, and a 10-foot source switcher dialog).

---

## 2. Source Classification & Audit Matrix

All 15 configured sources in NowShowing TV are classified into four distinct compatibility tiers:

1. **Native-Playable**: Directly exposes an authorized, standards-compliant HLS (`.m3u8`), MPEG-DASH (`.mpd`), or progressive MP4 stream.
2. **Requires Authorized Direct-Media Endpoint**: Configurable source requiring an authorized direct stream URL or user-configured endpoint.
3. **Unsupported Web-Only Embed**: Legacy third-party iframe embed page; strictly incompatible with native-only playback.
4. **Unavailable**: Offline, broken, or unresponsive endpoint.

| Provider / Source | Type | Endpoint Format | Native Media3 | TV Remote D-Pad | Classification Status | Technical Assessment |
|---|---|:---:|:---:|:---:|:---:|---|
| **Native HLS (Mux Stream)** | Native Stream | HLS (`.m3u8`) | ✅ 100% Native | ✅ Full D-Pad | **Native-Playable** | Multi-bitrate adaptive HLS stream with subtitle tracks. Hardware accelerated. |
| **Tears of Steel (HLS Demo)** | Sample Stream | HLS (`.m3u8`) | ✅ 100% Native | ✅ Full D-Pad | **Native-Playable** | Unified Streaming certified 1080p HLS stream. Verified ExoPlayer decoding. |
| **Big Buck Bunny (MP4)** | Sample Stream | Progressive MP4 | ✅ 100% Native | ✅ Full D-Pad | **Native-Playable** | Google TV reference progressive MP4 media stream. Immediate start, seekable. |
| **Sintel (MP4)** | Sample Stream | Progressive MP4 | ✅ 100% Native | ✅ Full D-Pad | **Native-Playable** | Blender Foundation high-definition progressive MP4 media stream. |
| **Custom Local / NAS Stream** | Custom Stream | HLS / DASH / MP4 | ✅ 100% Native | ✅ Full D-Pad | **Requires Direct Endpoint** | User or local network stream (e.g. `http://192.168.1.x:8080/movie.mp4`). |
| **VidSrc.to** | Legacy Web Embed | HTML / iframe | ❌ Incompatible | ❌ N/A | **Unsupported Web-Only Embed** | Obfuscated HTML player; no authorized direct HLS/MP4 URL provided. Rebuffed by resolver. |
| **AutoEmbed** | Legacy Web Embed | HTML / iframe | ❌ Incompatible | ❌ N/A | **Unsupported Web-Only Embed** | Responsive web player wrapper; no direct media endpoint. Rebuffed by resolver. |
| **2Embed.cc** | Legacy Web Embed | HTML / iframe | ❌ Incompatible | ❌ N/A | **Unsupported Web-Only Embed** | Third-party web embed; requires browser DOM/JS environment. Rebuffed by resolver. |
| **2Embed.stream** | Legacy Web Embed | HTML / iframe | ❌ Incompatible | ❌ N/A | **Unsupported Web-Only Embed** | Mirror of 2Embed; web iframe only. Rebuffed by resolver. |
| **VidSrc.icu** | Legacy Web Embed | HTML / iframe | ❌ Incompatible | ❌ N/A | **Unsupported Web-Only Embed** | Third-party web embed; no raw media stream available. Rebuffed by resolver. |
| **VidSrc.xyz** | Legacy Web Embed | HTML / iframe | ❌ Incompatible | ❌ N/A | **Unsupported Web-Only Embed** | Third-party web embed; no raw media stream available. Rebuffed by resolver. |
| **VidSrc.me** | Legacy Web Embed | HTML / iframe | ❌ Incompatible | ❌ N/A | **Unsupported Web-Only Embed** | Strict anti-hotlinking referrer checks; no direct media stream. Rebuffed by resolver. |
| **MoviesAPI** | Legacy Web Embed | HTML / iframe | ❌ Incompatible | ❌ N/A | **Unsupported Web-Only Embed** | Web embed aggregator; no direct media stream. Rebuffed by resolver. |
| **MultiEmbed** | Legacy Web Embed | HTML / iframe | ❌ Incompatible | ❌ N/A | **Unsupported Web-Only Embed** | Multi-server web embed container. Rebuffed by resolver. |
| **EmbedSu** | Legacy Web Embed | HTML / iframe | ❌ Incompatible | ❌ N/A | **Unsupported Web-Only Embed** | Third-party web embed; no raw media stream available. Rebuffed by resolver. |
| **VidSrc.mov** | Legacy Web Embed | HTML / iframe | ❌ Incompatible | ❌ N/A | **Unsupported Web-Only Embed** | Third-party web embed; no raw media stream available. Rebuffed by resolver. |

---

## 3. Resolution & Failure Protocol

When a user initiates playback for any movie or TV episode:

1. **Resolution Step**: `MediaSourceResolver.resolve(...)` inspects the selected provider.
2. **Incompatible Web Embed Handling**:
   - If the user selects any of the 11 legacy embed sources, the resolver rejects the request without opening a browser or rendering HTML.
   - The UI presents a clean, remote-navigable TV dialog:
     > **⚠️ Native Playback Unavailable**  
     > *Native playback is unavailable for this source (VidSrc.to). It is an unsupported web embed. Try another source.*
   - Two prominent, D-pad focusable action buttons are provided:
     - **⚡ Switch to Compatible Native Source**: Instantly switches to the next verified native HLS/MP4 stream.
     - **Choose from Source List**: Opens the full source drawer with compatibility badges.
     - **Back to Details**: Safely returns focus to the title details screen.
3. **No Automatic Browser Fallback**: Under no circumstance does the application fall back to launching an external web browser or rendering a WebView.

---

## 4. Redirect & Protocol Isolation

Native playback provides defense-in-depth against malicious network behavior:

- **HTTPS Requirement**: All external streaming endpoints must use `https://`. Unencrypted `http://` is strictly prohibited unless directed to a RFC-1918 private network host (`192.168.x.x`, `10.x.x.x`, `172.16-31.x.x`, or `localhost/127.0.0.1`) for local media streaming.
- **Redirect Loop Protection**: Network redirect chains are capped at 5 hops. Any chain exceeding 5 hops is terminated with an error.
- **Disallowed Schemes**: Schemes like `intent:`, `market:`, `javascript:`, and `data:` are rejected by `SourceValidation`.
- **Ad & Tracker Host Filtering**: Media request redirects are evaluated against `ContentBlockEngine` filter rules to ensure that a media server does not steer the player toward an advertising or tracking domain.

---

## 5. Security & Terms Compliance

NowShowing TV acts strictly as a metadata viewer and native client media player. It does not circumvent DRM, bypass CAPTCHAs, hijack private tokens, or scrape protected endpoints. Web-only providers that refuse direct media access are classified honestly as unsupported rather than bypassed through unauthorized means.
