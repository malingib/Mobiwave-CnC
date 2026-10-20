-- Enterprise onboarding requests for the configurable single-monolith platform.
-- Requests are not active tenants. Provisioning/activation remains an explicit,
-- audited MobiWave-admin operation performed server-side.
create table if not exists public.cnc_enterprise_requests (
  id uuid primary key default gen_random_uuid(),
  requester_user_id uuid not null references auth.users(id) on delete restrict,
  legal_name text not null check (length(trim(legal_name)) between 2 and 200),
  trading_name text check (trading_name is null or length(trim(trading_name)) <= 200),
  contact_name text not null check (length(trim(contact_name)) between 2 and 160),
  contact_email text not null check (length(trim(contact_email)) between 3 and 254),
  contact_phone text check (contact_phone is null or length(trim(contact_phone)) <= 40),
  country_code text not null default 'KE' check (country_code ~ '^[A-Z]{2}$'),
  estimated_users integer check (estimated_users is null or estimated_users between 1 and 1000000),
  locations_summary text check (locations_summary is null or length(locations_summary) <= 10000),
  department_summary text check (department_summary is null or length(department_summary) <= 10000),
  configuration_requirements text check (configuration_requirements is null or length(configuration_requirements) <= 20000),
  status text not null default 'configuration_requested'
    check (status in ('configuration_requested','under_review','approved','provisioning','active','rejected','suspended','closed')),
  reviewer_user_id uuid references auth.users(id) on delete set null,
  review_notes text check (review_notes is null or length(review_notes) <= 20000),
  reviewed_at timestamptz,
  provisioned_organisation_id uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint cnc_enterprise_request_review_consistency check (
    (status in ('approved','provisioning','active','rejected','suspended','closed')
      and reviewer_user_id is not null and reviewed_at is not null)
    or status in ('configuration_requested','under_review')
  ),
  constraint cnc_enterprise_request_active_requires_tenant check (
    status <> 'active' or provisioned_organisation_id is not null
  )
);

create index if not exists cnc_enterprise_requests_requester_created_idx
  on public.cnc_enterprise_requests (requester_user_id, created_at desc);
create index if not exists cnc_enterprise_requests_status_created_idx
  on public.cnc_enterprise_requests (status, created_at desc);

alter table public.cnc_enterprise_requests enable row level security;

-- Requesters can submit and inspect only their own requests. They cannot
-- approve, provision, activate, or change workflow status from the client.
drop policy if exists "requesters read own enterprise requests" on public.cnc_enterprise_requests;
create policy "requesters read own enterprise requests"
  on public.cnc_enterprise_requests
  for select to authenticated
  using (requester_user_id = (select auth.uid()));

drop policy if exists "authenticated users submit enterprise requests" on public.cnc_enterprise_requests;
create policy "authenticated users submit enterprise requests"
  on public.cnc_enterprise_requests
  for insert to authenticated
  with check (
    requester_user_id = (select auth.uid())
    and status = 'configuration_requested'
    and reviewer_user_id is null
    and reviewed_at is null
    and provisioned_organisation_id is null
  );

-- Deliberately no client UPDATE or DELETE policy. Review and provisioning must
-- be implemented in a server-side operation that verifies MobiWave-admin
-- permission, validates legal transitions, and writes an audit event.
comment on table public.cnc_enterprise_requests is
  'Inbound enterprise onboarding requests; never grants tenant access by itself.';
comment on column public.cnc_enterprise_requests.provisioned_organisation_id is
  'Set only by the trusted provisioning workflow after an organisation has been created.';
