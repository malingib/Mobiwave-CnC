# MobiWave Expense & eTIMS Accountability

Native Android prototype and Supabase backend foundation for post-spend accountability: receipt evidence, invoice verification, expense allocation, expense advances, finance review, reconciliation and audit.

## Safety status

This repository is not yet production-ready. The Android UI retains synthetic sample records. Live B2C is blocked in the local prototype; the backend submission function requires secured server-side secrets and a configured Daraja environment. No eTIMS invoice is considered verified unless an authorised configured provider returns a recognised positive result.

The Supabase migration and Edge Functions are an implementation foundation, not evidence that a client project has been provisioned, a migration applied, credentials configured, or live integrations tested.

## Stack

- Android: Kotlin, Jetpack Compose, Gradle.
- Backend: Supabase Auth, PostgreSQL, private Storage, Edge Functions (Deno/TypeScript).
- Payments: server-side Daraja B2C adapter; never put provider secrets in the APK.
- Invoice verification: adapter contract documented in docs/INTEGRATIONS.md.
- CI: Android test/build workflow.

## Repository map

- app/ — Android prototype.
- supabase/migrations/ — versioned database schema and RLS.
- supabase/functions/verify-invoice/ — authenticated invoice-verification adapter.
- supabase/functions/submit-b2c-batch/ — maker/checker-gated B2C submission.
- supabase/functions/daraja-callback/ — callback reconciliation endpoint.
- docs/IMPLEMENTATION_PLAN.md — release checklist.
- docs/SECURITY.md — security assumptions and release gates.

## Apply database migrations

1. Create a dedicated staging Supabase project.
2. Install the Supabase CLI and authenticate locally.
3. Link the project with: supabase link --project-ref YOUR_STAGING_PROJECT_REF
4. Inspect the SQL migration and apply it to staging with: supabase db push
5. Create roles and permissions for the first organisation, then provision users/employee mappings through a controlled admin process.
6. Verify RLS using at least two users in separate organisations and users with different permissions.
7. Test backup/restore and Storage evidence recovery before any production data.

Do not use the live Sunking project until security review and explicit approval.

## Configure Edge Function secrets

Set secrets in Supabase project settings or with the Supabase CLI. Never commit the values. Required secret names are listed in supabase/.env.example. For example:

    supabase secrets set ETIMS_VERIFICATION_URL=https://provider.example/verify
    supabase secrets set ETIMS_VERIFICATION_API_KEY='REPLACE_ME'
    supabase secrets set DARAJA_ENVIRONMENT=sandbox
    supabase secrets set DARAJA_CONSUMER_KEY='REPLACE_ME'
    supabase secrets set DARAJA_CONSUMER_SECRET='REPLACE_ME'
    supabase secrets set DARAJA_SHORTCODE='REPLACE_ME'
    supabase secrets set DARAJA_INITIATOR_NAME='REPLACE_ME'
    supabase secrets set DARAJA_SECURITY_CREDENTIAL='REPLACE_ME'
    supabase secrets set DARAJA_RESULT_URL='https://PROJECT.functions.supabase.co/daraja-callback'
    supabase secrets set DARAJA_TIMEOUT_URL='https://PROJECT.functions.supabase.co/daraja-callback'
    supabase secrets set DARAJA_CALLBACK_SECRET='USE_A_LONG_RANDOM_SECRET'

The callback function expects a long random shared secret in the callback URL query parameter named token because Daraja does not support arbitrary callback headers. Protect this URL from logs and access; where possible, put an IP-restricted gateway in front of it. Confirm current provider callback behavior before production and never remove callback protection.

## Deploy functions

    supabase functions deploy verify-invoice
    supabase functions deploy submit-b2c-batch
    supabase functions deploy daraja-callback

Function deployment does not enable live payments by itself. Configure sandbox first, validate callback handling and reconcile outcomes. Switch to production only after credentials, business short code, initiator/security credential, callback reachability, approval policy and payment limits are confirmed.

## Financial rules

- An allocation is a limit, not a transfer.
- A disbursement is not evidence of spending.
- An API acceptance response is not payment success; only a trusted callback/reconciliation confirms the result.
- A timeout/outage is not a confirmed payment failure. Mark it outcome_unconfirmed and reconcile before retrying.
- Expense advances and expense-allocation funding only. Payroll, salaries, commissions and general staff payments are out of scope.
- QR scanning or OCR does not prove KRA/eTIMS validity.
- Audit events are append-only; corrections should be new events with a reason.

## Android build

Use JDK 17 and Gradle 9.3.1 (the current repository does not include the gradlew wrapper script):

    gradle testDebugUnitTest assembleDebug

CI results must be green before merge. This commit has not been built in the author's environment yet; use CI to establish the actual result.
