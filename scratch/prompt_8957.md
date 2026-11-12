<USER_REQUEST>
# CBAIMS â€” CONTROLLED REMEDIATION OF SIX EXISTING MODULES
## Reporting | First/Second-Degree Collateral | Escalation | Collateral Release | Admin Authorization | Admin Configuration

**IMPORTANT: READ THE ENTIRE PROMPT BEFORE MODIFYING ANY CODE.**

---

# 1. PROJECT CONTEXT AND YOUR RESPONSIBILITY

You are working on an existing, substantially developed Collateral and Bank Asset Insurance Management System (CBAIMS).

The system has already been developed through previous implementation work. You have access to the current source code and may have access to previous project conversation context. However, you must not depend on previous conversations, remembered requirements, or assumptions to determine the business rules for this task.

**The BRD, Business Clarification Log, ITMD response and User Story documents have NOT been uploaded to your workspace.**

Therefore, this prompt explicitly supplies the approved business requirements relevant to the six requested areas.

Treat these supplied requirements as the business authority for this task.

Your responsibility is to:

1. Fully understand how the existing application works.
2. Inspect the existing backend, frontend, database, workflows, services, controllers, repositories, APIs, integrations and configuration.
3. Trace each relevant business process from the user interface to the database and back.
4. Compare actual existing behavior against the requirements stated in this prompt.
5. Identify incomplete, incorrect, hardcoded, duplicated, disconnected or simulated functionality.
6. Correct confirmed deviations without rebuilding existing modules.
7. Test each change and its impact on related functionality.
8. Provide a detailed implementation and acceptance report.

## Critical distinction

There are two different sources of information:

**A. Business requirements:** The explicit requirements contained in this prompt.

**B. Existing implementation:** What you discover by analyzing the current codebase.

The existing implementation is not automatically correct simply because it already exists.

Likewise, a behavior found in the code must not automatically be treated as an approved business requirement.

Do not use the following as authority for introducing new business rules:

- Previous AI-generated suggestions.
- Previous implementation assumptions.
- Frontend labels or placeholder values.
- Hardcoded values in existing services.
- Demo records.
- Test data.
- Comments that describe a developer's preferred behavior.
- Common banking practices that are not explicitly included in this prompt.

If a requirement is not defined here, do not invent it.

If an existing feature is working and is not inconsistent with an explicit requirement, preserve it.

---

# 2. NON-NEGOTIABLE IMPLEMENTATION RULES

## 2.1 Do not rebuild the system

This is a remediation task, not a redesign or rebuild.

Do not:

- Replace existing modules with new implementations.
- Create parallel services performing the same operations.
- Recreate existing controllers unnecessarily.
- Replace working APIs without a demonstrated reason.
- Change existing database structures unnecessarily.
- Delete existing functionality because you prefer another implementation.
- Change unrelated modules.
- introduce unnecessary dependencies.
- change existing business rules outside the six requested areas.

Reuse the existing architecture wherever possible.

## 2.2 Fully inspect before editing

For each area, identify:

- Frontend screens and components.
- Backend controllers.
- Backend services.
- DTOs and validation.
- Models/entities.
- Repositories.
- Database migrations.
- Configuration records.
- Scheduled jobs.
- Approval workflows.
- Audit logging.
- Relevant tests.
- Related modules consuming the same data.

Do not inspect only the frontend or only the backend.

Trace the complete execution path.

For example, when an administrator changes an escalation parameter, establish whether that value is actually read by the escalation service when a scheduled escalation executes.

A value appearing in the admin screen does not prove that the business process uses it.

## 2.3 Work from an evidence-based requirement matrix

Before implementation, create this matrix for all six areas:

| Requirement ID | Requirement | Existing frontend implementation | Existing backend implementation | Database/configuration | Existing behavior | Gap | Required action |
|---|---|---|---|---|---|---|---|

Every proposed business change must map to an explicit requirement ID from this prompt.

Classify each finding as:

- ALREADY COMPLIANT
- PARTIALLY IMPLEMENTED
- NOT IMPLEMENTED
- INCORRECT IMPLEMENTATION
- TECHNICAL DEFECT
- CLARIFICATION REQUIRED
- OUT OF SCOPE

Do not report a requirement as completed simply because a corresponding class, database field or UI element exists.

## 2.4 Handling ambiguity

When the required business behavior is not specified:

1. Identify the missing decision.
2. Identify where the decision affects the implementation.
3. Do not select a value or behavior based on your own judgment.
4. Do not silently introduce a default.
5. Preserve unaffected functionality.
6. Record the issue as CLARIFICATION REQUIRED.
7. Continue with other requirements that are sufficiently defined.

Do not block the entire remediation because one subrequirement is unclear.

## 2.5 No fake implementation

Do not create a UI button that has no functional backend operation.

Do not create configuration fields that are not consumed by business services.

Do not report a notification as delivered when the provider is simulated.

Do not report a document as uploaded to a real Alfresco server when the current provider is only a local filesystem simulation.

Do not report a scheduled report as generated unless an actual report artifact has been produced.

Do not mark a CBS operation successful when it has not actually been executed.

---

# 3. AREA ONE â€” REPORTING MODULE

## Requirement group RPT

### RPT-01: Understand the entire reporting architecture

Inspect the existing:

- Report catalog.
- Report definitions.
- Report screens.
- Dashboard services.
- Report APIs.
- Reporting controllers.
- Reporting repositories.
- Report data queries.
- Report filters.
- Export functions.
- Scheduled report entities.
- Scheduled report service.
- Scheduler implementation.
- Report configuration.
- Report authorization.
- Report audit logs.

Previously identified areas include ReportCatalog, scheduled-report components, AdminConfigController and CimsSchedulerService. Verify the actual names and execution paths in the current repository rather than assuming they remain unchanged.

Identify which reports are actually implemented and which only appear in the UI.

### RPT-02: Report coverage

Verify that the reporting module supports the approved operational and management reporting requirements, including:

1. Total insured collateral.
2. Uninsured collateral.
3. Policies approaching expiry.
4. Policies due for renewal within the specified seven-day monitoring window.
5. Expired policies.
6. Renewals in pipeline.
7. Insurance compliance status.
8. Insurance company exposure.
9. Exception summaries.
10. Policy aging.
11. Operational workload.

Preserve any additional approved reports already implemented.

Do not remove existing reports merely because they are not listed above.

For each report, identify:

- Purpose.
- Data source.
- Required columns.
- Calculation logic.
- Filters.
- User access.
- Export formats.
- Whether it supports drill-down.
- Whether it can be scheduled.

