# Poi cloud setup

Poi now supports a real Supabase backend for Postgres, authentication, row-level security, and realtime event updates. If cloud settings are absent, debug builds retain the local preview repository; production releases are configured to fail in GitHub Actions when the cloud secrets are missing.

## 1. Create the free project

Create a Supabase Free project and record its Project URL and publishable/anonymous key from **Project Settings → API**. The publishable key is designed for client apps; never put a service-role key in the Android project or GitHub build.

As checked on 19 August 2026, Supabase Free advertises 50,000 monthly active users, a 500 MB database, 1 GB file storage, and free projects that may pause after one week of inactivity. Recheck limits before launch: <https://supabase.com/pricing>.

## 2. Apply the database

Run `supabase/migrations/202608190001_poi_core.sql` in the Supabase SQL editor. It creates:

- profiles with protected member/admin roles;
- public, circle, and invite-only event records;
- attendance and check-in visibility;
- event reports and moderation state;
- row-level security for guests, members, owners, and administrators;
- realtime publication for event changes;
- three starter events for the first connected test.

The same migration can be deployed with the Supabase CLI after linking the project:

```bash
supabase link --project-ref YOUR_PROJECT_REF
supabase db push
```

## 3. Configure authentication

Email/password authentication works after Email is enabled in **Authentication → Providers**. Add `poi://auth-callback` to the allowed redirect URLs.

For Google sign-in, enable Google in Supabase, create an OAuth client in Google Cloud, and copy the client ID and secret into the Supabase provider configuration. Supabase shows the exact OAuth callback URL that must be added in Google Cloud. Poi returns from the browser through `poi://auth-callback`.

Phone OTP is intentionally off by default. It requires an SMS provider supported by Supabase and can create real per-message costs and abuse risk. Configure the provider first, then enable the build flag.

## 4. Configure a local build

Copy `supabase.properties.example` to the ignored `supabase.properties` file and fill in the public project values:

```properties
url=https://YOUR_PROJECT_REF.supabase.co
publishableKey=YOUR_PUBLISHABLE_KEY
googleAuthEnabled=false
phoneAuthEnabled=false
```

Environment variables are also supported:

- `POI_SUPABASE_URL`
- `POI_SUPABASE_PUBLISHABLE_KEY`
- `POI_GOOGLE_AUTH_ENABLED`
- `POI_PHONE_AUTH_ENABLED`

## 5. Configure automatic releases

Add these GitHub Actions repository secrets:

- `POI_SUPABASE_URL`
- `POI_SUPABASE_PUBLISHABLE_KEY`
- `POI_GOOGLE_AUTH_ENABLED` (`false` until Google OAuth is configured)
- `POI_PHONE_AUTH_ENABLED` (`false` until SMS is configured)

The release workflow injects the public client configuration into the signed APK. Existing signing secrets remain unchanged.

## 6. Bootstrap the private administrator

First create the administrator through the normal email signup flow. Then run this once in the Supabase SQL editor, replacing the email:

```sql
update public.profiles
set role = 'admin', updated_at = now()
where id = (
    select id from auth.users where lower(email) = lower('owner@example.com')
);
```

The admin entry remains hidden in Poi: sign out, open **Profile**, then press and hold **Poi · account access**. Use the administrator email and its normal account password. The app checks the protected server role before opening the console, and every privileged event update is independently enforced by database policy.

## Security rules

- Never commit a service-role key, signing key, OAuth client secret, or SMS provider secret.
- Admin status lives only in the protected `profiles.role` column; a hidden screen is not treated as authorization.
- Row-level security remains enabled on every connected table.
- Public keys in an APK are expected; database policies are the security boundary.
- Add audit logging and account/session revocation before a large public launch.
