# Release checklist — 1.6.0-rc.1 candidate

## Implemented/prepared
- [x] New version 1.6.0-rc.1 / 16000; no overwrite of a published version.
- [x] Current architecture, playback, controls, limitations and acceptance documentation.
- [x] Metadata/storage/filter/episode-policy regression tests added.
- [x] Existing workflow packages APKs, checksums, public certificate details and build-info.json.

## Required before a dependable public release
- [ ] Three matched browsing comparisons and audit-on/off overhead evaluated on the owner TV.
- [ ] Every frozen report page photographed without clipping; Background stops sampling.
- [ ] Demo routes fail safely; year filters/pagination and loaded genre/year filtering verified.
- [ ] Final commit JVM/Robolectric and release-script tests green.
- [ ] Debug and minified release APKs assembled; apksigner verifies both.
- [ ] Artifact version/commit/checksum agree with source and release notes.
- [ ] Signing mode checked from actual build; stable certificate confirmed against installed build.
- [ ] Same-certificate update tested without losing saved data.
- [ ] Real-TV acceptance completed, including source playback, focus, cache recovery and long browsing.
- [ ] Remaining provider-specific/manual progression limitations recorded.
- [x] Owner authorized the post-release implementation on 19 October 2026.
- [ ] Candidate TV acceptance and publication decision recorded; no publication is triggered by this PR.
- [ ] Actual published tag and downloadable asset links checked afterward.

A PR build or candidate version in README is not a published release. CI does not establish provider playback or real-device performance. PR artifacts use development signing; a new signing identity may require uninstall/data loss and must not be introduced silently.
