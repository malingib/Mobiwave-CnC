-- Complete the server-side finance review, cash-return and reconciliation foundation.
create unique index if not exists reconciliations_employee_period_unique
  on public.reconciliations (organisation_id, employee_id, period_start, period_end);

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

  ref := 'cash-return:' || trim(p_reference);
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
    raise exception 'cash return reference already exists' using errcode = '23505';
end
$function$;

create or replace function public.rebuild_reconciliation(
  p_employee_id uuid,
  p_period_start date,
  p_period_end date
) returns jsonb
language plpgsql
security definer
set search_path = public
as $function$
declare
  org_id uuid;
  opening numeric(14,2);
  disbursed numeric(14,2);
  accepted numeric(14,2);
  returned numeric(14,2);
  adjustment numeric(14,2);
  outstanding numeric(14,2);
  reconciliation_id uuid;
  current_status text;
begin
  if auth.uid() is null or not public.has_permission('reconciliations.manage') then
    raise exception 'permission denied' using errcode = '42501';
  end if;
  if p_employee_id is null or p_period_start is null or p_period_end is null or p_period_end < p_period_start then
    raise exception 'valid employee and period required' using errcode = '22023';
  end if;

  org_id := public.current_organisation_id();
  if not exists (
    select 1 from public.employees e
    where e.id = p_employee_id and e.organisation_id = org_id and e.status = 'active'
  ) then
    raise exception 'employee not found in current organisation' using errcode = 'P0002';
  end if;

  select id, status into reconciliation_id, current_status
  from public.reconciliations
  where organisation_id = org_id and employee_id = p_employee_id
    and period_start = p_period_start and period_end = p_period_end
  for update;

  if current_status = 'reconciled' then
    raise exception 'reconciled period is locked; use a documented adjustment' using errcode = '42501';
  end if;

  select coalesce(sum(case
    when transaction_type in ('advance_disbursed','allocation_funded') then amount
    when transaction_type in ('expense_accepted','cash_returned') then -amount
    else amount
  end), 0)
  into opening
  from public.fund_transactions
  where organisation_id = org_id and employee_id = p_employee_id
    and effective_at < p_period_start::timestamptz;

  select
    coalesce(sum(amount) filter (where transaction_type in ('advance_disbursed','allocation_funded')), 0),
    coalesce(sum(amount) filter (where transaction_type = 'expense_accepted'), 0),
    coalesce(sum(amount) filter (where transaction_type = 'cash_returned'), 0),
    coalesce(sum(amount) filter (where transaction_type = 'adjustment'), 0)
  into disbursed, accepted, returned, adjustment
  from public.fund_transactions
  where organisation_id = org_id and employee_id = p_employee_id
    and effective_at >= p_period_start::timestamptz
    and effective_at < (p_period_end + 1)::timestamptz;

  outstanding := opening + disbursed - accepted - returned + adjustment;

  insert into public.reconciliations (
    organisation_id, employee_id, period_start, period_end, opening_advance,
    confirmed_disbursements, accepted_expenses, returned_funds, adjustments,
    outstanding_balance, status, prepared_by
  ) values (
    org_id, p_employee_id, p_period_start, p_period_end, opening,
    disbursed, accepted, returned, adjustment, outstanding, 'open', auth.uid()
  )
  on conflict (organisation_id, employee_id, period_start, period_end)
  do update set
    opening_advance = excluded.opening_advance,
    confirmed_disbursements = excluded.confirmed_disbursements,
    accepted_expenses = excluded.accepted_expenses,
    returned_funds = excluded.returned_funds,
    adjustments = excluded.adjustments,
    outstanding_balance = excluded.outstanding_balance,
    prepared_by = auth.uid()
  where reconciliations.status <> 'reconciled'
  returning id into reconciliation_id;

  insert into public.audit_events (
    organisation_id, actor_user_id, action, entity_type, entity_id, after_data
  ) values (
    org_id, auth.uid(), 'reconciliation.rebuilt', 'reconciliation', reconciliation_id::text,
    jsonb_build_object('employee_id', p_employee_id, 'period_start', p_period_start, 'period_end', p_period_end,
      'opening_advance', opening, 'disbursements', disbursed, 'accepted_expenses', accepted,
      'returned_funds', returned, 'adjustments', adjustment, 'outstanding_balance', outstanding)
  );

  return jsonb_build_object(
    'reconciliation_id', reconciliation_id,
    'opening_advance', opening,
    'confirmed_disbursements', disbursed,
    'accepted_expenses', accepted,
    'returned_funds', returned,
    'adjustments', adjustment,
    'outstanding_balance', outstanding
  );
end
$function$;

revoke all on function public.record_cash_return(uuid, numeric, text, text, timestamptz) from public, anon;
revoke all on function public.rebuild_reconciliation(uuid, date, date) from public, anon;
grant execute on function public.record_cash_return(uuid, numeric, text, text, timestamptz) to authenticated;
grant execute on function public.rebuild_reconciliation(uuid, date, date) to authenticated;
