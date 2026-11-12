$adminHeaders = @{
    "Content-Type" = "application/json"
    "X-User-Id" = "sysadmin"
    "X-User-Role" = "SYSADMIN"
}

Write-Host "=== TEST 1: Parameter Save ==="
$param = @{
    paramKey = "approved_insurer_required"
    paramValue = "true"
    description = "Test Description"
} | ConvertTo-Json
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/parameters/save" -Method Post -Headers $adminHeaders -Body $param
    Write-Host "Parameter Save SUCCESS:" ($res | ConvertTo-Json -Compress)
} catch {
    Write-Host "Parameter Save ERROR:" $_.Exception.Message
    if ($_.ErrorDetails) { Write-Host "ErrorDetails:" $_.ErrorDetails.Message }
}

Write-Host "`n=== TEST 2: Reminder Schedule Save ==="
$rem = @{
    id = "rem-45"
    daysBeforeExpiry = 45
    audience = "Staff"
    triggerBasis = "Before Expiry"
    offsetDays = 45
    dayType = "Working Day"
    recurrence = "One-time"
    continueUntil = "Renewed"
    channelsJson = '["Email","SMS"]'
    recipientRolesJson = '["CRO","BRMGR"]'
    active = $true
} | ConvertTo-Json
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/reminder-schedules/save" -Method Post -Headers $adminHeaders -Body $rem
    Write-Host "Reminder Save SUCCESS:" ($res | ConvertTo-Json -Compress)
} catch {
    Write-Host "Reminder Save ERROR:" $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response Body:" $reader.ReadToEnd()
    }
}

Write-Host "`n=== TEST 3: Notification Template Validation & Save ==="
$tpl = @{
    templateType = "POLICY_EXPIRY_STAFF"
    channel = "Email"
    subjectTemplate = "Policy {{policyNumber}} Expiry Reminder"
    bodyTemplate = "Dear {{customerName}}, policy {{policyNumber}} expires on {{expiryDate}}."
    active = $true
} | ConvertTo-Json
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/templates/save" -Method Post -Headers $adminHeaders -Body $tpl
    Write-Host "Template Save SUCCESS:" ($res | ConvertTo-Json -Compress)
} catch {
    Write-Host "Template Save ERROR:" $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response Body:" $reader.ReadToEnd()
    }
}

Write-Host "`n=== TEST 4: District Save ==="
$dist = @{
    districtName = "North Addis Regional District"
    branchName = "Bole Special Branch"
    regionalDirectorName = "Ato Solomon Tekle"
    contactEmail = "solomon@bank.et"
} | ConvertTo-Json
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/districts/save" -Method Post -Headers $adminHeaders -Body $dist
    Write-Host "District Save SUCCESS:" ($res | ConvertTo-Json -Compress)
} catch {
    Write-Host "District Save ERROR:" $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response Body:" $reader.ReadToEnd()
    }
}
