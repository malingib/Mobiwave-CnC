-- Enforce same-tenant references at the database layer.
-- Row-level policies constrain the row's organisation_id; composite foreign keys
-- additionally prevent cross-tenant links even when a caller knows another UUID.
-- The target tables are empty in the audited project at the time this migration was prepared.

create unique index if not exists org_units_id_organisation_uidx on public.org_units (id, organisation_id);
create unique index if not exists employees_id_organisation_uidx on public.employees (id, organisation_id);
create unique index if not exists roles_id_organisation_uidx on public.roles (id, organisation_id);
create unique index if not exists employee_assignments_id_organisation_uidx on public.employee_assignments (id, organisation_id);
create unique index if not exists allocation_rules_id_organisation_uidx on public.allocation_rules (id, organisation_id);
create unique index if not exists allocations_id_organisation_uidx on public.allocations (id, organisation_id);
create unique index if not exists disbursement_batches_id_organisation_uidx on public.disbursement_batches (id, organisation_id);
create unique index if not exists disbursement_items_id_organisation_uidx on public.disbursement_items (id, organisation_id);
create unique index if not exists expenses_id_organisation_uidx on public.expenses (id, organisation_id);
create unique index if not exists cnc_invoices_id_organisation_uidx on public.cnc_invoices (id, organisation_id);

alter table public.org_units
  add constraint org_units_parent_same_organisation_fk
  foreign key (parent_id, organisation_id)
  references public.org_units (id, organisation_id)
  not valid;
alter table public.employee_assignments
  add constraint assignments_employee_same_organisation_fk
  foreign key (employee_id, organisation_id) references public.employees (id, organisation_id) not valid,
  add constraint assignments_role_same_organisation_fk
  foreign key (role_id, organisation_id) references public.roles (id, organisation_id) not valid,
  add constraint assignments_unit_same_organisation_fk
  foreign key (org_unit_id, organisation_id) references public.org_units (id, organisation_id) not valid,
  add constraint assignments_acting_for_same_organisation_fk
  foreign key (acting_for_assignment_id, organisation_id)
  references public.employee_assignments (id, organisation_id) not valid;
alter table public.reporting_relationships
  add constraint reporting_employee_same_organisation_fk
  foreign key (employee_id, organisation_id) references public.employees (id, organisation_id) not valid,
  add constraint reporting_manager_same_organisation_fk
  foreign key (manager_employee_id, organisation_id) references public.employees (id, organisation_id) not valid,
  add constraint reporting_unit_same_organisation_fk
  foreign key (org_unit_id, organisation_id) references public.org_units (id, organisation_id) not valid;
alter table public.allocation_rules
  add constraint allocation_rules_employee_same_organisation_fk
  foreign key (employee_id, organisation_id) references public.employees (id, organisation_id) not valid,
  add constraint allocation_rules_role_same_organisation_fk
  foreign key (role_id, organisation_id) references public.roles (id, organisation_id) not valid,
  add constraint allocation_rules_unit_same_organisation_fk
  foreign key (org_unit_id, organisation_id) references public.org_units (id, organisation_id) not valid;
alter table public.allocations
  add constraint allocations_employee_same_organisation_fk
  foreign key (employee_id, organisation_id) references public.employees (id, organisation_id) not valid,
  add constraint allocations_unit_same_organisation_fk
  foreign key (org_unit_id, organisation_id) references public.org_units (id, organisation_id) not valid,
  add constraint allocations_rule_same_organisation_fk
  foreign key (allocation_rule_id, organisation_id) references public.allocation_rules (id, organisation_id) not valid;
alter table public.disbursement_items
  add constraint disbursement_items_batch_same_organisation_fk
  foreign key (batch_id, organisation_id) references public.disbursement_batches (id, organisation_id) not valid,
  add constraint disbursement_items_employee_same_organisation_fk
  foreign key (employee_id, organisation_id) references public.employees (id, organisation_id) not valid;
