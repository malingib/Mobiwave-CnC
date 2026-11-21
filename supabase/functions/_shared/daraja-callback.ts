export interface DarajaB2CResult {
  originatorConversationId: string;
  resultCode: string;
  resultDescription: string;
  providerTransactionId: string | null;
}

export function parseDarajaB2CResult(payload: unknown): DarajaB2CResult {
  if (!payload || typeof payload !== "object" || !("Result" in payload)) {
    throw new Error("Invalid Daraja callback envelope.");
  }
  const result = (payload as { Result?: unknown }).Result;
  if (!result || typeof result !== "object") {
    throw new Error("Invalid Daraja callback result.");
  }
  const value = result as Record<string, unknown>;
  const originatorConversationId = typeof value.OriginatorConversationID === "string"
    ? value.OriginatorConversationID.trim() : "";
  const resultCode = typeof value.ResultCode === "number" || typeof value.ResultCode === "string"
    ? String(value.ResultCode) : "";
  if (!originatorConversationId || originatorConversationId.length > 128 || !/^\d+$/.test(resultCode)) {
    throw new Error("Daraja callback is missing a valid originator ID or result code.");
  }
  const resultDescription = typeof value.ResultDesc === "string" ? value.ResultDesc.slice(0, 500) : "";
  let receipt: string | null = null;
  const parameters = value.ResultParameters;
  if (parameters && typeof parameters === "object" && "ResultParameter" in parameters) {
    const list = (parameters as { ResultParameter?: unknown }).ResultParameter;
    if (Array.isArray(list)) {
      for (const entry of list) {
        if (!entry || typeof entry !== "object") continue;
        const pair = entry as Record<string, unknown>;
        if (pair.Key === "TransactionReceipt" && (typeof pair.Value === "string" || typeof pair.Value === "number")) {
          receipt = String(pair.Value).slice(0, 128);
          break;
        }
      }
    }
  }
  const transactionId = typeof value.TransactionID === "string" ? value.TransactionID : null;
  return {
    originatorConversationId,
    resultCode,
    resultDescription,
    providerTransactionId: (transactionId || receipt)?.slice(0, 128) ?? null,
  };
}
