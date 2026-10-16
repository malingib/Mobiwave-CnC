import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("Method not allowed", { status: 405 });
  const secret = Deno.env.get("DARAJA_CALLBACK_SECRET");
  const supplied = new URL(req.url).searchParams.get("token");
  if (!secret || !supplied || supplied !== secret) return new Response("Unauthorized", { status: 401 });
  const url = Deno.env.get("SUPABASE_URL"), service = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !service) return new Response("Server configuration incomplete", { status: 500 });
  let body: any;
  try { body = await req.json(); } catch { return new Response("Invalid JSON", { status: 400 }); }
  const result = body?.Result;
  if (!result || typeof result.OriginatorConversationID !== "string" || typeof result.ResultCode === "undefined") {
    return new Response("Invalid callback shape", { status: 400 });
  }
  const admin = createClient(url, service, { auth: { persistSession: false } });
  const { data: item, error } = await admin.from("disbursement_items")
    .select("id,organisation_id,batch_id,employee_id,amount,status").eq("originator_conversation_id", result.OriginatorConversationID).maybeSingle();
  if (error || !item) return new Response("Unknown transaction reference", { status: 404 });
  if (["successful", "failed"].includes(item.status)) {
    return new Response(JSON.stringify({ accepted: true, duplicate: true }), { headers: { "Content-Type": "application/json" } });
  }
  const successful = Number(result.ResultCode) === 0;
  const parameters = result.ResultParameters?.ResultParameter;
  const metadata = Array.isArray(parameters) ? Object.fromEntries(parameters.map((p: any) => [p.Key, p.Value])) : {};
  const now = new Date().toISOString();
  const { error: updateError } = await admin.from("disbursement_items").update({
    status: successful ? "successful" : "failed", result_code: String(result.ResultCode),
    result_description: String(result.ResultDesc ?? ""),
    provider_transaction_id: result.TransactionID ? String(result.TransactionID) : (metadata.TransactionReceipt ? String(metadata.TransactionReceipt) : null),
    confirmed_at: now, updated_at: now,
  }).eq("id", item.id);
  if (updateError) return new Response("Could not persist callback", { status: 500 });
  await admin.from("audit_events").insert({
    organisation_id: item.organisation_id,
    action: successful ? "disbursement_item.confirmed_success" : "disbursement_item.confirmed_failure",
    entity_type: "disbursement_item", entity_id: item.id,
    after_data: { result_code: String(result.ResultCode), transaction_id_present: Boolean(metadata.TransactionReceipt), confirmed_at: now },
  });
  if (successful) {
    const { data: batch } = await admin.from("disbursement_batches").select("purpose").eq("id", item.batch_id).maybeSingle();
    const transactionType = batch?.purpose === "expense_allocation" ? "allocation_funded" : "advance_disbursed";
    await admin.from("fund_transactions").upsert({
      organisation_id: item.organisation_id, employee_id: item.employee_id, disbursement_item_id: item.id,
      transaction_type: transactionType, amount: item.amount, currency: "KES",
      external_reference: result.TransactionID ? String(result.TransactionID) : String(result.OriginatorConversationID),
      effective_at: now, metadata: { provider: "Safaricom Daraja", confirmed: true },
    }, { onConflict: "disbursement_item_id", ignoreDuplicates: true });
  }
  const { data: items } = await admin.from("disbursement_items").select("status").eq("batch_id", item.batch_id);
  const states = (items ?? []).map((x: { status: string }) => x.status);
  const nextStatus = states.length && states.every((s: string) => s === "successful") ? "successful"
    : states.every((s: string) => ["successful", "failed"].includes(s)) && states.some((s: string) => s === "successful") && states.some((s: string) => s === "failed") ? "partial_success"
    : states.every((s: string) => ["successful", "failed"].includes(s)) && states.some((s: string) => s === "successful") ? "successful"
    : states.every((s: string) => s === "failed") ? "failed"
    : states.some((s: string) => s === "outcome_unconfirmed") ? "outcome_unconfirmed" : "submitted";
  await admin.from("disbursement_batches").update({ status: nextStatus, updated_at: now }).eq("id", item.batch_id);
  return new Response(JSON.stringify({ accepted: true }), { headers: { "Content-Type": "application/json" } });
});
