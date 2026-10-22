# Cloud catalog and mood recommendations

Edit `catalog/catalog.json` on the repository's **main** branch. The app fetches the raw HTTPS URL at most once every 15 minutes per process when a discovery screen loads; reopening/refreshing a screen after that interval checks again. GitHub CDN propagation can add delay. An APK containing this feature must be installed once before remote updates work.

Schema example:

```json
{"version":1,"sections":{"featured":["tt1375666"],"movies":["tt1375666"],"series":["tt0903747"],"mood_Happy":["tt0382932"]}}
```

Sections: `featured`, `movies`, `series`, `action`, `scifi`, `drama`, and `mood_` followed by the exact key in MoodSeeds.kt. A missing section uses bundled defaults. Present sections must contain 1–100 IMDb IDs, with 7–10 digits after `tt`. Duplicates are removed in editorial order. Maximum 64 sections and 128 KiB. Invalid documents are rejected as a whole. Do not include API keys, streaming URLs, JavaScript or personal information.

The manifest selects candidates, not movie metadata: posters, titles, plots and genres still come from OMDb and its cache. Metadata failures can still leave previously unseen IDs unavailable. Search remains title search; watchlist/history remain personal local data. Related titles hydrate up to eight catalog candidates and then use theme/genre ranking. Home shows up to eight movie and series picks; Browse exposes the section's full list.

Offline or failed updates use the last validated catalog, then bundled defaults. Catalog data is public and requires no cloud credentials. Change `CloudCatalog.URL` in source if moving hosts; this release deliberately has no TV text-entry URL setting.

Mood hydration is capped at the first 24 IDs per selection. Mood sections are editorial evidence. Curated IDs rank above generic metadata matches, but must still match a relevant genre and honor excluded genres (for example comfort moods exclude horror/thriller/crime/war). This is explainable local ranking, not an AI service or live popularity chart. Mood snapshots remain stable while the screen is active; Retry reloads candidates. Year filtering remains local.

## Validation and acceptance

Run `./gradlew testDebugUnitTest assembleDebug assembleRelease`. Test catalog failure/offline fallback, duplicate IDs, malformed documents, switching mood during loading, year filtering, and repeated provider Retry/Back on the actual TV. To exercise renderer termination, use a controlled debug-only test page; never add crash URLs to the published catalog.
