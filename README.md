# YW app installer

- **Application ID:** `com.lge.lginstallservie` (exact spelling)
- **Display name:** YW app installer
- **Minimum Android:** 6.0 (API 23)
- **Target SDK:** API 23

## Implemented behavior

- Opening an APK with this app creates and commits an Android `PackageInstaller` session. Android's PackageInstaller sees this app as the session creator/installer package; the app cannot silently bypass Android's user confirmation.
- If Android requires user action to complete an install, the app posts a notification that opens Android's install confirmation.
- A separate reminder notification has an **Activate device admin** action that opens Android's official `DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN` confirmation directly. It does not target the app's main activity.
- The reminder is scheduled to repeat every 60 seconds after the app has run or the boot receiver has run. Android may delay alarms during Doze or under device-specific battery restrictions.
- A system-state receiver posts separate notifications when it receives airplane-mode ON/OFF broadcasts or Bluetooth adapter ON/OFF broadcasts. It only monitors these settings; it does not change them. Intermediate Bluetooth states are ignored.

## Android limitations

- Device-admin activation always requires the user to confirm in system UI. Device admin alone does not grant silent APK installation privileges.
- Android does not guarantee a newly installed, never-launched app will execute code to schedule its first alarm. The reminder can be scheduled when the boot receiver runs or when the app is opened; a restricted device may require an allowed initial trigger. Therefore an immediate first notification after install is not guaranteed by this APK alone.
- Android versions and manufacturers may restrict background delivery of system broadcasts, and force-stopping the app prevents its components from running until the app is launched again. Therefore state-change notifications are best-effort and should be tested on the target phone.
- The device-admin XML requests no admin policies. This is sufficient to display the legacy admin-consent screen, but it does not grant useful device-admin policy powers.

## Build

GitHub Actions builds a debug APK on pushes to `main` and on manual workflow dispatch. The APK is uploaded as the `yw-app-installer-debug` workflow artifact.
