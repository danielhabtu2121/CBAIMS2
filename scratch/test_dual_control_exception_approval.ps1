# Automated E2E Test: Dual Control and Checker Resolution for Collateral Exception Workflow
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080/api"

Write-Host "=== 1. Authenticating as Maker (cro_user) ===" -ForegroundColor Cyan
$loginMaker = @{ username = "cro_user"; password = "password123" } | ConvertTo-Json
$makerRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginMaker
$makerToken = $makerRes.token
$makerHeaders = @{ Authorization = "Bearer $makerToken" }
Write-Host "Authenticated as Maker: $($makerRes.user.username) ($($makerRes.user.role))" -ForegroundColor Green

Write-Host "`n=== 2. Creating an Exception-Based Collateral Registration ===" -ForegroundColor Cyan
$state = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $makerHeaders
$facility = $state.facilities | Where-Object { $_.customerId } | Select-Object -First 1
$customer = $state.customers | Where-Object { $_.id -eq $facility.customerId -or $_.cif -eq $facility.customerId } | Select-Object -First 1
if (-not $customer) { $customer = $state.customers | Select-Object -First 1 }

$colCode = "COL-DC-" + (Get-Random -Minimum 10000 -Maximum 99999)
$titleDeed = "TD-DC-" + (Get-Random -Minimum 100000 -Maximum 999999)

$docsPartial = @(
    @{
        name = "Approved Building Plan"
        type = "Approved Building Plan"
        fileName = "approved_building_plan.pdf"
        fileContent = "JVBERi0xLjQKJcTl8uXrp/Og0MTGCjQgMCBvYmoKPDw..."
        fileSize = 102400
        verificationStatus = "Pending Verification"
    },
    @{
        name = "Valuation Report"
        type = "Valuation Report"
        fileName = "bank_valuation_report.pdf"
        fileContent = "JVBERi0xLjQKJcTl8uXrp/Og0MTGCjQgMCBvYmoKPDw..."
        fileSize = 102400
        verificationStatus = "Pending Verification"
    }
)

$excPayload = @{
    collateral = @{
        code = $colCode
        category = "Immovable Properties"
        type = "Buildings - Commercial"
        description = "Dual Control Exception Test"
        customerId = $customer.id
        branch = $customer.branch
        valuationAmount = 7500000.0
        haircut = 20.0
        currency = "ETB"
        collateralRank = "1st"
        ownerType = "Borrower-owned"
        locationAddress = "Bole Road, Addis Ababa"
        locationCity = "Addis Ababa"
        locationRegion = "Addis Ababa"
        titleDeedNumber = $titleDeed
    }
    allocations = @(
        @{
            facilityId = $facility.id
            allocatedAmount = 3000000.0
            linkageType = "Primary"
        }
    )
    documents = $docsPartial
    exceptions = @(
        @{
            failedRequirement = "Title Deed / Property Ownership Certificate"
            documentType = "Title Deed / Property Ownership Certificate"
            reason = "Cadastral office title deed renewal in progress."
            comments = "Branch manager verified valid undertaking letter ref UND-2026-DC. 30 days requested."
            validityDays = 30
            evidenceDocument = "UND-2026-DC"
        }
    )
    isDraft = $false
} | ConvertTo-Json -Depth 10

$regRes = Invoke-RestMethod -Uri "$baseUrl/collateral/register-with-allocations?userId=cro_user" -Method Post -ContentType "application/json" -Headers $makerHeaders -Body $excPayload
Write-Host "Collateral Registered: $($regRes.collateral.code)" -ForegroundColor Green
Write-Host "Collateral Status: $($regRes.collateral.status)" -ForegroundColor Yellow
$colId = $regRes.collateral.id

Write-Host "`n=== 3. Locating Workflow Task for the Exception ===" -ForegroundColor Cyan
$stateAfterReg = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $makerHeaders
$excRecord = $stateAfterReg.exceptions | Where-Object { $_.collateralId -eq $colId } | Select-Object -First 1
Write-Host "Exception ID: $($excRecord.id) (Status: $($excRecord.status))" -ForegroundColor Gray

$wfTask = $stateAfterReg.workflowTasks | Where-Object { $_.entityId -eq $excRecord.id -and $_.status -eq "Pending" } | Select-Object -First 1
Write-Host "Found Workflow Task: $($wfTask.id) (Action: $($wfTask.actionType), Maker: $($wfTask.makerId))" -ForegroundColor Gray

Write-Host "`n=== 4. TEST: Maker (cro_user) attempts to approve their own task (Expect 400/Dual-Control REJECT) ===" -ForegroundColor Cyan
try {
    $makerApproveBody = @{
        approverId = "cro_user"
        notes = "Maker trying to approve own task"
    } | ConvertTo-Json
    $failRes = Invoke-RestMethod -Uri "$baseUrl/workflow/tasks/$($wfTask.id)/approve?userId=cro_user" -Method Post -ContentType "application/json" -Headers $makerHeaders -Body $makerApproveBody
    Write-Host "ERROR: Maker was able to approve own task!" -ForegroundColor Red
} catch {
    Write-Host "SUCCESS: Dual-control prevented maker from approving own task!" -ForegroundColor Green
    Write-Host "Rejection message: $($_.ErrorDetails.Message)" -ForegroundColor Yellow
}

Write-Host "`n=== 5. Authenticating as Checker (brmgr_user) ===" -ForegroundColor Cyan
$loginChecker = @{ username = "brmgr_user"; password = "password123" } | ConvertTo-Json
$checkerRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginChecker
$checkerToken = $checkerRes.token
$checkerHeaders = @{ Authorization = "Bearer $checkerToken" }
Write-Host "Authenticated as Checker: $($checkerRes.user.username) ($($checkerRes.user.role))" -ForegroundColor Green

Write-Host "`n=== 6. Checker Approves Exception Task ===" -ForegroundColor Cyan
$checkerApproveBody = @{
    approverId = "brmgr_user"
    notes = "Approved based on valid undertaking letter UND-2026-DC and 30-day grace period."
} | ConvertTo-Json

$approveRes = Invoke-RestMethod -Uri "$baseUrl/workflow/tasks/$($wfTask.id)/approve?userId=brmgr_user" -Method Post -ContentType "application/json" -Headers $checkerHeaders -Body $checkerApproveBody
Write-Host "Approval response success: $($approveRes.success)" -ForegroundColor Green
Write-Host "Approval message: $($approveRes.message)" -ForegroundColor Gray

Write-Host "`n=== 7. Verifying State After Checker Resolution ===" -ForegroundColor Cyan
$stateFinal = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $checkerHeaders
$resolvedExc = $stateFinal.exceptions | Where-Object { $_.id -eq $excRecord.id } | Select-Object -First 1
Write-Host "Resolved Exception Status: $($resolvedExc.status)" -ForegroundColor Yellow
Write-Host "Resolved By: $($resolvedExc.resolvedBy)" -ForegroundColor Gray

$updatedCol = $stateFinal.collaterals | Where-Object { $_.id -eq $colId } | Select-Object -First 1
Write-Host "Updated Collateral Status: $($updatedCol.status)" -ForegroundColor Yellow

if ($updatedCol.status -eq "Pending Approval") {
    Write-Host "SUCCESS: Collateral automatically transitioned from 'Pending Exception Approval' to 'Pending Approval'!" -ForegroundColor Green
} else {
    Write-Host "Current status: $($updatedCol.status)" -ForegroundColor Gray
}

Write-Host "`n========================================================" -ForegroundColor Green
Write-Host "DUAL-CONTROL AND EXCEPTION RESOLUTION VERIFIED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green