Do not invent additional report metrics or calculations.

### RPT-03: Data accuracy

Every report must use actual system data.

Verify that:

- Collateral records are not duplicated in aggregated figures.
- Multiple active insurance policies are handled correctly.
- Cross-collateralized facilities are not incorrectly counted as separate collateral assets.
- Outstanding balances are sourced from the appropriate facility data.
- Expiry calculations use actual policy expiry dates.
- Closed, cancelled, replaced or expired policies are classified according to their actual lifecycle status.
- Report totals reconcile with the underlying records.
- Filters are applied before aggregation where required.
- Missing values do not silently become incorrect zero values.

Do not create new calculation rules where the business requirement does not specify one. Document any unresolved calculation definition.

### RPT-04: Report filtering and organizational scope

Verify that reports provide the required organizational views:

- Bank-wide.
- Segment.
- Branch.
- Relationship Manager.
- Other existing organizational scopes supported by the approved authorization model.

The backend must enforce the user's authorized scope.

A user must not be able to retrieve another branch's, segment's or organizational unit's data merely by manipulating request parameters.

Do not rely exclusively on hiding filters in the frontend.

### RPT-05: PDF and Excel generation

The reporting requirements include PDF and Excel output.

Verify that both formats generate real, usable files.

PDF requirements:

- Correct report title.
- Report generation date/time.
- Selected reporting period.
- Applied filters.
- Actual report data.
- Page numbering where appropriate.
- Readable tables and totals.

Excel requirements:

- Correct headers.
- Correct data.
- Properly typed dates and numbers.
- Usable column widths.
- Correct totals.
- Applied filter information where appropriate.

The User Story also includes CSV export. Verify and preserve CSV functionality where required by the report acceptance criteria.

Do not treat a frontend dropdown option as proof that the corresponding export works.

### RPT-06: Scheduled reporting â€” critical correction

The existing scheduler appears to contain logic that can identify a due report and issue a notification indicating that the report was generated without necessarily creating the actual report artifact.

Correct this.

A scheduled report must execute the real reporting process:

1. Identify the due schedule.
2. Load its saved report definition.
3. Load saved filters and authorized data scope.
4. Retrieve actual report records.
5. Apply the report's calculation and aggregation logic.
6. Generate the configured file format.
7. Persist the generated artifact using the existing approved storage architecture.
8. Record the actual execution result.
9. Pass the artifact to the configured delivery mechanism if delivery is part of that schedule.
10. Record delivery status separately from generation status.

At minimum, distinguish these outcomes:

- Report generation successful.
- Report generation failed.
- Report generated but delivery pending.
- Report delivery successful.
- Report delivery failed.

Use the existing status model if it correctly supports these distinctions. Do not create unnecessary duplicate status systems.

**Important integration limitation:** Outlook/Exchange and DMS providers in the previously audited code are simulated/local implementations. Do not claim real external delivery or real Alfresco storage when those integrations are still simulated.

A generated local file may be a valid development test artifact, but it must not be described as a production DMS upload or successful external email delivery.

### RPT-07: Report audit trail

Audit every report execution and export.

Record, using the existing audit architecture:

- Authenticated user.
- Report identifier.
- Report type.
- Date/time.
- Filters.
- Authorized data scope.
- Export format.
- Generation status.
- Generated artifact reference where available.
- Delivery status where applicable.
- Failure reason.

Do not expose confidential report data through audit records unnecessarily.

### RPT-08: Reporting performance and integrity

Inspect whether report operations can cause:

- Unnecessary repeated database queries.
- Incorrect joins.
- Duplicate rows.
- Unbounded data retrieval.
- Unrestricted downloads.
- Excessive memory use during exports.

Optimize only where an actual problem exists.

Do not alter business calculations just to improve performance.

---

# 4. AREA TWO â€” FIRST-DEGREE / SECOND-DEGREE COLLATERAL

## Requirement group DEG

### DEG-01: Understand the existing implementation

Inspect the complete first-degree/second-degree process:

- Collateral entity.
- Degree/rank fields.
- FirstDegreeSecondDegreeService or its actual equivalent.
- Controllers.
- DTOs.
- Frontend registration and amendment screens.
- Degree-related validation.
- Approval workflows.
- DMS document references.
- CBS references.
- Database migrations.
- Seed data.
- Test data.
- Notifications related to degree collateral.

Establish the exact relationship between a collateral, its first-degree charge, second-degree charge and affected facilities.

### DEG-02: First-degree data requirements

The implementation must distinguish between:

**Internal first-degree charge**

- First-degree institution/holder is the Bank.
- Reference the existing CBAIMS first-degree collateral record.
- Retrieve the first-degree loan outstanding balance from CBS as specified.
- Do not manually create a duplicate internal first-degree collateral record.
- Preserve the first-degree reference and relevant balance-as-of information.

**External first-degree charge**

The external first-degree information must support manual capture of:

- First-degree institution/holder.
- First-degree facility reference.
- First-degree outstanding balance.
- Outstanding balance as-of date.
- Mortgage/registered amount.
- Registration number.
- Registration date.
- Applicable first-degree insurance policy information.
- Insurer.
- Insurance expiry.
- Bank's position as second mortgagee.

The first-degree holder and facility reference must be actual record-specific data, not universal defaults.

### DEG-03: Remove hardcoded demo institution defaults

The existing code contains examples such as:

- Commercial Bank of Ethiopia.
- CBE.
- CBE-LN-2023-881.
- CBE-specific registration examples.

These may be legitimate external-institution examples in test data.

They must not be used as production defaults.

For a new external first-degree record:

- Institution field starts blank.
- Facility reference starts blank.
- Registration reference starts blank.
- Outstanding balance is entered or obtained from the permitted source.
- As-of date is populated only from an actual recorded date.
- Existing data is retained when editing an existing record.

Do not remove the external first-degree institution field. The institution must remain manually capturable.

Search the entire relevant project, including:

- Backend constants.
- Frontend default states.
- Dropdown defaults.
- Form initialization.
- Seed data.
- Database migrations.
- Test fixtures.
- Notification templates.
- Demo records.

Separate legitimate test fixtures from production behavior.

If the name Commercial Bank of Ethiopia is deliberately used as an external test example, retain it only as clearly identifiable test data.

### DEG-04: External first-degree evidence

For an external first-degree charge, support the evidence required by the approved clarification:

1. Confirmation letter from the other financial institution.
2. Mortgage/registration extract.

