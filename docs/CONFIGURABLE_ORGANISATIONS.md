# Configurable organisation model — single-organisation operating mode

**Supersedes the earlier multi-customer rollout assumption.** The approved CnC scope as of 2026-11-17 is one provisioned Sun King organisation in one modular monolith. The existing `organisation_id` data boundary remains in place for integrity and access control, but customer self-service onboarding and multi-tenant SaaS rollout are outside the current delivery scope.

## Decision

MobiWave CnC remains one deployable application, one backend and one database. Sun King is the initial and current operating organisation. Customer-specific structure is configuration data, not customer-specific code. MobiWave administrators provision and maintain the Sun King organisation and its operating configuration.

Do not create additional customer organisations, expose tenant registration, or build cross-customer administration as part of the retail foundation without a new explicit product decision.

## Configuration domains

- **Geographic organisation units:** a tree such as head office, zone, region and shop. Unit types are labels/configuration, not hard-coded enum values.
- **Departments:** independent business dimensions that may have their own roles, workflows and policies. LPG, Inverter and Phone Retail are initial Sun King labels, not global assumptions.
- **Roles and permissions:** role labels map to a controlled permission catalogue. A role name supplied by a client is never an authorization decision.
- **Assignments:** effective-dated employee-to-unit and role relationships. Multiple concurrent assignments are supported; expired assignments must not authorize actions.
- **Reporting and workflow:** explicit reporting edges and approval policy steps. Do not infer department reporting solely from geographic parentage.
- **Organisation boundary:** every business record is scoped to the provisioned Sun King organisation and authorized units. A client-provided organisation ID is never sufficient authorization.
- **Auditability:** configuration changes, role grants, support access, approvals and financial actions are append-only audited events with actor, target, timestamp and reason.

## Sun King initial configuration inputs

- 5 zones; 20 regions; 170 shops.
- Zone role positions reported: 5 ZBMs, 5 ZTMs, 5 ZRSMs, 5 ZRCSs.
- Region role positions reported: 20 RBMs, 20 RMs, 20 RSMs, 20 RCSs.
- Shop role positions reported: 170 ABMs, 170 Shop Managers and 170 Associates.
- 96 ATDs and 62 ASMs; their coverage and reporting relationships must be confirmed before enforcing them.
- LPG, Inverter and Phone Retail departments; their structures are independent/configurable.
- Counts are initial configuration inputs, not schema limits or verified unique headcount. People may hold multiple assignments.

## Authorisation rules

1. Derive organisation scope from the authenticated identity and active employee assignment on the server.
2. Validate effective assignment, permission, unit scope and workflow step on every protected operation.
3. Maker and checker must be different authenticated people for controlled financial actions.
4. Never accept role, approver, organisation, department, amount limit or verification outcome as trusted client claims.
5. Only approved MobiWave administrators may provision or alter the initial organisation-wide configuration.
6. Support access is time-bound, reason-coded, explicitly authorized and audited; represented-user actions remain attributable to both support actor and represented user.
7. Deny by default when configuration is missing, contradictory, expired or references an unauthorized unit.

## Delivery sequence

1. Validate the existing organisation and hierarchy schema against the live project.
2. Build organisation/unit/department/role/assignment/workflow configuration screens.
3. Import Sun King's initial structure through previewable, validated CSV/XLSX import.
4. Do not create invented employees or unconfirmed reporting lines.
5. Replace synthetic dashboard values with server-backed, unit-scoped queries.
6. Test shop, region, zone and finance role scopes, expired assignments, approval separation and support access before production use.

## Release gate

A schema migration alone does not mean the application is production-ready. Require applied migrations, RLS and unit-scope tests, a provisioned staging organisation, end-to-end workflows, backup/restore verification and security review before live operational use.
