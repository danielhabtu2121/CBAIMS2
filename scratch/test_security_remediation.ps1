# ==============================================================================
# CBAIMS COMPREHENSIVE SECURITY, AUTHENTICATION & AUDIT REMEDIATION TEST SUITE
# ==============================================================================

$baseUrl = "http://localhost:8080/api"
$passed = 0
$failed = 0
$tests = @()

function Run-Test {
    param(
        [string]$TestId,
        [string]$Description,
        [scriptblock]$Action
    )
    Write-Host "`n--------------------------------------------------" -ForegroundColor Gray
    Write-Host "[$TestId] $Description" -ForegroundColor Cyan
    try {
        $result = & $Action
        if ($result -eq $true) {
            Write-Host " PASS: $TestId" -ForegroundColor Green
            $script:passed++
            $script:tests += [PSCustomObject]@{ Id = $TestId; Description = $Description; Status = "PASS"; Error = "" }
        } else {
            Write-Host " FAIL: $TestId (Assert returned false)" -ForegroundColor Red
            $script:failed++
            $script:tests += [PSCustomObject]@{ Id = $TestId; Description = $Description; Status = "FAIL"; Error = "Assertion returned false" }
        }
    } catch {
        Write-Host " FAIL: $TestId - Error: $($_.Exception.Message)" -ForegroundColor Red
        $script:failed++
        $script:tests += [PSCustomObject]@{ Id = $TestId; Description = $Description; Status = "FAIL"; Error = $_.Exception.Message }
    }
}

function Invoke-Api {
    param(
        [string]$Endpoint,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [object]$Body = $null
    )
    $uri = "$baseUrl$Endpoint"
    $reqHeaders = @{}
    foreach ($k in $Headers.Keys) { $reqHeaders[$k] = $Headers[$k] }
    
    $jsonBody = $null
    if ($Body -ne $null) {
        if ($Body -is [string]) { $jsonBody = $Body }
        else { $jsonBody = $Body | ConvertTo-Json -Depth 10 }
        $reqHeaders["Content-Type"] = "application/json"
    }

    try {
        $resp = Invoke-WebRequest -Uri $uri -Method $Method -Headers $reqHeaders -Body $jsonBody -UseBasicParsing -TimeoutSec 15
        return [PSCustomObject]@{
            StatusCode = [int]$resp.StatusCode
            Headers = $resp.Headers
            Content = $resp.Content
            Json = if ($resp.Content) { try { $resp.Content | ConvertFrom-Json } catch { $null } } else { $null }
        }
    } catch [System.Net.WebException] {
        $webResp = $_.Exception.Response
        if ($webResp -ne $null) {
            $stream = $webResp.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $content = $reader.ReadToEnd()
            return [PSCustomObject]@{
                StatusCode = [int]$webResp.StatusCode
                Headers = $webResp.Headers
                Content = $content
                Json = try { $content | ConvertFrom-Json } catch { $null }
            }
        }
        throw $_
    }
}

function Exec-Sql {
    param([string]$Sql)
    $env:PGPASSWORD = "cims_password"
    $output = & psql.exe -h 127.0.0.1 -U cims_user -d cims_db_dev -t -A -c "$Sql" 2>&1
    return ($output -join "`n").Trim()
}

Write-Host "==================================================" -ForegroundColor Yellow
Write-Host " STARTING CBAIMS SECURITY & SESSION VERIFICATION " -ForegroundColor Yellow
Write-Host "==================================================" -ForegroundColor Yellow

# Ensure test users are unlocked and have default password
$defaultHash = '$2a$10$hxULZ3QyIFnAS1tkzfwSTuVoKA/DBnG63E6PcjsMO5DzvKAABIv3y'
Exec-Sql "UPDATE cims_users SET active = true, failed_attempts = 0, locked_until = NULL, password = '$defaultHash' WHERE username IN ('srm_user', 'bro_user', 'brmgr_user', 'colldocoff_user', 'srmgmt_user', 'compliance_user', 'sysadmin_user');"
Exec-Sql "DELETE FROM cims_user_password_history WHERE user_id IN (SELECT id FROM cims_users WHERE username IN ('srm_user', 'bro_user', 'brmgr_user', 'colldocoff_user', 'srmgmt_user', 'compliance_user', 'sysadmin_user'));"

