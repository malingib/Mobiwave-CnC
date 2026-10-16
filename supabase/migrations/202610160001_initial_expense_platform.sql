-- MobiWave Expense & eTIMS: initial multi-tenant schema.
-- Apply only to a dedicated staging project first; review access policies before production.
create extension if not exists pgcrypto;

create table if not exists public.organisations (
  id uuid primary key default gen_random_uuid(),
  name text not null, slug text not null unique,
  status text not null default 'active' check (status in ('active','suspended','closed')),
  created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table if not exists public.org_units (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id),
  parent_id uuid references public.org_units(id),
  unit_type text not null, code text not null, name text not null,
  status text not null default 'active' check (status in ('active','inactive','closed')),
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
  unique (organisation_id, code)
);
create index if not exists org_units_parent_idx on public.org_units(parent_id);

create table if not exists public.employees (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id),
  auth_user_id uuid unique references auth.users(id),
  employee_number text not null, full_name text not null, phone_e164 text, email text,
  status text not null default 'active' check (status in ('active','inactive','suspended')),
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
  unique (organisation_id, employee_number)
);
create table if not exists public.roles (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id),
  role_key text not null, name text not null, description text,
  is_system_role boolean not null default false, created_at timestamptz not null default now(),
  unique (organisation_id, role_key)
);
create table if not exists public.role_permissions (
  role_id uuid not null references public.roles(id) on delete cascade,
  permission_key text not null, created_at timestamptz not null default now(),
  primary key (role_id, permission_key)
);
create table if not exists public.employee_assignments (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id),
  employee_id uuid not null references public.employees(id),
  role_id uuid not null references public.roles(id),
  org_unit_id uuid not null references public.org_units(id),
  starts_at timestamptz not null default now(), ends_at timestamptz,
  is_acting boolean not null default false,
  acting_for_assignment_id uuid references public.employee_assignments(id),
  status text not null default 'active' check (status in ('active','revoked')),
  created_by uuid references auth.users(id), created_at timestamptz not null default now(),
  check (ends_at is null or ends_at > starts_at)
);
create index if not exists employee_assignments_active_idx on public.employee_assignments(employee_id, starts_at, ends_at) where status = 'active';
create table if not exists public.reporting_relationships (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id),
  employee_id uuid not null references public.employees(id),
  manager_employee_id uuid not null references public.employees(id),
  org_unit_id uuid references public.org_units(id),
  starts_at timestamptz not null default now(), ends_at timestamptz,
  created_at timestamptz not null default now(),
  check (employee_id <> manager_employee_id), check (ends_at is null or ends_at > starts_at)
);
create table if not exists public.allocation_rules (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id),
  name text not null,
  category text not null check (category in ('transport','accommodation','meals','incentives','other')),
  period text not null check (period in ('day','week','month')),
  limit_amount numeric(14,2) not null check (limit_amount >= 0),
  role_id uuid references public.roles(id), org_unit_id uuid references public.org_units(id),
  employee_id uuid references public.employees(id),
  effective_from date not null, effective_to date, is_active boolean not null default true,
  created_by uuid references auth.users(id), created_at timestamptz not null default now(),
  check (effective_to is null or effective_to >= effective_from)
);
create table if not exists public.allocations (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id),
  employee_id uuid not null references public.employees(id),
  org_unit_id uuid references public.org_units(id), allocation_rule_id uuid references public.allocation_rules(id),
  category text not null check (category in ('transport','accommodation','meals','incentives','other')),
  period_start date not null, period_end date not null,
  limit_amount numeric(14,2) not null check (limit_amount >= 0), currency char(3) not null default 'KES',
  status text not null default 'active' check (status in ('active','closed','cancelled')),
  created_by uuid references auth.users(id), created_at timestamptz not null default now(),
  check (period_end >= period_start)
);
create table if not exists public.disbursement_batches (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id),
  reference text not null, purpose text not null check (purpose in ('expense_advance','expense_allocation')),
  status text not null default 'draft' check (status in ('draft','pending_approval','approved','submitted','successful','partial_success','failed','outcome_unconfirmed','cancelled')),
  total_amount numeric(14,2) not null default 0 check (total_amount >= 0), currency char(3) not null default 'KES',
  created_by uuid not null references auth.users(id), approved_by uuid references auth.users(id), approved_at timestamptz,
  submitted_at timestamptz, provider_request_id text, result_summary jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
  unique (organisation_id, reference), check (approved_by is null or approved_by <> created_by)
);
create table if not exists public.disbursement_items (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id),
  batch_id uuid not null references public.disbursement_batches(id),
  employee_id uuid not null references public.employees(id), phone_e164 text not null,
  amount numeric(14,2) not null check (amount > 0), purpose text not null,
  idempotency_key text not null unique,
  status text not null default 'draft' check (status in ('draft','pending_approval','approved','submitted','successful','failed','outcome_unconfirmed','cancelled')),
  originator_conversation_id text unique, conversation_id text, provider_transaction_id text,
  result_code text, result_description text, confirmed_at timestamptz,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create index if not exists disbursement_items_batch_idx on public.disbursement_items(batch_id);
