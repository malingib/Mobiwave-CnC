import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
const corsHeaders = {
  "Access-Control-Allow-Origin": Deno.env.get("APP_ORIGIN") ?? "null",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS", "Vary": "Origin",
};
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), {
  status, headers: { ...corsHeaders, "Content-Type": "application/json" },
});
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const auth = req.headers.get("Authorization");
  if (!auth?.startsWith("Bearer ")) return json({ error: "authentication_required" }, 401);
  const url = Deno.env.get("SUPABASE_URL"), anon = Deno.env.get("SUPABASE_ANON_KEY");
  const service = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const consumerKey = Deno.env.get("DARAJA_CONSUMER_KEY"), consumerSecret = Deno.env.get("DARAJA_CONSUMER_SECRET");
  const shortcode = Deno.env.get("DARAJA_SHORTCODE"), initiator = Deno.env.get("DARAJA_INITIATOR_NAME");
  const securityCredential = Deno.env.get("DARAJA_SECURITY_CREDENTIAL");
  const resultUrl = Deno.env.get("DARAJA_RESULT_URL"), timeoutUrl = Deno.env.get("DARAJA_TIMEOUT_URL");
  const environment = Deno.env.get("DARAJA_ENVIRONMENT") ?? "sandbox";
  if (!url || !anon || !service) return json({ error: "server_configuration_incomplete" }, 500);
  if (!consumerKey || !consumerSecret || !shortcode || !initiator || !securityCredential || !resultUrl || !timeoutUrl) {
    return json({ error: "daraja_not_configured", message: "No live payment was attempted. Configure server-side Daraja secrets first." }, 503);
  }
  if (!["sandbox", "production"].includes(environment)) return json({ error: "invalid_daraja_environment" }, 500);
  let body: { batch_id?: string };
  try { body = await req.json(); } catch { return json({ error: "invalid_json" }, 400); }
  if (!body.batch_id || !/^[0-9a-f-]{36}$/i.test(body.batch_id)) return json({ error: "valid_batch_id_required" }, 400);
  const userClient = createClient(url, anon, {
    global: { headers: { Authorization: auth } }, auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  if (userError || !userData.user) return json({ error: "invalid_session" }, 401);
  const { data: permitted } = await userClient.rpc("has_permission", { p_permission: "disbursements.submit" });
  if (permitted !== true) return json({ error: "permission_denied" }, 403);

  // Lock and mark outcome_unconfirmed before any network call. Never auto-retry uncertain requests.
  const { data: prepared, error: prepareError } = await userClient.rpc("prepare_disbursement_submission", { p_batch_id: body.batch_id });
  if (prepareError || !prepared?.items?.length) return json({ error: "batch_not_ready", message: prepareError?.message ?? "No eligible items." }, 409);

  const base = environment === "production" ? "https://api.safaricom.co.ke" : "https://sandbox.safaricom.co.ke";
  const admin = createClient(url, service, { auth: { persistSession: false } });
  let token: string;
  try {
    const tokenResponse = await fetch(base + "/oauth/v1/generate?grant_type=client_credentials", {
      headers: { Authorization: "Basic " + btoa(consumerKey + ":" + consumerSecret) }, signal: AbortSignal.timeout(10000),
    });
    if (!tokenResponse.ok) throw new Error("OAuth rejected");
    const tokenBody = await tokenResponse.json();
    if (typeof tokenBody.access_token !== "string") throw new Error("Missing token");
    token = tokenBody.access_token;
  } catch {
    await admin.from("disbursement_batches").update({ status: "outcome_unconfirmed",
      result_summary: { message: "Gateway OAuth/network outcome unconfirmed; reconcile before retry." },
      updated_at: new Date().toISOString() }).eq("id", body.batch_id).eq("status", "outcome_unconfirmed");
    return json({ status: "outcome_unconfirmed", message: "No conclusive gateway session. Reconcile before retry." }, 502);
  }

    const outcomes: Array<{ item_id: string; status: string; message: string }> = [];
  for (const item of prepared.items as Array<{ id: string; phone_e164: string; amount: number; purpose: string; idempotency_key: string }>) {
    const originatorId = item.id.replace(/-/g, "").slice(0, 18); // deterministic, provider-safe length; item UUID remains the idempotency source.
    await admin.from("disbursement_items").update({ originator_conversation_id: originatorId, updated_at: new Date().toISOString() }).eq("id", item.id).eq("status", "outcome_unconfirmed");
    try {
      const response = await fetch(base + "/mpesa/b2c/v3/paymentrequest", {
        method: "POST", headers: { Authorization: "Bearer " + token, "Content-Type": "application/json" },
        body: JSON.stringify({
          OriginatorConversationID: originatorId, InitiatorName: initiator, SecurityCredential: securityCredential,
          CommandID: "BusinessPayment", Amount: Math.round(Number(item.amount)), PartyA: shortcode,
          PartyB: item.phone_e164.replace(/^\\+/, ""), Remarks: item.purpose.slice(0, 100),
          QueueTimeOutURL: timeoutUrl, ResultURL: resultUrl, Occasion: "MW-" + body.batch_id.slice(0, 8),
        }),
        signal: AbortSignal.timeout(15000),
      });
      const responseBody = await response.json().catch(() => ({}));
      if (response.ok && responseBody.ResponseCode === "0") {
        await admin.from("disbursement_items").update({ status: "submitted", conversation_id: responseBody.ConversationID ?? null,
          result_description: responseBody.ResponseDescription ?? "Accepted by gateway; awaiting callback confirmation.",
          updated_at: new Date().toISOString() }).eq("id", item.id).eq("status", "outcome_unconfirmed");
        outcomes.push({ item_id: item.id, status: "submitted", message: "Accepted by gateway; not confirmed paid." });
      } else {
        const explicitReject = !response.ok && typeof responseBody.errorCode === "string";
        const status = explicitReject ? "failed" : "outcome_unconfirmed";
        await admin.from("disbursement_items").update({ status, result_code: responseBody.errorCode ?? null,
          result_description: String(responseBody.errorMessage ?? responseBody.ResponseDescription ?? "Ambiguous response; reconcile before retry."),
          updated_at: new Date().toISOString() }).eq("id", item.id);
        outcomes.push({ item_id: item.id, status, message: "No success is assumed without a confirmed callback." });
      }
    } catch {
      await admin.from("disbursement_items").update({ status: "outcome_unconfirmed",
        result_description: "Network timeout; check provider result before retry.", updated_at: new Date().toISOString() }).eq("id", item.id);
      outcomes.push({ item_id: item.id, status: "outcome_unconfirmed", message: "Timeout; reconcile before retry." });
    }
  }
  const hasUnknown = outcomes.some((x) => x.status === "outcome_unconfirmed");
  const allFailed = outcomes.length > 0 && outcomes.every((x) => x.status === "failed");
  await admin.from("disbursement_batches").update({
    status: hasUnknown ? "outcome_unconfirmed" : allFailed ? "failed" : "submitted",
    result_summary: { item_count: outcomes.length, outcomes }, updated_at: new Date().toISOString(),
  }).eq("id", body.batch_id);
  await admin.from("audit_events").insert({
    organisation_id: prepared.organisation_id, actor_user_id: userData.user.id,
    action: "disbursement_batch.gateway_submission", entity_type: "disbursement_batch",
    entity_id: body.batch_id, after_data: { outcomes },
  });
  return json({ batch_id: body.batch_id, status: hasUnknown ? "outcome_unconfirmed" : allFailed ? "failed" : "submitted", outcomes }, 202);
});
