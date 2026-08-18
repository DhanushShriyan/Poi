# Poi privacy policy — offline test build draft

Effective date: 19 August 2026

Poi is currently an offline test application operated using the contact address `mjshriyan8@gmail.com`.

## Data handled by this test build

The test build stores event responses, locally created events, privacy preferences, notification preferences, and report choices on the Android device. It does not upload this information to a Poi server. It does not request precise location, contacts, camera, microphone, or photo-library permission.

When a user chooses Share, Directions, or Email feedback, Android opens another installed application. Information handled by that other application is governed by its privacy policy.

## Update checks

Release builds contact GitHub's public Releases API when Poi starts. GitHub receives standard network information such as the device's IP address and user agent. When a user accepts an update, Android's Download Manager retrieves the signed APK from GitHub. Poi does not send profile, event, attendance, or location data during this process.

## Retention and deletion

Local test data remains until the application data is cleared or the application is uninstalled. The production account version must add in-app account deletion and a public web deletion mechanism before release.

## Children

The test build is designed for users aged 18 and above and is not directed to children.

## Security

The test build excludes local preferences from Android cloud backup. A future connected version will require encryption in transit, row-level access controls, abuse monitoring, and a separate production privacy policy describing each cloud processor.

## Contact

Privacy questions: `mjshriyan8@gmail.com`.

This is a product draft and should be reviewed for the launch jurisdiction before public release.
