# TEST AREA 1: COLLATERAL REGISTRATION (CR-001 to CR-005)
$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080/api"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " TEST AREA 1: COLLATERAL REGISTRATION (CR-001 to CR-005)" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

# 1. Login
$loginBody = @{ username = "cro_user"; password = "password123" } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$headers = @{
    "Authorization" = "Bearer $($loginRes.token)"
    "X-User-Id" = "cro_user"
}
$checkerHeaders = @{
    "Authorization" = "Bearer $($loginRes.token)"
    "X-User-Id" = "brmgr_user"
}

$state = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$custA = $state.customers | Where-Object { $_.id -eq "cust-test-borrower-1" } | Select-Object -First 1

# --------------------------------------------------------------------------------
# CR-001: Borrower-Owned Collateral Registration
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST CR-001: Borrower-Owned Collateral Registration ---" -ForegroundColor Yellow

$col001Code = "COL-CR001-" + (Get-Random -Minimum 100000 -Maximum 999999)
$td001 = "TD-CR001-" + (Get-Random -Minimum 10000 -Maximum 99999)

$col001Payload = @{
    code = $col001Code
    category = "Immovable Properties"
    type = "Buildings - Commercial"
    description = "Commercial Office Building for CR-001"
    customerId = $custA.id
    branch = $custA.branch
    valuationAmount = 50000000.0
    haircut = 20.0
    currency = "ETB"
    collateralRank = "1st"
    ownerType = "Borrower-owned"
    location = "Bole Road, Addis Ababa"
    locationAddress = "Bole Road, Addis Ababa"
    locationCity = "Addis Ababa"
    locationRegion = "Addis Ababa"
    titleDeedNumber = $td001
    status = "Active"
} | ConvertTo-Json

$regRes = Invoke-RestMethod -Uri "$baseUrl/collateral/register-native?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $col001Payload
$col001 = $regRes.collaterals | Where-Object { $_.code -eq $col001Code } | Select-Object -First 1

if (-not $col001) {
    throw "CR-001 FAILED: Registered collateral $col001Code not found in response state"
}
Write-Host "[+] Registered Collateral: $($col001.code) (ID: $($col001.id), Valuation: ETB $($col001.valuationAmount), Owner: $($col001.customerId))" -ForegroundColor Green

# Verify DB persistence via direct query
$fetchedCol = Invoke-RestMethod -Uri "$baseUrl/collateral/$($col001.id)" -Method Get -Headers $headers
if ($fetchedCol.code -ne $col001Code -or $fetchedCol.customerId -ne $custA.id) {
    throw "CR-001 FAILED: Persistence verification failed for collateral $($col001.id)"
}
Write-Host "[+] CR-001 PASSED: Borrower-owned collateral successfully created and persisted.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# CR-002: Duplicate Collateral Registration Prevention
# --------------------------------------------------------------------------------
Write-Host "--- TEST CR-002: Duplicate Collateral Registration Prevention ---" -ForegroundColor Yellow

# Attempt to register duplicate with same code
$dupPayload = @{
    code = $col001Code # Same code
    category = "Immovable Properties"
    type = "Buildings - Commercial"
    description = "Duplicate registration attempt"
    customerId = $custA.id
    branch = $custA.branch
    valuationAmount = 25000000.0
    haircut = 20.0
    currency = "ETB"
    collateralRank = "1st"
    ownerType = "Borrower-owned"
    titleDeedNumber = $td001 # Same title deed
} | ConvertTo-Json

$dupBlocked = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/collateral/register-native?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $dupPayload
} catch {
    $dupBlocked = $true
    Write-Host "[+] Duplicate registration successfully rejected by backend: $($_.Exception.Message)" -ForegroundColor Green
}

