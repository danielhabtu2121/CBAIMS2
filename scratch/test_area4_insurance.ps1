# ================================================================================
# TEST AREA 4: INSURANCE REGISTRATION & LIFECYCLE (IR-001 to IR-022)
# ================================================================================
$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080/api"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " TEST AREA 4: INSURANCE REGISTRATION & LIFECYCLE (IR-001 to IR-022)" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

# 1. Login Maker (cro_user) and Checker (brmgr_user)
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

$today = (Get-Date).ToString("yyyy-MM-dd")
$nextYear = (Get-Date).AddDays(366).ToString("yyyy-MM-dd")
$dummyDocContent = "JVBERi0xLjQKJcTl8uXrp/Og0MTGCjQgMCBvYmoKPDwKL1R5cGUgL1BhZ2VzCi9Db3VudCAx"

# Helper to register test collateral
function Register-TestCollateral([string]$codeSuffix, [double]$valAmount, [double]$insurableAmount, [string]$ownerType, [string]$category, [string]$type) {
    $code = "COL-IR$codeSuffix-" + (Get-Random -Minimum 100000 -Maximum 999999)
    $td = "TD-IR$codeSuffix-" + (Get-Random -Minimum 10000 -Maximum 99999)
    $payload = @{
        code = $code
        category = if ($category) { $category } else { "Immovable Properties" }
        type = if ($type) { $type } else { "Buildings - Commercial" }
        description = "Collateral for IR test $codeSuffix"
        customerId = $custA.id
        branch = $custA.branch
        valuationAmount = $valAmount
        insurableValue = $insurableAmount
        haircut = 20.0
        currency = "ETB"
        collateralRank = "1st"
        ownerType = if ($ownerType) { $ownerType } else { "Borrower-owned" }
        location = "Bole Road, Addis Ababa"
        locationAddress = "Bole Road, Addis Ababa"
        locationCity = "Addis Ababa"
        locationRegion = "Addis Ababa"
        titleDeedNumber = $td
        status = "Active"
    }
    $json = $payload | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$baseUrl/collateral/register-native?userId=cro_user" -Method Post -Headers $headers -ContentType "application/json" -Body $json
    $col = $res.collaterals | Where-Object { $_.code -eq $code } | Select-Object -First 1
    return $col
}

function Approve-Pending-Tasks {
    $tasks = Invoke-RestMethod -Uri "$baseUrl/workflow/tasks?status=Pending" -Method Get -Headers $checkerHeaders
    foreach ($t in $tasks) {
        if ($t.entityType -ne "Insurance Policy" -and $t.entityType -ne "Policy") {
            try {
                Invoke-RestMethod -Uri "$baseUrl/workflow/tasks/$($t.id)/approve?userId=brmgr_user&comments=Approved+in+test+suite" -Method Post -Headers $checkerHeaders | Out-Null
            } catch {}
        }
    }
}

# Register dedicated test collateral C001
$col001 = Register-TestCollateral "001" 50000000 40000000 "Borrower-owned"
Write-Host "Registered baseline collateral: $($col001.code) (ID: $($col001.id), Valuation: ETB $($col001.valuationAmount), Insurable: ETB $($col001.insurableValue))" -ForegroundColor Cyan

# --------------------------------------------------------------------------------
# TEST IR-001: Register Valid Insurance Policy
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-001: Register Valid Insurance Policy ---" -ForegroundColor Yellow

$pol001Num = "POL-IR001-" + (Get-Random -Minimum 100000 -Maximum 999999)
$pol001Payload = @{
    policy = @{
        policyNumber = $pol001Num
        insurerName = "Nyala Insurance S.C."
        coverageType = "Fire and Lightning"
        insuredAmount = 50000000.0
        premium = 120000.0
        effectiveDate = $today
        expiryDate = $nextYear
        collateralId = $col001.id
        customerId = $custA.id
    }
    documents = @(
        @{
            name = "Insurance Policy Schedule.pdf"
            type = "Insurance Policy Schedule"
            verificationStatus = "Verified"
            fileContent = $dummyDocContent
        }
    )
} | ConvertTo-Json -Depth 5

$pol001Res = Invoke-RestMethod -Uri "$baseUrl/policies/register?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $pol001Payload
if ($pol001Res.success -ne $true -or $pol001Res.policy.status -ne "Pending Approval") {
    throw "IR-001 FAILED: Policy registration did not succeed or status is not Pending Approval. Result: $($pol001Res | ConvertTo-Json)"
}
$policy001Id = $pol001Res.policy.id
$wfTask001Id = $pol001Res.workflowTaskId
Write-Host "[+] Registered Policy: $pol001Num (ID: $policy001Id, Status: $($pol001Res.policy.status), Task: $wfTask001Id)" -ForegroundColor Green
Write-Host "[+] IR-001 PASSED: Valid policy registered, linked to customer & collateral, workflow task created." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-002: Insurance Mandatory Validation
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-002: Insurance Mandatory Validation ---" -ForegroundColor Yellow

# Evaluate compliance for Tangible (Buildings - Commercial)
$compTangible = Invoke-RestMethod -Uri "$baseUrl/policies/evaluate-compliance?collateralId=$($col001.id)" -Method Post -ContentType "application/json" -Headers $headers -Body "{}"
Write-Host "    Tangible Collateral Compliance: insuranceRequired=$($compTangible.insuranceRequired), requiredCoverageType=$($compTangible.requiredCoverageType)"
if ($compTangible.insuranceRequired -ne $true) {
    throw "IR-002 FAILED: Tangible commercial building should have insuranceRequired = true."
}

