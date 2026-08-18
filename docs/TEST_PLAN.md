# Test plan

## Installation

1. Allow installation from the app used to open the APK.
2. Install `poi-0.1.0-update-ready.apk` on Android 8.0 or newer.
3. Confirm Android identifies the application as Poi.

## Smoke test

- Open Discover and scroll through seeded events.
- Search for `sale`, `Kadri`, and an unknown term.
- Filter by Festival, Concert, Private, and All.
- Open an event and test Interested, Going, and I’m here.
- Choose each check-in privacy option.
- Open Plans and confirm the event appears in the correct section.
- Create an event with missing fields and confirm validation.
- Create a valid public or private event and reopen the app; confirm it persists.
- Toggle privacy and notification settings, reopen, and confirm persistence.
- Open directions with and without a maps app installed.
- Report one sample event and confirm that it disappears locally.
- Rotate the device, switch light/dark mode, and test large font size.

## Known test-build limitations

- Data is local to one device and does not represent real users.
- Poster scanning, media upload, messaging, push notifications, and server verification require cloud setup.
- Cloud user accounts and media are still outside this offline MVP.

## Signed update checks

- Confirm `assembleRelease` produces a signed APK when local signing properties are present.
- Verify the APK certificate with `apksigner verify --print-certs`.
- Install version `0.1.0`, publish a higher GitHub Release, and confirm the update dialog appears.
- Confirm the APK downloads inside Poi and Android's installer accepts it as an update rather than a separate app.
- Confirm Later dismisses the prompt and an offline update check does not block app startup.

Report feedback to `mjshriyan8@gmail.com` with the screen name, steps, expected result, actual result, device model, and Android version.
