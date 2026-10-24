# LightStart

**Less waiting. A lighter start.**

LightStart is an offline Android utility that uses an Accessibility Service to tap supported splash-ad skip buttons. It provides per-app controls, optional custom rules, and local activity statistics.

- Android 8.0 (API 26) or later.
- No Internet permission, root, VPN, or overlay required.
- Enable supported apps in one step; customize rules only when needed.
- Automatically learn skip-button resource IDs after an accepted click. Missing IDs fall back to text matching.
- Optional foreground protection with a persistent notification, a stop action, and opt-in restoration after boot.
- Settings and statistics stay on your device.

The current app interface is in Simplified Chinese. LightStart does not block every ad or prevent every ad-triggered redirect. Results depend on the app, device, and system background restrictions.

## Download

- [Latest release and APKs](https://github.com/taoHua1996/LightStart/releases/latest)
- [APK builds for each push](https://github.com/taoHua1996/LightStart/actions/workflows/android.yml)
- [0.4.0 release notes](docs/0.4.0-release.md) and [validation results](docs/0.4.0-validation.md)

Current APKs use debug signing. To upgrade the locally installed 0.4.0 build, download `LightStart-0.4.0-local-debug.apk` from the release. This asset uses the same signing key as that installation. The separate `LightStart-0.4.0-debug.apk` is built by CI; CI APK signatures may differ between builds and may not support an in-place upgrade. Uninstalling the app removes its local data.

## Getting started

1. Install the APK and open LightStart.
2. Follow the in-app guide to enable its Accessibility Service in Android settings. Some devices require allowing restricted settings for sideloaded apps first.
3. Turn on automatic skipping and enable the apps you want to use it with. The bulk-enable action applies the default rules without configuring each app separately.
4. Run the built-in demo, then open your selected apps. Automatic detection covers exact skip labels in the upper-right and lower corners during the first 12 seconds. Learned IDs accelerate lookup; the current label, position, visibility and button size are still checked. Manual rules take precedence.
5. Open **Settings** to enable background protection and, optionally, restoration after boot. Allow notifications and review the linked battery and system autostart settings. OEM autostart permission cannot be read reliably and is never shown as granted merely because its settings page was opened.

Swiping away LightStart's activity does not intentionally stop foreground protection. Android may restart a reclaimed sticky service, but force-stop, the system task manager and some OEM cleaners can still terminate it. Reopen LightStart to recover. The notification stop action disables protection persistently; boot restoration respects that choice. Pausing automatic skipping also stops protection. Accessibility authorization and binding remain controlled by Android.

Automatic learning stores only app package names and qualified skip-button IDs, locally. It does not save page text or screenshots. Disable learning in Settings, or reset a learned rule from the app's optional advanced-rule dialog. Accepted clicks are not proof that an ad disappeared.

Skip counts represent click actions accepted by Android, not confirmed ad dismissals. Demo actions are excluded from third-party app totals.

If nothing skips, use the always-visible test button on the Overview page first. It explains missing setup steps without requiring any third-party app to be enabled. The latest check on that page distinguishes disabled apps, unmatched buttons, and accepted or rejected click actions. A successful demo verifies the basic service path; it does not confirm compatibility with every real ad.

## Build

Open this directory in Android Studio, or use the included Gradle Wrapper. Requirements: JDK 17 or later, Android SDK Platform 36, and Build Tools 36.0.0. Configure `ANDROID_HOME` or a local `local.properties` file.

```sh
# Build the debug APK and run Android Lint
./gradlew assembleDebug lintDebug
```

On Windows, use the packaging script to build, run Lint, verify the APK signature, and copy the APK with its SHA-256 checksum into `releases/`:

```powershell
powershell -ExecutionPolicy Bypass -File tools/package.ps1

# Run the standalone core policy checks
powershell -ExecutionPolicy Bypass -File tools/test-core.ps1
```

The raw Gradle APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The application ID remains `com.qingping.app` for compatibility with earlier installations.

Every push runs core policy checks, builds an APK, and runs Lint in GitHub Actions. Successful builds retain APK artifacts for 90 days. A `v*` tag matching the app version also publishes a release with the APK and SHA-256 checksum. Failed builds do not publish an APK.

## Repository access

This repository is public and maintained by [taoHua1996](https://github.com/taoHua1996). Only the owner has direct write access. You may report issues, fork the project, or propose changes through pull requests; the owner controls what is merged.

## License

[MIT](LICENSE)
