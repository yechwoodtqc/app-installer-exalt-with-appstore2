package com.lge.lginstallservie;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class StateChangeReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "system_state_changes";
    private static final int AIRPLANE_NOTIFICATION_ID = 2501;
    private static final int BLUETOOTH_NOTIFICATION_ID = 2502;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
            boolean enabled = intent.getBooleanExtra(
                    "state",
                    Settings.Global.getInt(context.getContentResolver(),
                            Settings.Global.AIRPLANE_MODE_ON, 0) != 0);
            postStateNotification(context, AIRPLANE_NOTIFICATION_ID,
                    "Airplane mode " + (enabled ? "turned ON" : "turned OFF"),
                    "Airplane mode changed. Current state: " + (enabled ? "ON" : "OFF") + ".");
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_OFF) {
                boolean enabled = state == BluetoothAdapter.STATE_ON;
                postStateNotification(context, BLUETOOTH_NOTIFICATION_ID,
                        "Bluetooth turned " + (enabled ? "ON" : "OFF"),
                        "Bluetooth changed. Current state: " + (enabled ? "ON" : "OFF") + ".");
            }
            // Ignore TURNING_ON / TURNING_OFF intermediate broadcasts.
        }
    }

    private static void postStateNotification(Context context, int id, String title, String message) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || (Build.VERSION.SDK_INT >= 24 && !manager.areNotificationsEnabled())) return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "System state changes", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications when airplane mode or Bluetooth changes state.");
            manager.createNotificationChannel(channel);
        }

        Intent openApp = new Intent(context, MainActivity.class);
        openApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, id, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        if (Build.VERSION.SDK_INT >= 26) builder.setChannelId(CHANNEL_ID);
        manager.notify(id, builder.build());
    }
}
