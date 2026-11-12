$filePath = 'C:\Users\danie_mdh4j2\.gemini\antigravity\brain\64991802-2aec-4b02-a304-a86f7dfba279\.system_generated\logs\transcript_full.jsonl'
$match = Select-String -Path $filePath -Pattern 'TEST AREA 3' -Context 0, 50 | Select-Object -First 1
if ($match) {
    $match.Context.PostContext | Out-File -FilePath 'scratch/area3_spec.txt'
}
$match4 = Select-String -Path $filePath -Pattern 'TEST AREA 4' -Context 0, 80 | Select-Object -First 1
if ($match4) {
    $match4.Context.PostContext | Out-File -FilePath 'scratch/area4_spec.txt'
}