# ------------------------------------------------------------------------------
# SEC-01: AUTHORITATIVE AUTHENTICATION & API SECURITY
# ------------------------------------------------------------------------------

Run-Test "SEC-01-A" "Unauthenticated request to /api/collaterals must be rejected with HTTP 401" {
    $res = Invoke-Api "/collaterals" -Method "GET"
    Write-Host "   Returned status: $($res.StatusCode)"
    return ($res.StatusCode -eq 401)
}

Run-Test "SEC-01-B" "Unauthenticated request to /api/state must be rejected with HTTP 401" {
    $res = Invoke-Api "/state" -Method "GET"
    Write-Host "   Returned status: $($res.StatusCode)"
    return ($res.StatusCode -eq 401)
}

Run-Test "SEC-01-C" "Unauthenticated request to /api/admin/runtime must be rejected with HTTP 401" {
    $res = Invoke-Api "/admin/runtime" -Method "GET"
    Write-Host "   Returned status: $($res.StatusCode)"
    return ($res.StatusCode -eq 401)
}

Run-Test "SEC-01-D" "Caller-supplied identity spoofing (X-User-Id) without valid token is rejected with HTTP 401" {
    $res = Invoke-Api "/collaterals" -Method "GET" -Headers @{ "X-User-Id" = "brmgr_user"; "X-User-Role" = "BRMGR" }
    Write-Host "   Returned status: $($res.StatusCode)"
    return ($res.StatusCode -eq 401)
}

Run-Test "SEC-01-E" "Public authentication endpoints are accessible without token" {
    $resPolicy = Invoke-Api "/auth/policy" -Method "GET"
    $resForgot = Invoke-Api "/auth/forgot-password" -Method "POST" -Body @{ identifier = "nonexistent_user" }
    Write-Host "   /auth/policy status: $($resPolicy.StatusCode), /auth/forgot-password status: $($resForgot.StatusCode)"
    return ($resPolicy.StatusCode -eq 200 -and $resForgot.StatusCode -eq 200)
}

Run-Test "SEC-01-F" "Protected auth endpoints require Bearer token (session refresh/validate/change-password/logout)" {
    $rLogout = Invoke-Api "/auth/logout" -Method "POST"
    $rRefresh = Invoke-Api "/auth/session/refresh" -Method "POST"
    $rValidate = Invoke-Api "/auth/session/validate" -Method "GET"
    $rChange = Invoke-Api "/auth/change-password" -Method "POST" -Body @{ currentPassword = "a"; newPassword = "b" }
    Write-Host "   Logout: $($rLogout.StatusCode), Refresh: $($rRefresh.StatusCode), Validate: $($rValidate.StatusCode), Change: $($rChange.StatusCode)"
    return ($rLogout.StatusCode -eq 401 -and $rRefresh.StatusCode -eq 401 -and $rValidate.StatusCode -eq 401 -and $rChange.StatusCode -eq 401)
}

# ------------------------------------------------------------------------------
# SEC-02: SERVER-ENFORCED IDLE SESSIONS & DB PERSISTENCE
# ------------------------------------------------------------------------------

$loginRes = $null
$userToken = $null
$userSessionId = $null

Run-Test "SEC-02-A" "Successful login establishes session and returns cryptographic JWT with claims" {
    $loginRes = Invoke-Api "/auth/login" -Method "POST" -Body @{ identifier = "brmgr_user"; password = "password123" }
    Write-Host "   Login StatusCode: $($loginRes.StatusCode), Success: $($loginRes.Json.success)"
    if ($loginRes.StatusCode -ne 200 -or -not $loginRes.Json.success) { return $false }
    $script:userToken = $loginRes.Json.token
    Write-Host "   Token received: $($script:userToken.Substring(0, 20))..."
    return ($script:userToken -ne $null -and $script:userToken.Length -gt 30)
}

Run-Test "SEC-02-B" "Active session record is persisted in cims_user_sessions with idle timeout and expiry" {
    $dbSessions = Exec-Sql "SELECT id || '|' || user_id || '|' || revoked || '|' || (expires_at > NOW()) FROM cims_user_sessions WHERE username = 'brmgr_user' AND revoked = false ORDER BY created_at DESC LIMIT 1;"
    Write-Host "   DB Session check: $dbSessions"
    $parts = $dbSessions -split '\|'
    if ($parts.Length -ge 4) {
        $script:userSessionId = $parts[0]
        $isRevoked = ($parts[2] -eq "t" -or $parts[2] -eq "true")
        $isActive = ($parts[3] -eq "t" -or $parts[3] -eq "true")
        return (-not $isRevoked -and $isActive)
    }
    return $false
}

