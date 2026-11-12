# TEST AREA 3: CROSS-SEGMENT COLLATERALIZATION (CS-001 to CS-007)
$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080/api"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " TEST AREA 3: CROSS-SEGMENT COLLATERALIZATION (CS-001 to CS-007)" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

# 1. Login
$loginBody = @{ username = "cro_user"; password = "password123" } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$headers = @{
    "Authorization" = "Bearer $($loginRes.token)"
    "X-User-Id" = "cro_user"
}

$checkerLoginBody = @{ username = "brmgr_user"; password = "password123" } | ConvertTo-Json
$checkerLoginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $checkerLoginBody
$checkerHeaders = @{
    "Authorization" = "Bearer $($checkerLoginRes.token)"
    "X-User-Id" = "brmgr_user"
}

$state = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$custA = $state.customers | Where-Object { $_.id -eq "cust-test-borrower-1" } | Select-Object -First 1
$custB = $state.customers | Where-Object { $_.id -eq "cust-test-borrower-2" } | Select-Object -First 1
$facA1 = $state.facilities | Where-Object { $_.id -eq "loan-test-fac-1" } | Select-Object -First 1
$facA2 = $state.facilities | Where-Object { $_.id -eq "loan-test-fac-11b" } | Select-Object -First 1
$facB1 = $state.facilities | Where-Object { $_.id -eq "loan-test-fac-2" } | Select-Object -First 1

function Register-TestCollateral($code, $amount, $segment="Corporate Banking", $insurableVal=$null) {
    $payload = @{
        code = $code
        category = "Immovable Properties"
        type = "Buildings - Commercial"
        description = "Commercial Building for $code"
        customerId = $custA.id
        branch = $custA.branch
        owningSegment = $segment
        valuationAmount = $amount
        haircut = 10.0
        currency = "ETB"
        collateralRank = "1st"
        ownerType = "Borrower-owned"
        location = "Bole Road, Addis Ababa"
        locationAddress = "Bole Road, Addis Ababa"
        locationCity = "Addis Ababa"
        locationRegion = "Addis Ababa"
        titleDeedNumber = "TD-" + (Get-Random -Minimum 10000 -Maximum 99999)
        status = "Active"
    }
    if ($insurableVal) {
        $payload["insurableValue"] = $insurableVal
    }
    $json = $payload | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$baseUrl/collateral/register-native?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $json
    $col = $res.collaterals | Where-Object { $_.code -eq $code } | Select-Object -First 1
    return $col
}

function Approve-Pending-Tasks {
    $tasks = Invoke-RestMethod -Uri "$baseUrl/workflow/tasks?status=Pending" -Method Get -Headers $checkerHeaders
    foreach ($t in $tasks) {
        try {
            Invoke-RestMethod -Uri "$baseUrl/workflow/tasks/$($t.id)/approve?userId=brmgr_user&comments=Approved+in+test+suite" -Method Post -Headers $checkerHeaders | Out-Null
        } catch {
            # Ignore if not eligible
        }
    }
}

# --------------------------------------------------------------------------------
# CS-001: Same Segment Collateral
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST CS-001: Same Segment Collateral ---" -ForegroundColor Yellow

$col001Code = "COL-CS001-" + (Get-Random -Minimum 100000 -Maximum 999999)
$c001 = Register-TestCollateral $col001Code 60000000.0 "Corporate Banking"
Write-Host "    Registered C001: $($c001.code) (ID: $($c001.id))"

# Link to Facility A1 (Corporate Banking)
$linkA1Payload = @{
    collateralId = $c001.id
    facilityId = $facA1.id
    allocatedAmount = 15000000.0
    lienDegree = "First-degree"
    notes = "Securing Loan A1 in Corporate Banking"
} | ConvertTo-Json
$linkResA1 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $linkA1Payload

# Link to Facility A2 (Corporate Banking)
$linkA2Payload = @{
    collateralId = $c001.id
    facilityId = $facA2.id
    allocatedAmount = 15000000.0
    lienDegree = "First-degree"
    notes = "Securing Loan A2 in Corporate Banking"
} | ConvertTo-Json
$linkResA2 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $linkA2Payload

Approve-Pending-Tasks