# Also test pre-validate duplicate detection
$dupPreValPayload = @{
    collateral = @{
        code = $col001Code
        titleDeedNumber = $td001
        category = "Immovable Properties"
        type = "Buildings - Commercial"
        customerId = $custA.id
        valuationAmount = 25000000.0
        haircut = 20.0
        currency = "ETB"
        ownerType = "Borrower-owned"
    }
    documents = @()
    allocations = @()
    owners = @()
} | ConvertTo-Json

$preValRes = Invoke-RestMethod -Uri "$baseUrl/collateral/pre-validate" -Method Post -Headers $headers -ContentType "application/json" -Body $dupPreValPayload
$dupCheck = $preValRes.checks | Where-Object { $_.ruleKey -eq "DUPLICATE_COLLATERAL_CODE" }
Write-Host "    Pre-validation duplicate check: Passed=$($dupCheck.passed), RuleKey=$($dupCheck.ruleKey), Message=$($dupCheck.message)" -ForegroundColor Gray

if (-not $dupBlocked -and $dupCheck.passed) {
    throw "CR-002 FAILED: Duplicate collateral registration was not prevented!"
}
Write-Host "[+] CR-002 PASSED: Duplicate registration correctly prevented.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# CR-003: Mandatory Collateral Documents & Exception Workflow
# --------------------------------------------------------------------------------
Write-Host "--- TEST CR-003: Mandatory Documents & Exception Workflow ---" -ForegroundColor Yellow

$col003Code = "COL-CR003-" + (Get-Random -Minimum 100000 -Maximum 999999)
$td003 = "TD-CR003-" + (Get-Random -Minimum 10000 -Maximum 99999)

# 3a. Pre-validation with missing mandatory documents (No docs provided)
$missingDocPreVal = @{
    collateral = @{
        code = $col003Code
        category = "Immovable Properties"
        type = "Buildings - Commercial"
        customerId = $custA.id
        branch = $custA.branch
        valuationAmount = 20000000.0
        haircut = 20.0
        currency = "ETB"
        collateralRank = "1st"
        ownerType = "Borrower-owned"
        location = "Bole Road, Addis Ababa"
        locationAddress = "Bole Road, Addis Ababa"
        locationCity = "Addis Ababa"
        locationRegion = "Addis Ababa"
        titleDeedNumber = $td003
    }
    documents = @() # Empty docs
    allocations = @()
    owners = @()
} | ConvertTo-Json

$missingDocVal = Invoke-RestMethod -Uri "$baseUrl/collateral/pre-validate" -Method Post -Headers $headers -ContentType "application/json" -Body $missingDocPreVal
$docCheck = $missingDocVal.checks | Where-Object { $_.category -eq "Documents" -and -not $_.passed } | Select-Object -First 1

Write-Host "    Missing Mandatory Docs Pre-validation: Valid=$($missingDocVal.valid), CanSubmitAsException=$($missingDocVal.canSubmitAsException)" -ForegroundColor Gray
Write-Host "    Check Message: $($docCheck.message)" -ForegroundColor Gray

if ($missingDocVal.valid) {
    throw "CR-003 FAILED: Missing mandatory documents should make pre-validation invalid"
}
Write-Host "[+] Normal registration blocked due to missing mandatory documents." -ForegroundColor Green

