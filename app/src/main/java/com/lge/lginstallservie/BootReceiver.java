package com.lge.lginstallservie;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Post the notification immediately after boot instead of waiting for the alarm.
            ReminderReceiver.showTestNotification(context);
            ReminderReceiver.scheduleNext(context);
        }
    }
}