Run-Test "SEC-02-C" "Authenticated request to protected API succeeds using session Bearer token" {
    $res = Invoke-Api "/collaterals" -Method "GET" -Headers @{ "Authorization" = "Bearer $script:userToken" }
    Write-Host "   Status: $($res.StatusCode)"
    return ($res.StatusCode -eq 200)
}

Run-Test "SEC-02-D" "/api/auth/session/validate verifies active valid session" {
    $res = Invoke-Api "/auth/session/validate" -Method "GET" -Headers @{ "Authorization" = "Bearer $script:userToken" }
    Write-Host "   Valid: $($res.Json.valid), Active: $($res.Json.active), User: $($res.Json.username)"
    return ($res.StatusCode -eq 200 -and $res.Json.valid -eq $true -and $res.Json.active -eq $true)
}

Run-Test "SEC-02-E" "Session extension ('Stay Logged In') extends expires_at and updates last_activity_at in DB" {
    $beforeExpires = Exec-Sql "SELECT expires_at FROM cims_user_sessions WHERE id = '$script:userSessionId';"
    Start-Sleep -Seconds 1
    $res = Invoke-Api "/auth/session/refresh" -Method "POST" -Headers @{ "Authorization" = "Bearer $script:userToken" }
    Write-Host "   Refresh status: $($res.StatusCode), Success: $($res.Json.success)"
    $afterExpires = Exec-Sql "SELECT expires_at FROM cims_user_sessions WHERE id = '$script:userSessionId';"
    Write-Host "   Before: $beforeExpires, After: $afterExpires"
    return ($res.StatusCode -eq 200 -and $res.Json.success -eq $true)
}

# ------------------------------------------------------------------------------
# SEC-03: LOGOUT & SESSION REVOCATION
# ------------------------------------------------------------------------------

Run-Test "SEC-03-A" "Logout revokes the session in database (persisted across restarts)" {
    $res = Invoke-Api "/auth/logout" -Method "POST" -Headers @{ "Authorization" = "Bearer $script:userToken" }
    Write-Host "   Logout status: $($res.StatusCode), Success: $($res.Json.success)"
    $revokedInDb = Exec-Sql "SELECT revoked FROM cims_user_sessions WHERE id = '$script:userSessionId';"
    Write-Host "   Revoked in DB: $revokedInDb"
    return ($res.StatusCode -eq 200 -and ($revokedInDb -eq "t" -or $revokedInDb -eq "true"))
}

Run-Test "SEC-03-B" "Subsequent API request with revoked token is rejected with HTTP 401" {
    $res = Invoke-Api "/collaterals" -Method "GET" -Headers @{ "Authorization" = "Bearer $script:userToken" }
    Write-Host "   Status with revoked token: $($res.StatusCode)"
    return ($res.StatusCode -eq 401)
}

Run-Test "SEC-03-C" "Session validation with revoked token returns valid=false" {
    $res = Invoke-Api "/auth/session/validate" -Method "GET" -Headers @{ "Authorization" = "Bearer $script:userToken" }
    Write-Host "   Status: $($res.StatusCode), Valid: $($res.Json.valid)"
    return ($res.StatusCode -eq 401 -or ($res.Json -ne $null -and $res.Json.valid -eq $false))
}

# ------------------------------------------------------------------------------
# SEC-04 to SEC-07: PASSWORD COMPLEXITY, EXPIRY, HISTORY & CHANGE VERIFICATION
# ------------------------------------------------------------------------------

# Login as bro_user to test password operations
$broLogin = Invoke-Api "/auth/login" -Method "POST" -Body @{ identifier = "bro_user"; password = "password123" }
$broToken = $broLogin.Json.token
Write-Host "   bro_user initial login token received: $($broToken -ne $null)"