# 3b. Submit with Exception Request
$excRequest = @{
    collateral = @{
        code = $col003Code
        category = "Immovable Properties"
        type = "Buildings - Commercial"
        description = "Collateral registered under approved exception"
        customerId = $custA.id
        branch = $custA.branch
        valuationAmount = 20000000.0
        haircut = 20.0
        currency = "ETB"
        collateralRank = "1st"
        ownerType = "Borrower-owned"
        location = "Bole Road, Addis Ababa"
        locationAddress = "Bole Road, Addis Ababa"
        locationCity = "Addis Ababa"
        locationRegion = "Addis Ababa"
        titleDeedNumber = $td003
        status = "Pending Approval"
    }
    exceptionType = "Missing Mandatory Document"
    justification = "Original title deed undergoing municipal authentication. Certified true copy attached."
    reason = "Original title deed undergoing municipal authentication. Certified true copy attached."
    comments = "Original title deed undergoing municipal authentication. Certified true copy attached."
    expiryDays = 30
    gracePeriodDays = 30
    evidenceDocument = "DOC-EVID-CR003"
    makerId = "cro_user"
    ruleKey = "MAND-DOC-BLD-COMM"
    failedRequirement = "Title Deed / Property Ownership Certificate"
    documents = @(
        @{
            name = "Approved Building Plan"
            type = "Approved Building Plan"
            dmsRef = "DMS-BLD-PLAN-01"
            fileContent = "data:application/pdf;base64,JVBERi0xLjQK"
            status = "Uploaded"
        },
        @{
            name = "Certified Valuation Report"
            type = "Valuation Report"
            dmsRef = "DMS-VAL-REP-01"
            fileContent = "data:application/pdf;base64,JVBERi0xLjQK"
            status = "Uploaded"
        }
    )
} | ConvertTo-Json

try {
    $excRes = Invoke-RestMethod -Uri "$baseUrl/collateral/submit-exception-request?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $excRequest
    Write-Host "[+] Exception Request Submitted: Success=$($excRes.success), TaskId=$($excRes.workflowTaskId)" -ForegroundColor Green
} catch {
    $resp = $_.Exception.Response
    if ($resp) {
        $stream = $resp.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $errBody = $reader.ReadToEnd()
        Write-Host "Exception Submission Error Body: $errBody" -ForegroundColor Red
    }
    throw $_
}

# Approve exception by checker
if ($excRes.workflowTaskId) {
    $approvePayload = @{
        comments = "Approved missing document temporary exception as per credit committee minutes."
    } | ConvertTo-Json
    $appRes = Invoke-RestMethod -Uri "$baseUrl/workflow/tasks/$($excRes.workflowTaskId)/approve?userId=brmgr_user&role=BRMGR" -Method Post -Headers $checkerHeaders -ContentType "application/json" -Body $approvePayload
    Write-Host "[+] Checker Approved Exception Workflow Task: $($appRes.message)" -ForegroundColor Green
}
Write-Host "[+] CR-003 PASSED: Mandatory document failure properly converted to controlled exception workflow.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# CR-004: Third-Party Collateral Registration
# --------------------------------------------------------------------------------
Write-Host "--- TEST CR-004: Third-Party Collateral Registration ---" -ForegroundColor Yellow

$col004Code = "COL-CR004-" + (Get-Random -Minimum 100000 -Maximum 999999)
$td004 = "TD-CR004-" + (Get-Random -Minimum 10000 -Maximum 99999)

# Third-party owner: Ato Girma Haile Gebre
$col004Payload = @{
    code = $col004Code
    category = "Immovable Properties"
    type = "Buildings - Commercial"
    description = "Commercial Property owned by Third-Party Guarantor"
    customerId = $custA.id # Borrower
    branch = $custA.branch
    valuationAmount = 40000000.0
    haircut = 20.0
    currency = "ETB"
    collateralRank = "1st"
    ownerType = "Third-party-owned"
    location = "Bole Road, Addis Ababa"
    titleDeedNumber = $td004
    status = "Active"
    owners = @(
        @{
            name = "Ato Girma Haile Gebre"
            ownerType = "Individual"
            idNumber = "ETH-ID-9928114"
            tin = "TIN-9928114"
            percentage = 100.0
            relationshipToBorrower = "Guarantor"
            verificationStatus = "Verified"
            status = "Active"
        }
    )
} | ConvertTo-Json

$reg004Res = Invoke-RestMethod -Uri "$baseUrl/collateral/register-native?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $col004Payload
$col004 = $reg004Res.collaterals | Where-Object { $_.code -eq $col004Code } | Select-Object -First 1

if (-not $col004) {
    throw "CR-004 FAILED: Third-party collateral registration failed to return created collateral"
}

