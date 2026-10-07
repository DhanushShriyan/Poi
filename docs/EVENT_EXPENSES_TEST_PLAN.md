# Event expenses — friend testing

Update Poi through Profile → App updates → Check for updates. Sign in on two phones with separate accounts; send and accept a friend request.

1. Open the same event → Event expenses. Start your private circle and invite your friend from People.
2. On the second phone, open that event and accept the expense invitation. A third, uninvited account must not see the circle's expenses.
3. Add a ₹100 tickets expense paid by you, shared equally between both people. Balances should show your friend owes ₹50.
4. Try exact amounts, percentages, and shares. Incorrect totals must prevent saving; valid totals must preserve every paise.
5. Select multiple payers. The entered total is contributed equally among the selected payers; choose who shares the cost separately.
6. Add a receipt image (JPEG/PNG/WebP, up to 6 MB), note, and comment. Check the other phone receives changes. Search by title/category/payer and inspect category totals.
7. Edit an expense, then delete it. Check the activity history and recalculated balances on both phones.
8. In Balances, record a ₹50 payment from the friend to you. It must remain pending until you, the recipient, confirm it. The sender must not be able to confirm it.
9. Add a UPI ID and try opening your installed UPI app. Cancel the real payment for this test. Poi does not transfer money or automatically verify bank payments.
10. Share a reminder and export the statement. Create a separate circle for the same event to check that groups stay independent.
11. Check Classic, Pulse, Retro, light/dark mode, sign-out/sign-in, and temporary loss of internet. Capture screenshots of any errors without passwords or account tokens.

Report: Poi version, Android version, phone model, steps, expected result, actual result, and screenshot. Use test amounts; avoid real payments until your team approves the flow.

Automated verification: `scripts/test-event-expenses.ps1` exercises the live backend with temporary accounts and cleans up its test event/accounts. Unit tests cover allocation, balances, input validation, and formatting. Receipt rendering, UPI handoff, and real-device UI still need phone testing.