create table if not exists public.fund_transactions (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id),
  employee_id uuid not null references public.employees(id),
  allocation_id uuid references public.allocations(id),
  disbursement_item_id uuid unique references public.disbursement_items(id),
  transaction_type text not null check (transaction_type in ('advance_disbursed','allocation_funded','expense_accepted','cash_returned','adjustment')),
  amount numeric(14,2) not null, currency char(3) not null default 'KES',
  external_reference text, effective_at timestamptz not null default now(),
  created_by uuid references auth.users(id), created_at timestamptz not null default now(),
  metadata jsonb not null default '{}'::jsonb
);
create unique index if not exists fund_transactions_external_reference_uidx
  on public.fund_transactions(external_reference) where external_reference is not null;

create table if not exists public.expenses (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id),
  employee_id uuid not null references public.employees(id),
  allocation_id uuid references public.allocations(id), disbursement_item_id uuid references public.disbursement_items(id),
  org_unit_id uuid references public.org_units(id), client_submission_id uuid not null,
  merchant_name text not null, supplier_pin text, cu_invoice_number text,
  category text not null check (category in ('transport','accommodation','meals','incentives','other')),
  purpose text not null, gross_amount numeric(14,2) not null check (gross_amount >= 0),
  vat_amount numeric(14,2) not null default 0 check (vat_amount >= 0), currency char(3) not null default 'KES',
  expense_date date not null,
  status text not null default 'submitted' check (status in ('draft','submitted','needs_review','accepted','rejected','reconciled')),
  verification_status text not null default 'awaiting_verification' check (verification_status in ('awaiting_verification','data_extracted_not_verified','externally_verified','needs_review','verification_failed','offline_saved')),
  submitted_by uuid not null references auth.users(id), reviewed_by uuid references auth.users(id), reviewed_at timestamptz, review_note text,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
  unique (organisation_id, employee_id, client_submission_id)
);
create index if not exists expenses_org_date_idx on public.expenses(organisation_id, expense_date desc);
create index if not exists expenses_employee_status_idx on public.expenses(employee_id, status);
create table if not exists public.invoice_verifications (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id), expense_id uuid not null references public.expenses(id),
  provider text not null, status text not null check (status in ('externally_verified','not_verified','needs_review','verification_failed','awaiting_verification')),
  provider_reference text, checked_at timestamptz, response_code text, response_summary jsonb not null default '{}'::jsonb,
  requested_by uuid references auth.users(id), created_at timestamptz not null default now()
);
create table if not exists public.evidence_files (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id), expense_id uuid not null references public.expenses(id),
  storage_path text not null unique, mime_type text not null, file_size_bytes bigint not null check (file_size_bytes > 0),
  sha256 text, uploaded_by uuid not null references auth.users(id), created_at timestamptz not null default now()
);
create table if not exists public.reconciliations (
  id uuid primary key default gen_random_uuid(),
  organisation_id uuid not null references public.organisations(id), employee_id uuid not null references public.employees(id),
  period_start date not null, period_end date not null,
  opening_advance numeric(14,2) not null default 0, confirmed_disbursements numeric(14,2) not null default 0,
  accepted_expenses numeric(14,2) not null default 0, returned_funds numeric(14,2) not null default 0,
  adjustments numeric(14,2) not null default 0, outstanding_balance numeric(14,2) not null default 0,
  status text not null default 'open' check (status in ('open','under_review','reconciled','disputed')),
  prepared_by uuid references auth.users(id), reviewed_by uuid references auth.users(id), reviewed_at timestamptz,
  created_at timestamptz not null default now(), check (period_end >= period_start)
);
create table if not exists public.audit_events (
  id bigint generated always as identity primary key,
  organisation_id uuid not null references public.organisations(id), actor_user_id uuid references auth.users(id),
  action text not null, entity_type text not null, entity_id text not null, request_id text,
  before_data jsonb, after_data jsonb, reason text, created_at timestamptz not null default now()
);
create index if not exists audit_events_org_created_idx on public.audit_events(organisation_id, created_at desc);

