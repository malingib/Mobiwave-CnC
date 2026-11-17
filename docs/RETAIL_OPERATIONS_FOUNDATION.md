# MobiWave CnC — Retail Operations Foundation

**Status:** Approved architecture baseline  
**Decision date:** 2026-11-17  
**Repository baseline:** `main` at `beadb3e51dade6a7d528ebb29a6e5c48ec89982a`  
**Delivery branch:** `architecture/cnc-retail-foundation`

## 1. Product decision

The existing Expense & eTIMS Accountability application is the foundation of the complete MobiWave CnC retail operations platform for the initial Sun King deployment.

Finance is retained as a first-class, separately bounded domain inside the same product. It is not discarded, rewritten as a separate product, or allowed to become the owner of inventory and sales records.

The platform remains a **single deployable monolith**: one Android client, one React/Vite web console, one Supabase project and one coordinated database. The initial operating mode is one provisioned Sun King organisation. Existing `organisation_id` scoping remains a security boundary and compatibility mechanism; this decision does not authorize a multi-customer SaaS rollout or tenant self-service expansion.

No live staff import, payment movement, destructive migration or production integration activation is authorized by this architecture decision.

## 2. Confirmed operating context

Initial configuration inputs supplied for Sun King:

- 5 zones and 20 regions.
- 170 shops.
- 96 ATDs and 62 ASMs.
- Shop-facing positions include ABMs, Shop Managers and Associates.
- Zone and regional positions include ZBMs, ZTMs, ZRSMs, ZRCSs, RBMs, RMs, RSMs and RCSs.
- Business departments include LPG, Inverter and Phone Retail.

These are configuration inputs, not hard-coded schema limits or verified unique headcount. Coverage, reporting lines, concurrent assignments and actual employee identities must be confirmed before import. Do not infer that an ATD or ASM belongs to a particular zone or region from counts alone.

## 3. Architecture principles

1. **Modular monolith:** domain boundaries in code, migrations and service interfaces; no premature microservices.
2. **One source of truth:** the server/database owns all durable operational, stock, financial and compliance state.
3. **Ledger-first inventory and finance:** stock and money movements are append-only events; balances are derived or transactionally maintained projections.
4. **Explicit authority:** role labels are not permissions. Server-side permission and unit-scope checks protect every write and privileged read.
5. **Offline-aware, not offline-fiction:** clients may capture drafts offline, but must distinguish local-pending from server-committed records.
6. **Idempotent operations:** every retriable write has a stable client/request idempotency key and server-enforced uniqueness.
7. **Audit by design:** actor, effective identity, scope, reason, timestamp, request/correlation ID and before/after values are recorded for sensitive changes.
8. **Fail closed:** uncertain payment, eTIMS, stock or sync outcomes remain pending/reviewable; never infer success from request acceptance.
9. **Additive evolution:** preserve existing expense, disbursement, evidence, reconciliation, invoice-review and audit workflows.
10. **No invented customer data:** configuration and imports are previewed, validated and approved before persistence.

## 4. Domain map and ownership

| Domain | Owns | Does not own |
|---|---|---|
| Identity & access | Auth sessions, employee identity links, roles, permissions, effective assignments, unit scope | Business transactions |
| Organisation & workforce | Zones, regions, shops, departments, reporting relationships, staff register, coverage assignments | Sales or ledger balances |
| Product catalogue | Product families, SKUs, variants, units of measure, barcodes, active status | Physical stock |
| Inventory | Shop/warehouse stock ledger, receipts, transfers, counts, adjustments, reservations, valuation snapshots | Sales payment settlement |
| Sales & returns | Sales, line items, discounts, tax snapshots, returns, exchanges, fulfilment status | Provider payment truth |
| Payments | Payment intents, provider references, callbacks, settlement and reconciliation status | Sale or expense approval policy |
| Finance & accountability | Allocations, advances, expenses, evidence, approvals, returns of funds, reconciliation, disbursements | Product and stock master data |
| Compliance | eTIMS integration contracts, invoice submissions/verification evidence, compliance status | Manual assertions of provider success |
| Reporting | Read models and aggregates by shop, region, zone, department and period | Authoritative writes |
| Platform administration | Configuration, feature controls, support access, operational audit | Unscoped impersonation or direct data mutation |

### Boundary rule