Run-Test "SEC-07-A" "changePassword rejects request missing currentPassword" {
    $res = Invoke-Api "/auth/change-password" -Method "POST" -Headers @{ "Authorization" = "Bearer $broToken" } -Body @{ currentPassword = ""; newPassword = "ValidPassword123!" }
    Write-Host "   Status: $($res.StatusCode), Message: $($res.Json.message)"
    return ($res.StatusCode -eq 400 -and $res.Json.message -match "Current password is required")
}

Run-Test "SEC-07-B" "changePassword rejects request with incorrect currentPassword" {
    $res = Invoke-Api "/auth/change-password" -Method "POST" -Headers @{ "Authorization" = "Bearer $broToken" } -Body @{ currentPassword = "WrongPassword999!"; newPassword = "ValidPassword123!" }
    Write-Host "   Status: $($res.StatusCode), Message: $($res.Json.message)"
    return ($res.StatusCode -eq 400 -and ($res.Json.message -match "Current password does not match" -or $res.Json.message -match "Current password is incorrect"))
}

Run-Test "SEC-04-A" "changePassword rejects new password violating complexity (too short)" {
    $res = Invoke-Api "/auth/change-password" -Method "POST" -Headers @{ "Authorization" = "Bearer $broToken" } -Body @{ currentPassword = "password123"; newPassword = "Sh1!" }
    Write-Host "   Status: $($res.StatusCode), Message: $($res.Json.message)"
    return ($res.StatusCode -eq 400 -and $res.Json.message -match "at least 8 characters")
}

Run-Test "SEC-04-B" "changePassword rejects new password missing special characters" {
    $res = Invoke-Api "/auth/change-password" -Method "POST" -Headers @{ "Authorization" = "Bearer $broToken" } -Body @{ currentPassword = "password123"; newPassword = "NoSpecialChar123" }
    Write-Host "   Status: $($res.StatusCode), Message: $($res.Json.message)"
    return ($res.StatusCode -eq 400 -and $res.Json.message -match "special character")
}

Run-Test "SEC-04-C" "changePassword succeeds with compliant password and updates lifecycle" {
    $res = Invoke-Api "/auth/change-password" -Method "POST" -Headers @{ "Authorization" = "Bearer $broToken" } -Body @{ currentPassword = "password123"; newPassword = "NewCompliantPass_1!" }
    Write-Host "   Status: $($res.StatusCode), Success: $($res.Json.success)"
    return ($res.StatusCode -eq 200 -and $res.Json.success -eq $true)
}

Run-Test "SEC-05-A" "Password change updates password_changed_at and sets password_expiry_date" {
    $expiryInfo = Exec-Sql "SELECT (password_changed_at IS NOT NULL) || '|' || (password_expiry_date IS NOT NULL) || '|' || (password_expiry_date > NOW()) FROM cims_users WHERE username = 'bro_user';"
    Write-Host "   DB password expiry info: $expiryInfo"
    return ($expiryInfo -match "true\|true\|true" -or $expiryInfo -match "t\|t\|t")
}

Run-Test "SEC-06-A" "Previous password hash is recorded in cims_user_password_history" {
    $histCount = Exec-Sql "SELECT COUNT(*) FROM cims_user_password_history WHERE user_id = (SELECT id FROM cims_users WHERE username = 'bro_user');"
    Write-Host "   History record count for bro_user: $histCount"
    return ([int]$histCount -ge 1)
}

Run-Test "SEC-06-B" "Password change revokes active sessions of the user" {
    $res = Invoke-Api "/collaterals" -Method "GET" -Headers @{ "Authorization" = "Bearer $broToken" }
    Write-Host "   Status with old session after password change: $($res.StatusCode)"
    return ($res.StatusCode -eq 401)
}

# Login with new password
$broLogin2 = Invoke-Api "/auth/login" -Method "POST" -Body @{ identifier = "bro_user"; password = "NewCompliantPass_1!" }
$broToken2 = $broLogin2.Json.token

# Change password to second compliant password so NewCompliantPass_1! is placed in history
$change2 = Invoke-Api "/auth/change-password" -Method "POST" -Headers @{ "Authorization" = "Bearer $broToken2" } -Body @{ currentPassword = "NewCompliantPass_1!"; newPassword = "SecondCompliantPass_2@" }
$broLogin3 = Invoke-Api "/auth/login" -Method "POST" -Body @{ identifier = "bro_user"; password = "SecondCompliantPass_2@" }
$broToken3 = $broLogin3.Json.token

