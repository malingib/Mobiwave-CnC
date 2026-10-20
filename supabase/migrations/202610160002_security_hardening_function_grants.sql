-- Harden function grants found during post-migration verification.
-- The API's anon role had explicit EXECUTE privileges despite PUBLIC revokes.
revoke all on function public.current_organisation_id() from public, anon;
revoke all on function public.has_permission(text) from public, anon;
revoke all on function public.approve_disbursement_batch(uuid) from public, anon;
revoke all on function public.prepare_disbursement_submission(uuid) from public, anon;
revoke all on function public.submit_disbursement_batch_for_approval(uuid) from public, anon;
revoke all on function public.review_expense(uuid,text,text) from public, anon;
revoke all on function public.record_fund_adjustment(uuid,text,numeric,text,text,uuid) from public, anon;
revoke all on function public.prevent_audit_mutation() from public, anon, authenticated;

grant execute on function public.current_organisation_id() to authenticated;
grant execute on function public.has_permission(text) to authenticated;
grant execute on function public.approve_disbursement_batch(uuid) to authenticated;
grant execute on function public.prepare_disbursement_submission(uuid) to authenticated;
grant execute on function public.submit_disbursement_batch_for_approval(uuid) to authenticated;
grant execute on function public.review_expense(uuid,text,text) to authenticated;
grant execute on function public.record_fund_adjustment(uuid,text,numeric,text,text,uuid) to authenticated;

alter function public.prevent_audit_mutation() set search_path = public;
