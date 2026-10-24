# RoomReserved PayMongo hosted-checkout setup

## Current integration status

This repository now contains a **backend integration scaffold** for PayMongo Hosted Checkout v2 in the Java Spring Boot app. It creates a hosted checkout session from the server, redirects the signed-in guest to PayMongo, validates webhook signatures, checks the paid PHP amount against the server-calculated booking total, and confirms a reservation only after a matching verified webhook. It never stores raw card data.

It is **not live or fully verified**. No PayMongo account, API key, webhook secret, merchant approval, deployed HTTPS endpoint, or end-to-end test has been configured in this repository. Maven was not available in the working environment, so a Java compile/test run could not be completed.

## Payment methods and Philippine merchant eligibility

PayMongo's current public docs list online payment methods including QR Ph, GCash, Maya, GrabPay, ShopeePay, Visa/Mastercard, and direct online banking for supported banks. Method access depends on account capabilities being active in the PayMongo Dashboard.

- The safe default in this project is `PAYMONGO_PAYMENT_METHOD_TYPES=qrph`, because PayMongo says QR Ph is enabled by default after account activation.
- Add only payment method identifiers that are active on the merchant account. Example after approval: `qrph,gcash,paymaya,grab_pay,shopeepay,card,dob`.
- PayMongo's public pricing page currently describes MariBank under ShopeePay (including SPayLater and MariBank); the integration should not invent a separate `maribank` payment-method type. Confirm the exact customer option visible in the merchant's live dashboard.
- PayMongo's capability documentation says GCash and Visa/Mastercard require a registered business type. Other methods have different account eligibility requirements and provider approval. An individual account may not qualify for every method.
- Do not claim any payment method is enabled until the merchant dashboard shows it as Active and a test transaction succeeds.

References:
- Hosted Checkout v2: https://docs.paymongo.com/docs/payment-channels-hosted-checkout
- Hosted Checkout quick start: https://docs.paymongo.com/docs/payment-channels-hosted-checkout-quick-start
- Account capabilities and method eligibility: https://docs.paymongo.com/docs/account-settings-account-capabilities
- Account verification: https://docs.paymongo.com/docs/account-settings-account-setup
- Website requirements: https://docs.paymongo.com/docs/account-settings-website-guidelines
- Go-live checklist: https://docs.paymongo.com/docs/payment-channels-go-live-checklist

## 1. Set server environment variables

Copy `.env.example` to a private local `.env` for local development. Do not commit `.env` or real keys.

```dotenv
PAYMONGO_SECRET_KEY=sk_test_REPLACE_WITH_YOUR_TEST_SECRET_KEY
PAYMONGO_WEBHOOK_SECRET=REPLACE_WITH_TEST_WEBHOOK_SECRET
PAYMONGO_LIVE_MODE=false
PAYMONGO_PAYMENT_METHOD_TYPES=qrph
APP_BASE_URL=http://localhost:8080
```

Get keys only from your own PayMongo Dashboard under **Settings/Developers**. The secret key belongs only on the Java server. Do not put it in HTML, JavaScript, Cloudflare Pages environment that is exposed to client code, GitHub, screenshots, or chat.

For production, use the deployed HTTPS base URL, a live secret key (`sk_live_...`), the live webhook secret, `PAYMONGO_LIVE_MODE=true`, and only approved payment methods. Keep test and live keys/secrets separate.

## 2. Run the Java app and database

Deploy the Spring Boot app and PostgreSQL database to a server that runs Java. Cloudflare Pages/Workers is not a Java Spring Boot host. Configure the `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` variables separately. Set `APP_BASE_URL` to the exact public HTTPS origin of the Java app.

## 3. Register the webhook

In PayMongo Dashboard, create a webhook endpoint:

`https://YOUR_PUBLIC_BACKEND_HOST/webhooks/paymongo`

Subscribe to `checkout_session.payment.paid` and, if available for your account/channel, `checkout_session.payment.failed`. Copy the webhook secret into `PAYMONGO_WEBHOOK_SECRET` on the backend. Redeploy/restart the app after changing environment variables.

The webhook endpoint is public by necessity but validates the `Paymongo-Signature` HMAC using the raw request body and the test/live signature field. Only the webhook can confirm a paid booking; the success redirect only displays a pending-verification message.

## 4. Test before live mode

1. Use test API keys and test webhook secret.
2. Create a booking as a signed-in user.
3. Click **Pay securely online**. The browser should redirect to the PayMongo-hosted checkout page.
4. Complete a test checkout using the current PayMongo test instructions for an enabled method.
5. Confirm the webhook reaches the backend, signature verification passes, amount/currency/reference/session match, and the booking becomes `CONFIRMED` / `PAID` exactly once.
6. Test failed, abandoned, expired, replayed, mismatched-amount, wrong-signature, and repeated-webhook cases.
7. Verify abandoned sessions are expired before a hold is released. If PayMongo says a session is paid or still processing, the sweep retries and does not silently mark the reservation paid; reconcile any `PAID_REVIEW_REQUIRED` case manually.
8. Confirm the public website is complete for merchant review: real business/property details, prices in PHP, Philippines address, contact information, Terms and Conditions, Privacy Policy, and Refund/Cancellation Policy. PayMongo's published website rules reject placeholder or work-in-progress sites.
9. Only switch to live after merchant activation, method approval, HTTPS webhook verification, successful end-to-end tests, and review of current fees and policies.

## Implemented here vs not yet done

Implemented in code: server-side v2 checkout-session creation, server-calculated total in PHP centavos, signed-in ownership checks, checkout redirect, signature validation, matching booking reference/session/amount/currency, paid webhook confirmation, failed-payment status update, and an expiry sweep that attempts to close abandoned PayMongo sessions before marking holds expired.

Still required before production: compile and run automated tests; test real PayMongo test-mode payloads against the current account; verify webhook signature behavior in test and live modes; add durable webhook-event/idempotency audit storage and operational alerts; finish refund/dispute handling and admin reconciliation; configure an approved merchant account, active methods, production secrets, HTTPS domain, database backups, and privacy/security review. The UI, account workflows, notifications, and wider reservation lifecycle are not made production-ready merely by adding this payment integration.