Associate the relevant documents with the correct collateral and degree record using the existing DMS abstraction.

Do not accept a fabricated DMS success response.

The current audited DMS provider uses local filesystem simulation rather than real Alfresco API communication. Preserve this distinction in status and test results.

Where evidence is mandatory, backend validation must enforce it, not merely frontend warnings.

### DEG-05: Second-degree dependency

A second-degree charge must not be created independently without its corresponding first-degree information.

The system must:

- Require a first-degree record/reference.
- Distinguish internal from external first-degree information.
- Require the relevant first-degree supporting evidence.
- Validate the presence and consistency of required fields.
- Preserve the first-degree reference.
- Track the second-degree status and related insurance information.

Do not invent additional eligibility restrictions or rank rules.

Do not impose an additional owner-CIF equality rule.

Do not invent a restriction that the collateral can secure only one facility.

### DEG-06: External balance refresh

The clarification requires periodic refreshing of external first-degree outstanding balances.

Inspect whether this functionality already exists.

If an approved configurable refresh frequency exists, use it.

If the frequency is not defined by the current system's approved configuration, do not invent a daily, weekly or monthly refresh interval.

Instead:

- Preserve the balance and as-of date.
- Record the refresh capability/status accurately.
- Identify the missing frequency as a clarification/configuration dependency.
- Do not pretend that an external institution's balance has been automatically refreshed.

### DEG-07: Internal first-degree release and second-degree status

Inspect the existing behavior when the internal first-degree facility or charge is released or closed.

Preserve the requirement to identify the affected second-degree record and initiate its required review/promotion process.

Do not automatically change a second-degree charge into a first-degree charge unless the existing approved workflow explicitly authorizes that operation.

The system must not silently change mortgage rank without the required review and authorization process.

Record the effect in the audit trail.

### DEG-08: Data integrity

Ensure that changing first-degree information does not accidentally:

- Delete the collateral.
- Delete second-degree evidence.
- Delete facility relationships.
- Change the registered owner.
- Change other mortgage information.
- Alter unrelated insurance policies.

Any degree amendment must preserve the existing historical record according to the existing audit/history architecture.

---

# 5. AREA THREE â€” ESCALATION AND REMINDER MANAGEMENT

## Requirement group ESC

### ESC-01: Keep reminders and escalations separate

This distinction is essential.

The approved requirements contain two related but different processes:

**A. Routine expiry reminders**

These notify staff and customers about upcoming expiry.

**B. Management escalation**

This escalates unresolved renewal/expiry matters to higher organizational authority.

Do not combine their schedules into one notification rule.

### ESC-02: Staff expiry reminders

The approved staff reminder schedule is:

- 30 working days before expiry.
- 25 working days before expiry.
- 20 working days before expiry.
- Every other working day thereafter, according to the configured business calendar.

Use the existing holiday/business calendar implementation.

Do not substitute calendar days for working days in this staff reminder schedule.

### ESC-03: Customer expiry reminders

The customer reminder schedule is:

- Begin 30 days before expiry.
- Repeat every five days.

The reminder process must use the actual policy expiry date.

Do not send a customer reminder for a policy that no longer qualifies for the reminder process under its current lifecycle status.

Do not invent additional customer reminder intervals.

### ESC-04: Daily notification limit

The clarified notification limit is:

**Maximum two reminder notifications per recipient per day.**

The intended distribution is:

- One at the beginning of the day.
- One at mid-day.

Preserve the available beginning-of-day and mid-day scheduling mechanism.

Do not invent exact clock times where none are specified.

Enforce the maximum per recipient, not merely the maximum per policy.

For example, if an RM has several policies reaching reminder milestones on the same day, the system must not create an unlimited number of individual reminder deliveries.

Consolidate relevant items where the existing notification design supports it.

Track sent notifications so scheduler reruns do not create duplicates.

### ESC-05: Management escalation schedule

The approved escalation schedule is:

**First escalation: 10 calendar days before policy expiry**

- Notify the responsible Relationship Manager.
- Copy the owning Relationship Officer.

**Second escalation: Five days after the first escalation, if the matter remains unresolved**

- Escalate to the applicable Branch Manager OR Corporate Center Manager.

The actual responsible organizational level must be determined from the existing organizational assignment/configuration, not guessed.

**Third escalation: When the policy is more than 10 days overdue**

- Escalate to the applicable District Business Manager OR Area Manager.
- Copy the respective manager as required by the approved organizational routing.
- Include a summary grouped by area office and branch.
- Include how long each policy has been overdue.

**Recurring escalation:**

- Repeat every 15 days thereafter while the escalation remains unresolved.

Do not invent other escalation levels, intervals or recipients.

### ESC-06: Consolidation

Where several policies reach the same escalation stage on the same day, consolidate them into the appropriate summary notification/report.

The consolidated output must allow management to identify:

- Relevant organizational area.
- Branch.
- Policy.
- Collateral.
- Expiry date.
- Days overdue, where applicable.
- Insurer where available.
- Relevant exposure/amount where required by the existing report definition.

Do not include invented risk scores, penalty amounts or additional business decisions.

### ESC-07: Recipient routing

Resolve recipients using the actual organizational structure and assignments.

Do not send every Branch Manager an escalation for every branch.

Do not send every District Business Manager a bank-wide escalation.

Check:

- RM ownership.
- RO assignment.
- Branch relationship.
- Corporate Center responsibility.
- District hierarchy.
- Area hierarchy.
- Active user status.
- Configured role/organizational assignments.

If the existing organization model cannot determine which responsible manager applies, identify the missing mapping. Do not arbitrarily choose a recipient.

### ESC-08: Remove the unsupported bank-placed coverage instruction

The previously audited implementation contains an instruction to:

"Initiate bank-placed coverage if renewal is not filed within five business days."

This instruction is not established by the business requirements supplied for this task.

Remove this business action from the escalation workflow and notification messages.

Do not replace it with another penalty, automatic coverage action, policy cancellation, loan restriction or other unsupported consequence.

The escalation process must communicate the approved status and escalate to the approved recipients.

### ESC-09: Scheduler reliability

Verify:

- No duplicate notifications from repeated scheduler execution.
- No notification for an ineligible policy status.
- Correct reminder stage.
- Correct escalation stage.
- Correct working-day calculations.
- Correct calendar-day escalation calculations.
- Correct recipient.
- Correct consolidation.
- Daily limits.
- Audit of successful and unsuccessful processing.

Provider simulations must remain clearly identified as simulations.

---

# 6. AREA FOUR â€” COLLATERAL RELEASE

