package com.lge.lginstallservie;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;

public class InstallStatusReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "apk_install_status";
    private static final int NOTIFICATION_ID = 2401;

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "APK installation", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        String title;
        String text;
        PendingIntent contentIntent = null;

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            title = "Confirm APK installation";
            text = "Tap to continue in Android's installation screen.";
            if (confirmation != null) {
                contentIntent = PendingIntent.getActivity(context, 2402, confirmation,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            }
        } else if (status == PackageInstaller.STATUS_SUCCESS) {
            title = "APK installation complete";
            text = "Android reports that the APK was installed successfully.";
        } else {
            title = "APK installation failed";
            String detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            text = detail == null ? "Android could not install this APK." : detail;
        }

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= 26) builder.setChannelId(CHANNEL_ID);
        if (contentIntent != null) builder.setContentIntent(contentIntent);
        manager.notify(NOTIFICATION_ID, builder.build());
        String packageName = intent.getStringExtra("yw_package_name");
        String version = intent.getStringExtra("yw_version");
        if (packageName != null && packageName.length() > 0) {
            StoreReporter.report(context.getApplicationContext(), packageName, version == null ? "" : version,
                    status == PackageInstaller.STATUS_SUCCESS ? "installed" : "failed");
        }
    }
}
