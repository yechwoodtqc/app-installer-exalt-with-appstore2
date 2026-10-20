package com.lge.lginstallservie;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    private static final int NOTIFICATION_ID = 2301;
    private static final int ALARM_REQUEST = 2302;
    private static final String CHANNEL_ID = "admin_activation_reminder";
    private static final long REMINDER_INTERVAL_MS = 24L * 60L * 60L * 1000L;

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

    public static void showActivationNotification(Context context) {
        createNotificationChannel(context);
        postNotification(context, false);
    }

    public static void scheduleNext(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(
                context, ALARM_REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        long nextAlarm = System.currentTimeMillis() + REMINDER_INTERVAL_MS;
        if (Build.VERSION.SDK_INT >= 19) {
            alarms.setExact(AlarmManager.RTC_WAKEUP, nextAlarm, pending);
        } else {
            alarms.set(AlarmManager.RTC_WAKEUP, nextAlarm, pending);
        }
    }

    private static void postNotification(Context context, boolean test) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 24 && !manager.areNotificationsEnabled()) return;

        createNotificationChannel(context);

        Intent activationFlow = new Intent(context, AdminActivationActivity.class);
        activationFlow.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent activatePending = PendingIntent.getActivity(
                context, 2303, activationFlow,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        String title = test ? "Test notification - YW app installer" : "YW app installer";
        String body = test
                ? "Tap to open Android's device-admin activation confirmation."
                : "Tap to activate YW app installer as a device administrator.";

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(
                        test
                                ? "This notification should open Android's official device-admin activation confirmation directly, without opening the main app screen."
                                : "Tap Activate to open Android's official confirmation screen for YW app installer. Activation remains subject to device policy and user approval."))
                .setAutoCancel(true)
                .setContentIntent(activatePending)
                .addAction(android.R.drawable.ic_lock_lock, "Activate admin", activatePending);

        if (Build.VERSION.SDK_INT >= 26) builder.setChannelId(CHANNEL_ID);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
