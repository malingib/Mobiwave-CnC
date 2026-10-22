-- Keep platform billing and audit tables server-managed.
-- Applied to Supabase project uzfsbjfkmicorrchulfs on 2026-10-22.
-- This repository copy records the production migration; do not replay manually
-- against a database where the equivalent migration has already been applied.
begin;
revoke all privileges on all tables in schema public from anon;
revoke truncate, references, trigger on all tables in schema public from authenticated;

revoke insert, update, delete, truncate, references, trigger
  on table public.cnc_subscriptions from authenticated;
revoke insert, update, delete, truncate, references, trigger
  on table public.cnc_invoices from authenticated;
revoke insert, update, delete, truncate, references, trigger
  on table public.cnc_payments from authenticated;
revoke insert, update, delete, truncate, references, trigger
  on table public.cnc_audit_logs from authenticated;
revoke insert, update, delete, truncate, references, trigger
  on table public.cnc_plans from authenticated;
revoke insert, update, delete, truncate, references, trigger
  on table public.cnc_enterprise_request_events from authenticated;
commit;
