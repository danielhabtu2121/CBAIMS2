$baseUrl = "http://localhost:8080/api"
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body (@{ username="cro_user"; password="password123" } | ConvertTo-Json)
$headers = @{ "Authorization" = "Bearer $($loginRes.token)"; "X-User-Id" = "cro_user" }

$linksById = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?collateralId=col-1794213171562" -Method Get -Headers $headers
Write-Host "By ID (col-1794213171562): $($linksById.Count)"
$linksById | Format-Table id, collateralId, facilityId, loanAccountId, relationshipStatus, authStat

$linksByCode = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?collateralId=COL-CS002-478422" -Method Get -Headers $headers
Write-Host "By Code (COL-CS002-478422): $($linksByCode.Count)"
$linksByCode | Format-Table id, collateralId, facilityId, loanAccountId, relationshipStatus, authStat

$allLinks = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links" -Method Get -Headers $headers
$allMatching = $allLinks | Where-Object { $_.collateralId -eq 'col-1794213171562' -or $_.collateralId -eq 'COL-CS002-478422' }
Write-Host "All matching from full list: $($allMatching.Count)"
$allMatching | Format-Table id, collateralId, facilityId, loanAccountId, relationshipStatus, authStat