A business workflow may coordinate multiple domains, but writes must go through the owning domain's service/RPC. For example, completing a sale requests an inventory issue and records payment state; it must not directly edit a stock balance or declare a payment successful.

## 5. Organisation and workforce model

### Geographic structure

Use the existing organisation-unit hierarchy for the geographic/operational chain:

`Organisation → Zone → Region → Shop`

Warehouses or other stock locations may be represented as explicitly typed units/locations when confirmed by the operating model. Unit types remain data/configuration, not fixed enum limits.

### Departments

LPG, Inverter and Phone Retail are a separate business dimension, not children of the geographic tree. A person, shop or product may be associated with one or more departments subject to approved policy. Do not force department membership into the single-parent geographic hierarchy.

### Workforce and assignments

- Employee identity is separate from assignment.
- A person may hold multiple concurrent or acting assignments.
- Assignments are effective-dated and include role, geographic scope and optional department scope.
- Reporting relationships are explicit and effective-dated; they are not inferred from unit parentage.
- Expired, revoked or future assignments do not grant current access.
- A role maps to a controlled permission catalogue; user-editable metadata and client-supplied role strings never authorize an operation.
- Support/login-as-client must be time-bound, reason-coded, visibly indicated and audited with both the support actor and represented identity.

### Initial setup gate

Before employee import, Sun King must approve: unit codes/names, actual shop-to-region and region-to-zone mapping, department participation, role catalogue, employee identifiers, assignment dates, reporting lines, ATD/ASM coverage and exception handling.

## 6. Core operational workflows

### 6.1 Product and catalogue setup

1. Authorized catalogue manager creates/imports product families and SKUs.
2. Validate unique SKU/barcode, unit of measure, department, tax classification and active dates.
3. Preview import errors and duplicates before commit.
4. Changes to tax or product classification are versioned; completed sales retain their original snapshots.

### 6.2 Inventory receiving

1. Create a receipt against an approved source document or transfer.
2. Validate destination location, SKU, quantities, unit cost and reference.
3. Record a stock-receipt ledger event in one database transaction.
4. Update the stock projection atomically.
5. Attach supporting evidence and audit the receiving actor.
6. Duplicate requests with the same idempotency key return the original result, not a second receipt.

### 6.3 Stock transfer

1. Source location creates a transfer with line items.
2. Enforce available-to-transfer quantity and required approval thresholds.
3. Dispatch creates a source stock issue and an in-transit balance.
4. Receiving location confirms actual quantities.
5. Receipt creates destination stock receipt; discrepancies enter an exception workflow.
6. A transfer is never represented as a direct edit to two stock balances.

### 6.4 Sales

1. Resolve the authenticated employee's active shop and department scope.
2. Validate SKU status, price authority, stock availability and discount permissions.
3. Create sale header and immutable line snapshots.
4. Reserve or issue stock according to the configured fulfilment policy.
5. Create a payment intent; keep sale and payment state separate.
6. Confirm payment only from an authenticated provider callback or verified reconciliation.
7. On completion, commit the sale, inventory issue and audit event atomically where they share the database.
8. An uncertain provider outcome remains pending/reconciliation-required; do not retry a charge blindly.

### 6.5 Returns and exchanges

1. Reference the original completed sale and line quantities.
2. Validate return window, reason, condition and approval policy.
3. Record returned quantity into a designated inspection/quarantine or saleable location.
4. Record refund/credit as a separate payment event.
5. Exchanges are linked return and replacement-sale workflows; never silently overwrite the original sale.
6. Preserve original tax and price snapshots.

### 6.6 Finance and expense accountability

The existing expense and eTIMS accountability domain remains intact:

- allocations and disbursement batches;
- maker/checker approval;
- expense submission and evidence;
- manual invoice-review queue;
- returned-cash recording;
- reconciliation and audit.

Retail sales, stock movements and provider payments must not be represented as expenses. Cross-domain finance postings require an explicit, tested posting contract and reconciliation reference.

### 6.7 Compliance

Supplier invoice verification for expenses and eTIMS issuance/receipt for retail sales are distinct capabilities. The current manual-review expense workflow does not prove that customer sales eTIMS issuance is implemented.

Direct KRA integration remains gated on the actual approved API contract, onboarding, sandbox access and any required certification. Never guess endpoints or claim external verification from OCR, QR parsing or manual review.

