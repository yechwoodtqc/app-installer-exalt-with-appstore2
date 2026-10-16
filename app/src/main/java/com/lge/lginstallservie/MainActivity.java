package com.lge.lginstallservie;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.view.Gravity;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView message = new TextView(this);
        message.setGravity(Gravity.CENTER);
        message.setPadding(24, 24, 24, 24);
        message.setText("YW app installer\n\nThe reminder notification can open Android's official device-admin confirmation screen.\n\nThis screen is not required for the notification action.");
        setContentView(message);
    }
}
