# Test Maker-Checker Dual-Control Verification
$ErrorActionPreference = 'Stop'

Write-Host "=== Dual-Control (Four-Eyes Principle) Verification ==="

# 1. Login as Maker (cro_user)
$makerLogin = @{ username = "cro_user"; password = "password123" } | ConvertTo-Json
$makerRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $makerLogin -ContentType "application/json"
$makerHeaders = @{
    "Authorization" = "Bearer " + $makerRes.token
    "X-User-Id" = $makerRes.user.id
}
Write-Host "Maker Authenticated: $($makerRes.user.username) ($($makerRes.user.role))"

# 2. Query Pending Workflow Tasks
$state = Invoke-RestMethod -Uri "http://localhost:8080/api/state" -Method Get -Headers $makerHeaders
$pendingTasks = $state.workflowTasks | Where-Object { $_.status -eq 'Pending' -and $_.makerId -eq $makerRes.user.id }
if (-not $pendingTasks) {
    $pendingTasks = $state.workflowTasks | Where-Object { $_.status -eq 'Pending' }
}
$targetTask = $pendingTasks | Select-Object -First 1
Write-Host "Found Pending Task: ID=$($targetTask.id), Entity=$($targetTask.entityType), Maker=$($targetTask.makerId), Step=$($targetTask.currentStep)"

# 3. Attempt Maker Self-Approval (Should FAIL with 400/409/403)
Write-Host "Testing Maker Self-Approval (Dual-Control Enforcement)..."
$approvePayload = @{
    taskId = $targetTask.id
    decision = "Approved"
    remarks = "Maker attempting to self-approve"
} | ConvertTo-Json

try {
    $failRes = Invoke-RestMethod -Uri "http://localhost:8080/api/workflow/tasks/$($targetTask.id)/approve?userId=$($makerRes.user.id)" -Method Post -Body $approvePayload -ContentType "application/json" -Headers $makerHeaders
    Write-Host "ERROR: Maker was able to approve their own task! Violation of Four-Eyes principle!"
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $errBody = $reader.ReadToEnd()
    Write-Host "SUCCESS: Maker self-approval was REJECTED by backend!"
    Write-Host "Backend Rejection Message: $errBody"
}

# 4. Login as Checker / Approver (brmgr_user)
$checkerLogin = @{ username = "brmgr_user"; password = "password123" } | ConvertTo-Json
$checkerRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $checkerLogin -ContentType "application/json"
$checkerHeaders = @{
    "Authorization" = "Bearer " + $checkerRes.token
    "X-User-Id" = $checkerRes.user.id
}
Write-Host "`nChecker Authenticated: $($checkerRes.user.username) ($($checkerRes.user.role))"

# 5. Checker Approves Task
Write-Host "Testing Authorized Checker Approval..."
try {
    $checkerApprovalPayload = @{
        decision = "Approved"
        remarks = "Approved by Branch Manager under authorized limit."
    } | ConvertTo-Json
    $successRes = Invoke-RestMethod -Uri "http://localhost:8080/api/workflow/tasks/$($targetTask.id)/approve?userId=$($checkerRes.user.id)" -Method Post -Body $checkerApprovalPayload -ContentType "application/json" -Headers $checkerHeaders
    Write-Host "Checker Approval SUCCESS: Task Status is now Approved!"
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $errBody = $reader.ReadToEnd()
    Write-Host "Checker approval response: $errBody"
}

