$transcript = "C:\Users\danie_mdh4j2\.gemini\antigravity\brain\64991802-2aec-4b02-a304-a86f7dfba279\.system_generated\logs\transcript.jsonl"
$fullTranscript = "C:\Users\danie_mdh4j2\.gemini\antigravity\brain\64991802-2aec-4b02-a304-a86f7dfba279\.system_generated\logs\transcript_full.jsonl"

Get-Content $transcript | ForEach-Object {
    $line = $_
    if ($line -match '"source":"USER_EXPLICIT"') {
        $obj = $line | ConvertFrom-Json
        Write-Host "Found step_index=$($obj.step_index) preview=$($obj.content.Substring(0, [Math]::Min(120, $obj.content.Length)))"
    }
}