## 7. Data model direction

The following is the logical model to validate against the live schema before writing migrations. Names are conceptual until the migration author checks existing tables, constraints, triggers and deployed function contracts.

### Existing foundation to preserve

- `organisations`, `org_units`, `employees`, `roles`, `role_permissions`
- `employee_assignments`, `reporting_relationships`
- `allocation_rules`, `allocations`
- `disbursement_batches`, `disbursement_items`, `fund_transactions`
- `expenses`, `invoice_verifications`, `evidence_files`
- `reconciliations`, `audit_events`
- enterprise onboarding/request tables and their server-side transition path

### New retail entities to design

- `departments`, `employee_department_assignments` (if department membership is many-to-many)
- `product_families`, `products` / `product_skus`, `product_price_versions`
- `stock_locations`, `stock_ledger_entries`, `stock_balances` (projection), `stock_reservations`
- `stock_receipts`, `stock_receipt_lines`
- `stock_transfers`, `stock_transfer_lines`
- `stock_counts`, `stock_count_lines`, `stock_adjustment_reasons`
- `sales`, `sale_lines`, `sale_discounts`
- `sale_returns`, `sale_return_lines`, `exchange_links`
- `payment_intents`, `payment_events`, `provider_callbacks`, `settlement_reconciliations`
- `integration_outbox` / `sync_receipts` for reliable external side effects and client sync

### Required database properties

- UUID primary keys and organisation-scoped uniqueness where applicable.
- Composite foreign keys ensure related rows belong to the same organisation.
- Explicit foreign keys from shops/locations to their approved unit.
- CHECK constraints for positive quantities, valid money values, states and effective date ranges.
- Append-only stock ledger and payment event history; correction is a compensating event, not mutation of historical facts.
- Stable idempotency key scoped to operation and organisation, with request hash to reject key reuse for different payloads.
- Atomic transaction for ledger event plus balance projection update.
- No client write permission to balance projections or provider-confirmed payment fields.
- All sensitive tables have intentional RLS policies or verified server-only grants. RLS enabled without a policy may be deliberate deny-by-default, but must be documented and tested.
- SECURITY DEFINER functions require fixed search_path, least-privilege execution grants, explicit auth checks, tenant/scope checks, permission checks, row locks where needed and negative tests.

Do not create all conceptual tables in one unreviewed migration. Deliver small additive migrations with rollback/forward-recovery notes and tests.

## 8. Application architecture

### Android field application

The Android app is the field execution client for shop staff and field leadership:

- sign-in and session recovery;
- role- and assignment-aware navigation;
- shop dashboard and assigned scope;
- stock lookup, receiving, transfers and counts;
- sales, returns and payment status;
- expense capture and evidence;
- offline draft/outbox and sync status;
- notifications and exception queues.

Local Room/database state is a cache and durable outbox, not the financial or inventory system of record. Every screen must visibly distinguish `Local draft`, `Sync pending`, `Server accepted`, `Needs review` and `Failed`. Retried requests retain the same idempotency key.

### React/Vite web console

The web console is the management and administration surface:

- organisation and unit configuration;
- workforce, roles, assignments and import preview;
- product/catalogue administration;
- stock oversight and transfer approvals;
- sales/returns oversight and reconciliations;
- finance review, allocations, expenses and disbursements;
- integration configuration and operational audit;
- reporting by shop, region, zone, department and time.

The web project already exists under `web/`; Vercel's root-directory/build configuration must be verified against the current project settings and deployed commit, not inferred from an old release note.

### Backend

Supabase remains the managed backend for Auth, Postgres, RLS, private Storage and Edge Functions. Domain modules live in the monolith. Use Edge Functions for provider integrations and trusted orchestration; keep core invariants in database constraints/transactions/RPCs so they cannot be bypassed by either client.

## 9. Security and operational controls

- No service-role key or provider secret in APK, web bundle, logs or repository.
- Separate test identities for shop associate, shop manager, ABM, regional roles, zone roles, finance maker/checker and platform administrator.
- Unit-scope authorization must be checked in addition to organisation scope.
- Cross-shop, cross-region and cross-department access tests are mandatory.
- Privileged support access requires approval, expiry and full audit attribution.
- Stock adjustments, discounts, refunds, write-offs, payment reconciliation and finance approvals require reason codes and configurable limits.
- Callback verification, replay protection, signature/token validation, provider event idempotency and reconciliation are mandatory.
- Backups include database and Storage evidence; restore both and verify referential integrity.
- PII minimisation, retention, export, incident response and data-residency decisions require sign-off before production.

