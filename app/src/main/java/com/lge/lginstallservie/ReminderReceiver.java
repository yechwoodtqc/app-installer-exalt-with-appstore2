package com.lge.lginstallservie;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    private static final int NOTIFICATION_ID = 2301;
    private static final int ALARM_REQUEST = 2302;
    private static final String CHANNEL_ID = "admin_activation_reminder";

    @Override
    public void onReceive(Context context, Intent intent) {
        postNotification(context);
        scheduleNext(context);
    }

    public static void scheduleNext(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(
                context, ALARM_REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 9);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(Calendar.getInstance())) next.add(Calendar.DAY_OF_YEAR, 1);

        if (Build.VERSION.SDK_INT >= 23) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pending);
        } else {
            alarms.set(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pending);
        }
    }

    private static void postNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    CHANNEL_ID, "Device admin reminder", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminder to review device administrator activation.");
            manager.createNotificationChannel(channel);
        }

        Intent adminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        adminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                new android.content.ComponentName(context, AdminReceiver.class));
        adminIntent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Review and confirm whether to activate device administrator for YW app installer.");
        PendingIntent activatePending = PendingIntent.getActivity(
                context, 2303, adminIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle("YW app installer")
                .setContentText("Tap to review device administrator activation.")
                .setStyle(new Notification.BigTextStyle().bigText(
                        "Tap Activate device admin to open Android's official confirmation screen. The app's main screen will not be opened."))
                .setAutoCancel(true)
                .setContentIntent(activatePending)
                .addAction(android.R.drawable.ic_lock_lock, "Activate device admin", activatePending);

        if (Build.VERSION.SDK_INT >= 26) builder.setChannelId(CHANNEL_ID);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