## Requirement group REL

### REL-01: Understand the current release process

Inspect all related components:

- Release request frontend.
- Early release service.
- Release controller.
- Release entity/model.
- Release DTOs.
- Workflow/approval service.
- Collateral-facility relationship service.
- Allocation service.
- Insurance adequacy service.
- Exception management.
- Claim data.
- DMS document references.
- CBS linkage/status.
- Audit trail.

Do not implement release logic in a separate parallel service if the existing release architecture can be corrected.

### REL-02: Pre-release assessment

Before a release is submitted for approval, the system must calculate the effect of releasing the collateral.

The assessment must consider the remaining collateral position and the facilities secured by the collateral.

For cross-collateralized arrangements, inspect every affected facility, not just the facility selected by the user.

The assessment must identify:

- Collateral being released.
- Related facilities.
- Existing allocations/exposure.
- Remaining collateral position.
- Post-release coverage/margin result.
- Relevant validation failures.
- Whether the request can proceed to the configured approval workflow.

Do not calculate the result using only one selected facility when other facilities remain linked.

### REL-03: Remaining collateral coverage and margin

The approved release requirement is:

**After the collateral is released, the remaining collateral must continue to meet the required coverage and margin for all affected facilities.**

This must be evaluated using the applicable approved coverage/margin rule.

The current audited implementation includes a hardcoded 100% release requirement.

Do not assume this is a confirmed universal release-margin rule.

Important distinction:

- The clarified 100% insurance coverage threshold is an insurance adequacy parameter.
- It does not automatically establish a universal 100% collateral-release margin.

Inspect the existing approved rule/configuration source.

If a specific release margin is supported by an existing approved configuration, use that configuration.

If no authoritative release-margin value is available:

- Do not invent a value.
- Do not silently replace 100% with another value.
- Do not disguise the unresolved requirement as a completed fix.
- Flag the release-margin value as CLARIFICATION REQUIRED.
- Preserve the existing implementation until the business decision is established, and clearly report that this remains a release-control dependency.

### REL-04: Cross-collateralized release

Validate all relevant relationships.

Test all four relationship patterns:

- One collateral supporting one facility.
- One collateral supporting multiple facilities.
- Multiple collateral assets supporting one facility.
- Multiple collateral assets supporting multiple facilities.

For every release:

1. Identify all affected facilities.
2. Recalculate the remaining eligible security position.
3. Apply the approved coverage/margin rule.
4. Identify any facility that becomes non-compliant.
5. Do not remove valid relationships unrelated to the approved release.
6. Do not over-release available collateral allocation.
7. Record the post-release state.

If any affected facility fails a confirmed mandatory release condition, the release must not proceed through approval as if it were compliant.

### REL-05: Insurance exceptions and claims

The existing release implementation contains checks relating to unresolved insurance exceptions and pending claims.

Inspect exactly how these checks work.

Apply only the blocking conditions supported by the existing approved release requirements:

- An unresolved insurance exception must be handled according to the applicable release restriction.
- A pending insurance claim must block release only where it qualifies under the approved blocking condition.

Do not automatically treat every open exception, every historical claim or every informational issue as a release blocker.

Where the exact blocking status is not defined by the current implementation requirements, record it for clarification rather than inventing a new restriction.

### REL-06: Release documents

Release must use the required supporting documents and existing DMS document association mechanism.

Validate document presence on the backend.

Do not satisfy document validation merely because the user selected a filename in the frontend.

The system must distinguish between:

- Document metadata saved.
- Document stored using the local simulator.
- Document actually uploaded to Alfresco.

The current audited DMS provider is a local filesystem simulation. Do not claim that real Alfresco storage has been completed.

### REL-07: Credit approval and maker-checker

Release approval must follow the configured authorized credit approval hierarchy.

The maker must not approve their own request where maker-checker separation applies.

Verify:

- Maker identity.
- Approver identity.
- Approval authority.
- Delegation where supported.
- Current request status.
- Rejection/return behavior.
- Audit records.

Do not invent a new approval level or approval percentage.

Do not automatically give System Administrator credit approval rights.

### REL-08: CBS unlinking

The approved ITMD clarification states that after release approval, the linkage must also be removed from CBS manually.

The current CBS implementation is a simulator, not a verified production FLEXCUBE integration.

Therefore:

- Do not claim a real CBS unlink occurred.
- Do not simulate a successful live CBS API transaction.
- Show the required manual CBS unlink action/status clearly.
- Preserve the approved CBAIMS release decision separately from the CBS manual action.
- Record the manual CBS unlink status accurately where the existing workflow supports it.
- Do not mark the CBS operation complete without an actual recorded completion.

### REL-09: Release audit history

Record:

- Request.
- Collateral.
- Affected facilities.
- Pre-release assessment.
- Validation results.
- Maker.
- Approver.
- Decision.
- Release date.
- Post-release position.
- Document references.
- CBS manual unlink status.
- Any rejection or failed condition.

Do not overwrite historical release decisions.

---

# 7. AREA FIVE â€” ADMIN AUTHORIZATION

## Requirement group AUTH

This is a critical security remediation.

### AUTH-01: Fully inspect current authorization

Inspect:

- SecurityConfig.
- SecurityAuthenticationInterceptor.
- AdminAuthorizationInterceptor.
- WebMvcConfig.
- Token validation.
- Authentication principal resolution.
- AdminConfigController.
- Other admin controllers.
- Service-level authorization checks.
- Frontend API authentication headers.
- Persona-switching/testing logic.

Trace how the backend decides who is calling an admin endpoint.

### AUTH-02: Authenticated identity must be authoritative

The previously audited code permits identity resolution through:

- Authorization token.
- X-User-Id.
- userId.
- adminUserId.

Do not trust arbitrary caller-supplied user IDs as proof of identity.

The authenticated identity must be resolved from the validated authentication/security context.

A caller must not become another user simply by changing:

- X-User-Id.
- userId query parameter.
- adminUserId request parameter.
- Frontend persona selection.
- Other caller-controlled identity fields.

If legacy identity headers exist for simulator/testing compatibility, they must not override the authenticated identity or elevate privileges in production mode.

Do not rebuild the login module as part of this task. Harden the existing authentication and authorization integration.

### AUTH-03: API security configuration

Inspect broad permit-all rules, especially rules equivalent to:

`.requestMatchers("/api/**", "/**").permitAll()`

Do not leave administrative endpoints accessible without authentication.

Implement authorization at the backend security layer and verify that MVC interceptors are not the only protection.

