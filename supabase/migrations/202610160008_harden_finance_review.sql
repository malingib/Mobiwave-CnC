-- Harden the existing finance review RPC and make manual return references tenant-scoped.
create or replace function public.review_expense(
  p_expense_id uuid,
  p_decision text,
  p_reason text default null
) returns jsonb
language plpgsql
security definer
set search_path = public
as $function$
declare
  e public.expenses%rowtype;
  next_status text;
begin
  if auth.uid() is null or not public.has_permission('expenses.review') then
    raise exception 'permission denied' using errcode = '42501';
  end if;
  if p_expense_id is null or p_decision is null
     or p_decision not in ('accept', 'reject', 'needs_review')
     or p_reason is null or length(trim(p_reason)) < 5 then
    raise exception 'valid review decision and reason required' using errcode = '22023';
  end if;

  select * into e
  from public.expenses
  where id = p_expense_id
    and organisation_id = public.current_organisation_id()
  for update;

  if not found then raise exception 'expense not found' using errcode = 'P0002'; end if;
  if e.submitted_by = auth.uid() then
    raise exception 'submitter cannot review own expense' using errcode = '42501';
  end if;
  if e.status not in ('submitted', 'needs_review') then
    raise exception 'expense is not awaiting review' using errcode = '22023';
  end if;
  if p_decision = 'accept' and e.verification_status <> 'externally_verified' then
    raise exception 'expense requires authoritative invoice verification before acceptance' using errcode = '22023';
  end if;

  next_status := case p_decision
    when 'accept' then 'accepted'
    when 'reject' then 'rejected'
    else 'needs_review'
  end;

  update public.expenses
  set status = next_status,
      reviewed_by = auth.uid(),
      reviewed_at = now(),
      review_note = trim(p_reason),
      updated_at = now()
  where id = e.id;

  if p_decision = 'accept' then
    insert into public.fund_transactions (
      organisation_id, employee_id, allocation_id, transaction_type, amount, currency,
      external_reference, effective_at, created_by, metadata
    ) values (
      e.organisation_id, e.employee_id, e.allocation_id, 'expense_accepted', e.gross_amount, e.currency,
      'EXPENSE:' || e.id::text, now(), auth.uid(),
      jsonb_build_object('expense_id', e.id, 'accepted_by', auth.uid())
    )
    on conflict (external_reference) where external_reference is not null do nothing;
  end if;

  insert into public.audit_events (
    organisation_id, actor_user_id, action, entity_type, entity_id, before_data, after_data, reason
  ) values (
    e.organisation_id, auth.uid(), 'expense.reviewed', 'expense', e.id::text,
    jsonb_build_object('status', e.status, 'verification_status', e.verification_status),
    jsonb_build_object(
      'status', next_status,
      'reviewed_at', now(),
      'externally_verified', e.verification_status = 'externally_verified'
    ),
    trim(p_reason)
  );

  return jsonb_build_object(
    'expense_id', e.id,
    'status', next_status,
    'externally_verified', e.verification_status = 'externally_verified'
  );
end
$function$;

create or replace function public.record_cash_return(
  p_employee_id uuid,
  p_amount numeric,
  p_reference text,
  p_reason text,
  p_effective_at timestamptz default now()
) returns jsonb
language plpgsql
security definer
set search_path = public
as $function$
declare
  org_id uuid;
  transaction_id uuid;
  ref text;
begin
  if auth.uid() is null or not public.has_permission('reconciliations.manage') then
    raise exception 'permission denied' using errcode = '42501';
  end if;
  if p_employee_id is null or p_amount is null or p_amount <= 0
     or p_reference is null or length(trim(p_reference)) < 3
     or p_reason is null or length(trim(p_reason)) < 5
     or p_effective_at is null then
    raise exception 'valid employee, positive amount, reference, date and reason required' using errcode = '22023';
  end if;

  org_id := public.current_organisation_id();
  if not exists (
    select 1 from public.employees e
    where e.id = p_employee_id and e.organisation_id = org_id and e.status = 'active'
  ) then
    raise exception 'employee not found in current organisation' using errcode = 'P0002';
  end if;

  ref := org_id::text || ':cash-return:' || trim(p_reference);
  insert into public.fund_transactions (
    organisation_id, employee_id, transaction_type, amount, currency,
    external_reference, effective_at, created_by, metadata
  ) values (
    org_id, p_employee_id, 'cash_returned', p_amount, 'KES',
    ref, p_effective_at, auth.uid(),
    jsonb_build_object('reference', trim(p_reference), 'reason', trim(p_reason), 'recorded_by', auth.uid())
  )
  returning id into transaction_id;

  insert into public.audit_events (
    organisation_id, actor_user_id, action, entity_type, entity_id, reason, after_data
  ) values (
    org_id, auth.uid(), 'cash_return.recorded', 'fund_transaction', transaction_id::text,
    trim(p_reason), jsonb_build_object('employee_id', p_employee_id, 'amount', p_amount, 'reference', trim(p_reference))
  );

  return jsonb_build_object('transaction_id', transaction_id, 'recorded', true, 'transfer_sent', false);
exception
  when unique_violation then
    raise exception 'cash return reference already exists in this organisation' using errcode = '23505';
end
$function$;

revoke all on function public.review_expense(uuid, text, text) from public, anon;
revoke all on function public.record_cash_return(uuid, numeric, text, text, timestamptz) from public, anon;
grant execute on function public.review_expense(uuid, text, text) to authenticated;
grant execute on function public.record_cash_return(uuid, numeric, text, text, timestamptz) to authenticated;
