# Signed updates and GitHub Releases

## User experience

1. Install the latest signed Poi APK once.
2. Poi checks the public GitHub repository's latest release when the app starts.
3. When a newer version exists, Poi downloads it inside the app with visible byte and percentage progress.
4. Android asks the user to allow Poi as an installation source the first time.
5. Android's package installer asks for final approval for every update.

Downloads are bounded by connection, read, and overall timeouts. A stalled download can be cancelled or retried, and a browser download remains available as a fallback. Before opening Android's installer, Poi checks that the file is a newer build of the same package signed with the installed app's certificate.

If an older release is already stuck on the download screen, install the latest signed APK manually once. The fixed updater will handle later releases from inside Poi.

Silent self-installation is intentionally impossible for a normal Android app. The approval screen protects users from unauthorized software replacement.

## Release automation

Every push to `main` runs `.github/workflows/release.yml`. The workflow:

- assigns a monotonically increasing Android version code;
- runs lint and unit tests;
- builds a signed release APK;
- creates a GitHub Release tagged with the new version; and
- attaches the APK that installed clients discover.

Pull requests and development branches run `.github/workflows/android.yml` without publishing an update.

## Required GitHub Actions secrets

- `POI_KEYSTORE_BASE64`
- `POI_KEYSTORE_PASSWORD`
- `POI_KEY_ALIAS`
- `POI_KEY_PASSWORD`

The local keystore and `keystore.properties` are ignored by Git. Losing both the local keystore and the GitHub secret makes it impossible to update existing installations. Keep the local `.signing` directory in the protected project backup and never commit or share its contents.

## Version policy

The initial update-ready build is version `0.1.0` with version code `1`. GitHub Actions uses version names `0.1.<workflow run number>` and version codes starting at `1001`, guaranteeing that each published build can replace the previous one.
