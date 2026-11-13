$cases = Get-Content "scratch/uat_all_cases.json" -Raw | ConvertFrom-Json

$byModule = $cases | Group-Object Module
foreach ($m in $byModule) {
    Write-Host "`n======================================================="
    Write-Host "MODULE: $($m.Name) ($($m.Count) test cases)"
    Write-Host "======================================================="
    foreach ($tc in $m.Group) {
        Write-Host "$($tc.TestId) | $($tc.Scenario)"
    }
}