# Verify via /api/dashboard/shared-collaterals
$sharedList = Invoke-RestMethod -Uri "$baseUrl/dashboard/shared-collaterals?userId=brmgr_user" -Method Get -Headers $checkerHeaders
$item001 = $sharedList | Where-Object { $_.collateralId -eq $c001.id -or $_.collateralCode -eq $c001.code } | Select-Object -First 1

if ($null -eq $item001) {
    throw "CS-001 FAILED: Collateral $($c001.code) not found in shared-collaterals query."
}

Write-Host "    Collateral Code:          $($item001.collateralCode)"
Write-Host "    Linked Facilities Count:  $($item001.linkedFacilitiesCount)"
Write-Host "    Shared Across Facilities: $($item001.sharedAcrossFacilities)"
Write-Host "    Shared Across Segments:   $($item001.sharedAcrossSegments)"
Write-Host "    Business Segments:        $(($item001.businessSegments) -join ', ')"

if ($item001.sharedAcrossSegments -eq $true) {
    throw "CS-001 FAILED: Collateral with facilities only in Corporate Banking was incorrectly flagged as sharedAcrossSegments=True."
}
if ($item001.sharedAcrossFacilities -ne $true) {
    throw "CS-001 FAILED: Collateral with 2 facilities in same segment was not identified as sharedAcrossFacilities=True."
}
Write-Host "[+] CS-001 PASSED: Same-segment collateral correctly identified as shared across facilities and NOT cross-segment." -ForegroundColor Green


# --------------------------------------------------------------------------------
# CS-002: Cross-Segment Collateral
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST CS-002: Cross-Segment Collateral ---" -ForegroundColor Yellow

$col002Code = "COL-CS002-" + (Get-Random -Minimum 100000 -Maximum 999999)
$c002 = Register-TestCollateral $col002Code 70000000.0 "Corporate Banking"
Write-Host "    Registered C002: $($c002.code) (ID: $($c002.id))"

# Link to Facility A1 (Corporate Banking)
$linkCS2A1Payload = @{
    collateralId = $c002.id
    facilityId = $facA1.id
    allocatedAmount = 15000000.0
    lienDegree = "First-degree"
    notes = "Securing Loan A1 in Corporate Banking"
} | ConvertTo-Json
$linkCS2A1 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $linkCS2A1Payload

# Upload pledge doc for Customer B facility
$docB1 = @{
    entityType = "Collateral"
    entityId = $c002.id
    docName = "Pledge Agreement for Customer B Facility"
    docType = "Third-Party Pledge Agreement"
    fileName = "pledge_cs002.pdf"
    fileContent = "data:application/pdf;base64,JVBERi0xLjQK"
    expiryDate = "2030-12-31"
} | ConvertTo-Json
$upDocB1 = Invoke-RestMethod -Uri "$baseUrl/documents/upload" -Method Post -Headers $headers -ContentType "application/json" -Body $docB1

# Link to Facility B1 (Commercial Banking - Customer B)
$linkCS2B1Payload = @{
    collateralId = $c002.id
    facilityId = $facB1.id
    allocatedAmount = 15000000.0
    lienDegree = "First-degree"
    relationshipContext = "Third-party"
    thirdPartyPledge = $true
    relationshipToBorrower = "Guarantor"
    pledgeAgreementRef = "PLG-CS002-001"
    consentDocumentRef = $upDocB1.id
    finalSubmission = $true
    notes = "Securing Loan B1 in Commercial Banking as third-party pledge"
} | ConvertTo-Json
$linkCS2B1 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $linkCS2B1Payload

Approve-Pending-Tasks

# Query shared collaterals
$sharedList2 = Invoke-RestMethod -Uri "$baseUrl/dashboard/shared-collaterals?userId=brmgr_user" -Method Get -Headers $checkerHeaders
$item002 = $sharedList2 | Where-Object { $_.collateralId -eq $c002.id -or $_.collateralCode -eq $c002.code } | Select-Object -First 1

if ($null -eq $item002) {
    throw "CS-002 FAILED: Collateral $($c002.code) not found in shared-collaterals query."
}

