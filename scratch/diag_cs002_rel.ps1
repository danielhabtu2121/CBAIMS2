$baseUrl = "http://localhost:8080/api"
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body (@{ username="cro_user"; password="password123" } | ConvertTo-Json)
$headers = @{ "Authorization" = "Bearer $($loginRes.token)"; "X-User-Id" = "cro_user" }

$links = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?collateralId=col-1794212141489" -Method Get -Headers $headers
$links | Format-Table id, collateralId, facilityId, relationshipStatus, authStat, linkageType, relationshipContext

