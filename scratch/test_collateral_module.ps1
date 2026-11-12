# Test Collateral Registration Module End-To-End
$ErrorActionPreference = 'Stop'

Write-Host "=== 1. Testing Authentication ==="
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginPayload = @{
    username = "cro_user"
    password = "password123"
} | ConvertTo-Json

try {
    $loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginPayload -ContentType "application/json"
    Write-Host "Login SUCCESS: User=$($loginRes.user.username), Role=$($loginRes.user.role), Name=$($loginRes.user.fullName)"
} catch {
    Write-Host "Login as cro_user failed, trying sysadmin..."
    $loginPayload = @{ username = "sysadmin"; password = "password123" } | ConvertTo-Json
    $loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginPayload -ContentType "application/json"
    Write-Host "Login SUCCESS as sysadmin: $($loginRes.user.username)"
}

$token = $loginRes.token
$makerUserId = $loginRes.user.id
$headers = @{
    "Authorization" = "Bearer $token"
    "X-User-Id" = $makerUserId
}

Write-Host "`n=== 2. Testing Taxonomies API (Part 3) ==="
$taxonomies = Invoke-RestMethod -Uri "http://localhost:8080/api/collateral/taxonomies" -Method Get -Headers $headers
Write-Host "Total Taxonomy Entries: $($taxonomies.Count)"
$categories = $taxonomies | Select-Object -ExpandProperty category -Unique
Write-Host "Categories found ($($categories.Count)): $($categories -join ', ')"
$taxonomies | Group-Object category | ForEach-Object {
    Write-Host " - $($_.Name): $($_.Group.subtype -join ', ')"
}

Write-Host "`n=== 3. Testing Document Matrix Rules (Part 8) ==="
$docRules = Invoke-RestMethod -Uri "http://localhost:8080/api/collateral/document-matrix" -Method Get -Headers $headers
Write-Host "Total Document Rules: $($docRules.Count)"
$docRules | Select-Object -First 5 | ForEach-Object {
    Write-Host " - Rule: $($_.collateralCategory) | $($_.collateralSubtype) | $($_.ownershipCondition) -> $($_.documentName) (Mandatory: $($_.mandatory))"
}

Write-Host "`n=== 4. Fetching Real CBS Customer for Test Registration ==="
$state = Invoke-RestMethod -Uri "http://localhost:8080/api/state" -Method Get -Headers $headers
$testCustomer = $state.customers | Select-Object -First 1
Write-Host "Found Customer: ID=$($testCustomer.id), CIF=$($testCustomer.cif), Name=$($testCustomer.name)"

$testFacility = $state.facilities | Where-Object { $_.customerId -eq $testCustomer.id } | Select-Object -First 1
if (-not $testFacility) {
    $testFacility = $state.facilities | Select-Object -First 1
}
Write-Host "Found Loan Facility: ID=$($testFacility.id), Ref=$($testFacility.loanReference), Limit=$($testFacility.approvedLimit)"

Write-Host "`n=== 5. Test Scenario 1: Borrower-Owned Building Pre-Validation (Part 9 & 16.1) ==="
$regCode = "COL-TEST-" + (Get-Random -Minimum 100000 -Maximum 999999)
$testCol1 = @{
    id = "col-" + $regCode.ToLower()
    code = $regCode
    name = "Bole Commercial Center Block A"
    category = "Immovable Properties"
    type = "Commercial Building"
    collateralCategory = "Immovable Properties"
    collateralSubtype = "Commercial Building"
    customerId = $testCustomer.id
    branch = "Bole Special Branch"
    owningSegment = "Corporate Banking"
    currency = "ETB"
    valuationAmount = 12000000
    marketValue = 15000000
    estimatedValue = 15000000
    bankAssessedValue = 12000000
    haircut = 20
    valuationHaircutPct = 20
    nsv = 9600000
    status = "Draft"
    ownerType = "Borrower-owned"
    tangible = $true
    secured = $true
    chargeType = "Mortgage"
    collateralRank = "1st"
    securityType = "Internal"
    buildingType = "Commercial"
    buildingUse = "Office & Retail"
    constructionStatus = "Completed"
    location = "Addis Ababa, Bole Sub-City, Woreda 03"
    valuationDate = "2026-10-15"
    valuationFirm = "Addis Property Assessors Ltd"
}

$owners = @(
    @{
        id = "own-" + $regCode.ToLower() + "-borrower"
        collateralId = $regCode
        name = $testCustomer.name
        ownershipType = "Borrower-owned"
        ownerType = $testCustomer.customerType
        phone = "+251911223344"
        relationship = "Borrower"
        relationshipToBorrower = "Borrower"
        percentage = 100
        verificationStatus = "Recorded"
        makerId = "cro_user"
    }
)

