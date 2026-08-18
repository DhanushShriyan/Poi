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

The test build uses an offline repository with local persistence. `EventRepository` is the boundary for a later Supabase implementation, so cloud sync can be added without rewriting feature screens.

## Modules

| Module | Responsibility |
|---|---|
| `app` | App shell, navigation, dependency assembly |
| `core:model` | Platform-independent product models |
| `core:data` | Repository contract and offline implementation |
| `core:designsystem` | Theme and shared UI components |
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

## Production services

No credentials are committed. The planned Supabase, Firebase Cloud Messaging, and Google Play setup is documented in [docs/CLOUD_SETUP.md](docs/CLOUD_SETUP.md).
