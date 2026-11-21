/** Normalize an E.164 Kenyan mobile number for Safaricom Daraja PartyB. */
export function normalizeDarajaPartyB(phoneE164: string): string {
  const normalized = phoneE164.trim().replace(/^\+/, "");
  if (!/^254[17]\d{8}$/.test(normalized)) {
    throw new Error("Recipient must be a Kenyan mobile number in E.164 format (+2547XXXXXXXX or +2541XXXXXXXX).");
  }
  return normalized;
}
