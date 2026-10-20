# Install and update NowShowing TV

Android TV / Google TV, Android 8.0 (API 26) or later. Samsung Tizen and LG webOS cannot install Android APKs. Fire TV compatibility is unverified.

## Choose the right download

Open [v1.6.0-rc.2](https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases/tag/v1.6.0-rc.2), then expand Assets.

| Download | Use |
| --- | --- |
| NowShowing-TV-1.6.0-rc.2-debug.apk | Installs as **NowShowing Preview**, package `com.nowshowing.tv.preview`. Use this to test alongside your existing app. |
| NowShowing-TV-1.6.0-rc.2-release.apk | Minified build, package `com.nowshowing.tv`. An existing installation requires a matching signing certificate to update. |

Preview has separate, initially empty watchlist/history/settings. It does not copy or migrate data from the installed app. Its debug performance is not a measurement of minified release performance. The release contains checksums, public certificate fingerprints and exact build metadata. Source ZIP/TAR downloads are not installable apps.

## Install with a TV file manager

1. Copy the chosen APK to a USB drive or transfer it using your existing TV file-transfer method.
2. Open the APK in a TV file manager.
3. If prompted, allow that file manager to install unknown apps and confirm.
4. Open **NowShowing Preview** or **NowShowing TV**, according to the selected download.
5. Disable the file manager's install permission again if no longer needed.

Developer options are not required for file-manager installation.

## Install Preview using ADB

Enable the appropriate debugging option on your TV and authorize your computer. Older TVs commonly use network ADB on port 5555:

```sh
adb connect TV_IP:5555
adb install -r NowShowing-TV-1.6.0-rc.2-debug.apk
adb shell am start -n com.nowshowing.tv.preview/com.nowshowing.tv.ui.MainActivity
```

Newer TVs may require Wireless debugging pairing:

```sh
adb pair TV_IP:PAIRING_PORT
adb connect TV_IP:DEBUG_PORT
```

Use the ports displayed on your TV. Disable debugging when finished.

## Update without losing local data

The candidate is version **1.6.0-rc.2**, Android version code **16001**. Android preserves local data when package name, signing certificate and accepted version code match. Compare the published certificate report with your installed app before an upgrade.

If Android reports UPDATE_INCOMPATIBLE or a certificate mismatch, stop. Do not uninstall the existing app or clear its storage to bypass this: local watchlists, history and settings could be lost. A previous runner's private development key cannot be reconstructed from its APK. Use Preview alongside the existing app for testing, and obtain a matching-key release build for a compatible upgrade. There is currently no in-app export/restore.

A newly configured permanent key does not prove continuity with an existing certificate. See [release signing](RELEASE_SIGNING.md).

## First use

- Search accepts typed title words or the D-pad keyboard, with type/year filters.
- Watchlist's Show and Sort buttons cycle through local type and ordering options.
- Open a title, select an episode if needed, then open its player.
- Player arrows move the pointer; OK clicks provider controls. Back exits provider fullscreen, then opens app controls, then returns to the title. Menu opens controls directly.
- Use manual Next episode for web providers without verified completion support.
- Settings → App health records browsing on request and saves numbered photo-friendly report pages.
- Recently opened remembers title, episode and source; it does not guarantee timestamp resume inside third-party players.

See [known limitations](KNOWN_LIMITATIONS.md), [TV acceptance](TV_ACCEPTANCE.md) and [troubleshooting](TROUBLESHOOTING.md).
