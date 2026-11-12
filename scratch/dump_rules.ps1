$baseUrl = 'http://localhost:8080/api'
$loginBody = @{ username = 'cro_user'; password = 'password123' } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType 'application/json' -Body $loginBody
$token = $loginRes.token
$headers = @{ Authorization = "Bearer $token" }

$state = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$targetRules = $state.mandatoryDocRules | Where-Object { $_.id -in @("doc-r-title-deed", "doc-r1", "doc-r2", "doc-r-building-plan", "doc-r-valuation-report") }
foreach ($r in $targetRules) {
    Write-Host "=== Rule $($r.id) ==="
    $r | Format-List *
}

