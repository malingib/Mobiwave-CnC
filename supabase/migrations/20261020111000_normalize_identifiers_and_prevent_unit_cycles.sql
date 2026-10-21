-- Prevent duplicate identifiers that differ only by case/whitespace and prevent cyclic organisation trees.
create unique index if not exists employees_org_employee_number_normalized_uidx
  on public.employees (organisation_id, upper(btrim(employee_number)));
create unique index if not exists org_units_org_code_normalized_uidx
  on public.org_units (organisation_id, upper(btrim(code)));
create unique index if not exists roles_org_role_key_normalized_uidx
  on public.roles (organisation_id, lower(btrim(role_key)));

create or replace function public.prevent_org_unit_cycle()
returns trigger
language plpgsql
set search_path = public, pg_temp
as $$
declare
  would_cycle boolean;
begin
  if new.parent_id is null then
    return new;
  end if;

  if new.parent_id = new.id then
    raise exception 'An organisation unit cannot be its own parent' using errcode = '23514';
  end if;

  with recursive ancestors(id, parent_id, path) as (
    select u.id, u.parent_id, array[u.id]
    from public.org_units u
    where u.id = new.parent_id and u.organisation_id = new.organisation_id
    union all
    select p.id, p.parent_id, a.path || p.id
    from public.org_units p
    join ancestors a on p.id = a.parent_id
    where p.organisation_id = new.organisation_id
      and not p.id = any(a.path)
  )
  select exists(select 1 from ancestors where id = new.id) into would_cycle;

  if would_cycle then
    raise exception 'Organisation unit hierarchy cannot contain a cycle' using errcode = '23514';
  end if;
  return new;
end;
$$;

drop trigger if exists org_units_prevent_cycle on public.org_units;
create trigger org_units_prevent_cycle
before insert or update of parent_id, organisation_id on public.org_units
for each row execute function public.prevent_org_unit_cycle();

revoke all on function public.prevent_org_unit_cycle() from public, anon, authenticated;
