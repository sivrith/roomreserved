# RoomReserved Luxury — Cloudflare Static Site

This is a standalone static frontend designed to render correctly on `workers.dev` or Cloudflare Pages. It does **not** try to run the Java application inside Cloudflare Workers, which do not run a normal Spring Boot JVM server.

## What's included
- Responsive luxury-hotel homepage and room collection
- Mobile navigation, category filters, sort by price, destination search
- Save/unsave stays (browser local storage)
- Room detail dialogs and booking estimate dialogs
- Date validation, night-count and estimated total calculation
- Account/newsletter preview forms with clear demo-only messages
- Accessible dialog controls, reduced-motion support, responsive layouts

## Fastest way to publish this frontend
1. Open the Cloudflare dashboard and select **Workers & Pages**.
2. Create a new Pages project using direct upload (if available in your dashboard).
3. Upload the contents of the `cloudflare-site` folder, keeping `index.html`, `styles.css`, `app.js`, and `_headers` at the site root.
4. Deploy and open the URL Cloudflare shows. Verify that images load and the search, filters, saved hearts, and booking estimate work.

Alternatively, with Node.js installed, install Wrangler and run `npx wrangler deploy` from the project root. Review the Cloudflare dashboard's deploy output and use the URL it actually returns; do not guess the URL.

## Important: what is and isn't connected
This frontend is intentionally self-contained and can be served as static assets. The visible interactions work in the browser, but this is a **front-end demo**, not a live booking service:
- Room names, rates, ratings, photos, and Cebu location are sample content and must be verified/replaced.
- Search filters sample cards; it does not query inventory.
- Booking dialog calculates an estimate only. It does not create or save reservations.
- Sign-in and newsletter forms explicitly say they are not connected.
- Saved stays are stored in the visitor's browser only.

For real accounts, room inventory, availability checks, bookings, reviews, and admin tools, deploy the Java 21/Spring Boot backend with PostgreSQL using the existing Docker setup (see `DEPLOY-RAILWAY.md`) and connect this frontend to authenticated backend APIs. Do not accept payment or real customer bookings until the backend, transaction safety, security, privacy, and backup procedures are tested.
