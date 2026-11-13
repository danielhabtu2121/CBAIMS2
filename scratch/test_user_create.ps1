# Login as sysadmin
$loginBody = @{
    username = "sysadmin_user"
    password = "password123"
} | ConvertTo-Json

$loginRes = $null
try {
    $loginRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body $loginBody -ContentType 'application/json'
    Write-Host "Login as sysadmin_user succeeded."
} catch {
    Write-Host "Failed to login as sysadmin_user: $($_.Exception.Message). Trying sysadmin..."
    $loginBody2 = @{
        username = "sysadmin"
        password = "password123"
    } | ConvertTo-Json
    $loginRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body $loginBody2 -ContentType 'application/json'
    Write-Host "Login as sysadmin succeeded."
}

$token = $loginRes.token
Write-Host "Got token: $($token.Substring(0, 15))..."

$headers = @{
    'Authorization' = "Bearer $token"
    'X-User-Id' = $loginRes.user.username
}

# Create a new user
$newUid = [System.Guid]::NewGuid().ToString().Substring(0, 6)
$newUsername = "test_user_$newUid"
$createBody = @{
    username = $newUsername
    fullName = "Test User $newUid"
    email = "test_$newUid@bank.et"
    phone = "+251911000000"
    role = "CRO"
    branch = "Bole Special Branch"
    segment = "Corporate Banking"
    active = $true
} | ConvertTo-Json

Write-Host "Creating user: $newUsername"
$saveRes = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users/save?adminUserId=$($loginRes.user.username)" -Method Post -Headers $headers -Body $createBody -ContentType 'application/json'
Write-Host "User save result: $($saveRes | ConvertTo-Json)"

# Get users
$allUsers = Invoke-RestMethod -Uri 'http://localhost:8080/api/admin/users' -Method Get -Headers $headers
Write-Host "Total users in GET /api/admin/users: $($allUsers.Count)"
$match = $allUsers | Where-Object { $_.username -eq $newUsername }
if ($match) {
    Write-Host "SUCCESS: User $newUsername is present in GET /api/admin/users!"
    Write-Host ($match | ConvertTo-Json)
} else {
    Write-Host "FAIL: User $newUsername NOT found in GET /api/admin/users!"
}
