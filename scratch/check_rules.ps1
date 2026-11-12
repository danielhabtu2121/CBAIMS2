$auth = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body '{"username":"cro_user","password":"password123"}'
$headers = @{ Authorization = "Bearer $($auth.token)" }
$resp = Invoke-RestMethod -Uri 'http://localhost:8080/api/collateral/mandatory-document-rules' -Headers $headers
foreach ($x in $resp) {
    if ($x.collateralCategory -match 'Immovable' -or $x.documentType -match 'Title' -or $x.mandatory -eq $true) {
        Write-Host "$($x.id) | $($x.documentType) | $($x.collateralCategory) | Mand: $($x.mandatory) | Act: $($x.active) | Exc: $($x.exceptionAllowed)"
    }
}
