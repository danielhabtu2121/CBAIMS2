$authBody = @{ username = "sysadmin"; password = "password123" } | ConvertTo-Json
$authResp = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $authBody -ContentType "application/json"
$h = @{ "Authorization" = "Bearer $($authResp.token)"; "Content-Type" = "application/json" }

$data = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/data?reportType=coverage" -Method Get -Headers $h
Write-Host "Total records: $($data.Count)"
$branches = $data | Group-Object Branch | Select-Object Name, Count
$branches | Format-Table -AutoSize

if ($branches.Count -gt 0) {
    $firstBranch = $branches[0].Name
    Write-Host "Testing branch filter: $firstBranch"
    $filtered = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/data?reportType=coverage&branch=$([System.Web.HttpUtility]::UrlEncode($firstBranch))" -Method Get -Headers $h
    Write-Host "Filtered records count: $($filtered.Count)"
    Write-Host "Is non-null: $([bool]($filtered -ne $null))"
}

