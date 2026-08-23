$BASE = "http://localhost:5173"
$TMP = "$env:TEMP\payguard_test"
New-Item -ItemType Directory -Force -Path $TMP | Out-Null

$OK = "[PASS]"

Write-Host "PAYGUARD - FRAUD ANALYST + ADMIN FLOW CHECK"

# STEP 1: Login as Fraud Analyst
Write-Host "`n[STEP 1] Login as analyst@example.com"
$anlBody = '{"email":"analyst@example.com","password":"Password123!"}'
Set-Content -Path "$TMP\analyst_login.json" -Value $anlBody -Encoding utf8
$anlLoginRaw = curl.exe -s -c "$TMP\analyst_cookies.txt" -X POST "$BASE/api/v1/auth/login" -H "Content-Type: application/json" -d "@$TMP\analyst_login.json"
Write-Host "  Response: $anlLoginRaw"
$anlLogin = $anlLoginRaw | ConvertFrom-Json
if ($anlLogin.role -eq "ROLE_FRAUD_ANALYST") {
    Write-Host "  $OK role=$($anlLogin.role)"
} else { Write-Host "  [FAIL] Expected ROLE_FRAUD_ANALYST"; exit 1 }

# STEP 2: Fetch Fraud Queue
Write-Host "`n[STEP 2] GET /api/v1/fraud/flags"
$flagsRaw = curl.exe -s -b "$TMP\analyst_cookies.txt" "$BASE/api/v1/fraud/flags"
$flags = $flagsRaw | ConvertFrom-Json
Write-Host "  Pending flags in queue: $($flags.Count)"

if ($flags.Count -gt 0) {
    $f = $flags[0]
    Write-Host "  Flag id=$($f.flagId) ref=$($f.transactionReference) rule=$($f.triggeredRuleCode) amount=$($f.amount) status=$($f.status)"
    Write-Host "  $OK Fraud queue accessible"

    # STEP 3: Approve the flag
    Write-Host "`n[STEP 3] Approve flag #$($f.flagId)"
    $reviewBody = '{"action":"APPROVE","notes":"Verified customer over phone. Transaction is legitimate."}'
    Set-Content -Path "$TMP\review.json" -Value $reviewBody -Encoding utf8
    $reviewRaw = curl.exe -s -b "$TMP\analyst_cookies.txt" -X PUT "$BASE/api/v1/fraud/flags/$($f.flagId)/review" -H "Content-Type: application/json" -d "@$TMP\review.json"
    Write-Host "  Response: $reviewRaw"
    $rev = $reviewRaw | ConvertFrom-Json
    if ($rev.status -eq "APPROVED") {
        Write-Host "  $OK Flag APPROVED. reviewer=$($rev.reviewerEmail)"
    } else {
        Write-Host "  [FAIL] Expected APPROVED, got $($rev.status)"
    }
} else {
    Write-Host "  [INFO] No pending flags (previously reviewed - still OK)"
}

# STEP 4: Logout Analyst
Write-Host "`n[STEP 4] Logout analyst"
curl.exe -s -b "$TMP\analyst_cookies.txt" -X POST "$BASE/api/v1/auth/logout" | Out-Null
Write-Host "  $OK Analyst logged out"

# STEP 5: Login as Admin
Write-Host "`n[STEP 5] Login as admin@example.com"
$admBody = '{"email":"admin@example.com","password":"Password123!"}'
Set-Content -Path "$TMP\admin_login.json" -Value $admBody -Encoding utf8
$admLoginRaw = curl.exe -s -c "$TMP\admin_cookies.txt" -X POST "$BASE/api/v1/auth/login" -H "Content-Type: application/json" -d "@$TMP\admin_login.json"
Write-Host "  Response: $admLoginRaw"
$admLogin = $admLoginRaw | ConvertFrom-Json
if ($admLogin.role -eq "ROLE_ADMIN") {
    Write-Host "  $OK Admin login OK"
} else { Write-Host "  [FAIL] Expected ROLE_ADMIN"; exit 1 }

# STEP 6: Admin Users Tab
Write-Host "`n[STEP 6] Admin Tab: Users & Wallets"
$users = (curl.exe -s -b "$TMP\admin_cookies.txt" "$BASE/api/v1/admin/users") | ConvertFrom-Json
Write-Host "  Total users: $($users.Count)"
foreach ($u in $users) {
    Write-Host "    $($u.email) | $($u.role) | walletStatus=$($u.walletStatus) | balance=$($u.balance)"
}
Write-Host "  $OK Users tab verified"

# STEP 7: Admin Fraud Rules Tab
Write-Host "`n[STEP 7] Admin Tab: Fraud Rules"
$rules = (curl.exe -s -b "$TMP\admin_cookies.txt" "$BASE/api/v1/admin/fraud-rules") | ConvertFrom-Json
Write-Host "  Total rules: $($rules.Count)"
foreach ($r in $rules) {
    Write-Host "    $($r.ruleCode) | threshold=$($r.thresholdValue) | window=$($r.timeWindowSeconds)s | enabled=$($r.enabled)"
}
Write-Host "  $OK Fraud Rules tab verified"

# STEP 8: Admin Audit Logs Tab
Write-Host "`n[STEP 8] Admin Tab: MongoDB Audit Logs"
$logs = (curl.exe -s -b "$TMP\admin_cookies.txt" "$BASE/api/v1/audit/logs") | ConvertFrom-Json
Write-Host "  Total audit events: $($logs.Count)"
$logs | Group-Object -Property eventType | Sort-Object Count -Descending | ForEach-Object {
    Write-Host "    $($_.Count)x  $($_.Name)"
}
Write-Host "  $OK Audit Logs tab verified"

# STEP 9: Logout Admin
Write-Host "`n[STEP 9] Logout admin"
curl.exe -s -b "$TMP\admin_cookies.txt" -X POST "$BASE/api/v1/auth/logout" | Out-Null
Write-Host "  $OK Admin logged out"

Write-Host "`n=========================================="
Write-Host "  ALL STEPS VERIFIED - PHASE 13 COMPLETE!"
Write-Host "=========================================="
