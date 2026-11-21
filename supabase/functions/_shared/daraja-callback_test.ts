import { parseDarajaB2CResult } from "./daraja-callback.ts";

function assertEquals(actual: unknown, expected: unknown): void {
  if (actual !== expected) throw new Error(`Expected ${String(expected)}, received ${String(actual)}`);
}
function assertThrows(fn: () => unknown): void {
  try { fn(); } catch { return; }
  throw new Error("Expected invalid callback to throw.");
}

Deno.test("parses successful callback and transaction receipt", () => {
  const parsed = parseDarajaB2CResult({
    Result: {
      OriginatorConversationID: "0123456789abcdef01",
      ResultCode: 0,
      ResultDesc: "The service request is processed successfully.",
      ResultParameters: { ResultParameter: [{ Key: "TransactionReceipt", Value: "QAB123XYZ" }] },
    },
  });
  assertEquals(parsed.originatorConversationId, "0123456789abcdef01");
  assertEquals(parsed.resultCode, "0");
  assertEquals(parsed.providerTransactionId, "QAB123XYZ");
});

Deno.test("uses provider TransactionID when supplied", () => {
  const parsed = parseDarajaB2CResult({
    Result: { OriginatorConversationID: "origin-123", ResultCode: "1", TransactionID: "provider-456" },
  });
  assertEquals(parsed.providerTransactionId, "provider-456");
  assertEquals(parsed.resultCode, "1");
});

Deno.test("handles callback without optional description or receipt", () => {
  const parsed = parseDarajaB2CResult({
    Result: { OriginatorConversationID: "origin-123", ResultCode: 0 },
  });
  assertEquals(parsed.resultDescription, "");
  assertEquals(parsed.providerTransactionId, null);
});

Deno.test("rejects missing result envelope", () => {
  assertThrows(() => parseDarajaB2CResult({}));
});

Deno.test("rejects missing originator reference", () => {
  assertThrows(() => parseDarajaB2CResult({ Result: { ResultCode: 0 } }));
});

Deno.test("rejects non-numeric result codes", () => {
  assertThrows(() => parseDarajaB2CResult({ Result: { OriginatorConversationID: "origin-123", ResultCode: "success" } }));
});

Deno.test("ignores malformed optional parameter entries", () => {
  const parsed = parseDarajaB2CResult({
    Result: {
      OriginatorConversationID: "origin-123", ResultCode: 0,
      ResultParameters: { ResultParameter: [null, {}, { Key: "TransactionReceipt", Value: 12345 }] },
    },
  });
  assertEquals(parsed.providerTransactionId, "12345");
});
