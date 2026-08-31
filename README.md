# Poi Theme Demos

Two separate, offline Android apps for comparing visual directions before choosing Poi's final design.

**Archive branch: `codex/theme-demos`. Production Poi stays on `main`.**

This branch has independent history and contains only demo projects, their sample artwork,
the Gradle wrapper, documentation and installable test APKs. It contains no production app,
Supabase configuration, signing keys, GitHub Actions workflows or in-app update feed.
Do not merge this archive into `main`.

## Download and compare

| Demo | Look | APK | Source and notes |
| --- | --- | --- | --- |
| Scribble Lab · `0.1-theme-lab` | Hand-drawn notebook styling, paper and sketch-like details | [Download Scribble APK · 25.6 MiB](https://github.com/DhanushShriyan/Poi/raw/refs/heads/codex/theme-demos/prototypes/scribble-theme/dist/poi-scribble-theme-demo.apk) | [Scribble project](prototypes/scribble-theme/) |
| Geometric Retro Lab · `0.1-retro-lab` | Cream, mustard, teal and burnt-orange blocks, angular cards and bold geometry | [Download Retro APK · 8.3 MiB](https://github.com/DhanushShriyan/Poi/raw/refs/heads/codex/theme-demos/prototypes/geometric-retro/dist/poi-geometric-retro-demo.apk) | [Retro project](prototypes/geometric-retro/) |

Both require Android 8.0 or newer and use separate application IDs, so they can be installed
alongside one another and the main Poi app. No uninstall of Poi is needed. They are test-signed
theme previews, not production releases or real event services.

- Scribble: `com.dhanushshriyan.poi.scribble`
- Retro: `com.dhanushshriyan.poi.retrolab`

All events, photos and activity are sample content. Demo actions stay local. Neither app
uses the production database or changes a real booking/attendance record.

## Quick friend-testing checklist

1. Install both APKs on the same phone. Keep the normal Poi app installed.
2. Compare the home feed: readability, colours, card spacing and whether the theme feels inviting.
3. Open an event and try Interested / Going / I'm here. Compare how obvious the controls are.
4. Search for a sample event and browse dates/plans. In Retro, also change the sample distance filter.
5. In Retro, open Moments and try a like, a local comment and text sharing.
6. Increase Android's font size and check whether important labels still fit.
7. Report the phone model, Android version, demo name, screenshot and exact steps for any issue.

Suggested feedback: favourite theme, easiest screen to use, hardest screen to read, and the
one change that would most improve the experience. No device-level QA is implied by this archive.

## Build separately

Clone this branch into its own directory, not over a production checkout:

```sh
git clone --branch codex/theme-demos --single-branch https://github.com/DhanushShriyan/Poi.git PoiThemeDemos
cd PoiThemeDemos
```

Install/configure JDK 17 and the Android SDK (platform/build tools 36), then set `JAVA_HOME`
and `ANDROID_SDK_ROOT`, or supply a local `local.properties` file inside the chosen prototype.
The Gradle wrapper downloads its pinned version. Local SDK files, build caches and signing
keys must never be committed.

Windows PowerShell:

```powershell
.\gradlew.bat -p .\prototypes\scribble-theme test assembleDebug
.\gradlew.bat -p .\prototypes\geometric-retro test lintDebug lintRelease assembleRelease
```

macOS/Linux:

```sh
./gradlew -p prototypes/scribble-theme test assembleDebug
./gradlew -p prototypes/geometric-retro test lintDebug lintRelease assembleRelease
```

Use the project-specific `-p` argument; there is intentionally no production/root app to build.
A build on another computer may have a different debug certificate, so its APK may not install
over an archived APK without uninstalling that demo first. This does not affect Poi.

## Archive maintenance

- Save future theme iterations on this branch only; never use it as a production release branch.
- Commit the related source, updated test APK, version notes and SHA-256 checksum together.
- Both `dist/` APKs are deliberately tracked here for durable downloads; generated build folders are excluded.
- Git history preserves older snapshots. Open an earlier commit to retrieve the matching source and APK.
- No GitHub Release was created, so Poi's production update selection is unchanged.

Initial archive: 31 August 2026. Source files and APKs were copied from the local demos; Git
normalizes text line endings for cross-platform use, while APKs and images remain byte-for-byte
identical. [APK checksums](CHECKSUMS.sha256) allow downloaded binaries to be verified. The Retro
[validation record](prototypes/geometric-retro/VALIDATION.md) describes checks made before this
archival push; [artwork provenance and prompts](prototypes/geometric-retro/ARTWORK.md) are included.
