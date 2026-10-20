-- Preserve missing VAT as NULL so the verifier can distinguish "not supplied" from zero VAT.
alter table public.expenses
  alter column vat_amount drop not null;

comment on column public.expenses.vat_amount is
  'VAT amount declared on the supplier invoice; NULL means it was not captured, zero means explicitly zero VAT.';