create or replace function public.current_organisation_id()
returns uuid language sql stable security definer set search_path = public
as $$
  select e.organisation_id from public.employees e
  where e.auth_user_id = auth.uid() and e.status = 'active' limit 1
$$;
create or replace function public.has_permission(p_permission text)
returns boolean language sql stable security definer set search_path = public
as $$
  select exists (
    select 1 from public.employees e
    join public.employee_assignments a on a.employee_id = e.id
    join public.role_permissions rp on rp.role_id = a.role_id
    where e.auth_user_id = auth.uid() and e.status = 'active' and a.status = 'active'
      and a.starts_at <= now() and (a.ends_at is null or a.ends_at > now())
      and rp.permission_key = p_permission
  )
$$;

create or replace function public.submit_disbursement_batch_for_approval(p_batch_id uuid)
returns jsonb language plpgsql security definer set search_path = public
as $$
declare b public.disbursement_batches%rowtype; calculated_total numeric(14,2); item_count integer;
begin
  if auth.uid() is null or not public.has_permission('disbursements.prepare') then raise exception 'permission denied' using errcode = '42501'; end if;
  select * into b from public.disbursement_batches where id = p_batch_id for update;
  if not found or b.organisation_id <> public.current_organisation_id() then raise exception 'batch not found' using errcode = 'P0002'; end if;
  if b.created_by <> auth.uid() then raise exception 'only the batch maker can submit it for approval' using errcode = '42501'; end if;
  if b.status <> 'draft' then raise exception 'batch is not a draft' using errcode = '22023'; end if;
  select count(*), coalesce(sum(amount),0) into item_count, calculated_total from public.disbursement_items where batch_id = p_batch_id and status = 'draft';
  if item_count = 0 then raise exception 'batch has no recipients' using errcode = '22023'; end if;
  if exists (select 1 from public.disbursement_items where batch_id = p_batch_id and (amount <= 0 or phone_e164 !~ '^\+254[17][0-9]{8}$')) then
    raise exception 'recipient phone or amount is invalid' using errcode = '22023';
  end if;
  update public.disbursement_batches set status = 'pending_approval', total_amount = calculated_total, updated_at = now() where id = p_batch_id;
  update public.disbursement_items set status = 'pending_approval', updated_at = now() where batch_id = p_batch_id and status = 'draft';
  insert into public.audit_events(organisation_id,actor_user_id,action,entity_type,entity_id,after_data)
    values (b.organisation_id,auth.uid(),'disbursement_batch.submitted_for_approval','disbursement_batch',p_batch_id::text,
      jsonb_build_object('status','pending_approval','total_amount',calculated_total,'item_count',item_count));
  return jsonb_build_object('batch_id',p_batch_id,'status','pending_approval','total_amount',calculated_total,'item_count',item_count);
end $$;

create or replace function public.approve_disbursement_batch(p_batch_id uuid)
returns jsonb language plpgsql security definer set search_path = public
as $$
declare b public.disbursement_batches%rowtype;
begin
  if auth.uid() is null or not public.has_permission('disbursements.approve') then
    raise exception 'permission denied' using errcode = '42501';
  end if;
  select * into b from public.disbursement_batches where id = p_batch_id for update;
  if not found or b.organisation_id <> public.current_organisation_id() then raise exception 'batch not found' using errcode = 'P0002'; end if;
  if b.created_by = auth.uid() then raise exception 'maker and checker must be different users' using errcode = '42501'; end if;
  if b.status <> 'pending_approval' then raise exception 'batch is not pending approval' using errcode = '22023'; end if;
  update public.disbursement_batches set status = 'approved', approved_by = auth.uid(), approved_at = now(), updated_at = now() where id = p_batch_id;
  update public.disbursement_items set status = 'approved', updated_at = now() where batch_id = p_batch_id and status = 'pending_approval';
  insert into public.audit_events(organisation_id,actor_user_id,action,entity_type,entity_id,after_data)
  values (b.organisation_id,auth.uid(),'disbursement_batch.approved','disbursement_batch',p_batch_id::text,jsonb_build_object('status','approved','approved_at',now()));
  return jsonb_build_object('batch_id',p_batch_id,'status','approved');
end $$;

