package com.amaze.filemanager.utils.provider;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import com.amaze.filemanager.exceptions.TypeNotSupportedException;
import com.amaze.filemanager.services.cloud.CloudAccountsService;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OpenMode;

/**
 * Created by vishal on 15/2/17.
 *
 * Helper class to handle invoke creation and sync of {@link android.accounts.Account}
 * using {@link android.accounts.AccountManager}
 * Supports creation and handling of various accounts for various types
 */

public class SyncUtils {

    private static final long SYNC_FREQUENCY = 60 * 60;  // 1 hour (in seconds)

    /**
     * Creates a new account if not already created in system accounts list
     * Possibly the first time or user deleted the account earlier
     * @param context
     */
    public static void CreateSyncAccount(Context context, String userName, OpenMode accountType,
                                         String password) {

        boolean newAccount = false;

        Account account = CloudAccountsService.GET_ACCOUNT(userName,
                OpenMode.ACCOUNT_MAP.get(accountType));
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        if (accountManager.addAccountExplicitly(account, null, null)) {
            ContentResolver.setIsSyncable(account, DatabaseContract.PROVIDER_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, DatabaseContract.PROVIDER_AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, DatabaseContract.PROVIDER_AUTHORITY,
                    new Bundle(), SYNC_FREQUENCY);
            accountManager.setPassword(account, password);
            newAccount = true;
        }

        if (newAccount) {
            TriggerRefresh(userName, accountType);
        }
    }

    /**
     * Queries for accounts in central repository in system
     * @param activity
     * @return the accounts associated with type
     */
    public static Account[] QueryAccounts(Activity activity, OpenMode accountType) throws TypeNotSupportedException {

        if (accountType.ordinal()<=5) {
            // we're not dealing with cloud account
            throw new TypeNotSupportedException();
        }

        AccountManager accountManager = (AccountManager) activity.getSystemService(Context.ACCOUNT_SERVICE);

        if (!MainActivityHelper.checkAccountsPermission(activity))
            MainActivityHelper.requestAccountsPermission(activity);
        return accountManager.getAccountsByType(OpenMode.ACCOUNT_MAP.get(accountType));
    }

    /**
     * Returns
     * @param activity
     * @param accountName username of account to get
     * @param accountType
     * @return an account object associated with a specific account name and type from
     *         from central repository. Returns null if account not in central repository
     */
    public static boolean ContainsAccount(Activity activity, String accountName, OpenMode accountType) {
        AccountManager accountManager = (AccountManager) activity.getSystemService(Context.ACCOUNT_SERVICE);

        if (!MainActivityHelper.checkAccountsPermission(activity))
            MainActivityHelper.requestAccountsPermission(activity);

        Account[] accounts = accountManager.getAccountsByType(OpenMode.ACCOUNT_MAP.get(accountType));
        for (Account account : accounts) {
            if (account.name.equals(accountName)) return true;
        }

        return false;
    }

    public static boolean RemoveAccount(Activity activity, Account account) {

        AccountManager accountManager = (AccountManager) activity.getSystemService(Context.ACCOUNT_SERVICE);

        return accountManager.removeAccountExplicitly(account);
    }

    public static void RenameAccount(Context context, Account account, String newName, String newPassword) {

        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        accountManager.setPassword(account, newPassword);
        accountManager.renameAccount(account, newName, null, null);
    }

    public static void TriggerRefresh(String accountName, OpenMode accountType) {
        Bundle b = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(
                CloudAccountsService.GET_ACCOUNT(accountName,
                        OpenMode.ACCOUNT_MAP.get(accountType)), // Sync account
                DatabaseContract.PROVIDER_AUTHORITY,                 // Content authority
                b);                                             // Extras
    }
}
