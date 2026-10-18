# TV remote controls — 1.5.1

## Browsing
Arrows move focus, OK selects, and Back returns through the navigation stack. Selected cards/actions and list/grid position are restored when possible. Focus has a bright outline; a checkmark/tint indicates a selected source or episode.

## Embedded player
| Input | Action |
|---|---|
| Short arrow press | Precise pointer movement |
| Held arrow | Faster pointer movement |
| OK / Enter | One click at the pointer; held OK does not repeatedly click |
| Back in provider fullscreen | Exit fullscreen, keeping player interaction |
| Back while interacting with player | Open app controls |
| Back in Sources/Episodes/Help | Return to main controls |
| Back in main controls | Return to title details |
| Menu | Open app controls directly |
| Return to video | Focus provider interaction and briefly reveal the pointer |

Fullscreen pointer support forwards touch events to the provider custom view. Some provider implementations may still behave differently; actual remote testing remains necessary.

Main controls provide prominent Next episode and Episodes for series, Change source, Retry this source, Remote help, and Back to title. The episode panel displays the current season/episode and metadata when available. Selecting an episode is a deliberate action; no iframe completion detection is claimed.

First-use tips can be dismissed with Got it and reset from Settings. Help remains available regardless of dismissal.

## Native diagnostics
Settings > Help > Native playback checks opens fixed HLS/MP4 reference videos in the native Media3 player. Its native playback controls remain available. Diagnostics do not affect Recently opened or native Continue Watching records.

## Candidate daily-use changes
Details initially focuses Play. Back restores the previous card/action and saved position where retained. Play directly focuses provider pointer input; any required provider-owned play click remains. In series controls, Next episode is prominent and initially focused; metadata is required to resolve season boundaries safely. Settings contains default-on autoplay (web completion currently unsupported), personal metadata key recovery, About and Diagnostics. See WEB_PLAYBACK.md for the authoritative playback contract.

## Settings sections
Use Left/Right on Playback, Metadata, Privacy, Help and About tabs, then Down into that section. About → View diagnostic report opens a report dialog; use arrows to read its chunks and Done/Back to close. Blank personal-key input cannot replace a saved key; use the explicit Use demo key action instead. Busy update/cache/report actions are temporarily disabled.
