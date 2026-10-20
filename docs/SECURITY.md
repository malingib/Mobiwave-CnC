# Security model and release gates

## Identity and tenant isolation

- Supabase Auth identifies the caller. An employee row must be linked to the authenticated user.
- Every business record carries organisation_id; policies use the caller's active organisation.
- Permissions are assigned to roles and active, effective-dated employee assignments. Creating a role grants no permission by default.
- The initial policies are a baseline and require independent RLS testing. Do not assume they satisfy every hierarchy-scope requirement yet.
- Never ship a service-role key, Daraja credential, eTIMS provider secret, or callback secret in the Android app or web bundle.

## Money movement

- B2C is restricted to expense advances and expense-allocation funding. Salary, payroll, commissions and general staff transfers are not supported.
- Maker, approver and submitter must be separate authenticated users.
- Each payment has a unique idempotency key and provider correlation reference.
- Submission is marked outcome_unconfirmed before network calls. A timeout must not trigger an automatic retry.
- Gateway acceptance means submitted, not successful. Only a trusted result callback/reconciliation may mark success.
- The callback endpoint requires a long random shared token in the callback URL query. Protect the URL from logs and add IP/source validation at a gateway where possible.
- Never update a displayed float from a local client action.

## Invoice verification

- Only the server-side configured provider adapter may set externally_verified.
- QR scanning and OCR produce extracted data, not proof of validity.
- Missing provider, timeout or malformed response remains awaiting verification or needs review.
- Manual fallback must be recorded as manual review, not external verification.

## Evidence and personal data

- Receipt files are private and limited to expected MIME types and 10 MB by default.
- Validate file signatures and malware-scan uploaded files in the production ingestion pipeline.
- Minimise employee and tax identifiers in logs; redact provider payloads and secrets.
- Configure retention, access review, export controls and breach response before production.
- Database backups do not necessarily include Storage objects; test both restore paths.

## Required before production

1. Independent RLS tests across two organisations, all roles and hierarchy scopes.
2. MFA for finance approvers and privileged administrators.
3. Rate limits, request IDs, audit event coverage and alerting.
4. Verify callback authenticity and provider retry semantics with Safaricom.
5. Reconcile all payment items against provider statements; handle partial-success batches.
6. Verify eTIMS provider authorisation, data protection, terms and response schema.
7. Confirm data residency, dedicated deployment requirements, backup retention and disaster recovery.
8. Threat model, dependency scanning, secret scanning and a penetration test.
9. Legal/finance approval of categories, limits, retention and exception handling.
10. Release signing, key custody, app identity and rollback procedures.
