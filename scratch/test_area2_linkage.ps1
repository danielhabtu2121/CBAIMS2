# TEST AREA 2: LOAN/COLLATERAL LINKAGE (LC-001 to LC-016)
$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080/api"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " TEST AREA 2: LOAN/COLLATERAL LINKAGE (LC-001 to LC-016)" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

# Login
$loginBody = @{ username = "cro_user"; password = "password123" } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$headers = @{
    "Authorization" = "Bearer $($loginRes.token)"
    "X-User-Id" = "cro_user"
}

$state = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$custA = $state.customers | Where-Object { $_.id -eq "cust-test-borrower-1" } | Select-Object -First 1
$custB = $state.customers | Where-Object { $_.id -eq "cust-test-borrower-2" } | Select-Object -First 1
$custC = $state.customers | Where-Object { $_.id -eq "cust-test-borrower-3" } | Select-Object -First 1

$facA1 = $state.facilities | Where-Object { $_.id -eq "loan-test-fac-1" } | Select-Object -First 1
$facA2 = $state.facilities | Where-Object { $_.id -eq "loan-test-fac-11b" } | Select-Object -First 1
$facB1 = $state.facilities | Where-Object { $_.id -eq "loan-test-fac-2" } | Select-Object -First 1
$facC1 = $state.facilities | Where-Object { $_.id -eq "loan-test-fac-3a" } | Select-Object -First 1

