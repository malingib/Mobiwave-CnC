-- Audited, server-only enterprise request transitions.
create table if not exists public.cnc_enterprise_request_events (
  id uuid primary key default gen_random_uuid(),
  request_id uuid not null references public.cnc_enterprise_requests(id) on delete restrict,
  actor_user_id uuid not null references auth.users(id) on delete restrict,
  from_status text not null,
  to_status text not null,
  reason text not null check (length(trim(reason)) between 3 and 4000),
  created_at timestamptz not null default now()
);

create index if not exists cnc_enterprise_request_events_request_created_idx
  on public.cnc_enterprise_request_events (request_id, created_at desc);
alter table public.cnc_enterprise_request_events enable row level security;
-- No client policies: only trusted server-side operations can write/read this log.

create or replace function public.cnc_admin_transition_enterprise_request(
  p_request_id uuid,
  p_actor_user_id uuid,
  p_to_status text,
  p_reason text,
  p_provisioned_organisation_id uuid default null,
  p_review_notes text default null
)
returns public.cnc_enterprise_requests
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_request public.cnc_enterprise_requests;
  v_org_exists boolean := false;
begin
  if p_request_id is null or p_actor_user_id is null then
    raise exception 'Request and actor are required' using errcode = '22023';
  end if;
  if length(trim(coalesce(p_reason, ''))) < 3 or length(p_reason) > 4000 then
    raise exception 'A reason of 3 to 4000 characters is required' using errcode = '22023';
  end if;
  if p_to_status not in ('under_review','approved','provisioning','active','rejected','suspended','closed') then
    raise exception 'Unsupported target status' using errcode = '22023';
  end if;

  select * into v_request
  from public.cnc_enterprise_requests
  where id = p_request_id
  for update;

  if not found then
    raise exception 'Enterprise request not found' using errcode = 'P0002';
  end if;

  if not (
    (v_request.status = 'configuration_requested' and p_to_status in ('under_review','rejected'))
    or (v_request.status = 'under_review' and p_to_status in ('approved','rejected'))
    or (v_request.status = 'approved' and p_to_status in ('provisioning','rejected'))
    or (v_request.status = 'provisioning' and p_to_status in ('active','rejected'))
    or (v_request.status = 'active' and p_to_status in ('suspended','closed'))
    or (v_request.status = 'suspended' and p_to_status in ('active','closed'))
    or (v_request.status = 'rejected' and p_to_status = 'closed')
  ) then
    raise exception 'Invalid enterprise request transition: % -> %', v_request.status, p_to_status
      using errcode = '22023';
  end if;

  if p_to_status = 'active' then
    if coalesce(p_provisioned_organisation_id, v_request.provisioned_organisation_id) is null then
      raise exception 'Activation requires a provisioned organisation' using errcode = '22023';
    end if;

    -- Validate the tenant identifier against a known existing organisation table.
    if to_regclass('public.organisations') is not null then
      execute 'select exists(select 1 from public.organisations where id = $1)'
        into v_org_exists using coalesce(p_provisioned_organisation_id, v_request.provisioned_organisation_id);
    elsif to_regclass('public.organizations') is not null then
      execute 'select exists(select 1 from public.organizations where id = $1)'
        into v_org_exists using coalesce(p_provisioned_organisation_id, v_request.provisioned_organisation_id);
    else
      raise exception 'Organisation table could not be identified; activation is blocked' using errcode = '55000';
    end if;
    if not v_org_exists then
      raise exception 'Provisioned organisation does not exist' using errcode = '23503';
    end if;
  end if;

  insert into public.cnc_enterprise_request_events
    (request_id, actor_user_id, from_status, to_status, reason)
  values
    (v_request.id, p_actor_user_id, v_request.status, p_to_status, trim(p_reason));

  update public.cnc_enterprise_requests
  set status = p_to_status,
      reviewer_user_id = p_actor_user_id,
      reviewed_at = now(),
      review_notes = coalesce(p_review_notes, review_notes),
      provisioned_organisation_id = coalesce(p_provisioned_organisation_id, provisioned_organisation_id),
      updated_at = now()
  where id = p_request_id
  returning * into v_request;

  return v_request;
end;
$$;

revoke all on function public.cnc_admin_transition_enterprise_request(uuid, uuid, text, text, uuid, text) from public, anon, authenticated;
grant execute on function public.cnc_admin_transition_enterprise_request(uuid, uuid, text, text, uuid, text) to service_role;

comment on function public.cnc_admin_transition_enterprise_request(uuid, uuid, text, text, uuid, text) is
  'Server-only state transition with row locking, legal-transition validation and an append-only audit event. Caller must authenticate a MobiWave admin before invoking.';