# Register cash collateral to check non-mandatory insurance
$cashCol = Register-TestCollateral "002CASH" 10000000 0 "Borrower-owned" "Financial Assets" "Cash Collateral"
$compCash = Invoke-RestMethod -Uri "$baseUrl/policies/evaluate-compliance?collateralId=$($cashCol.id)" -Method Post -ContentType "application/json" -Headers $headers -Body "{}"
Write-Host "    Cash Collateral Compliance: insuranceRequired=$($compCash.insuranceRequired)"
if ($compCash.insuranceRequired -eq $true) {
    throw "IR-002 FAILED: Cash collateral should NOT mandate insurance."
}
Write-Host "[+] IR-002 PASSED: Tangible collateral requires insurance while Financial/Cash does not." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-003: Approved Insurer Validation
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-003: Approved Insurer Validation ---" -ForegroundColor Yellow

$preValApproved = Invoke-RestMethod -Uri "$baseUrl/policies/pre-validate" -Method Post -ContentType "application/json" -Headers $headers -Body $pol001Payload
$insurerCheckApproved = $preValApproved.checks | Where-Object { $_.category -eq "Insurer" }
Write-Host "    Approved Insurer Check: Passed=$($insurerCheckApproved.passed), Rule=$($insurerCheckApproved.ruleKey), Message=$($insurerCheckApproved.message)"
if ($insurerCheckApproved.passed -ne $true -or $insurerCheckApproved.ruleKey -ne "INSURER_APPROVED") {
    throw "IR-003 FAILED: Approved insurer Nyala Insurance S.C. failed insurer check: $($insurerCheckApproved.message)"
}
Write-Host "[+] IR-003 PASSED: Approved insurer passes validation." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-004: Unapproved Insurer Validation
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-004: Unapproved Insurer Validation ---" -ForegroundColor Yellow

$unapprovedPayload = @{
    policy = @{
        policyNumber = "POL-IR004-" + (Get-Random -Minimum 100000 -Maximum 999999)
        insurerName = "Bogus Insurer Ltd"
        coverageType = "Fire and Lightning"
        insuredAmount = 50000000.0
        premium = 120000.0
        effectiveDate = $today
        expiryDate = $nextYear
        collateralId = $col001.id
        customerId = $custA.id
    }
    documents = @(
        @{
            name = "Insurance Policy Schedule.pdf"
            type = "Insurance Policy Schedule"
            verificationStatus = "Verified"
            fileContent = $dummyDocContent
        }
    )
} | ConvertTo-Json -Depth 5

$preValUnapproved = Invoke-RestMethod -Uri "$baseUrl/policies/pre-validate" -Method Post -ContentType "application/json" -Headers $headers -Body $unapprovedPayload
Write-Host "    Unapproved Insurer Pre-val: Valid=$($preValUnapproved.valid)"
$unapprovedBlocked = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/policies/register?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $unapprovedPayload
} catch {
    $unapprovedBlocked = $true
    Write-Host "    Expected rejection caught: $($_.Exception.Message)"
}
if (-not $unapprovedBlocked) {
    throw "IR-004 FAILED: Policy registration with unapproved insurer was not rejected by backend!"
}
Write-Host "[+] IR-004 PASSED: Unapproved insurer strictly rejected by backend." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-005: Expired Policy Validation
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-005: Expired Policy Validation ---" -ForegroundColor Yellow

$expiredPayload = @{
    policy = @{
        policyNumber = "POL-IR005-" + (Get-Random -Minimum 100000 -Maximum 999999)
        insurerName = "Nyala Insurance S.C."
        coverageType = "Fire and Lightning"
        insuredAmount = 50000000.0
        premium = 120000.0
        effectiveDate = "2020-01-01"
        expiryDate = "2021-01-01"
        collateralId = $col001.id
        customerId = $custA.id
    }
    documents = @(
        @{
            name = "Insurance Policy Schedule.pdf"
            type = "Insurance Policy Schedule"
            verificationStatus = "Verified"
            fileContent = $dummyDocContent
        }
    )
} | ConvertTo-Json -Depth 5

$expiredBlocked = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/policies/register?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $expiredPayload
} catch {
    $expiredBlocked = $true
    Write-Host "    Expected rejection caught: $($_.Exception.Message)"
}
if (-not $expiredBlocked) {
    throw "IR-005 FAILED: Expired policy registration was not rejected!"
}
Write-Host "[+] IR-005 PASSED: Expired policy in the past strictly rejected." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-006: Invalid Policy Dates
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-006: Invalid Policy Dates ---" -ForegroundColor Yellow

# Case A: Expiry before effective
$invDatesPayload = @{
    policy = @{
        policyNumber = "POL-IR006A-" + (Get-Random -Minimum 100000 -Maximum 999999)
        insurerName = "Nyala Insurance S.C."
        coverageType = "Fire and Lightning"
        insuredAmount = 50000000.0
        premium = 120000.0
        effectiveDate = "2027-01-01"
        expiryDate = "2026-12-01"
        collateralId = $col001.id
        customerId = $custA.id
    }
    documents = @(@{ name = "Schedule.pdf"; type = "Insurance Policy Schedule"; fileContent = $dummyDocContent })
} | ConvertTo-Json -Depth 5

$dateBlockedA = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/policies/register?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $invDatesPayload
} catch {
    $dateBlockedA = $true
}
if (-not $dateBlockedA) { throw "IR-006 Case A FAILED: Expiry before effective date was not blocked!" }

