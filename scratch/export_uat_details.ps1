Add-Type -AssemblyName System.IO.Compression.FileSystem

$zipPath = "C:\Users\danie_mdh4j2\.gemini\antigravity\brain\64991802-2aec-4b02-a304-a86f7dfba279\.user_uploaded\media_1794480337697_c7b9610f.xlsx"
$zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)

$sstEntry = $zip.GetEntry("xl/sharedStrings.xml")
$sharedStrings = @()
if ($sstEntry) {
    $s = $sstEntry.Open()
    $r = New-Object System.IO.StreamReader($s)
    $xml = [xml]$r.ReadToEnd()
    $r.Close()
    $s.Close()
    foreach ($si in $xml.sst.si) {
        if ($si.t) { $sharedStrings += $si.t }
        elseif ($si.r) { $sharedStrings += (($si.r | ForEach-Object { $_.t }) -join "") }
        else { $sharedStrings += "" }
    }
}

$sheetEntry = $zip.GetEntry("xl/worksheets/sheet2.xml")
$s = $sheetEntry.Open()
$r = New-Object System.IO.StreamReader($s)
$sheetXml = [xml]$r.ReadToEnd()
$r.Close()
$s.Close()
$zip.Dispose()

$rows = $sheetXml.worksheet.sheetData.row
$testCases = @()

foreach ($row in $rows) {
    $rowNum = [int]$row.r
    if ($rowNum -lt 5) { continue }
    
    $cells = @{}
    foreach ($c in $row.c) {
        $col = $c.r -replace '\d+',''
        $val = ""
        if ($c.v) {
            $val = $c.v
            if ($c.t -eq "s") {
                $idx = [int]$val
                $val = $sharedStrings[$idx]
            }
        }
        $cells[$col] = $val
    }
    
    $tcId = $cells['A']
    if ($tcId) {
        $testCases += [PSCustomObject]@{
            RowNum = $rowNum
            TestId = $tcId
            Module = $cells['B']
            Ref = $cells['C']
            Scenario = $cells['D']
            Preconditions = $cells['E']
            Steps = $cells['F']
            Expected = $cells['G']
            Actual = $cells['H']
            Status = $cells['I']
            DefectId = $cells['J']
            Tester = $cells['K']
            TestDate = $cells['L']
            Notes = $cells['M']
        }
    }
}

$testCases | ConvertTo-Json -Depth 5 | Out-File "scratch/uat_all_cases.json" -Encoding utf8
Write-Host "Exported $($testCases.Count) test cases to scratch/uat_all_cases.json"

