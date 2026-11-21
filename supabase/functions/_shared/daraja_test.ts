import { normalizeDarajaPartyB } from "./daraja.ts";

function assertEquals(actual: string, expected: string): void {
  if (actual !== expected) throw new Error(`Expected ${expected}, received ${actual}`);
}
function assertThrows(fn: () => unknown, expectedMessage: string): void {
  try {
    fn();
  } catch (error) {
    if (error instanceof Error && error.message.includes(expectedMessage)) return;
    throw new Error(`Unexpected error: ${String(error)}`);
  }
  throw new Error("Expected function to throw.");
}

Deno.test("normalizes E.164 Safaricom 07 mobile number", () => {
  assertEquals(normalizeDarajaPartyB("+254712345678"), "254712345678");
});
Deno.test("normalizes E.164 Kenyan 01 mobile number", () => {
  assertEquals(normalizeDarajaPartyB("+254112345678"), "254112345678");
});
Deno.test("accepts already normalized Daraja PartyB", () => {
  assertEquals(normalizeDarajaPartyB("254712345678"), "254712345678");
});
Deno.test("trims surrounding whitespace", () => {
  assertEquals(normalizeDarajaPartyB("  +254712345678  "), "254712345678");
});
Deno.test("rejects local-format numbers", () => {
  assertThrows(() => normalizeDarajaPartyB("0712345678"), "E.164 format");
});
Deno.test("rejects malformed and non-Kenyan numbers", () => {
  assertThrows(() => normalizeDarajaPartyB("+15551234567"), "E.164 format");
  assertThrows(() => normalizeDarajaPartyB("+25471234567"), "E.164 format");
  assertThrows(() => normalizeDarajaPartyB("+254912345678"), "E.164 format");
});
