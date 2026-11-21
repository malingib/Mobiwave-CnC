-- Record Daraja B2C callbacks, the financial ledger entry, audit event and batch
-- aggregate in one database transaction. Duplicate/concurrent callbacks are safe.
create or replace function public.record_daraja_b2c_callback(
  p_originator_conversation_id text,
  p_result_code text,
  p_result_description text,
  p_provider_transaction_id text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_item public.disbursement_items%rowtype;
  v_batch public.disbursement_batches%rowtype;
  v_next_status text;
  v_total integer;
  v_success integer;
  v_failed integer;
  v_now timestamptz := now();
  v_repaired boolean := false;
begin
  if auth.uid() is not null then
    raise exception 'callback RPC is restricted to server-side service-role requests' using errcode = '42501';
  end if;

  if nullif(btrim(p_originator_conversation_id), '') is null
     or nullif(btrim(p_result_code), '') is null
     or p_result_code !~ '^[0-9]+$' then
    raise exception 'invalid Daraja callback identifiers or result code' using errcode = '22023';
  end if;

  select * into v_item
  from public.disbursement_items
  where originator_conversation_id = btrim(p_originator_conversation_id)
  for update;

  if not found then
    raise exception 'unknown Daraja originator conversation id' using errcode = 'P0002';
  end if;

  select * into v_batch
  from public.disbursement_batches
  where id = v_item.batch_id
  for update;

  if not found then
    raise exception 'disbursement batch not found' using errcode = 'P0002';
  end if;

  -- Recover a ledger entry if an older callback marked an item successful but
  -- silently failed while writing its financial transaction.
  if v_item.status = 'successful' then
    if not exists (
      select 1 from public.fund_transactions
      where disbursement_item_id = v_item.id
    ) then
      insert into public.fund_transactions (
        organisation_id, employee_id, disbursement_item_id, transaction_type,
        amount, currency, external_reference, effective_at, metadata
      ) values (
        v_item.organisation_id, v_item.employee_id, v_item.id,
        case when v_batch.purpose = 'expense_allocation'
          then 'allocation_funded' else 'advance_disbursed' end,
        v_item.amount, 'KES',
        coalesce(nullif(btrim(v_item.provider_transaction_id), ''),
                 btrim(p_provider_transaction_id),
                 btrim(p_originator_conversation_id)),
        coalesce(v_item.confirmed_at, v_now),
        jsonb_build_object('provider', 'Safaricom Daraja', 'confirmed', true, 'recovered', true)
      );
      v_repaired := true;
      insert into public.audit_events (
        organisation_id, action, entity_type, entity_id, after_data
      ) values (
        v_item.organisation_id, 'disbursement_item.ledger_repaired',
        'disbursement_item', v_item.id::text,
        jsonb_build_object('provider', 'Safaricom Daraja', 'ledger_repaired', true)
      );
    end if;

    return jsonb_build_object(
      'accepted', true, 'duplicate', true, 'ledger_repaired', v_repaired,
      'item_status', 'successful'
    );
  end if;

  if v_item.status = 'failed' then
    return jsonb_build_object(
      'accepted', true, 'duplicate', true, 'ledger_repaired', false,
      'item_status', 'failed'
    );
  end if;

  if v_item.status not in ('submitted', 'outcome_unconfirmed') then
    raise exception 'callback received for item in invalid state: %', v_item.status
      using errcode = '22023';
  end if;

  update public.disbursement_items
  set status = case when p_result_code = '0' then 'successful' else 'failed' end,
      result_code = p_result_code,
      result_description = coalesce(p_result_description, ''),
      provider_transaction_id = coalesce(nullif(btrim(p_provider_transaction_id), ''), provider_transaction_id),
      confirmed_at = v_now,
      updated_at = v_now
  where id = v_item.id;

  if p_result_code = '0' then
    insert into public.fund_transactions (
      organisation_id, employee_id, disbursement_item_id, transaction_type,
      amount, currency, external_reference, effective_at, metadata
    ) values (
      v_item.organisation_id, v_item.employee_id, v_item.id,
      case when v_batch.purpose = 'expense_allocation'
        then 'allocation_funded' else 'advance_disbursed' end,
      v_item.amount, 'KES',
      coalesce(nullif(btrim(p_provider_transaction_id), ''), btrim(p_originator_conversation_id)),
      v_now,
      jsonb_build_object('provider', 'Safaricom Daraja', 'confirmed', true, 'result_code', p_result_code)
    );
  end if;

  insert into public.audit_events (
    organisation_id, action, entity_type, entity_id, after_data
  ) values (
    v_item.organisation_id,
    case when p_result_code = '0'
      then 'disbursement_item.confirmed_success'
      else 'disbursement_item.confirmed_failure' end,
    'disbursement_item', v_item.id::text,
    jsonb_build_object(
      'result_code', p_result_code,
      'provider_transaction_id_present', nullif(btrim(p_provider_transaction_id), '') is not null,
      'confirmed_at', v_now
    )
  );

  select count(*),
         count(*) filter (where status = 'successful'),
         count(*) filter (where status = 'failed')
  into v_total, v_success, v_failed
  from public.disbursement_items
  where batch_id = v_item.batch_id;

  if v_total > 0 and v_success = v_total then
    v_next_status := 'successful';
  elsif v_total > 0 and v_success + v_failed = v_total and v_success > 0 and v_failed > 0 then
    v_next_status := 'partial_success';
  elsif v_total > 0 and v_failed = v_total then
    v_next_status := 'failed';
  elsif exists (
    select 1 from public.disbursement_items
    where batch_id = v_item.batch_id and status = 'outcome_unconfirmed'
  ) then
    v_next_status := 'outcome_unconfirmed';
  else
    v_next_status := 'submitted';
  end if;

  update public.disbursement_batches
  set status = v_next_status, updated_at = v_now
  where id = v_item.batch_id;

  return jsonb_build_object(
    'accepted', true, 'duplicate', false, 'ledger_repaired', false,
    'item_status', case when p_result_code = '0' then 'successful' else 'failed' end,
    'batch_status', v_next_status
  );
end;
$$;

revoke all on function public.record_daraja_b2c_callback(text, text, text, text)
  from public, anon, authenticated;
grant execute on function public.record_daraja_b2c_callback(text, text, text, text)
  to service_role;


-- Persist each gateway submission result and recompute the batch under the same
-- item-then-batch lock order used by the callback RPC. A fast callback can win
-- the race without a later HTTP response overwriting its terminal state.
create or replace function public.record_daraja_b2c_submission_result(
  p_item_id uuid,
  p_originator_conversation_id text,
  p_status text,
  p_conversation_id text,
  p_result_code text,
  p_result_description text,
  p_actor_user_id uuid
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_item public.disbursement_items%rowtype;
  v_batch public.disbursement_batches%rowtype;
  v_next_status text;
  v_total integer;
  v_success integer;
  v_failed integer;
  v_now timestamptz := now();
begin
  if auth.uid() is not null then
    raise exception 'submission result RPC is restricted to server-side service-role requests' using errcode = '42501';
  end if;
  if p_item_id is null or p_actor_user_id is null
     or p_status not in ('submitted', 'failed', 'outcome_unconfirmed')
     or nullif(btrim(p_originator_conversation_id), '') is null then
    raise exception 'invalid submission result arguments' using errcode = '22023';
  end if;

  select * into v_item
  from public.disbursement_items
  where id = p_item_id
  for update;
  if not found then
    raise exception 'disbursement item not found' using errcode = 'P0002';
  end if;

  select * into v_batch
  from public.disbursement_batches
  where id = v_item.batch_id
  for update;
  if not found then
    raise exception 'disbursement batch not found' using errcode = 'P0002';
  end if;

  if v_item.originator_conversation_id is distinct from btrim(p_originator_conversation_id) then
    raise exception 'originator conversation id does not match persisted item' using errcode = '22023';
  end if;

  -- The callback may have completed while the B2C HTTP request was in flight.
  -- Never downgrade a terminal callback result to submitted/unknown/failed.
  if v_item.status not in ('successful', 'failed') then
    if v_item.status <> 'outcome_unconfirmed' then
      raise exception 'item is not awaiting a gateway result: %', v_item.status using errcode = '22023';
    end if;

    update public.disbursement_items
    set status = p_status,
        conversation_id = coalesce(nullif(btrim(p_conversation_id), ''), conversation_id),
        result_code = coalesce(nullif(btrim(p_result_code), ''), result_code),
        result_description = coalesce(p_result_description, result_description),
        updated_at = v_now
    where id = v_item.id
    returning * into v_item;

    insert into public.audit_events (
      organisation_id, actor_user_id, action, entity_type, entity_id, after_data
    ) values (
      v_item.organisation_id, p_actor_user_id,
      'disbursement_item.gateway_submission_result', 'disbursement_item', v_item.id::text,
      jsonb_build_object(
        'status', v_item.status,
        'conversation_id_present', nullif(btrim(p_conversation_id), '') is not null,
        'result_code', nullif(btrim(p_result_code), '')
      )
    );
  end if;

  select count(*),
         count(*) filter (where status = 'successful'),
         count(*) filter (where status = 'failed')
  into v_total, v_success, v_failed
  from public.disbursement_items
  where batch_id = v_batch.id;

  if v_total > 0 and v_success = v_total then
    v_next_status := 'successful';
  elsif v_total > 0 and v_success + v_failed = v_total and v_success > 0 and v_failed > 0 then
    v_next_status := 'partial_success';
  elsif v_total > 0 and v_failed = v_total then
    v_next_status := 'failed';
  elsif exists (
    select 1 from public.disbursement_items
    where batch_id = v_batch.id and status = 'outcome_unconfirmed'
  ) then
    v_next_status := 'outcome_unconfirmed';
  else
    v_next_status := 'submitted';
  end if;

  update public.disbursement_batches
  set status = v_next_status, updated_at = v_now
  where id = v_batch.id;

  return jsonb_build_object(
    'item_status', v_item.status,
    'batch_status', v_next_status,
    'callback_already_won', v_item.status in ('successful', 'failed')
  );
end;
$$;

revoke all on function public.record_daraja_b2c_submission_result(uuid, text, text, text, text, text, uuid)
  from public, anon, authenticated;
grant execute on function public.record_daraja_b2c_submission_result(uuid, text, text, text, text, text, uuid)
  to service_role;


-- OAuth preflight failure happens before any B2C request. Restore the approved
-- batch only when no callback reference was persisted and no item was sent.
create or replace function public.restore_daraja_batch_after_auth_failure(
  p_batch_id uuid,
  p_actor_user_id uuid
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_batch public.disbursement_batches%rowtype;
  v_item_count integer;
begin
  if auth.uid() is not null then
    raise exception 'preflight recovery RPC is restricted to server-side service-role requests' using errcode = '42501';
  end if;
  if p_batch_id is null or p_actor_user_id is null then
    raise exception 'batch and actor are required' using errcode = '22023';
  end if;

  select * into v_batch
  from public.disbursement_batches
  where id = p_batch_id
  for update;
  if not found then
    raise exception 'disbursement batch not found' using errcode = 'P0002';
  end if;

  if v_batch.status <> 'outcome_unconfirmed'
     or exists (
       select 1 from public.disbursement_items
       where batch_id = p_batch_id
         and (status <> 'outcome_unconfirmed' or originator_conversation_id is not null)
     ) then
    return jsonb_build_object('restored', false, 'batch_status', v_batch.status);
  end if;

  select count(*) into v_item_count
  from public.disbursement_items
  where batch_id = p_batch_id;
  if v_item_count = 0 then
    raise exception 'disbursement batch has no items' using errcode = '22023';
  end if;

  update public.disbursement_items
  set status = 'approved', updated_at = now()
  where batch_id = p_batch_id and status = 'outcome_unconfirmed';

  update public.disbursement_batches
  set status = 'approved',
      result_summary = jsonb_build_object('preflight', 'daraja_oauth_failed', 'b2c_request_sent', false),
      updated_at = now()
  where id = p_batch_id;

  insert into public.audit_events (
    organisation_id, actor_user_id, action, entity_type, entity_id, after_data
  ) values (
    v_batch.organisation_id, p_actor_user_id,
    'disbursement_batch.preflight_failed', 'disbursement_batch', p_batch_id::text,
    jsonb_build_object('status', 'approved', 'b2c_request_sent', false, 'item_count', v_item_count)
  );

  return jsonb_build_object('restored', true, 'batch_status', 'approved');
end;
$$;

revoke all on function public.restore_daraja_batch_after_auth_failure(uuid, uuid)
  from public, anon, authenticated;
grant execute on function public.restore_daraja_batch_after_auth_failure(uuid, uuid)
  to service_role;
