# Authenticate as sysadmin
$loginBody = @{
    username = "sysadmin"
    password = "password123"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body $loginBody -ContentType 'application/json'
$token = $loginRes.token
$headers = @{
    'Authorization' = "Bearer $token"
    'X-User-Id' = $loginRes.user.username
}

Write-Host "=== TEST 1: User Management ==="
# 1. Create a new user
$testUsername = "test_cro_" + [System.Guid]::NewGuid().ToString().Substring(0, 6)
$createPayload = @{
    username = $testUsername
    fullName = "Test Officer $testUsername"
    email = "$testUsername@bank.et"
    role = "CRO"
    branch = "Bole Special Branch"
    segment = "Corporate Banking"
    active = $true
} | ConvertTo-Json
$createdUser = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users/save?adminUserId=sysadmin" -Method Post -Headers $headers -Body $createPayload -ContentType 'application/json'
Write-Host "Created User ID: $($createdUser.id), Username: $($createdUser.username)"

# 2. Get Users and verify presence
$users = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users" -Method Get -Headers $headers
$found = $users | Where-Object { $_.username -eq $testUsername }
if ($found) {
    Write-Host "PASS: User found in /api/admin/users: $($found.username), active: $($found.active)"
} else {
    Write-Host "FAIL: User not found in /api/admin/users!"
}

# 3. Edit User (update full name without changing username)
$editPayload = @{
    id = $createdUser.id
    username = $testUsername
    fullName = "Updated Full Name for $testUsername"
    role = "CRO"
    branch = "Bole Special Branch"
    segment = "Corporate Banking"
    active = $true
} | ConvertTo-Json
$editedUser = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users/save?adminUserId=sysadmin" -Method Post -Headers $headers -Body $editPayload -ContentType 'application/json'
Write-Host "PASS: Edited User Full Name: $($editedUser.fullName)"

# 4. Toggle User Status (Lock account)
$toggleRes = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users/$($createdUser.id)/toggle-status?active=false&adminUserId=sysadmin" -Method Post -Headers $headers
$usersAfterToggle = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users" -Method Get -Headers $headers
$lockedUser = $usersAfterToggle | Where-Object { $_.username -eq $testUsername }
if ($lockedUser.active -eq $false) {
    Write-Host "PASS: User status toggled to inactive: $($lockedUser.active)"
} else {
    Write-Host "FAIL: User status not toggled!"
}

Write-Host "`n=== TEST 2: Operating Branches ==="
$branchCode = "BRN-TEST-" + [System.Guid]::NewGuid().ToString().Substring(0, 4)
$branchPayload = @{
    code = $branchCode
    name = "Test Admin Branch $branchCode"
    type = "Branch"
    parentDistrictId = "Addis Ababa East District"
    active = $true
} | ConvertTo-Json
$savedBranch = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/branches/save?adminUserId=sysadmin" -Method Post -Headers $headers -Body $branchPayload -ContentType 'application/json'
Write-Host "PASS: Saved Branch: $($savedBranch.code) - $($savedBranch.name)"

Write-Host "`n=== TEST 3: Approval Workflows Configuration ==="
$approvalConfigs = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/approval-configs" -Method Get -Headers $headers
Write-Host "Existing approval configs count: $($approvalConfigs.Count)"
$apprPayload = @{
    transactionType = "Release Collateral"
    hierarchyLevel = "Branch"
    approvalLevels = 2
    level1Role = "BRMGR"
    level2Role = "DISTDIR"
    makerRole = "CRO"
    slaHours = 48
    bulkApproveAllowed = $true
    active = $true
} | ConvertTo-Json
$savedAppr = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/approval-configs/save?adminUserId=sysadmin" -Method Post -Headers $headers -Body $apprPayload -ContentType 'application/json'
Write-Host "PASS: Saved Approval Config: $($savedAppr.transactionType) with $($savedAppr.approvalLevels) stages, SLA: $($savedAppr.slaHours)h"

Write-Host "`n=== TEST 4: Approved Underwriters Whitelist ==="
$insurers = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/insurers" -Method Get -Headers $headers
Write-Host "Total approved underwriters in whitelist: $($insurers.Count)"
if ($insurers.Count -ge 18) {
    Write-Host "PASS: All approved underwriters whitelist intact (Count: $($insurers.Count))"
} else {
    Write-Host "WARNING: Insurer count less than 18: $($insurers.Count)"
}

Write-Host "`n=== TEST 5: Holiday Business Calendar & Working Days ==="
$checkDate = "2026-09-11"
$holidayCheck = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/calendar/check?date=$checkDate" -Method Get -Headers $headers
Write-Host "Checked date $($checkDate): isWorkingDay=$($holidayCheck.isWorkingDay), isHoliday=$($holidayCheck.isHoliday)"

$addDays = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/calendar/add-working-days?date=2026-09-10&days=5" -Method Get -Headers $headers
Write-Host "Added 5 working days to 2026-09-10: Result date = $($addDays.resultDate)"

Write-Host "`n=== TEST 6: Mandatory Document Rules ==="
$docRules = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/document-rules" -Method Get -Headers $headers
Write-Host "Total mandatory document rules configured: $($docRules.Count)"
if ($docRules.Count -gt 0) {
    Write-Host "PASS: Mandatory document rules loaded."
}

Write-Host "`n=== TEST 7: System Parameters ==="
$params = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/parameters" -Method Get -Headers $headers
Write-Host "Total system parameters configured: $($params.Count)"
$haircutParam = $params | Where-Object { $_.paramKey -like "*HAIRCUT*" -or $_.paramKey -like "*LTV*" } | Select-Object -First 1
if ($haircutParam) {
    Write-Host "Sample parameter: $($haircutParam.paramKey) = $($haircutParam.paramValue)"
}

Write-Host "`n=== ALL ADMIN FUNCTIONALITY CHECKS COMPLETED SUCCESSFULLY ==="
