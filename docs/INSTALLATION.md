> Download from the actual versioned GitHub Release. Compare public signing certificates before upgrading an existing installation. A certificate mismatch cannot be repaired by renaming the APK; uninstalling erases local data.

# Install and update NowShowing TV

## Supported TVs
Android TV or Google TV, Android 8.0 / API 26 or later. Samsung Tizen and LG webOS cannot install Android APKs. Fire TV compatibility has not been verified.

## Download
Open [GitHub Releases](https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases).
Open **v1.5.1**, expand Assets, and download **NowShowing-TV-1.5.1-release.apk**. If marked Prerelease, read the signing notice before updating. Debug APKs are for troubleshooting.
The debug APK is for development. Source-code ZIP/TAR downloads are not installable apps.

The release includes SHA256SUMS.txt, public signing-certificate fingerprints, and build-info.json. These identify exactly which version, commit, and signing mode you downloaded. This candidate uses development signing until a permanent key is configured; read the release notes before updating.

## Install using a USB drive or TV file transfer
1. Copy the release APK to a USB drive or transfer it to the TV using your existing file-transfer method.
2. Open the APK in a TV file manager.
3. If prompted, allow that file manager to install unknown apps, then confirm installation.
4. Open NowShowing TV from the TV's app list or Nook TV.
5. Disable that file manager's install permission again if you do not need it.

A developer-options setup is not required for ordinary file-manager installation.

## Install using ADB
Use Android SDK Platform Tools on your computer. Enable developer options and the appropriate debugging option on your TV.
Older devices commonly expose network ADB on port 5555:
```sh
adb connect TV_IP:5555
adb install -r NowShowing-TV-1.5.1-release.apk
adb shell am start -n com.nowshowing.tv/.ui.MainActivity
```
Newer devices may require Wireless debugging pairing instead:
```sh
adb pair TV_IP:PAIRING_PORT
adb connect TV_IP:DEBUG_PORT
```
Use the ports shown on your TV; the pairing and debugging ports can differ. Confirm the computer authorization prompt on the TV. Disable debugging after installation if it is no longer needed.

## Updating without losing local data
Install the newer APK over the existing app. Android preserves local data when the package name and signing certificate match and the version code is accepted.
This build is version **1.5.1**, Android version code **15004**. Older builds may show 1.0.0 in Android despite a different GitHub release tag.

If Android reports a signing mismatch / UPDATE_INCOMPATIBLE, stop. Reinstalling or clearing app storage erases local watchlists, history, preferences, and caches. A previous runner-generated development certificate cannot be reconstructed from its APK. Obtain a matching-key build if the old key exists; otherwise a deliberate fresh installation is required. There is currently no in-app export/restore.

The database upgrade adds history storage without dropping existing tables, but it cannot preserve data across uninstalling.

## First use and remote controls
Open a title, select an episode if needed, then Open player.
- Arrows move the pointer; OK clicks the provider's play or subtitle controls.
- Tap for precise movement; hold an arrow to move faster.
- Back exits provider fullscreen first, opens app controls next, then returns to the title.
- Menu opens app controls directly.
- Return to video returns control to the pointer.
- Change source offers existing movie providers. Demo streams live under Settings > Help > Native playback checks.
- Remote help is always available in the player and Settings.

Recently opened remembers the title, episode, and source. It does not guarantee timestamp resume inside third-party players. See [known limitations](KNOWN_LIMITATIONS.md) and [troubleshooting](TROUBLESHOOTING.md).
