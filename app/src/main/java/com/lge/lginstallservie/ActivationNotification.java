package com.lge.lginstallservie;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Builds the one activation notification used by the Accounts/Sync trigger.
 *
 * Both the notification body and its action point straight to Android's
 * system-owned ACTION_ADD_DEVICE_ADMIN screen. Neither PendingIntent targets
 * a YW Installer Activity, and no trampoline Activity is involved.
 */
public final class ActivationNotification {
    private static final int NOTIFICATION_ID = 2301;
    private static final String CHANNEL_ID = "admin_activation_reminder";

    private ActivationNotification() { }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Device admin activation",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription(
                        "User-requested shortcut to Android's device-admin confirmation.");
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showActivationNotification(Context context) {
        createNotificationChannel(context);
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 24 && !manager.areNotificationsEnabled()) return;

        ComponentName adminComponent = new ComponentName(context, AdminReceiver.class);
        Intent systemAdminConfirmation = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        systemAdminConfirmation.putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        systemAdminConfirmation.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "YW app installer requests device administrator status. Android will ask you to approve or cancel.");
        systemAdminConfirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Direct PendingIntent to the system intent resolver/admin UI. Do not
        // target AdminActivationActivity (or any YW Activity) as an intermediary.
        PendingIntent adminConfirmationPendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID + 1,
                systemAdminConfirmation,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle("YW app installer")
                .setContentText("Tap to open Android's device-admin activation confirmation.")
                .setStyle(new Notification.BigTextStyle().bigText(
                        "This opens Android's system-owned confirmation directly. YW Installer does not open an Activity first, and activation still requires your approval."))
                .setAutoCancel(true)
                .setContentIntent(adminConfirmationPendingIntent)
                .addAction(
                        android.R.drawable.ic_lock_lock,
                        "Activate admin",
                        adminConfirmationPendingIntent);

        if (Build.VERSION.SDK_INT >= 26) builder.setChannelId(CHANNEL_ID);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
