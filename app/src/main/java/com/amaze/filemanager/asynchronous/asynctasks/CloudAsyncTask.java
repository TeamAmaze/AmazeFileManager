package com.amaze.filemanager.asynchronous.asynctasks;

import android.database.Cursor;
import android.os.AsyncTask;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.database.models.CloudEntry;
import com.amaze.filemanager.exceptions.CloudPluginException;
import com.amaze.filemanager.ui.views.drawer.Drawer;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.application.AppConfig;
import com.cloudrail.si.CloudRail;
import com.cloudrail.si.exceptions.AuthenticationException;
import com.cloudrail.si.exceptions.ParseException;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Box;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.OneDrive;
import com.cloudrail.si.services.PCloud;

import java.lang.ref.WeakReference;

/**
 * Created by Vishal on 08-03-2018.
 *
 * Class handles connection with cloud services after the result retrieved from cloud plugin
 * and storage of returned objects to a persistent storage so that user don't have to enter
 * credentials after app restart
 */

public class CloudAsyncTask extends AsyncTask<Cursor, Void, Boolean> {

    private DataUtils dataUtils = DataUtils.getInstance();
    private WeakReference<MainActivity> weakReference;
    private Cursor data;
    private Drawer drawer;

    public CloudAsyncTask(MainActivity mainActivity, Drawer drawer) {
        weakReference = new WeakReference<>(mainActivity);
        this.drawer = drawer;
    }