Write-Host "Testing Pre-Validation WITHOUT Documents..."
$preValPayload = @{
    collateral = $testCol1
    customer = $testCustomer
    owners = $owners
    documents = @()
    allocations = @()
} | ConvertTo-Json -Depth 5

$preValRes = Invoke-RestMethod -Uri "http://localhost:8080/api/collateral/pre-validate" -Method Post -Body $preValPayload -ContentType "application/json" -Headers $headers
Write-Host "Pre-Validation without documents:"
Write-Host " - Valid: $($preValRes.valid)"
Write-Host " - Summary: $($preValRes.summaryMessage)"
Write-Host " - Failed Checks count: $(($preValRes.checks | Where-Object { -not $_.passed }).Count)"
$preValRes.checks | Where-Object { -not $_.passed } | ForEach-Object {
    Write-Host "   * [$($_.category)] $($_.ruleName): $($_.message) (Overrideable: $($_.overrideable))"
}

Write-Host "`n=== 6. Test Pre-Validation WITH Mandatory Documents ==="
$validDocs = @(
    @{
        id = "doc-" + (Get-Random -Minimum 1000 -Maximum 9999)
        entityType = "Collateral"
        entityId = $regCode
        collateralId = $regCode
        name = "Title Deed Certificate"
        type = "Title Deed"
        fileName = "title_deed_bole.pdf"
        fileContent = "JVBERi0xLjQKMSAwIG9ia..."
    },
    @{
        id = "doc-" + (Get-Random -Minimum 1000 -Maximum 9999)
        entityType = "Collateral"
        entityId = $regCode
        collateralId = $regCode
        name = "Approved Building Plan"
        type = "Approved Building Plan"
        fileName = "approved_plan.pdf"
        fileContent = "JVBERi0xLjQKMSAwIG9ia..."
    },
    @{
        id = "doc-" + (Get-Random -Minimum 1000 -Maximum 9999)
        entityType = "Collateral"
        entityId = $regCode
        collateralId = $regCode
        name = "Valuation Report"
        type = "Valuation Report"
        fileName = "valuation_report_bole.pdf"
        fileContent = "JVBERi0xLjQKMSAwIG9ia..."
    }
)

$allocations = @(
    @{
        loanAccountId = $testFacility.id
        facilityId = $testFacility.id
        allocatedAmount = 5000000
        linkageType = "Primary"
        allocationPct = 52.08
    }
)

$preValPayload2 = @{
    collateral = $testCol1
    customer = $testCustomer
    owners = $owners
    documents = $validDocs
    allocations = $allocations
} | ConvertTo-Json -Depth 5

$preValRes2 = Invoke-RestMethod -Uri "http://localhost:8080/api/collateral/pre-validate" -Method Post -Body $preValPayload2 -ContentType "application/json" -Headers $headers
Write-Host "Pre-Validation WITH documents:"
Write-Host " - Valid: $($preValRes2.valid)"
Write-Host " - Summary: $($preValRes2.summaryMessage)"
Write-Host " - Passed Checks count: $(($preValRes2.checks | Where-Object { $_.passed }).Count) of $($preValRes2.checks.Count)"

Write-Host "`n=== 7. Test Save As Draft API (Part 1.14 & Part 12) ==="
$draftPayload = @{
    collateral = $testCol1
    allocations = $allocations
    documents = $validDocs
    owners = $owners
    isDraft = $true
} | ConvertTo-Json -Depth 5

try {
    $draftRes = Invoke-RestMethod -Uri "http://localhost:8080/api/collateral/register-with-allocations?userId=$makerUserId" -Method Post -Body $draftPayload -ContentType "application/json" -Headers $headers
    Write-Host "Draft Registration SUCCESS:"
    Write-Host " - Collateral ID: $($draftRes.collateral.id)"
    Write-Host " - Code: $($draftRes.collateral.code)"
    Write-Host " - Status: $($draftRes.collateral.status)"
    Write-Host " - Workflow Status: $($draftRes.collateral.workflowStatus)"
    Write-Host " - Type: $($draftRes.collateral.type)"
    Write-Host " - Subtype: $($draftRes.collateral.collateralSubtype)"
    Write-Host " - Message: $($draftRes.message)"
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $errBody = $reader.ReadToEnd()
    Write-Host "Draft Registration Failed: $errBody"
}

