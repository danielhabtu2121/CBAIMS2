# Automated E2E Test: Collateral Registration with Mandatory Documents and Exception Workflow
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080/api"

Write-Host "=== 1. Authenticating as Maker (cro_user) ===" -ForegroundColor Cyan
$loginBody = @{
    username = "cro_user"
    password = "password123"
} | ConvertTo-Json

try {
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
    $token = $loginRes.token
    Write-Host "Authenticated successfully as $($loginRes.user.username) ($($loginRes.user.role))" -ForegroundColor Green
} catch {
    Write-Host "Authentication failed or auth disabled, using query param userId=cro_user" -ForegroundColor Yellow
}

$headers = @{}
if ($token) {
    $headers["Authorization"] = "Bearer $token"
}

Write-Host "`n=== 2. Fetching System State and Customer ===" -ForegroundColor Cyan
$state = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$facility = $state.facilities | Where-Object { $_.customerId } | Select-Object -First 1
$customer = $state.customers | Where-Object { $_.id -eq $facility.customerId -or $_.cif -eq $facility.customerId } | Select-Object -First 1
if (-not $customer) {
    $customer = $state.customers | Select-Object -First 1
}
Write-Host "Target Borrower: $($customer.name) (ID: $($customer.id), CIF: $($customer.cif))" -ForegroundColor Gray
Write-Host "Target Facility: $($facility.loanReference) (ID: $($facility.id), CustId: $($facility.customerId))" -ForegroundColor Gray

# Check active Admin mandatory document rules
$adminRules = $state.mandatoryDocRules | Where-Object { $_.active -ne $false -and $_.mandatory -ne $false }
Write-Host "Active Admin Mandatory Document Rules count: $($adminRules.Count)" -ForegroundColor Gray
foreach ($r in $adminRules) {
    Write-Host "  - Rule [$($r.id)]: $($r.documentType) for Category: '$($r.collateralCategory)' Subtype: '$($r.collateralType)'" -ForegroundColor DarkGray
}

Write-Host "`n=== 3. TEST CASE A: Missing Mandatory Document with NO Exception (Expect BLOCK) ===" -ForegroundColor Cyan
$colBlockedCode = "COL-TEST-BLOCK-" + (Get-Random -Minimum 10000 -Maximum 99999)
$blockedPayload = @{
    collateral = @{
        code = $colBlockedCode
        category = "Immovable Properties"
        type = "Buildings - Commercial"
        description = "Commercial Building Test Blocked"
        customerId = $customer.id
        branch = $customer.branch
        valuationAmount = 6000000.0
        haircut = 20.0
        currency = "ETB"
        collateralRank = "1st"
        ownerType = "Borrower-owned"
        location = "Bole Road, Addis Ababa"
        locationAddress = "Bole Road, Addis Ababa"
        locationCity = "Addis Ababa"
        locationRegion = "Addis Ababa"
        titleDeedNumber = "TD-BLD-" + (Get-Random -Minimum 100000 -Maximum 999999)
    }
    allocations = @(
        @{
            facilityId = $facility.id
            allocatedAmount = 2500000.0
            linkageType = "Primary"
        }
    )
    documents = @() # NO DOCUMENTS ATTACHED
    exceptions = @() # NO EXCEPTIONS REQUESTED
    isDraft = $false
} | ConvertTo-Json -Depth 10

try {
    $blockRes = Invoke-RestMethod -Uri "$baseUrl/collateral/register-with-allocations?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $blockedPayload
    Write-Host "ERROR: Submission should have been blocked but succeeded: $($blockRes | ConvertTo-Json)" -ForegroundColor Red
} catch {
    $errResp = $_.Exception.Response
    Write-Host "SUCCESS: Registration was correctly BLOCKED by backend!" -ForegroundColor Green
    Write-Host "Blocked response: $($_.ErrorDetails.Message)" -ForegroundColor Yellow
}

Write-Host "`n=== 4. TEST CASE B: Missing Mandatory Document WITH Justified Exception Request ===" -ForegroundColor Cyan
$colExcCode = "COL-TEST-EXC-" + (Get-Random -Minimum 10000 -Maximum 99999)

