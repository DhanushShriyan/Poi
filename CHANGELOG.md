# Changelog

## 0.4 location + social release — 2026-08-26

- Added foreground-only location discovery with real on-device distance calculation and user-selected radius filtering.
- Added privacy-safe proximity check-in: raw coordinates are cleared by the database trigger and only the verification result, distance, and accuracy are retained.
- Added venue coordinates and organizer-selected check-in areas to event creation, with a manual fallback for events without a venue point.
- Added a realtime People area with limited public profiles, friend requests, accepted friendships, friend removal, event invitations, and invitation acceptance.
- Added privacy-aware friend activity and server-enforced controls for sharing future plans and check-ins.
- Added personalized discovery using saved categories and opted-in friend activity.
- Added opt-in, device-local event reminders that survive a phone restart and require no paid messaging service.
- Kept social, location, notification, and feature UI code isolated behind dedicated modules and repository contracts.

## 0.2 connected foundation — 2026-08-25

- Connected email accounts, profiles, events, attendance, reports, and event moments to Supabase.
- Added realtime event refresh, visible connection health, resilient retries, and server-authoritative attendance counts.
- Added editable synced profiles and privacy-aware permanent account deletion, including uploaded-image cleanup.
- Added private-event membership policies, stricter row-level security, protected administrator controls, and an immutable admin audit log.
- Kept Google OAuth ready behind configuration and phone OTP disabled until a safe SMS provider is selected.
- Added Release 0.2 tests and retained automatic signed GitHub update delivery.

## 0.1.0 update delivery

- Added automatic GitHub Release checks for signed production builds.
- Added in-app APK downloading and Android installer handoff.
- Added stable release signing support and main-branch release automation.
- Added an isolated `core:update` module and release operations guide.

## 0.1.0-test — 2026-08-19

- Added modular native Android app shell and Poi visual system.
- Added realistic offline event discovery, search, filters, and event details.
- Added interested, going, privacy-aware check-in, plans, and local persistence.
- Added public/private event creation with validation.
- Added profile, trust indicators, privacy settings, reports, and safety centre.
- Added unit tests, GitHub build workflow, privacy draft, cloud plan, and test documentation.