    @Override
    protected Boolean doInBackground(Cursor... params) {
        boolean hasUpdatedDrawer = false;

        data = params[0];
        MainActivity mainActivity = weakReference.get();

        if (data.getCount() > 0 && data.moveToFirst()) {
            do {

                switch (data.getInt(0)) {
                    case 1:
                        try {
                            CloudRail.setAppKey(data.getString(1));
                        } catch (Exception e) {
                            // any other exception due to network conditions or other error
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.failed_cloud_api_key));
                            return false;
                        }
                        break;
                    case 2:
                        // DRIVE
                        try {
                            CloudEntry cloudEntryGdrive = null;
                            CloudEntry savedCloudEntryGdrive;

                            GoogleDrive cloudStorageDrive = new GoogleDrive(mainActivity.getApplicationContext(),
                                    data.getString(1), "", MainActivity.CLOUD_AUTHENTICATOR_REDIRECT_URI, data.getString(2));
                            cloudStorageDrive.useAdvancedAuthentication();

                            if ((savedCloudEntryGdrive = mainActivity.cloudHandler.findEntry(OpenMode.GDRIVE)) != null) {
                                // we already have the entry and saved state, get it

                                try {
                                    cloudStorageDrive.loadAsString(savedCloudEntryGdrive.getPersistData());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    // we need to update the persist string as existing one is been compromised

                                    cloudStorageDrive.login();
                                    cloudEntryGdrive = new CloudEntry(OpenMode.GDRIVE, cloudStorageDrive.saveAsString());
                                    mainActivity.cloudHandler.updateEntry(OpenMode.GDRIVE, cloudEntryGdrive);
                                }
                            } else {
                                cloudStorageDrive.login();
                                cloudEntryGdrive = new CloudEntry(OpenMode.GDRIVE, cloudStorageDrive.saveAsString());
                                mainActivity.cloudHandler.addEntry(cloudEntryGdrive);
                            }

                            dataUtils.addAccount(cloudStorageDrive);
                            hasUpdatedDrawer = true;
                        } catch (CloudPluginException e) {
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.cloud_error_plugin));
                            mainActivity.deleteConnection(OpenMode.GDRIVE);
                            return false;
                        } catch (AuthenticationException e) {
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.cloud_fail_authenticate));
                            mainActivity.deleteConnection(OpenMode.GDRIVE);
                            return false;
                        } catch (Exception e) {
                            // any other exception due to network conditions or other error
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.failed_cloud_new_connection));
                            mainActivity.deleteConnection(OpenMode.GDRIVE);
                            return false;
                        }
                        break;
                    case 3:
                        // DROPBOX
                        try {
                            CloudEntry cloudEntryDropbox = null;
                            CloudEntry savedCloudEntryDropbox;

                            CloudStorage cloudStorageDropbox = new Dropbox(mainActivity.getApplicationContext(),
                                    data.getString(1), data.getString(2));

                            if ((savedCloudEntryDropbox = mainActivity.cloudHandler.findEntry(OpenMode.DROPBOX)) != null) {
                                // we already have the entry and saved state, get it
                                try {
                                    cloudStorageDropbox.loadAsString(savedCloudEntryDropbox.getPersistData());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    // we need to persist data again

                                    cloudStorageDropbox.login();
                                    cloudEntryDropbox = new CloudEntry(OpenMode.DROPBOX, cloudStorageDropbox.saveAsString());
                                    mainActivity.cloudHandler.updateEntry(OpenMode.DROPBOX, cloudEntryDropbox);
                                }
                            } else {
                                cloudStorageDropbox.login();
                                cloudEntryDropbox = new CloudEntry(OpenMode.DROPBOX, cloudStorageDropbox.saveAsString());
                                mainActivity.cloudHandler.addEntry(cloudEntryDropbox);
                            }

                            dataUtils.addAccount(cloudStorageDropbox);
                            hasUpdatedDrawer = true;
                        } catch (CloudPluginException e) {
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.cloud_error_plugin));
                            mainActivity.deleteConnection(OpenMode.DROPBOX);
                            return false;
                        } catch (AuthenticationException e) {
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.cloud_fail_authenticate));
                            mainActivity.deleteConnection(OpenMode.DROPBOX);
                            return false;
                        } catch (Exception e) {
                            // any other exception due to network conditions or other error
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.failed_cloud_new_connection));
                            mainActivity.deleteConnection(OpenMode.DROPBOX);
                            return false;
                        }
                        break;
                    case 4:
                        // BOX
                        try {
                            CloudEntry cloudEntryBox = null;
                            CloudEntry savedCloudEntryBox;

                            CloudStorage cloudStorageBox = new Box(mainActivity.getApplicationContext(),
                                    data.getString(1), data.getString(2));

                            if ((savedCloudEntryBox = mainActivity.cloudHandler.findEntry(OpenMode.BOX)) != null) {
                                // we already have the entry and saved state, get it
                                try {
                                    cloudStorageBox.loadAsString(savedCloudEntryBox.getPersistData());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    // we need to persist data again
                                    cloudStorageBox.login();
                                    cloudEntryBox = new CloudEntry(OpenMode.BOX, cloudStorageBox.saveAsString());
                                    mainActivity.cloudHandler.updateEntry(OpenMode.BOX, cloudEntryBox);
                                }
                            } else {
                                cloudStorageBox.login();
                                cloudEntryBox = new CloudEntry(OpenMode.BOX, cloudStorageBox.saveAsString());
                                mainActivity.cloudHandler.addEntry(cloudEntryBox);
                            }

                            dataUtils.addAccount(cloudStorageBox);
                            hasUpdatedDrawer = true;
                        } catch (CloudPluginException e) {
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.cloud_error_plugin));
                            mainActivity.deleteConnection(OpenMode.BOX);
                            return false;
                        } catch (AuthenticationException e) {
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.cloud_fail_authenticate));
                            mainActivity.deleteConnection(OpenMode.BOX);
                            return false;
                        } catch (Exception e) {
                            // any other exception due to network conditions or other error
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.failed_cloud_new_connection));
                            mainActivity.deleteConnection(OpenMode.BOX);
                            return false;
                        }
                        break;
                    case 5:
                        // ONEDRIVE
                        try {
                            CloudEntry cloudEntryOnedrive = null;
                            CloudEntry savedCloudEntryOnedrive;

                            CloudStorage cloudStorageOnedrive = new OneDrive(mainActivity.getApplicationContext(),
                                    data.getString(1), data.getString(2));

                            if ((savedCloudEntryOnedrive = mainActivity.cloudHandler.findEntry(OpenMode.ONEDRIVE)) != null) {
                                // we already have the entry and saved state, get it
                                try {
                                    cloudStorageOnedrive.loadAsString(savedCloudEntryOnedrive.getPersistData());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    // we need to persist data again

                                    cloudStorageOnedrive.login();
                                    cloudEntryOnedrive = new CloudEntry(OpenMode.ONEDRIVE, cloudStorageOnedrive.saveAsString());
                                    mainActivity.cloudHandler.updateEntry(OpenMode.ONEDRIVE, cloudEntryOnedrive);
                                }
                            } else {
                                cloudStorageOnedrive.login();
                                cloudEntryOnedrive = new CloudEntry(OpenMode.ONEDRIVE, cloudStorageOnedrive.saveAsString());
                                mainActivity.cloudHandler.addEntry(cloudEntryOnedrive);
                            }

                            dataUtils.addAccount(cloudStorageOnedrive);
                            hasUpdatedDrawer = true;
                        } catch (CloudPluginException e) {
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.cloud_error_plugin));
                            mainActivity.deleteConnection(OpenMode.ONEDRIVE);
                            return false;
                        } catch (AuthenticationException e) {
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.cloud_fail_authenticate));
                            mainActivity.deleteConnection(OpenMode.ONEDRIVE);
                            return false;
                        } catch (Exception e) {
                            // any other exception due to network conditions or other error
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.failed_cloud_new_connection));
                            mainActivity.deleteConnection(OpenMode.ONEDRIVE);
                            return false;
                        }
                        break;
                    case 6:
                        // pcloud
                        try {
                            CloudEntry cloudEntryPCloud = null;
                            CloudEntry savedCloudEntryPCloud;

                            CloudStorage cloudStoragePCloud = new PCloud(mainActivity.getApplicationContext(),
                                    data.getString(1), data.getString(2));

                            if ((savedCloudEntryPCloud = mainActivity.cloudHandler.findEntry(OpenMode.PCLOUD)) != null) {
                                // we already have the entry and saved state, get it
                                try {
                                    cloudStoragePCloud.loadAsString(savedCloudEntryPCloud.getPersistData());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    // we need to persist data again

                                    cloudStoragePCloud.login();
                                    cloudEntryPCloud = new CloudEntry(OpenMode.PCLOUD, cloudStoragePCloud.saveAsString());
                                    mainActivity.cloudHandler.updateEntry(OpenMode.PCLOUD, cloudEntryPCloud);
                                }
                            } else {
                                cloudStoragePCloud.login();
                                cloudEntryPCloud = new CloudEntry(OpenMode.PCLOUD, cloudStoragePCloud.saveAsString());
                                mainActivity.cloudHandler.addEntry(cloudEntryPCloud);
                            }

                            dataUtils.addAccount(cloudStoragePCloud);
                            hasUpdatedDrawer = true;
                        } catch (CloudPluginException e) {
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.cloud_error_plugin));
                            mainActivity.deleteConnection(OpenMode.PCLOUD);
                            return false;
                        } catch (AuthenticationException e) {
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.cloud_fail_authenticate));
                            mainActivity.deleteConnection(OpenMode.PCLOUD);
                            return false;
                        } catch (Exception e) {
                            // any other exception due to network conditions or other error
                            e.printStackTrace();
                            AppConfig.toast(mainActivity, mainActivity.getResources().getString(R.string.failed_cloud_new_connection));
                            mainActivity.deleteConnection(OpenMode.PCLOUD);
                            return false;
                        }
                        break;
                    default:
                        Toast.makeText(mainActivity, mainActivity.getResources().getString(R.string.cloud_error_failed_restart),
                                Toast.LENGTH_LONG).show();
                        return false;
                }
            } while (data.moveToNext());

            mainActivity = null;
        }
        return hasUpdatedDrawer;
    }

    @Override
    protected void onPostExecute(Boolean refreshDrawer) {
        super.onPostExecute(refreshDrawer);
        if (refreshDrawer) {

            drawer.refreshDrawer();
        }
    }
}
