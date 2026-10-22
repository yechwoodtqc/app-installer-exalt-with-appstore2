package com.lge.lginstallservie;

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/** Android binds this service when the user adds the YW account in Settings. */
public class AccountAuthenticatorService extends Service {
    private YwAccountAuthenticator authenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        authenticator = new YwAccountAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent != null
                && AccountManager.ACTION_AUTHENTICATOR_INTENT.equals(intent.getAction())) {
            return authenticator.getIBinder();
        }
        return null;
    }
}
