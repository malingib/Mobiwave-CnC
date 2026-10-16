import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": Deno.env.get("APP_ORIGIN") ?? "null",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Vary": "Origin",
};
function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { ...corsHeaders, "Content-Type": "application/json" } });
}
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) return json({ error: "authentication_required" }, 401);

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !anonKey || !serviceKey) return json({ error: "server_configuration_incomplete" }, 500);
  const userClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authHeader } }, auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  if (userError || !userData.user) return json({ error: "invalid_session" }, 401);
  const { data: permitted, error: permissionError } = await userClient.rpc("has_permission", { p_permission: "expenses.verify" });
  if (permissionError || permitted !== true) return json({ error: "permission_denied" }, 403);

  let payload: { expense_id?: string };
  try { payload = await req.json(); } catch { return json({ error: "invalid_json" }, 400); }
  if (!payload.expense_id || !/^[0-9a-f-]{36}$/i.test(payload.expense_id)) return json({ error: "valid_expense_id_required" }, 400);

  const { data: orgId } = await userClient.rpc("current_organisation_id");
  if (typeof orgId !== "string") return json({ error: "employee_organisation_not_configured" }, 403);
  const admin = createClient(supabaseUrl, serviceKey, { auth: { persistSession: false } });
  const { data: expense, error: expenseError } = await admin.from("expenses")
    .select("id,organisation_id,supplier_pin,cu_invoice_number,gross_amount,expense_date")
    .eq("id", payload.expense_id).eq("organisation_id", orgId).maybeSingle();
  if (expenseError || !expense) return json({ error: "expense_not_found" }, 404);

  const providerUrl = Deno.env.get("ETIMS_VERIFICATION_URL");
  const providerKey = Deno.env.get("ETIMS_VERIFICATION_API_KEY");
  if (!providerUrl || !providerKey) {
    await admin.from("expenses").update({ verification_status: "awaiting_verification" }).eq("id", expense.id);
    return json({ status: "awaiting_verification", verified: false,
      message: "No authorised eTIMS verification provider is configured. Receipt capture or QR scanning is not verification." }, 503);
  }

  let providerResponse: Response;
  try {
    providerResponse = await fetch(providerUrl, {
      method: "POST", headers: { "Content-Type": "application/json", "Authorization": "Bearer " + providerKey },
      body: JSON.stringify({ supplier_pin: expense.supplier_pin, cu_invoice_number: expense.cu_invoice_number,
        gross_amount: expense.gross_amount, expense_date: expense.expense_date }),
      signal: AbortSignal.timeout(12000),
    });
  } catch {
    await admin.from("expenses").update({ verification_status: "awaiting_verification" }).eq("id", expense.id);
    return json({ status: "awaiting_verification", verified: false, message: "Provider unavailable; retry or use approved manual review." }, 503);
  }
  if (!providerResponse.ok) {
    await admin.from("expenses").update({ verification_status: "awaiting_verification" }).eq("id", expense.id);
    return json({ status: "awaiting_verification", verified: false, message: "Provider returned no conclusive result." }, 502);
  }

  let result: { status?: string; reference?: string; checked_at?: string; code?: string };
  try { result = await providerResponse.json(); } catch {
    return json({ status: "needs_review", verified: false, message: "Provider response was not valid JSON." }, 502);
  }
  const mapping: Record<string, string> = { verified: "externally_verified", not_verified: "not_verified", needs_review: "needs_review" };
  const status = result.status ? mapping[result.status] : undefined;
  if (!status) return json({ status: "needs_review", verified: false, message: "Unrecognised provider response; no verified status assigned." }, 502);
  const checkedAt = result.checked_at && !Number.isNaN(Date.parse(result.checked_at))
    ? new Date(result.checked_at).toISOString() : new Date().toISOString();

  const { error: insertError } = await admin.from("invoice_verifications").insert({
    organisation_id: expense.organisation_id, expense_id: expense.id, provider: new URL(providerUrl).host,
    status, provider_reference: result.reference ?? null, checked_at: checkedAt, response_code: result.code ?? null,
    response_summary: { status, reference: result.reference ?? null }, requested_by: userData.user.id,
  });
  if (insertError) return json({ error: "could_not_record_verification" }, 500);
  const verificationStatus = status === "externally_verified" ? status : status === "needs_review" ? "needs_review" : "verification_failed";
  const { error: updateError } = await admin.from("expenses").update({ verification_status: verificationStatus, updated_at: new Date().toISOString() }).eq("id", expense.id);
  if (updateError) return json({ error: "could_not_update_expense_status" }, 500);
  await admin.from("audit_events").insert({
    organisation_id: expense.organisation_id, actor_user_id: userData.user.id, action: "invoice_verification.completed",
    entity_type: "expense", entity_id: expense.id,
    after_data: { status: verificationStatus, provider: new URL(providerUrl).host, checked_at: checkedAt },
  });
  return json({ status: verificationStatus, verified: verificationStatus === "externally_verified", checked_at: checkedAt });
});
