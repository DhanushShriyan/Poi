# Administrator access

## Opening the console

Administrator entry is intentionally absent from ordinary navigation.

1. Sign out and open **Profile**.
2. Press and hold **Poi · account access** at the bottom of the guest profile.
3. Enter the administrator email and its account password.
4. After verification, use **Profile → Administration**.

The console can search all listings, inspect locally reported events, restore reports, create events, and edit or delete every event. The editor controls titles, descriptions, organizers, venue and address, dates, categories, visibility, verification, attendance count, featured state, cancellation state, and organizer verification.

## Cloud role setup

Production administration is authorized by the protected `profiles.role` column in Supabase. Create the account normally, then promote it once from the Supabase SQL editor:

```sql
update public.profiles
set role = 'admin', updated_at = now()
where id = (select id from auth.users where lower(email) = lower('owner@example.com'));
```

The app verifies that role after authentication, and Supabase row-level security independently enforces privileged event changes. Full cloud instructions are in `docs/CLOUD_SETUP.md`.

For an offline developer preview only, ignored `admin.properties` can still provide a local email and SHA-256 access-code hash. Production releases do not depend on that fallback.

## Security boundary

The hidden gesture prevents casual discovery but is not the security boundary. The server role and database policies protect create, update, delete, verification, and moderation operations. Add a durable audit log and session revocation controls before a large public launch.
