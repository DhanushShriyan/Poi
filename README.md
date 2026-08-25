# Poi Android

Poi is a privacy-first, hyperlocal event discovery and friend-planning Android app. This repository contains a modular native Android app backed by Supabase's free tier, with a local preview fallback for builds that do not contain cloud configuration.

## Test build capabilities

- Browse, search, and filter realistic nearby events before creating an account.
- Switch between persisted light and dark appearances.
- Create and use real email accounts, with Google OAuth and phone OTP gated until their providers are configured.
- See live, upcoming, verified, private, and community-submitted event states.
- Mark events as interested, going, or checked in.
- Calculate real distance on-device with a user-selected discovery radius and no background location tracking.
- Verify eligible check-ins by proximity while retaining only the verification result, not raw device coordinates.
- Keep check-in visibility private, friends-only, or event-visible.
- Create public, circle, or invitation-only events and sync them across devices.
- View personal plans and profile statistics.
- Share event details and open directions in the installed maps app.
- Upload event photos, then view, like, comment, share, and delete them under the event.
- Report events and hide reported content, with protected administrator moderation.
- Edit a synced profile, monitor cloud connection health, retry failed loads, and permanently delete an account.
- Configure privacy and notification preferences.
- Find friends, manage requests, invite accepted friends, and follow privacy-aware live activity.
- See personalized event picks and opt into local one-hour event reminders.
- Detect, download, and hand off signed updates from GitHub Releases.
- Use a role-protected administrator console to review reports and edit, cancel, feature, verify, restore, or delete any event.

Release builds use Supabase for accounts, profiles, events, attendance, reports, event moments, friendships, invitations, activity, and realtime refresh. Debug builds use the same connected path when `supabase.properties` is configured, otherwise they fall back to isolated local preview data.

Google sign-in becomes real when its Supabase provider and build flag are enabled. Phone OTP stays off until an SMS provider and abuse controls are configured because SMS is not reliably free. See [docs/ADMIN_ACCESS.md](docs/ADMIN_ACCESS.md) for restricted access and the production security boundary.

## Modules

| Module | Responsibility |
|---|---|
| `app` | App shell, navigation, dependency assembly |
| `core:model` | Platform-independent product models |
| `core:data` | Repository contracts plus Supabase and local-preview implementations |
| `core:auth` | Supabase authentication, preview identity, account deletion, and admin policy |
| `core:designsystem` | Theme and shared UI components |
| `core:location` | Foreground location access and in-memory location state |
| `core:notifications` | Private device-local event reminder scheduling |
| `core:update` | Release checking, APK download, and installer handoff |
| `feature:discover` | Discovery feed and event details |
| `feature:plans` | Saved and upcoming plans |
| `feature:create` | Event creation flow |
| `feature:profile` | Profile, privacy, and safety settings |
| `feature:social` | Friend search, requests, invitations, and activity |
| `feature:auth` | Guest, member sign-in, and restricted access screens |
| `feature:admin` | Moderation dashboard and full event editor |

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

No credentials are committed. Release signing values live in local ignored files and GitHub Actions secrets. Supabase setup and the deliberately deferred Firebase Cloud Messaging and Google Play work are documented in [docs/CLOUD_SETUP.md](docs/CLOUD_SETUP.md).