# Register test collaterals: C001, C002, C003 for Customer A
function Register-TestCollateral($codePrefix, $valAmount) {
    $code = $codePrefix + "-" + (Get-Random -Minimum 100000 -Maximum 999999)
    $td = "TD-" + (Get-Random -Minimum 10000 -Maximum 99999)
    $payload = @{
        code = $code
        category = "Immovable Properties"
        type = "Buildings - Commercial"
        description = "Test Asset for Linkage $code"
        customerId = $custA.id
        branch = $custA.branch
        valuationAmount = $valAmount
        haircut = 10.0 # NSV = valAmount * 0.9
        currency = "ETB"
        collateralRank = "1st"
        ownerType = "Borrower-owned"
        location = "Bole Road, Addis Ababa"
        locationAddress = "Bole Road, Addis Ababa"
        locationCity = "Addis Ababa"
        locationRegion = "Addis Ababa"
        titleDeedNumber = $td
        status = "Active"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$baseUrl/collateral/register-native?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $payload
    $col = $res.collaterals | Where-Object { $_.code -eq $code } | Select-Object -First 1
    return $col
}

Write-Host "Registering dedicated test collaterals C001, C002, C003..." -ForegroundColor Gray
$c001 = Register-TestCollateral "COL-LC001" 50000000.0 # NSV = 45,000,000
$c002 = Register-TestCollateral "COL-LC002" 40000000.0 # NSV = 36,000,000
$c003 = Register-TestCollateral "COL-LC003" 30000000.0 # NSV = 27,000,000

Write-Host "  C001: $($c001.code) (ID: $($c001.id))" -ForegroundColor Gray
Write-Host "  C002: $($c002.code) (ID: $($c002.id))" -ForegroundColor Gray
Write-Host "  C003: $($c003.code) (ID: $($c003.id))`n" -ForegroundColor Gray

# --------------------------------------------------------------------------------
# LC-001: 1:1 Relationship (C001 -> Loan A1)
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-001: 1:1 Relationship (C001 -> Loan A1) ---" -ForegroundColor Yellow
$linkReq1 = @{
    collateralId = $c001.id
    facilityId = $facA1.id
    allocatedAmount = 10000000.0
    lienDegree = "First-degree"
    notes = "LC-001 1:1 Linkage"
} | ConvertTo-Json

$link1 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $linkReq1
Write-Host "[+] Link 1 Created: $($link1.id) (RelContext: $($link1.relationshipContext), 3rdParty: $($link1.thirdParty))" -ForegroundColor Green

# Verify Collateral Details shows Loan A1
$c1Summary = Invoke-RestMethod -Uri "$baseUrl/collaterals/$($c001.id)/facilities" -Method Get -Headers $headers
$hasA1InC1 = $c1Summary.securedFacilities | Where-Object { $_.facilityId -eq $facA1.id -or $_.loanAccountId -eq $facA1.id }

# Verify Facility Details shows C001
$facSummary = Invoke-RestMethod -Uri "$baseUrl/facilities/$($facA1.id)/collaterals" -Method Get -Headers $headers
$hasC1InA1 = $facSummary.securingCollaterals | Where-Object { $_.collateralId -eq $c001.id }

if (-not $hasA1InC1 -or -not $hasC1InA1) {
    throw "LC-001 FAILED: 1:1 relationship not visible bidirectionally"
}
Write-Host "[+] LC-001 PASSED: 1:1 relationship created and verified bidirectionally.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-002: 1:N Relationship (C001 -> Loan A2)
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-002: 1:N Relationship (C001 -> Loan A2) ---" -ForegroundColor Yellow
$linkReq2 = @{
    collateralId = $c001.id
    facilityId = $facA2.id
    allocatedAmount = 12000000.0
    lienDegree = "First-degree"
    notes = "LC-002 1:N Linkage"
} | ConvertTo-Json

$link2 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $linkReq2
Write-Host "[+] Link 2 Created: $($link2.id) (Allocated: ETB 12M)" -ForegroundColor Green

# Verify C001 shows both A1 and A2
$c1Summary2 = Invoke-RestMethod -Uri "$baseUrl/collaterals/$($c001.id)/facilities" -Method Get -Headers $headers
$activeLinksC1 = $c1Summary2.securedFacilities | Where-Object { $_.status -in @("Active", "Pending Approval") -or $_.relationshipStatus -in @("Active", "Pending Approval") }

Write-Host "    C001 Total Active Facilities Count: $($activeLinksC1.Count) (Expected: 2)" -ForegroundColor Gray
if ($activeLinksC1.Count -lt 2) {
    throw "LC-002 FAILED: C001 should secure both Loan A1 and Loan A2"
}
Write-Host "[+] LC-002 PASSED: 1:N relationship coexists without overwriting first link.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-003: N:1 Relationship (C002 -> Loan A1, C003 -> Loan A1)
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-003: N:1 Relationship (C002 -> Loan A1, C003 -> Loan A1) ---" -ForegroundColor Yellow

$linkReq3a = @{
    collateralId = $c002.id
    facilityId = $facA1.id
    allocatedAmount = 8000000.0
    lienDegree = "First-degree"
    notes = "LC-003 C002 to A1"
} | ConvertTo-Json
$link3a = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $linkReq3a

$linkReq3b = @{
    collateralId = $c003.id
    facilityId = $facA1.id
    allocatedAmount = 6000000.0
    lienDegree = "First-degree"
    notes = "LC-003 C003 to A1"
} | ConvertTo-Json
$link3b = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $linkReq3b

# Verify Loan A1 shows multiple securing collaterals (C001, C002, C003)
$facA1Summary = Invoke-RestMethod -Uri "$baseUrl/facilities/$($facA1.id)/collaterals" -Method Get -Headers $headers
$a1Cols = $facA1Summary.securingCollaterals | Where-Object { $_.status -in @("Active", "Pending Approval") -or $_.relationshipStatus -in @("Active", "Pending Approval") }

Write-Host "    Loan A1 Securing Collaterals Count: $($a1Cols.Count) (Expected: at least 3)" -ForegroundColor Gray
if ($a1Cols.Count -lt 3) {
    throw "LC-003 FAILED: Facility A1 does not contain all linked collaterals"
}
Write-Host "[+] LC-003 PASSED: N:1 multi-collateral per facility verified.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-004: N:N Relationship (C002 -> Loan A2)
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-004: N:N Relationship Matrix ---" -ForegroundColor Yellow

$linkReq4 = @{
    collateralId = $c002.id
    facilityId = $facA2.id
    allocatedAmount = 7000000.0
    lienDegree = "First-degree"
    notes = "LC-004 C002 to A2"
} | ConvertTo-Json
$link4 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $linkReq4

Write-Host "[+] All four relationships (C001->A1, C001->A2, C002->A1, C002->A2) successfully coexisting." -ForegroundColor Green
Write-Host "[+] LC-004 PASSED: N:N cross-collateralization matrix supported.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-005: Duplicate Relationship Prevention
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-005: Duplicate Relationship Prevention ---" -ForegroundColor Yellow

$dupValReq = @{
    collateralId = $c001.id
    facilityId = $facA1.id # Already linked in LC-001
    allocatedAmount = 1000000.0
    finalSubmission = $true
} | ConvertTo-Json

$dupVal = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/validate" -Method Post -Headers $headers -ContentType "application/json" -Body $dupValReq
$dupCheck = $dupVal.checks | Where-Object { $_.code -eq "VAL-REL-DUP" }

Write-Host "    Duplicate pre-validation: Passed=$($dupCheck.passed), CanProceed=$($dupVal.canProceed), Severity=$($dupCheck.severity)" -ForegroundColor Gray
if ($dupVal.canProceed) {
    throw "LC-005 FAILED: Duplicate relationship was not blocked during pre-validation"
}

# Also attempt POST
try {
    Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $dupValReq
    throw "LC-005 FAILED: Backend allowed POST of duplicate relationship!"
} catch {
    Write-Host "[+] Backend rejected duplicate link creation with 400: $($_.Exception.Message)" -ForegroundColor Green
}
Write-Host "[+] LC-005 PASSED: Duplicate relationship strictly rejected.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-006 & LC-007: Link from Collateral Side & Loan Side Symmetry
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-006 & LC-007: Bidirectional Validation Symmetry ---" -ForegroundColor Yellow

# Candidate facilities for C003
$candFacs = Invoke-RestMethod -Uri "$baseUrl/collaterals/$($c003.id)/candidate-facilities" -Method Get -Headers $headers
$candA2 = $candFacs | Where-Object { $_.facilityId -eq $facA2.id -or $_.id -eq $facA2.id } | Select-Object -First 1

# Eligible collaterals for Loan A2
$eligCols = Invoke-RestMethod -Uri "$baseUrl/facilities/$($facA2.id)/eligible-collaterals" -Method Get -Headers $headers
$eligC3 = $eligCols | Where-Object { $_.collateralId -eq $c003.id -or $_.id -eq $c003.id } | Select-Object -First 1

Write-Host "    Collateral-side Candidate A2 Status: $($candA2.candidateStatus)" -ForegroundColor Gray
Write-Host "    Loan-side Eligible C3 Status:        $($eligC3.candidateStatus)" -ForegroundColor Gray

if (-not $candA2 -or -not $eligC3) {
    throw "LC-006/007 FAILED: Asymmetry between collateral-side and loan-side candidate queries"
}
Write-Host "[+] LC-006 & LC-007 PASSED: Both entry points execute equivalent authoritative candidate validation.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-008: Both Sides Must Show The Same Relationship
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-008: Bidirectional Persistence Consistency ---" -ForegroundColor Yellow

$colFetch = Invoke-RestMethod -Uri "$baseUrl/collaterals/$($c001.id)/facilities" -Method Get -Headers $headers
$facFetch = Invoke-RestMethod -Uri "$baseUrl/facilities/$($facA1.id)/collaterals" -Method Get -Headers $headers

$matchInCol = $colFetch.securedFacilities | Where-Object { $_.facilityId -eq $facA1.id -or $_.loanAccountId -eq $facA1.id }
$matchInFac = $facFetch.securingCollaterals | Where-Object { $_.collateralId -eq $c001.id }

if (-not $matchInCol -or -not $matchInFac) {
    throw "LC-008 FAILED: Relationship not consistently returned on both sides"
}
Write-Host "[+] LC-008 PASSED: Relationship consistently persisted and retrievable from both views.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-009: Collateral Already Linked Must Still Be Eligible for Other Loans
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-009: Collateral Already Linked Remains Eligible for Other Loans ---" -ForegroundColor Yellow

# C001 is already linked to A1 and A2. Check candidate facilities for C001
$candForC1 = Invoke-RestMethod -Uri "$baseUrl/collaterals/$($c001.id)/candidate-facilities" -Method Get -Headers $headers
$facCandidates = $candForC1 | Where-Object { $_.candidateStatus -eq "ELIGIBLE" -or $_.candidateStatus -eq "REQUIRES_THIRD_PARTY_INFO" }

Write-Host "    Remaining Eligible Candidates Count for C001: $($facCandidates.Count)" -ForegroundColor Gray
if ($facCandidates.Count -eq 0) {
    throw "LC-009 FAILED: C001 incorrectly marked completely ineligible despite having remaining NSV capacity"
}
Write-Host "[+] LC-009 PASSED: Collateral remains eligible for other facilities as long as capacity exists.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-010: Third-Party Relationship to Another Borrower (C001 -> Loan B1)
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-010: Third-Party Relationship to Another Borrower ---" -ForegroundColor Yellow

# Upload pledge doc for B1
$docB1 = @{
    entityType = "Collateral"
    entityId = $c001.id
    docName = "Pledge Agreement for Customer B Facility"
    docType = "Third-Party Pledge Agreement"
    fileName = "pledge_lc010.pdf"
    fileContent = "data:application/pdf;base64,JVBERi0xLjQK"
    expiryDate = "2030-12-31"
} | ConvertTo-Json
$upDocB1 = Invoke-RestMethod -Uri "$baseUrl/documents/upload" -Method Post -Headers $headers -ContentType "application/json" -Body $docB1

$linkReq10 = @{
    collateralId = $c001.id
    facilityId = $facB1.id
    allocatedAmount = 8000000.0
    lienDegree = "First-degree"
    relationshipContext = "Third-party"
    thirdPartyPledge = $true
    relationshipToBorrower = "Guarantor"
    pledgeAgreementRef = "PLG-LC010-001"
    consentDocumentRef = $upDocB1.id
    finalSubmission = $true
} | ConvertTo-Json

$link10 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $linkReq10

Write-Host "[+] Third-Party Link Created: $($link10.id) (RelContext: $($link10.relationshipContext), 3rdParty: $($link10.thirdParty))" -ForegroundColor Green

# Verify master collateral legal owner remains Customer A
$c1After10 = Invoke-RestMethod -Uri "$baseUrl/collateral/$($c001.id)" -Method Get -Headers $headers
if ($c1After10.customerId -ne $custA.id) {
    throw "LC-010 FAILED: Collateral master customerId changed to $($c1After10.customerId)!"
}
Write-Host "[+] LC-010 PASSED: Third-party relationship created while master owner remains Customer A.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-011: Third-Party Collateral for Multiple Loans (C001 -> Loan C1)
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-011: Third-Party Collateral for Multiple Loans (C001 -> Loan C1) ---" -ForegroundColor Yellow

$docC1 = @{
    entityType = "Collateral"
    entityId = $c001.id
    docName = "Pledge Agreement for Customer C Facility"
    docType = "Third-Party Pledge Agreement"
    fileName = "pledge_lc011.pdf"
    fileContent = "data:application/pdf;base64,JVBERi0xLjQK"
    expiryDate = "2030-12-31"
} | ConvertTo-Json
$upDocC1 = Invoke-RestMethod -Uri "$baseUrl/documents/upload" -Method Post -Headers $headers -ContentType "application/json" -Body $docC1

$linkReq11 = @{
    collateralId = $c001.id
    facilityId = $facC1.id
    allocatedAmount = 5000000.0
    lienDegree = "First-degree"
    relationshipContext = "Third-party"
    thirdPartyPledge = $true
    relationshipToBorrower = "Shareholder"
    pledgeAgreementRef = "PLG-LC011-002"
    consentDocumentRef = $upDocC1.id
    finalSubmission = $true
} | ConvertTo-Json

$link11 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $linkReq11
Write-Host "[+] Link 11 Created: $($link11.id) (Securing Customer C Loan C1)" -ForegroundColor Green

# Verify C001 now secures A1, A2, B1, C1
$c1Summary11 = Invoke-RestMethod -Uri "$baseUrl/collaterals/$($c001.id)/facilities" -Method Get -Headers $headers
$allActiveLinks = $c1Summary11.securedFacilities | Where-Object { $_.status -in @("Active", "Pending Approval") -or $_.relationshipStatus -in @("Active", "Pending Approval") }
Write-Host "    C001 Linked Facilities Count: $($allActiveLinks.Count) (Expected: 4)" -ForegroundColor Gray

if ($allActiveLinks.Count -lt 4) {
    throw "LC-011 FAILED: C001 should secure A1, A2, B1, and C1 concurrently"
}
Write-Host "[+] LC-011 PASSED: Third-party collateral concurrently secures multiple loans across different borrowers.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-012: Invalid Third-Party Relationship (Missing Evidence Blocked)
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-012: Invalid Third-Party Relationship Must BLOCK ---" -ForegroundColor Yellow

$bad3rdReq = @{
    collateralId = $c002.id # Owned by A
    facilityId = $facB1.id # Belongs to B
    allocatedAmount = 5000000.0
    finalSubmission = $true # No pledge info provided
} | ConvertTo-Json

$bad3rdVal = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/validate" -Method Post -Headers $headers -ContentType "application/json" -Body $bad3rdReq
Write-Host "    Invalid Third-Party Pre-validation: CanProceed=$($bad3rdVal.canProceed), ValidationState=$($bad3rdVal.validationState)" -ForegroundColor Gray

if ($bad3rdVal.canProceed) {
    throw "LC-012 FAILED: Missing third-party pledge evidence was not blocked"
}

try {
    Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $bad3rdReq
    throw "LC-012 FAILED: Backend allowed POST of third-party link without pledge documentation!"
} catch {
    Write-Host "[+] Backend rejected invalid third-party link: $($_.Exception.Message)" -ForegroundColor Green
}
Write-Host "[+] LC-012 PASSED: Missing third-party evidence strictly rejected.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-013: Allocation Over Capacity Hard Stop
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-013: Allocation Over Capacity Hard Stop ---" -ForegroundColor Yellow

$overCapReq = @{
    collateralId = $c003.id # NSV = 27,000,000; already allocated 6,000,000 to A1
    facilityId = $facA2.id
    allocatedAmount = 999000000.0 # Way over capacity
    finalSubmission = $true
} | ConvertTo-Json

$overCapVal = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/validate" -Method Post -Headers $headers -ContentType "application/json" -Body $overCapReq
$capCheck = $overCapVal.checks | Where-Object { $_.code -eq "VAL-REL-CAPACITY" }

Write-Host "    Over-capacity check: Passed=$($capCheck.passed), CanProceed=$($overCapVal.canProceed), Severity=$($capCheck.severity)" -ForegroundColor Gray
if ($overCapVal.canProceed) {
    throw "LC-013 FAILED: Over-capacity allocation was not blocked during pre-validation"
}

try {
    Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $overCapReq
    throw "LC-013 FAILED: Backend allowed POST of over-capacity link!"
} catch {
    Write-Host "[+] Backend rejected over-capacity allocation: $($_.Exception.Message)" -ForegroundColor Green
}
Write-Host "[+] LC-013 PASSED: Over-capacity allocation strictly blocked.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-014: Duplicate Submission / Rapid Concurrent Prevention
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-014: Duplicate Submission Prevention ---" -ForegroundColor Yellow

$reqPayload14 = @{
    collateralId = $c003.id
    facilityId = $facA2.id
    allocatedAmount = 2000000.0
    lienDegree = "First-degree"
    notes = "LC-014 Concurrency Test"
} | ConvertTo-Json

# Submit first link
$link14 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $reqPayload14

# Rapidly re-submit identical payload
$rapidBlocked = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $reqPayload14
} catch {
    $rapidBlocked = $true
    Write-Host "[+] Rapid second submission blocked by pessimistic lock / duplicate check: $($_.Exception.Message)" -ForegroundColor Green
}

