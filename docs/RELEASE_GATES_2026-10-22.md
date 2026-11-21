# MobiWave CnC release gates — 22 October 2026

## Verified live change

The least-privilege grants migration was applied to Supabase project `uzfsbjfkmicorrchulfs`. Verification showed no `anon` grants on CNC billing/audit tables and authenticated users have SELECT only on the six CNC billing/audit tables. Authenticated write grants were removed. The equivalent SQL is tracked in `supabase/migrations/20261022170000_cnc_least_privilege_table_grants.sql`; do not replay manually against the already-updated database.

## Remaining release blockers

1. Billing tables have RLS enabled but no policies. This is fail-closed. Add organisation-scoped read policies only after confirming the user-to-organisation membership model; keep writes server-side.
2. Wire subscription, invoice and payment history to persisted data, and verify plan limits at database operations.
3. Manual payment records must remain distinct from provider-confirmed payments. Verify callback authenticity, amount/currency/reference matching, idempotency and replay handling.
4. Review all ten SECURITY DEFINER functions callable by authenticated users individually for fixed search_path, organisation scoping and permission checks. Do not blanket-revoke RPCs used by app workflows.
5. Supabase Auth leaked-password protection is disabled. Enable in Auth password/security settings and verify the advisor clears; no connected setting-management action was available here.
6. Run separate-session tests for platform admins and ordinary workspace users, including cross-organisation denial and billing mutation denial. Current database tables were empty during this pass.
7. Preserve single-organisation mode. Validate organisation structure before import; Sun King's staff import remains on hold.
8. Vercel project `mobiwavecnc` targets a `web` directory absent from this Android/Supabase repository. Project metadata reports `live: false`; a READY deployment is not proof of a usable production billing UI.

## Verified status

The `verify-invoice`, `cnc-admin-console`, and `review-enterprise-request` Edge Functions are ACTIVE with JWT verification enabled. Supabase security advisors still report six RLS-enabled billing/audit tables without policies and ten callable SECURITY DEFINER functions. The current main commit has a successful Vercel status check, but the web-root mismatch remains unresolved.

**Release decision: not production-ready.** Privilege-grant hardening is complete. Billing UI/data policies, access regression tests, function review, leaked-password protection, correct web source/deployment and Android device validation remain open. No live money movement or Sun King staff import is authorised by this change.
