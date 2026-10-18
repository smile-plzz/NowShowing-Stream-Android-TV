# Release checklist — 1.5.1

## Implemented/prepared
- [x] New version 1.5.1 / 15004; no overwrite of a published version.
- [x] Current architecture, playback, controls, limitations and acceptance documentation.
- [x] Metadata/storage/filter/episode-policy regression tests added.
- [x] Existing workflow packages APKs, checksums, public certificate details and build-info.json.

## Required before a dependable public release
- [ ] Final commit JVM/Robolectric and release-script tests green.
- [ ] Debug and minified release APKs assembled; apksigner verifies both.
- [ ] Artifact version/commit/checksum agree with source and release notes.
- [ ] Signing mode checked from actual build; stable certificate confirmed against installed build.
- [ ] Same-certificate update tested without losing saved data.
- [ ] Real-TV acceptance completed, including source playback, focus, cache recovery and long browsing.
- [ ] Remaining provider-specific/manual progression limitations recorded.
- [x] Owner authorized final polish, merge and publication on 18 October 2026. Publication must use the gated workflow.
- [ ] Actual published tag and downloadable asset links checked afterward.

A PR build or candidate version in README is not a published release. CI does not establish provider playback or real-device performance. PR artifacts use development signing; a new signing identity may require uninstall/data loss and must not be introduced silently.
