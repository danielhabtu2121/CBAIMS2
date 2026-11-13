$json = Get-Content 'scratch/uat_all_cases.json' -Raw | ConvertFrom-Json
Write-Host "Total UAT Test Cases in Excel Pack: $($json.Count)"
$groups = $json | Group-Object Module
foreach ($g in $groups) {
    Write-Host ("- {0}: {1} cases" -f $g.Name, $g.Count)
}

