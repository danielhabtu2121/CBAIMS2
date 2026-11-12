$baseUrl = "http://localhost:8080/api"
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body (@{ username="brmgr_user"; password="password123" } | ConvertTo-Json)
$headers = @{ "Authorization" = "Bearer $($loginRes.token)"; "X-User-Id" = "brmgr_user" }

# Get active links for col-1794213580851
$links = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?collateralId=col-1794213580851" -Method Get -Headers $headers
$linkB1 = $links | Where-Object { $_.facilityId -eq 'FAC-TEST-002' -or $_.loanAccountId -eq 'loan-test-fac-2' } | Select-Object -First 1

Write-Host "Target Link to end: $($linkB1.id)"
try {
    $res = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/$($linkB1.id)/end?reason=Facility+repaid&userId=brmgr_user" -Method Post -Headers $headers
    Write-Host "Success: $($res | ConvertTo-Json)"
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    Write-Host "Error Body: $($reader.ReadToEnd())"
}

