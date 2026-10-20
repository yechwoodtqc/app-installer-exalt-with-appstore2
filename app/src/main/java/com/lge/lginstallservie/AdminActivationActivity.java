package com.lge.lginstallservie;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

/**
 * A tiny notification-entry activity. It launches Android's system-owned
 * device-admin consent screen, then closes when the user returns.
 */
public class AdminActivationActivity extends Activity {
    private static final int REQUEST_ACTIVATE_ADMIN = 2304;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DevicePolicyManager policy =
                (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);

        if (policy != null && policy.isAdminActive(admin)) {
            finish();
            return;
        }

        Intent activate = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        activate.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
        activate.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "YW app installer requests device administrator status. This is separate from APK installation; Android will ask you to approve or cancel.");

        try {
            startActivityForResult(activate, REQUEST_ACTIVATE_ADMIN);
        } catch (RuntimeException e) {
            // If the device policy or OEM system UI blocks activation, fail closed
            // instead of crashing. The OS does not permit apps to bypass that policy.
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ACTIVATE_ADMIN) finish();
    }
}
