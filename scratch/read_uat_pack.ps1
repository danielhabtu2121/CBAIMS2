Add-Type -AssemblyName System.IO.Compression.FileSystem

$zipPath = "C:\Users\danie_mdh4j2\.gemini\antigravity\brain\64991802-2aec-4b02-a304-a86f7dfba279\.user_uploaded\media_1794480337697_c7b9610f.xlsx"
$zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)

# Read workbook.xml to get sheet names
$wbEntry = $zip.GetEntry("xl/workbook.xml")
$stream = $wbEntry.Open()
$reader = New-Object System.IO.StreamReader($stream)
$wbXml = [xml]$reader.ReadToEnd()
$reader.Close()
$stream.Close()

Write-Host "Sheets found:"
foreach ($sheet in $wbXml.workbook.sheets.sheet) {
    Write-Host "- $($sheet.name) (Id: $($sheet.sheetId), r:id: $($sheet.id))"
}

$zip.Dispose()

