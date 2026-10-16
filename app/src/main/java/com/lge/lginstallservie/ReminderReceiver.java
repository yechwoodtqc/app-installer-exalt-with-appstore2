package com.lge.lginstallservie;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    private static final int NOTIFICATION_ID = 2301;
    private static final int ALARM_REQUEST = 2302;
    private static final String CHANNEL_ID = "admin_activation_reminder";

    @Override
    public void onReceive(Context context, Intent intent) {
        postNotification(context, false);
        scheduleNext(context);
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Device admin reminder", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Reminder to review device administrator activation.");
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showTestNotification(Context context) {
        createNotificationChannel(context);
        postNotification(context, true);
    }

    public static void scheduleNext(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(
                context, ALARM_REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        long nextAlarm = System.currentTimeMillis() + 60_000L;
        if (Build.VERSION.SDK_INT >= 19) {
            alarms.setExact(AlarmManager.RTC_WAKEUP, nextAlarm, pending);
        } else {
            alarms.set(AlarmManager.RTC_WAKEUP, nextAlarm, pending);
        }
    }

    private static void postNotification(Context context, boolean test) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        createNotificationChannel(context);

        Intent adminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        adminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                new android.content.ComponentName(context, AdminReceiver.class));
        adminIntent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Activate Yechwood installer as a device administrator.");
        PendingIntent activatePending = PendingIntent.getActivity(
                context, 2303, adminIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        String title = test ? "Test notification - YW app installer" : "YW app installer";
        String body = test
                ? "Test succeeded. Tap this notification to test the direct device-admin confirmation."
                : "Tap to activate Yechwood installer.";

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(
                        test
                                ? "This test notification was posted by the app. Tapping it should open Android's official device-admin confirmation without opening the app's main screen."
                                : "Tap Activate Yechwood installer to open Android's official confirmation screen. The app's main screen will not be opened."))
                .setAutoCancel(true)
                .setContentIntent(activatePending)
                .addAction(android.R.drawable.ic_lock_lock, "Activate Yechwood installer", activatePending);

        if (Build.VERSION.SDK_INT >= 26) builder.setChannelId(CHANNEL_ID);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
