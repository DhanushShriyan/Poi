param([string]$ConfigPath = (Join-Path $PSScriptRoot '..\supabase.properties'))
$ErrorActionPreference = 'Stop'
$expenseConfig = @{}
Get-Content -LiteralPath $ConfigPath | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $expenseConfig[$matches[1].Trim()] = $matches[2].Trim() }
}
$expenseApi = $expenseConfig['url']
$expenseKey = $expenseConfig['publishableKey']
$testUsers = @()
function Invoke-ExpenseApi($User, $Method, $Path, $Body = $null) {
    $headers = @{ apikey = $expenseKey; Prefer = 'return=representation' }
    if ($User) { $headers['Authorization'] = 'Bearer ' + $User.access_token }
    $arguments = @{ Uri = "$expenseApi/$Path"; Method = $Method; Headers = $headers; ContentType = 'application/json'; TimeoutSec = 45 }
    if ($null -ne $Body) { $arguments['Body'] = ConvertTo-Json -InputObject $Body -Depth 20 -Compress }
    try { $response = Invoke-RestMethod @arguments; $response }
    catch {
        $details = $_.ErrorDetails.Message
        if (-not $details -and $_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $details = $reader.ReadToEnd()
            $reader.Dispose()
        }
        throw "$Method $Path failed: $details"
    }
}
function Assert-Expense($Condition, $Message) { if (-not $Condition) { throw $Message } }
try {
    $suffix = [guid]::NewGuid().ToString('N').Substring(0,12)
    foreach ($person in @('alice', 'bob', 'outsider')) {
        $account = Invoke-ExpenseApi $null POST 'auth/v1/signup' @{
            email = "poi.expense.$person.$suffix@example.com"
            password = 'PoiTest!' + [guid]::NewGuid().ToString('N')
            data = @{ display_name = "Expense test $person" }
        }
        Assert-Expense ($account.access_token) 'Test signup did not produce a session.'
        $testUsers += $account
    }
    $alice, $bob, $outsider = $testUsers
    $now = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $event = @(Invoke-ExpenseApi $alice POST 'rest/v1/events' @{
        title = "Expense verification $suffix"; summary = 'Temporary automated verification'
        description = 'Temporary expense flow verification, automatically removed after the test.'
        category = 'community'; starts_at_millis = $now + 86400000; ends_at_millis = $now + 90000000
        venue = 'Test venue'; address = 'Test location'; organizer_name = 'Expense test'
        visibility = 'public'; theme_key = 'festival'; check_in_radius_meters = 500
    })[0]
    $group = @(Invoke-ExpenseApi $alice POST 'rest/v1/event_expense_groups' @{
        event_id = $event.id; created_by = $alice.user.id; currency = 'INR'
    })[0]
    $creator = @(Invoke-ExpenseApi $alice GET "rest/v1/event_expense_members?group_id=eq.$($group.id)")
    Assert-Expense ($creator.Count -eq 1 -and $creator[0].status -eq 'active') 'Group creator membership was not initialized.'
    $friend = @(Invoke-ExpenseApi $alice POST 'rest/v1/friendships' @{
        requester_id = $alice.user.id; addressee_id = $bob.user.id
    })[0]
    $null = Invoke-ExpenseApi $bob PATCH "rest/v1/friendships?id=eq.$($friend.id)" @{ status = 'accepted' }
    $null = Invoke-ExpenseApi $alice POST 'rest/v1/event_expense_members' @{
        group_id = $group.id; user_id = $bob.user.id; invited_by = $alice.user.id
    }
    $expenseId = [guid]::NewGuid().ToString()
    $draft = @{
        p_expense_id = $expenseId; p_group_id = $group.id; p_title = 'Tickets'
        p_note = 'Two tickets'; p_category = 'tickets'; p_currency = 'INR'; p_amount_minor = 10000
        p_split_method = 'equal'; p_receipt_path = $null; p_created_at_millis = $now
        p_payments = @(@{ user_id = $alice.user.id; amount_minor = 10000 })
        p_shares = @(@{ user_id = $alice.user.id; amount_minor = 5000 }, @{ user_id = $bob.user.id; amount_minor = 5000 })
    }
    $rejected = $false
    try { $null = Invoke-ExpenseApi $alice POST 'rest/v1/rpc/create_poi_event_expense' $draft } catch { $rejected = $true }
    Assert-Expense $rejected 'An unaccepted participant was included in an expense.'
    $null = Invoke-ExpenseApi $bob PATCH "rest/v1/event_expense_members?group_id=eq.$($group.id)&user_id=eq.$($bob.user.id)" @{ status = 'active' }
    $receiptPath = "$($group.id)/$($alice.user.id)/$expenseId.png"
    $receiptHeaders = @{ apikey = $expenseKey; Authorization = 'Bearer ' + $alice.access_token }
    $receiptBytes = [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aE1cAAAAASUVORK5CYII=')
    $null = Invoke-RestMethod -Uri "$expenseApi/storage/v1/object/event-expense-receipts/$receiptPath" -Method POST -Headers $receiptHeaders -ContentType 'image/png' -Body $receiptBytes -TimeoutSec 45
    $signedReceipt = Invoke-ExpenseApi $bob POST "storage/v1/object/sign/event-expense-receipts/$receiptPath" @{ expiresIn = 60 }
    Assert-Expense ($signedReceipt.signedURL) 'An accepted member could not view a private receipt.'
    $receiptHidden = $false
    try { $null = Invoke-ExpenseApi $outsider POST "storage/v1/object/sign/event-expense-receipts/$receiptPath" @{ expiresIn = 60 } } catch { $receiptHidden = $true }
    Assert-Expense $receiptHidden 'Private receipt was accessible to a nonmember.'
    $draft.p_receipt_path = $receiptPath
    $null = Invoke-ExpenseApi $alice POST 'rest/v1/rpc/create_poi_event_expense' $draft
    $visible = @(Invoke-ExpenseApi $bob GET "rest/v1/event_expenses?id=eq.$expenseId")
    Assert-Expense ($visible.Count -eq 1 -and $visible[0].amount_minor -eq 10000) 'Accepted friend could not read the expense.'
    $hidden = @(Invoke-ExpenseApi $outsider GET "rest/v1/event_expenses?id=eq.$expenseId")
    Assert-Expense ($hidden.Count -eq 0) 'Expense was exposed to a nonmember.'
    $null = Invoke-ExpenseApi $bob POST 'rest/v1/event_expense_comments' @{ expense_id = $expenseId; body = 'Received, thank you.' }
    $settlement = @(Invoke-ExpenseApi $bob POST 'rest/v1/event_expense_settlements' @{
        group_id = $group.id; payer_id = $bob.user.id; payee_id = $alice.user.id
        amount_minor = 5000; currency = 'INR'; note = 'Paid outside Poi'
    })[0]
    $selfConfirm = @(Invoke-ExpenseApi $bob PATCH "rest/v1/event_expense_settlements?id=eq.$($settlement.id)" @{ status = 'confirmed' })
    Assert-Expense ($selfConfirm.Count -eq 0) 'Payer confirmed their own settlement.'
    $confirmed = @(Invoke-ExpenseApi $alice PATCH "rest/v1/event_expense_settlements?id=eq.$($settlement.id)" @{ status = 'confirmed' })[0]
    Assert-Expense ($confirmed.status -eq 'confirmed' -and $confirmed.confirmed_at_millis) 'Recipient confirmation failed.'
    $activities = @(Invoke-ExpenseApi $alice GET "rest/v1/event_expense_activity?group_id=eq.$($group.id)")
    Assert-Expense ($activities.Count -eq 3) 'Activity history did not record expense and payment changes.'
    $null = Invoke-ExpenseApi $alice POST 'rest/v1/rpc/delete_poi_event_expense' @{ p_expense_id = $expenseId }
    $deleted = @(Invoke-ExpenseApi $bob GET "rest/v1/event_expenses?id=eq.$expenseId")[0]
    Assert-Expense ($deleted.deleted_at_millis) 'Expense deletion did not preserve the audit record.'
    Write-Output 'PASS: signup, group creation, accepted-friend invitation, allocation rules, member and receipt privacy, comments, recipient confirmation, activity and soft deletion.'
} finally {
    if ($receiptPath -and $alice) {
        try { $null = Invoke-ExpenseApi $alice DELETE 'storage/v1/object/event-expense-receipts' @{ prefixes = @($receiptPath) } }
        catch { Write-Warning 'Temporary verification receipt cleanup failed.' }
    }
    if ($event -and $alice) {
        try { $null = Invoke-ExpenseApi $alice DELETE "rest/v1/events?id=eq.$($event.id)" }
        catch { Write-Warning 'Temporary verification event cleanup failed.' }
    }
    foreach ($account in $testUsers) {
        try { $null = Invoke-ExpenseApi $account POST 'rest/v1/rpc/delete_poi_account' @{} }
        catch { Write-Warning "Temporary test account cleanup failed for $($account.user.id): $($_.Exception.Message)" }
    }
}
