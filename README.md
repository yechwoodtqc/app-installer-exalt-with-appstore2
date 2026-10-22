# YW app installer

- **Application ID:** `com.lge.lginstallservie` (exact spelling)
- **Display name:** YW app installer
- **Minimum Android:** 6.0 (API 23)
- **Target SDK:** API 23

## Existing behavior

- Opening an APK with this app creates and commits an Android `PackageInstaller` session. It cannot silently bypass Android's install confirmation.
- The reminder notification contains an **Activate admin** action. When the user taps it, it launches Android's official `DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN` confirmation. Posting the notification does not launch an Activity.
- System-state receivers attempt to notify when airplane-mode or Bluetooth adapter broadcasts are received.
- The reminder alarm is scheduled for approximately once every 24 hours when an eligible component runs. Android may delay alarms under power management.

## Accounts / Sync trigger

This version adds a local account authenticator and sync adapter to test a Settings-based background entry point on Android 6, without launching YW Installer's Activity to initiate the sync.

1. Install the updated APK.
2. Open **Settings → Accounts** (or **Accounts & sync**) → **Add account**.
3. Select **YW app installer**.
4. Open the account and look for **Sync now** (sometimes under an overflow menu).
5. When Android invokes the sync adapter, YW Installer posts its **existing device-admin activation notification**. Tapping that notification launches Android's standard admin-consent screen; it is never opened automatically by the sync service.

The account is local-only and uses no credentials or network service. The empty content provider is registered only as the authority required by Android's sync framework.

**The Accounts/Sync path has not been tested on the LG Exalt hardware.** Settings screens vary by OEM; the account type may be hidden or Sync now may not be exposed. Android may also delay or decline a sync request depending on account-sync settings and system policy.

## Limitations

- Device-admin activation still requires the user's confirmation in Android system UI. The notification does not silently enable device admin.
- The sync adapter does not bypass Android force-stop behavior or override device-management policy. It tests whether Android invokes the registered service and then posts the notification.
- Android may prevent package components from running when the app is force-stopped or disabled by device policy.
- The device-admin XML requests no admin policies. It is enough to show the legacy admin-consent screen, but it does not grant device-admin policy powers.

## Build

GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch. The APK is uploaded as the `yw-app-installer-debug` workflow artifact.
