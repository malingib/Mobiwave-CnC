# Integration contracts

## eTIMS invoice verification — direct KRA first

### Current implementation boundary

MobiWave-CnC captures supplier KRA PIN, eTIMS/Control Unit invoice number, gross amount, invoice date and VAT amount. These fields are completeness inputs; they do not establish invoice validity.

KRA's published system-to-system integration documentation describes OSCU (online invoicing) and VSCU (bulk/offline-capable invoicing) and requires testing and certification for self-integrators and third-party vendors. That documentation does not, on its own, confirm a general-purpose lookup API for verifying any supplier's previously issued invoice.

Until KRA confirms the relevant lookup/validation operation and provides the current approved technical contract and access path:
- Do not call a guessed endpoint.
- Do not create or store fabricated credentials.
- Do not mark an invoice externally verified based on OCR, QR decoding, a PIN check alone, or a manual reviewer.
- The verify-invoice function fails closed and places the expense in the authorised manual-review queue.
- Manual review outcomes can be `not_verified` or `needs_review`; this workflow cannot set `externally_verified`.
- The expense remains `needs_review` pending the separate expense review/exception policy.

### KRA onboarding checklist

1. Contact KRA eTIMS support through the official eTIMS contact channels and request the current OSCU/VSCU technical specifications and sandbox access.
2. Ask specifically whether KRA offers an approved API operation for a buyer to validate a received supplier invoice by supplier PIN, invoice/Control Unit number, gross amount, invoice date and VAT amount. Ask for the exact endpoint, request/response schema, authentication, permitted use, rate limits and audit requirements.
3. Confirm whether the operation is available to an expense-management buyer or only to certified Trader Invoicing Systems / approved integrators.
4. Complete any required registration, sandbox testing, technical vetting and certification before enabling direct integration.
5. Add contract tests using KRA sandbox test fixtures. Treat timeouts, malformed responses, unknown statuses and partial matches as inconclusive and queue manual review.
6. Only map an authoritative positive response from the approved KRA operation to `externally_verified`. Store the provider reference, check time and minimal response evidence without logging secrets.

Official references:
- KRA system-to-system integration: https://www.kra.go.ke/business/etims-electronic-tax-invoice-management-system/learn-about-etims/etims-system-to-system-integration
- eTIMS onboarding and certification: https://etims.kra.go.ke/main/signup/indexLearnMore

### Manual review fallback

The queue is visible only to authenticated users with the `expenses.review` permission in the same organisation. Review actions require a reason, prevent a submitter from reviewing their own expense, write an audit event and cannot certify the invoice as externally verified. QR/OCR output may support evidence review but is not authoritative validation.

## Future certified provider adapter

A third-party route may be added only after a provider is verified against KRA's current approved-integrator information and confirms that its service validates received supplier invoices (not merely issuing invoices). Obtain the real API contract, test credentials and production credentials from that provider. Do not copy secrets into this repository. A provider's response may set `externally_verified` only after contract validation and security review.

## Safaricom Daraja B2C

Required secrets: DARAJA_CONSUMER_KEY, DARAJA_CONSUMER_SECRET, DARAJA_SHORTCODE, DARAJA_INITIATOR_NAME, DARAJA_SECURITY_CREDENTIAL, DARAJA_RESULT_URL, DARAJA_TIMEOUT_URL, DARAJA_CALLBACK_SECRET, and DARAJA_ENVIRONMENT.

The function submits each recipient as an individual B2C request with an idempotency-derived correlation identifier. A request accepted by the gateway is marked submitted, not successful. Provider callback results are used to reconcile the payment.

Before using this adapter, confirm the current Daraja API version, required credential encoding, callback URL format, initiator permissions, command ID, amount/phone rules, result payload, rate limits and timeout behavior with Safaricom. The adapter is intentionally conservative; no live credentials are present in this repository.

The callback handler expects a long random shared token in the callback URL query parameter named token. Protect this URL from logs and, where possible, validate the source through an IP-restricted gateway. Do not expose an unauthenticated callback handler.

## Offline synchronisation

Client submissions should use a stable client-generated UUID as client_submission_id. The unique constraint on organisation + employee + client ID makes retries idempotent. Upload evidence to the private bucket first, then submit metadata; clean up orphaned objects under a retention policy. Do not interpret local queue progress as a server commit.
