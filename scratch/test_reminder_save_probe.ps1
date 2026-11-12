$authBody = @{ username = "sysadmin"; password = "password123" } | ConvertTo-Json
$authResp = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $authBody -ContentType "application/json"
$h = @{ "Authorization" = "Bearer $($authResp.token)"; "Content-Type" = "application/json" }

$randDay = 185
$schedBody = @{
    daysBeforeExpiry = $randDay
    recipientRolesJson = '["CRO","BRMGR"]'
    channelsJson = '["Email"]'
    active = $true
} | ConvertTo-Json

Write-Host "Sending body: $schedBody"
try {
    $savedRem = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/reminder-schedules/save" -Method Post -Body $schedBody -Headers $h
    Write-Host "Success! Saved schedule: $($savedRem.id)"
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "Response details: $($reader.ReadToEnd())"
    }
}

