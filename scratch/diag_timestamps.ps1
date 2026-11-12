$baseUrl = "http://localhost:8080/api"
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body (@{ username="cro_user"; password="password123" } | ConvertTo-Json)
$headers = @{ "Authorization" = "Bearer $($loginRes.token)"; "X-User-Id" = "cro_user" }

$links = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?collateralId=col-1794213171562" -Method Get -Headers $headers
$links | Format-List id, collateralId, facilityId, loanAccountId, makerDtStamp, checkerDtStamp, relationshipStatus, authStat

