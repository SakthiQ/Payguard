Write-Host "=== 1. Login via Vite Proxy (port 5173) ===" -ForegroundColor Cyan
$loginRes = curl.exe -s -c cookies.txt -X POST http://localhost:5173/api/v1/auth/login -H "Content-Type: application/json" -d "@login.json"
Write-Host $loginRes

Write-Host "`n=== 2. My Wallet ===" -ForegroundColor Cyan
$walletRes = curl.exe -s -b cookies.txt http://localhost:5173/api/v1/wallets/me
Write-Host $walletRes

Write-Host "`n=== 3. Transaction History ===" -ForegroundColor Cyan
$txRes = curl.exe -s -b cookies.txt http://localhost:5173/api/v1/transactions
Write-Host $txRes

Write-Host "`n=== 4. RBAC Guard: Customer accessing fraud queue (should be 403) ===" -ForegroundColor Yellow
$rbacRes = curl.exe -s -b cookies.txt http://localhost:5173/api/v1/fraud/flags
Write-Host $rbacRes

Write-Host "`n=== 5. Logout ===" -ForegroundColor Cyan
$logoutRes = curl.exe -s -b cookies.txt -c cookies.txt -X POST http://localhost:5173/api/v1/auth/logout
Write-Host $logoutRes

Write-Host "`n=== 6. Login as Admin ===" -ForegroundColor Cyan
curl.exe -s -X POST -c admin_cookies.txt http://localhost:5173/api/v1/auth/login -H "Content-Type: application/json" -d "@admin_login.json" | Write-Host

Write-Host "`n=== 7. Admin: All Users ===" -ForegroundColor Cyan
curl.exe -s -b admin_cookies.txt http://localhost:5173/api/v1/admin/users | Write-Host

Write-Host "`n=== 8. Admin: Fraud Rules ===" -ForegroundColor Cyan
curl.exe -s -b admin_cookies.txt http://localhost:5173/api/v1/admin/fraud-rules | Write-Host

Write-Host "`n=== 9. Admin: MongoDB Audit Logs (count) ===" -ForegroundColor Cyan
$logs = curl.exe -s -b admin_cookies.txt http://localhost:5173/api/v1/audit/logs | ConvertFrom-Json
Write-Host "Total audit log events: $($logs.Count)"

Write-Host "`n=== ALL SMOKE TESTS COMPLETE ===" -ForegroundColor Green
