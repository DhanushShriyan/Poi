# Goa / Munnar trip testing release

## Before travelling

1. Update Android through Profile → Check app updates. iPhone users open https://poi-trip-companion.dhanushriyan22.chatgpt.site in Safari and may Add to Home Screen.
2. Create an account with your real Gmail/other email and password. Existing accounts use the same credentials in both clients. Google/SMS buttons are not working methods until their external providers are configured. Do not enter someone else's email or reuse an important password.
3. Search for your friends in People, send a request, and accept from the other account.
4. Create separate Goa and Munnar events with your real dates and locations. Private events require explicit invitations or an owner-issued guest join link on Android/web; friendship alone does not grant access. Public events expose details and albums; expenses remain group-private. For Android, use Share guest join link inside your event; on web, open the event and use Create guest join link. Send it only to intended guests. Friends open it, sign in, and tap Join invited event. Links last 30 days; replacement/revocation blocks further joins without removing already-joined members. A plain private event link opens only for existing members.
5. Confirm everyone accepted the event invitation or joined its guest link. Confirmed members automatically get shared expense access; Interested/Going alone is not membership. Open shared expenses and verify all participants are active. New contributions use the same shared ledger. If older versions created several circles, use the ledger selector; existing charges and settlements stay in their original ledger.

## During the trip

1. Choose Interested / Going. When actually attending, check in. Web check-in is self-reported, not GPS-verified.
2. Add a camera photo under that event and an optional caption, including after getting home and without selecting I'm here. Sign-in and event access are still required. Confirm another permitted account sees it. Like/unlike and comment from both devices. Verify counts after Refresh. Private photo sharing generates an expiring bearer link; anyone you send it to may view it until expiry.
3. Add fuel, food and stay expenses. Try ₹100.01 split between three people; test equal, exact, percentage and shares. Confirm allocations add to the total. Both Android and web support multiple payers; test ₹60.01 + ₹40 paid on a ₹100.01 expense. On web, expand Payers, split & comments and Group activity.
4. Upload a receipt and confirm a nonmember cannot see it. Edit an expense and verify both accounts update. Delete only your own test expense; it is a soft deletion with an audit record.
5. Pay outside Poi, record a pending settlement and let the recipient confirm receipt. Pending records must not change balances. Poi never transfers money or treats opening a payment app as proof of payment.
6. Refresh after restoring mobile data or returning to the app. Neither client should claim a failed write was saved. There is no guaranteed offline expense queue. Do not rely on cached balances to make a final settlement without refreshing.
7. Android feedback: Profile → Privacy & notifications → Send trip testing feedback. Web: Feedback tab. Include feature, rating, what happened, expected result and reproduction steps. Never include passwords, OTPs, bank details or private receipts. Check your submitted feedback appears in history.

## Verification performed

Live disposable-account tests cover email signup/password login, private event/photo access before and after an accepted invitation, a real Postgres realtime photo update, likes/comments/unique views, signed image access, feedback persistence/privacy/impersonation rejection, and cleanup. The earlier expense integration script passed invitations, split validation, receipts/privacy, recipient confirmation, activity and soft deletion. Unit tests cover feedback input, sign-in versus signup validation, expense calculations and formatting, and web integer-currency calculations. Android build/lint and hosted-browser QA complement these tests but do not certify physical iPhone/Safari or Android behavior: friends must still test their real devices.

Poster-to-event UI is hidden, not removed from the backend or source. No trip events or dates are invented or seeded into your live data. Both clients point to the permanent `pzjojdkcnufdnrcjmdkb` project. Apply `202610120001_trip_feedback.sql` once, and record dashboard-applied migrations before future CLI migration synchronization.

## Specific event / iPhone regression checks

1. Open an event link while signed out. Public events show a preview; private events do not expose their details. Sign in and ensure the intended event remains selected. Unknown/private IDs must not open a different event silently.
2. The owner creates a join link for a private event. A nonfriend account joins using it, can see details/photos, and now also sees shared expenses without a separate expense invitation. Verify a nonmember still cannot see those expenses.
3. Replace/revoke the link and verify the old link fails for new joins. Existing membership remains intact.
4. Tap Open event on a card, then test location, full description, calendar export, photos and expenses. Owners test Edit event and invitations.
5. Check the Calendar tab, optional January local calendar and Kannada switch. Profile contains theme/light-dark selection. Radius filtering requires foreground location and excludes unlocated events when enabled.
6. Browser limitations remain: no background geofencing, native APK installer, Android admin console or UPI-app-specific integration. This release expands core trip flows; it does not claim complete native feature parity.

`202610120002_event_links_and_later_photos.sql` was applied to the live dashboard with owner approval. `scripts/test-event-links.mjs` requires explicit disposable-account cleanup approval before execution.