For all admin endpoints:

- Unauthenticated request: HTTP 401.
- Authenticated user without required permission: HTTP 403.
- Authenticated, active, authorized user: permitted.

Do not use frontend visibility as security enforcement.

### AUTH-04: Explicit System Administrator business permission

The approved requirement is:

**System Administrator has full configuration access and user-management access, but does not have transactional approval rights merely because of the SYSADMIN role.**

System Administrator may manage:

- Users.
- Roles and permissions.
- System parameters.
- Insurance configuration.
- Reminder configuration.
- Escalation configuration.
- Approval hierarchy configuration.
- Notification templates.
- Collateral taxonomy.
- Approved insurer configuration.
- Branch hierarchy.
- Holiday calendar.
- Other approved system configuration.

System Administrator must not automatically receive:

- Insurance policy transactional approval.
- Collateral release transactional approval.
- Exception transactional approval.
- Other maker-checker business approval rights.

Configuration authority and transaction approval authority must remain separate.

A user may have additional explicit transactional permissions only where assigned through the approved authorization model.

### AUTH-05: Remove persona-based authorization bypasses

The existing implementation contains testing/persona-switching behavior that may allow an unrestricted admin persona.

Inspect this carefully.

The frontend must not be able to obtain admin authorization merely by selecting SYSADMIN from a persona dropdown.

Remove production authorization bypasses used for UI testing.

If a development-only simulator mode exists, enforce it strictly through the development/test profile and ensure it cannot activate in production.

### AUTH-06: Admin endpoint authorization matrix

Create a complete matrix of every administrative endpoint:

| Endpoint | HTTP method | Purpose | Required permission | Current enforcement | Required correction |
|---|---|---|---|---|---|

Include every administrative operation, not just AdminConfigController.

Cover:

- User management.
- Role/permission management.
- System configuration.
- Configuration creation.
- Configuration amendment.
- Configuration deletion/deactivation.
- Workflow hierarchy.
- Approval matrix.
- Notification templates.
- Insurer management.
- Collateral configuration.
- Reminder/escalation configuration.
- Business calendar.
- Other admin endpoints present in the repository.

Every administrative mutation must have explicit backend permission enforcement.

### AUTH-07: Authorization audit

Audit:

- Successful admin access.
- Denied admin access.
- Unauthorized configuration modifications.
- Attempts to impersonate another user.
- Attempts to use administrative permissions through transaction endpoints.

Do not record sensitive authentication tokens in the audit trail.

---

# 8. AREA SIX â€” ADMIN CONFIGURATION

## Requirement group CFG

### CFG-01: BR-18 objective

The system must support configuration of approved business parameters without requiring software code changes for ordinary configuration updates.

Inspect the existing admin configuration infrastructure.

Previously identified areas include:

- ConfigurationService.
- AdminConfigController.
- Reminder settings.
- Escalation settings.
- Approval hierarchy.
- Notification templates.
- Approved insurers.
- Collateral taxonomy.
- Branch hierarchy.
- Holiday calendar.
- System parameters.
- Mandatory document configuration.
- Workflow configuration.

Verify the actual implementation and preserve existing configuration features.

### CFG-02: Approved system parameter values

The following values were identified in the approved clarification baseline. Preserve them as the initial/default business settings.

| Parameter | Approved value/behavior |
|---|---|
| Staff reminder 1 | 30 working days before expiry |
| Staff reminder 2 | 25 working days before expiry |
| Staff reminder 3 | 20 working days before expiry |
| Subsequent staff reminder | Every other working day |
| Customer reminder starting point | 30 days before expiry |
| Customer reminder interval | Every 5 days |
| Maximum reminder notifications | 2 per recipient per day |
| Daily reminder distribution | Beginning of day and mid-day |
| Insurance coverage threshold | 100% |
| Minimum policy validity | 1 year |
| Document expiry warning | 30 days |

Important:

The 100% value is the approved insurance coverage threshold.

It must not automatically be reused as the collateral-release margin.

Do not confuse:

- Staff expiry reminders.
- Customer expiry reminders.
- Management escalation.
- Document expiry warnings.

They are different configurations and must not overwrite one another.

### CFG-03: Persistent configuration

For every parameter, verify:

1. It appears in the correct admin configuration screen.
2. Its current value loads from persistent backend storage.
3. An authorized administrator can edit it.
4. Input validation is applied.
5. The updated value is saved.
6. The old and new values are audited.
7. The relevant backend service actually consumes the new value.
8. The behavior remains correct after refresh or application restart.

Do not store a value only in React state or frontend constants.

Do not create duplicate configuration records every time the admin saves a setting.

### CFG-04: Configuration-driven business services

Trace each configured parameter to the service that consumes it.

For example:

- Staff reminder interval â†’ staff reminder scheduling.
- Customer reminder interval â†’ customer reminder scheduling.
- Maximum reminder notifications â†’ notification throttling.
- Insurance coverage threshold â†’ insurance adequacy calculation.
- Minimum policy validity â†’ policy validation.
- Document warning period â†’ document expiry monitoring.

If the value is stored but ignored by the business service, the configuration is incomplete.

Fix the connection.

### CFG-05: Hardcoded business-rule audit

Search the backend and frontend for hardcoded values related to:

- Reminder days.
- Escalation days.
- Customer notification frequency.
- Daily notification limits.
- Insurance adequacy percentage.
- Minimum policy validity.
- Document expiry warning.
- Release margin.
- First/second-degree defaults.
- Approval role assignments.
- Escalation role assignments.

Classify each finding:

**Type A â€” Confirmed approved default:** Make it centrally configurable where the requirement calls for configurability.

**Type B â€” Fixed approved rule:** Keep the rule, but centralize/document its source.

**Type C â€” Demo/test data:** Keep only in clearly isolated test/demo environments.

**Type D â€” Unsupported business assumption:** Remove from production business logic or flag for clarification.

Do not blindly replace every numeric literal. Some values may be technical limits rather than business rules.

### CFG-06: Configuration security

Only authenticated administrators with the specific configuration permission may modify administrative settings.

Enforce this in the backend.

A normal business user must not be able to modify configuration by directly calling an API.

System Administrator configuration access must not automatically grant transactional approval authority.

### CFG-07: Configuration change impact

Where configuration changes affect existing policies, facilities or scheduled operations:

- Preserve existing records.
- Avoid modifying historical decisions retroactively unless the requirement explicitly says otherwise.
- Apply new values according to the existing scheduling/processing design.
- Identify whether existing scheduled jobs need recalculation.
- Do not silently alter already approved transactions.