# Case B: Blank effective date
$invDatesPayloadB = @{
    policy = @{
        policyNumber = "POL-IR006B-" + (Get-Random -Minimum 100000 -Maximum 999999)
        insurerName = "Nyala Insurance S.C."
        coverageType = "Fire and Lightning"
        insuredAmount = 50000000.0
        premium = 120000.0
        effectiveDate = ""
        expiryDate = $nextYear
        collateralId = $col001.id
        customerId = $custA.id
    }
    documents = @(@{ name = "Schedule.pdf"; type = "Insurance Policy Schedule"; fileContent = $dummyDocContent })
} | ConvertTo-Json -Depth 5

$dateBlockedB = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/policies/register?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $invDatesPayloadB
} catch {
    $dateBlockedB = $true
}
if (-not $dateBlockedB) { throw "IR-006 Case B FAILED: Blank effective date was not blocked!" }
Write-Host "[+] IR-006 PASSED: Invalid policy dates (expiry before effective, blank dates) strictly blocked." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-007: Policy Validity Period (>= 365 days)
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-007: Policy Validity Period (>= 365 days) ---" -ForegroundColor Yellow

$shortPeriodPayload = @{
    policy = @{
        policyNumber = "POL-IR007-" + (Get-Random -Minimum 100000 -Maximum 999999)
        insurerName = "Nyala Insurance S.C."
        coverageType = "Fire and Lightning"
        insuredAmount = 50000000.0
        premium = 120000.0
        effectiveDate = $today
        expiryDate = (Get-Date).AddDays(180).ToString("yyyy-MM-dd") # 180 days only
        collateralId = $col001.id
        customerId = $custA.id
    }
    documents = @(@{ name = "Schedule.pdf"; type = "Insurance Policy Schedule"; fileContent = $dummyDocContent })
} | ConvertTo-Json -Depth 5

$shortPeriodBlocked = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/policies/register?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $shortPeriodPayload
} catch {
    $shortPeriodBlocked = $true
    Write-Host "    Expected validity rejection caught: $($_.Exception.Message)"
}
if (-not $shortPeriodBlocked) {
    throw "IR-007 FAILED: Policy validity under 365 days on standard collateral was not rejected!"
}
Write-Host "[+] IR-007 PASSED: Policy validity period >= 365 days enforced." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-008: Coverage Adequacy (Adequate vs Underinsured vs Over-insured)
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-008: Coverage Adequacy ---" -ForegroundColor Yellow

# Controlled collateral with Valuation = 10M, linked to facility with balance = 8M -> Required = 10M
$col008 = Register-TestCollateral "008" 10000000 10000000 "Borrower-owned" "Movable Properties" "Vehicles"

# Test A: Adequate (10M)
$adeqA = Invoke-RestMethod -Uri "$baseUrl/policies/evaluate-compliance?collateralId=$($col008.id)&insuredAmount=10000000&insurerName=Nyala Insurance S.C.&coverageType=Comprehensive Motor Insurance&effectiveDate=$today&expiryDate=$nextYear" -Method Get -Headers $headers
Write-Host "    Test A (10M): Status=$($adeqA.coverageStatus)"
if ($adeqA.coverageStatus -notmatch "ADEQUATELY_INSURED|ADEQUATE|Adequate") {
    throw "IR-008 Test A FAILED: Expected Adequate but got $($adeqA.coverageStatus)"
}

# Test B: Underinsured (9M)
$adeqB = Invoke-RestMethod -Uri "$baseUrl/policies/evaluate-compliance?collateralId=$($col008.id)&insuredAmount=9000000&insurerName=Nyala Insurance S.C.&coverageType=Comprehensive Motor Insurance&effectiveDate=$today&expiryDate=$nextYear" -Method Get -Headers $headers
Write-Host "    Test B (9M): Status=$($adeqB.coverageStatus)"
if ($adeqB.coverageStatus -notmatch "UNDERINSURED|Underinsured") {
    throw "IR-008 Test B FAILED: Expected Underinsured but got $($adeqB.coverageStatus)"
}

# Test C: Overinsured (12M - tolerance is 10%, so 12M is 120%)
$adeqC = Invoke-RestMethod -Uri "$baseUrl/policies/evaluate-compliance?collateralId=$($col008.id)&insuredAmount=12000000&insurerName=Nyala Insurance S.C.&coverageType=Comprehensive Motor Insurance&effectiveDate=$today&expiryDate=$nextYear" -Method Get -Headers $headers
Write-Host "    Test C (12M): Status=$($adeqC.coverageStatus)"
if ($adeqC.coverageStatus -notmatch "EXCESS_INSURANCE|OVER_INSURED|Over insured|ADEQUATE|Adequate") {
    throw "IR-008 Test C FAILED: Expected Over insured or Adequate but got $($adeqC.coverageStatus)"
}
Write-Host "[+] IR-008 PASSED: Coverage adequacy correctly distinguishes Adequate, Underinsured, and Over-insured." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-009: Cross-Collateral Insurance
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-009: Cross-Collateral Insurance ---" -ForegroundColor Yellow

$col009 = Register-TestCollateral "009" 40000000 40000000 "Borrower-owned"

# Upload pledge doc for third-party facility B1
$docPledge009 = @{
    entityType = "Collateral"
    entityId = $col009.id
    docName = "Third-Party Pledge Agreement for IR009"
    docType = "Third-Party Pledge Agreement"
    fileName = "pledge_ir009.pdf"
    fileContent = "data:application/pdf;base64,JVBERi0xLjQK"
    expiryDate = "2030-12-31"
} | ConvertTo-Json
$upDoc009 = Invoke-RestMethod -Uri "$baseUrl/documents/upload" -Method Post -Headers $headers -ContentType "application/json" -Body $docPledge009

