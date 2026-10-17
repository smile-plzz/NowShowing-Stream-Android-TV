> Current implementation: see ARCHITECTURE.md and WEB_PLAYBACK.md. Filtering is limited to supported rule syntax and requests exposed by WebView. This is not full Brave Shields compatibility. Updates use the maintained Android repository, bounded validation and atomic per-list replacement; partial/failed updates retain known-good rules. API URL logging is disabled. A personal metadata key is stored in local preferences, not a secret backend. Diagnostic export excludes keys, URLs and viewing history and is initiated by the user; no new analytics/upload service is introduced.

> **October 17, 2026 playback update:** See [WEB_PLAYBACK.md](WEB_PLAYBACK.md) for the current dual-engine playback contract. Earlier native-only / zero-WebView requirements below are historical and superseded. Real-provider TV testing remains required.

# Privacy & Network Isolation: NowShowing TV

## 1. Overview & Vision
NowShowing TV incorporates a built-in, native content and tracker blocking architecture inspired by **Brave Shields**. Designed specifically for Android TV and Google TV, it operates natively within the application runtime without requiring external browser installations, browser extensions, root permissions, or VPN services.

Following the migration to **Mandatory Native Playback via AndroidX Media3 / ExoPlayer**, WebViews and embedded third-party browser players have been eliminated from the playback pipeline. Consequently, the privacy and content blocking subsystem protects the application at the **network resolution and metadata query boundary**:
- Validates all media source redirect chains (capped at 5 hops).
- Strictly enforces HTTPS transport and rejects non-web URI schemes (`javascript:`, `data:`, `intent:`, `market:`).
- Evaluates destination domains and hostnames against Adblock Plus (ABP) and EasyPrivacy-inspired blocklists before network requests are initiated.
- Intercepts telemetry, analytical beacons, and ad network calls during metadata fetching and catalog discovery.

```
                     ┌────────────────────────────────────┐
                     │ Stream Request (ExoPlayer/Network) │
                     └─────────────────┬──────────────────┘
                                       │
                                       ▼
                     ┌────────────────────────────────────┐
                     │ SourceValidation & Redirect Guard  │
                     └─────────────────┬──────────────────┘
                                       │
                     ┌─────────────────┴──────────────────┐
                     ▼                                    ▼
           [Non-HTTPS / Invalid Scheme?]        [Redirect Depth > 5?]
                     │                                    │
                     ├─────────────── YES ────────────────┘
                     │                                    
                     ▼ (Reject)                           
             [Block & Isolate]                            
             - Record Threat / Redirect                   
                     │                                    
                     ▼ NO (Clean Scheme)                  
       ┌───────────────────────────┐                      
       │  ContentBlockEngine       │                      
       │  - ABP Domain Match       │                      
       │  - Whitelist Rules (@@)   │                      
       │  - Path / Regex Patterns  │                      
       │  - Sub-ms LRU Cache       │                      
       └─────────────┬─────────────┘                      
                     │                                    
             ┌───────┴───────┐                            
             ▼               ▼                            
        [MATCHED]       [CLEAN / NO MATCH]                
             │               │                            
             ▼               ▼                            
     [Block Request]    [Allow Request]                   
     - Record Local Stat  - Proceed to ExoPlayer / HttpEngine
     - Threat / Tracker                                   
```

## 2. Core Architectural Components

The privacy and network isolation subsystem is located under `com.nowshowing.tv.privacy`:

### 2.1. `ContentBlockEngine.kt`
- **Rule Parser & Evaluator**: Supports Adblock Plus (ABP) syntax including domain anchors (`||doubleclick.net^`), wildcards (`*popunder*`), path rules (`/ad/banner/*`), and whitelist exceptions (`@@||autoembed.co^`).
- **Sub-Millisecond Decision Cache**: Utilizes a synchronized, bounded LRU cache (1,000 entries) ensuring repeat evaluations take under 0.05ms, preventing frame drops or remote lag on television chipsets.
- **Fast Domain Indexing**: Exact domain matches and reverse-domain suffix lookups execute in $O(1)$ and $O(k)$ time where $k$ is domain depth.

### 2.2. `FilterListManager.kt`
- **Offline Baseline**: Ships with curated, compact filter lists bundled directly in app assets (`filters_adblock.txt` and `filters_tracking.txt`), derived from established open-source rules (EasyList and EasyPrivacy concepts).
- **Online HTTPS Updates**: Supports on-demand and periodic background updates over HTTPS with atomic file replacement and checksum verification.
- **Fail-Safe Fallback**: Retains the last known working ruleset if an update attempt fails or times out.

### 2.3. `SourceValidation.kt` & Redirect Isolation
- **Redirect Chain Capping**: Automatically limits redirect chains to a maximum of 5 hops to prevent recursive redirect hijacking and CPU exhaustion.
- **Protocol Enforcement**: Enforces `https://` on all resolved media endpoints and rejects insecure cleartext HTTP unless explicitly whitelisted.
- **Threat & Hijack Isolation**: Rejects unsafe URI schemes including `javascript:`, `data:`, `intent:`, `market:`, `tel:`, and `blob:`.
- **Live Counter Integration**: Feeds security events directly into `BlockingStatistics` (`recordThreatBlocked`, `recordTrackerBlocked`, `recordRedirectBlocked`).

### 2.4. `ProtectionPreferences.kt` & `BlockingStatistics.kt`
- **User Settings**: Persists toggles for Content & Ad Blocking, Tracker & Telemetry Protection, Strict Redirect & Domain Validation, and Filter List Auto-Updates.
- **Per-Source Exceptions**: Allows users to temporarily or permanently disable protection for specific streaming providers if a feed requires specialized headers.
- **Local Diagnostics**: Tracks local counters for Threats Blocked, Trackers Blocked, and Redirects Isolated. Zero telemetry or personal data is collected or transmitted externally.

## 3. Television User Experience & Settings

### 3.1. Television Settings Screen
Accessible directly via the TV Top Bar ("Settings" tab):
- **Toggles**: Content & Ad Blocking, Tracker & Telemetry Protection, and Strict Redirect & Domain Validation.
- **Live Shield Metrics Card**: Displays real-time counts for Threats Blocked, Trackers Blocked, and Redirects Isolated.
- **Maintenance Actions**: "Check for Filter Updates" (with live timestamp feedback) and "Reset Protection Statistics".
- **Hardware Optimization**: Built with 100% Android TV D-pad focusability and visual highlight borders for 10-foot remote interaction.

## 4. Privacy & Compliance Boundaries
1. **No Paywall or DRM Circumvention**: The engine does not decrypt protected media or bypass provider authentication. It operates strictly as a network safety and privacy filter.
2. **Zero Remote Logging**: All blocking metrics and preference overrides remain on the local television storage.
3. **Open-Source Attribution**: Bundled rulesets credit and adhere to open-source EasyList and EasyPrivacy licensing standards.