### CFG-08: Configuration history

For every configuration change, record:

- Administrator.
- Configuration parameter.
- Old value.
- New value.
- Date/time.
- Operation result.
- Reason if the current approved design requires one.

Existing historical audit entries must be retained.

---

# 9. SHARED INTEGRITY REQUIREMENTS

These requirements apply across all six modules.

## INT-01: Existing loan-collateral functionality must remain intact

Do not break:

- 1:1 relationships.
- 1:N relationships.
- N:1 relationships.
- N:N relationships.
- Cross-collateralization.
- Duplicate relationship prevention.
- Allocation validation.
- Third-party collateral.
- Existing collateral ownership.
- Existing relationship history.

Do not introduce an unsupported requirement that collateral owner CIF must equal borrower CIF.

## INT-02: Preserve insurance lifecycle

Do not break:

- Insurance registration.
- Policy submission.
- Approval.
- Amendment.
- Renewal.
- Endorsement.
- Replacement.
- Cancellation.
- Closure.
- Reopening.
- Insurance adequacy.
- Insurance exception management.

## INT-03: Preserve existing workflow controls

Do not bypass:

- Maker-checker.
- Authorization hierarchy.
- Existing approval status transitions.
- Audit history.
- Rejection/return workflows.

## INT-04: Preserve data

Before database changes:

- Review existing migrations.
- Check current entity relationships.
- Avoid destructive changes.
- Avoid dropping existing columns or data.
- Preserve historical relationships and audit entries.
- Add migration scripts only where necessary.
- Ensure existing records remain usable.

## INT-05: Resolve duplicate source structures

If the repository contains multiple frontend source trees or duplicated components:

1. Identify the actual build entry point.
2. Determine which source tree is used by the running application.
3. Determine whether the other tree is stale, generated or separately used.
4. Modify the authoritative active source.
5. Do not delete duplicate directories without proving they are unused.
6. Report which copies were modified and why.

Do not assume a source directory is active merely because its name appears more appropriate.

---

# 10. REQUIRED TEST CASES

Execute these test cases after implementation.

Use the actual test framework and current application architecture.

Do not report PASS unless the test was executed and the actual result was verified.

## TEST GROUP A â€” REPORTING

| Test ID | Scenario | Expected result |
|---|---|---|
| RPT-T01 | Open report catalog | All implemented approved reports are displayed correctly |
| RPT-T02 | Generate total insured collateral report | Actual underlying records are used; totals are correct |
| RPT-T03 | Generate uninsured collateral report | Only records qualifying as uninsured are included |
| RPT-T04 | Generate approaching-expiry report | Correct policies and expiry dates are displayed |
| RPT-T05 | Generate policies due within seven days report | Date filtering is accurate |
| RPT-T06 | Generate expired policy report | Expired policies are correctly classified |
| RPT-T07 | Generate renewal pipeline report | Actual renewal statuses are represented |
| RPT-T08 | Generate insurer exposure report | Aggregations reconcile with underlying records |
| RPT-T09 | Generate exception report | Correct exception records and statuses are displayed |
| RPT-T10 | Generate policy aging report | Aging is calculated using actual policy dates |
| RPT-T11 | Filter by segment | Only authorized segment records appear |
| RPT-T12 | Filter by branch | Only authorized branch records appear |
| RPT-T13 | Filter by RM | Correct assigned records appear |
| RPT-T14 | Export PDF | A valid PDF containing the report data is generated |
| RPT-T15 | Export Excel | A valid XLSX containing the report data is generated |
| RPT-T16 | Export CSV where required | A valid CSV is generated |
| RPT-T17 | Drill down from report total | Relevant underlying records are displayed |
| RPT-T18 | Attempt unauthorized report scope | Backend rejects unauthorized data access |
| RPT-T19 | Execute scheduled report | Actual report file is produced |
| RPT-T20 | Scheduled report generation failure | Failure is recorded accurately; no false success |
| RPT-T21 | Simulated email delivery | Delivery is identified as simulated/pending rather than real delivery |
| RPT-T22 | Report execution audit | Correct user, filters, report and result are recorded |

## TEST GROUP B â€” FIRST/SECOND-DEGREE COLLATERAL

| Test ID | Scenario | Expected result |
|---|---|---|
| DEG-T01 | Create external first-degree record | External institution can be entered manually |
| DEG-T02 | Open new external first-degree form | No CBE or other institution is preselected |
| DEG-T03 | Enter actual external facility reference | Entered value is saved and retrieved correctly |
| DEG-T04 | Enter external outstanding balance | Actual value and as-of date are saved |
| DEG-T05 | Attach confirmation letter | Evidence is associated with the correct record |
| DEG-T06 | Attach registration extract | Evidence is associated with the correct record |
| DEG-T07 | Create second-degree without first-degree | Backend rejects the invalid operation |
| DEG-T08 | Create internal first-degree reference | Existing CBAIMS reference is used |
| DEG-T09 | Retrieve internal first-degree balance | Permitted CBS balance source is used |
| DEG-T10 | Edit an existing external first-degree record | Existing institution and facility values are preserved |
| DEG-T11 | Check new-record defaults | Demo institution values are not used in production defaults |
| DEG-T12 | Close/release internal first-degree | Affected second-degree record is identified for the approved review/promotion workflow |
| DEG-T13 | Check balance refresh | Actual refresh date/source is recorded; no invented refresh is reported |
| DEG-T14 | Review historical changes | Existing history and audit records remain available |
| DEG-T15 | Verify unrelated collateral relationships | Degree amendments do not damage unrelated relationships |

## TEST GROUP C â€” REMINDERS AND ESCALATION

