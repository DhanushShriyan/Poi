# Free-tier cloud plan

The test APK makes no network request and needs no API key, payment card, or cloud account.

For the first connected beta, use:

- Supabase Free for Postgres, authentication, row-level security, realtime, and event media.
- Firebase Cloud Messaging for push delivery; FCM is a no-cost product.
- Android geo intents for directions instead of embedding Google Maps, avoiding a Maps billing account during the beta.
- GitHub Actions for repeatable builds.

As checked on 19 August 2026, Supabase Free advertises 50,000 monthly active users, a 500 MB database, 1 GB file storage, and free projects that may pause after one week of inactivity. Recheck before production: <https://supabase.com/pricing>.

Firebase Cloud Storage is not selected because new and existing Firebase Storage use requires a Blaze billing account as of February 2026, even though no-cost usage allowances may remain: <https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024>.

## Planned schema

- `profiles`: display name, handle, home area, trust state.
- `friendships`: requester, receiver, state.
- `circles` and `circle_members`.
- `events`: core listing data, visibility, verification, lifecycle status.
- `event_invites`: private-event access.
- `attendance`: interested, going, checked-in, attended, visibility.
- `reports`: target, reason, reporter, moderation state.
- `moments`: event-scoped media with audience and moderation state.
- `messages`: temporary event conversations.
- `organizer_claims`: organizer and venue verification evidence.

Every table containing personal data requires row-level security. Public event counts should be served separately from attendee identities.

## Secrets

- Supabase public URL and anonymous key may be supplied through local Gradle properties.
- Service-role keys must exist only in server-side functions.
- FCM server credentials must exist only in a trusted server environment.
- Never commit `google-services.json`, signing keys, or service-account JSON.

## Account choice

Start with Google/email authentication. Phone OTP is excluded from the free beta because SMS verification requires billable Firebase usage and creates avoidable abuse risk.

