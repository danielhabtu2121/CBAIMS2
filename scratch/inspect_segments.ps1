$baseUrl = "http://localhost:8080/api"
$loginBody = @{ username = "cro_user"; password = "password123" } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$headers = @{ "Authorization" = "Bearer $($loginRes.token)"; "X-User-Id" = "cro_user" }
$state = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers

Write-Host "--- Business Segments ---"
$state.segments | Format-Table id, name, code, active

Write-Host "--- Target Customers ---"
$targetCusts = $state.customers | Where-Object { $_.id -in @("cust-test-borrower-1", "cust-test-borrower-2", "cust-test-borrower-3") }
$targetCusts | Format-Table id, cif, name, branch, segment

Write-Host "--- Target Facilities ---"
$targetFacs = $state.facilities | Where-Object { $_.customerId -in @("cust-test-borrower-1", "cust-test-borrower-2", "cust-test-borrower-3") }
$targetFacs | Format-Table id, loanReference, customerId, lineCode, segment, approvedAmount, outstandingBalance