alter table public.expenses
  add constraint expenses_employee_same_organisation_fk
  foreign key (employee_id, organisation_id) references public.employees (id, organisation_id) not valid,
  add constraint expenses_unit_same_organisation_fk
  foreign key (org_unit_id, organisation_id) references public.org_units (id, organisation_id) not valid,
  add constraint expenses_allocation_same_organisation_fk
  foreign key (allocation_id, organisation_id) references public.allocations (id, organisation_id) not valid,
  add constraint expenses_disbursement_item_same_organisation_fk
  foreign key (disbursement_item_id, organisation_id) references public.disbursement_items (id, organisation_id) not valid;
alter table public.fund_transactions
  add constraint fund_transactions_employee_same_organisation_fk
  foreign key (employee_id, organisation_id) references public.employees (id, organisation_id) not valid,
  add constraint fund_transactions_allocation_same_organisation_fk
  foreign key (allocation_id, organisation_id) references public.allocations (id, organisation_id) not valid,
  add constraint fund_transactions_disbursement_item_same_organisation_fk
  foreign key (disbursement_item_id, organisation_id) references public.disbursement_items (id, organisation_id) not valid;
alter table public.reconciliations
  add constraint reconciliations_employee_same_organisation_fk
  foreign key (employee_id, organisation_id) references public.employees (id, organisation_id) not valid;
alter table public.evidence_files
  add constraint evidence_expense_same_organisation_fk
  foreign key (expense_id, organisation_id) references public.expenses (id, organisation_id) not valid;
alter table public.invoice_verifications
  add constraint invoice_verifications_expense_same_organisation_fk
  foreign key (expense_id, organisation_id) references public.expenses (id, organisation_id) not valid;
alter table public.invoice_manual_review_queue
  add constraint invoice_review_expense_same_organisation_fk
  foreign key (expense_id, organisation_id) references public.expenses (id, organisation_id) not valid;
alter table public.cnc_payments
  add constraint cnc_payments_invoice_same_organisation_fk
  foreign key (invoice_id, organisation_id) references public.cnc_invoices (id, organisation_id) not valid;

-- Validate existing rows before accepting the migration as complete.
alter table public.org_units validate constraint org_units_parent_same_organisation_fk;
alter table public.employee_assignments validate constraint assignments_employee_same_organisation_fk;
alter table public.employee_assignments validate constraint assignments_role_same_organisation_fk;
alter table public.employee_assignments validate constraint assignments_unit_same_organisation_fk;
alter table public.employee_assignments validate constraint assignments_acting_for_same_organisation_fk;
alter table public.reporting_relationships validate constraint reporting_employee_same_organisation_fk;
alter table public.reporting_relationships validate constraint reporting_manager_same_organisation_fk;
alter table public.reporting_relationships validate constraint reporting_unit_same_organisation_fk;
alter table public.allocation_rules validate constraint allocation_rules_employee_same_organisation_fk;
alter table public.allocation_rules validate constraint allocation_rules_role_same_organisation_fk;
alter table public.allocation_rules validate constraint allocation_rules_unit_same_organisation_fk;
alter table public.allocations validate constraint allocations_employee_same_organisation_fk;
alter table public.allocations validate constraint allocations_unit_same_organisation_fk;
alter table public.allocations validate constraint allocations_rule_same_organisation_fk;
alter table public.disbursement_items validate constraint disbursement_items_batch_same_organisation_fk;
alter table public.disbursement_items validate constraint disbursement_items_employee_same_organisation_fk;
alter table public.expenses validate constraint expenses_employee_same_organisation_fk;
alter table public.expenses validate constraint expenses_unit_same_organisation_fk;
alter table public.expenses validate constraint expenses_allocation_same_organisation_fk;
alter table public.expenses validate constraint expenses_disbursement_item_same_organisation_fk;
alter table public.fund_transactions validate constraint fund_transactions_employee_same_organisation_fk;
alter table public.fund_transactions validate constraint fund_transactions_allocation_same_organisation_fk;
alter table public.fund_transactions validate constraint fund_transactions_disbursement_item_same_organisation_fk;
alter table public.reconciliations validate constraint reconciliations_employee_same_organisation_fk;
alter table public.evidence_files validate constraint evidence_expense_same_organisation_fk;
alter table public.invoice_verifications validate constraint invoice_verifications_expense_same_organisation_fk;
alter table public.invoice_manual_review_queue validate constraint invoice_review_expense_same_organisation_fk;
alter table public.cnc_payments validate constraint cnc_payments_invoice_same_organisation_fk;
