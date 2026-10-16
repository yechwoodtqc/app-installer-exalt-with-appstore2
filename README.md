# YW app installer

Android application ID: `com.lge.lginstallservie`  
Display name: **YW app installer**  
Minimum Android version: 6.0 (API 23)  
Target SDK: API 23

## Behavior

- A boot receiver schedules a daily 9:00 AM local-time reminder.
- The reminder notification's **Activate device admin** action opens Android's official `ACTION_ADD_DEVICE_ADMIN` confirmation screen directly.
- The app's main activity is not used by the notification action.
- Device administrator activation always requires the user to confirm in Android's system UI.

## Important Android limitation

Android does not guarantee that a newly installed, never-launched app will receive boot broadcasts or execute code to schedule its first alarm. This implementation schedules from `BOOT_COMPLETED`, where the device permits that receiver to run. Some restricted devices require an installer/management agent or one allowed initial trigger to schedule the first alarm. The notification action itself does not launch the app's main activity.

The admin receiver requests the Android `force-lock` device-admin policy. Remove that policy if it is not needed.

## Build

GitHub Actions builds a debug APK on pushes and manual workflow dispatch. The APK is uploaded as a workflow artifact.
