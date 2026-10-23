package com.lge.lginstallservie;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/** Invoked by Android's account-sync UI; deliberately does not start an Activity. */
public class AccountSyncAdapterService extends Service {
    private static final String TAG = "YWAccountSync";
    private AbstractThreadedSyncAdapter syncAdapter;

    @Override
    public void onCreate() {
        super.onCreate();
        final Context appContext = getApplicationContext();
        syncAdapter = new AbstractThreadedSyncAdapter(this, true, false) {
            @Override
            public void onPerformSync(Account account, Bundle extras, String authority,
                    ContentProviderClient provider, SyncResult syncResult) {
                Log.i(TAG, "Android invoked the YW account sync adapter; posting the existing activation notification.");
                ActivationNotification.showActivationNotification(appContext);
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent != null && "android.content.SyncAdapter".equals(intent.getAction())) {
            return syncAdapter.getSyncAdapterBinder();
        }
        return null;
    }
}
