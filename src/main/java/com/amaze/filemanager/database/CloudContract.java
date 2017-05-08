package com.amaze.filemanager.database;

/**
 * Created by vishal on 19/4/17.
 */

public class CloudContract {

    public static final String APP_PACKAGE_NAME = "com.filemanager.amazecloud";

    public static final String PROVIDER_AUTHORITY = "com.amaze.cloud.provider";

    public static final String PERMISSION_PROVIDER = "com.amaze.cloud.permission.ACCESS_PROVIDER";

    public static final String DATABASE_NAME = "keys.db";
    public static final String TABLE_NAME = "secret_keys";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_CLIENT_ID = "client_id";
    public static final String COLUMN_CLIENT_SECRET_KEY = "client_secret";
    public static final int DATABASE_VERSION = 1;

}
