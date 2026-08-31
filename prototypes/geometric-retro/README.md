# Poi Geometric Retro Lab

A separate, offline Android theme prototype inspired by the supplied geometric-retro reference.

- App label: **Poi Retro Lab**
- Application ID: `com.dhanushshriyan.poi.retrolab`
- Version: `0.1-retro-lab` (code 1)
- Minimum Android: 8.0 / API 26
- Test certificate: Android debug certificate (not Poi's production signing key)

This project is not included in Poi's root settings or release workflow. It does not import
production modules, read production properties, connect to Supabase or request internet,
location, account, camera or notification access. AndroidX declares its own package-scoped
receiver-protection permission. It can coexist with both Poi and Poi Scribble Lab.

## Included

- Cream, mustard, teal and burnt-orange colour blocking
- Angular cut-corner cards, thin grid rules, geometric marks and a five-block navigation bar
- Five fictional events with dates relative to the day the demo is opened
- Three original AI-generated editorial event photos
- Feed, search, category and sample-distance filters
- Event details and local Interested / Going / I'm here reactions
- Calendar and My plans
- Sample moments with local likes, comments and Android text sharing
- Local demo profile and theme-comparison prompts

Reactions and comments persist only in this package's private preferences. There are no
real users, bookings, uploads, GPS checks or remote notifications. Demo sharing text
explicitly labels listings as fictional.

## Build independently

From the Poi repository root, use the existing wrapper without changing its project:

```powershell
$env:JAVA_HOME='<Poi workspace>\.tooling\jdk\jdk-17.0.20+8'
$env:ANDROID_SDK_ROOT='<Poi workspace>\.tooling\android-sdk'
.\gradlew.bat -p .\prototypes\geometric-retro test lintDebug lintRelease assembleRelease
```

Deliverable: `dist/poi-geometric-retro-demo.apk`.

## Quick theme test

1. Install this APK beside Poi and Scribble Lab; do not uninstall either.
2. Scroll the Feed and open each photo/event card.
3. Search for “analog” or “form”, switch categories, then change the sample radius.
4. Mark an event Going and find it under Plans → My plans.
5. Compare the date-calendar tiles and the event-detail layout.
6. Open Moments; like a photo, add a test comment and try sharing.
7. Close/reopen the demo to verify local reactions remain.
8. Compare readability, colour density, photo treatment and navigation against Scribble Lab.

Artwork generation provenance and exact prompts: [ARTWORK.md](ARTWORK.md).

Build checks, APK checksum and remaining device checks: [VALIDATION.md](VALIDATION.md).
