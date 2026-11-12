$baseUrl = "http://localhost:8080/api"
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body (@{ username = "cro_user"; password = "password123" } | ConvertTo-Json)
$headers = @{
    "Authorization" = "Bearer $($loginRes.token)"
    "X-User-Id" = "cro_user"
}
$state = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$col = $state.collaterals | Select-Object -Last 1

$payload = @{
    policy = @{
        policyNumber = "POL-TEST-DEBUG-" + (Get-Random -Minimum 10000 -Maximum 99999)
        insurerName = "Nyala Insurance S.C."
        coverageType = "Fire and Lightning"
        insuredAmount = 50000000.0
        premium = 120000.0
        effectiveDate = "2026-11-09"
        expiryDate = "2027-11-10"
        collateralId = $col.id
        customerId = $col.customerId
    }
    documents = @(
        @{
            name = "Insurance Policy Schedule.pdf"
            type = "Insurance Policy Schedule"
            verificationStatus = "Verified"
            fileContent = "JVBERi0xLjQKJcTl8uXrp/Og0MTGCjQgMCBvYmoKPDwKL1R5cGUgL1BhZ2VzCi9Db3VudCAx"
        }
    )
} | ConvertTo-Json -Depth 5

Write-Host "Calling pre-validate..."
$preVal = Invoke-RestMethod -Uri "$baseUrl/policies/pre-validate" -Method Post -ContentType "application/json" -Headers $headers -Body $payload
Write-Host "Pre-val: Valid=$($preVal.valid)"
$preVal | ConvertTo-Json -Depth 3 | Write-Host

Write-Host "Calling register..."
try {
    $res = Invoke-RestMethod -Uri "$baseUrl/policies/register?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $payload
    Write-Host "Success: $($res | ConvertTo-Json)"
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    if ($stream) {
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "Response Body: $($reader.ReadToEnd())"
    } else {
        Write-Host "Exception: $($_.Exception.Message)"
    }
}