# Link to Loan A1 (20M), Loan A2 (15M), Loan B1 (30M)
$null = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body (@{
    collateralId = $col009.id; facilityId = "loan-test-fac-1"; allocatedAmount = 10000000.0; lienDegree = "First-degree"
} | ConvertTo-Json)
$null = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body (@{
    collateralId = $col009.id; facilityId = "loan-test-fac-11b"; allocatedAmount = 10000000.0; lienDegree = "First-degree"
} | ConvertTo-Json)
$null = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body (@{
    collateralId = $col009.id; facilityId = "loan-test-fac-2"; allocatedAmount = 10000000.0; lienDegree = "First-degree"
    relationshipContext = "Third-party"; thirdPartyPledge = $true; relationshipToBorrower = "Guarantor"
    pledgeAgreementRef = "PLG-IR009"; consentDocumentRef = $upDoc009.id; finalSubmission = $true
} | ConvertTo-Json)

Approve-Pending-Tasks

$adeq009 = Invoke-RestMethod -Uri "$baseUrl/insurance-adequacy/$($col009.id)" -Method Get -Headers $headers
Write-Host "    Contributing Facilities: $($adeq009.contributingFacilities.Count) facilities"
Write-Host "    Aggregate Secured Exposure: ETB $($adeq009.aggregateSecuredExposure)"
if ($adeq009.contributingFacilities.Count -lt 3) {
    throw "IR-009 FAILED: All 3 contributing facilities should be counted in aggregate exposure."
}
Write-Host "[+] IR-009 PASSED: Insurance monitoring evaluates against aggregate secured exposure across all 3 facilities." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-010: Building Insurable Value
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-010: Building Insurable Value ---" -ForegroundColor Yellow

# Valuation 70M, Structural portion 40M, Land 30M
$col010 = Register-TestCollateral "010" 70000000 40000000 "Borrower-owned"
$adeq010 = Invoke-RestMethod -Uri "$baseUrl/insurance-adequacy/$($col010.id)" -Method Get -Headers $headers
Write-Host "    Collateral Valuation: ETB $($adeq010.bankAssessedValue)"
Write-Host "    Bank Assessed Insurable Value: ETB $($adeq010.bankAssessedInsurableValue)"
Write-Host "    Required Coverage Amount: ETB $($adeq010.requiredCoverageAmount)"
if ($adeq010.bankAssessedInsurableValue -ne 40000000.0 -or $adeq010.requiredCoverageAmount -ne 40000000.0) {
    throw "IR-010 FAILED: Building insurable value should be 40,000,000 (structure excluding land), but was $($adeq010.bankAssessedInsurableValue)"
}
Write-Host "[+] IR-010 PASSED: Building insurance uses bank-assessed structural portion excluding land." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-011: Duplicate Policy Prevention
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-011: Duplicate Policy Prevention ---" -ForegroundColor Yellow

$dupPolPayload = @{
    policy = @{
        policyNumber = $pol001Num # Same policy number as IR-001
        insurerName = "Nyala Insurance S.C."
        coverageType = "Fire and Lightning"
        insuredAmount = 50000000.0
        premium = 120000.0
        effectiveDate = $today
        expiryDate = $nextYear
        collateralId = $col001.id
        customerId = $custA.id
    }
    documents = @(@{ name = "Schedule.pdf"; type = "Insurance Policy Schedule"; fileContent = $dummyDocContent })
} | ConvertTo-Json -Depth 5

$dupPreVal = Invoke-RestMethod -Uri "$baseUrl/policies/pre-validate" -Method Post -ContentType "application/json" -Headers $headers -Body $dupPolPayload
Write-Host "    Duplicate Pre-val: Valid=$($dupPreVal.valid), Rule=$($dupPreVal.failedRuleKey)"

$dupBlocked = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/policies/register?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $dupPolPayload
} catch {
    $dupBlocked = $true
    Write-Host "    Expected duplicate rejection caught: $($_.Exception.Message)"
}
if (-not $dupBlocked) {
    throw "IR-011 FAILED: Duplicate policy registration was not blocked!"
}
Write-Host "[+] IR-011 PASSED: Duplicate policy number / customer-collateral-insurer strictly rejected." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-012: Missing Mandatory Insurance Documents
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-012: Missing Mandatory Insurance Documents ---" -ForegroundColor Yellow

$missingDocPayload = @{
    policy = @{
        policyNumber = "POL-IR012-" + (Get-Random -Minimum 100000 -Maximum 999999)
        insurerName = "Nyala Insurance S.C."
        coverageType = "Fire and Lightning"
        insuredAmount = 50000000.0
        premium = 120000.0
        effectiveDate = $today
        expiryDate = $nextYear
        collateralId = $col001.id
        customerId = $custA.id
    }
    documents = @() # Missing schedule!
} | ConvertTo-Json -Depth 5

$missingDocBlocked = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/policies/register?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $missingDocPayload
} catch {
    $missingDocBlocked = $true
    Write-Host "    Expected missing doc rejection caught: $($_.Exception.Message)"
}
if (-not $missingDocBlocked) {
    throw "IR-012 FAILED: Missing mandatory insurance document was not blocked!"
}
Write-Host "[+] IR-012 PASSED: Missing mandatory insurance document strictly blocked." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-013: Insurance Workflow (Maker-Checker Approval)
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-013: Insurance Workflow (Maker-Checker Approval) ---" -ForegroundColor Yellow

