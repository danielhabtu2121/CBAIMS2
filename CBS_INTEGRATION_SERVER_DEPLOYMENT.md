# CBAIMS — CBS Oracle Three-View Integration & Deployment Guide

This document provides step-by-step instructions for deploying and configuring the production CBS (FLEXCUBE) Oracle integration in CBAIMS.

---

## 1. Where the Addresses & Credentials Are Located

All CBS Oracle connection settings are managed in **two places**:

### A. The Spring Boot Configuration File
**File Path**: `backend/src/main/resources/application.yml`  
Under the `cims.cbs.oracle` section (lines 45–58):

```yaml
cims:
  cbs:
    # Mode: Set to 'REAL' for production Oracle, or 'SIMULATOR' for local testing
    mode: ${CIMS_CBS_MODE:REAL}
    oracle:
      # CBS Oracle Database JDBC URL
      jdbc-url: "${CIMS_CBS_JDBC_URL:jdbc:oracle:thin:@//10.41.30.114:1521/FCUBUAT}"
      # Oracle Database Username
      username: "${CIMS_CBS_USERNAME:CBAIMSUSER}"
      # Oracle Database Password (quoted for special characters @ and !)
      password: "${CIMS_CBS_PASSWORD:CBaims@12345!}"
      # Oracle Schema Name
      schema: "${CIMS_CBS_SCHEMA:FCUBSLIVE}"
      
      # Three CBS Oracle Views (62 columns total)
      customer-view: "${CIMS_CBS_CUSTOMER_VIEW:FCUBSLIVE.STVWCBS_CBAIMS_CUSTOMER}"
      facility-view: "${CIMS_CBS_FACILITY_VIEW:FCUBSLIVE.STVWCBS_CBAIMS_FACILITY}"
      collateral-view: "${CIMS_CBS_COLLATERAL_VIEW:FCUBSLIVE.STVWCBS_CBAIMS_COLLATERAL}"
```

### B. Server Environment Variables (Optional override)
On your deployment server (`172.22.11.55`), you can override these values if needed:

| Environment Variable | Description | Default Value |
|---|---|---|
| `CIMS_CBS_MODE` | Switch to Real Oracle integration | `REAL` |
| `CIMS_CBS_JDBC_URL` | Oracle JDBC connection string | `jdbc:oracle:thin:@//10.41.30.114:1521/FCUBUAT` |
| `CIMS_CBS_USERNAME` | CBS Database User | `CBAIMSUSER` |
| `CIMS_CBS_PASSWORD` | CBS Oracle Password | `CBaims@12345!` |
| `CIMS_CBS_SCHEMA` | CBS Schema containing the views | `FCUBSLIVE` |

---

## 2. Server Deployment Instructions (Host: 172.22.11.55)

### Step 1: Connect via SSH (MobaXterm)
Open MobaXterm, start a new SSH session to:
- **Host**: `172.22.11.55`
- **Port**: `22`
- **Username**: *(your server deploy user)*

### Step 2: Verify Network Egress to CBS Oracle Database
From the server shell on `172.22.11.55`, test that port `1521` on the CBS Oracle host `10.41.30.114` is reachable through the bank firewall:
```bash
# Using nc (netcat)
nc -zv 10.41.30.114 1521

# Or using telnet / curl
telnet 10.41.30.114 1521
```
*Expected result:* `Connection to 10.41.30.114 1521 port [tcp/oracle] succeeded!`

### Step 3: Configure Environment Variables
If running as a `systemd` service (e.g. `/etc/systemd/system/cbaims-backend.service`), add the environment variables to the service definition:

```ini
[Service]
Environment="CIMS_CBS_MODE=REAL"
Environment="CIMS_CBS_JDBC_URL=jdbc:oracle:thin:@//10.41.30.114:1521/FCUBUAT"
Environment="CIMS_CBS_USERNAME=CBAIMSUSER"
Environment="CIMS_CBS_PASSWORD=CBaims@12345!"
Environment="CIMS_CBS_SCHEMA=FCUBSLIVE"
```

Then reload systemd and restart the service:
```bash
sudo systemctl daemon-reload
sudo systemctl restart cbaims-backend
```

If running manually or via Docker / Shell script:
```bash
export CIMS_CBS_MODE=REAL
export CIMS_CBS_JDBC_URL="jdbc:oracle:thin:@//10.41.30.114:1521/FCUBUAT"
export CIMS_CBS_USERNAME="CBAIMSUSER"
export CIMS_CBS_PASSWORD="CBaims@12345!"
export CIMS_CBS_SCHEMA="FCUBSLIVE"

# Start the backend
mvn spring-boot:run
```

---

## 3. Post-Deployment Verification via REST API

Once deployed, run these commands (or use the frontend **CBS Data Quality Workbench**) to verify the live integration:

### 1. Connection Health Check
```bash
curl -s -H "Authorization: Bearer <YOUR_JWT_TOKEN>" http://localhost:8080/api/cbs/health | jq .
```
Expected output:
```json
{
  "mode": "REAL",
  "status": "AVAILABLE",
  "message": "Real CBS Oracle database connected successfully to jdbc:oracle:thin:@//10.41.30.114:1521/FCUBUAT (Schema: FCUBSLIVE)",
  "timestamp": "..."
}
```

### 2. Schema Validation (All 3 Views & All 62 Columns)
```bash
curl -s -H "Authorization: Bearer <YOUR_JWT_TOKEN>" http://localhost:8080/api/cbs/schema/validate | jq .
```
Expected output:
```json
{
  "status": "AVAILABLE",
  "message": "All 3 Oracle views and all 62 columns validated successfully against FCUBSLIVE",
  "totalColumnsVerified": 62,
  "views": {
    "FCUBSLIVE.STVWCBS_CBAIMS_CUSTOMER": 16,
    "FCUBSLIVE.STVWCBS_CBAIMS_FACILITY": 15,
    "FCUBSLIVE.STVWCBS_CBAIMS_COLLATERAL": 31
  }
}
```

### 3. Dry-Run Import Preview
```bash
curl -s -H "Authorization: Bearer <YOUR_JWT_TOKEN>" http://localhost:8080/api/cbs/three-views/preview | jq .
```
Verifies counts, detects duplicate collateral codes, checks customer-facility matches, and previews sample rows without modifying the database.

### 4. Execute Three-View Production Import
```bash
curl -s -X POST http://localhost:8080/api/cbs/three-views/import \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"batchReference":"CBS-PROD-BATCH-001","userId":"sysadmin","chunkSize":50}' | jq .
```

---

## 4. Architectural Safeguards Built Into CBAIMS

1. **Strict Read-Only Access to CBS Oracle**:
   - Connection pool is initialized with `readOnly = true` and `autoCommit = false`.
   - All queries against Oracle are `SELECT` only. Zero writes, zero updates, zero DDL executed on CBS.
2. **CBAIMS is Master After Import**:
   - Once imported, CBAIMS becomes the operational master for collateral details, classifications, owners, valuations, and insurance.
   - Enriched fields are protected from recurring overwrite.
3. **Existing CBAIMS Collateral Protection**:
   - Any collateral with `originType = 'CBAIMS_REGISTERED'` is strictly preserved and never overwritten by CBS data. Any matching codes are routed to the Duplicate Review Queue.
4. **Audit Provenance & Snapshots**:
   - Complete unedited source rows are archived in `cbs_customer_raw_snapshots`, `cbs_facility_raw_snapshots`, and `cbs_collateral_raw_snapshots`.
   - Field-level modifications by CROs are logged with timestamp, user ID, previous value, and reason in provenance audit tables.

