package com.amaze.filemanager.utils.provider;

/**
 * Created by vishal on 15/2/17.
 */

public class DatabaseContract {

    public static final String APP_PACKAGE_NAME = "com.filemanager.amazecloud";

    public static final String PROVIDER_AUTHORITY = "com.amaze.cloud.provider";

    public static final String PERMISSION_PROVIDER = "com.amaze.cloud.permission.ACCESS_PROVIDER";

    public static final String ACCOUNT_TYPE_GOOGLE_DRIVE = "com.amaze.cloud.account.GDRIVE";
    public static final String ACCOUNT_TYPE_DROPBOX = "com.amaze.cloud.account.DROPBOX";
    public static final String ACCOUNT_TYPE_BOX = "com.amaze.cloud.account.BOX";
    public static final String ACCOUNT_TYPE_ONE_DRIVE = "com.amaze.cloud.account.ONEDRIVE";

    public static final String DATABASE_NAME_GDRIVE = "cloud_gdrive.db";
    public static final String DATABASE_NAME_DROPBOX = "cloud_dropbox.db";
    public static final String DATABASE_NAME_BOX = "cloud_box.db";
    public static final String DATABASE_NAME_ONEDRIVE = "cloud_onedrive.db";
    public static final int DATABASE_VERSION = 1;

    public static final String ROW_ID = "_id";
    public static final String ROW_DELETE = "delete";
}
