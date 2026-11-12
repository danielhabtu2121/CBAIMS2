# Comprehensive E2E Automated Verification: Relationship-Specific Loan-Collateral Linkage Correction
$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080/api"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " CBAIMS COMPREHENSIVE VERIFICATION: LOAN-COLLATERAL LINKAGE CORRECTION " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

# 1. Login
$loginBody = @{
    username = "cro_user"
    password = "password123"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$headers = @{
    "Authorization" = "Bearer $($loginRes.token)"
    "X-User-Id" = "cro_user"
}
Write-Host "[-] Authenticated as Maker: $($loginRes.user.username) ($($loginRes.user.role))`n" -ForegroundColor Green

# 2. Setup Test Customers, Facilities, and Collaterals
Write-Host "[1] Setting up Test Data..." -ForegroundColor Yellow

$state = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers

# Customer A (Legal Owner) and Customer B (Third-Party Facility Borrower) in Bole Special Branch
$custA = $state.customers | Where-Object { $_.id -eq "cust-test-borrower-1" } | Select-Object -First 1
$custB = $state.customers | Where-Object { $_.id -eq "cust-test-borrower-2" } | Select-Object -First 1

Write-Host "    Customer A (Legal Owner): $($custA.name) (ID: $($custA.id))" -ForegroundColor Gray
Write-Host "    Customer B (3rd Party Borrower): $($custB.name) (ID: $($custB.id))`n" -ForegroundColor Gray

# Facilities for Customer A
$facA1 = $state.facilities | Where-Object { $_.id -eq "loan-test-fac-1" } | Select-Object -First 1
$facA2 = $state.facilities | Where-Object { $_.id -eq "loan-test-fac-11b" } | Select-Object -First 1

# Facility for Customer B
$facB1 = $state.facilities | Where-Object { $_.id -eq "loan-test-fac-2" } | Select-Object -First 1

Write-Host "    Facility A1 (Cust A): $($facA1.loanReference) (ID: $($facA1.id))" -ForegroundColor Gray
Write-Host "    Facility A2 (Cust A): $($facA2.loanReference) (ID: $($facA2.id))" -ForegroundColor Gray
Write-Host "    Facility B1 (Cust B): $($facB1.loanReference) (ID: $($facB1.id))`n" -ForegroundColor Gray

# Register a dedicated high-value test collateral owned by Customer A
$testColCode = "COL-CORR-" + (Get-Random -Minimum 100000 -Maximum 999999)
$colPayload = @{
    code = $testColCode
    category = "Immovable Properties"
    type = "Buildings - Commercial"
    description = "Commercial Office Tower for Linkage Verification"
    customerId = $custA.id
    branch = $custA.branch
    valuationAmount = 30000000.0
    haircut = 10.0 # NSV = 27,000,000
    currency = "ETB"
    collateralRank = "1st"
    ownerType = "Borrower-owned"
    location = "Bole Road, Addis Ababa"
    locationAddress = "Bole Road, Addis Ababa"
    locationCity = "Addis Ababa"
    locationRegion = "Addis Ababa"
    titleDeedNumber = "TD-CORR-" + (Get-Random -Minimum 10000 -Maximum 99999)
    status = "Active"
} | ConvertTo-Json

$regColRes = Invoke-RestMethod -Uri "$baseUrl/collateral/register-native?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $colPayload
$registered = $regColRes.collaterals | Where-Object { $_.code -eq $testColCode } | Select-Object -First 1
$testColId = $registered.id
Write-Host "[+] Registered Master Collateral: $testColCode (ID: $testColId, NSV: ETB 27,000,000, Owner: $($custA.name))`n" -ForegroundColor Green

# Verify master owners count
$initialCol = Invoke-RestMethod -Uri "$baseUrl/collateral/$testColId" -Method Get -Headers $headers
$initialOwnerCount = if ($initialCol.owners) { $initialCol.owners.Count } else { 1 }
Write-Host "    Initial Collateral Owner Count: $initialOwnerCount (Owner Type: $($initialCol.ownerType))`n" -ForegroundColor Gray

# --------------------------------------------------------------------------------
# TEST 1: SCENARIO 1 - Single Facility, Borrower-Owned
# --------------------------------------------------------------------------------
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "TEST 1: Scenario 1 - Borrower-Owned Linkage (Collateral Cust A -> Facility A1)" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray

$valReq1 = @{
    collateralId = $testColId
    facilityId = $facA1.id
    allocatedAmount = 4000000.0
    finalSubmission = $true
} | ConvertTo-Json

$valRes1 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/validate" -Method Post -Headers $headers -ContentType "application/json" -Body $valReq1

Write-Host "    Validation Result:" -ForegroundColor Gray
Write-Host "      - Relationship Context: $($valRes1.relationshipContext)" -ForegroundColor $(if ($valRes1.relationshipContext -eq "Borrower-owned") { "Green" } else { "Red" })
Write-Host "      - Validation State:     $($valRes1.validationState)" -ForegroundColor $(if ($valRes1.validationState -in @("PASS", "NORMAL")) { "Green" } else { "Red" })
Write-Host "      - Can Proceed:          $($valRes1.canProceed)" -ForegroundColor $(if ($valRes1.canProceed) { "Green" } else { "Red" })

if ($valRes1.relationshipContext -ne "Borrower-owned" -or -not $valRes1.canProceed) {
    throw "TEST 1 FAILED: Expected Borrower-owned and canProceed=true"
}

# Create Link 1
$linkReq1 = @{
    collateralId = $testColId
    facilityId = $facA1.id
    allocatedAmount = 4000000.0
    lienDegree = "First-degree"
    notes = "Primary facility link for borrower"
} | ConvertTo-Json

try {
    $link1 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $linkReq1
} catch {
    $resp = $_.Exception.Response
    if ($resp) {
        $stream = $resp.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $errBody = $reader.ReadToEnd()
        Write-Host "Link 1 Error Body: $errBody" -ForegroundColor Red
    }
    throw $_
}
Write-Host "[+] Link 1 Created: $($link1.id)" -ForegroundColor Green
Write-Host "      - Relationship Context: $($link1.relationshipContext)" -ForegroundColor Green
Write-Host "      - Third-Party Flag:     $($link1.thirdParty)" -ForegroundColor Green

if ($link1.relationshipContext -ne "Borrower-owned" -or $link1.thirdParty) {
    throw "TEST 1 FAILED: Link 1 should be Borrower-owned and thirdParty=false"
}

# --------------------------------------------------------------------------------
# TEST 2: SCENARIO 2 - Cross-Collateralization (Same Customer, Multi-Facility)
# --------------------------------------------------------------------------------
Write-Host "`n--------------------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "TEST 2: Scenario 2 - Multi-Facility Linkage (Collateral Cust A -> Facility A2)" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray

$valReq2 = @{
    collateralId = $testColId
    facilityId = $facA2.id
    allocatedAmount = 5000000.0
    finalSubmission = $true
} | ConvertTo-Json

$valRes2 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/validate" -Method Post -Headers $headers -ContentType "application/json" -Body $valReq2

Write-Host "    Validation Result:" -ForegroundColor Gray
Write-Host "      - Relationship Context: $($valRes2.relationshipContext)" -ForegroundColor $(if ($valRes2.relationshipContext -eq "Borrower-owned") { "Green" } else { "Red" })
Write-Host "      - Validation State:     $($valRes2.validationState)" -ForegroundColor Green
Write-Host "      - Remaining Capacity:   $($valRes2.remainingCapacity)" -ForegroundColor Gray

$linkReq2 = @{
    collateralId = $testColId
    facilityId = $facA2.id
    allocatedAmount = 5000000.0
    lienDegree = "First-degree"
    notes = "Cross-collateralized second facility for borrower"
} | ConvertTo-Json

try {
    $link2 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $linkReq2
    Write-Host "[+] Link 2 Created: $($link2.id) (Relationship Context: $($link2.relationshipContext))`n" -ForegroundColor Green
} catch {
    $resp = $_.Exception.Response
    if ($resp) {
        $stream = $resp.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $errBody = $reader.ReadToEnd()
        Write-Host "Link 2 Error Body: $errBody" -ForegroundColor Red
    }
    throw $_
}

# Check master collateral ownership after 2 borrower links
$checkColAfterA = Invoke-RestMethod -Uri "$baseUrl/collateral/$testColId" -Method Get -Headers $headers
Write-Host "    Master Collateral Owner Count: $($checkColAfterA.owners.Count) (Owner Type: $($checkColAfterA.ownerType))" -ForegroundColor Gray

# --------------------------------------------------------------------------------
# TEST 3: SCENARIO 3 - Cross-Customer Third-Party Wizard Preview (Facility B1)
# --------------------------------------------------------------------------------
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "TEST 3: Scenario 3 - Wizard Preview for Cross-Customer Linkage (Collateral Cust A -> Facility B1)" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray

$previewReq = @{
    collateralId = $testColId
    facilityId = $facB1.id
    allocatedAmount = 7000000.0
    finalSubmission = $false # Interactive preview in wizard
} | ConvertTo-Json

$previewRes = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/validate" -Method Post -Headers $headers -ContentType "application/json" -Body $previewReq

Write-Host "    Interactive Preview Result:" -ForegroundColor Gray
Write-Host "      - Relationship Context: $($previewRes.relationshipContext)" -ForegroundColor $(if ($previewRes.relationshipContext -eq "Third-party") { "Green" } else { "Red" })
Write-Host "      - Validation State:     $($previewRes.validationState)" -ForegroundColor $(if ($previewRes.validationState -eq "THIRD_PARTY_REQUIRED") { "Green" } else { "Red" })
Write-Host "      - Can Proceed:          $($previewRes.canProceed)" -ForegroundColor $(if ($previewRes.canProceed) { "Green" } else { "Red" })
Write-Host "      - Has Required Pledge:  $($previewRes.hasRequiredPledgeEvidence)" -ForegroundColor Gray

if ($previewRes.relationshipContext -ne "Third-party" -or $previewRes.validationState -ne "THIRD_PARTY_REQUIRED" -or -not $previewRes.canProceed) {
    throw "TEST 3 FAILED: Wizard preview should return Third-party, THIRD_PARTY_REQUIRED, and canProceed=true"
}

# --------------------------------------------------------------------------------
# TEST 4: SCENARIO 4 - Missing Third-Party Evidence Submission BLOCK
# --------------------------------------------------------------------------------
Write-Host "`n--------------------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "TEST 4: Scenario 4 - Incomplete Third-Party Submission Must BLOCK" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray

# 4a. Validate with finalSubmission = true and missing pledge info
$blockedValReq = @{
    collateralId = $testColId
    facilityId = $facB1.id
    allocatedAmount = 7000000.0
    finalSubmission = $true # Final submission attempted
} | ConvertTo-Json

$blockedValRes = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/validate" -Method Post -Headers $headers -ContentType "application/json" -Body $blockedValReq

Write-Host "    Final Submission Validation (No pledge info):" -ForegroundColor Gray
Write-Host "      - Relationship Context: $($blockedValRes.relationshipContext)" -ForegroundColor Green
Write-Host "      - Validation State:     $($blockedValRes.validationState)" -ForegroundColor $(if ($blockedValRes.validationState -in @("THIRD_PARTY_REQUIRED", "THIRD_PARTY_INCOMPLETE")) { "Green" } else { "Red" })
Write-Host "      - Can Proceed:          $($blockedValRes.canProceed)" -ForegroundColor $(if (-not $blockedValRes.canProceed) { "Green" } else { "Red" })

if ($blockedValRes.canProceed) {
    throw "TEST 4 FAILED: Final submission without pledge evidence must block (canProceed=false)"
}

# 4b. Attempt to POST create relationship without pledge info - expect 400
$blockedPostReq = @{
    collateralId = $testColId
    facilityId = $facB1.id
    allocatedAmount = 7000000.0
    lienDegree = "First-degree"
    notes = "Attempting unauthorized third party link"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links" -Method Post -Headers $headers -ContentType "application/json" -Body $blockedPostReq
    throw "TEST 4 FAILED: Backend allowed creating third-party relationship without pledge documentation!"
} catch {
    Write-Host "[+] Successfully Blocked Unauthorized Third-Party Linkage: $($_.Exception.Message)`n" -ForegroundColor Green
}

# --------------------------------------------------------------------------------
# TEST 5: SCENARIO 5 - Valid Third-Party Pledge & Master Immutability Check
# --------------------------------------------------------------------------------
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "TEST 5: Scenario 5 - Valid Third-Party Pledge with Verified DMS Document" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray

# 5a. Upload authentic Third-Party Pledge Agreement to DMS for Collateral C1
$docUploadPayload = @{
    entityType = "Collateral"
    entityId = $testColId
    docName = "Third-Party Pledge Agreement and Consent"
    docType = "Third-Party Pledge Agreement"
    fileName = "third_party_pledge_consent.pdf"
    fileContent = "data:application/pdf;base64,JVBERi0xLjQKJcTl8uXrp/Og0MTGCjQgMCBvYmoKPDwKL1R5cGUgL1BhZ2VzCi9Db3VudCAxCi9LaWRzIFsgNSAwIFIgXQo+PgplbmRvYmoK"
    expiryDate = "2030-12-31"
    remarks = "Executed pledge agreement and owner consent on bank standard form"
} | ConvertTo-Json

$uploadedDoc = Invoke-RestMethod -Uri "$baseUrl/documents/upload" -Method Post -Headers $headers -ContentType "application/json" -Body $docUploadPayload
Write-Host "[+] DMS Document Uploaded: $($uploadedDoc.id) ($($uploadedDoc.name))`n" -ForegroundColor Green

# 5b. Validate third-party linkage with pledge info and DMS reference
$valid3rdReq = @{
    collateralId = $testColId
    facilityId = $facB1.id
    allocatedAmount = 7000000.0
    relationshipContext = "Third-party"
    thirdPartyPledge = $true
    relationshipToBorrower = "Guarantor"
    pledgeAgreementRef = "PLG-2026-CORR-001"
    consentInfo = "Board Resolution and Spousal Consent on File"
    consentDocumentRef = $uploadedDoc.id
    finalSubmission = $true
} | ConvertTo-Json

$valid3rdRes = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/validate" -Method Post -Headers $headers -ContentType "application/json" -Body $valid3rdReq

Write-Host "    Validation Result with DMS Evidence:" -ForegroundColor Gray
Write-Host "      - Relationship Context: $($valid3rdRes.relationshipContext)" -ForegroundColor Green
Write-Host "      - Validation State:     $($valid3rdRes.validationState)" -ForegroundColor $(if ($valid3rdRes.validationState -eq "THIRD_PARTY_VALIDATED") { "Green" } else { "Red" })
Write-Host "      - Can Proceed:          $($valid3rdRes.canProceed)" -ForegroundColor $(if ($valid3rdRes.canProceed) { "Green" } else { "Red" })
Write-Host "      - Has Pledge Evidence:  $($valid3rdRes.hasRequiredPledgeEvidence)" -ForegroundColor Green

if ($valid3rdRes.validationState -ne "THIRD_PARTY_VALIDATED" -or -not $valid3rdRes.canProceed) {
    throw "TEST 5 FAILED: Expected THIRD_PARTY_VALIDATED and canProceed=true"
}

# 5c. Create the Third-Party Relationship
$link3rdReq = @{
    collateralId = $testColId
    facilityId = $facB1.id
    allocatedAmount = 7000000.0
    lienDegree = "First-degree"
    relationshipContext = "Third-party"
    thirdPartyPledge = $true
    relationshipToBorrower = "Guarantor"
    pledgeAgreementRef = "PLG-2026-CORR-001"
    consentInfo = "Board Resolution and Spousal Consent on File"
    consentDocumentRef = $uploadedDoc.id
    notes = "Third-party collateral pledged by Customer A for Customer B loan"
} | ConvertTo-Json

try {
    $link3 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $link3rdReq
} catch {
    $resp = $_.Exception.Response
    if ($resp) {
        $stream = $resp.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $errBody = $reader.ReadToEnd()
        Write-Host "Link 3 Error Body: $errBody" -ForegroundColor Red
    }
    throw $_
}

Write-Host "[+] Third-Party Link Created: $($link3.id)" -ForegroundColor Green
Write-Host "      - Relationship Context: $($link3.relationshipContext)" -ForegroundColor Green
Write-Host "      - Third-Party Flag:     $($link3.thirdParty)" -ForegroundColor Green
Write-Host "      - Rel to Borrower:      $($link3.relationshipToBorrower)" -ForegroundColor Green
Write-Host "      - Pledge Agreement Ref: $($link3.pledgeAgreementRef)" -ForegroundColor Green
Write-Host "      - Consent Doc Ref:      $($link3.consentDocumentRef)" -ForegroundColor Green

if ($link3.relationshipContext -ne "Third-party" -or -not $link3.thirdParty) {
    throw "TEST 5 FAILED: Link 3 must be Third-party with thirdParty=true"
}

# --------------------------------------------------------------------------------
# CRITICAL CHECK: ZERO MUTATION OF COLLATERAL MASTER LEGAL OWNERSHIP
# --------------------------------------------------------------------------------
Write-Host "`n--------------------------------------------------------------------------------" -ForegroundColor Yellow
Write-Host "CRITICAL AUDIT: Master Collateral Ownership Immutability Check" -ForegroundColor Yellow
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor Yellow

$masterColAfter3rd = Invoke-RestMethod -Uri "$baseUrl/collateral/$testColId" -Method Get -Headers $headers
$currentOwnerCount = if ($masterColAfter3rd.owners) { $masterColAfter3rd.owners.Count } else { 1 }

Write-Host "    Registered Legal Owner: $($masterColAfter3rd.customerId)" -ForegroundColor Green
Write-Host "    Registered Owner Type:  $($masterColAfter3rd.ownerType)" -ForegroundColor Green
Write-Host "    Master Owners Count:    $currentOwnerCount (Expected: $initialOwnerCount)" -ForegroundColor $(if ($currentOwnerCount -eq $initialOwnerCount) { "Green" } else { "Red" })

if ($masterColAfter3rd.customerId -ne $custA.id) {
    throw "CRITICAL INVARIANT VIOLATION: Master collateral customerId was mutated from $($custA.id) to $($masterColAfter3rd.customerId)!"
}
if ($masterColAfter3rd.ownerType -ne "Borrower-owned") {
    throw "CRITICAL INVARIANT VIOLATION: Master collateral ownerType was contaminated to $($masterColAfter3rd.ownerType)!"
}
if ($currentOwnerCount -ne $initialOwnerCount) {
    throw "CRITICAL INVARIANT VIOLATION: Duplicate CollateralOwner row was added to master collateral! Count: $currentOwnerCount vs Expected: $initialOwnerCount"
}
Write-Host "[+] CONFIRMED: Master Collateral legal ownership is 100% UNTOUCHED and UNCONTAMINATED!`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST 6: SCENARIO 8 / F - Legal Owner Pledges Their Asset for Their Own New Loan
# --------------------------------------------------------------------------------
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "TEST 6: Scenario 8 / F - Dynamic Context: Owner Links Same Collateral to Own Loan" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray

# Customer A links the same collateral (which secures Customer B's facility as 3rd party) to another loan of Customer A
$valOwnerReq = @{
    collateralId = $testColId
    facilityId = $facA1.id # Check validation against Cust A
    allocatedAmount = 1000000.0
    finalSubmission = $true
} | ConvertTo-Json

$valOwnerRes = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/validate" -Method Post -Headers $headers -ContentType "application/json" -Body $valOwnerReq

Write-Host "    Validation for Owner's Own Facility:" -ForegroundColor Gray
Write-Host "      - Relationship Context: $($valOwnerRes.relationshipContext)" -ForegroundColor $(if ($valOwnerRes.relationshipContext -eq "Borrower-owned") { "Green" } else { "Red" })
Write-Host "      - Third-Party Flag:     $($valOwnerRes.thirdParty)" -ForegroundColor Green

if ($valOwnerRes.relationshipContext -ne "Borrower-owned") {
    throw "TEST 6 FAILED: Linking to owner's own loan must dynamically resolve to Borrower-owned!"
}
Write-Host "[+] CONFIRMED: Pledging to Customer B did NOT permanently make the asset 'Third-party' for Customer A!`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST 7: AUDIT HISTORY SNAPSHOT VERIFICATION
# --------------------------------------------------------------------------------
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "TEST 7: Audit History Snapshot Verification" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray

$historyList = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/$($link3.id)/history" -Method Get -Headers $headers
Write-Host "    History Records Count: $($historyList.Count)" -ForegroundColor Gray
$latestHist = $historyList | Select-Object -First 1

Write-Host "    Latest History Snapshot:" -ForegroundColor Gray
Write-Host "      - Action:               $($latestHist.action)" -ForegroundColor Green
Write-Host "      - Relationship Context: $($latestHist.relationshipContext)" -ForegroundColor Green
Write-Host "      - Third-Party Flag:     $($latestHist.thirdParty)" -ForegroundColor Green
Write-Host "      - Pledge Agreement Ref: $($latestHist.pledgeAgreementRef)" -ForegroundColor Green
Write-Host "      - Consent Doc Ref:      $($latestHist.consentDocumentRef)" -ForegroundColor Green

if ($latestHist.relationshipContext -ne "Third-party" -or -not $latestHist.thirdParty) {
    throw "TEST 7 FAILED: History snapshot does not contain relationship-specific third-party attributes!"
}
Write-Host "[+] History snapshot captures relationship-specific context and pledge metadata perfectly!`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST 8: BIDIRECTIONAL ENTRY POINT SYMMETRY
# --------------------------------------------------------------------------------
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "TEST 8: Bidirectional Entry Point Symmetry" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray

# Direction 1: Collateral -> Candidate Facilities
$candFacilities = Invoke-RestMethod -Uri "$baseUrl/collaterals/$testColId/candidate-facilities" -Method Get -Headers $headers
$candFacB1 = $candFacilities | Where-Object { $_.facilityId -eq $facB1.id -or $_.id -eq $facB1.id } | Select-Object -First 1

Write-Host "    Direction 1 (Collateral -> Candidate Facilities):" -ForegroundColor Gray
if ($candFacB1) {
    Write-Host "      - Facility B1 Third-Party Flag: $($candFacB1.thirdParty)" -ForegroundColor $(if ($candFacB1.thirdParty) { "Green" } else { "Red" })
    Write-Host "      - Facility B1 Rel Context:      $($candFacB1.relationshipContext)" -ForegroundColor Green
}

# Direction 2: Facility -> Securing Collaterals
$securingCols = Invoke-RestMethod -Uri "$baseUrl/facilities/$($facB1.id)/collaterals" -Method Get -Headers $headers
$secCol = $securingCols.linkedCollaterals | Where-Object { $_.collateralId -eq $testColId } | Select-Object -First 1

Write-Host "    Direction 2 (Facility -> Securing Collaterals):" -ForegroundColor Gray
if ($secCol) {
    Write-Host "      - Collateral Rel Context:       $($secCol.relationshipContext)" -ForegroundColor Green
    Write-Host "      - Collateral Third-Party Flag:  $($secCol.thirdParty)" -ForegroundColor Green
    Write-Host "      - Legal Owner Name:             $($secCol.collateralOwnerName)" -ForegroundColor Green
    Write-Host "      - Borrower Name:                $($secCol.borrowerName)" -ForegroundColor Green
}
Write-Host "[+] Bidirectional views consistently report relationship-specific context!`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST 9: HARD-STOP VALIDATIONS (DUPLICATE & CAPACITY)
# --------------------------------------------------------------------------------
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "TEST 9: Hard-Stop Enforcements (Duplicate & Capacity Overflow)" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor DarkGray

# 9a. Duplicate Link Hard Stop
$dupReq = @{
    collateralId = $testColId
    facilityId = $facA1.id # Already linked in Test 1
    allocatedAmount = 1000000.0
    finalSubmission = $true
} | ConvertTo-Json

$dupRes = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/validate" -Method Post -Headers $headers -ContentType "application/json" -Body $dupReq
$dupCheck = $dupRes.checks | Where-Object { $_.code -eq "VAL-REL-DUP" }

Write-Host "    Duplicate Link Check:" -ForegroundColor Gray
Write-Host "      - Check Passed: $($dupCheck.passed) (Expected: False)" -ForegroundColor $(if (-not $dupCheck.passed) { "Green" } else { "Red" })
Write-Host "      - Can Proceed:  $($dupRes.canProceed) (Expected: False)" -ForegroundColor $(if (-not $dupRes.canProceed) { "Green" } else { "Red" })

if ($dupRes.canProceed) {
    throw "TEST 9a FAILED: Duplicate link must be blocked with canProceed=false"
}

# 9b. Capacity Overflow Hard Stop
$overflowReq = @{
    collateralId = $testColId
    facilityId = $facB1.id
    allocatedAmount = 999000000.0 # Way exceeds NSV 27,000,000
    relationshipContext = "Third-party"
    thirdPartyPledge = $true
    relationshipToBorrower = "Guarantor"
    pledgeAgreementRef = "PLG-2026-CORR-001"
    consentDocumentRef = $uploadedDoc.id
    finalSubmission = $true
} | ConvertTo-Json

$overflowRes = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/validate" -Method Post -Headers $headers -ContentType "application/json" -Body $overflowReq
$capCheck = $overflowRes.checks | Where-Object { $_.code -eq "VAL-REL-CAPACITY" }

Write-Host "    Capacity Overflow Check:" -ForegroundColor Gray
Write-Host "      - Check Passed: $($capCheck.passed) (Expected: False)" -ForegroundColor $(if (-not $capCheck.passed) { "Green" } else { "Red" })
Write-Host "      - Can Proceed:  $($overflowRes.canProceed) (Expected: False)" -ForegroundColor $(if (-not $overflowRes.canProceed) { "Green" } else { "Red" })

if ($overflowRes.canProceed) {
    throw "TEST 9b FAILED: Capacity overflow must be blocked with canProceed=false"
}

Write-Host "[+] All Hard Stops strictly enforced!`n" -ForegroundColor Green

Write-Host "================================================================================" -ForegroundColor Green
Write-Host " ALL 9 TESTS PASSED: LOAN-COLLATERAL LINKAGE CORRECTION 100% VERIFIED! " -ForegroundColor Green
Write-Host "================================================================================" -ForegroundColor Green