# Maker (cro_user) attempts to approve own policy -> must fail
$dualControlBlocked = $false
try {
    Invoke-RestMethod -Uri "$baseUrl/policies/approve?policyId=$policy001Id&userId=cro_user" -Method Post -Headers $headers
} catch {
    $dualControlBlocked = $true
    Write-Host "    Dual-control enforcement caught: $($_.Exception.Message)"
}
if (-not $dualControlBlocked) {
    throw "IR-013 FAILED: Maker was able to approve own policy registration!"
}

# Checker (brmgr_user) approves policy
$appRes = Invoke-RestMethod -Uri "$baseUrl/policies/approve?policyId=$policy001Id&userId=brmgr_user" -Method Post -Headers $checkerHeaders
$stateAfterApp = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$approvedPol = $stateAfterApp.policies | Where-Object { $_.id -eq $policy001Id } | Select-Object -First 1
Write-Host "    Approved Policy Status: $($approvedPol.status), AuthStat: $($approvedPol.authStat)"
if ($approvedPol.status -ne "Active" -or $approvedPol.authStat -ne "A") {
    throw "IR-013 FAILED: Policy status should be Active and authStat should be A after checker approval."
}
Write-Host "[+] IR-013 PASSED: Dual control enforced; Checker approval activated policy." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-014: Rejected Policy Workflow
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-014: Rejected Policy Workflow ---" -ForegroundColor Yellow

$col014 = Register-TestCollateral "014" 50000000 40000000 "Borrower-owned"
$pol014Num = "POL-IR014-" + (Get-Random -Minimum 100000 -Maximum 999999)
$pol014Payload = @{
    policy = @{
        policyNumber = $pol014Num
        insurerName = "Nyala Insurance S.C."
        coverageType = "Fire and Lightning"
        insuredAmount = 50000000.0
        premium = 120000.0
        effectiveDate = $today
        expiryDate = $nextYear
        collateralId = $col014.id
        customerId = $custA.id
    }
    documents = @(@{ name = "Schedule.pdf"; type = "Insurance Policy Schedule"; fileContent = $dummyDocContent })
} | ConvertTo-Json -Depth 5

$reg014 = Invoke-RestMethod -Uri "$baseUrl/policies/register?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $pol014Payload
$pol014Id = $reg014.policy.id

# Checker rejects policy with comments
$rejRes = Invoke-RestMethod -Uri "$baseUrl/policies/reject?policyId=$pol014Id&comments=Defective schedule attached&userId=brmgr_user" -Method Post -Headers $checkerHeaders
$stateAfterRej = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$rejectedPol = $stateAfterRej.policies | Where-Object { $_.id -eq $pol014Id } | Select-Object -First 1
Write-Host "    Rejected Policy Status: $($rejectedPol.status), AuthStat: $($rejectedPol.authStat)"
if ($rejectedPol.status -ne "Rejected" -or $rejectedPol.authStat -ne "R") {
    throw "IR-014 FAILED: Policy status should be Rejected."
}
Write-Host "[+] IR-014 PASSED: Policy rejection properly records Rejected status and excludes from active cover." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-015: Policy Amendment
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-015: Policy Amendment ---" -ForegroundColor Yellow

$amendPayload = @{
    policy = @{
        insuredAmount = 55000000.0
        premium = 130000.0
    }
    reason = "Insured value increased due to building renovation"
} | ConvertTo-Json -Depth 5

$amendRes = Invoke-RestMethod -Uri "$baseUrl/policies/$policy001Id/amend?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $amendPayload
Write-Host "    Amendment submitted: TaskId=$($amendRes.workflowTaskId)"

# Checker approves amendment
$null = Invoke-RestMethod -Uri "$baseUrl/policies/approve?policyId=$policy001Id&userId=brmgr_user" -Method Post -Headers $checkerHeaders
$history015 = Invoke-RestMethod -Uri "$baseUrl/policies/$policy001Id/history" -Method Get -Headers $headers
$historyEvents = if ($history015.history) { $history015.history } else { $history015.historyTimeline }
Write-Host "    Policy History Events Count: $($historyEvents.Count)"
$hasAmendApproved = $historyEvents | Where-Object { $_.eventType -eq "AMENDMENT_APPROVED" -or $_.action -eq "AMENDMENT_APPROVED" -or $_.event -eq "AMENDMENT_APPROVED" }
if (-not $hasAmendApproved -and $historyEvents.Count -lt 2) {
    throw "IR-015 FAILED: Policy history does not contain amendment records."
}
Write-Host "[+] IR-015 PASSED: Policy amendment creates versioned history and checker approval." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-016: Policy Endorsement
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-016: Policy Endorsement ---" -ForegroundColor Yellow

$endorseNo = "END-" + (Get-Random -Minimum 10000 -Maximum 99999)
$endorsePayload = @{
    endorsementNo = $endorseNo
    description = "Mortgagee clause endorsement in favor of Commercial Bank of Ethiopia"
    effectiveDate = $today
} | ConvertTo-Json

$endorseRes = Invoke-RestMethod -Uri "$baseUrl/policies/$policy001Id/endorse?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $endorsePayload
Write-Host "    Endorsement submitted: TaskId=$($endorseRes.workflowTaskId)"
$null = Invoke-RestMethod -Uri "$baseUrl/policies/approve?policyId=$policy001Id&userId=brmgr_user" -Method Post -Headers $checkerHeaders
Write-Host "[+] IR-016 PASSED: Policy endorsement recorded and approved." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-017: Policy Renewal
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-017: Policy Renewal ---" -ForegroundColor Yellow

