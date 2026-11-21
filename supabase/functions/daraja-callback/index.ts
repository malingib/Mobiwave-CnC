import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { parseDarajaB2CResult } from "../_shared/daraja-callback.ts";

function constantTimeEqual(left: string, right: string): boolean {
  const a = new TextEncoder().encode(left);
  const b = new TextEncoder().encode(right);
  if (a.length !== b.length) return false;
  let difference = 0;
  for (let index = 0; index < a.length; index++) difference |= a[index] ^ b[index];
  return difference === 0;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("Method not allowed", { status: 405 });

  const secret = Deno.env.get("DARAJA_CALLBACK_SECRET");
  const supplied = new URL(req.url).searchParams.get("token") ?? "";
  if (!secret || !supplied || !constantTimeEqual(secret, supplied)) {
    return new Response("Unauthorized", { status: 401 });
  }

  const url = Deno.env.get("SUPABASE_URL");
  const service = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !service) return new Response("Server configuration incomplete", { status: 500 });

  const contentLength = Number(req.headers.get("content-length") ?? "0");
  if (contentLength > 64 * 1024) return new Response("Payload too large", { status: 413 });

  let parsed;
  try {
    parsed = parseDarajaB2CResult(await req.json());
  } catch {
    return new Response("Invalid callback payload", { status: 400 });
  }

  const admin = createClient(url, service, { auth: { persistSession: false, autoRefreshToken: false } });
  const { data, error } = await admin.rpc("record_daraja_b2c_callback", {
    p_originator_conversation_id: parsed.originatorConversationId,
    p_result_code: parsed.resultCode,
    p_result_description: parsed.resultDescription,
    p_provider_transaction_id: parsed.providerTransactionId,
  });

  if (error) {
    // Return non-2xx so Daraja retries; the database function is transactional.
    console.error("Daraja callback persistence failed", {
      code: error.code,
      message: error.message,
      originatorConversationId: parsed.originatorConversationId,
    });
    return new Response("Callback persistence failed", { status: 500 });
  }

  return new Response(JSON.stringify({
    accepted: true,
    duplicate: data?.duplicate === true,
    ledger_repaired: data?.ledger_repaired === true,
    item_status: data?.item_status,
    batch_status: data?.batch_status,
  }), { headers: { "Content-Type": "application/json" } });
});
