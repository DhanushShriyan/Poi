-- A circle has one currency for its entire ledger. Changing it after recording
-- expenses would combine unrelated monetary units in balances.
revoke update (currency) on public.event_expense_groups from authenticated;
