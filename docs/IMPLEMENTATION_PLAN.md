# Implementation status and release plan

## Changes in this foundation branch

- [x] Create a versioned multi-tenant schema for organisations, org units, employees, roles, assignments, acting appointments, reporting relationships, allocations, expenses, evidence, verification, reconciliation, disbursements and audit events.
- [x] Add baseline RLS policies and permission checks; maker/checker RPCs reject same-user approval/submission.
- [x] Add server-side invoice verification adapter that never treats QR/OCR as verification and fails closed when no authorised provider is configured.
- [x] Add server-side Daraja B2C submission and callback reconciliation foundation; no client-side action claims to send money.
- [x] Add CI workflow and security/integration documentation.
- [x] Fix the local prototype's misleading success states for eTIMS and B2C.
- [ ] Apply and validate migration in a dedicated staging project.
- [ ] Build and run Android tests in CI; fix any build/test failures.
- [ ] Add real sign-in/session management to Android and wire UI screens to authenticated backend APIs.
- [ ] Add employee/org administration UI and CSV/XLSX import with preview and validation.
- [ ] Add secure receipt camera/file upload, file signature checks, malware scanning and durable offline drafts.
- [ ] Complete expense review, correction history, returned-cash recording and reconciliation ledger transactions.
- [ ] Test role and hierarchy scope policies with multiple users and multiple organisations.
- [ ] Validate actual Daraja sandbox credentials, request/response schema, callback authentication, retries and reconciliation.
- [ ] Integrate an authorised eTIMS verification source or deliver a controlled manual verification workflow.
- [ ] Add load tests for the target 1,500–2,000 users, security tests, alerts, backup/restore tests and release signing.
- [ ] Approve data residency, retention, dedicated deployment and client security requirements before production.

## Six implementation tracks

1. Prototype stability: clear demo states, unique expense IDs, current dates, Android CI.
2. Backend persistence: schema, migrations, Auth linkage, RLS, private evidence storage.
3. Financial domain: allocation/disbursement/expense/accepted expense/returned cash ledger separation and idempotency.
4. Trusted integrations: eTIMS provider adapter and Daraja B2C/callback reconciliation.
5. Configurable organisation: data-driven units, roles, effective assignments, acting roles, reporting, import.
6. Quality and release: automated tests, security validation, staging, monitoring, backups and rollback.

## Known blockers / not claimed as complete

- No Supabase project has been linked or migrated by this change.
- No provider secrets or live credentials are present.
- The Android app is not yet connected to Supabase and still uses in-memory sample state in many screens.
- The web controllership screen is currently a Compose wide-screen view, not a separately deployable browser app.
- The eTIMS endpoint contract and Daraja callback behavior must be confirmed against the authorised providers.
- No successful build, device test, live integration test or production security review is claimed by this commit.
