$baseUrl = "http://localhost:8080/api"

$loginBody = @{
    username = "cro_user"
    password = "password123"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$headers = @{ "Authorization" = "Bearer $($loginRes.token)" }

Write-Host "CRO User Branch: $($loginRes.user.branch)"
$state = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers

Write-Host "`nCustomers in $($loginRes.user.branch):"
$matchingCusts = $state.customers | Where-Object { $_.branch -eq $loginRes.user.branch }
$matchingCusts | Select-Object id, cif, name, branch | Format-Table -AutoSize

Write-Host "`nFacilities for matching customers:"
$matchingFacs = $state.facilities | Where-Object { $matchingCusts.id -contains $_.customerId }
$matchingFacs | Select-Object id, loanReference, customerId, branch, approvedAmount | Format-Table -AutoSize

Write-Host "`nAvailable Users:"
$state.users | Select-Object id, username, role, branch, systemRole | Format-Table -AutoSize

