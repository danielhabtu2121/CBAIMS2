param([string]$ModuleName)
$cases = Get-Content "scratch/uat_all_cases.json" -Raw | ConvertFrom-Json
$filtered = $cases | Where-Object { $_.Module -like "*$ModuleName*" }

Write-Host "Found $($filtered.Count) test cases for '$ModuleName':`n"
foreach ($tc in $filtered) {
    Write-Host "--------------------------------------------------------------------------------"
    Write-Host "ID:       $($tc.TestId)"
    Write-Host "Scenario: $($tc.Scenario)"
    Write-Host "Ref:      $($tc.Ref)"
    Write-Host "Precond:  $($tc.Preconditions -replace '`n', ' ')"
    Write-Host "Steps:    $($tc.Steps -replace '`n', ' -> ')"
    Write-Host "Expected: $($tc.Expected -replace '`n', ' ')"
}

