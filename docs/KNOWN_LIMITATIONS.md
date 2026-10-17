# Known limitations — 1.5.0-rc.1

- Existing movie providers supply web/iframe players. They remain inside Android System WebView. Direct diagnostic streams use Media3; no stream extraction or DRM bypass is implemented.
- Provider availability, video catalog, advertisements, subtitle options, seeking, and browser compatibility are outside the app's control. HTML loading is not proof of video playback.
- Some provider controls may still be difficult to operate, including custom fullscreen controls. The app provides a pointer and clearer surrounding controls; identical behavior across providers is not guaranteed.
- Cross-origin web players do not expose reliable progress/completion events to this app. Recently opened is page history, manual Watched is a user label, and exact native Continue Watching is separate.
- Last episode/source memory is local to this TV. No accounts, cloud sync, downloads, history export, or backup/restore are provided.
- Episode names and season counts depend on OMDb. A missing episode list does not prove the provider has no episodes. The app does not claim automatic iframe completion or next-episode playback.
- Network filtering covers requests exposed by WebView and bundled rule support; it is not equivalent to the Brave browser or a guarantee of removing every advertisement.
- Public/demo OMDb credentials can hit rate limits. Featured and popular shelves are curated seeds, not a live popularity ranking.
- Permanent release signing is not configured in the repository as of this candidate. CI uses development signing and marks publication as prerelease. Different runner-generated keys may prevent in-place updates. See RELEASE_SIGNING.md.
- Previous playback was confirmed by the owner on a real smart TV. The new menu/pointer/episode refinements require another real-device pass; automated tests/builds do not establish remote usability or universal provider compatibility.
- Google Play publication, Fire TV certification, Samsung Tizen, and LG webOS support are not part of this release.
