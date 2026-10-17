# TV remote controls — 1.5.0-rc.1

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
| Open player / Return to video | Focus provider interaction and briefly reveal the pointer |

Fullscreen pointer support forwards touch events to the provider custom view. Some provider implementations may still behave differently; actual remote testing remains necessary.

Main controls provide Change source, Episodes for series, Retry this source, Remote help, and Back to title. The episode panel displays the current season/episode and metadata when available. Selecting an episode is a deliberate action; no iframe completion detection is claimed.

First-use tips can be dismissed with Got it and reset from Settings. Help remains available regardless of dismissal.

## Native diagnostics
Settings > Playback diagnostics opens fixed HLS/MP4 reference videos in the native Media3 player. Its native playback controls remain available. Diagnostics do not affect Recently opened or native Continue Watching records.
