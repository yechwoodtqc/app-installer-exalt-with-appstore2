# YW App Store API (Cloudflare Workers + D1)

This backend is designed for free-tier deployment. The Android app must be built with the deployed HTTPS Worker URL in `ApiConfig.java`. The Worker enforces authorization for catalog and report endpoints; APK files should be hosted at HTTPS URLs (for example, GitHub Releases or Cloudflare R2).

## Deploy

1. Create a Cloudflare account and install Wrangler: `npm install -g wrangler`.
2. From this directory, run `wrangler login`.
3. Create the database: `wrangler d1 create yw-app-store`. Copy its database ID into `wrangler.toml`.
4. Initialize the schema: `wrangler d1 execute yw-app-store --remote --file=schema.sql`.
5. Set an admin secret: `wrangler secret put ADMIN_SECRET`. Use a long random secret and do not put it in the Android app or commit it.
6. Deploy: `wrangler deploy`. Copy the Worker HTTPS URL into `app/src/main/java/com/lge/lginstallservie/ApiConfig.java`, then push so GitHub Actions builds the APK.

## Create authorization codes

Codes are single-use and expire (default 7 days). Keep the admin secret private.

```sh
curl -X POST "https://YOUR-WORKER.workers.dev/admin/codes" \
  -H "Authorization: Bearer YOUR_ADMIN_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"count":5,"ttlHours":168}'
```

The response contains the plaintext codes. They are stored as SHA-256 hashes and cannot be retrieved again.

## Publish the catalog

Host each APK at an HTTPS URL and send the catalog as JSON. `apkUrl` is the downloadable APK URL; `sizeBytes` is the exact size when known. `iconUrl` is optional metadata for future icon rendering.

```sh
curl -X PUT "https://YOUR-WORKER.workers.dev/admin/apps" \
  -H "Authorization: Bearer YOUR_ADMIN_SECRET" \
  -H "Content-Type: application/json" \
  --data-binary @apps.json
``

Example `apps.json`:

```json
{
  "apps": [
    {
      "id": "example-app",
      "name": "Example App",
      "description": "Short description for the catalog",
      "version": "1.0",
      "sizeBytes": 1234567,
      "packageName": "com.example.app",
      "apkUrl": "https://example.com/releases/example.apk",
      "iconUrl": "",
      "enabled": true
    }
  ]
}
```

## Monitoring and revocation

- `GET /admin/stats` returns active authorized-device count, unused code count, and aggregated report counts.
- `POST /admin/revoke/DEVICE_ID` revokes a device token.
- Android devices must send authenticated `POST /api/report` requests for download/install reports. The endpoint is implemented; wiring install-result callbacks into reporting is still pending.
- Authorization is enforced by the API, not just hidden in the UI. Revocation takes effect on the next API request. Existing direct APK URLs are not protected by the Worker, so use private storage or short-lived signed download URLs if the APK files themselves must be access-controlled.

This is a starter backend, not a deployed service. Do not distribute an APK until the Worker URL is configured, deployed, and tested.

 
## GitHub Actions deployment

A manual workflow at `.github/workflows/deploy-store-api.yml` can deploy the API. First create the D1 database once in Cloudflare (`wrangler d1 create yw-app-store`), then add these repository settings under **Settings → Secrets and variables → Actions**.

**Secrets:** `CLOUDFLARE_API_TOKEN` (scoped to Workers and D1) and `STORE_ADMIN_SECRET` (a long random secret).

**Variables:** `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_D1_DATABASE_ID`, and after the first deploy, `STORE_API_BASE_URL` (the actual Worker HTTPS URL, e.g. `https://yw-app-store-api.YOUR-SUBDOMAIN.workers.dev`).

Then run **Actions → Deploy Store API → Run workflow**. It applies the database schema, configures the admin secret, and deploys the Worker. Set `STORE_API_BASE_URL` to the real URL and rerun the Android APK workflow. Never commit credentials or admin secrets.
