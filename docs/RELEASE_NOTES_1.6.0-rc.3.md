# 1.6.0-rc.3 / 16002 — refinement candidate source

Not published. Build/test evidence and candidate artifacts are tracked in [PR #9](https://github.com/smile-plzz/NowShowing-Stream-Android-TV/pull/9). Real-TV checks remain pending.

- Clear mood phrases, six more specific choices and exact-year/All Years selection.
- Replaces mood-word title searches with fresh cached metadata and bounded authored IMDb-ID seeds.
- Stable local scoring and session snapshots; year filtering does not make network requests.
- More Like This requires shared theme evidence and never pads to six results.
- Creator bio, LinkedIn and Facebook in About.
- Reconciles App health and TV acceptance instructions.

Playback, providers and ad handling are unchanged. Dynamic collections are incomplete: no permitted maintained upstream was selected. Signing continuity and TV acceptance remain release gates. Do not uninstall an existing installation to bypass certificate mismatch.
