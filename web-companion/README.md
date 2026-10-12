# Poi trip companion

Live: https://poi-trip-companion.dhanushriyan22.chatgpt.site

This mobile-first static companion uses the same Supabase Auth, event/photo storage, friendships, expense RPCs and feedback table as Android. The login page is publicly reachable with the owner's approval; data access is enforced by existing Supabase RLS, not by hiding buttons. `dist/config.js` contains only the intentionally public client configuration; never put service-role, Gemini or OAuth secrets there.

Use email/password (including Gmail addresses). Google appears only if the existing provider is enabled; Google OAuth and phone/SMS were not configured at this release. This is not Sign in with Apple and does not require an Apple developer account. On iPhone, use Safari → Share → Add to Home Screen. No offline-write queue or background geofence is promised: reconnect and confirm successful saves before assuming anything synced.

Included: create an event/trip; accepted-friend/private visibility; interested/going/manual check-in; photo resize/upload, likes, comments, unique views and expiring share links; friend requests; private expense groups/invitations; equal/exact/percentage/share splits with integer currency arithmetic; receipts; editing and soft deletion; pending payments confirmed by recipients; CSV export; private tester feedback/history. The web form supports one selected payer; use Android for multi-payer expenses, advanced expense comments/activity/UPI actions, GPS verification, event-specific private invitations and the full calendar/theme features. Multi-payer expenses cannot be edited on web without a deliberate change to one payer.

Vendor: official `@supabase/supabase-js` 2.117.3 browser bundle from jsDelivr, MIT licensed. The SDK handles session persistence/refresh; the companion does not invent a separate authentication protocol. It is hosted as a public static Site using the normal authenticated Supabase client, not Sites/ChatGPT identity as a substitute for Poi accounts.

Run `node --check dist/app.js` and `node --test test/money.test.mjs`. The Sites checkout/identity is isolated at the workspace's visualization output location to avoid embedded Git repositories in the Android checkout. Its project ID is `appgprj_6accceafecac81919bbe76c26d03d166`; update that same Site, not a duplicate. For future publication, open the saved Site through its source helper, copy only changed companion files from this directory, package the exact pushed source and deploy through Sites. Keep `.openai/hosting.json` in the Site checkout.
