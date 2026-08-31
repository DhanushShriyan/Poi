# Retro Lab validation

Validated 31 August 2026. This is a standalone theme prototype, not a Poi release.

## Build and package

- `test lintDebug lintRelease assembleRelease`: successful.
- Eight unit tests passed in both debug and release variants (zero failures/errors).
- Both Android lint variants passed with zero errors. Two informational dependency-update warnings remain; cached, pinned library versions were retained.
- Release APK verified with Android APK Signature Scheme v2 using the Android debug test certificate.
- Package: `com.dhanushshriyan.poi.retrolab`; label: `Poi Retro Lab`.
- Version: `0.1-retro-lab`, code 1; minimum Android 8.0 / API 26.
- APK size: 8,659,997 bytes (approximately 8.3 MiB).
- SHA-256: `3B94B8315C8724B5820E05AE17A0D66204FB0698CDC37EB8E35F942B2DA18BAA`.
- Deliverable: `dist/poi-geometric-retro-demo.apk`.

## Isolation

- Main Poi tracked files were unchanged (`git diff --stat` empty).
- Existing Scribble source/artwork hashes match the pre-work baseline.
- Separate Android identity means this package does not upgrade/replace either existing app.
- No Git commit, push, release or database operation was performed.
- Manifest inspection confirms no internet, location, camera, notification or account permissions. The only declared permission is AndroidX's package-scoped receiver-protection permission.
- Preferences are package-private; backup and device-transfer rules exclude them.

## Still needs phone testing

No Android device/emulator was connected. Installation, on-screen layout, keyboard behaviour,
large-font accessibility and sharing-sheet behaviour have **not** been exercised on a device.
Use the steps in README.md, including a smaller phone and an increased system font size.

All event listings, attendance counts and social posts are fictional. RSVP, likes and comments
are local demo actions; no real attendance, upload or booking is performed.
