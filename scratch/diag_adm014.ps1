$baseUrl = "http://localhost:8080/api"

# Login CRO
$croLogin = @{ username = "cro_user"; password = "password123" } | ConvertTo-Json
$croRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $croLogin
$croHeaders = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $($croRes.token)"
    "X-User-Id" = "cro_user"
    "X-User-Role" = "CRO"
}

$testRuleId = "cov-rule-test-" + (Get-Random -Minimum 1000 -Maximum 9999)

$rulePayload = @{
    id = $testRuleId
    collateralType = "Vehicles"
    businessSegment = "Corporate Banking"
    requiredCoveragePercentage = 95.0
    haircut = 0.20
    policyType = "Comprehensive Motor"
    effectiveFrom = (Get-Date).AddDays(-10).ToString("yyyy-MM-dd")
    effectiveTo = (Get-Date).AddYears(1).ToString("yyyy-MM-dd")
    description = "Test 95% coverage rule for Corporate vehicles"
} | ConvertTo-Json

try {
    $res = Invoke-RestMethod -Uri "$baseUrl/insurance-adequacy/rules" -Method Post -Headers $croHeaders -Body $rulePayload
    Write-Host "Save Rule SUCCESS:" ($res | ConvertTo-Json -Compress)
} catch {
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host "Save Rule ERROR:" $reader.ReadToEnd()
}

