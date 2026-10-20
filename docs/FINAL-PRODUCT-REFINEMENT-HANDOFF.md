# NowShowing TV — Final product refinement handoff

Prepared: 20 October 2026 · Asia/Dhaka  
Owner: Ismail Hossain · [GitHub](https://github.com/smile-plzz)  
Repository: [NowShowing-Stream-Android-TV](https://github.com/smile-plzz/NowShowing-Stream-Android-TV)  
Reviewed application baseline: `6513131280b55eb9ad1ec90809d9882b6f71fa6b` on `main`  
Published candidate: **1.6.0-rc.2 / 16001**  
Status: **Owner authorized implementation on 20 October 2026 with “প্রসিড”. Local refinement source is prepared; Android validation and remote publication remain pending.**

See [implementation checkpoint](REFINEMENT_IMPLEMENTATION_STATUS.md) for the actual delivery state. The original start restriction below is historical and was satisfied by the latest owner message. Dynamic-feed and signing gates remain unresolved.

## 1. Read this first

The owner is finalizing a functioning product. During the latest TV review, he reported that the app feels substantially smoother and that the remaining ad is manageable. Preserve that experience. This milestone is a focused final refinement, not a new architecture, provider migration or visual redesign.

This document captures the owner's voice feedback, checks it against current source, and defines the next implementation milestone. It supersedes the older optimization handoff for **what to do next**; older documents remain historical references. Current source and release notes describe what is already shipped. Requirements in this document describe future work, not implemented features.

**Current authorization is documentation only. Do not change application code, run a new APK build, bump versions, publish, configure signing or start implementation until Ismail explicitly says to start.** Once implementation is authorized, proceed within the agreed scope without repeatedly asking for routine implementation choices. Keep the source-selection decision below explicit rather than silently adding a service.

## 2. Verified starting point

| Item | Reviewed state |
| --- | --- |
| Main source | `6513131280b55eb9ad1ec90809d9882b6f71fa6b` |
| Version | `1.6.0-rc.2`, version code `16001` |
| Release | [Published prerelease and assets](https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases/tag/v1.6.0-rc.2) |
| Publication checks | [Run 39346244205](https://github.com/smile-plzz/NowShowing-Stream-Android-TV/actions/runs/39346244205) completed successfully; tests, debug/release assembly, package identity, signing and bundle checks passed |
| Pull requests at review | None open; earlier PRs #7 and #8 are merged |
| Device target | Android TV / Google TV; min API 26, target/compile SDK 35, JDK/JVM 21 |
| Release package | `com.nowshowing.tv` |
| Debug package | `com.nowshowing.tv.preview`, displayed as NowShowing Preview, with separate storage |
| Playback | Android System WebView with existing providers; no registered native demo source |
| Metadata | OMDb; local memory/disk cache and two global metadata request slots |
| Shipped discovery | Curated Home/Browse seeds; title/type/year search; loaded Browse genre/start-year filters |
| Shipped library | Watchlist type filtering and saved/title/release-year sorting; history and manual watched labels |
| Signing | Published candidate is development-signed; compatible upgrade signing remains unresolved |

The new subjective smoothness feedback is valuable acceptance evidence. It is **not** a quantitative benchmark, proof of instrumentation overhead, or a confirmation of the exact tested version/variant/certificate. Record those before the next comparison. Do not keep treating performance as wholly untested, and do not claim every TV acceptance item has passed.

## 3. Scope and explicit exclusions

| Area | Agreed direction | Boundary |
| --- | --- | --- |
| Moods | Replace plain mood names with short, clear, emotion/scenario phrases; add more specific choices | The final direction is understandable phrases around three words, not vague slang such as “Delulu Era” |
| Mood year control | Small, minimalist, modern D-pad picker; default All Years | This is an exact-year filter, not forced newest-first sorting |
| Home / Movies collections | Refreshable IMDb ID lists; OMDb still supplies metadata and artwork | No TMDB or other additional movie API, paid AI, new account/key requirement or large backend by default |
| More Like This | Match core appeal: themes/subgenres first, tone/setting next; actors weaker | Fewer credible results instead of filler; no claims of full-catalog coverage |
| About | Clearly credit Ismail, include concise relevant bio and his social links | Use sourced URLs; keep TV layout readable |
| Playback and ads | Preserve working behavior | No ad-blocking expansion or provider/player rewrite in this milestone |
| Cast | Keep actor names | **Cast photos are excluded**, not a queued dependency |
| App health | Preserve existing diagnostics | No new diagnostics feature was requested in this feedback set |
| TV Bridge | Separate product | No new dependency or integration in this milestone |

Do not interpret the owner's initial “Gen Z” description as overriding his later approval of clearer phrases. Approved examples include **Need a Laugh**, **Let Me Cry**, **Keep Me Guessing**, **Scare Me Tonight**, **Make Me Swoon** and **Fuel My Rage**. The complete catalog below is a proposed implementation map, not a claim that every label was individually approved.

## 4. Repository findings and implementation touchpoints

All paths below are relative to the repository root.

| Files / symbols | Actual behavior and implication |
| --- | --- |
| `domain/mood/MoodModels.kt` under `app/src/main/java/com/nowshowing/tv/` | `MoodItem` has name/emoji/label/description. Current name also acts as a rule key. Add a stable internal identity or retain legacy keys independently of display copy. |
| `domain/mood/MoodRecommendationEngine.kt` | 29 moods; `MOOD_MAP` and text guessing use English name keys. Three randomly selected genre/keyword terms go to OMDb title search. Up to 24 candidates receive detail lookups; ranking counts shared genres, then rating. |
| `ui/mood/MoodViewModel.kt` | No year state. Cancels previous mood job; exposes retry, Surprise Me and text guessing. It retains old recommendation data while loading but the screen hides it behind a spinner. |
| `ui/mood/MoodScreen.kt` | Horizontal mood palette and lazy grid. Pills show `mood.name`, not `mood.label`. Simply changing the label field will not change the visible names. No year control or explicit saved grid state. |
| `data/api/OmdbApiService.kt` | Search already accepts `y` and type. Lookup by IMDb ID already exists. A movie API replacement is unnecessary. |
| `data/repository/MediaRepository.kt` | `TRENDING_SEED`, `POPULAR_MOVIES`, `POPULAR_TV` and `GENRE_ROWS` contain hard-coded **title strings**, not remotely updated IDs. Names such as TRENDING_SEED do not establish live rankings. |
| `MediaRepository.lookupTitlesProgressively()` | Cached-first, original seed order, deduplication and missing lookups in pairs, currently using title lookup/cache keys. An ID-based counterpart is needed for a future manifest; do not pass IDs as titles. |
| `ui/home/HomeViewModel.kt` | Featured, first three genre rows, movies and series are loaded through progressive title-seed lookup. Several launches are immediately joined, so shelf loading is staged; do not assume everything already loads concurrently. |
| `ui/browse/BrowseViewModel.kt` | Local sort/genre/year operations over hydrated curated titles. `startYear()` handles series year ranges. Feed updates must preserve these controls and explain collection coverage. |
| `MediaRepository.relatedTitles()` | Reads cached detail payloads, deduplicates IDs, excludes current title, requires same type and any genre overlap, then takes six. There is no thematic relevance score. |
| `data/db/NowShowingDatabase.kt` | `cachedDetailPayloads()` returns at most 100 fresh payload rows, ordered by expiry, before repository deduplication. Both title-key and ID-key entries can consume this bound. No complete recommendation catalog exists. |
| `ui/details/DetailsViewModel.kt` / `DetailsScreen.kt` | Calls relatedTitles after main detail becomes visible. More Like This is omitted if empty. Cast is currently plain names. Preserve quick title rendering and Play focus. |
| `data/model/OmdbModels.kt` | Available signals include plot, genre, year, director, actors, language, country, type and ratings. No structured theme, subgenre, tone, setting or cast-photo fields exist. |
| `ui/settings/SettingsScreen.kt` | About already includes creator name, build/version, GitHub/source/releases/issues links and browser-unavailable feedback. Extend it rather than replacing it. |
| `diagnostics/BrowsingRecorder.kt` | Opt-in recording includes Mood and Details as well as other browsing routes. Settings/playback frames are excluded. |
| `app/src/test/java/com/nowshowing/tv/domain/mood/MoodRecommendationEngineTest.kt` | An exact-29 test and old name/text mappings will need deliberate updates. Existing tests do not establish semantic recommendation quality. |

Source paths in the first column after the first entry use the same `app/src/main/java/com/nowshowing/tv/` prefix unless fully specified.

### The central relevance defect

OMDb documents `s` as **movie title search**, not genre, theme or plot search. Searching “Comedy,” “psychological” or “revenge” as title words cannot reliably discover films with those properties. The existing genre verification does not fix a poor candidate pool, and zero-genre matches can still remain in the ranked output.

Fix candidate sourcing and ranking together. Do not preserve the title-word approach and describe new labels as a semantic recommendation improvement. Avoid the current arbitrary `.shuffled()` term choice as a quality mechanism.

## 5. Workstream A — Mood language and useful specificity

Preserve existing internal mood identities where possible. A display rename must not break `MOOD_MAP`, text guessing, selected state or tests. Separate stable ID, display phrase, description and rule configuration. Keep descriptions to one clear line; emojis are secondary.

### Proposed map for the existing 29 moods

| Existing key | Proposed visible phrase | Meaning to preserve |
| --- | --- | --- |
| Happy | Need a Laugh | Lighthearted comedy and warmth |
| Sad | Let Me Cry | Emotional drama and catharsis |
| Excited | Bring the Adrenaline | High-energy action and stakes |
| Relaxed | Help Me Unwind | Gentle pacing and low-stakes stories |
| Angry | Fuel My Rage | Confrontation and righteous anger |
| Scared | Scare Me Tonight | Horror and deliberate scares |
| Inspired | Get Me Motivated | Ambition, perseverance and achievement |
| Nostalgic | Take Me Back | Memory, coming of age and throwback feeling |
| Curious | Feed My Curiosity | Discovery, investigation and unusual ideas |
| Romantic | Make Me Swoon | Romance and emotional chemistry |
| Adventurous | Take Me Somewhere | Journeys, quests and exploration |
| Thoughtful | Question My World | Moral questions and reflection |
| Anxious | Calm My Nerves | Comforting stories; not anxiety-inducing thrillers |
| Hopeful | Give Me Hope | Recovery, optimism and resilience |
| Playful | Keep It Playful | Whimsy, imagination and light fun |
| Adult | Keep It Grown | Mature relationships and complex grown-up drama |
| Wired | Keep Me Wired | Kinetic pacing and sustained energy |
| Vengeful | Make Them Pay | Payback and retribution |
| Goofy | Make It Ridiculous | Absurdity, parody and slapstick |
| Eerie | Something Feels Wrong | Atmosphere, unease and slow dread |
| Empowered | Make Me Fearless | Agency, courage and overcoming obstacles |
| Jaded | Keep It Cynical | Satire, cynicism and dark humor |
| Awestruck | Blow My Mind | Spectacle and imaginative ideas |
| Challenged | Make Me Think | Puzzles and demanding narratives |
| Giddy | Lift My Spirits | Joyful, buoyant stories |
| Focused | Keep Me Hooked | Tight storytelling and sustained attention |
| Escapist | Get Me Away | Immersive worlds and escapism |
| Intrigued | Keep Me Guessing | Mystery and layered reveals |
| Tense | Keep Me Tense | Pressure and suspense |

The Adult mapping means mature relationships/complex cinema, matching the current rule description. It must not be silently reinterpreted as a guarantee of explicit content or as IMDb's separate adult classification.

### Proposed additional specific moods

These are proposals supported by the request for more moods; their exact names/count were not dictated by the owner.

| New stable key suggestion | Phrase | Distinct selection intent |
| --- | --- | --- |
| heartbreak | Break My Heart | Relationship loss and heartbreak, rather than any sad drama |
| complicated_love | Love Gets Complicated | Messy relationships, conflicting commitments and romantic dilemmas |
| friendship | Friendship Hits Different | Friendship, belonging and found family |
| paranoia | Trust Absolutely Nobody | Distrust, conspiracy and hidden motives |
| redemption | One Last Chance | Redemption, rebuilding and second chances |
| underdog | Root For Underdogs | Outsiders overcoming odds |

A 35-choice catalog is a starting proposal, not a target to pad. Every new mood needs a meaningful rule, positive examples and counterexamples; omit a redundant choice rather than pretending broad genres make it distinct. Retain the light carousel and lazy results. Avoid a new full-screen taxonomy or unrequested settings surface.

### Candidate and ranking plan

1. Use a bounded pool of known titles with OMDb detail metadata: existing cached collections, deliberate bundled ID seeds, and approved manifest IDs when available.
2. Do not let the remote-feed decision block labels, year state or a cache/bundled-candidate improvement.
3. Apply type/year eligibility, remove duplicate/current or invalid IDs as appropriate, then score useful evidence from available genres and plot text. Treat curated local mood tags as authored rules, not provider-supplied truth.
4. Require substantive mood evidence. Generic genre overlap alone should not qualify a precise mood; missing metadata should lower confidence rather than turn every title into a match.
5. Keep rating as a minor quality/tie-break signal, not a substitute for relevance. Use a deterministic final tie-break such as IMDb ID.
6. Hydrate only a bounded missing subset through the existing global request slots. Do not fetch the entire catalog per mood or repeat detail requests on each local year change.
7. Empty/sparse states must say “No matches in the current collection” and offer All Years or a different mood. Never imply no such films exist globally.

If renaming a confidence badge, do not present the current “2x Match” genre count as a scientifically calibrated relevance score.

## 6. Workstream B — Compact year picker inside Mood

### Required behavior

- Initial value: **All Years**. Preserve relevance/editorial ordering; do not add an implicit chronological sort.
- D-pad action labeled `Year: All Years` or `Year: 2024` near the mood selection/result controls.
- Small anchored/dialog picker with All Years first, then descending years in a lazy list. Focus the current selection on open, keep the highlighted row visible and restore focus to the opener on close.
- OK applies; Back dismisses without changing the applied value. A draft selection must not leak into the live grid.
- Keep the selected year when switching mood or using Surprise Me within the current navigation session; clearing the year is explicit. Returning from Details restores mood, year and practical grid position/focus.
- Movies use release year; series use start year, consistent with `startYear()`. Unknown/malformed years appear under All Years and are excluded by an exact-year filter.
- Do not restrict selection silently to years already loaded. A virtualized year range can cover historical years through the current year; a sparse result still needs honest collection wording.
- Do not add a new persistent preference or year/type/settings hierarchy unless necessary for the agreed flow.

### State and request correctness

Add year to MoodUiState and the recommendation request identity. Cancel superseded mood/year work and check that only the latest identity may publish. Identical candidates and filter should produce stable ordering. Returning to All Years restores the same candidate ranking; it should not reroll random searches.

Do year filtering locally when metadata is known. Passing `y` to OMDb is valid for real title searches, but it does not make mood-word title queries into discovery queries. If a future bounded candidate lookup is needed, carry year through any request/cache identity that actually depends on it.

## 7. Workstream C — Refreshable collections without a new movie API

### Locked direction versus unresolved source

The desired data flow is:

**small permitted IMDb-ID collection file → bounded ID lookups through OMDb → cached Home/Browse cards**

OMDb remains the source of posters/details. A static HTTPS JSON file is still an external data dependency and needs an owner, hosting and update process, even though it does not add a second movie API/account/key.

**No reliable automatically refreshed upstream ID-list source was selected or verified in this review.** Do not claim that this handoff has solved live new-release discovery.

| Option | Assessment for this milestone |
| --- | --- |
| Owner-controlled, versioned JSON manifest on a static host such as raw GitHub | Simplest proposed transport. Can update shelves without an APK. It only stays current if someone or an approved upstream process maintains it. Manual editorial updates must be labeled as editorial. |
| Existing third-party maintained ID feed | Possible only after verifying exact URL, permissions, maintenance history, schema, ID/type quality and fallback behavior. No such feed is approved here. |
| IMDb official non-commercial datasets | Officially refreshed daily; contain IDs, type, start year and genres. They are bulk gzipped TSV datasets subject to stated terms, not a small trending/new-release endpoint. Not selected as an in-app feed. |
| OMDb keyword/genre title queries | Rejected as a solution to dynamic discovery: `s` searches titles. |
| New TMDB/Trakt/etc. API, paid AI, arbitrary website scraping | Not part of the agreed plan. Do not add these silently to fill the source gap. |

If genuinely automatic updates are required, a permitted maintained upstream or a separately agreed update job is necessary. A collection JSON file without that process is remotely editable curation, not automatic new-release discovery. Document this distinction and leave the feature gated if the dependency remains unresolved.

### Proposed minimal manifest contract

Illustrative schema only; this file is not implemented or hosted by this documentation change:

```json
{
  "schemaVersion": 1,
  "revision": "editorial-001",
  "updatedAt": "2026-10-20T00:00:00Z",
  "collections": [
    {
      "id": "featured-movies",
      "label": "Featured Movies",
      "type": "movie",
      "imdbIds": ["tt0133093"]
    }
  ]
}
```

Use stable collection IDs and IMDb IDs, with clear ordering and provenance maintained by the feed owner. Do not include playback URLs, API keys, arbitrary executable instructions or a second metadata catalog. Validate hydrated type/year where a shelf label implies them. A changed file revision is not proof of a film's release date.

### Proposed conservative runtime policy

These budgets are engineering proposals, not previously measured owner requirements:

- One allowlisted HTTPS manifest origin; bounded response size (e.g. 64 KiB), timeout and redirect handling. Treat JSON as untrusted data.
- Start with at most six collections and twelve IDs per collection; validate ID shape, type values, unique collection IDs, timestamp/schema and duplicates before accepting. Do not hard-code a fixed seven-digit IMDb ID assumption.
- Keep the full last-known-good snapshot on malformed, unsupported, empty or failed replacement. A deliberately empty catalog needs an explicit contract, not accidental erasure.
- Persist the last accepted manifest/revision independently of evictable OMDb metadata. Keep a bundled offline seed snapshot as the final fallback. No destructive database migration.
- Show cached cards first. Refresh the manifest off the critical render path on a bounded cadence (proposed daily), coalesce concurrent refreshes and permit an explicit retry without a request loop.
- Initially resolve only the visible bounded subset (proposed eight cards per active shelf, no more than roughly the existing launch request budget). Defer remaining IDs until relevant. Preserve the global two-slot OMDb limit, cancellation, request coalescing, quota behavior and cache bounds.
- Add a progressive **ID** hydration method using `getDetails(imdbId)` / `details_<id>` cache identity; the current title-based progressive method is not an interchangeable ID loader.
- Retain populated shelves during failure/refresh. Update rows predictably; do not steal D-pad focus, discard scroll, repeatedly change hero or reorder the card under a user mid-navigation.
- Preserve Movies/TV separation, existing local sort/filter behavior and honest loaded-collection wording. Do not add unrelated Browse filters.
- A manifest refresh, metadata response and provider playback are separate facts. A title being newly listed does not prove it is newly released or playable.

Until trustworthy ranking/release provenance exists, use labels such as Featured Movies, Recently Added to Collection or Updated Picks. Avoid Trending, Popular This Week and New Releases as claims without supporting source data and a defined time window.

## 8. Workstream D — More Like This that earns the recommendation

### Required ranking priorities

1. Shared themes and subgenres that express the title's core appeal.
2. Tone, setting and narrative similarity.
3. Broad genre compatibility.
4. Director/cast overlap as weak supporting evidence.
5. Ratings/year proximity only as tie-break/quality context.

The app lacks structured theme/tone/subgenre fields. A deterministic local heuristic can derive limited signals from available plot/genre metadata and small authored vocabularies. Label this as heuristic matching; do not describe it as verified semantic understanding or add AI/embedding services.

### Proposed implementation

- Extract a pure, testable scorer rather than expanding a large UI composable. Normalize case/punctuation, ignore missing/N/A fields, use token/phrase boundaries and a small explicit synonym map. Avoid raw substring accidents and generic plot-word matches.
- Use the current title's strongest evidence. For a psychological thriller, prefer a candidate with psychological tension, investigation or twists over unrelated general drama. A shared actor alone must not qualify an otherwise weak film.
- Maintain same-type matching initially, as the current implementation does. Cross-type recommendations were not requested.
- Deduplicate candidates by IMDb ID **before** applying the unique-candidate cap. Fix or work around the current 100-payload-before-deduplication bias without growing an unbounded cache.
- Reuse fresh local metadata first. Stale metadata may be used only under the repository's documented recovery policy, with honest status. Do not silently bypass expiry.
- Start cache-first with no extra request on opening Details. Approved manifest/bundled IDs can expand the pool through existing collection hydration later; do not hydrate every recommendation candidate on every title page.
- Show at most six qualified results; one to five is valid. Hide the row or display a calm unavailable state when none qualify. Never pad with weak matches.
- Publish details/Play before recommendation work. Tie asynchronous results to the currently displayed IMDb ID and type, and preserve title-card return focus.
- If a bounded future enrichment stage is needed to remedy cold-start emptiness, document its request budget first. The lack of a large pool is a coverage limitation, not a reason to add another API silently.

## 9. Workstream E — Creator identity and About

Extend the existing About section. Retain version/code/variant/commit, source/download/issues links, playback limitations and dependency credits.

Suggested concise copy:

> Created by Ismail Hossain.  
> A Dhaka-based technology professional working across Shopify SaaS, QA, product support and practical software development. NowShowing TV is an independent project built for comfortable everyday viewing on Android TV.

Owner profile links recovered from his saved portfolio, not guessed:

| Label | URL |
| --- | --- |
| GitHub | https://github.com/smile-plzz |
| LinkedIn | https://www.linkedin.com/in/md-ismail-hossain-2938541b1/ |
| Facebook | https://www.facebook.com/optimisticyetpessimist/ |

Provenance: the owner's saved portfolio text dated 11 August 2026 contains his name and these exact links; LinkedIn also appears in earlier owner-supplied context. This review verifies the stored addresses, not current unauthenticated profile-page availability. Do not substitute another person if a site requires login.

Keep the full URL visible with an explicit Open action and the current no-browser fallback. No launch-time social-network calls, embedded social widgets, remote headshots or new image work. Personal phone numbers, address, relationship/family information and a full CV do not belong in the TV app. Additional social networks/education details are optional future copy choices, not blockers.

## 10. Protect the accepted product

- Preserve current WebView/provider/pointer/Back/Menu/episode/source behavior and content protection. The owner accepts current ad handling; leave it out of this round.
- Preserve metadata failure classification, invalid-key-only failover, authorized local credential configuration and redacted diagnostics. Do not add bundled personal keys or rotate keys to evade quota.
- Keep cached-first progressive browsing, bounded memory/image work and the two global OMDb network slots. Do not replace warm content with full-screen loading on a background refresh.
- Run ranking, parsing and storage work off the main thread. A local year change should not fan out network requests.
- Preserve watchlist/history/watched labels/autoplay preferences. Do not reuse metadata cache clearing as a collection reset that erases user data.
- Preserve opt-in App health and photo reports. No new automatic upload or mandatory diagnostic/analytics service.
- Mood recording is supported by the recorder even though the current App health guide does not list it in every sentence. Update documentation during the implementation milestone rather than claiming the recorder excludes Mood.
- Do not infer video FPS, input latency or playback completion from browsing window-frame metrics.

## 11. Recommended execution order after authorization

| Order | Deliverable | Dependency / exit condition |
| --- | --- | --- |
| 0 | Re-read this handoff and diff main against the reviewed baseline | Preserve later user/repository changes; confirm actual installed build/variant before comparisons |
| 1 | Stable mood identities, catalog proposal and pure candidate/scoring helpers | Show clear label-to-rule mapping and semantic examples; do not retain title-word discovery as a hidden fallback |
| 2 | Mood year state and compact picker, with bounded local candidate flow | Correct All Years / exact-year behavior, latest-request ownership and remote focus |
| 3 | Improved cache-first More Like This | Deterministic relevance tests, no title-page request burst, no weak-result padding |
| 4 | About creator copy and sourced links | TV layout and browser-unavailable behavior; no social network requests during loading |
| 5 | Source selection and bounded collection manifest integration | Exact permitted source + update responsibility/cadence documented first; if unresolved, keep current editorial fallback and mark dynamic collection delivery incomplete |
| 6 | Integrated TV acceptance and documentation reconciliation | Preserve smooth browsing/playback and user data; resolve regressions before expanding scope |
| 7 | Release candidate packaging | Increasing version/code, CI, signing evidence and exact limitations; stable promotion only when gates below are satisfied |

This order is a recommendation, not a new restriction requiring approval at every stage. An unresolved feed must not hold unrelated UI/ranking/About work hostage, but it also must not be reported as completed. Do not publish an all-features-complete claim while the update source remains unresolved.

## 12. Acceptance and regression matrix

| Area | Required checks |
| --- | --- |
| Mood catalog | Unique stable IDs; every visible mood has a useful rule/description; legacy text guessing still maps sensibly; no accidental key/display-name coupling; remove the obsolete exact-29 assertion in favor of intended catalog invariants |
| Semantic quality | Per specific mood: representative positives and counterexamples; unrelated Drama must not outrank a thematically appropriate thriller just because of rating or shared cast; results do not depend on random search-term selection |
| Year | All Years initially; exact release/start year; open-ended series range; missing/N/A/malformed year; no match; Apply vs Back cancellation; restoration after Details; Surprise Me and mood change retain year |
| Async work | Rapid mood/year changes, cancel/retry/background; old results cannot replace a newer request; old title recommendations cannot appear under a new title |
| Collections | Valid update; corrupt JSON; unknown schema; empty list; duplicate/invalid IDs; wrong type; timeout/offline; repeated refresh; last-known-good recovery after restart; no cache/user-data destruction |
| Budget | Warm-cache zero unnecessary detail refetch; bounded cold hydration; preserved metadata concurrency; no per-keystroke/year-change request fan-out; no unbounded async task list |
| More Like This | Current title excluded; unique candidates before cap; same type; deterministic score/ties; sparse/unknown metadata; no recommendations; weak matches not padded; candidate row does not block Play |
| About | Exact creator and URL copy, readable line wrapping, D-pad access, no-browser fallback; no unrequested personal data |
| Existing functions | Search with both keyboards; Browse sort/filter; watchlist/history/watched labels; app health photo paging/persistence; movie and series playback, episode controls and source switching |
| Real TV | Same device and installed variant; cold/warm browsing separately; repeatable Home → Movies → Mood/year → Details → Back → Watchlist/Search route; compare with recording on/off, then test playback separately |

Meaningful new tests should cover pure scoring, candidate/filter identity, feed validation/failure recovery and remote picker behavior. Extend existing Browse/cache/loading tests where appropriate. The present source contains mood structure tests, repository/cache tests, Browse loading/year tests, watchlist presentation tests, TV polish tests, and photo report persistence/layout tests. Do not replace behavior tests with source-text checks or hard-coded catalog-count checks.

When implementation is authorized, the existing baseline commands are:

```sh
./gradlew testDebugUnitTest
python3 -m unittest discover -s scripts -p 'test_*.py'
./gradlew assembleDebug assembleRelease
```

Those commands are for the future implementation milestone. They were not run for this documentation-only handoff. CI remains authoritative for the already published candidate; it does not prove the new requirements are implemented.

## 13. Release and signing gates

Do not reuse published 1.6.0-rc.2 / 16001 assets or tags for changed code. Choose the next version/code at implementation time after checking current main and releases.

The current debug APK is a separate Preview package; it does not migrate the main app's data. It is useful for safe feature testing, but debug performance does not establish minified release performance.

A previously reported installed certificate was:

`22:AA:2A:F5:98:20:E0:03:E4:51:FF:B3:0F:26:42:50:66:76:41:FC:21:00:40:98:79:8E:B6:93:44:0E:B3:52`

That is historical evidence, not a fresh certificate reading from the latest TV review. Recheck the actual installation. A new permanent key alone does not make an older different-key installation upgrade-compatible. Never ask the owner to uninstall the valued installation to bypass a mismatch. Do not recover or handle another project's private key as if it belonged to NowShowing.

Stable promotion needs durable signing, an explicit compatible-install/update strategy and acceptance of the actual candidate on TV. Keep development-signed builds labeled as prereleases. See [Release signing](RELEASE_SIGNING.md), [Installation](INSTALLATION.md) and [TV acceptance](TV_ACCEPTANCE.md).

## 14. Documentation reconciliation during implementation

- Update README, release notes, product readiness, installation, limitations and tests to describe what actually ships.
- The current README's candidate-release-notes link points to rc.1 although the displayed version is rc.2; this handoff documentation change corrects that link.
- TV_ACCEPTANCE currently has a 1.5.1 heading and older section/report instructions. At implementation, reconcile it with the current six Settings sections, App health flow and actual candidate version.
- RELEASE_SIGNING starts with a historical v1.5.1 status paragraph. Do not treat it as current signing evidence; use release build-info/certificate assets.
- Keep older architecture/handoff claims historical where they conflict with source, particularly native playback and automatic web completion.
- No further features are implicitly included because they appear in an older wishlist.

## 15. Open decisions and what is already known

| Item | Status / next action |
| --- | --- |
| Start development | Explicit owner instruction still required; this handoff is not that instruction |
| Complete mood catalog | Six approved example phrases establish the style; remaining mappings and six additions are proposals to validate for distinct meaning |
| Automatic ID updates | Upstream source/update process unresolved; document exact candidate and maintenance responsibility before integration |
| Facebook / LinkedIn | Exact addresses recovered from saved owner material; no need to invent or ask for the same URLs |
| Current tested TV build | Owner confirms improved experience, but exact version/variant/certificate was not supplied in this latest feedback |
| App health feedback | No new diagnostic feature request or numeric report was provided in this set |
| Stable signing | Still unresolved; do not equate successful APK verification with compatible updates |

## 16. Primary references checked 20 October 2026

- [OMDb API parameters](https://www.omdbapi.com/): documents ID lookup, title search, type and release year. It does not document a trending, genre-discovery or plot-keyword-discovery endpoint.
- [IMDb non-commercial datasets](https://data.imdb.com/non-commercial-datasets/): documents daily refresh, bulk TSV fields and use subject to stated terms. Review exact permission/redistribution requirements before selecting a derived feed. No dataset import or redistribution is authorized by this handoff.
- [Published 1.6.0-rc.2](https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases/tag/v1.6.0-rc.2).
- [App health scope](APP_HEALTH.md), [product readiness](PRODUCT_READINESS.md), [current limitations](KNOWN_LIMITATIONS.md).

## 17. Starter instruction for the next thread

> Read docs/FINAL-PRODUCT-REFINEMENT-HANDOFF.md first, then inspect current main and its diff from baseline 6513131280b55eb9ad1ec90809d9882b6f71fa6b. We are finalizing NowShowing TV. Preserve the smoother browsing, working playback and current ad handling. The agreed next scope is clearer three-word moods plus useful specific additions, a compact All Years/exact-year picker in Mood, refreshed IMDb-ID collections while retaining OMDb, better cache-first More Like This, and creator information with the sourced social URLs. Cast photos, a new movie API and ad/player rewrites are excluded. Dynamic collection sourcing remains unresolved and must not be faked. Confirm whether my message explicitly authorizes implementation; if not, stay in documentation/planning. Do not rebuild, bump or publish merely because you received this handoff. Once I say start, implement the agreed work, validate it and report any remaining source/signing/TV acceptance gates honestly.