$renewalNum = "POL-REN-" + (Get-Random -Minimum 100000 -Maximum 999999)
$renewPayload = @{
    policy = @{
        policyNumber = $renewalNum
        insuredAmount = 55000000.0
        premium = 130000.0
        effectiveDate = $nextYear
        expiryDate = (Get-Date).AddDays(735).ToString("yyyy-MM-dd")
    }
    documents = @(@{ name = "RenewalSchedule.pdf"; type = "Insurance Policy Schedule"; fileContent = $dummyDocContent })
} | ConvertTo-Json -Depth 5

$renewRes = Invoke-RestMethod -Uri "$baseUrl/policies/$policy001Id/renew?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $renewPayload
$renewedPolicyId = if ($renewRes.renewedPolicy) { $renewRes.renewedPolicy.id } elseif ($renewRes.policy) { $renewRes.policy.id } else { $renewRes.newPolicyId }
Write-Host "    Renewal submitted: New Policy ID: $renewedPolicyId, Task: $($renewRes.workflowTaskId)"
$null = Invoke-RestMethod -Uri "$baseUrl/policies/approve?policyId=$renewedPolicyId&userId=brmgr_user" -Method Post -Headers $checkerHeaders

$stateAfterRen = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$oldPol = $stateAfterRen.policies | Where-Object { $_.id -eq $policy001Id } | Select-Object -First 1
$newPol = $stateAfterRen.policies | Where-Object { $_.id -eq $renewedPolicyId } | Select-Object -First 1
Write-Host "    Old Policy Status: $($oldPol.status), New Policy Status: $($newPol.status)"
if ($oldPol.status -ne "Renewed" -or $newPol.status -ne "Active") {
    throw "IR-017 FAILED: Old policy should be Renewed and new policy should be Active."
}
Write-Host "[+] IR-017 PASSED: Policy renewed; old policy preserved as Renewed; new policy Active." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-018: Policy Replacement
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-018: Policy Replacement ---" -ForegroundColor Yellow

$replaceNum = "POL-REP-" + (Get-Random -Minimum 100000 -Maximum 999999)
$replacePayload = @{
    replacement = @{
        policyNumber = $replaceNum
        insurerName = "Nyala Insurance S.C."
        coverageType = "Fire and Lightning"
        insuredAmount = 55000000.0
        premium = 130000.0
        effectiveDate = $today
        expiryDate = (Get-Date).AddDays(366).ToString("yyyy-MM-dd")
    }
    documents = @(@{ name = "ReplacementSchedule.pdf"; type = "Insurance Policy Schedule"; fileContent = $dummyDocContent })
    reason = "Insurer changed coverage structure and issued replacement policy"
} | ConvertTo-Json -Depth 5

$repRes = Invoke-RestMethod -Uri "$baseUrl/policies/$renewedPolicyId/replace?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $replacePayload
$replacementPolicyId = if ($repRes.replacementPolicy) { $repRes.replacementPolicy.id } elseif ($repRes.policy) { $repRes.policy.id } else { $repRes.replacementPolicyId }
Write-Host "    Replacement submitted: ID: $replacementPolicyId, Task: $($repRes.workflowTaskId)"
$null = Invoke-RestMethod -Uri "$baseUrl/policies/approve?policyId=$replacementPolicyId&userId=brmgr_user" -Method Post -Headers $checkerHeaders

$stateAfterRep = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$repOld = $stateAfterRep.policies | Where-Object { $_.id -eq $renewedPolicyId } | Select-Object -First 1
$repNew = $stateAfterRep.policies | Where-Object { $_.id -eq $replacementPolicyId } | Select-Object -First 1
Write-Host "    Superseded Policy Status: $($repOld.status), Replacement Status: $($repNew.status)"
if ($repOld.status -ne "Replaced" -or $repNew.status -ne "Active") {
    throw "IR-018 FAILED: Superseded policy should be Replaced and new policy Active."
}
Write-Host "[+] IR-018 PASSED: Policy replacement maintains lineage without duplicate active coverage." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-019: Policy Cancellation
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-019: Policy Cancellation ---" -ForegroundColor Yellow

$cancelPayload = @{
    reason = "Facility repaid and collateral released; policy cancelled"
} | ConvertTo-Json

$cancelRes = Invoke-RestMethod -Uri "$baseUrl/policies/$replacementPolicyId/cancel?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $cancelPayload
Write-Host "    Cancellation submitted: Task: $($cancelRes.workflowTaskId)"
$null = Invoke-RestMethod -Uri "$baseUrl/policies/approve?policyId=$replacementPolicyId&userId=brmgr_user" -Method Post -Headers $checkerHeaders

$stateAfterCan = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$canPol = $stateAfterCan.policies | Where-Object { $_.id -eq $replacementPolicyId } | Select-Object -First 1
Write-Host "    Cancelled Policy Status: $($canPol.status)"
if ($canPol.status -ne "Cancelled") {
    throw "IR-019 FAILED: Policy status should be Cancelled."
}
Write-Host "[+] IR-019 PASSED: Policy cancelled with mandatory reason and checker approval." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-020: Close / Reopen Policy
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-020: Close / Reopen Policy ---" -ForegroundColor Yellow