Write-Host "    Collateral Code:        $($item002.collateralCode)"
Write-Host "    Shared Across Segments: $($item002.sharedAcrossSegments)"
Write-Host "    Business Segments:      $(($item002.businessSegments) -join ', ')"
Write-Host "    Linked Facilities:      $($item002.linkedFacilities.Count) facilities"
foreach ($f in $item002.linkedFacilities) {
    Write-Host "      - FacRef: $($f.facilityRef), Segment: $($f.segment), Allocated: ETB $($f.allocatedSecurity)"
}

if ($item002.sharedAcrossSegments -ne $true) {
    throw "CS-002 FAILED: Collateral linking Corporate Banking and Commercial Banking was not flagged as sharedAcrossSegments=True."
}
$segStr = ($item002.businessSegments) -join ' '
if (-not ($segStr -like "*Corporate*" -and $segStr -like "*Commercial*")) {
    throw "CS-002 FAILED: Both Corporate and Commercial segments must be listed. Actual: $segStr"
}

# Verify original collateral owner is unchanged
$auditCol2 = Invoke-RestMethod -Uri "$baseUrl/collateral/$($c002.id)" -Method Get -Headers $headers
if ($auditCol2.customerId -ne $custA.id) {
    throw "CS-002 FAILED: Collateral master customer changed! Expected $($custA.id), got $($auditCol2.customerId)"
}
Write-Host "[+] CS-002 PASSED: Cross-segment collateral successfully flagged with all linked segments and facilities, and owner remained unchanged." -ForegroundColor Green


# --------------------------------------------------------------------------------
# CS-003: Third-Party Cross-Segment Collateral Distinction
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST CS-003: Third-Party Cross-Segment Collateral ---" -ForegroundColor Yellow

# Master ownership check
Write-Host "    Master Legal Owner: $($auditCol2.customerId) ($($auditCol2.ownerType))"
if ($auditCol2.customerId -ne $custA.id -or $auditCol2.ownerType -ne "Borrower-owned") {
    throw "CS-003 FAILED: Master collateral record must be owned by Customer A."
}

# Check Relationship 1 (Loan A1)
$rel1 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/$($linkCS2A1.id)" -Method Get -Headers $headers
Write-Host "    Relationship 1 (Loan A1): Borrower=$($rel1.borrowerCustomerId), Context=$($rel1.relationshipContext)"
if ($rel1.relationshipContext -ne "Borrower-owned" -or $rel1.thirdParty -eq $true) {
    throw "CS-003 FAILED: Relationship 1 must be Borrower-owned."
}

# Check Relationship 2 (Loan B1)
$rel2 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/$($linkCS2B1.id)" -Method Get -Headers $headers
Write-Host "    Relationship 2 (Loan B1): Borrower=$($rel2.borrowerCustomerId), Context=$($rel2.relationshipContext)"
if ($rel2.relationshipContext -ne "Third-party" -or $rel2.thirdParty -ne $true) {
    throw "CS-003 FAILED: Relationship 2 must be Third-party."
}

Write-Host "[+] CS-003 PASSED: System correctly distinguishes master owner, borrower-owned relationship, and third-party relationship." -ForegroundColor Green


# --------------------------------------------------------------------------------
# CS-004: Cross-Segment Must Not Duplicate Collateral
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST CS-004: Cross-Segment Must Not Duplicate Collateral ---" -ForegroundColor Yellow

$allColls = Invoke-RestMethod -Uri "$baseUrl/collateral" -Method Get -Headers $headers
$col2Matches = $allColls | Where-Object { $_.code -eq $c002.code -or $_.id -eq $c002.id }
$col2MatchCount = ($col2Matches | Measure-Object).Count
Write-Host "    Collateral Master Records with code $($c002.code): $col2MatchCount"

if ($col2MatchCount -ne 1) {
    throw "CS-004 FAILED: Cross-segment linkage created duplicate collateral records! Expected 1, got $col2MatchCount"
}

$col2Links = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?collateralId=$($c002.id)" -Method Get -Headers $headers
$col2LinkCount = ($col2Links | Measure-Object).Count
Write-Host "    Active Relationship Records for $($c002.id): $col2LinkCount"
foreach ($cl in $col2Links) {
    Write-Host "      - Link ID: $($cl.id), Facility: $($cl.facilityId), Status: $($cl.relationshipStatus)"
}

