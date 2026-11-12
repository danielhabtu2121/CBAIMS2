$filePath = 'C:\Users\danie_mdh4j2\.gemini\antigravity\brain\64991802-2aec-4b02-a304-a86f7dfba279\.system_generated\logs\transcript_full.jsonl'
$matches = Select-String -Path $filePath -Pattern 'CS-001'
Write-Host "Found matches: " $matches.Count
foreach ($m in $matches) {
    Write-Host "Line number: " $m.LineNumber
    $raw = $m.Line
    $idx = $raw.IndexOf('CS-001')
    if ($idx -ge 0) {
        $start = [Math]::Max(0, $idx - 500)
        $len = [Math]::Min(25000, $raw.Length - $start)
        [System.IO.File]::WriteAllText('scratch/master_area3_and_4.txt', $raw.Substring($start, $len), [System.Text.Encoding]::UTF8)
        Write-Host "Saved snippet from line $($m.LineNumber) around index $idx"
        break
    }
}
