$filePath = "C:\Users\danie_mdh4j2\.gemini\antigravity\brain\64991802-2aec-4b02-a304-a86f7dfba279\.system_generated\logs\transcript_full.jsonl"
$line = Get-Content -Path $filePath | Where-Object { $_ -match '"step_index":7094' }
if ($line) {
    $json = $line | ConvertFrom-Json
    $content = $json.content
    $start = $content.IndexOf("TEST AREA 4")
    if ($start -ge 0) {
        $sub = $content.Substring($start)
        $end = $sub.IndexOf("# 8. REGRESSION")
        if ($end -gt 0) {
            $sub = $sub.Substring(0, $end)
        }
        Set-Content -Path "scratch/test_area4_requirements.txt" -Value $sub -Encoding UTF8
        Write-Output "Extracted $($sub.Length) chars to scratch/test_area4_requirements.txt"
    }
}