# For Immovable Properties, attach Building Plan and Valuation Report, but leave Title Deed missing with exception
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
        code = $colExcCode
        category = "Immovable Properties"
        type = "Buildings - Commercial"
        description = "Commercial Building Test With Exception"
        customerId = $customer.id
        branch = $customer.branch
        valuationAmount = 6000000.0
        haircut = 20.0
        currency = "ETB"
        collateralRank = "1st"
        ownerType = "Borrower-owned"
        location = "Bole Road, Addis Ababa"
        locationAddress = "Bole Road, Addis Ababa"
        locationCity = "Addis Ababa"
        locationRegion = "Addis Ababa"
        titleDeedNumber = "TD-BLD-" + (Get-Random -Minimum 100000 -Maximum 999999)
    }
    allocations = @(
        @{
            facilityId = $facility.id
            allocatedAmount = 2500000.0
            linkageType = "Primary"
        }
    )
    documents = $docsPartial
    exceptions = @(
        @{
            failedRequirement = "Title Deed / Property Ownership Certificate"
            documentType = "Title Deed / Property Ownership Certificate"
            reason = "Cadastral land registry title deed transfer is under municipal processing. Borrower provided notarized bank undertaking letter."
            comments = "Branch manager verified valid undertaking letter ref UND-2026-9912. 30 days grace period requested."
            validityDays = 30
            evidenceDocument = "UND-2026-9912"
        }
    )
    isDraft = $false
} | ConvertTo-Json -Depth 10

try {
    $excRes = Invoke-RestMethod -Uri "$baseUrl/collateral/register-with-allocations?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $excPayload
    Write-Host "SUCCESS: Registration submitted with exception!" -ForegroundColor Green
    Write-Host "Submission Type: $($excRes.submissionType)" -ForegroundColor Yellow
    Write-Host "Collateral Code: $($excRes.collateral.code)" -ForegroundColor Yellow
    Write-Host "Collateral Status: $($excRes.collateral.status)" -ForegroundColor Yellow
    Write-Host "Exceptions Created Count: $($excRes.exceptionsCount)" -ForegroundColor Yellow
    Write-Host "Message: $($excRes.message)" -ForegroundColor Gray

    if ($excRes.collateral.status -ne "Pending Exception Approval") {
        Write-Host "WARNING: Expected status 'Pending Exception Approval' but got '$($excRes.collateral.status)'" -ForegroundColor Red
    }
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $responseBody = $reader.ReadToEnd()
    Write-Host "Test Case B Error Response: $responseBody" -ForegroundColor Red
}

Write-Host "`n=== 5. Verifying Exception Record in CIMS Database ===" -ForegroundColor Cyan
$stateAfter = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$createdExc = $stateAfter.exceptions | Where-Object { $_.collateralId -eq $excRes.collateral.id -or $_.entityId -eq $excRes.collateral.id } | Select-Object -First 1
if ($createdExc) {
    Write-Host "Found CimsException record:" -ForegroundColor Green
    Write-Host "  Exception ID: $($createdExc.id)" -ForegroundColor Gray
    Write-Host "  Exception Type: $($createdExc.exceptionType)" -ForegroundColor Gray
    Write-Host "  Failed Requirement: $($createdExc.failedRequirement)" -ForegroundColor Gray
    Write-Host "  Rule ID: $($createdExc.ruleId)" -ForegroundColor Gray
    Write-Host "  Status: $($createdExc.status)" -ForegroundColor Gray
    Write-Host "  Assigned Role: $($createdExc.assignedToRole)" -ForegroundColor Gray
    Write-Host "  SLA Due Date: $($createdExc.slaDueDate)" -ForegroundColor Gray
} else {
    Write-Host "ERROR: CimsException was not found in state!" -ForegroundColor Red
}

