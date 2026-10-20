# Implementation status and release plan

## Implemented in the production-completion branch

- [x] Versioned multi-tenant schema for organisations, units, employees, roles, effective assignments, allocations, expenses, evidence, verification, reconciliation, disbursements and audit events.
- [x] Baseline RLS policies and permission checks; maker/checker RPCs reject same-user submission/approval.
- [x] Server-side invoice verification function that fails closed when no authorised KRA/provider lookup contract is configured.
- [x] Manual invoice-review queue with organisation/permission scoping, maker-checker protection, audit events, and outcomes that cannot certify external verification.
- [x] Android email/password sign-in, encrypted local token storage, refresh-token recovery on app start, sign-out, and session-gated entry.
- [x] Authenticated expense submission using the employee/organisation linked to the signed-in account.
- [x] Receipt document picker for JPEG, PNG, WebP and PDF; 10 MB bound, content-signature validation, SHA-256 metadata, private Storage upload and evidence metadata insertion.
- [x] Separate invoice_date column and nullable VAT capture so missing VAT is not confused with an explicit zero.
- [x] Invoice modal no longer claims an expense is certified for KRA tax filing; it calls the authenticated verification function and reports only the returned result.
- [x] Daraja B2C submission/callback foundation that does not treat gateway acceptance as confirmed payment.
- [x] CI creates a temporary debug signing keystore and runs Android unit tests plus debug APK assembly.

## Added on feature/configurable-organisation-monolith

- [x] Architecture contract for one monolith with tenant-specific, configurable organisation structures, departments, roles, effective-dated assignments and approval workflows.
- [x] Initial Sun King configuration requirements documented as editable input data: 5 zones, 20 regions, 170 shops, listed role positions, 96 ATDs, 62 ASMs, and configurable LPG/Inverter/Phone Retail departments.
- [x] Secure enterprise request intake table with authenticated requester-only insert/read policies and no client-side update/delete path. A request does not create or activate a tenant.
- [ ] Build and secure the MobiWave-admin review/provisioning operation; validate transitions and write audit events.
- [ ] Build organisation/unit/department/role/assignment/workflow configuration screens and validated CSV/XLSX import.
- [ ] Validate migration against the actual Supabase project and existing schema before applying it. The migration has not been applied or remotely tested by this commit.

## Still required before production

- [ ] Confirm CI is green on the latest commit and perform a real device/emulator smoke test.
- [ ] Configure SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in the Android build secrets. Do not use a service-role key in the APK.
- [ ] Provision Sun King's organisation, employees, roles, permissions and assignments through a controlled administrator workflow; do not fabricate unconfirmed reporting lines.
- [ ] Replace remaining synthetic dashboard, advances, approvals and organisation figures with authenticated backend queries.
- [ ] Complete the full expense lifecycle: finance acceptance/rejection, correction history, returned-cash recording, period reconciliation and ledger reports.
- [ ] Add a robust durable offline draft/outbox and idempotent retry/sync flow. Offline submission currently fails closed rather than pretending to save.
- [ ] Add camera capture, malware scanning and orphaned-storage cleanup. The current evidence workflow uses document selection and file-signature checks; it does not malware-scan files.
- [ ] Add automated RLS and hierarchy tests using multiple authenticated users across at least two organisations and multiple roles.
- [ ] Configure and validate Safaricom Daraja sandbox credentials, callback reachability/authenticity, retry semantics and reconciliation. No payment credentials are present, and no live transfer is enabled.
- [ ] Confirm direct KRA invoice lookup availability or approve a provider contract; test authoritative positive, negative, partial, timeout and malformed responses before external verification can be enabled.
- [ ] Add load tests for 1,500–2,000 users, dependency/secret scanning, alerts, backup/restore tests, release signing and rollback.
- [ ] Approve data residency, retention, deployment and client security requirements.

## Release boundary

This branch contains the architecture documentation and an onboarding-request migration proposal. The migration has not been applied or validated against the live Supabase schema, the admin review workflow and configuration UI are not implemented, and this is not a production release. No provider credentials or live money movement are implied.

## Implementation tracks

1. Prototype stability and truthful status messaging.
2. Authenticated persistence, private evidence storage and offline synchronization.
3. Finance workflow, correction history, returns and reconciliation ledger.
4. Trusted eTIMS and Daraja integrations.
5. Configurable organisation, roles, assignments, enterprise onboarding and imports.
6. Automated security, load, recovery and release validation.
