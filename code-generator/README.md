# YW Code Generator

A small Android 6+ companion app for the YW App Store administrator. It requests one-time activation codes from the existing Worker endpoint.

## Security
- The admin secret is typed by the operator and is not stored in preferences or embedded in the APK.
- Requests use HTTPS and the secret is sent only as a Bearer token to the configured Worker origin.
- Generated one-time activation codes are displayed on screen and are not persisted by the app.
- Install this tool only on an administrator-controlled device.

## Build
Open this folder as an Android Gradle project, or use the repository's GitHub Actions workflow **Build Code Generator APK**. The debug APK is uploaded as the `yw-code-generator-debug` artifact.

Default API endpoint: `https://yw-app-store-api.yw-app-installer-store-2026.workers.dev/admin/codes`.
