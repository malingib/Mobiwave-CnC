-- Active plans are non-sensitive catalog metadata; keep inactive plans server-only.
create policy cnc_plans_active_read
on public.cnc_plans
for select
to authenticated
using (is_active = true);
