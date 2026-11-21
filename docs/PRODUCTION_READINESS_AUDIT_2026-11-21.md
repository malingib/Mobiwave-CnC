# MobiWave CnC production-readiness audit

**Audit date:** 2026-11-21  
**Repository baseline:** `main` at `1aed510c217927a06cb3c1c0155238f17687bcd0`  
**Approved product scope:** one provisioned Sun King organisation in a modular monolith  
**Release decision:** not production-ready

## Executive result

The repository has a working Android/web build foundation and a substantial expense/eTIMS accountability backend. It does not yet implement the approved complete retail operations platform. The most important current risks are deployment/source drift, an active untracked platform-admin Edge Function that still supports cross-organisation operations, unverified live database security state, synthetic Android demo data, and incomplete operational domains.

## Changes completed during this audit

- PR #17 merged after both Android CI workflows passed. Release builds now start with empty demo collections and do not expose the hardcoded sample profile/financial metrics; debug builds retain labelled demo fixtures. Merge: `56eac0cb`.

- PR #14 merged: approved the Sun King retail operations foundation and single-organisation scope.
- PR #16 merged: removed the active multi-customer admin dashboard and enterprise admission UI from the web application. Platform-admin accounts now enter the provisioned workspace rather than the obsolete cross-customer console. Web build and Android CI passed.
- PR #17 opened: gates synthetic Android sample data to debug builds, adds a persistent demo warning, and removes the inaccurate "Online (Sync auto-active)" status. Android CI is still pending at the time of this audit.

## Verified repository and deployment observations

### GitHub and CI

- Default branch is `main`.
- Latest main commit at audit time: `1aed510c217927a06cb3c1c0155238f17687bcd0`.
- The preceding main commit `7bc4d47` had successful Android CI, web build and Vercel deployment checks.
- PR #16 passed web build and both Android checks before merge.
- PR #17 is open and awaits Android CI.

### Supabase deployed Edge Functions

The connected project `uzfsbjfkmicorrchulfs` reports these active deployed functions. The Supabase control plane reports `ACTIVE_HEALTHY`, but the database connection itself continues to time out; a restore request reported that the project was already active and may still be waking. A simple `SELECT now()` also timed out. Do not consider database restoration complete until SQL, migrations and advisors respond successfully.

| Function | Live version | Source status |
|---|---:|---|
| `verify-invoice` | 4 | Tracked in repository |
| `cnc-admin-console` | 6 | **Not tracked in current repository** |
| `review-enterprise-request` | 3 | Tracked in repository |

The repository contains `daraja-callback` and `submit-b2c-batch`, but they were not present in the deployed function listing returned during this audit. Do not assume live B2C capability from source files alone. The deployed `verify-invoice` function version 4 correctly fails closed: it explicitly states that no authorised KRA supplier-invoice lookup contract has been confirmed, marks the expense for manual review, and never claims an external verification occurred.

The active, untracked `cnc-admin-console` function source was retrieved and reviewed. It authenticates users with `auth.getUser(token)` and checks trusted `app_metadata` for `super_admin`, then creates a Supabase service-role client. It contains actions for listing/inviting super-admins, listing and creating client organisations, creating demo organisations, viewing client expenses, changing organisation status, managing plans/subscriptions/invoices/payments, confirming payments through `cnc_confirm_payment`, and reading billing audit logs. This conflicts with the approved single-organisation Sun King rollout and is deployment/source drift that must be resolved. The function also sets `Access-Control-Allow-Origin: *`; while it checks a trusted admin claim, the broad origin policy should be replaced with a controlled allowlist during the staged remediation.

**Critical integrity issue:** the function's `create_demo_client` action inserts an illustrative expense with `verification_status = externally_verified`. That is not an authoritative provider result and conflicts with the fail-closed eTIMS contract. Although the action is restricted to trusted super-admin claims, it must not be used or exposed as a production workflow. Its audit helper only logs audit-write failures to the function console and continues, so a successful privileged mutation can be returned even when its audit record was not persisted. This is another release blocker for privileged operations.

Do not delete or replace the deployed function until its callers, database objects, audit dependencies and rollback path are understood. First capture and review the deployed source, then stage a restricted replacement and decommission obsolete actions deliberately.

### Live database inspection limitation

The Supabase control plane currently reports `ACTIVE_HEALTHY`, and the explicit restore call reports that the project is no longer paused and may take time to fully restore. Nevertheless, repeated SQL-backed inspection, migration listing, table listing and advisor calls still fail with connection timeouts or hibernation responses. A minimal read-only `SELECT now()` also timed out. Migration history, live table grants, RLS policies and security/performance advisors therefore remain unverified. No SQL or production database changes were made.

### Open PR #11 — do not merge blindly

PR #11 records a least-privilege migration that its description says was already applied to the connected project. It adds:
- broad privilege revocations for `anon` and selected authenticated operations;
- a read policy for active `cnc_plans`;
- a release-gate document describing RLS, SECURITY DEFINER, leaked-password-protection and billing gaps.

The equivalent SQL is not on current `main`. Because live migration history could not be queried, do not merge/replay this migration until the migration ledger and existing policy are checked. In particular, avoid replaying a non-idempotent `CREATE POLICY` statement against a database where it may already exist.

### Authorization context requiring a staging test

The existing `current_organisation_id()` function derives organisation scope from an active employee row linked to `auth.uid()`. A platform administrator who is not also linked to an active Sun King employee assignment will not automatically receive an organisation context. PR #16 routes platform administrators into the normal workspace; therefore test a real platform-admin identity before claiming organisation configuration access works. Do not solve this by trusting a client-supplied organisation UUID or by granting unscoped service-role access in the browser.

## Confirmed implementation gaps

1. Replace hardcoded Android profile, allocation, approval, advance, inventory and zone metrics with authenticated server-backed data. PR #17 prevents release builds from showing the current fixtures but does not implement live profile loading.
2. Implement the approved organisation/department/role/assignment and unit-scope model with separate-session authorization tests.
3. Complete finance review, correction history, returned-cash recording, period reconciliation and ledger reporting.
4. Implement catalogue, SKU/price versioning, stock ledger, receipts, transfers, counts and adjustments.
5. Implement sales, returns/exchanges, payment intents/events, provider callbacks and settlement reconciliation.
6. Implement durable offline drafts/outbox, stable idempotency keys and explicit local-pending/server-committed states.
7. Reconcile deployed Edge Functions against source control and validate CORS, JWT verification, service-role use, callback authenticity and replay handling.
8. Review every SECURITY DEFINER function individually; verify fixed search_path, least-privilege grants, organisation/unit scope and negative tests.
9. Enable leaked-password protection and verify the Supabase security advisor.
10. Complete staging, real-device, load, backup/restore, monitoring, release-signing and rollback validation.

## Immediate next sequence

1. Complete PR #17 Android CI and merge only if green.
2. Wake/restore the Supabase project only through an approved operational window; then inspect migration history, grants, RLS and advisors without applying changes.
3. Capture `cnc-admin-console` source in version control and prepare a staged restricted replacement; do not deploy it directly to production.
4. Verify platform-admin and ordinary-user organisation context with dedicated staging identities.
5. Implement Phase 1 organisation/access and real-data workflows.
6. Continue into catalogue/inventory, then sales/returns/payments, preserving finance as a separate domain.

## Production boundary

No real Sun King staff import, live payment movement, direct KRA/eTIMS activation or production rollout is authorized by this audit. A successful CI build or READY Vercel deployment is not production acceptance.
