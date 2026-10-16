# Integration contracts

## eTIMS verification adapter

Configure ETIMS_VERIFICATION_URL and ETIMS_VERIFICATION_API_KEY as server-side secrets only. The endpoint is expected to accept a JSON POST with supplier_pin, cu_invoice_number, gross_amount and expense_date, and return JSON with status, reference, checked_at and code.

Allowed statuses are verified, not_verified, and needs_review. The adapter must be an authorised and contractually approved source. A timeout, 5xx, malformed response or unknown status never marks an invoice verified. Confirm the real provider's API and adapt the contract before enabling this function.

## Safaricom Daraja B2C

Required secrets: DARAJA_CONSUMER_KEY, DARAJA_CONSUMER_SECRET, DARAJA_SHORTCODE, DARAJA_INITIATOR_NAME, DARAJA_SECURITY_CREDENTIAL, DARAJA_RESULT_URL, DARAJA_TIMEOUT_URL, DARAJA_CALLBACK_SECRET, and DARAJA_ENVIRONMENT.

The function submits each recipient as an individual B2C request with an idempotency-derived correlation identifier. A request accepted by the gateway is marked submitted, not successful. Provider callback results are used to reconcile the payment.

Before using this adapter, confirm the current Daraja API version, required credential encoding, callback URL format, initiator permissions, command ID, amount/phone rules, result payload, rate limits and timeout behavior with Safaricom. The adapter is intentionally conservative; no live credentials are present in this repository.

The callback handler expects a long random shared token in the callback URL query parameter named token. Protect this URL from logs and, where possible, validate the source through an IP-restricted gateway. Do not expose an unauthenticated callback handler.

## Offline synchronisation

Client submissions should use a stable client-generated UUID as client_submission_id. The unique constraint on organisation + employee + client ID makes retries idempotent. Upload evidence to the private bucket first, then submit metadata; clean up orphaned objects under a retention policy. Do not interpret local queue progress as a server commit.