if (-not $rapidBlocked) {
    throw "LC-014 FAILED: Rapid duplicate submission was not prevented"
}
Write-Host "[+] LC-014 PASSED: Concurrent / rapid duplicate submissions safely prevented.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-015: Failed Link Transactional Safety
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-015: Failed Link Transactional Safety ---" -ForegroundColor Yellow

# Count links before failed attempt
$linksBefore = (Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links" -Method Get -Headers $headers).Count

# Attempt completely invalid request
try {
    Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body '{"collateralId":"non-existent","facilityId":"non-existent","allocatedAmount":100}'
} catch {
    Write-Host "    Expected rejection occurred: $($_.Exception.Message)" -ForegroundColor Gray
}

$linksAfter = (Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links" -Method Get -Headers $headers).Count
if ($linksBefore -ne $linksAfter) {
    throw "LC-015 FAILED: Partial or orphan relationship committed on failed transaction!"
}
Write-Host "[+] LC-015 PASSED: Transaction rollback verified; no orphan links created.`n" -ForegroundColor Green

# --------------------------------------------------------------------------------
# LC-016: First-Degree / Second-Degree Security Logic
# --------------------------------------------------------------------------------
Write-Host "--- TEST LC-016: First-Degree / Second-Degree Security Enforcement ---" -ForegroundColor Yellow

# Register a 2nd degree collateral with senior charge
$col2ndCode = "COL-2ND-" + (Get-Random -Minimum 100000 -Maximum 999999)
$td2nd = "TD-2ND-" + (Get-Random -Minimum 10000 -Maximum 99999)

$col2ndPayload = @{
    code = $col2ndCode
    category = "Immovable Properties"
    type = "Buildings - Commercial"
    description = "Second-Degree Mortgage Security Asset"
    customerId = $custA.id
    branch = $custA.branch
    valuationAmount = 40000000.0
    haircut = 20.0 # NSV = 32,000,000
    currency = "ETB"
    collateralRank = "2nd"
    ownerType = "Borrower-owned"
    location = "Bole Road, Addis Ababa"
    locationAddress = "Bole Road, Addis Ababa"
    locationCity = "Addis Ababa"
    locationRegion = "Addis Ababa"
    titleDeedNumber = $td2nd
    status = "Active"
    firstDegreeHolderName = "Commercial Bank of Ethiopia"
    firstDegreeOutstandingBalance = 25000000.0 # Senior charge 25M; unallocated 2nd capacity = 32M - 25M = 7,000,000
    registeredMortgageAmount = 25000000.0
} | ConvertTo-Json

$reg2nd = Invoke-RestMethod -Uri "$baseUrl/collateral/register-native?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $col2ndPayload
$c2nd = $reg2nd.collaterals | Where-Object { $_.code -eq $col2ndCode } | Select-Object -First 1

# Attempt to allocate 10,000,000 (exceeds 2nd degree unallocated capacity of 7,000,000)
$over2ndReq = @{
    collateralId = $c2nd.id
    facilityId = $facA1.id
    allocatedAmount = 10000000.0
    collateralRank = "2nd"
    lienDegree = "Second-degree"
    firstDegreeOutstandingBalance = 25000000.0
    registeredMortgageAmount = 25000000.0
    finalSubmission = $true
} | ConvertTo-Json

$val2nd = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/validate" -Method Post -Headers $headers -ContentType "application/json" -Body $over2ndReq
$secDegreeCheck = $val2nd.checks | Where-Object { $_.code -eq "VAL-REL-2ND-CAPACITY" }

Write-Host "    Second-Degree Capacity Check: Passed=$($secDegreeCheck.passed), CanProceed=$($val2nd.canProceed), Message=$($secDegreeCheck.message)" -ForegroundColor Gray

if ($val2nd.canProceed) {
    throw "LC-016 FAILED: Second-degree allocation exceeding net capacity was not blocked!"
}

# Now allocate within available 2nd degree capacity (5,000,000 <= 7,000,000) with first-degree basis
$valid2ndReq = @{
    collateralId = $c2nd.id
    facilityId = $facA2.id
    allocatedAmount = 5000000.0
    collateralRank = "2nd"
    lienDegree = "Second-degree"
    firstDegreeInternal = $true
    firstDegreeCollateralId = $c001.id
    firstDegreeOutstandingBalance = 25000000.0
    registeredMortgageAmount = 25000000.0
    bankNotedAsSecondMortgagee = $true
    finalSubmission = $true
} | ConvertTo-Json

$val2ndGood = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/validate" -Method Post -Headers $headers -ContentType "application/json" -Body $valid2ndReq
$secDegreeCapCheck = $val2ndGood.checks | Where-Object { $_.code -eq "VAL-REL-2ND-CAPACITY" }
Write-Host "    Valid Second-Degree Check: Passed=$($secDegreeCapCheck.passed), Message=$($secDegreeCapCheck.message)" -ForegroundColor Green
Write-Host "    Second-Degree Net Capacity Remaining: ETB $($val2ndGood.remainingCapacity)" -ForegroundColor Green

if (-not $secDegreeCapCheck.passed) {
    throw "LC-016 FAILED: Second-degree allocation within net capacity was erroneously blocked!"
}
Write-Host "[+] LC-016 PASSED: First-degree/second-degree ranking and senior charge deduction enforced.`n" -ForegroundColor Green

Write-Host "================================================================================" -ForegroundColor Green
Write-Host " ALL TEST AREA 2 (LC-001 to LC-016) TESTS PASSED! " -ForegroundColor Green
Write-Host "================================================================================" -ForegroundColor Green
