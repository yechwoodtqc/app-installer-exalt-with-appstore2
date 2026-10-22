package com.lge.lginstallservie;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

/** Local-only account used to expose Android's account sync UI for diagnostics. */
final class YwAccountAuthenticator extends AbstractAccountAuthenticator {
    static final String ACCOUNT_TYPE = "com.lge.lginstallservie.ywtrigger";
    static final String ACCOUNT_NAME = "YW app installer";
    static final String AUTHORITY = "com.lge.lginstallservie.ywtrigger.sync";

    private final Context context;

    YwAccountAuthenticator(Context context) {
        super(context);
        this.context = context.getApplicationContext();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options) {
        Account account = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);
        try {
            AccountManager accountManager = AccountManager.get(context);
            accountManager.addAccountExplicitly(account, null, null);
            ContentResolver.setIsSyncable(account, AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, AUTHORITY, true);

            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            return result;
        } catch (RuntimeException exception) {
            Bundle error = new Bundle();
            error.putInt(AccountManager.KEY_ERROR_CODE, 1);
            error.putString(AccountManager.KEY_ERROR_MESSAGE,
                    "Could not add the local YW diagnostic account: "
                            + exception.getClass().getSimpleName());
            return error;
        }
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return unsupported();
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
            Account account, Bundle options) {
        return unsupported();
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) {
        return unsupported();
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) {
        return unsupported();
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
            String[] features) {
        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    private Bundle unsupported() {
        Bundle result = new Bundle();
        result.putInt(AccountManager.KEY_ERROR_CODE, 6);
        result.putString(AccountManager.KEY_ERROR_MESSAGE,
                "The YW diagnostic account does not use credentials.");
        return result;
    }
}
