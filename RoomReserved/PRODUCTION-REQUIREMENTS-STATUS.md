# RoomReserved production requirements and status

This document deliberately distinguishes a front-end demonstration from production behavior. The interactive static frontend is deployable as a preview, but **it is not a production booking engine**.

## Implemented in the static frontend demo
- Responsive luxury design and mobile navigation.
- Sample room cards, detail dialogs, amenities, capacity, illustrative rates, sample ratings/reviews, and policy copy.
- Destination search, category filters, sorting, check-in/check-out validation, nights calculation, guest and room quantity selection.
- Illustrative price breakdown: room subtotal, sample 12% tax, sample 5% service fee, demo promo discount, total.
- Demo inventory statuses for Available and Limited availability, and a sample remaining-room message.
- Multi-step guest details and review, online-payment choice placeholder (PayMongo not connected to static frontend), browser-local demo reference.
- Saved stays, notification bell/history/unread count/mark-read, review preview, Maps search/directions links, admin operations preview.
- Basic local storage for demo favorites, demo bookings, and demo notifications.

## Not implemented / not safe for real customers
- No verified property data, real address, coordinates, actual contact details, confirmed facilities/policies, or licensed photo catalogue.
- No API connection from the static frontend to the Spring Boot backend.
- No production PostgreSQL deployment, versioned migration validation, or verified database backup/restore.
- A basic PostgreSQL room-row lock, overlapping quantity check, pending hold, and scheduled hold expiry are scaffolded in the Java backend, but they are not yet integrated with the frontend or verified under concurrency. Maintenance-period conflicts, robust inventory-ledger constraints, and production load/concurrency tests remain unimplemented.
- No complete reservation status lifecycle (Pending, Confirmed, Checked In, Completed, Cancelled, Expired, Refunded) enforced by backend state transitions.
- PayMongo Hosted Checkout v2 session creation and signed webhook-processing code have been added to the Java backend. It remains unconfigured and untested; no merchant account or active payment methods are verified, no refunds/dispute workflow is implemented, and no durable webhook-event ledger exists.
- No email delivery, push notifications, WebSockets/SSE, durable notification records, or duplicate notification protection.
- No real promo administration or backend eligibility/redemption-limit enforcement.
- No verified-completed-stay review eligibility, moderation workflow, category ratings, administrator response, or aggregate rating calculation.
- No full customer account/profile/settings, receipts, payment records, permissions, property/room/maintenance/housekeeping CRUD, analytics, or audit logs.
- No production hardening review, integration tests, concurrency tests, rate-limit tests, security penetration testing, or privacy/legal review.

## Backend and database direction
The backend configuration now uses PostgreSQL and includes an `@Transactional` hold service that pessimistically locks the room row, checks overlapping inventory quantities, stores a pending hold with an expiry, and calculates configurable tax/service-fee/discount amounts. A scheduled task expires abandoned holds. However, the static frontend is not connected to this service; there are no versioned migration scripts, verified production database, maintenance-period inventory checks, payment webhook verification, full status lifecycle, or concurrency tests. Do not treat this as a production booking engine yet.

## Suggested delivery sequence
1. Verify the actual property owner, legal property details, address/coordinates, room inventory, rates, taxes/fees, policies, cancellation terms, and online-only payment terms.
2. Finalize the PostgreSQL schema and migration path; migrate existing data if any; implement property/room/photo/amenity/maintenance and pricing APIs.
3. Implement transactional availability + temporary holds + hold expiration + booking state transitions; write concurrent booking tests.
4. Configure PayMongo Hosted Checkout with test credentials; verify webhook signatures, amount/currency/session/reference matching, duplicate deliveries, expiration, and payment idempotency; never store raw card data. See `PAYMONGO-SETUP.md`.
5. Implement secure account flows, role permissions, admin operations, notification persistence/deduplication, email/push/WebSocket delivery, promo validation, and verified-stay reviews.
6. Connect the frontend to the APIs, remove all sample/demo wording only after real behavior is verified, and run end-to-end tests.
7. Deploy frontend and backend separately; configure secrets using hosting environment variables/secret manager; add TLS, monitoring, backups, restore tests, privacy notices, and incident procedures.

## Truthful deployment status
- Static frontend: package prepared; can be uploaded to Cloudflare Pages.
- Public deployment: not performed or verified in this workspace.
- Production backend and PostgreSQL: not provisioned or verified.
- Real reservations and payments: not available. PayMongo code is present but no keys, merchant approval, webhook URL, or end-to-end test has been configured.


## Added in the ISATU / Scan-to-Pay UI update
- [x] Added an amenities overview to the static Cloudflare website, marked as sample information.
- [x] Added an embedded Google Maps search and external Google Maps/directions links for ISATU main campus, Burgos Street, La Paz, Iloilo City. This is a requested landmark pin, not verified lodging coordinates.
- [x] Added a QR Ph Scan-to-Pay explanation to the booking review UI.
- [x] Updated the server-rendered Java templates with the ISATU map reference and QR Ph checkout guidance.
- [ ] Still required: deploy the backend, configure a real PayMongo account and verified webhooks, and test payment success/failure/expiry end to end. The static frontend does not create a real payable QR.
- [ ] Still required: verify actual property address and amenities.