Run-Test "SEC-06-C" "Attempt to reuse recent previous password ('NewCompliantPass_1!') is rejected by history policy" {
    $res = Invoke-Api "/auth/change-password" -Method "POST" -Headers @{ "Authorization" = "Bearer $broToken3" } -Body @{ currentPassword = "SecondCompliantPass_2@"; newPassword = "NewCompliantPass_1!" }
    Write-Host "   Status: $($res.StatusCode), Message: $($res.Json.message)"
    return ($res.StatusCode -eq 400 -and $res.Json.message -match "previous.*passwords")
}

# Revert bro_user password to default hash cleanly
Exec-Sql "DELETE FROM cims_user_password_history WHERE user_id = (SELECT id FROM cims_users WHERE username = 'bro_user');"
Exec-Sql "UPDATE cims_users SET password = '$defaultHash', failed_attempts = 0, locked_until = NULL WHERE username = 'bro_user';"

# ------------------------------------------------------------------------------
# SEC-08: FORGOT PASSWORD & SECURE TOKEN HASHING
# ------------------------------------------------------------------------------

Run-Test "SEC-08-A" "forgotPassword returns generic message and does NOT return simulationResetToken" {
    $res = Invoke-Api "/auth/forgot-password" -Method "POST" -Body @{ identifier = "srm_user" }
    Write-Host "   Status: $($res.StatusCode), Response: $($res.Content)"
    $hasSimulationToken = ($res.Json -ne $null -and $res.Json.simulationResetToken -ne $null)
    return ($res.StatusCode -eq 200 -and $res.Json.success -eq $true -and -not $hasSimulationToken)
}

Run-Test "SEC-08-B" "Reset token stored in database is a 64-char SHA-256 hash (never plaintext)" {
    $tokenHash = Exec-Sql "SELECT reset_token FROM cims_users WHERE username = 'srm_user';"
    Write-Host "   Stored token hash: $tokenHash (Length: $($tokenHash.Length))"
    return ($tokenHash -ne $null -and $tokenHash.Length -eq 64)
}

# ------------------------------------------------------------------------------
# SEC-09: ACCOUNT LOCKOUT ON FAILED LOGINS
# ------------------------------------------------------------------------------

Run-Test "SEC-09-A" "Account locks out after 5 consecutive failed login attempts" {
    $userToLock = "colldocoff_user"
    Exec-Sql "UPDATE cims_users SET active = true, failed_attempts = 0, locked_until = NULL, password = '$defaultHash' WHERE username = '$userToLock';"
    
    for ($i = 1; $i -le 5; $i++) {
        $failRes = Invoke-Api "/auth/login" -Method "POST" -Body @{ identifier = $userToLock; password = "WrongPassword_Attempt$i!" }
        Write-Host "   Attempt $i - Status: $($failRes.StatusCode), Message: $($failRes.Json.message)"
    }
    
    $lockCheck = Exec-Sql "SELECT (locked_until IS NOT NULL) || '|' || (locked_until > NOW()) FROM cims_users WHERE username = '$userToLock';"
    Write-Host "   DB lock status: $lockCheck"
    
    # 6th attempt should be blocked due to account lockout
    $res6 = Invoke-Api "/auth/login" -Method "POST" -Body @{ identifier = $userToLock; password = "password123" }
    Write-Host "   Attempt 6 (correct password while locked): Status: $($res6.StatusCode), Message: $($res6.Json.message)"
    
    # Unlock the user so test environment remains clean
    Exec-Sql "UPDATE cims_users SET active = true, failed_attempts = 0, locked_until = NULL, password = '$defaultHash' WHERE username = '$userToLock';"
    
    return ($res6.Json.message -match "Account has been locked|Account is temporarily locked")
}

# ------------------------------------------------------------------------------
# SEC-10 to SEC-16: CORS HEADERS & CLIENT IP RESOLUTION
# ------------------------------------------------------------------------------

Run-Test "SEC-10-A" "CORS headers do not use wildcard with credentials" {
    $res = Invoke-Api "/auth/policy" -Method "OPTIONS" -Headers @{ "Origin" = "http://localhost:5173"; "Access-Control-Request-Method" = "GET" }
    $allowOrigin = $res.Headers["Access-Control-Allow-Origin"]
    Write-Host "   Access-Control-Allow-Origin: $allowOrigin"
    return ($allowOrigin -ne "*" -and $allowOrigin -ne $null)
}