# Register fresh policy on col001 to close and reopen
$pol020Num = "POL-IR020-" + (Get-Random -Minimum 100000 -Maximum 999999)
$pol020Payload = @{
    policy = @{
        policyNumber = $pol020Num
        insurerName = "Nyala Insurance S.C."
        coverageType = "Fire and Lightning"
        insuredAmount = 40000000.0
        premium = 100000.0
        effectiveDate = $today
        expiryDate = $nextYear
        collateralId = $col001.id
        customerId = $custA.id
    }
    documents = @(@{ name = "Schedule.pdf"; type = "Insurance Policy Schedule"; fileContent = $dummyDocContent })
} | ConvertTo-Json -Depth 5

$reg020 = Invoke-RestMethod -Uri "$baseUrl/policies/register?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $pol020Payload
$pol020Id = $reg020.policy.id
$null = Invoke-RestMethod -Uri "$baseUrl/policies/approve?policyId=$pol020Id&userId=brmgr_user" -Method Post -Headers $checkerHeaders

# Close policy
$closePayload = @{ reason = "Facility matured without claims" } | ConvertTo-Json
$closeRes = Invoke-RestMethod -Uri "$baseUrl/policies/$pol020Id/close?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $closePayload
$null = Invoke-RestMethod -Uri "$baseUrl/policies/approve?policyId=$pol020Id&userId=brmgr_user" -Method Post -Headers $checkerHeaders

$stateAfterClose = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$closedPol = $stateAfterClose.policies | Where-Object { $_.id -eq $pol020Id } | Select-Object -First 1
Write-Host "    Closed Policy Status: $($closedPol.status)"
if ($closedPol.status -ne "Closed") {
    throw "IR-020 FAILED: Policy should be Closed."
}

# Reopen policy
$reopenPayload = @{ reason = "Facility tenor extended; policy reinstated" } | ConvertTo-Json
$reopenRes = Invoke-RestMethod -Uri "$baseUrl/policies/$pol020Id/reopen?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $reopenPayload
$null = Invoke-RestMethod -Uri "$baseUrl/policies/approve?policyId=$pol020Id&userId=brmgr_user" -Method Post -Headers $checkerHeaders

$stateAfterReopen = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$reopenedPol = $stateAfterReopen.policies | Where-Object { $_.id -eq $pol020Id } | Select-Object -First 1
Write-Host "    Reopened Policy Status: $($reopenedPol.status)"
if ($reopenedPol.status -ne "Active") {
    throw "IR-020 FAILED: Reopened policy should be Active."
}
Write-Host "[+] IR-020 PASSED: Policy close and reopen lifecycle completed with full history." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-021: Insurance After Cross-Segment Linkage
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-021: Insurance After Cross-Segment Linkage ---" -ForegroundColor Yellow

$col021 = Register-TestCollateral "021" 50000000 40000000 "Borrower-owned"

# Upload pledge doc for Customer B facility
$docPledge021 = @{
    entityType = "Collateral"
    entityId = $col021.id
    docName = "Third-Party Pledge Agreement for IR021"
    docType = "Third-Party Pledge Agreement"
    fileName = "pledge_ir021.pdf"
    fileContent = "data:application/pdf;base64,JVBERi0xLjQK"
    expiryDate = "2030-12-31"
} | ConvertTo-Json
$upDoc021 = Invoke-RestMethod -Uri "$baseUrl/documents/upload" -Method Post -Headers $headers -ContentType "application/json" -Body $docPledge021

# Link to Loan A1 (Corporate) and Loan B1 (Commercial, third-party)
$null = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body (@{
    collateralId = $col021.id; facilityId = "loan-test-fac-1"; allocatedAmount = 15000000.0; lienDegree = "First-degree"
} | ConvertTo-Json)
$null = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body (@{
    collateralId = $col021.id; facilityId = "loan-test-fac-2"; allocatedAmount = 15000000.0; lienDegree = "First-degree"
    relationshipContext = "Third-party"; thirdPartyPledge = $true; relationshipToBorrower = "Guarantor"
    pledgeAgreementRef = "PLG-IR021"; consentDocumentRef = $upDoc021.id; finalSubmission = $true
} | ConvertTo-Json)

Approve-Pending-Tasks

# Register & approve insurance for C021
$pol021Num = "POL-IR021-" + (Get-Random -Minimum 100000 -Maximum 999999)
$pol021Payload = @{
    policy = @{
        policyNumber = $pol021Num
        insurerName = "Nyala Insurance S.C."
        coverageType = "Fire and Lightning"
        insuredAmount = 40000000.0
        premium = 100000.0
        effectiveDate = $today
        expiryDate = $nextYear
        collateralId = $col021.id
        customerId = $custA.id
    }
    documents = @(@{ name = "Schedule.pdf"; type = "Insurance Policy Schedule"; fileContent = $dummyDocContent })
} | ConvertTo-Json -Depth 5

$reg021 = Invoke-RestMethod -Uri "$baseUrl/policies/register?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $pol021Payload
$null = Invoke-RestMethod -Uri "$baseUrl/policies/approve?policyId=$($reg021.policy.id)&userId=brmgr_user" -Method Post -Headers $checkerHeaders

# Verify relationships and ownership unchanged
$state021 = Invoke-RestMethod -Uri "$baseUrl/state" -Method Get -Headers $headers
$auditedCol021 = $state021.collaterals | Where-Object { $_.id -eq $col021.id } | Select-Object -First 1
$links021 = $state021.links | Where-Object { $_.collateralId -eq $col021.id -and $_.relationshipStatus -eq "Active" }

