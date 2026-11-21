import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { normalizeDarajaPartyB } from "../_shared/daraja.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": Deno.env.get("APP_ORIGIN") ?? "null",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Vary": "Origin",
};
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), {
  status,
  headers: { ...corsHeaders, "Content-Type": "application/json" },
});

type PreparedItem = {
  id: string;
  phone_e164: string;
  amount: number;
  purpose: string;
};

type SubmissionStatus = "submitted" | "failed" | "outcome_unconfirmed";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const auth = req.headers.get("Authorization");
  if (!auth?.startsWith("Bearer ")) return json({ error: "authentication_required" }, 401);

  const url = Deno.env.get("SUPABASE_URL");
  const anon = Deno.env.get("SUPABASE_ANON_KEY");
  const service = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const consumerKey = Deno.env.get("DARAJA_CONSUMER_KEY");
  const consumerSecret = Deno.env.get("DARAJA_CONSUMER_SECRET");
  const shortcode = Deno.env.get("DARAJA_SHORTCODE");
  const initiator = Deno.env.get("DARAJA_INITIATOR_NAME");
  const securityCredential = Deno.env.get("DARAJA_SECURITY_CREDENTIAL");
  const resultUrl = Deno.env.get("DARAJA_RESULT_URL");
  const timeoutUrl = Deno.env.get("DARAJA_TIMEOUT_URL");
  const environment = Deno.env.get("DARAJA_ENVIRONMENT") ?? "sandbox";

  if (!url || !anon || !service) return json({ error: "server_configuration_incomplete" }, 500);
  if (!consumerKey || !consumerSecret || !shortcode || !initiator ||
      !securityCredential || !resultUrl || !timeoutUrl) {
    return json({
      error: "daraja_not_configured",
      message: "No live payment was attempted. Configure server-side Daraja secrets first.",
    }, 503);
  }
  if (!["sandbox", "production"].includes(environment)) {
    return json({ error: "invalid_daraja_environment" }, 500);
  }

  let body: { batch_id?: string };
  try {
    body = await req.json();
  } catch {
    return json({ error: "invalid_json" }, 400);
  }
  if (!body.batch_id || !/^[0-9a-f-]{36}$/i.test(body.batch_id)) {
    return json({ error: "valid_batch_id_required" }, 400);
  }

  const userClient = createClient(url, anon, {
    global: { headers: { Authorization: auth } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  if (userError || !userData.user) return json({ error: "invalid_session" }, 401);

  const { data: permitted, error: permissionError } = await userClient.rpc(
    "has_permission",
    { p_permission: "disbursements.submit" },
  );
  if (permissionError || permitted !== true) return json({ error: "permission_denied" }, 403);

  // This RPC locks the approved batch and moves its items to outcome_unconfirmed
  // before any external request. Ambiguous requests must never be auto-retried.
  const { data: prepared, error: prepareError } = await userClient.rpc(
    "prepare_disbursement_submission",
    { p_batch_id: body.batch_id },
  );
  if (prepareError || !prepared?.items?.length) {
    return json({
      error: "batch_not_ready",
      message: prepareError?.message ?? "No eligible items.",
    }, 409);
  }

  const base = environment === "production"
    ? "https://api.safaricom.co.ke"
    : "https://sandbox.safaricom.co.ke";
  const admin = createClient(url, service, { auth: { persistSession: false } });

  let token: string;
  try {
    const tokenResponse = await fetch(
      base + "/oauth/v1/generate?grant_type=client_credentials",
      {
        headers: { Authorization: "Basic " + btoa(consumerKey + ":" + consumerSecret) },
        signal: AbortSignal.timeout(10000),
      },
    );
    if (!tokenResponse.ok) throw new Error("OAuth rejected");
    const tokenBody = await tokenResponse.json();
    if (typeof tokenBody.access_token !== "string") throw new Error("Missing token");
    token = tokenBody.access_token;
  } catch {
    // No B2C payment request was issued. Keep the batch locked for reconciliation;
    // do not claim success or automatically restore an approved batch.
    return json({
      status: "outcome_unconfirmed",
      message: "Daraja authentication failed before submission; reconcile the batch before retry.",
    }, 502);
  }

  const outcomes: Array<{ item_id: string; status: string; message: string }> = [];
  let batchStatus = "outcome_unconfirmed";

  for (const item of prepared.items as PreparedItem[]) {
    const originatorId = item.id.replace(/-/g, "").slice(0, 18);

    // A callback can only be matched if its lookup key is durable before the
    // network request. Fail closed: never contact Daraja if this write fails.
    const { data: persisted, error: originatorError } = await admin
      .from("disbursement_items")
      .update({
        originator_conversation_id: originatorId,
        updated_at: new Date().toISOString(),
      })
      .eq("id", item.id)
      .eq("status", "outcome_unconfirmed")
      .is("originator_conversation_id", null)
      .select("id")
      .maybeSingle();

    if (originatorError || !persisted) {
      return json({
        error: "originator_persistence_failed",
        batch_id: body.batch_id,
        outcomes,
        message: "No Daraja request was sent for this recipient because its callback reference could not be persisted. Reconcile before retry.",
      }, 500);
    }

    let nextStatus: SubmissionStatus = "outcome_unconfirmed";
    let conversationId: string | null = null;
    let resultCode: string | null = null;
    let resultDescription = "Ambiguous gateway response; reconcile before retry.";

    try {
      const partyB = normalizeDarajaPartyB(item.phone_e164);
      const response = await fetch(base + "/mpesa/b2c/v3/paymentrequest", {
        method: "POST",
        headers: {
          Authorization: "Bearer " + token,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          OriginatorConversationID: originatorId,
          InitiatorName: initiator,
          SecurityCredential: securityCredential,
          CommandID: "BusinessPayment",
          Amount: Math.round(Number(item.amount)),
          PartyA: shortcode,
          PartyB: partyB,
          Remarks: item.purpose.slice(0, 100),
          QueueTimeOutURL: timeoutUrl,
          ResultURL: resultUrl,
          Occasion: "MW-" + body.batch_id.slice(0, 8),
        }),
        signal: AbortSignal.timeout(15000),
      });
      const responseBody = await response.json().catch(() => ({}));

      if (response.ok && responseBody.ResponseCode === "0") {
        nextStatus = "submitted";
        conversationId = typeof responseBody.ConversationID === "string"
          ? responseBody.ConversationID
          : null;
        resultDescription = typeof responseBody.ResponseDescription === "string"
          ? responseBody.ResponseDescription
          : "Accepted by gateway; awaiting callback confirmation.";
      } else if (!response.ok && typeof responseBody.errorCode === "string") {
        nextStatus = "failed";
        resultCode = responseBody.errorCode;
        resultDescription = String(responseBody.errorMessage ?? "Daraja rejected the request.");
      } else {
        resultCode = typeof responseBody.ResponseCode === "string"
          ? responseBody.ResponseCode
          : typeof responseBody.errorCode === "string"
          ? responseBody.errorCode
          : null;
        resultDescription = String(
          responseBody.errorMessage ??
            responseBody.ResponseDescription ??
            "Ambiguous gateway response; reconcile before retry.",
        );
      }
    } catch (error) {
      // A timeout or malformed network response does not prove the payment failed.
      resultDescription = error instanceof Error && error.message.includes("E.164")
        ? "Recipient phone is invalid; no payment request was sent."
        : "Network outcome is unknown; check Daraja before retry.";
      if (resultDescription.startsWith("Recipient phone")) nextStatus = "failed";
    }

    const { data: recorded, error: recordError } = await admin.rpc(
      "record_daraja_b2c_submission_result",
      {
        p_item_id: item.id,
        p_originator_conversation_id: originatorId,
        p_status: nextStatus,
        p_conversation_id: conversationId,
        p_result_code: resultCode,
        p_result_description: resultDescription.slice(0, 500),
        p_actor_user_id: userData.user.id,
      },
    );

    if (recordError || !recorded) {
      // The gateway may already have accepted this item. Stop sending further
      // recipients; the callback reference is durable and reconciliation is required.
      return json({
        error: "submission_result_persistence_failed",
        batch_id: body.batch_id,
        outcomes,
        message: "Daraja may have accepted this recipient, but its result could not be committed. Do not retry automatically; reconcile using the persisted originator reference.",
      }, 502);
    }

    const actualItemStatus = String(recorded.item_status ?? nextStatus);
    batchStatus = String(recorded.batch_status ?? batchStatus);
    outcomes.push({
      item_id: item.id,
      status: actualItemStatus,
      message: actualItemStatus === "successful"
        ? "Payment confirmed by callback."
        : actualItemStatus === "failed"
        ? "Recipient payment failed."
        : actualItemStatus === "submitted"
        ? "Accepted by gateway; not confirmed paid."
        : "Outcome unconfirmed; reconcile before retry.",
    });
  }

  return json({
    batch_id: body.batch_id,
    status: batchStatus,
    outcomes,
  }, 202);
});
