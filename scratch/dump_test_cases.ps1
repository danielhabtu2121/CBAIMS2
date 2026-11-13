Add-Type -AssemblyName System.IO.Compression.FileSystem

$zipPath = "C:\Users\danie_mdh4j2\.gemini\antigravity\brain\64991802-2aec-4b02-a304-a86f7dfba279\.user_uploaded\media_1794480337697_c7b9610f.xlsx"
$zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)

# 1. Read shared strings
$sstEntry = $zip.GetEntry("xl/sharedStrings.xml")
$sharedStrings = @()
if ($sstEntry) {
    $s = $sstEntry.Open()
    $r = New-Object System.IO.StreamReader($s)
    $xml = [xml]$r.ReadToEnd()
    $r.Close()
    $s.Close()
    
    foreach ($si in $xml.sst.si) {
        if ($si.t) {
            $sharedStrings += $si.t
        } elseif ($si.r) {
            $t = ($si.r | ForEach-Object { $_.t }) -join ""
            $sharedStrings += $t
        } else {
            $sharedStrings += ""
        }
    }
}
Write-Host "Total shared strings: $($sharedStrings.Count)"

# 2. Find Test Cases sheet
$wbEntry = $zip.GetEntry("xl/workbook.xml")
$s = $wbEntry.Open()
$r = New-Object System.IO.StreamReader($s)
$wbXml = [xml]$r.ReadToEnd()
$r.Close()
$s.Close()

$testCaseSheetId = ($wbXml.workbook.sheets.sheet | Where-Object { $_.name -eq "Test Cases" }).sheetId
Write-Host "Test cases sheet id: $testCaseSheetId"

$sheetEntry = $zip.GetEntry("xl/worksheets/sheet$testCaseSheetId.xml")
if (-not $sheetEntry) {
    $sheetEntry = $zip.GetEntry("xl/worksheets/sheet2.xml")
}

$s = $sheetEntry.Open()
$r = New-Object System.IO.StreamReader($s)
$sheetXml = [xml]$r.ReadToEnd()
$r.Close()
$s.Close()
$zip.Dispose()

$rows = $sheetXml.worksheet.sheetData.row
Write-Host "Total rows in Test Cases: $($rows.Count)"

# Print first 10 rows
for ($i = 0; $i -lt [Math]::Min(15, $rows.Count); $i++) {
    $row = $rows[$i]
    $cells = @()
    foreach ($c in $row.c) {
        $val = ""
        if ($c.v) {
            $val = $c.v
            if ($c.t -eq "s") {
                $idx = [int]$val
                $val = $sharedStrings[$idx]
            }
        }
        $cells += "$($c.r): $val"
    }
    Write-Host "Row $($row.r): $($cells -join ' | ')"
}

