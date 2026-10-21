package com.lge.lginstallservie;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Invisible user-initiated link entry point. Opening ywinstaller://activate
 * starts this activity, requests the reminder notification, and immediately
 * closes without showing MainActivity.
 */
public class LinkTriggerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ReminderReceiver.createNotificationChannel(this);
        ReminderReceiver.showActivationNotification(this);
        Toast.makeText(
                this,
                "YW DEBUG: activation link opened; notification request sent.",
                Toast.LENGTH_LONG
        ).show();

        finish();
    }
}