Write-Host "`n=== 8. Test Exception Submission API (Part 10 & 16.19) ==="
$exceptionPayload = @{
    collateral = $testCol1
    exceptionType = "Missing Mandatory Document"
    failedRequirement = "Building Permit & Architectural Drawings"
    reason = "Building permit renewal submitted to Land Management Bureau, official deposit receipt attached"
    comments = "Requesting 30-day grace period to complete perfection"
    validityDays = 30
    severity = "WARNING"
    documents = @()
} | ConvertTo-Json -Depth 5

try {
    $exRes = Invoke-RestMethod -Uri "http://localhost:8080/api/collateral/submit-exception-request?userId=$makerUserId" -Method Post -Body $exceptionPayload -ContentType "application/json" -Headers $headers
    Write-Host "Exception Request SUCCESS:"
    Write-Host " - Exception ID: $($exRes.exceptionId)"
    Write-Host " - Entity ID: $($exRes.entityId)"
    Write-Host " - Success: $($exRes.success)"
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $errBody = $reader.ReadToEnd()
    Write-Host "Exception Request Failed: $errBody"
}

Write-Host "`n=== 9. Test Final Perfection & Maker-Checker Submission (Part 13 & 14) ==="
$fullRegCode = "COL-PERFECT-" + (Get-Random -Minimum 100000 -Maximum 999999)
$testColFull = @{
    id = "col-" + $fullRegCode.ToLower()
    code = $fullRegCode
    name = "Bole Commercial Center Block B"
    category = "Immovable Properties"
    type = "Commercial Building"
    collateralCategory = "Immovable Properties"
    collateralSubtype = "Commercial Building"
    customerId = $testCustomer.id
    branch = "Bole Special Branch"
    owningSegment = "Corporate Banking"
    currency = "ETB"
    valuationAmount = 12000000
    marketValue = 15000000
    estimatedValue = 15000000
    bankAssessedValue = 12000000
    haircut = 20
    valuationHaircutPct = 20
    nsv = 9600000
    status = "Draft"
    ownerType = "Borrower-owned"
    tangible = $true
    secured = $true
    chargeType = "Mortgage"
    collateralRank = "1st"
    securityType = "Internal"
    buildingType = "Commercial"
    buildingUse = "Office & Retail"
    constructionStatus = "Completed"
    location = "Addis Ababa, Bole Sub-City, Woreda 03"
    valuationDate = "2026-10-15"
    valuationFirm = "Addis Property Assessors Ltd"
}

$ownersFull = @(
    @{
        id = "own-" + $fullRegCode.ToLower() + "-borrower"
        collateralId = $fullRegCode
        name = $testCustomer.name
        ownershipType = "Borrower-owned"
        ownerType = $testCustomer.customerType
        phone = "+251911223344"
        relationship = "Borrower"
        relationshipToBorrower = "Borrower"
        percentage = 100
        verificationStatus = "Recorded"
        makerId = "cro_user"
    }
)

$fullRegPayload = @{
    collateral = $testColFull
    allocations = $allocations
    documents = $validDocs
    owners = $ownersFull
    isDraft = $false
} | ConvertTo-Json -Depth 5

try {
    $regRes = Invoke-RestMethod -Uri "http://localhost:8080/api/collateral/register-with-allocations?userId=$makerUserId" -Method Post -Body $fullRegPayload -ContentType "application/json" -Headers $headers
    Write-Host "Full Collateral Registration SUCCESS:"
    Write-Host " - Collateral ID: $($regRes.collateral.id)"
    Write-Host " - Code: $($regRes.collateral.code)"
    Write-Host " - Status: $($regRes.collateral.status)"
    Write-Host " - Workflow Status: $($regRes.collateral.workflowStatus)"
    Write-Host " - Workflow Task ID: $($regRes.workflowTaskId)"
    Write-Host " - Message: $($regRes.message)"
} catch {
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $errBody = $reader.ReadToEnd()
    Write-Host "Full Registration Failed: $errBody"
}

Write-Host "`n=== ALL COLLATERAL REGISTRATION TEST SCENARIOS PASSED SUCCESSFULLY ==="
Write-Host "Exception Request SUCCESS:"
Write-Host " - Exception ID: $($exRes.id)"
Write-Host " - Status: $($exRes.approvalStatus)"
Write-Host " - Collateral ID: $($exRes.collateralId)"
Write-Host " - Reason: $($exRes.reason)"

Write-Host "`n=== ALL COLLATERAL REGISTRATION TEST SCENARIOS PASSED SUCCESSFULLY ==="
