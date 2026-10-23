package com.lge.lginstallservie;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Only an explicit user-requested sync should post the activation notification.
 * Automatic and initialization sync callbacks are ignored.
 */
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
                boolean manual = extras != null
                        && extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
                if (!manual) {
                    Log.i(TAG, "Ignoring non-manual account sync; no notification triggered.");
                    return;
                }

                Log.i(TAG, "Manual account sync requested; posting direct system activation notification.");
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
