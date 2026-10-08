param([switch]$AllSamples)
$ErrorActionPreference = 'Stop'
$posterConfig = @{}
Get-Content -LiteralPath (Join-Path $PSScriptRoot '../supabase.properties') | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $posterConfig[$matches[1].Trim()] = $matches[2].Trim() }
}
$posterApi = $posterConfig['url']
$posterKey = $posterConfig['publishableKey']
$accounts = @()
function Request-PosterApi($Account, $Path, $Body) {
    $headers = @{ apikey = $posterKey }
    if ($Account) { $headers.Authorization = 'Bearer ' + $Account.access_token }
    try { Invoke-RestMethod -Uri "$posterApi/$Path" -Method POST -Headers $headers -ContentType 'application/json' -Body (ConvertTo-Json -InputObject $Body -Depth 12 -Compress) -TimeoutSec 85 }
    catch { throw "Request failed ($Path): $($_.ErrorDetails.Message)" }
}
Add-Type -AssemblyName System.Drawing
$sampleNames = @(
 'codex-clipboard-10ea25a0-2cb1-498a-aa95-52104d374099.png',
 'codex-clipboard-a3d6a002-98e6-43df-adfb-1ba0e97d3387.png',
 'codex-clipboard-ee70854a-fc81-49af-99bb-c92d7f041adc.png',
 'codex-clipboard-e9485ae7-8a97-43e3-82c6-5fd69f085151.png',
 'codex-clipboard-305d307a-f385-4c78-91b8-9aab497b1333.png',
 'codex-clipboard-349be8a5-e616-4811-9bad-a74a4d76f383.png'
)
try {
    # Temporary identities only. Never publish the sample posters as events.
    for ($i = 0; $i -lt $(if ($AllSamples) { 2 } else { 1 }); $i++) {
        $account = Request-PosterApi $null 'auth/v1/signup' @{
            email = "poi.poster.$([guid]::NewGuid().ToString('N'))@example.com"
            password = 'PoiTest!' + [guid]::NewGuid().ToString('N')
            data = @{ display_name = 'Temporary poster verification' }
        }
        if (-not $account.access_token) { throw 'Test signup did not produce a session.' }
        $accounts += $account
    }
    # Deny unauthenticated proxy access and user attempts to reserve quota directly.
    try { $null = Request-PosterApi $null 'functions/v1/read-poster' @{}; throw 'Anonymous request was accepted.' }
    catch { if ($_.Exception.Message -notmatch 'Sign in') { throw } }
    try { $null = Request-PosterApi $accounts[0] 'rest/v1/rpc/reserve_poster_scan' @{ scan_user = $accounts[0].user.id }; throw 'User bypassed quota gate.' }
    catch { if ($_.Exception.Message -notmatch 'permission denied|Could not find') { throw } }
    $indexes = if ($AllSamples) { @(0,1,2,3,4,5) } else { @(2) }
    $results = @()
    foreach ($index in $indexes) {
        $bitmap = [System.Drawing.Image]::FromFile((Join-Path 'C:/Users/mshriyan/AppData/Local/Temp' $sampleNames[$index]))
        $memory = New-Object System.IO.MemoryStream
        try { $bitmap.Save($memory, [System.Drawing.Imaging.ImageFormat]::Jpeg); $encoded = [Convert]::ToBase64String($memory.ToArray()) }
        finally { $bitmap.Dispose(); $memory.Dispose() }
        $account = $accounts[$(if ($AllSamples) { $index % 2 } else { 0 })]
        $draft = Request-PosterApi $account 'functions/v1/read-poster' @{ consent = $true; mimeType = 'image/jpeg'; image = $encoded }
        if ($draft.endTime) { throw 'Reader invented an end time.' }
        if ($index -in @(0,1,4) -and @($draft.dates | Where-Object { $null -ne $_.year }).Count) { throw 'Reader guessed a missing year.' }
        if ($index -eq 1 -and (@($draft.dates).Count -ne 2 -or 30 -in $draft.dates.day)) { throw 'Separated IPL dates incorrectly expanded.' }
        if ($index -eq 2 -and ($draft.dates[0].year -ne 2026 -or $draft.dates[0].month -ne 4 -or $draft.dates[0].day -ne 19)) { throw 'Concert date not read correctly.' }
        if ($index -eq 4 -and (@($draft.dates).Count -ne 10 -or $draft.startTimes -notcontains '17:30' -or $draft.startTimes -notcontains '20:00')) { throw 'Circus dates or shows incorrect.' }
        if ($index -eq 5 -and ($draft.dates[0].year -ne 2022 -or $draft.dateRelationship -ne 'range')) { throw 'Historical range not retained.' }
        $results += @{ image = $index + 1; draft = $draft }
        Write-Output ("PASS image {0}: {1}; dates={2}; times={3}" -f ($index+1), $draft.title, ($draft.dates | ConvertTo-Json -Compress), ($draft.startTimes -join ','))
        if ($AllSamples) { Start-Sleep -Seconds 9 }
    }
    $results | ConvertTo-Json -Depth 12 | Out-File (Join-Path $PSScriptRoot '../artifacts/cloud-poster-results.json') -Encoding utf8
    Write-Output 'PASS: authenticated cloud reading, date checks, no guessed duration, and private quota access.'
} finally {
    foreach ($account in $accounts) {
        try { $null = Request-PosterApi $account 'rest/v1/rpc/delete_poi_account' @{}; Write-Output 'Removed temporary verification identity.' }
        catch { Write-Warning 'Temporary poster verification identity cleanup failed.' }
    }
}