## 10. Delivery sequence and release gates

### Phase 0 — baseline integrity (current)

- [ ] Reconcile open release-hardening PR #11 with current `main`; do not merge solely because it is mergeable.
- [ ] Confirm live migration history and deployed grants before applying any migration.
- [ ] Review six RLS-enabled no-policy billing/audit tables and document intentional server-only access.
- [ ] Review each flagged SECURITY DEFINER function individually.
- [ ] Enable leaked-password protection and confirm the security advisor clears.
- [ ] Run web build, Android unit tests/APK build and authenticated smoke tests from the current branch head.
- [ ] Confirm Vercel root, production deployment source commit and live HTTP response.

### Phase 1 — organisation and access foundation

- [ ] Provision a dedicated staging organisation with synthetic test data only.
- [ ] Build unit/department/role/permission/assignment configuration.
- [ ] Implement hierarchy-aware authorization and separate-session negative tests.
- [ ] Build validated import preview for units and employees; no implicit assignments.
- [ ] Replace synthetic dashboard data with server-backed scoped queries.

### Phase 2 — catalogue and inventory

- [ ] Approve SKU, unit-of-measure, barcode and department rules.
- [ ] Implement append-only stock ledger and transactional balance projection.
- [ ] Implement receipts, transfers, counts and adjustments.
- [ ] Test concurrency, idempotency, negative stock, transfer discrepancy and replay cases.

### Phase 3 — sales and returns

- [ ] Implement sale snapshots, discounts and authorization limits.
- [ ] Implement payment intent/event state machine and provider reconciliation.
- [ ] Implement returns, exchanges and refund linkage.
- [ ] Test partial payment, timeout, duplicate callback and charge uncertainty.

### Phase 4 — finance and compliance integration

- [ ] Complete existing expense lifecycle, evidence recovery and reconciliation.
- [ ] Validate Daraja sandbox callbacks and reconciliation before any production credential.
- [ ] Obtain and validate KRA's current authorised eTIMS contract for the intended sales/expense use case.
- [ ] Keep manual review separate from external verification.

### Phase 5 — pilot readiness

- [ ] Seed realistic synthetic data at 170-shop / 5-zone / 20-region scale.
- [ ] Run 1,500–2,000 user load and concurrency tests.
- [ ] Complete Android device/offline/recovery testing.
- [ ] Complete security review, backup/restore, monitoring, alerting and rollback rehearsal.
- [ ] Obtain Sun King approval of hierarchy, role matrix, workflows, data imports and pilot acceptance.

### Production acceptance rule

No production staff import or live financial/provider operation until all applicable Phase 0–5 gates are evidenced, signed off and linked to the release commit. A successful build or READY deployment alone is not production acceptance.

## 11. Architecture decisions and open validations

| Decision | Baseline | Validation still required |
|---|---|---|
| Deployment shape | Modular monolith | Confirm repository/module boundaries and release ownership |
| Initial organisation mode | One Sun King organisation | Confirm whether future customer onboarding remains out of current scope |
| Geographic hierarchy | Organisation → zone → region → shop | Confirm actual mappings and non-shop stock locations |
| Department model | Separate dimension | Confirm department-to-shop and employee membership rules |
| Inventory truth | Append-only ledger + balance projection | Approve costing/valuation method and negative-stock policy |
| Sales completion | Sale and payment state separated | Confirm cash, M-Pesa and any other accepted tender types |
| Stock transfer | Dispatch/in-transit/receive | Confirm approvals, discrepancy tolerances and source documents |
| Returns | Linked compensating workflow | Confirm return windows, quarantine and refund policy |
| Offline | Durable outbox with stable idempotency | Confirm permitted offline actions and expiry rules |
| eTIMS | Direct KRA first, fail closed | Obtain authoritative contract, credentials and certification path |
| Reporting | Read models by operational scope | Confirm KPI definitions and source-of-truth formulas |

No unresolved validation above should be silently replaced by a developer assumption.
