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

import { isDefinitiveDarajaRejection } from "./daraja.ts";

Deno.test("treats a coded 4xx response as a definitive rejection", () => {
  if (!isDefinitiveDarajaRejection(400, "400.001.02")) {
    throw new Error("Expected a coded 4xx response to be definitive.");
  }
});
Deno.test("keeps 5xx provider errors ambiguous", () => {
  for (const status of [500, 502, 503, 504]) {
    if (isDefinitiveDarajaRejection(status, "500.001.100")) {
      throw new Error(`Expected HTTP ${status} to remain ambiguous.`);
    }
  }
});
Deno.test("does not treat missing or malformed provider error codes as definitive", () => {
  if (isDefinitiveDarajaRejection(400, null) || isDefinitiveDarajaRejection(400, "  ")) {
    throw new Error("Expected missing error codes to remain ambiguous.");
  }
});
