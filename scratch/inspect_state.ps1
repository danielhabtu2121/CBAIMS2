$baseUrl = "http://localhost:8080/api"

$loginBody = @{
    username = "cro_user"
    password = "password123"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$headers = @{
    "Authorization" = "Bearer $($loginRes.token)"
}

$state = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
Write-Host "Total Customers: $($state.customers.Count)"
Write-Host "Total Facilities: $($state.facilities.Count)"
Write-Host "Total Collaterals: $($state.collaterals.Count)"

Write-Host "`n--- Sample Customers ---"
$state.customers | Select-Object -First 5 id, cif, name, branch | Format-Table -AutoSize

Write-Host "`n--- All Facilities ---"
$state.facilities | Select-Object id, loanReference, customerId, lineCode, approvedAmount, outstandingBalance | Format-Table -AutoSize

Write-Host "`n--- Sample Collaterals ---"
$state.collaterals | Select-Object -First 5 id, code, customerId, ownerType, valuationAmount, haircut, status | Format-Table -AutoSize
