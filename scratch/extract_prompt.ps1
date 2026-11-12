$fullTranscript = "C:\Users\danie_mdh4j2\.gemini\antigravity\brain\64991802-2aec-4b02-a304-a86f7dfba279\.system_generated\logs\transcript_full.jsonl"
$targetFile = "scratch\prompt_8957.md"

Get-Content $fullTranscript | ForEach-Object {
    $line = $_
    if ($line -match '"step_index":8957\b') {
        $obj = $line | ConvertFrom-Json
        $obj.content | Out-File -FilePath $targetFile -Encoding utf8
        Write-Host "Wrote step 8957 to $targetFile, length: $($obj.content.Length)"
    }
}

