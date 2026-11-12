$baseUrl = 'http://localhost:8080/api'
$loginBody = @{ username = 'cro_user'; password = 'password123' } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType 'application/json' -Body $loginBody
$token = $loginRes.token
$headers = @{ Authorization = "Bearer $token" }

$payload = @{
    collateral = @{
        code = 'COL-DIAG-1'
        category = 'Immovable Properties'
        type = 'Buildings - Commercial'
        customerId = 'cust-1788161148959'
        branch = 'Addis Ababa Main Branch'
        valuationAmount = 6000000.0
        haircut = 20.0
        currency = 'ETB'
        collateralRank = '1st'
        ownerType = 'Borrower-owned'
        location = 'Bole Road, Addis Ababa'
        locationAddress = 'Bole Road, Addis Ababa'
        locationCity = 'Addis Ababa'
        titleDeedNumber = 'TD-DIAG-001'
    }
    documents = @(
        @{ name = 'Approved Building Plan'; type = 'Approved Building Plan'; fileName = 'plan.pdf'; fileContent = 'abc'; fileSize = 1024 },
        @{ name = 'Valuation Report'; type = 'Valuation Report'; fileName = 'val.pdf'; fileContent = 'abc'; fileSize = 1024 }
    )
    exceptions = @(
        @{
            failedRequirement = 'Title Deed / Property Ownership Certificate'
            documentType = 'Title Deed / Property Ownership Certificate'
            reason = 'Processing'
        }
    )
} | ConvertTo-Json -Depth 10

$preVal = Invoke-RestMethod -Uri "$baseUrl/collateral/pre-validate" -Method Post -ContentType 'application/json' -Headers $headers -Body $payload
Write-Host "Overall Status: $($preVal.overallStatus)"
Write-Host "Can Submit As Exception: $($preVal.canSubmitAsException)"
foreach ($c in $preVal.checks) {
    Write-Host "Check: [$($c.passed)] $($c.category) - $($c.checkName): $($c.message) (RuleId: $($c.ruleId))"
}

