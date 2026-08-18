# Test plan

## Installation

1. Allow installation from the app used to open the APK.
2. Install `poi-0.1.0-test.apk` on Android 8.0 or newer.
3. Android may warn that this is a debug-signed package; this is expected for a test build.

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
- The APK uses a debug certificate and cannot be published as a production release.

Report feedback to `mjshriyan8@gmail.com` with the screen name, steps, expected result, actual result, device model, and Android version.