create or replace function public.review_expense(p_expense_id uuid, p_decision text, p_reason text default null)
returns jsonb language plpgsql security definer set search_path = public
as $$
declare e public.expenses%rowtype; next_status text;
begin
  if auth.uid() is null or not public.has_permission('expenses.review') then raise exception 'permission denied' using errcode = '42501'; end if;
  if p_decision not in ('accept','reject','needs_review') then raise exception 'invalid review decision' using errcode = '22023'; end if;
  select * into e from public.expenses where id = p_expense_id for update;
  if not found or e.organisation_id <> public.current_organisation_id() then raise exception 'expense not found' using errcode = 'P0002'; end if;
  if e.submitted_by = auth.uid() then raise exception 'submitter cannot review own expense' using errcode = '42501'; end if;
  if p_decision = 'accept' and e.verification_status <> 'externally_verified' then
    raise exception 'expense requires external verification or documented exception workflow' using errcode = '22023';
  end if;
  next_status := case p_decision when 'accept' then 'accepted' when 'reject' then 'rejected' else 'needs_review' end;
  update public.expenses set status = next_status, reviewed_by = auth.uid(), reviewed_at = now(), review_note = p_reason, updated_at = now() where id = p_expense_id;
  if p_decision = 'accept' then
    insert into public.fund_transactions(organisation_id,employee_id,allocation_id,transaction_type,amount,currency,external_reference,effective_at,created_by,metadata)
    values (e.organisation_id,e.employee_id,e.allocation_id,'expense_accepted',e.gross_amount,e.currency,'EXPENSE:' || e.id::text,now(),auth.uid(),
      jsonb_build_object('expense_id',e.id,'accepted_by',auth.uid()))
    on conflict (external_reference) where external_reference is not null do nothing;
  end if;
  insert into public.audit_events(organisation_id,actor_user_id,action,entity_type,entity_id,before_data,after_data,reason)
  values (e.organisation_id,auth.uid(),'expense.reviewed','expense',e.id::text,
    jsonb_build_object('status',e.status,'verification_status',e.verification_status),
    jsonb_build_object('status',next_status,'reviewed_at',now()),p_reason);
  return jsonb_build_object('expense_id',p_expense_id,'status',next_status);
end $$;

create or replace function public.record_fund_adjustment(
  p_employee_id uuid, p_transaction_type text, p_amount numeric, p_external_reference text, p_reason text, p_allocation_id uuid default null
)
returns jsonb language plpgsql security definer set search_path = public
as $$
declare org_id uuid; txn_id uuid;
begin
  if auth.uid() is null or not public.has_permission('reconciliations.manage') then
    raise exception 'permission denied' using errcode = '42501';
  end if;
  if p_transaction_type not in ('cash_returned','adjustment') then
    raise exception 'only cash returns and traceable adjustments are allowed here' using errcode = '22023';
  end if;
  if p_amount <= 0 or p_external_reference is null or length(trim(p_external_reference)) = 0 or length(trim(p_reason)) = 0 then
    raise exception 'amount, unique reference and reason are required' using errcode = '22023';
  end if;
  org_id := public.current_organisation_id();
  if not exists (select 1 from public.employees e where e.id = p_employee_id and e.organisation_id = org_id) then
    raise exception 'employee not found' using errcode = 'P0002';
  end if;
  insert into public.fund_transactions(organisation_id,employee_id,allocation_id,transaction_type,amount,currency,external_reference,effective_at,created_by,metadata)
  values (org_id,p_employee_id,p_allocation_id,p_transaction_type,p_amount,'KES',p_external_reference,now(),auth.uid(),
    jsonb_build_object('reason',p_reason))
  returning id into txn_id;
  insert into public.audit_events(organisation_id,actor_user_id,action,entity_type,entity_id,after_data,reason)
  values (org_id,auth.uid(),'fund_transaction.recorded','fund_transaction',txn_id::text,
    jsonb_build_object('type',p_transaction_type,'amount',p_amount,'reference',p_external_reference),p_reason);
  return jsonb_build_object('transaction_id',txn_id,'status','recorded');
end $$;

