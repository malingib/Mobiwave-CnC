# Implementation status and release plan

## Approved product scope

MobiWave CnC is the foundation of the complete Sun King retail operations platform. The existing Expense & eTIMS Accountability capability remains a distinct finance domain within the same modular monolith. Current rollout scope is one provisioned Sun King organisation; do not expand into multi-customer SaaS onboarding without a separate decision.

See [Retail Operations Foundation](RETAIL_OPERATIONS_FOUNDATION.md) for the approved domain architecture, data model direction, workflow boundaries and release gates.

## Implemented foundation (repository and connected project)

- [x] Versioned organisation, unit, employee, role, assignment, reporting, allocation, expense, evidence, invoice review, reconciliation, disbursement and audit schema.
- [x] Tenant/organisation integrity constraints and normalized identifiers on the existing foundation.
- [x] Baseline RLS and permission checks; maker/checker RPCs reject same-user submission/approval.
- [x] Server-side invoice verification fails closed without an authorised KRA/provider lookup contract.
- [x] Manual invoice-review queue with organisation/permission scoping, maker-checker protection and auditable outcomes that cannot certify external verification.
- [x] Android email/password sign-in, encrypted local token storage, refresh-token recovery, sign-out and session-gated entry.
- [x] Authenticated expense submission using the employee/organisation linked to the signed-in account.
- [x] Receipt document picker with file bounds, content-signature validation, SHA-256 metadata and private Storage evidence.
- [x] Daraja B2C submission/callback foundation that does not treat gateway acceptance as confirmed payment.
- [x] React/Vite web application under `web/`; production build/deployment configuration was corrected in the October release work.
- [x] Android and web CI workflows exist in the repository.

## Current gaps — do not describe as complete

- [ ] Validate current migration history and deployed grants against the live project before any further migration.
- [ ] Review the six RLS-enabled billing/audit tables without policies; preserve intentional deny-by-default/server-only access and document each access path.
- [ ] Review every flagged SECURITY DEFINER function individually for fixed search_path, organisation/unit scope, permissions, least-privilege grants and negative tests.
- [ ] Enable leaked-password protection and confirm the security advisor clears.
- [ ] Build organisation/unit/department/role/assignment/workflow configuration screens and validated imports.
- [ ] Provision a synthetic staging organisation and test identities; actual Sun King employee import remains on hold.
- [ ] Replace synthetic Android dashboard and operational data with authenticated, scoped queries.
- [ ] Complete finance lifecycle: review, correction history, returned-cash recording, period reconciliation and ledger reports.
- [ ] Implement durable offline drafts/outbox and stable idempotent retry/sync; offline submission must not be represented as server committed.
- [ ] Implement retail product catalogue, stock ledger, receipts, transfers, counts, sales, returns and payment reconciliation.
- [ ] Validate Daraja sandbox callbacks, retries, replay handling and reconciliation; no live transfers until explicitly approved.
- [ ] Obtain the current KRA eTIMS contract for the intended use case; do not guess a supplier-invoice lookup or sales-issuance endpoint.
- [ ] Complete device/emulator smoke tests, security tests, load tests, monitoring, backup/restore, release signing and rollback rehearsal.

## Current release boundary

A READY deployment or successful build is not proof that all authenticated application workflows are production-ready. The web app is present under `web/`; verify Vercel's configured root, deployed source commit and live HTTP responses against the current project settings. The connected Supabase project previously showed no live application rows, so populated-tenant authorization and operational workflows remain unproven.

No provider credentials, Sun King staff import or live money movement are implied by the implemented foundation.

## Delivery tracks

1. Baseline integrity, release hardening and security verification.
2. Organisation, department, hierarchy, roles, assignments and import preview.
3. Real authenticated operational data and complete finance workflows.
4. Product catalogue and ledger-based inventory.
5. Sales, returns, payment state machine and reconciliation.
6. KRA eTIMS and Daraja integration validation.
7. Scale, offline recovery, security, backup/restore and pilot acceptance.

## Production release rule

No production staff import or live financial/provider operation until the relevant acceptance gates are evidenced, signed off and linked to the release commit. Never merge a PR solely because it is mergeable; require all applicable CI, migration, security and acceptance checks to pass.
