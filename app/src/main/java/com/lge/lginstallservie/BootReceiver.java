package com.lge.lginstallservie;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Post the activation notification immediately after boot, then keep
            // the reminder schedule alive. This receiver does not launch MainActivity.
            ReminderReceiver.showActivationNotification(context);
            ReminderReceiver.scheduleNext(context);
        }
    }
}
