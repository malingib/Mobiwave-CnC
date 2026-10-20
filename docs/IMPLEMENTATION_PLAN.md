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

## Configurable organisation and onboarding foundation

- [x] Documented single-monolith, tenant-configurable organisation model and Sun King's initial configuration inputs.
- [x] Enterprise request intake migration with requester-only insert/read RLS; request submission does not create or activate a tenant.
- [x] Server-only SQL transition function with legal transition checks, row locking, bounded reason text and append-only request transition events.
- [x] Edge Function for MobiWave-admin review transitions; validates a Supabase-authenticated user and requires trusted app_metadata admin claim. The service-role key is server-side only.
- [x] JWT verification enabled for review-enterprise-request in Supabase config.
- [ ] Validate the migrations and function against the actual project/schema in staging, including the organisation table naming and activation check.
- [ ] Build the registration/request UI and MobiWave-admin review UI.
- [ ] Build organisation/unit/department/role/assignment/workflow configuration screens and validated CSV/XLSX import.
- [ ] Provision Sun King's verified organisation and staff data; do not invent unconfirmed reporting lines.
- [ ] Replace synthetic dashboard values with authenticated tenant-scoped queries and finish the full finance workflow.

## Still required before production

- [ ] Confirm CI is green on the latest commit and perform a real device/emulator smoke test.
- [ ] Configure SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in Android build secrets. Never place a service-role key in the APK.
- [ ] Automated RLS and hierarchy tests using separate authenticated identities across at least two organisations and multiple roles.
- [ ] Full expense lifecycle: finance acceptance/rejection, correction history, returned-cash recording, period reconciliation and ledger reports.
- [ ] Durable offline draft/outbox and idempotent retry/sync; offline submission currently fails closed rather than pretending to save.
- [ ] Camera capture, malware scanning and orphaned-storage cleanup.
- [ ] Configure and validate Safaricom Daraja sandbox credentials, callback authenticity, retries and reconciliation. No live transfer is enabled.
- [ ] Confirm direct KRA invoice lookup availability or approve a provider contract; test authoritative positive, negative, partial, timeout and malformed responses.
- [ ] Load tests for 1,500–2,000 users, dependency/secret scanning, alerts, backup/restore, release signing and rollback.
- [ ] Approve data residency, retention, deployment and client security requirements.

## Release boundary

The onboarding foundation has not been applied or validated against the live Supabase project. The Edge Function has not been deployed or integration-tested; no authenticated end-to-end tenant provisioning test has been run. The Vercel preview currently fails before build because the linked project specifies a non-existent `web` root directory; the repository is an Android/Supabase project and has no verified web app directory at that path. This is not a production release. No provider credentials or live money movement are implied.

## Implementation tracks

1. Prototype stability and truthful status messaging.
2. Authenticated persistence, private evidence storage and offline synchronization.
3. Finance workflow, correction history, returns and reconciliation ledger.
4. Trusted eTIMS and Daraja integrations.
5. Configurable organisation, roles, assignments, enterprise onboarding and imports.
6. Automated security, load, recovery and release validation.
