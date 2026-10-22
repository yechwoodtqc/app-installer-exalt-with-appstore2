# YW app installer

- **Application ID:** `com.lge.lginstallservie` (exact spelling)
- **Display name:** YW app installer
- **Minimum Android:** 6.0 (API 23)
- **Target SDK:** API 23

## Existing behavior

- Opening an APK with this app creates and commits an Android `PackageInstaller` session. It cannot silently bypass Android's install confirmation.
- The existing reminder notification's activation action opens Android's official `DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN` confirmation when tapped.
- System-state receivers attempt to notify when airplane-mode or Bluetooth adapter broadcasts are received.
- The reminder alarm is scheduled for approximately once every 24 hours when an eligible component runs. Android may delay alarms under power management.

## Experimental account/sync diagnostic

This version adds an Android account authenticator and sync adapter to test whether the LG Exalt firmware exposes a system Settings entry point to background code without launching YW Installer's Activity.

1. Install version 1.4.
2. Open **Settings → Accounts** (or **Accounts & sync**) → **Add account**.
3. Select **YW app installer**.
4. Open the account and look for **Sync now** (sometimes under an overflow menu).
5. A successful sync should post a plain notification titled **YW account sync ran**. It does not launch an Activity. Logcat tag: `YWAccountSync`.

The account is local-only and uses no credentials or network service. The content provider is an empty authority required by Android's sync framework.

**Not yet tested on the LG Exalt.** The exact menu and availability of Sync now depend on the OEM Settings implementation. If the account appears but the button is absent, the firmware may not expose a manual sync control. If Sync now is visible but no notification appears, check `YWAccountSync` logs and notification settings.

## Limitations

- The test does not prove that any notification or Activity will bypass a device-management policy. It only tests whether Android invokes the registered sync adapter.
- Device-admin activation still requires the user's confirmation in Android system UI.
- Android may prevent package components from running when the app is force-stopped or otherwise disabled by device policy.
- The account sync adapter does not store or transmit user data.

## Build

GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch. The APK is uploaded as the `yw-app-installer-debug` workflow artifact.
