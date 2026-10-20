# Configurable organisation model (monolith)

## Decision

MobiWave-CnC remains a single deployable monolith: one application, one backend and one database. Customer-specific structure is configuration data, not customer-specific code. Sun King is the first configured tenant; new customers submit a registration and enterprise-configuration request and are activated only after MobiWave review and provisioning.

## Configuration domains

- **Organisation units:** a tenant-defined tree (head office, zone, region, shop, department, or other unit type). Unit types are labels/configuration, not hard-coded enum values.
- **Departments:** independent business units that may have their own reporting scope, roles, approval chains and expense policies. LPG, Inverter and Phone Retail are initial Sun King configuration labels, not global assumptions.
- **Roles and permissions:** tenant-defined role labels map to a controlled permission catalogue. Never treat a role name supplied by the client as an authorization decision.
- **Assignments:** effective-dated user/employee-to-unit and role relationships. Multiple concurrent assignments are supported; expired assignments must not authorize actions.
- **Reporting and workflow:** explicit reporting edges and approval policy steps. Do not infer department reporting solely from geographic parentage.
- **Tenant boundary:** every business record and every configuration lookup is scoped by the authenticated user's active organisation and authorized units. A client-provided organisation ID is never sufficient authorization.
- **Auditability:** configuration changes, role grants, impersonation/login-as-client, approvals and financial actions are append-only audited events with actor, tenant, target, timestamp and reason.

## Sun King initial configuration (editable)

- 5 zones; 20 regions; 170 shops.
- Zone role positions reported: 5 ZBMs, 5 ZTMs, 5 ZRSMs, 5 ZRCSs.
- Region role positions reported: 20 RBMs, 20 RMs, 20 RSMs, 20 RCSs.
- Shop role positions reported: 170 ABMs, 170 Shop Managers and 170 Associates.
- 96 ATDs and 62 ASMs; their coverage and reporting relationships must be confirmed before enforcing them.
- LPG, Inverter and Phone Retail departments; their structures are independent/configurable.
- Counts are initial configuration inputs, not schema limits or verified unique headcount. People may hold multiple assignments.

## Client onboarding lifecycle

registered -> configuration_requested -> under_review -> approved -> provisioning -> active

Additional operational states: rejected, suspended, closed. Registration does not create an active tenant workspace. Only authorized MobiWave administrators may approve, provision, activate, suspend or close a tenant; each transition is audited.

## Authorization rules

1. Derive tenant scope from the authenticated identity and active membership on the server.
2. Validate effective assignment, permission, unit scope and workflow step on every protected operation.
3. Maker and checker must be different authenticated people for controlled financial actions.
4. Never accept role, approver, organisation, department, amount limit or verification outcome as trusted client claims.
5. Tenant admins can configure their own allowed units and workflows but cannot alter the global permission catalogue or another tenant.
6. MobiWave support access is time-bound, reason-coded, explicitly authorized and audited; impersonated financial actions must remain attributable to both support actor and represented user.
7. Deny by default when configuration is missing, contradictory, expired or references an unauthorized unit.

## Delivery sequence

1. Add configuration and onboarding persistence with database constraints and RLS.
2. Build MobiWave-admin configuration UI and request review/provisioning workflow.
3. Build tenant-admin screens for units, departments, roles, assignments and workflow policies.
4. Import Sun King's initial structure through previewable, validated CSV/XLSX import; do not create invented employees or unconfirmed reporting lines.
5. Replace synthetic dashboard values with tenant-scoped backend queries.
6. Test two tenants, multiple roles, expired assignments, cross-department boundaries, approval separation and support impersonation before production use.

## Release gate

A schema migration alone does not mean the application is production-ready. Require applied migrations, RLS tests using separate authenticated identities, a provisioned Sun King test tenant, end-to-end finance workflow tests, backup/restore verification and security review before live financial use.
