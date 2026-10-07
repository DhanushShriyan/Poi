# Changelog

## Poster reader field-test improvements

- Prefer layout-aware titles and filter low-confidence OCR words; normalize small/dark poster images and use an English-header pass in automatic mode.
- Suggest unlabelled footer venues, dates without years, selected show dates and multiple start times. Users explicitly confirm years, occurrences and ranges before applying a draft.
- Add parser regression cases based on six user-supplied local poster examples; preserve historical dates and never infer show durations.

## Unreleased — themes and local calendar

- Added an offline Kannada/English/Hindi neural poster reader with editable event drafts, language selection, rotation, low-quality warnings, cancellation and bounded scanning. No image upload or per-scan fee.
- Replaced placeholder event scheduling with exact start/end date and time pickers. Uncertain dates, years, times and duration require review before publishing.

- Added private, event-scoped shared expense groups for accepted friends.
- Added multiple payers and equal, exact, percentage, or share-based splits with paise-safe totals.
- Added live balances, simplified settlement suggestions, UPI handoff, recipient-confirmed payments, receipts, comments, search, category totals, activity history, and CSV sharing.
- Protected expense data and receipt images with server-side membership rules; Poi records settlements but never holds money or connects to bank accounts.

- Migrated production accounts and event data to the permanent Supabase project.
- Restored email sign-up and sign-in with clear validation and user-friendly recovery errors.
- Added live starter events plus an offline event fallback so Discover remains useful during outages.
- Made newly published events appear immediately and added reliable failure recovery to event creation.

- Added Classic, Poi Pulse Lab, and Poi Retro Lab as production app-wide styles.
- Added a clean Profile appearance selector for guests and signed-in users, with Classic as the default.
- Persisted the chosen app style locally and retained light/dark appearance controls for every style.

- Added a subtle January 2026 local-calendar entry point to Discover.
- Added a dot-based month overview with progressive disclosure for detailed observances.
- Added reviewed bundled Sharada Calendar entries plus Mangaluru sunrise and sunset data.
- Made English the calendar default and added a persistent English/ಕನ್ನಡ language switch.
- Isolated calendar models, data, UI, and tests in a dedicated feature module for safe month-by-month expansion.

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
