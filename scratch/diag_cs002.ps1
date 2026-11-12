$baseUrl = "http://localhost:8080/api"
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body (@{ username="cro_user"; password="password123" } | ConvertTo-Json)
$headers = @{ "Authorization" = "Bearer $($loginRes.token)"; "X-User-Id" = "cro_user" }

$col = Invoke-RestMethod -Uri "$baseUrl/collateral/col-1794212141489" -Method Get -Headers $headers
Write-Host "Collateral: $($col.code), ID: $($col.id), Status: $($col.status), OwningSegment: $($col.owningSegment)"

$links = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?collateralId=col-1794212141489" -Method Get -Headers $headers
Write-Host "Found $($links.Count) links:"
$links | Format-Table id, collateralId, facilityId, loanAccountId, status, authStat, relationshipType, thirdParty

$allPending = Invoke-RestMethod -Uri "$baseUrl/workflow/tasks?status=Pending" -Method Get -Headers $headers
Write-Host "Found $($allPending.Count) pending tasks in workflow:"
$allPending | Format-Table id, entityType, entityId, taskType, status, candidateRole, makerId

