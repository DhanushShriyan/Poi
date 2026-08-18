# Administrator access

## Opening the console

Administrator entry is intentionally absent from ordinary navigation.

1. Sign out and open **Profile**.
2. Press and hold **Poi · account access** at the bottom of the guest profile.
3. Enter the configured administrator email and private access code.
4. After verification, use **Profile → Administration**.

The console can search all listings, inspect locally reported events, restore reports, create events, and edit or delete every event. The editor controls titles, descriptions, organizers, venue and address, dates, categories, visibility, verification, attendance count, featured state, cancellation state, and organizer verification.

## Build configuration

Local credentials are read from ignored `admin.properties`:

```properties
email=owner@example.com
codeSha256=<lowercase SHA-256 of the private code>
```

Release builds read `POI_ADMIN_EMAIL` and `POI_ADMIN_CODE_SHA256` from GitHub Actions secrets. The private code itself is never committed or uploaded as a workflow secret.

## Security boundary

This offline APK verifies a high-entropy local access code and protects every admin screen with an in-app role check. That is suitable for a private product preview, but it is not a substitute for server authorization.

Before cloud data or public onboarding is enabled:

- authenticate users through the chosen identity provider;
- store the administrator role only in a protected server-side table or signed claim;
- enforce the role for every create, update, delete, verification, and moderation request;
- enable an audit log and revoke sessions after credential changes;
- never rely on a hidden screen or client-side role as the security control.
