import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json",
};

const allowedStatuses = new Set([
  "under_review",
  "approved",
  "provisioning",
  "active",
  "rejected",
  "suspended",
  "closed",
]);

function response(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: corsHeaders,
  });
}

function isUuid(value: unknown): value is string {
  return typeof value === "string" &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}

Deno.serve(async (request: Request) => {
  if (request.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (request.method !== "POST") {
    return response(405, { error: "method_not_allowed" });
  }

  const authorization = request.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) {
    return response(401, { error: "authentication_required" });
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) {
    return response(503, { error: "server_configuration_incomplete" });
  }

  const token = authorization.slice("Bearer ".length).trim();
  const adminClient = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data: authData, error: authError } = await adminClient.auth.getUser(token);
  const user = authData?.user;
  if (authError || !user) {
    return response(401, { error: "invalid_session" });
  }

  // Only trusted app_metadata claims count. Never accept a role from user_metadata or request JSON.
  const appMetadata = user.app_metadata ?? {};
  const trustedRoles = Array.isArray(appMetadata.roles) ? appMetadata.roles : [];
  const isMobiwaveAdmin = appMetadata.mobiwave_admin === true ||
    appMetadata.role === "mobiwave_admin" ||
    appMetadata.role === "super_admin" ||
    appMetadata.is_super_admin === true ||
    trustedRoles.includes("mobiwave_admin") ||
    trustedRoles.includes("super_admin");
  if (!isMobiwaveAdmin) {
    return response(403, { error: "mobiwave_admin_required" });
  }

  let body: Record<string, unknown>;
  try {
    const parsed = await request.json();
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return response(400, { error: "invalid_json_body" });
    }
    body = parsed as Record<string, unknown>;
  } catch {
    return response(400, { error: "invalid_json_body" });
  }

  if (body.action === "list") {
    const { data: requests, error: listError } = await adminClient
      .from("cnc_enterprise_requests")
      .select("id, legal_name, trading_name, contact_name, contact_email, contact_phone, country_code, estimated_users, locations_summary, department_summary, configuration_requirements, status, reviewer_user_id, review_notes, reviewed_at, provisioned_organisation_id, created_at, updated_at")
      .order("created_at", { ascending: false })
      .limit(200);

    if (listError) {
      return response(500, { error: "enterprise_request_list_failed" });
    }
    return response(200, { requests: requests ?? [] });
  }

  const requestId = body.request_id;
  const toStatus = body.to_status;
  const reason = typeof body.reason === "string" ? body.reason.trim() : "";
  const reviewNotes = typeof body.review_notes === "string" ? body.review_notes.trim() : null;
  const organisationId = body.provisioned_organisation_id;

  if (!isUuid(requestId) || typeof toStatus !== "string" || !allowedStatuses.has(toStatus)) {
    return response(400, { error: "valid_request_id_and_target_status_required" });
  }
  if (reason.length < 3 || reason.length > 4000) {
    return response(400, { error: "reason_must_be_3_to_4000_characters" });
  }
  if (reviewNotes !== null && reviewNotes.length > 20000) {
    return response(400, { error: "review_notes_too_long" });
  }
  if (organisationId !== undefined && organisationId !== null && !isUuid(organisationId)) {
    return response(400, { error: "invalid_provisioned_organisation_id" });
  }

  const { data, error } = await adminClient.rpc("cnc_admin_transition_enterprise_request", {
    p_request_id: requestId,
    p_actor_user_id: user.id,
    p_to_status: toStatus,
    p_reason: reason,
    p_provisioned_organisation_id: organisationId ?? null,
    p_review_notes: reviewNotes,
  });

  if (error) {
    // Do not expose SQL, schema names or provider data to callers.
    const clientError = ["22023", "P0002", "23503", "55000"].includes(error.code ?? "");
    return response(clientError ? 422 : 500, {
      error: clientError ? "request_transition_rejected" : "request_transition_failed",
      code: error.code ?? "unknown",
      message: clientError ? error.message : "Unable to update enterprise request",
    });
  }

  return response(200, {
    request: {
      id: data.id,
      status: data.status,
      reviewer_user_id: data.reviewer_user_id,
      reviewed_at: data.reviewed_at,
      provisioned_organisation_id: data.provisioned_organisation_id,
      updated_at: data.updated_at,
    },
  });
});