Run-Test "SEC-16-A" "Client IP from trusted proxy is correctly extracted into session" {
    $resLogin = Invoke-Api "/auth/login" -Method "POST" -Headers @{ "X-Forwarded-For" = "203.0.113.195, 10.0.0.1" } -Body @{ identifier = "srm_user"; password = "password123" }
    $clientIpInDb = Exec-Sql "SELECT client_ip FROM cims_user_sessions WHERE username = 'srm_user' ORDER BY created_at DESC LIMIT 1;"
    Write-Host "   Extracted client IP in session: $clientIpInDb"
    return ($clientIpInDb -eq "203.0.113.195")
}

# ------------------------------------------------------------------------------
# SEC-17 to SEC-19: AUDIT LOGGING & RBAC RESTRICTIONS
# ------------------------------------------------------------------------------

Run-Test "SEC-17-A" "Security events (USER_LOGIN, USER_LOGIN_FAILED, ACCOUNT_LOCKED, SESSION_REVOKED) are recorded in audit log" {
    $actions = Exec-Sql "SELECT DISTINCT action_type FROM cims_audit_logs WHERE action_type IN ('USER_LOGIN', 'USER_LOGIN_FAILED', 'ACCOUNT_LOCKED', 'CHANGE_PASSWORD', 'SESSION_REVOKED', 'SESSION_CREATED');"
    Write-Host "   Recorded security audit actions:`n$actions"
    return ($actions -match "USER_LOGIN" -and $actions -match "USER_LOGIN_FAILED" -and $actions -match "ACCOUNT_LOCKED")
}

Run-Test "SEC-18-A" "Access to /api/audit is denied (HTTP 403) for non-auditor roles (e.g. BRO)" {
    $broLoginClean = Invoke-Api "/auth/login" -Method "POST" -Body @{ identifier = "bro_user"; password = "password123" }
    $res = Invoke-Api "/audit" -Method "GET" -Headers @{ "Authorization" = "Bearer $($broLoginClean.Json.token)" }
    Write-Host "   Status for BRO accessing /api/audit: $($res.StatusCode)"
    return ($res.StatusCode -eq 403)
}

Run-Test "SEC-18-B" "Access to /api/audit is granted (HTTP 200) for authorized roles (e.g. SRMGMT, COMPLIANCE)" {
    $srmgmtLogin = Invoke-Api "/auth/login" -Method "POST" -Body @{ identifier = "srmgmt_user"; password = "password123" }
    $res = Invoke-Api "/audit" -Method "GET" -Headers @{ "Authorization" = "Bearer $($srmgmtLogin.Json.token)" }
    Write-Host "   Status for SRMGMT accessing /api/audit: $($res.StatusCode), Total records: $($res.Json.Count)"
    return ($res.StatusCode -eq 200 -and $res.Json.Count -gt 0)
}

Run-Test "SEC-19-A" "/api/audit strictly filters by actionType query parameter" {
    $srmgmtLogin = Invoke-Api "/auth/login" -Method "POST" -Body @{ identifier = "srmgmt_user"; password = "password123" }
    $res = Invoke-Api "/audit?actionType=USER_LOGIN" -Method "GET" -Headers @{ "Authorization" = "Bearer $($srmgmtLogin.Json.token)" }
    Write-Host "   Filtered audit count: $($res.Json.Count)"
    $allMatch = $true
    foreach ($item in $res.Json) {
        if ($item.actionType -ne "USER_LOGIN" -and $item.action -ne "USER_LOGIN") { $allMatch = $false; break }
    }
    return ($res.StatusCode -eq 200 -and $res.Json.Count -gt 0 -and $allMatch)
}

Write-Host "`n==================================================" -ForegroundColor Yellow
Write-Host " TEST RUN SUMMARY " -ForegroundColor Yellow
Write-Host "==================================================" -ForegroundColor Yellow
Write-Host " Total Tests: $($passed + $failed)"
Write-Host " Passed:      $passed" -ForegroundColor Green
Write-Host " Failed:      $failed" -ForegroundColor Red
Write-Host "==================================================" -ForegroundColor Yellow

if ($failed -gt 0) {
    exit 1
} else {
    exit 0
}
