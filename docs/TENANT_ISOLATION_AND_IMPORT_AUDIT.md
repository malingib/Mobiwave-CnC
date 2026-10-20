# Tenant isolation and enterprise onboarding audit

Audit date: 2026-10-20  
Repository baseline: `main` at `254ca9c5e94c0d7141617e1574a28efef554a72f`  
Database: connected MobiWave-CnC Supabase project

## Findings and changes

### Database tenant integrity

The initial schema used single-column foreign keys for relationships where each row separately carried an `organisation_id`. Row-level security on the child row did not itself guarantee that the referenced employee, role, unit, allocation, disbursement batch or invoice belonged to the same tenant.

Added composite foreign keys and validated them for:
- unit parent links;
- assignment employee, role, unit and acting-for assignment;
- reporting employee, manager and unit;
- allocation rule employee, role and unit;
- allocation employee, unit and rule;
- disbursement item batch and employee;
- expense employee, unit, allocation and disbursement item;
- fund transaction employee, allocation and disbursement item;
- reconciliation employee;
- evidence file, invoice verification and manual review queue expense links;
- payment invoice link.

Composite unique indexes support the same-tenant foreign keys. The migration was applied to the connected database and all new constraints report as validated. The audited organisation/employee/configuration/finance tables had zero estimated live rows, and the enterprise request and event tables were empty; therefore there was no existing populated tenant dataset to exercise these constraints against.

A second migration adds case/whitespace-normalized uniqueness for employee numbers, unit codes and role keys, and a database trigger rejecting self-parenting and cyclic organisation-unit trees. Both migrations have been applied to the connected database.

### Employee CSV import

The organisation configuration UI now validates:
- required and unique column names;
- required employee number and full name;
- unsupported columns and malformed row column counts;
- optional email format and E.164 phone format;
- allowed status values;
- duplicate employee numbers case-insensitively within the file and against the current tenant's employee register;
- file size and batch-size limits already present in the UI.

The import still creates employee identity records only. It does not infer assignments, roles, reporting lines, zone/region coverage, or department membership. A database uniqueness index remains the final race-safe duplicate guard.

### Enterprise request API

The Edge Function uses Supabase Auth `getUser(token)` and trusted `app_metadata` claims; it does not trust request JSON or user-editable `user_metadata` for platform-admin authorization. Missing/invalid sessions should return 401; valid non-admin sessions should return 403. Admin list requests use the service role only after the trusted claim check. Status transitions go through a server-only RPC that locks the request row, checks legal transitions and writes an audit event.

The enterprise request and event tables exist in the connected database. The transition RPC is granted to `service_role` only. The request/event tables currently contain no rows.

## Remaining live-session verification

A real authenticated positive/negative test has not been completed in this environment because no test-user access tokens or staging test identities were available to this audit. Do not use a real customer request or production admin account for destructive status-transition testing.

Before calling this area production-verified, run the following with dedicated staging identities:
1. No bearer token and malformed/expired token: expect 401.
2. Valid authenticated non-admin token, including a user whose `user_metadata` claims admin: expect 403.
3. Valid trusted platform-admin token: list succeeds with HTTP 200 and a `requests` array.
4. Admin moves a seeded test request through legal transitions; each transition returns 200 and appends exactly one audit event.
5. Attempt an illegal transition and activation with an unknown organisation UUID; both are rejected and leave status/audit state unchanged except for the documented failed-attempt handling.
6. Verify requester sessions can read only their own request rows; ordinary authenticated users cannot read or mutate the audit-event table or call the transition RPC.
7. Remove/expire test access and repeat to confirm revoked/expired sessions are rejected.

Do not populate Sun King's actual staff data until these tests pass and the organisation hierarchy, role matrix, assignment dates, department coverage and manager/reporting mappings have been reviewed with the customer.

## Separate Supabase advisor items

The security advisor still reports six RLS-enabled tables without explicit policies: `cnc_audit_logs`, `cnc_enterprise_request_events`, `cnc_invoices`, `cnc_payments`, `cnc_plans` and `cnc_subscriptions`. For the request-event table and billing/payment tables, deny-by-default may be intentional; verify table grants and each caller before changing policy. The advisor also flags security-definer RPCs callable by authenticated users. Several are intentionally authenticated RPCs with in-function permission and tenant checks, but they require per-function negative tests rather than blanket grant changes.

The current connected database shows zero estimated rows across the public application tables, so these checks should be repeated after a staging dataset is seeded and before customer import.
