# Troubleshooting

| Symptom | What to do |
|---|---|
| App not installed / signing conflict | Check the release's signing report and existing build. Do not uninstall or clear storage unless you accept losing local data. See INSTALLATION.md. |
| APK will not open | Confirm Android TV / Google TV API 26+, enough free storage, and that you downloaded the release APK rather than source code. |
| TV has no app icon | Look in the full app list and restart the launcher. With ADB, launch com.nowshowing.tv/.ui.MainActivity. |
| Provider page is blank or fails | Open controls with Back/Menu. Retry this source, then Change source. Check the TV's connection and update Android System WebView through the TV's supported app store. |
| Page loads but the video does not | Enter the player and click the provider's actual play button. Page loading and video playback are different. Try another source if needed. |
| Pointer is difficult to position | Use short arrow taps for precision; hold to accelerate. The pointer appears briefly on entering the player and after movement. |
| Cannot find controls | Back leaves provider fullscreen first. Press it again for app controls. Menu opens app controls directly. |
| Subtitles, pause, or seeking behave differently | Use the provider's controls with the pointer. Universal native playback shortcuts are not available for every web player. |
| Episode list is empty | Retry the list; metadata may be unavailable or rate-limited. Use the existing next-episode option when appropriate. Do not assume a missing list means the series has ended. |
| Recently opened restarts the video | Expected for providers without their own resume behavior. The app remembers episode/source, not an inaccessible timestamp. |
| Home shelves are empty | Try Refresh shelves and check network/API quota. Valid cached shelves and local history are independent of provider playback. |
| A source breaks while filtering is enabled | Try a different source first. Review only that source's existing exemption controls if necessary; HTTPS/certificate protections remain enforced. |
| Native playback needs checking | Settings > Playback diagnostics opens fixed demo videos. They test Media3 and codecs; they do not resolve your selected movie. |

## Reporting a problem
Include the version/build from Settings, TV model, Android version, source name, movie title or season/episode, steps taken, and whether another source works. Never post passwords, signing keys, private cookies, or tokens.

Avoid clearing app storage as a routine troubleshooting step. It removes watchlists, viewing history, source preferences, and caches.