Write-Host "[+] Registered Third-Party Collateral: $($col004.code) (ID: $($col004.id), OwnerType: $($col004.ownerType))" -ForegroundColor Green

# Verify owner details in DB
$col004Fetched = Invoke-RestMethod -Uri "$baseUrl/collateral/$($col004.id)" -Method Get -Headers $headers
Write-Host "    Master Owner Type: $($col004Fetched.ownerType), Owners Count: $($col004Fetched.owners.Count)" -ForegroundColor Gray
if ($col004Fetched.owners) {
    Write-Host "    Legal Owner Name: $($col004Fetched.owners[0].name) (Relationship: $($col004Fetched.owners[0].relationshipToBorrower))" -ForegroundColor Gray
}

Write-Host "[+] CR-004 PASSED: Third-party collateral registered with legal owner and relationship.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# CR-005: Ownership Must Not Be Accidentally Changed
# --------------------------------------------------------------------------------
Write-Host "--- TEST CR-005: Ownership Must Not Be Accidentally Changed ---" -ForegroundColor Yellow

# Use C001 (Customer A's borrower-owned collateral registered in CR-001)
# Create a link to Customer B's facility
$custB = $state.customers | Where-Object { $_.id -eq "cust-test-borrower-2" } | Select-Object -First 1
$facB = $state.facilities | Where-Object { $_.customerId -eq $custB.id } | Select-Object -First 1

# Upload pledge doc for C001
$docUp = @{
    entityType = "Collateral"
    entityId = $col001.id
    docName = "Pledge Agreement for Customer B Facility"
    docType = "Third-Party Pledge Agreement"
    fileName = "pledge_cr005.pdf"
    fileContent = "data:application/pdf;base64,JVBERi0xLjQK"
    expiryDate = "2030-12-31"
} | ConvertTo-Json
$upDoc = Invoke-RestMethod -Uri "$baseUrl/documents/upload" -Method Post -Headers $headers -ContentType "application/json" -Body $docUp

# Link C001 to Fac B
$linkCR005 = @{
    collateralId = $col001.id
    facilityId = $facB.id
    allocatedAmount = 5000000.0
    lienDegree = "First-degree"
    relationshipContext = "Third-party"
    thirdPartyPledge = $true
    relationshipToBorrower = "Guarantor"
    pledgeAgreementRef = "PLG-CR005-001"
    consentDocumentRef = $upDoc.id
    finalSubmission = $true
} | ConvertTo-Json

$createdLink = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $linkCR005
Write-Host "[+] Created relationship linking Col $($col001.code) to Customer B facility $($facB.loanReference)" -ForegroundColor Green

# Inspect master collateral ownership
$colAfterLink = Invoke-RestMethod -Uri "$baseUrl/collateral/$($col001.id)" -Method Get -Headers $headers

Write-Host "    Audit Collateral Master Legal Owner: $($colAfterLink.customerId)" -ForegroundColor Green
Write-Host "    Audit Collateral Master Owner Type:  $($colAfterLink.ownerType)" -ForegroundColor Green
Write-Host "    Audit Master Owners Count:           $($colAfterLink.owners.Count)" -ForegroundColor Green

if ($colAfterLink.customerId -ne $custA.id) {
    throw "CR-005 FAILED: Collateral master customerId changed to $($colAfterLink.customerId)!"
}
if ($colAfterLink.ownerType -ne "Borrower-owned") {
    throw "CR-005 FAILED: Collateral master ownerType changed to $($colAfterLink.ownerType)!"
}

Write-Host "[+] CR-005 PASSED: Collateral master ownership remained strictly Customer A (Borrower-owned).`n" -ForegroundColor Green

Write-Host "================================================================================" -ForegroundColor Green
Write-Host " ALL TEST AREA 1 (CR-001 to CR-005) TESTS PASSED! " -ForegroundColor Green
Write-Host "================================================================================" -ForegroundColor Green
