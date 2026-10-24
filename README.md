# LightStart

**Less waiting. A lighter start.**

LightStart is an offline Android utility that uses an Accessibility Service to tap supported splash-ad skip buttons. It provides per-app controls, optional custom rules, and local activity statistics.

- Android 8.0 (API 26) or later.
- No Internet permission, root, VPN, or overlay required.
- Enable supported apps in one step; customize rules only when needed.
- Settings and statistics stay on your device.

The current app interface is in Simplified Chinese. LightStart does not block every ad or prevent every ad-triggered redirect. Results depend on the app, device, and system background restrictions.

## Download

- [Latest release and APKs](https://github.com/taoHua1996/LightStart/releases/latest)
- [APK builds for each push](https://github.com/taoHua1996/LightStart/actions/workflows/android.yml)

Current APKs use debug signing. To upgrade an existing local test installation, use the `local-debug.apk` release asset signed with the same key. CI APK signatures may differ between builds and may not support an in-place upgrade. Uninstalling the app removes its local data.

## Getting started

1. Install the APK and open LightStart.
2. Follow the in-app guide to enable its Accessibility Service in Android settings. Some devices require allowing restricted settings for sideloaded apps first.
3. Turn on automatic skipping and enable the apps you want to use it with. The bulk-enable action applies the default rules without configuring each app separately.
4. Run the built-in demo to check the service, then try your selected apps. If an app is not supported by the defaults, add a precise text or resource-ID rule. Disable the app if a rule taps the wrong control.
5. Adjust battery or autostart settings if your device stops the service in the background. These settings cannot guarantee uninterrupted operation.

Skip counts represent click actions accepted by Android, not confirmed ad dismissals. Demo actions are excluded from third-party app totals.

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
