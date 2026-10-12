# Goa / Munnar trip testing release

## Before travelling

1. Update Android through Profile → Check app updates. iPhone users open https://poi-trip-companion.dhanushriyan22.chatgpt.site in Safari and may Add to Home Screen.
2. Create an account with your real Gmail/other email and password. Existing accounts use the same credentials in both clients. Google/SMS buttons are not working methods until their external providers are configured. Do not enter someone else's email or reuse an important password.
3. Search for your friends in People, send a request, and accept from the other account.
4. Create separate Goa and Munnar events with your real dates and locations. Use Private circle for accepted friends, or Invite only and explicitly invite people through Android. Public events expose event details and albums to people who can view public events; expenses remain group-private.
5. Create an expense group, invite accepted friends and have each friend accept before including them in expenses. Verify every participant appears as active.

## During the trip

1. Choose Interested / Going. When actually attending, check in. Web check-in is self-reported, not GPS-verified.
2. Add a camera photo under that event and an optional caption. Confirm a different account sees it. Like/unlike and comment from both devices. Verify counts after Refresh. Private photo sharing generates an expiring bearer link; anyone you send that link to may view it until it expires.
3. Add fuel, food and stay expenses. Try an odd total like ₹100.01 split between three people to check rounding. Try equal, exact, percentage and shares; confirm allocations add to the total. Android also supports multiple payers and advanced group tools.
4. Upload a receipt and confirm a nonmember cannot see it. Edit an expense and verify both accounts update. Delete only your own test expense; it is a soft deletion with an audit record.
5. Pay outside Poi, record a pending settlement and let the recipient confirm receipt. Pending records must not change balances. Poi never transfers money or treats opening a payment app as proof of payment.
6. Refresh after restoring mobile data or returning to the app. Neither client should claim a failed write was saved. There is no guaranteed offline expense queue. Do not rely on cached balances to make a final settlement without refreshing.
7. Android feedback: Profile → Privacy & notifications → Send trip testing feedback. Web: Feedback tab. Include feature, rating, what happened, expected result and reproduction steps. Never include passwords, OTPs, bank details or private receipts. Check your submitted feedback appears in history.

## Verification performed

Live disposable-account tests cover email signup/password login, private event/photo access before and after an accepted invitation, a real Postgres realtime photo update, likes/comments/unique views, signed image access, feedback persistence/privacy/impersonation rejection, and cleanup. The earlier expense integration script passed invitations, split validation, receipts/privacy, recipient confirmation, activity and soft deletion. Unit tests cover feedback input, sign-in versus signup validation, expense calculations and formatting, and web integer-currency calculations. Android build/lint and hosted-browser QA complement these tests but do not certify physical iPhone/Safari or Android behavior: friends must still test their real devices.

Poster-to-event UI is hidden, not removed from the backend or source. No trip events or dates are invented or seeded into your live data. Both clients point to the permanent `pzjojdkcnufdnrcjmdkb` project. Apply `202610120001_trip_feedback.sql` once, and record dashboard-applied migrations before future CLI migration synchronization.
