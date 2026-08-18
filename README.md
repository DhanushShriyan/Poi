# Poi Android

Poi is a privacy-first, hyperlocal event discovery and friend-planning Android app. This repository contains a modular native Android MVP that works without a cloud account, paid API, or map key.

## Test build capabilities

- Browse, search, and filter realistic nearby events.
- See live, upcoming, verified, private, and community-submitted event states.
- Mark events as interested, going, or checked in.
- Keep check-in visibility private, friends-only, or event-visible.
- Create public, circle, or invitation-only events and persist them on-device.
- View personal plans and profile statistics.
- Share event details and open directions in the installed maps app.
- Report events and hide reported content locally.
- Configure privacy and notification preferences.
- Detect, download, and hand off signed updates from GitHub Releases.

The test build uses an offline repository with local persistence. `EventRepository` is the boundary for a later Supabase implementation, so cloud sync can be added without rewriting feature screens.

## Modules

| Module | Responsibility |
|---|---|
| `app` | App shell, navigation, dependency assembly |
| `core:model` | Platform-independent product models |
| `core:data` | Repository contract and offline implementation |
| `core:designsystem` | Theme and shared UI components |
| `core:update` | Release checking, APK download, and installer handoff |
| `feature:discover` | Discovery feed and event details |
| `feature:plans` | Saved and upcoming plans |
| `feature:create` | Event creation flow |
| `feature:profile` | Profile, privacy, and safety settings |

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) before making structural changes.

## Build

On Windows, after Android SDK 36 and JDK 17 are installed:

```powershell
.\gradlew.bat test assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Automatic test updates

The distributable APK is signed with a stable project key. Every push to `main` runs tests, creates a higher version, builds a signed APK, and publishes it as the latest GitHub Release. A release build of Poi checks that release on launch, downloads a newer APK inside the app, and opens Android's package installer.

Android does not permit ordinary apps to install themselves silently. The user must approve installation and, on the first update, allow Poi as an APK installation source. See [docs/RELEASES.md](docs/RELEASES.md).

## Production services

No credentials are committed. Release signing values live in local ignored files and GitHub Actions secrets. The planned Supabase, Firebase Cloud Messaging, and Google Play setup is documented in [docs/CLOUD_SETUP.md](docs/CLOUD_SETUP.md).
