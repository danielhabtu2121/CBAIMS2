$baseUrl = "http://localhost:8080/api"
$loginBody = @{ username = "cro_user"; password = "password123" } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$headers = @{ "Authorization" = "Bearer $($loginRes.token)"; "X-User-Id" = "cro_user" }

$links = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links" -Method Get -Headers $headers
Write-Host "Total links: $($links.Count)"
$latest = $links | Select-Object -Last 3
$latest | Format-Table id, collateralId, facilityId, loanAccountId, allocatedAmount, status

if ($latest.Count -gt 0) {
    $colId = $latest[0].collateralId
    $facId = $latest[0].facilityId
    Write-Host "`nQuerying Collateral Facilities for $($colId):"
    $c1Summary = Invoke-RestMethod -Uri "$baseUrl/collaterals/$colId/facilities" -Method Get -Headers $headers
    $c1Summary | ConvertTo-Json -Depth 3 | Write-Host

    Write-Host "`nQuerying Facility Collaterals for $($facId):"
    $facSummary = Invoke-RestMethod -Uri "$baseUrl/facilities/$facId/collaterals" -Method Get -Headers $headers
    $facSummary | ConvertTo-Json -Depth 3 | Write-Host
}
