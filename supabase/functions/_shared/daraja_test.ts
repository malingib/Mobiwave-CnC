import { assertEquals, assertThrows } from "https://deno.land/std@0.224.0/assert/mod.ts";
import { normalizeDarajaPartyB } from "./daraja.ts";

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
  assertThrows(() => normalizeDarajaPartyB("0712345678"), Error, "E.164 format");
});
Deno.test("rejects malformed and non-Kenyan numbers", () => {
  assertThrows(() => normalizeDarajaPartyB("+15551234567"), Error, "E.164 format");
  assertThrows(() => normalizeDarajaPartyB("+25471234567"), Error, "E.164 format");
  assertThrows(() => normalizeDarajaPartyB("+254912345678"), Error, "E.164 format");
});
