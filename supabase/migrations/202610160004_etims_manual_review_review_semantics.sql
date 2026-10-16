-- Tighten manual-review decision validation and keep verification review separate from expense approval.
create or replace function public.resolve_invoice_manual_review(
  p_queue_id uuid,
  p_decision text,
  p_reason text
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  q public.invoice_manual_review_queue%rowtype;
  e public.expenses%rowtype;
  next_verification_status text;
begin
  if auth.uid() is null or not public.has_permission('expenses.review') then
    raise exception 'permission denied' using errcode = '42501';
  end if;
  if p_decision is null or p_decision not in ('not_verified','needs_review') then
    raise exception 'manual review cannot certify an invoice as verified' using errcode = '22023';
  end if;
  if p_reason is null or length(trim(p_reason)) < 5 then
    raise exception 'a review note of at least 5 characters is required' using errcode = '22023';
  end if;

  select * into q from public.invoice_manual_review_queue
    where id = p_queue_id for update;
  if not found or q.organisation_id <> public.current_organisation_id() then
    raise exception 'review item not found' using errcode = 'P0002';
  end if;
  if q.status = 'resolved' then
    raise exception 'review item already resolved' using errcode = '22023';
  end if;

  select * into e from public.expenses
    where id = q.expense_id and organisation_id = q.organisation_id for update;
  if not found then raise exception 'expense not found' using errcode = 'P0002'; end if;
  if e.submitted_by = auth.uid() then
    raise exception 'submitter cannot review own expense' using errcode = '42501';
  end if;

  next_verification_status := case when p_decision = 'not_verified'
    then 'verification_failed' else 'needs_review' end;

  update public.invoice_manual_review_queue
    set status = 'resolved', outcome = p_decision, reviewed_by = auth.uid(),
        review_note = trim(p_reason), reviewed_at = now(), updated_at = now()
    where id = q.id;

  -- Do not mark the expense approved or populate its expense-review fields here.
  -- Manual invoice review is not KRA verification and is not expense approval.
  update public.expenses
    set verification_status = next_verification_status,
        status = 'needs_review',
        updated_at = now()
    where id = e.id;

  insert into public.audit_events(
    organisation_id, actor_user_id, action, entity_type, entity_id,
    before_data, after_data, reason
  ) values (
    q.organisation_id, auth.uid(), 'invoice_verification.manual_review_completed',
    'expense', e.id::text,
    jsonb_build_object('verification_status', e.verification_status, 'expense_status', e.status),
    jsonb_build_object('verification_status', next_verification_status,
      'expense_status', 'needs_review', 'queue_id', q.id, 'outcome', p_decision),
    trim(p_reason)
  );

  return jsonb_build_object(
    'queue_id', q.id, 'expense_id', e.id,
    'verification_status', next_verification_status,
    'expense_status', 'needs_review',
    'externally_verified', false
  );
end;
$$;

revoke all on function public.resolve_invoice_manual_review(uuid,text,text) from public, anon;
grant execute on function public.resolve_invoice_manual_review(uuid,text,text) to authenticated;
