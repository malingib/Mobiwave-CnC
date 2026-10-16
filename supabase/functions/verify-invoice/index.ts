import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": Deno.env.get("APP_ORIGIN") ?? "null",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Vary": "Origin",
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) return json({ error: "authentication_required" }, 401);

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !anonKey || !serviceKey) {
    return json({ error: "server_configuration_incomplete" }, 500);
  }

  const userClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authHeader } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  if (userError || !userData.user) return json({ error: "invalid_session" }, 401);

  const { data: permitted, error: permissionError } = await userClient.rpc("has_permission", {
    p_permission: "expenses.verify",
  });
  if (permissionError || permitted !== true) return json({ error: "permission_denied" }, 403);

  let payload: { expense_id?: string };
  try {
    payload = await req.json();
  } catch {
    return json({ error: "invalid_json" }, 400);
  }
  if (!payload.expense_id || !/^[0-9a-f-]{36}$/i.test(payload.expense_id)) {
    return json({ error: "valid_expense_id_required" }, 400);
  }

  const { data: orgId, error: orgError } = await userClient.rpc("current_organisation_id");
  if (orgError || typeof orgId !== "string") {
    return json({ error: "employee_organisation_not_configured" }, 403);
  }

  const admin = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: expense, error: expenseError } = await admin.from("expenses")
    .select("id,organisation_id,submitted_by,supplier_pin,cu_invoice_number,gross_amount,vat_amount,expense_date")
    .eq("id", payload.expense_id)
    .eq("organisation_id", orgId)
    .maybeSingle();

  if (expenseError || !expense) return json({ error: "expense_not_found" }, 404);

  const missingFields: string[] = [];
  if (!expense.supplier_pin?.trim()) missingFields.push("supplier_pin");
  if (!expense.cu_invoice_number?.trim()) missingFields.push("invoice_number");
  if (expense.gross_amount === null || Number(expense.gross_amount) < 0) missingFields.push("gross_amount");
  if (!expense.expense_date) missingFields.push("invoice_date");
  if (expense.vat_amount === null || Number(expense.vat_amount) < 0) missingFields.push("vat_amount");

  // KRA's published OSCU/VSCU APIs concern system-to-system invoicing. A general
  // supplier-invoice lookup endpoint/contract has not been confirmed. Do not guess
  // an endpoint, payload, response schema, or credentials. Fail closed until KRA
  // confirms an approved verification operation and grants access.
  const reasonCode = missingFields.length ? "missing_required_invoice_fields" : "kra_lookup_not_yet_authorised";
  const reasonDetail = missingFields.length
    ? `Required invoice fields are missing or invalid: ${missingFields.join(", ")}. No authoritative verification was performed.`
    : "Direct KRA invoice-lookup operation and approved API contract are not yet confirmed. No external verification was performed.";

  const { error: expenseUpdateError } = await admin.from("expenses")
    .update({ verification_status: "needs_review", status: "needs_review", updated_at: new Date().toISOString() })
    .eq("id", expense.id)
    .eq("organisation_id", orgId);
  if (expenseUpdateError) return json({ error: "could_not_queue_expense_for_review" }, 500);

  const { queueId, error: queueError } = await enqueueReview(admin, expense, reasonCode, reasonDetail);
  if (queueError || !queueId) {
    return json({
      status: "needs_review",
      verified: false,
      queued_for_manual_review: false,
      error: "manual_review_queue_unavailable",
      message: "No authoritative KRA verification was performed. Queue creation failed; retry or contact an administrator.",
    }, 503);
  }

  const { error: auditError } = await admin.from("audit_events").insert({
    organisation_id: expense.organisation_id,
    actor_user_id: userData.user.id,
    action: "invoice_verification.queued_for_manual_review",
    entity_type: "expense",
    entity_id: expense.id,
    after_data: {
      queue_id: queueId,
      reason_code: reasonCode,
      verified: false,
      missing_fields: missingFields,
    },
  });
  if (auditError) {
    return json({
      status: "needs_review",
      verified: false,
      queued_for_manual_review: true,
      queue_id: queueId,
      error: "audit_record_failed",
      message: "The review item exists, but the audit event could not be recorded. Contact an administrator before proceeding.",
    }, 500);
  }

  return json({
    status: "needs_review",
    verified: false,
    queued_for_manual_review: true,
    queue_id: queueId,
    missing_fields: missingFields,
    fields_captured: {
      supplier_pin: Boolean(expense.supplier_pin?.trim()),
      invoice_number: Boolean(expense.cu_invoice_number?.trim()),
      gross_amount: expense.gross_amount !== null,
      invoice_date: Boolean(expense.expense_date),
      vat_amount: expense.vat_amount !== null,
    },
    message: missingFields.length
      ? "Invoice details are incomplete. No authoritative KRA verification was performed; the item is queued for authorised manual review."
      : "No authoritative KRA verification was performed. The item is queued for authorised manual review.",
  }, 202);
});

async function enqueueReview(
  admin: ReturnType<typeof createClient>,
  expense: { id: string; organisation_id: string },
  reasonCode: string,
  reasonDetail: string,
): Promise<{ queueId: string | null; error: unknown | null }> {
  const { data: inserted, error: insertError } = await admin.from("invoice_manual_review_queue")
    .insert({
      organisation_id: expense.organisation_id,
      expense_id: expense.id,
      reason_code: reasonCode,
      reason_detail: reasonDetail,
    })
    .select("id")
    .maybeSingle();

  if (!insertError && inserted?.id) return { queueId: inserted.id, error: null };

  // A concurrent or previous request may already have created the open queue item.
  const { data: existing, error: selectError } = await admin.from("invoice_manual_review_queue")
    .select("id")
    .eq("expense_id", expense.id)
    .in("status", ["pending", "in_review"])
    .maybeSingle();

  if (!selectError && existing?.id) return { queueId: existing.id, error: null };
  return { queueId: null, error: insertError ?? selectError };
}
