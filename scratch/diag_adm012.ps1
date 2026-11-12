$adminHeaders = @{
    "Content-Type" = "application/json"
    "X-User-Id" = "sysadmin"
    "X-User-Role" = "SYSADMIN"
}

Write-Host "=== TEST ADM-012: Notification Templates ==="

# 1. Invalid placeholder
$invalidTplPayload = @{
    subject = "Policy {{policyNumber}} Notice"
    body = "Dear Customer, {{invalidFieldPlaceholder}} is expiring."
} | ConvertTo-Json
try {
    $res1 = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/templates/validate" -Method Post -Headers $adminHeaders -Body $invalidTplPayload
    Write-Host "Validate Invalid Result:" ($res1 | ConvertTo-Json -Compress)
} catch {
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host "Validate Invalid ERROR:" $reader.ReadToEnd()
}

# 2. Valid placeholder
$validTplPayload = @{
    subject = "Policy {{policyNumber}} Expiry Reminder"
    body = "Dear {{customerName}}, your collateral {{collateralCode}} policy {{policyNumber}} expires on {{expiryDate}}."
} | ConvertTo-Json
try {
    $res2 = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/templates/validate" -Method Post -Headers $adminHeaders -Body $validTplPayload
    Write-Host "Validate Valid Result:" ($res2 | ConvertTo-Json -Compress)
} catch {
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host "Validate Valid ERROR:" $reader.ReadToEnd()
}

# 3. Save template
$tplEntity = @{
    id = "tpl-diag-" + (Get-Random -Minimum 1000 -Maximum 9999)
    name = "Staff Policy Expiry Alert"
    type = "Email"
    channel = "Email"
    triggerDaysBefore = 30
    subject = "Policy {{policyNumber}} Expiry Reminder"
    body = "Dear {{customerName}}, policy {{policyNumber}} expires on {{expiryDate}}."
    active = $true
} | ConvertTo-Json
try {
    $res3 = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/templates/save" -Method Post -Headers $adminHeaders -Body $tplEntity
    Write-Host "Save Template Result:" ($res3 | ConvertTo-Json -Compress)
} catch {
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host "Save Template ERROR:" $reader.ReadToEnd()
}

# 4. Preview template
$previewPayload = @{
    template = "Notice: Policy {{policyNumber}} for {{customerName}} matures on {{expiryDate}}."
    context = @{
        policyNumber = "POL-998877"
        customerName = "Almaz Ayana"
        expiryDate = "2026-12-31"
    }
} | ConvertTo-Json
try {
    $res4 = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/templates/preview" -Method Post -Headers $adminHeaders -Body $previewPayload
    Write-Host "Preview Template Result:" ($res4 | ConvertTo-Json -Compress)
} catch {
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host "Preview Template ERROR:" $reader.ReadToEnd()
}

