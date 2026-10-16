-- Preserve invoice date separately from the date the expense was recorded.
alter table public.expenses
  add column if not exists invoice_date date;

comment on column public.expenses.invoice_date is
  'Date printed on the supplier eTIMS invoice; distinct from the date the expense record is submitted.';
