# RoomReserved deployment checklist (Railway + Cloudflare)

## Current status

No hosting provider, domain, Cloudflare zone, or Cloudflare Tunnel has been configured for this project. This guide does not imply a live deployment.

## 1. Create a Git repository

1. Create/sign in to GitHub.
2. Create a **private** repository named `roomreserved` (recommended while the project is still being hardened).
3. Extract this project and upload/commit the project files to the repository root. Do not commit `.env`, credentials, database dumps, or tunnel tokens.
4. Confirm `Dockerfile`, `pom.xml`, `src/`, and `README.md` are at the repository root.

## 2. Deploy the app on Railway

1. Create/sign in to a Railway account and connect GitHub.
2. Create a project and deploy from the `roomreserved` repository. Railway detects the included Dockerfile.
3. Add a PostgreSQL database service to the same Railway project. Keep it private; do not enable public database access.
4. In the Java app service's Variables settings, add these variables using Railway's variable-reference picker. Use Railway’s PostgreSQL service and its variable names as shown in the Railway dashboard; use the exact service name shown in your project.

   - `DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}`
   - `DB_USERNAME=${{Postgres.PGUSER}}`
   - `DB_PASSWORD=${{Postgres.PGPASSWORD}}`
   - `PORT=8080` (optional; Spring Boot defaults to 8080)

   Use Railway's UI to insert references rather than typing secret values into chat. Adjust `Postgres` if your database service has a different name. Do not set DB_URL to a local `localhost` address.

5. Deploy/redeploy the app. Check build and runtime logs for database connectivity errors. The app needs to start successfully before generating a public domain.
6. Under the Java service's Networking settings, generate a Railway public domain. Open it in a private/incognito browser and confirm the RoomReserved homepage loads. Test registration, login, room availability, and booking/cancellation with test accounts only.

## 3. Domain and HTTPS

A custom domain is optional for the first test deployment. Railway's generated public domain is sufficient to test the application. Railway's public endpoint uses platform-managed HTTPS.

When ready for a custom domain:

1. Choose and purchase a domain through a registrar. Cloudflare Registrar is an option if the domain/TLD is available in the dashboard.
2. Add the domain to Cloudflare and complete the registrar/nameserver instructions. Verify the domain appears as an active zone in the Cloudflare dashboard.
3. In Railway, add the chosen hostname (for example, `booking.example.com`) as a custom domain and note the exact DNS target Railway displays.
4. In Cloudflare DNS, create the record exactly as Railway instructs. Do not guess the CNAME target. Follow the current Railway instructions for proxy status and certificate validation.
5. Wait for domain and certificate status to become active, then test `https://booking.example.com` and verify the certificate in a browser.

## 4. Does this require Cloudflare Tunnel?

Not for the recommended Railway deployment. Railway already exposes the app through its public ingress, so a Tunnel is redundant for the basic setup. Use Cloudflare DNS/custom-domain integration and the platform's HTTPS first.

Cloudflare Tunnel is a better fit when `cloudflared` can run on a server/VM you control, or as an intentionally designed private service. It requires a domain in Cloudflare and a running `cloudflared` connector. Do not create a Tunnel until the domain and origin architecture are decided. Never expose the PostgreSQL port publicly.

## 5. Before real customers

This project is a starter and has not been production-audited. Before real reservations, implement database transaction/locking protections against concurrent double bookings, booking modification workflows, email confirmations, account verification and password reset, rate limiting, admin room editing/user management, verified-stay review checks, monitoring, backup/restore drills, privacy/legal notices, and comprehensive tests. Review the sample Cebu City map and sample room listings; replace them with accurate property data. Do not accept payments until a compliant payment provider is integrated and tested.