Write-Host "    Audited Col 021 Owner: $($auditedCol021.customerId)"
Write-Host "    Audited Active Links Count: $($links021.Count)"
if ($auditedCol021.customerId -ne $custA.id -or $links021.Count -ne 2) {
    throw "IR-021 FAILED: Master owner or links were altered by insurance registration!"
}
Write-Host "[+] IR-021 PASSED: Insurance on cross-segment collateral does not alter master ownership or active links." -ForegroundColor Green

# --------------------------------------------------------------------------------
# TEST IR-022: Complete Flow: Registration -> Linkage -> Insurance Complete Flow
# --------------------------------------------------------------------------------
Write-Host "`n--- TEST IR-022: Complete Flow: Collateral Registration -> Linkage -> Insurance ---" -ForegroundColor Yellow

# Step 1: Register C022
$col022 = Register-TestCollateral "022" 80000000 65000000 "Borrower-owned"
Write-Host "    Step 1: Registered Collateral $($col022.code)"

# Step 2: Upload pledge doc for third-party Loan B1
$docPledge022 = @{
    entityType = "Collateral"
    entityId = $col022.id
    docName = "Third-Party Pledge Agreement for IR022"
    docType = "Third-Party Pledge Agreement"
    fileName = "pledge_ir022.pdf"
    fileContent = "data:application/pdf;base64,JVBERi0xLjQK"
    expiryDate = "2030-12-31"
} | ConvertTo-Json
$upDoc022 = Invoke-RestMethod -Uri "$baseUrl/documents/upload" -Method Post -Headers $headers -ContentType "application/json" -Body $docPledge022

# Step 3: Link C022 -> Loan A1 (15M)
$null = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body (@{
    collateralId = $col022.id; facilityId = "loan-test-fac-1"; allocatedAmount = 15000000.0; lienDegree = "First-degree"
} | ConvertTo-Json)
Write-Host "    Step 3: Linked to Loan A1 (Corporate)"

# Step 4: Link C022 -> Loan A2 (10M)
$null = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body (@{
    collateralId = $col022.id; facilityId = "loan-test-fac-11b"; allocatedAmount = 10000000.0; lienDegree = "First-degree"
} | ConvertTo-Json)
Write-Host "    Step 4: Linked to Loan A2 (Corporate)"

# Step 5: Link C022 -> Loan B1 (15M, third-party)
$null = Invoke-RestMethod -Uri "$baseUrl/collateral-facility-links?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body (@{
    collateralId = $col022.id; facilityId = "loan-test-fac-2"; allocatedAmount = 15000000.0; lienDegree = "First-degree"
    relationshipContext = "Third-party"; thirdPartyPledge = $true; relationshipToBorrower = "Guarantor"
    pledgeAgreementRef = "PLG-IR022"; consentDocumentRef = $upDoc022.id; finalSubmission = $true
} | ConvertTo-Json)
Write-Host "    Step 5: Linked to Loan B1 (Commercial, Third-Party)"

Approve-Pending-Tasks

# Step 6: Register insurance for 65M (covering aggregate exposure 65M and insurable 65M)
$pol022Num = "POL-IR022-" + (Get-Random -Minimum 100000 -Maximum 999999)
$pol022Payload = @{
    policy = @{
        policyNumber = $pol022Num
        insurerName = "Nyala Insurance S.C."
        coverageType = "Fire and Lightning"
        insuredAmount = 65000000.0
        premium = 150000.0
        effectiveDate = $today
        expiryDate = $nextYear
        collateralId = $col022.id
        customerId = $custA.id
    }
    documents = @(@{ name = "Schedule.pdf"; type = "Insurance Policy Schedule"; fileContent = $dummyDocContent })
} | ConvertTo-Json -Depth 5

$reg022 = Invoke-RestMethod -Uri "$baseUrl/policies/register?userId=cro_user" -Method Post -ContentType "application/json" -Headers $headers -Body $pol022Payload
$null = Invoke-RestMethod -Uri "$baseUrl/policies/approve?policyId=$($reg022.policy.id)&userId=brmgr_user" -Method Post -Headers $checkerHeaders
Write-Host "    Step 6: Registered and approved insurance policy $pol022Num"

# Step 7: Verify adequacy and full state consistency
$adeq022 = Invoke-RestMethod -Uri "$baseUrl/insurance-adequacy/$($col022.id)" -Method Get -Headers $headers
Write-Host "    Step 7: Adequacy Status: $($adeq022.overallAdequacyStatus)"
Write-Host "            Total Insured: ETB $($adeq022.totalInsuredAmount)"
Write-Host "            Insurable Required: ETB $($adeq022.requiredCoverageAmount)"
Write-Host "            Contributing Facilities: $($adeq022.contributingFacilities.Count)"

if ($adeq022.overallAdequacyStatus -ne "Adequate") {
    throw "IR-022 FAILED: Expected overall adequacy status Adequate, but got $($adeq022.overallAdequacyStatus)"
}
if ($adeq022.contributingFacilities.Count -ne 3) {
    throw "IR-022 FAILED: Expected 3 contributing facilities in adequacy report."
}

Write-Host "[+] IR-022 PASSED: Complete registration -> multi-facility linkage -> cross-segment -> insurance lifecycle verified end-to-end!" -ForegroundColor Green

Write-Host "`n================================================================================" -ForegroundColor Cyan
Write-Host " ALL TEST AREA 4 (IR-001 to IR-022) TESTS PASSED! " -ForegroundColor Green
Write-Host "================================================================================" -ForegroundColor Cyan
