package com.lge.lginstallservie;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        boolean shouldNotify =
                Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_USER_PRESENT.equals(action)
                || Intent.ACTION_POWER_CONNECTED.equals(action)
                || Intent.ACTION_POWER_DISCONNECTED.equals(action);

        if (!shouldNotify) return;

        // Diagnostic Toast: proves this receiver was invoked even if Android/LG
        // suppresses the notification. Do not launch MainActivity here.
        ReminderReceiver.createNotificationChannel(context);
        ReminderReceiver.showActivationNotification(context);

        String event = eventLabel(action);
        String notificationState = getNotificationState(context);
        Toast.makeText(
                context,
                "YW DEBUG: " + event + " detected. " + notificationState,
                Toast.LENGTH_LONG
        ).show();

        // Only lifecycle/time events should reset the daily reminder timer.
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            ReminderReceiver.scheduleNext(context);
        }
    }

    private static String eventLabel(String action) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return "APP UPDATE";
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) return "BOOT COMPLETED";
        if (Intent.ACTION_USER_PRESENT.equals(action)) return "USER UNLOCKED";
        if (Intent.ACTION_POWER_CONNECTED.equals(action)) return "CHARGER CONNECTED";
        if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) return "CHARGER DISCONNECTED";
        if (Intent.ACTION_DATE_CHANGED.equals(action)) return "DATE CHANGED";
        if (Intent.ACTION_TIME_CHANGED.equals(action)) return "TIME CHANGED";
        if (Intent.ACTION_TIMEZONE_CHANGED.equals(action)) return "TIME ZONE CHANGED";
        return action;
    }

    private static String getNotificationState(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return "ERROR: notification service unavailable.";
        if (Build.VERSION.SDK_INT >= 24 && !manager.areNotificationsEnabled()) {
            return "BLOCKED: app notifications are disabled.";
        }
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel =
                    manager.getNotificationChannel("admin_activation_reminder");
            if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                return "BLOCKED: reminder notification channel is disabled.";
            }
        }
        return "Notification request sent; check the notification shade.";
    }
}
