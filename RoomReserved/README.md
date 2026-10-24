# RoomReserved — Luxury booking website starter

This repository contains two pieces that must be deployed separately:

1. **`cloudflare-site/` — luxury static frontend.** It can be deployed to Cloudflare Pages or a static hosting service. Its interactions are a browser-side demo; they are not connected to live inventory, accounts, payments, or a database.
2. **Spring Boot 3 / Java 21 backend** at the project root. It uses Spring MVC, Thymeleaf, Spring Security, JPA, and PostgreSQL. A transactional booking-hold service now locks the room row, checks overlapping room quantity, calculates a configurable price breakdown, creates a pending hold, and expires abandoned pending holds. This is useful foundation work, not a production-certified booking engine.

## Important deployment distinction
A `workers.dev` URL runs a Cloudflare Worker. It does not automatically run a Java Spring Boot application or PostgreSQL. Deploy `cloudflare-site/` to Cloudflare Pages for the visual frontend, and deploy the backend to a Java-capable host with a private PostgreSQL database for persistent features. The frontend still needs API integration before its booking forms can call the backend.

## Deploy the frontend preview
Read [`CLOUDFLARE-STATIC-DEPLOY.md`](CLOUDFLARE-STATIC-DEPLOY.md). Upload the **contents** of `cloudflare-site/` so `index.html`, `styles.css`, `app.js`, and `_headers` are at the site root. The exact public URL is assigned by Cloudflare after deployment.

### Frontend interactions in this demo
- Responsive luxury design and mobile navigation
- Destination search, category filters, price/rating sorting
- Date validation and night calculation
- Sample room details, amenities, capacity, status labels, map search/directions
- Multi-step booking UI with adults, children, room quantity, arrival/departure time fields
- Illustrative tax, service fee, discount, and total breakdown
- Demo promo codes, guest details, and an online-payment placeholder (PayMongo is not connected to this static frontend)
- Browser-local demo booking reference, saved stays, notification list/unread count/mark-read
- Review preview, newsletter interaction, and sample operations dashboard

**All static frontend data and bookings are demo-only.** They are stored or calculated in the browser and do not reserve real inventory or reach the Java server.

## Run the backend locally
Requirements: Java 21, Maven, and Docker Compose.

1. Copy `.env.example` to `.env` and replace the sample database password.
2. Run `docker compose up --build`.
3. Open `http://localhost:8080`.
4. The app connects to PostgreSQL at `postgres:5432` within Compose. Do not expose the database port publicly.

Configure pricing with `TAX_PERCENT` and `SERVICE_FEE_PERCENT`. They default to `0` deliberately; do not assume the demo's sample 12% tax and 5% service fee are legally or commercially correct. Configure the hold timeout with `BOOKING_HOLD_MINUTES` (default 15) and the expiry sweep with `BOOKING_HOLD_SWEEP_MS` (default 60000).

## Booking hold foundation
The backend creates `PENDING` holds rather than immediately marking a reservation as paid/confirmed. It locks the room row during inventory checking, counts overlapping confirmed/check-in/pending non-expired quantities, validates capacity and dates, computes the current configured fee/tax/discount breakdown, and automatically marks expired pending holds as `EXPIRED`. This must still be tested under concurrent load. It currently models inventory by room record; real property inventory and maintenance periods need a complete data model and APIs.

## Admin access
No default admin account is created. Create a user through registration, then promote the intended administrator using PostgreSQL:

```sql
UPDATE app_users SET role='ADMIN' WHERE email='your-admin-email@example.com';
```

Sign out and back in so the updated role is loaded. Never publish a shared default admin password.

## PayMongo Hosted Checkout scaffold
Read [`PAYMONGO-SETUP.md`](PAYMONGO-SETUP.md) for the current integration notes and official merchant setup requirements. The Java backend now contains server-side PayMongo Hosted Checkout v2 session creation, a signed webhook endpoint, amount/currency/session/reference checks, payment state updates, and a scheduled attempt to expire abandoned checkout sessions. The safe default payment method is QR Ph only; enable more methods only after they show Active in the merchant dashboard. This is unconfigured and untested until you add your own PayMongo test credentials, public HTTPS webhook URL, and run end-to-end tests. Never commit keys or confirm bookings from the browser return URL.

## Not yet production-ready
- Static frontend is not connected to backend APIs.
- No verified property address, contact information, actual inventory, photos/licensing, real policies, or approved tax/fee settings.
- PayMongo merchant account, API keys, webhook secret, payment method approvals, live endpoint, test-mode end-to-end verification, refunds/disputes, and durable webhook event ledger are not configured.
- No email delivery, web push, WebSockets/SSE, durable notification API, or notification deduplication.
- No complete admin CRUD for properties, room photos, amenities, inventory, maintenance, staff, housekeeping, reports, or audit logs.
- No promo rules for eligibility, min spend, redemption limits, seasonal/long-stay offers, or per-property restrictions.
- Reviews are not yet restricted to eligible completed stays and lack moderation/category ratings/admin responses.
- No versioned database migrations, end-to-end tests, concurrency tests, penetration testing, privacy/legal review, backup restore drills, or monitoring verification.
- No public deployment has been performed or verified from this workspace.

See [`PRODUCTION-REQUIREMENTS-STATUS.md`](PRODUCTION-REQUIREMENTS-STATUS.md) for a feature-by-feature status and delivery plan. Do not accept real bookings or payments until the remaining requirements are implemented and tested.


## Latest UI additions: ISATU map, amenities, and Scan to Pay

- The Cloudflare static frontend includes a responsive amenities overview and an embedded Google Maps search pinned to the ISATU main-campus landmark (Burgos Street, La Paz, Iloilo City), with separate Google Maps and directions links. This is a map reference only; it does not claim the lodging property is inside the university. Verify the property's actual address before accepting guests.
- The booking review screen describes QR Ph Scan to Pay. The static Cloudflare site is only a UI demo and cannot create a payable QR or verify payment. For real payments, deploy the Spring Boot backend, configure PayMongo credentials and QR Ph eligibility, set the public HTTPS base URL, and register/verify the webhook. The backend should confirm a booking only after the verified payment webhook.
- Sample amenities, rooms, ratings, prices, and inventory must be replaced with accurate property-managed data before launch.