| Test ID | Scenario | Expected result |
|---|---|---|
| ESC-T01 | Policy reaches 30 working days before expiry | First staff reminder is due |
| ESC-T02 | Policy reaches 25 working days before expiry | Second staff reminder is due |
| ESC-T03 | Policy reaches 20 working days before expiry | Third staff reminder is due |
| ESC-T04 | Policy reaches subsequent staff reminder period | Every-other-working-day schedule is respected |
| ESC-T05 | Customer reaches 30-day reminder window | Customer reminder process starts |
| ESC-T06 | Customer reminder interval reaches five days | Next reminder is due |
| ESC-T07 | Recipient has two reminders in one day | Maximum of two is enforced |
| ESC-T08 | Multiple policies qualify for the same recipient | Duplicate/unlimited notifications are prevented |
| ESC-T09 | Policy reaches 10 calendar days before expiry | RM receives first escalation; owning RO is copied |
| ESC-T10 | Matter remains unresolved five days later | Applicable BM or CCM receives second escalation |
| ESC-T11 | Policy becomes more than 10 days overdue | Applicable DBM or AM receives overdue escalation |
| ESC-T12 | Multiple policies reach same escalation stage | Consolidated summary is produced |
| ESC-T13 | Fifteen-day recurring escalation becomes due | Recurring escalation is executed |
| ESC-T14 | Branch/district/area recipients are resolved | Only the applicable organizational recipients are selected |
| ESC-T15 | Scheduler executes more than once | Duplicate escalations are prevented |
| ESC-T16 | Check escalation message contents | Days overdue and required organizational summaries are accurate |
| ESC-T17 | Check bank-placed coverage logic | No undocumented bank-placed coverage instruction is generated |
| ESC-T18 | Check reminder after policy status changes | Ineligible records are not incorrectly reminded |

## TEST GROUP D â€” COLLATERAL RELEASE

| Test ID | Scenario | Expected result |
|---|---|---|
| REL-T01 | Request release of collateral linked to one facility | Pre-release assessment identifies the affected facility |
| REL-T02 | Request release of collateral linked to multiple facilities | All affected facilities are evaluated |
| REL-T03 | Request release in N:N arrangement | All relevant allocations and facilities are evaluated |
| REL-T04 | Remaining collateral meets approved release margin | Request proceeds to the configured approval process |
| REL-T05 | Remaining collateral fails required coverage | Release cannot proceed as compliant |
| REL-T06 | Only one facility is compliant but another is not | The complete cross-collateral assessment identifies the failure |
| REL-T07 | Unresolved blocking insurance exception exists | Approved release restriction is enforced |
| REL-T08 | Pending blocking insurance claim exists | Approved claim restriction is enforced |
| REL-T09 | Required release document is missing | Backend rejects or returns the request according to the workflow |
| REL-T10 | Valid release documents are associated | Correct document references are retained |
| REL-T11 | Maker attempts self-approval | Approval is rejected |
| REL-T12 | Authorized credit checker approves | Approval is processed according to the configured hierarchy |
| REL-T13 | Unauthorized user attempts approval | Backend rejects the operation |
| REL-T14 | Release is approved | Correct CBAIMS relationship/status changes are recorded |
| REL-T15 | Review CBS unlink status | Manual CBS unlink is not falsely reported as completed |
| REL-T16 | Review remaining collateral after release | Remaining valid relationships and allocations are preserved |
| REL-T17 | Review release audit history | Complete request and decision history are recorded |
| REL-T18 | Check release margin calculation | No unsupported hardcoded 100% release rule is silently introduced |

## TEST GROUP E â€” ADMIN AUTHORIZATION

| Test ID | Scenario | Expected result |
|---|---|---|
| AUTH-T01 | Unauthenticated user calls admin API | HTTP 401 |
| AUTH-T02 | Authenticated ordinary user calls admin API | HTTP 403 |
| AUTH-T03 | Authorized SYSADMIN reads configuration | Access permitted |
| AUTH-T04 | Authorized SYSADMIN updates configuration | Update permitted and audited |
| AUTH-T05 | SYSADMIN attempts transactional insurance approval | Denied unless separate approved transaction permission exists |
| AUTH-T06 | SYSADMIN attempts collateral release approval | Denied unless separate approved transaction permission exists |
| AUTH-T07 | User changes X-User-Id header | No identity impersonation |
| AUTH-T08 | User changes userId parameter | No identity impersonation |
| AUTH-T09 | User changes adminUserId parameter | No administrative elevation |
| AUTH-T10 | User changes frontend persona | No backend permission elevation |
| AUTH-T11 | Unauthorized user directly calls mutation endpoint | Backend rejects the operation |
| AUTH-T12 | Authorized checker approves a transaction | Allowed according to the actual approval permission |
| AUTH-T13 | Inactive user calls admin endpoint | Rejected |
| AUTH-T14 | Admin action is denied | Appropriate security/audit event is recorded |
| AUTH-T15 | Inspect all administrative endpoints | Every mutation has explicit server-side authorization |

## TEST GROUP F â€” ADMIN CONFIGURATION

| Test ID | Scenario | Expected result |
|---|---|---|
| CFG-T01 | Open reminder configuration | Current persisted values are displayed |
| CFG-T02 | Update staff reminder value | Saved value is reflected by staff reminder processing |
| CFG-T03 | Update customer reminder interval | Customer reminder scheduler uses the new configured value |
| CFG-T04 | Update maximum daily notifications | Notification throttling uses the configured limit |
| CFG-T05 | Update insurance coverage threshold | Adequacy engine uses the configured value |
| CFG-T06 | Update minimum policy validity | Relevant policy validation uses the configured value |
| CFG-T07 | Update document expiry warning | Document expiry monitoring uses the configured value |
| CFG-T08 | Update escalation configuration | Escalation service consumes the persisted settings |
| CFG-T09 | Refresh configuration page | Saved values remain correct |
| CFG-T10 | Restart application where applicable | Persistent configuration remains intact |
| CFG-T11 | Unauthorized user modifies configuration | Backend rejects the operation |
| CFG-T12 | Save configuration | Old and new values are recorded in audit history |
| CFG-T13 | Inspect hardcoded rules | Confirmed configurable rules are not duplicated in unrelated services |
| CFG-T14 | Run affected business service after update | New configuration actually affects execution |
| CFG-T15 | Verify insurance threshold versus release margin | The two rules remain separate |

---

# 11. ACCEPTANCE CRITERIA

The remediation is accepted only when all the applicable criteria below are satisfied.

## AC-01: Reporting

- Actual report data is generated.
- Required PDF and Excel outputs work.
- CSV works where required by the User Story.
- Scheduled reports produce actual artifacts.
- Report data is correctly filtered and aggregated.
- Report access respects organizational scope.
- Execution and export are audited.
- Failed generation is not represented as successful generation.
- Simulated external delivery is not represented as actual delivery.

## AC-02: First/Second-Degree Collateral

- New external first-degree records have no hardcoded institution defaults.
- External institution and facility information can be captured correctly.
- Internal records reference existing CBAIMS records.
- The permitted CBS balance source is respected.
- Required external supporting documents are validated.
- Second-degree creation requires first-degree information and required evidence.
- Internal first-degree closure/release triggers the approved second-degree review/promotion process.
- No unrelated collateral relationships or ownership records are damaged.