create or replace function public.prepare_disbursement_submission(p_batch_id uuid)
returns jsonb language plpgsql security definer set search_path = public
as $$
declare b public.disbursement_batches%rowtype; item_count integer;
begin
  if auth.uid() is null or not public.has_permission('disbursements.submit') then raise exception 'permission denied' using errcode = '42501'; end if;
  select * into b from public.disbursement_batches where id = p_batch_id for update;
  if not found or b.organisation_id <> public.current_organisation_id() then raise exception 'batch not found' using errcode = 'P0002'; end if;
  if b.created_by = auth.uid() or b.approved_by is null or b.approved_by = auth.uid() then
    raise exception 'submitter must be separate from maker and approver' using errcode = '42501';
  end if;
  if b.status <> 'approved' then raise exception 'batch is not approved' using errcode = '22023'; end if;
  select count(*) into item_count from public.disbursement_items where batch_id = p_batch_id and status = 'approved';
  if item_count = 0 then raise exception 'batch has no approved recipients' using errcode = '22023'; end if;
  update public.disbursement_batches set status = 'outcome_unconfirmed', submitted_at = now(), updated_at = now() where id = p_batch_id;
  update public.disbursement_items set status = 'outcome_unconfirmed', updated_at = now() where batch_id = p_batch_id and status = 'approved';
  insert into public.audit_events(organisation_id,actor_user_id,action,entity_type,entity_id,after_data)
  values (b.organisation_id,auth.uid(),'disbursement_batch.submission_started','disbursement_batch',p_batch_id::text,jsonb_build_object('status','outcome_unconfirmed','item_count',item_count));
  return jsonb_build_object('batch_id',b.id,'organisation_id',b.organisation_id,'purpose',b.purpose,
    'items',(select coalesce(jsonb_agg(jsonb_build_object('id',i.id,'employee_id',i.employee_id,'phone_e164',i.phone_e164,
      'amount',i.amount,'purpose',i.purpose,'idempotency_key',i.idempotency_key)),'[]'::jsonb)
      from public.disbursement_items i where i.batch_id = b.id and i.status = 'outcome_unconfirmed'));
end $$;

revoke all on function public.current_organisation_id() from public;
revoke all on function public.has_permission(text) from public;
revoke all on function public.approve_disbursement_batch(uuid) from public;
revoke all on function public.prepare_disbursement_submission(uuid) from public;
revoke all on function public.submit_disbursement_batch_for_approval(uuid) from public;
revoke all on function public.review_expense(uuid,text,text) from public;
revoke all on function public.record_fund_adjustment(uuid,text,numeric,text,text,uuid) from public;
grant execute on function public.current_organisation_id() to authenticated;
grant execute on function public.has_permission(text) to authenticated;
grant execute on function public.approve_disbursement_batch(uuid) to authenticated;
grant execute on function public.prepare_disbursement_submission(uuid) to authenticated;
grant execute on function public.submit_disbursement_batch_for_approval(uuid) to authenticated;
grant execute on function public.review_expense(uuid,text,text) to authenticated;
grant execute on function public.record_fund_adjustment(uuid,text,numeric,text,text,uuid) to authenticated;

alter table public.organisations enable row level security;
alter table public.org_units enable row level security;
alter table public.employees enable row level security;
alter table public.roles enable row level security;
alter table public.role_permissions enable row level security;
alter table public.employee_assignments enable row level security;
alter table public.reporting_relationships enable row level security;
alter table public.allocation_rules enable row level security;
alter table public.allocations enable row level security;
alter table public.disbursement_batches enable row level security;
alter table public.disbursement_items enable row level security;
alter table public.fund_transactions enable row level security;
alter table public.expenses enable row level security;
alter table public.invoice_verifications enable row level security;
alter table public.evidence_files enable row level security;
alter table public.reconciliations enable row level security;
alter table public.audit_events enable row level security;

create policy "organisation members can read own organisation" on public.organisations for select to authenticated using (id = public.current_organisation_id());
create policy "org unit readers" on public.org_units for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "org unit managers write" on public.org_units for all to authenticated
  using (organisation_id = public.current_organisation_id() and public.has_permission('organisation.manage'))
  with check (organisation_id = public.current_organisation_id() and public.has_permission('organisation.manage'));
create policy "employees read own organisation" on public.employees for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "employee managers write" on public.employees for all to authenticated
  using (organisation_id = public.current_organisation_id() and public.has_permission('employees.manage'))
  with check (organisation_id = public.current_organisation_id() and public.has_permission('employees.manage'));
create policy "roles read own organisation" on public.roles for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "role managers write" on public.roles for all to authenticated
  using (organisation_id = public.current_organisation_id() and public.has_permission('roles.manage'))
  with check (organisation_id = public.current_organisation_id() and public.has_permission('roles.manage'));