if ($col2LinkCount -lt 2) {
    throw "CS-004 FAILED: Expected at least 2 relationship records, found $col2LinkCount"
}

Write-Host "[+] CS-004 PASSED: Exactly 1 master collateral record maintained with multiple distinct relationship records." -ForegroundColor Green


# --------------------------------------------------------------------------------
# CS-005: Cross-Segment Exposure Calculation
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST CS-005: Cross-Segment Exposure ---" -ForegroundColor Yellow

Write-Host "    Collateral Market Value:      ETB $($item002.marketValue)"
Write-Host "    Total Allocated Security:     ETB $($item002.totalAllocatedSecurity)"
Write-Host "    Aggregate Secured Exposure:   ETB $($item002.aggregateSecuredExposure)"
Write-Host "    Insurance Requirement:        ETB $($item002.insuranceRequirement)"

# Collateral valuation must be single-counted (70,000,000, NOT 140,000,000)
if ($item002.marketValue -ne 70000000.0) {
    throw "CS-005 FAILED: Collateral valuation was double-counted! Value: $($item002.marketValue)"
}

# Total allocated security = 15,000,000 + 15,000,000 = 30,000,000
if ($item002.totalAllocatedSecurity -ne 30000000.0) {
    throw "CS-005 FAILED: Expected totalAllocatedSecurity = 30,000,000, got $($item002.totalAllocatedSecurity)"
}

Write-Host "[+] CS-005 PASSED: Total exposure calculated cleanly without double counting collateral valuation." -ForegroundColor Green


# --------------------------------------------------------------------------------
# CS-006: Remove One Cross-Segment Link
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST CS-006: Remove One Cross-Segment Link ---" -ForegroundColor Yellow

# End relationship to Loan B1 (Commercial Banking)
$endRes = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/$($linkCS2B1.id)/end?reason=Facility+repaid+and+obligation+cleared&userId=brmgr_user" -Method Post -Headers $checkerHeaders

Write-Host "    Ended relationship $($linkCS2B1.id). Status: $($endRes.relationshipStatus), ReleaseStatus: $($endRes.releaseStatus)"

# 1. Master collateral still active
$colAfterEnd = Invoke-RestMethod -Uri "$baseUrl/collateral/$($c002.id)" -Method Get -Headers $headers
if ($colAfterEnd.status -ne "Active") {
    throw "CS-006 FAILED: Master collateral status changed to $($colAfterEnd.status) after ending one link!"
}

# 2. Remaining relationship (Loan A1) still Active
$relA1After = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links/$($linkCS2A1.id)" -Method Get -Headers $headers
if ($relA1After.relationshipStatus -ne "Active") {
    throw "CS-006 FAILED: Unrelated link to Loan A1 was modified! Status: $($relA1After.relationshipStatus)"
}

# 3. Query shared collaterals again: should no longer show as cross-segment
$sharedListAfter = Invoke-RestMethod -Uri "$baseUrl/dashboard/shared-collaterals?userId=brmgr_user" -Method Get -Headers $checkerHeaders
$item002After = $sharedListAfter | Where-Object { $_.collateralId -eq $c002.id -or $_.collateralCode -eq $c002.code } | Select-Object -First 1

if ($null -ne $item002After) {
    Write-Host "    Shared Across Segments After Release: $($item002After.sharedAcrossSegments)"
    Write-Host "    Remaining Segments:                   $(($item002After.businessSegments) -join ', ')"
    if ($item002After.sharedAcrossSegments -eq $true) {
        throw "CS-006 FAILED: Collateral still flagged as sharedAcrossSegments=True after Commercial link ended."
    }
} else {
    Write-Host "    Collateral has only 1 active link left, so it correctly left the multi-facility shared register."
}

Write-Host "[+] CS-006 PASSED: Cross-segment link cleanly removed; master collateral and remaining link intact; cross-segment flag cleared." -ForegroundColor Green


# --------------------------------------------------------------------------------
# CS-007: Cross-Collateralization Must Not Break Insurance Calculation
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST CS-007: Cross-Collateralization Must Not Break Insurance Calculation ---" -ForegroundColor Yellow

