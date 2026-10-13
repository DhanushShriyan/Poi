# Event member shared expenses

Cause: guest links / accepted event invitations populated `event_members`, while expense RLS and both clients required a separate active `event_expense_members` entry. Older versions could also create a separate expense circle per creator.

Prepared migration: `supabase/migrations/202610130001_event_member_shared_expenses.sql`.

- Backfill confirmed event owners/members into existing expense ledgers.
- Synchronize future accepted invitations / redeemed guest links and new groups.
- Keep plain public viewers and Interested/Going-only users outside financial access.
- Track automatically granted membership separately so leaving an event removes that automatic access. Keep previously accepted independent expense invitations valid.
- Reuse the existing oldest ledger for new contributions; preserve additional ledgers and expose them explicitly.
- Do not merge, delete, recompute or update any existing expense, payer, share, receipt or settlement records. Added members start with no historic debt unless already named in a split.

Owner approved the access change and reported running the migration on 13 October 2026. Authenticated API verification confirmed the new `membership_source` column and the shared-ledger RPC's membership guard. No verification events, expenses, accounts or files were created, changed or deleted. Verification did not impersonate the real Goa members; their two-device visibility check is still required.

Verification must be read-only against the live database: count event members, active expense memberships and expenses visible under each affected user's RLS identity. Compare financial-table row counts/content fingerprints before and after application. Use a transaction for identity simulation with `SET LOCAL` and roll it back; do not create/delete test accounts, events, charges or uploaded files while real testing is running.

Do not run `test-event-expenses.ps1`, `test-trip-sync.mjs` or `test-event-links.mjs` on the live project during this request: they create and clean up fixtures, which the owner explicitly prohibited.