create policy "role permissions read" on public.role_permissions for select to authenticated
  using (exists (select 1 from public.roles r where r.id = role_id and r.organisation_id = public.current_organisation_id()));
create policy "role permissions manage" on public.role_permissions for all to authenticated
  using (public.has_permission('roles.manage') and exists (select 1 from public.roles r where r.id = role_id and r.organisation_id = public.current_organisation_id()))
  with check (public.has_permission('roles.manage') and exists (select 1 from public.roles r where r.id = role_id and r.organisation_id = public.current_organisation_id()));
create policy "assignments read" on public.employee_assignments for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "assignments manage" on public.employee_assignments for all to authenticated
  using (organisation_id = public.current_organisation_id() and public.has_permission('organisation.manage'))
  with check (organisation_id = public.current_organisation_id() and public.has_permission('organisation.manage'));
create policy "reporting read" on public.reporting_relationships for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "reporting manage" on public.reporting_relationships for all to authenticated
  using (organisation_id = public.current_organisation_id() and public.has_permission('organisation.manage'))
  with check (organisation_id = public.current_organisation_id() and public.has_permission('organisation.manage'));
create policy "allocation rules read" on public.allocation_rules for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "allocation rules manage" on public.allocation_rules for all to authenticated
  using (organisation_id = public.current_organisation_id() and public.has_permission('allocations.manage'))
  with check (organisation_id = public.current_organisation_id() and public.has_permission('allocations.manage'));
create policy "allocations read" on public.allocations for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "allocations manage" on public.allocations for all to authenticated
  using (organisation_id = public.current_organisation_id() and public.has_permission('allocations.manage'))
  with check (organisation_id = public.current_organisation_id() and public.has_permission('allocations.manage'));
create policy "batches read" on public.disbursement_batches for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "batches create" on public.disbursement_batches for insert to authenticated
  with check (organisation_id = public.current_organisation_id() and public.has_permission('disbursements.prepare') and created_by = auth.uid() and status = 'draft');
create policy "items read" on public.disbursement_items for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "items prepare" on public.disbursement_items for insert to authenticated
  with check (organisation_id = public.current_organisation_id() and public.has_permission('disbursements.prepare')
    and status = 'draft' and exists (select 1 from public.disbursement_batches b where b.id = batch_id and b.organisation_id = public.current_organisation_id()));
create policy "fund transactions read" on public.fund_transactions for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "fund transaction insert server only" on public.fund_transactions for insert to authenticated with check (false);
create policy "expenses read own organisation" on public.expenses for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "employees submit expenses" on public.expenses for insert to authenticated
  with check (organisation_id = public.current_organisation_id() and submitted_by = auth.uid()
    and status in ('draft','submitted') and verification_status in ('awaiting_verification','data_extracted_not_verified','offline_saved'));
create policy "invoice verification read" on public.invoice_verifications for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "invoice verification insert server only" on public.invoice_verifications for insert to authenticated with check (false);
create policy "evidence read" on public.evidence_files for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "evidence metadata insert" on public.evidence_files for insert to authenticated
  with check (organisation_id = public.current_organisation_id() and uploaded_by = auth.uid());
create policy "reconciliation read" on public.reconciliations for select to authenticated using (organisation_id = public.current_organisation_id());
create policy "reconciliation manage" on public.reconciliations for all to authenticated
  using (organisation_id = public.current_organisation_id() and public.has_permission('reconciliations.manage'))
  with check (organisation_id = public.current_organisation_id() and public.has_permission('reconciliations.manage'));
create policy "audit read" on public.audit_events for select to authenticated
  using (organisation_id = public.current_organisation_id() and public.has_permission('audit.read'));

create or replace function public.prevent_audit_mutation()
returns trigger language plpgsql as $$
begin raise exception 'audit events are append-only'; end $$;
create trigger audit_events_append_only before update or delete on public.audit_events for each row execute function public.prevent_audit_mutation();

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values ('expense-evidence','expense-evidence',false,10485760,array['image/jpeg','image/png','image/webp','application/pdf'])
on conflict (id) do nothing;
create policy "evidence bucket authenticated reads" on storage.objects for select to authenticated
  using (bucket_id = 'expense-evidence' and exists (
    select 1 from public.evidence_files f where f.storage_path = name and f.organisation_id = public.current_organisation_id()
  ));
create policy "evidence bucket authenticated uploads" on storage.objects for insert to authenticated
  with check (bucket_id = 'expense-evidence' and auth.uid() is not null and (storage.foldername(name))[1] = public.current_organisation_id()::text);
