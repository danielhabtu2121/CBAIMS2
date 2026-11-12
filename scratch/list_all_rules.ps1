$baseUrl = 'http://localhost:8080/api'
$loginBody = @{ username = 'cro_user'; password = 'password123' } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType 'application/json' -Body $loginBody
$token = $loginRes.token
$headers = @{ Authorization = "Bearer $token" }

$state = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$rules = $state.mandatoryDocRules
Write-Host "Total mandatory document rules: $($rules.Count)"
foreach ($r in $rules) {
    Write-Host "[$($r.id)] $($r.documentType) | Cat: '$($r.collateralCategory)' | Subtype: '$($r.collateralType)' | Mand: $($r.mandatory) | ExcAllowed: $($r.exceptionAllowed) | Act: $($r.active)"
}

