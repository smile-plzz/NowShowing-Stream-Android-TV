> **October 17, 2026 playback update:** See [WEB_PLAYBACK.md](WEB_PLAYBACK.md) for the current dual-engine playback contract. Earlier native-only / zero-WebView requirements below are historical and superseded. Real-provider TV testing remains required.

# Remote Navigation & Focus Management: NowShowing TV

## 1. Remote Control Key Matrix

NowShowing TV is built from the ground up to be 100% operable with standard Android TV remote controls (including Google TV voice remotes, Shield TV remotes, and generic HDMI-CEC television remotes).

| Remote Key | Android KeyCode | Primary Action | Screen Context |
|------------|-----------------|----------------|----------------|
| **D-pad Up** | `KEYCODE_DPAD_UP` | Move focus up / Show HUD | Moves between carousels, inputs, or header tabs; reveals player HUD |
| **D-pad Down** | `KEYCODE_DPAD_DOWN` | Move focus down / Show HUD | Moves down to next carousel row or list item; reveals player HUD |
| **D-pad Left** | `KEYCODE_DPAD_LEFT` | Move focus left / Seek -10s | Navigates backwards along carousels; seeks -10s in Player |
| **D-pad Right** | `KEYCODE_DPAD_RIGHT` | Move focus right / Seek +10s | Navigates forward along carousels; seeks +10s in Player |
| **Center / OK** | `KEYCODE_DPAD_CENTER` / `KEYCODE_ENTER` | Select / Activate / Play-Pause | Opens details, triggers search, plays/pauses media, confirms next episode |
| **Back** | `KEYCODE_BACK` | Back / Exit / Dismiss | Closes player/drawer/modal, returns to previous screen with focus intact |
| **Play / Pause** | `KEYCODE_MEDIA_PLAY_PAUSE` | Toggle playback | Directly toggles video playback in player view |
| **Fast Forward** | `KEYCODE_MEDIA_FAST_FORWARD` | Seek forward | Seeks +10s in Media3 player |
| **Rewind** | `KEYCODE_MEDIA_REWIND` | Seek backward | Seeks -10s in Media3 player |

## 2. Visual Focus Indicator Specifications

To prevent user disorientation from 10 feet away:
1. **Focus State Styling**:
   - Every interactive card, button, and pill features an active focus border (2dp bright accent cyan `#00D2C4` or gold `#FFB800`).
   - Slight subtle scale transform (`1.05x`) with smooth 150ms interpolation.
   - Gentle drop shadow / elevation glow behind focused element.
2. **Unfocused State**:
   - Dark, matte surface with clear contrast against pure black background (`#0D0E12`).
   - Text elements remain legible with minimum 4.5:1 WCAG contrast ratio.

## 3. Screen-by-Screen Navigation Flows

### 3.1. Home Screen
- **Top Bar**: Search icon, Movies tab, TV Shows tab, Moods tab, Watchlist tab, Settings tab.
  - Pressing `Down` from Top Bar drops focus into Hero or the first carousel item.
- **Hero Banner**:
  - Focusable "Play" and "More Info" buttons.
  - Pressing `Down` enters the first horizontal content row ("Continue Watching" or "Trending Now").
- **Horizontal Carousels**:
  - Scrolling is horizontal (`Left`/`Right`).
  - Pressing `Down` moves focus to the vertically aligned card in the row below, maintaining horizontal scroll offset.
  - Pressing `Up` moves focus back to the row above.
  - When the first card in a row is focused, pressing `Left` stops smoothly without erratic jumps.

### 3.2. Title Details Screen
- **Action Row**: "Play / Resume", "Watchlist" (Add/Remove), "Sources" (Drawer trigger).
- **Compatibility Badges**:
  - Native HLS/MP4 streams display cyan `[NATIVE]` badge.
  - Legacy browser-only embed providers display muted coral `[EMBED]` badge.
- **Season Selector**:
  - Horizontal list of season chips ("Season 1", "Season 2", etc.).
  - Pressing `Down` transfers focus directly to the first episode in the episode list.
- **Episode List**:
  - Vertical list of episodes showing episode number, title, and runtime.
  - Pressing `OK` on an episode immediately launches native playback for that episode.
- **Recommended Titles Carousel**:
  - Located below the episode list for one-click discovery of related titles.
- **Back Navigation**:
  - Pressing `Back` restores focus to the exact card previously selected on the Home/Catalog screen.

### 3.3. Mood Discovery Screen
- **29-Mood Grid**:
  - Arranged in a responsive TV grid (4 columns wide).
  - Clean D-pad navigation across rows and columns.
- **Quick Actions Bar**:
  - "Surprise Me" button for instant random recommendation generation.
  - Keyword input field with on-screen TV keyboard trigger.
- **Results Row**:
  - Below the mood selector, results populate dynamically. Moving `Down` enters the recommendation results list.

### 3.4. 10-Foot Search Screen
- **On-Screen Keyboard**:
  - High-visibility QWERTY layout optimized for rapid D-pad traversal.
  - Space, Backspace, Clear, and Search action buttons.
  - Type filter pills (All, Movies, TV Series) immediately accessible above the keyboard.
- **Recent Searches**:
  - One-click pill list of previous queries.
- **Results Grid**:
  - Results pane dynamically displays live search results.
  - Pressing `Right` or `Down` moves focus from the keyboard to the results grid.

### 3.5. Native Player Screen (AndroidX Media3)
- **Zero Third-Party WebViews**:
  - 100% native ExoPlayer surface rendering hardware-accelerated HLS, DASH, and MP4 streams.
- **HUD Auto-hide & Activity Reset**:
  - Controls overlay automatically fades after 4 seconds of inactivity.
  - Any D-pad key press (`Up`, `Down`, `Left`, `Right`, `OK`) immediately displays the HUD and resets the timer.
- **D-pad Remote Seek Feedback**:
  - Pressing `Left` or `Right` seeks -10s or +10s and triggers a visual feedback pill (`⏪ -10s`, `⏩ +10s`).
- **Series Auto-Next Episode Prompt**:
  - When within 30 seconds of an episode's completion, an overlay banner prompts:
    `Next Episode in Xs: [OK] Next Episode  [Back] Dismiss`.
  - Pressing `OK` immediately transitions to the next episode.
- **TV Source Drawer**:
  - Clicking the "Sources" button opens an on-screen drawer listing all configured providers with compatibility indicators.
  - Selecting a compatible native provider hot-swaps the active MediaItem seamlessly.
- **Dismissal & State Preservation**:
  - Pressing `Back` pauses playback, persists resume position to `ContinueWatchingStore`, and cleanly returns to Title Details.

### 3.6. Settings Screen
- **D-pad Toggle Navigation**:
  - Vertical list of settings rows with toggle switches for Ad Blocking, Tracker Protection, and Strict Redirect Validation.
  - Action buttons for checking filter updates and clearing statistics with visible focus borders.

## 4. Focus Retention & State Restoration
1. **Back-Stack State Preservation**:
   - `rememberLazyListState` and `LazyRowState` keys are preserved per route.
   - Returning from Title Details to Home preserves the exact carousel scroll position and restores focus to the previously active poster card.
2. **Asynchronous Load Resilience**:
   - Shimmer skeleton loaders receive temporary focus placeholders if needed, but focus automatically shifts to loaded content upon data arrival without focus dropping off-screen.
   - When a row is empty or loading, D-pad navigation safely bypasses it to the next valid focus target.