## AC-03: Escalation

- Reminder and escalation schedules remain separate.
- Working-day and calendar-day calculations are not confused.
- All stated milestones operate correctly.
- Notification maximum is enforced per recipient.
- Correct organizational recipients receive escalations.
- Multiple policies are consolidated where required.
- Duplicate scheduler execution does not generate duplicate notifications.
- Unsupported bank-placed coverage instructions are removed.

## AC-04: Collateral Release

- Release assessment considers all affected facilities.
- Cross-collateralization is properly evaluated.
- Remaining security meets the approved coverage/margin requirement.
- Confirmed insurance exception and claim restrictions are applied correctly.
- Required supporting documents are validated.
- Maker-checker is enforced.
- An unauthorized user cannot approve.
- Post-release allocations and statuses remain accurate.
- CBS manual unlink is not falsely presented as a completed integration operation.
- Unspecified release margin remains explicitly identified for clarification.

## AC-05: Admin Authorization

- Identity comes from validated authentication.
- Caller-supplied IDs cannot impersonate users.
- Every admin mutation has backend authorization.
- System Administrator can perform authorized configuration tasks.
- System Administrator does not automatically receive transactional approval authority.
- Persona selection cannot elevate backend privileges.
- Unauthorized operations return the proper security response.

## AC-06: Admin Configuration

- Approved configuration values are present.
- Settings persist in the database.
- Authorized administrators can change them.
- Changes are audited.
- Relevant services consume the configured values.
- No dead or decorative configuration fields are presented as functional.
- Insurance coverage threshold is not confused with release margin.
- No undocumented business rule is added.

## AC-07: Regression

All existing unaffected functionality must continue working.

At minimum regression-test:

- Collateral registration.
- Borrower-owned collateral.
- Third-party collateral.
- Customer and facility association.
- Loan-collateral linkage from both directions.
- 1:1, 1:N, N:1 and N:N relationships.
- Cross-collateralization.
- First/second-degree collateral.
- Insurance registration.
- Policy amendment and renewal.
- Insurance adequacy.
- Exceptions.
- Maker-checker.
- Audit history.
- Dashboard summaries.
- Existing admin configuration.
- Existing role and permission behavior.

---

# 12. FINAL DELIVERY REPORT â€” MANDATORY

When implementation and testing are complete, provide a detailed report.

Do not simply say "completed successfully."

Use these sections.

## A. Executive summary

Explain what was actually improved and what remains outstanding.

## B. Requirement-by-requirement results

| Requirement ID | Description | Initial status | Final status | Evidence |
|---|---|---|---|---|

## C. Exact files modified

Separate:

- Frontend files.
- Backend files.
- Database migrations.
- Configuration files.
- Tests.

For each file explain why it was changed.

## D. Business-rule corrections

List each hardcoded or incorrect business rule found.

Show:

- Original behavior.
- Why it was incorrect or unsupported.
- Corrected behavior.
- Requirement ID supporting the correction.

## E. Configuration verification

Provide a table showing:

| Parameter | Stored value | Service consuming it | Test result |
|---|---|---|---|

## F. Authorization results

Provide the admin endpoint authorization matrix, including endpoints that remain unresolved.

## G. Test results

| Test ID | Expected result | Actual result | Status | Evidence |
|---|---|---|---|---|

Use only:

- PASS â€” actually executed and verified.
- FAIL â€” executed and failed.
- BLOCKED â€” unable to execute due to an identified dependency.
- NOT RUN â€” not executed.

Do not label an unexecuted test as PASS.

## H. Remaining issues

Separate:

1. Confirmed code defects not fixed.
2. External integration dependencies.
3. Business clarification requirements.
4. Environment/build/test blockers.
5. Out-of-scope items.

## I. Regression assessment

Explicitly report whether changes affected or potentially affected the existing collateral, insurance, linkage, workflow, exception, dashboard, audit and administration modules.

## J. Final implementation status

Classify each of the six areas individually:

- COMPLETE
- PARTIALLY COMPLETE
- NOT COMPLETE
- BLOCKED BY CLARIFICATION
- BLOCKED BY TECHNICAL DEPENDENCY

Do not give one overall COMPLETE status when material subrequirements remain outstanding.

---

# 13. FINAL INSTRUCTION â€” NO INVENTED BUSINESS LOGIC

You must follow this rule throughout the task:

**Every business-rule change must be supported by an explicit requirement in this prompt.**

Examples:

- Do not invent a collateral-release percentage.
- Do not invent an external first-degree balance refresh interval.
- Do not invent additional escalation levels.
- Do not invent notification recipients.
- Do not invent a bank-placed coverage requirement.
- Do not treat CBE test data as an approved production default.
- Do not assume System Administrator can approve transactions.
- Do not treat insurance coverage percentage as collateral-release margin.
- Do not invent restrictions on third-party collateral.
- Do not create additional approval levels.
- Do not create additional report calculations without an approved definition.

When a business decision is missing, identify the exact missing decision and leave that subrequirement unresolved.

**The objective is to make the existing CBAIMS implementation accurately follow the defined requirements, not to make the application behave according to your own interpretation of how a banking system should work.**

Inspect thoroughly. Fix precisely. Test completely. Report honestly.
</USER_REQUEST>
<ADDITIONAL_METADATA>
The current local time is: 2026-11-10T11:01:49+03:00.

The user's current state is as follows:
Active Document: c:\Users\danie_mdh4j2\Desktop\cims proj\scratch\test_dual_control_exception_approval.ps1 (LANGUAGE_UNSPECIFIED)
Cursor is on line: 1
Other open documents:
- c:\Users\danie_mdh4j2\Desktop\cims proj\frontend\src\components\collateral\wizard\CollateralRegistrationWizard.tsx (LANGUAGE_UNSPECIFIED)
- c:\Users\danie_mdh4j2\Desktop\cims proj\backend\src\main\java\com\bank\cims\service\ValidationService.java (LANGUAGE_UNSPECIFIED)
- c:\Users\danie_mdh4j2\Desktop\cims proj\backend\src\main\java\com\bank\cims\service\CimsService.java (LANGUAGE_UNSPECIFIED)
- c:\Users\danie_mdh4j2\Desktop\cims proj\CIMS_TESTING_PLAYBOOK.md (LANGUAGE_UNSPECIFIED)
- c:\Users\danie_mdh4j2\Desktop\cims proj\backend\src\main\java\com\bank\cims\config\FlywayConfig.java (LANGUAGE_UNSPECIFIED)
</ADDITIONAL_METADATA>
