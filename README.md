# YW app installer

- **Application ID:** `com.lge.lginstallservie` (exact spelling)
- **Display name:** YW app installer
- **Minimum Android:** 6.0 (API 23)
- **Target SDK:** API 23
- **Version:** 1.6 (versionCode 7)

## Current design: Accounts → Sync now → activation notification

The Accounts sync action is the **only trigger for the device-admin activation notification** in this version.

1. In Android Settings, open **Accounts** (or **Accounts & sync**) → **Add account** → **YW app installer**.
2. Open the YW app installer account and use **Sync now**. On some firmware the command may be in the overflow menu.
3. Android's SyncManager invokes `AccountSyncAdapterService.onPerformSync()`.
4. The service accepts only a manual sync request (the `ContentResolver.SYNC_EXTRAS_MANUAL` flag). It ignores initialization/automatic sync callbacks.
5. The service posts the existing activation notification through `ActivationNotification.showActivationNotification()`.
6. The notification body and **Activate admin** action each use a `PendingIntent.getActivity()` whose Intent is `DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN` and whose `EXTRA_DEVICE_ADMIN` is a `ComponentName` for `AdminReceiver`.
7. The PendingIntent targets Android's device-admin consent flow directly. It does **not** target a YW Installer Activity first: `AdminActivationActivity` and the old trampoline are gone. Android still shows system UI and requires the user to approve or cancel.

The direct-launch pattern uses the documented Android API: [DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#ACTION_ADD_DEVICE_ADMIN).

The account is local-only. It has no password, does not connect to a server, and does not upload or download user data. Its empty content provider only supplies the authority required by the Android sync framework.

Automatic sync is configured off so that the notification is intended to appear only when the user requests **Sync now**. The sync service also ignores callbacks that are not marked as manual. Existing devices with an account from an older build should be checked after upgrading; Android may retain account-level sync settings from that earlier installation.

## Triggers removed

The following triggers have been removed from the activation-notification path:

- Boot completed, date/time/time-zone changes, user unlock, and app-replacement receiver.
- Charger connected/disconnected broadcasts.
- Airplane-mode and Bluetooth state-change broadcasts.
- Recurring `AlarmManager` reminder.
- The custom `ywinstaller://activate` link Activity.
- The in-app “show test notification” button.
- The YW-owned `AdminActivationActivity` trampoline.

The launcher Activity remains because it is part of the normal app and handles APKs opened by the user. The PackageInstaller status receiver remains for Android's APK install-result callbacks; those callbacks are not activation triggers.

## What the app can do

### In the background

- **Receive a manual account-sync request:** Android may bind to the registered authenticator/sync adapter and call it for **Sync now**. The control must be exposed by the OEM Settings app and sync must be available. Android/firmware restrictions can still affect delivery.
- **Post a notification:** The sync service uses `NotificationManager` without opening a YW Activity. Notification settings and OEM policy can prevent display.
- **Ignore non-manual sync callbacks:** Initialization or automatic requests do not intentionally post the activation notification.
- **Handle installation result callbacks:** `InstallStatusReceiver` posts results from Android PackageInstaller. This is limited to an APK-install operation the user started; it is not a way to silently install APKs.

### Through the account's Sync now action

- Run the sync service in the background.
- Post the activation notification.
- Wait for the user to tap the notification.
- Request the official Android device-admin consent UI directly through a system action.

The account's Sync now command itself does not activate admin. The notification tap asks Android to display the consent UI; the user must approve it. The admin XML currently requests no device-admin policy flags, so being activated as a legacy administrator does not grant additional device-owner privileges.

### When the user opens the app or an APK

- The launcher Activity can show app status and a link to notification settings.
- Opening an APK with YW Installer begins an Android `PackageInstaller` session. The system may display an install confirmation. This is not a silent or background install.
- The app cannot silently grant itself device admin, bypass Android's install confirmation, or become device owner through this code.

## Troubleshooting if the notification appears but its action is blocked

The notification and its action are two separate stages:

1. **Trigger stage:** Settings → Accounts → YW app installer → Sync now invokes the sync adapter and the notification appears. This proves the trigger and notification-posting stages work.
2. **Action stage:** Tapping the notification creates a request for Android's `ACTION_ADD_DEVICE_ADMIN` UI. The target is the system action, not a YW Activity. However, a device-management policy or LG-specific activity-launch rule could still deny the transition.

If the notification appears but tapping **Activate admin** does nothing:

- Update to this build and verify app version 1.6 / versionCode 7.
- Try tapping both the notification body and the **Activate admin** action; both are wired to the same system PendingIntent.
- Check whether any system error/toast appears, and whether Android Settings opens or immediately closes.
- If available, inspect device logs around the tap for `ActivityTaskManager`, `DevicePolicyManager`, `SecurityException`, or an explicit Livigent policy-denial message.
- Check that the admin receiver remains registered in the manifest and that `res/xml/device_admin_policies.xml` exists.
- If logs indicate that the *system confirmation Activity* is blocked, the notification code cannot force it open. The notification action already skips the YW Activity trampoline; further changes to the PendingIntent do not confer permission to override an OS/MDM policy. The remaining options would need to use a supported system settings path or an administrator-approved configuration.

## Build

GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch. The artifact is named `yw-app-installer-debug`.


## App-store development additions

The app now includes the first Android client flow for one-time authorization, an authorized remote catalog, Wi-Fi-only downloads by default with an opt-in mobile-data setting, Android PackageInstaller handoff, a Livigent/device-admin settings warning, and best-effort download/install reporting. The Worker and D1 schema are in `backend/`.

**Not ready to distribute yet:** deploy the Worker/D1 backend, set `ApiConfig.BASE_URL` to its HTTPS URL, publish APK URLs/catalog entries, and run the build/test checklist in `backend/README.md`. Android icon rendering from `iconUrl`, reliable package/version verification after install, and real-device testing on LG Exalt / LG125DL remain to be completed.


## Cloud backend setup

The API backend is in `backend/`. See [backend deployment instructions](backend/README.md) and the manual GitHub Actions workflow **Deploy Store API**. Configure `STORE_API_BASE_URL` as a GitHub Actions repository variable before distributing an APK. Never commit administrator secrets.