$col007Code = "COL-CS007-" + (Get-Random -Minimum 100000 -Maximum 999999)
$c007 = Register-TestCollateral $col007Code 50000000.0 "Corporate Banking" 40000000.0
Write-Host "    Registered C007: $($c007.code) (ID: $($c007.id))"

# Link to A1 (balance 20M)
$l7A1 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body (@{
    collateralId = $c007.id; facilityId = $facA1.id; allocatedAmount = 10000000.0; lienDegree = "First-degree"; notes = "Link A1"
} | ConvertTo-Json)

# Link to A2 (balance 15M)
$l7A2 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body (@{
    collateralId = $c007.id; facilityId = $facA2.id; allocatedAmount = 10000000.0; lienDegree = "First-degree"; notes = "Link A2"
} | ConvertTo-Json)

# Upload pledge doc for Customer B facility on C007
$docB1Col7 = @{
    entityType = "Collateral"
    entityId = $c007.id
    docName = "Pledge Agreement for Customer B Facility on C007"
    docType = "Third-Party Pledge Agreement"
    fileName = "pledge_cs007.pdf"
    fileContent = "data:application/pdf;base64,JVBERi0xLjQK"
    expiryDate = "2030-12-31"
} | ConvertTo-Json
$upDocB1Col7 = Invoke-RestMethod -Uri "$baseUrl/documents/upload" -Method Post -Headers $headers -ContentType "application/json" -Body $docB1Col7

# Link to B1 (balance 30M)
$l7B1 = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body (@{
    collateralId = $c007.id
    facilityId = $facB1.id
    allocatedAmount = 15000000.0
    lienDegree = "First-degree"
    relationshipContext = "Third-party"
    thirdPartyPledge = $true
    relationshipToBorrower = "Guarantor"
    pledgeAgreementRef = "PLG-CS007-001"
    consentDocumentRef = $upDocB1Col7.id
    finalSubmission = $true
    notes = "Link B1"
} | ConvertTo-Json)

Approve-Pending-Tasks

# Evaluate insurance adequacy via API
$adequacy = Invoke-RestMethod -Uri "$baseUrl/insurance-adequacy/$($c007.id)" -Method Get -Headers $headers

Write-Host "    Collateral Code:             $($adequacy.collateralCode)"
Write-Host "    Bank Assessed Insurable Val: ETB $($adequacy.bankAssessedInsurableValue)"
Write-Host "    Contributing Facilities:     $($adequacy.contributingFacilities.Count) facilities"
foreach ($cf in $adequacy.contributingFacilities) {
    Write-Host "      - $($cf.loanReference): Outstanding Balance = ETB $($cf.outstandingBalance)"
}
Write-Host "    Aggregate Secured Exposure:  ETB $($adequacy.aggregateSecuredExposure)"
Write-Host "    Required Coverage Amount:    ETB $($adequacy.requiredCoverageAmount)"
Write-Host "    Overall Adequacy Status:     $($adequacy.overallAdequacyStatus)"

# Expected: 3 facilities aggregated: 20M + 15M + 30M = 65M
if ($adequacy.contributingFacilities.Count -ne 3) {
    throw "CS-007 FAILED: Expected 3 contributing facilities in adequacy calculation, found $($adequacy.contributingFacilities.Count)"
}
if ($adequacy.aggregateSecuredExposure -ne 65000000.0) {
    throw "CS-007 FAILED: Aggregate exposure mismatch! Expected ETB 65,000,000, got ETB $($adequacy.aggregateSecuredExposure)"
}
# Collateral value was not multiplied
if ($adequacy.bankAssessedValue -ne 50000000.0) {
    throw "CS-007 FAILED: Collateral valuation was multiplied! Got $($adequacy.bankAssessedValue)"
}

Write-Host "[+] CS-007 PASSED: Insurance calculation aggregates all 3 facilities without multiplying collateral value or losing facilities." -ForegroundColor Green

Write-Host "`n================================================================================" -ForegroundColor Cyan
Write-Host " ALL TEST AREA 3 (CS-001 to CS-007) TESTS PASSED! " -ForegroundColor Green
Write-Host "================================================================================" -ForegroundColor Cyan