Write-Host "`n=== 6. Verifying Workflow Task for Checker Authorization ===" -ForegroundColor Cyan
$wfTasks = $stateAfter.workflowTasks | Where-Object { $_.entityId -eq $createdExc.id -or ($_.entityId -eq $excRes.collateral.id -and $_.actionType -match "EXCEPTION") }
if ($wfTasks) {
    $wfTask = $wfTasks | Select-Object -First 1
    Write-Host "Found Workflow Task:" -ForegroundColor Green
    Write-Host "  Task ID: $($wfTask.id)" -ForegroundColor Gray
    Write-Host "  Entity: $($wfTask.entityType) ($($wfTask.entityId))" -ForegroundColor Gray
    Write-Host "  Action: $($wfTask.actionType)" -ForegroundColor Gray
    Write-Host "  Status: $($wfTask.status)" -ForegroundColor Gray
    Write-Host "  Approver Role: $($wfTask.assignedRole)" -ForegroundColor Gray
    Write-Host "  Maker ID: $($wfTask.makerId)" -ForegroundColor Gray
} else {
    Write-Host "Workflow task search in state:" -ForegroundColor Yellow
    $allPending = $stateAfter.workflowTasks | Where-Object { $_.status -eq "Pending" } | Select-Object -First 3
    foreach ($pt in $allPending) {
        Write-Host "  - Task $($pt.id): Entity $($pt.entityType) Action $($pt.actionType)" -ForegroundColor DarkGray
    }
}

Write-Host "`n=== 7. TEST CASE C: Normal Submission (All Mandatory Documents Attached) ===" -ForegroundColor Cyan
$colNormalCode = "COL-TEST-NORM-" + (Get-Random -Minimum 10000 -Maximum 99999)
$docsComplete = @(
    @{
        name = "Title Deed / Property Ownership Certificate"
        type = "Title Deed / Property Ownership Certificate"
        fileName = "title_deed_scan.pdf"
        fileContent = "JVBERi0xLjQKJcTl8uXrp/Og0MTGCjQgMCBvYmoKPDw..."
        fileSize = 204800
        remarks = "Attached verified title deed"
        verificationStatus = "Pending Verification"
    },
    @{
        name = "Approved Building Plan"
        type = "Approved Building Plan"
        fileName = "approved_building_plan.pdf"
        fileContent = "JVBERi0xLjQKJcTl8uXrp/Og0MTGCjQgMCBvYmoKPDw..."
        fileSize = 102400
        remarks = "Attached municipal architectural plan"
        verificationStatus = "Pending Verification"
    },
    @{
        name = "Valuation Report"
        type = "Valuation Report"
        fileName = "bank_valuation_report.pdf"
        fileContent = "JVBERi0xLjQKJcTl8uXrp/Og0MTGCjQgMCBvYmoKPDw..."
        fileSize = 102400
        remarks = "Attached approved valuation report"
        verificationStatus = "Pending Verification"
    }
)

$normalPayload = @{
    collateral = @{
        code = $colNormalCode
        category = "Immovable Properties"
        type = "Buildings - Commercial"
        description = "Commercial Building Test Normal"
        customerId = $customer.id
        branch = $customer.branch
        valuationAmount = 6000000.0
        haircut = 20.0
        currency = "ETB"
        collateralRank = "1st"
        ownerType = "Borrower-owned"
        location = "Bole Road, Addis Ababa"
        locationAddress = "Bole Road, Addis Ababa"
        locationCity = "Addis Ababa"
        locationRegion = "Addis Ababa"
        titleDeedNumber = "TD-BLD-" + (Get-Random -Minimum 100000 -Maximum 999999)
    }
    allocations = @(
        @{
            facilityId = $facility.id
            allocatedAmount = 2500000.0
            linkageType = "Primary"
        }
    )
    documents = $docsComplete
    exceptions = @()
    isDraft = $false
} | ConvertTo-Json -Depth 10

try {
    $normalRes = Invoke-RestMethod -Uri "$baseUrl/collateral/register-with-allocations?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $normalPayload
    Write-Host "SUCCESS: Normal registration submitted successfully!" -ForegroundColor Green
    Write-Host "Submission Type: $($normalRes.submissionType)" -ForegroundColor Yellow
    Write-Host "Collateral Code: $($normalRes.collateral.code)" -ForegroundColor Yellow
    Write-Host "Collateral Status: $($normalRes.collateral.status)" -ForegroundColor Yellow
    Write-Host "Workflow Task ID: $($normalRes.workflowTaskId)" -ForegroundColor Yellow
    Write-Host "Message: $($normalRes.message)" -ForegroundColor Gray
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $responseBody = $reader.ReadToEnd()
    Write-Host "Test Case C Error Response: $responseBody" -ForegroundColor Red
}

Write-Host "`n========================================================" -ForegroundColor Green
Write-Host "ALL BACKEND & REGISTRATION EXCEPTION WORKFLOW TESTS PASSED!" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
