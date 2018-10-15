package com.amaze.filemanager.asynchronous.asynctasks;

import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.database.models.CloudEntry;
import com.amaze.filemanager.exceptions.CloudPluginException;
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

import java.lang.ref.WeakReference;

public class CloudLoaderAsyncTask extends AsyncTask<Void, Void, Boolean> {

    private Cursor data;
    private WeakReference<MainActivity> mainActivityWeakReference;
    private CloudHandler cloudHandler;
    private DataUtils dataUtils;
    public CloudLoaderAsyncTask(WeakReference<MainActivity> mainActivityWeakReference,
                                CloudHandler cloudHandler, Cursor data) {
        this.data = data;
        this.mainActivityWeakReference = mainActivityWeakReference;
        this.cloudHandler = cloudHandler;
        this.dataUtils = DataUtils.getInstance();
    }

    @Override
    @NonNull
    public Boolean doInBackground(Void... voids) {
        boolean hasUpdatedDrawer = false;

        if (data == null) return false;
        if (data.getCount() > 0 && data.moveToFirst()) {
            do {

                if (mainActivityWeakReference.get() == null) {
                    cancel(true);
                    return false;
                }
                if (isCancelled()) cancel(true);

                switch (data.getInt(0)) {
                    case 1:
                        try {
                            CloudRail.setAppKey(data.getString(1));
                        } catch (Exception e) {
                            // any other exception due to network conditions or other error
                            e.printStackTrace();
                            if (mainActivityWeakReference.get() != null) {
                                AppConfig.toast(mainActivityWeakReference.get(),
                                        mainActivityWeakReference.get().getString(R.string.failed_cloud_api_key));
                            } else {
                                cancel(true);
                            }
                            return false;
                        }
                        break;
                    case 2:
                        // DRIVE
                        try {
                            CloudEntry cloudEntryGdrive = null;
                            CloudEntry savedCloudEntryGdrive;
                            GoogleDrive cloudStorageDrive;
                            if (mainActivityWeakReference.get() != null) {
                                cloudStorageDrive = new GoogleDrive(mainActivityWeakReference.get().getApplicationContext(),
                                        data.getString(1), "",
                                        MainActivity.CLOUD_AUTHENTICATOR_REDIRECT_URI, data.getString(2));
                            } else {
                                cancel(true);
                                return false;
                            }
                            cloudStorageDrive.useAdvancedAuthentication();

                            if ((savedCloudEntryGdrive = cloudHandler.findEntry(OpenMode.GDRIVE)) != null) {
                                // we already have the entry and saved state, get it

                                try {
                                    cloudStorageDrive.loadAsString(savedCloudEntryGdrive.getPersistData());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    // we need to update the persist string as existing one is been compromised

                                    cloudStorageDrive.login();
                                    cloudEntryGdrive = new CloudEntry(OpenMode.GDRIVE, cloudStorageDrive.saveAsString());
                                    cloudHandler.updateEntry(OpenMode.GDRIVE, cloudEntryGdrive);
                                }
                            } else {
                                cloudStorageDrive.login();
                                cloudEntryGdrive = new CloudEntry(OpenMode.GDRIVE, cloudStorageDrive.saveAsString());
                                cloudHandler.addEntry(cloudEntryGdrive);
                            }

                            dataUtils.addAccount(cloudStorageDrive);
                            hasUpdatedDrawer = true;
                        } catch (CloudPluginException e) {
                            e.printStackTrace();
                            if (mainActivityWeakReference.get() != null) {
                                AppConfig.toast(mainActivityWeakReference.get(),
                                        mainActivityWeakReference.get().getResources().getString(R.string.cloud_error_plugin));
                                mainActivityWeakReference.get().deleteConnection(OpenMode.GDRIVE);
                            } else {
                                cancel(true);
                            }
                            return false;
                        } catch (AuthenticationException e) {
                            e.printStackTrace();
                            if (mainActivityWeakReference.get() != null) {
                                AppConfig.toast(mainActivityWeakReference.get(),
                                        mainActivityWeakReference.get().getResources().getString(R.string.cloud_fail_authenticate));
                                mainActivityWeakReference.get().deleteConnection(OpenMode.GDRIVE);
                            } else {
                                cancel(true);
                            }
                            return false;
                        } catch (Exception e) {
                            // any other exception due to network conditions or other error
                            e.printStackTrace();
                            if (mainActivityWeakReference.get() != null) {
                                AppConfig.toast(mainActivityWeakReference.get(),
                                        mainActivityWeakReference.get().getResources().getString(R.string.failed_cloud_new_connection));
                                mainActivityWeakReference.get().deleteConnection(OpenMode.GDRIVE);
                            } else {
                                cancel(true);
                            }
                            return false;
                        }
                        break;
                    case 3:
                        // DROPBOX
                        try {
                            CloudEntry cloudEntryDropbox = null;
                            CloudEntry savedCloudEntryDropbox;
                            CloudStorage cloudStorageDropbox;
                            if (mainActivityWeakReference.get() != null) {
                                cloudStorageDropbox = new Dropbox(mainActivityWeakReference.get().getApplicationContext(),
                                        data.getString(1), data.getString(2));
                            } else {
                                cancel(true);
                                return false;
                            }

                            if ((savedCloudEntryDropbox = cloudHandler.findEntry(OpenMode.DROPBOX)) != null) {
                                // we already have the entry and saved state, get it
                                try {
                                    cloudStorageDropbox.loadAsString(savedCloudEntryDropbox.getPersistData());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    // we need to persist data again

                                    cloudStorageDropbox.login();
                                    cloudEntryDropbox = new CloudEntry(OpenMode.DROPBOX, cloudStorageDropbox.saveAsString());
                                    cloudHandler.updateEntry(OpenMode.DROPBOX, cloudEntryDropbox);
                                }
                            } else {
                                cloudStorageDropbox.login();
                                cloudEntryDropbox = new CloudEntry(OpenMode.DROPBOX, cloudStorageDropbox.saveAsString());
                                cloudHandler.addEntry(cloudEntryDropbox);
                            }

                            dataUtils.addAccount(cloudStorageDropbox);
                            hasUpdatedDrawer = true;
                        } catch (CloudPluginException e) {
                            e.printStackTrace();
                            if (mainActivityWeakReference.get() != null) {
                                AppConfig.toast(mainActivityWeakReference.get(),
                                        mainActivityWeakReference.get().getResources().getString(R.string.cloud_error_plugin));
                                mainActivityWeakReference.get().deleteConnection(OpenMode.DROPBOX);
                            } else {
                                cancel(true);
                            }
                            return false;
                        } catch (AuthenticationException e) {
                            e.printStackTrace();
                            if (mainActivityWeakReference.get() != null) {
                                AppConfig.toast(mainActivityWeakReference.get(),
                                        mainActivityWeakReference.get().getResources().getString(R.string.cloud_fail_authenticate));
                                mainActivityWeakReference.get().deleteConnection(OpenMode.DROPBOX);
                            } else cancel(true);
                            return false;
                        } catch (Exception e) {
                            // any other exception due to network conditions or other error
                            e.printStackTrace();
                            if (mainActivityWeakReference.get() != null) {
                                AppConfig.toast(mainActivityWeakReference.get(),
                                        mainActivityWeakReference.get().getResources().getString(R.string.failed_cloud_new_connection));
                                mainActivityWeakReference.get().deleteConnection(OpenMode.DROPBOX);
                            } else cancel(true);
                            return false;
                        }
                        break;
                    case 4:
                        // BOX
                        try {
                            CloudEntry cloudEntryBox = null;
                            CloudEntry savedCloudEntryBox;
                            CloudStorage cloudStorageBox;
                            if (mainActivityWeakReference.get() != null) {
                                cloudStorageBox = new Box(mainActivityWeakReference.get().getApplicationContext(),
                                        data.getString(1), data.getString(2));
                            } else {
                                cancel(true);
                                return false;
                            }

                            if ((savedCloudEntryBox = cloudHandler.findEntry(OpenMode.BOX)) != null) {
                                // we already have the entry and saved state, get it
                                try {
                                    cloudStorageBox.loadAsString(savedCloudEntryBox.getPersistData());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    // we need to persist data again
                                    cloudStorageBox.login();
                                    cloudEntryBox = new CloudEntry(OpenMode.BOX, cloudStorageBox.saveAsString());
                                    cloudHandler.updateEntry(OpenMode.BOX, cloudEntryBox);
                                }
                            } else {
                                cloudStorageBox.login();
                                cloudEntryBox = new CloudEntry(OpenMode.BOX, cloudStorageBox.saveAsString());
                                cloudHandler.addEntry(cloudEntryBox);
                            }

                            dataUtils.addAccount(cloudStorageBox);
                            hasUpdatedDrawer = true;
                        } catch (CloudPluginException e) {
                            e.printStackTrace();
                            if (mainActivityWeakReference.get() != null) {
                                AppConfig.toast(mainActivityWeakReference.get(),
                                        mainActivityWeakReference.get().getResources().getString(R.string.cloud_error_plugin));
                                mainActivityWeakReference.get().deleteConnection(OpenMode.BOX);
                            } else cancel(true);
                            return false;
                        } catch (AuthenticationException e) {
                            e.printStackTrace();
                            if (mainActivityWeakReference.get() != null) {
                                AppConfig.toast(mainActivityWeakReference.get(),
                                        mainActivityWeakReference.get().getResources().getString(R.string.cloud_fail_authenticate));
                                mainActivityWeakReference.get().deleteConnection(OpenMode.BOX);
                            } else cancel(true);
                            return false;
                        } catch (Exception e) {
                            // any other exception due to network conditions or other error
                            e.printStackTrace();
                            if (mainActivityWeakReference.get() != null) {
                                AppConfig.toast(mainActivityWeakReference.get(),
                                        mainActivityWeakReference.get().getResources().getString(R.string.failed_cloud_new_connection));
                                mainActivityWeakReference.get().deleteConnection(OpenMode.BOX);
                            } else cancel(true);
                            return false;
                        }
                        break;
                    case 5:
                        // ONEDRIVE
                        try {
                            CloudEntry cloudEntryOnedrive = null;
                            CloudEntry savedCloudEntryOnedrive;
                            CloudStorage cloudStorageOnedrive;
                            if (mainActivityWeakReference.get() != null) {
                                cloudStorageOnedrive= new OneDrive(mainActivityWeakReference.get().getApplicationContext(),
                                        data.getString(1), data.getString(2));
                            } else {
                                cancel(true);
                                return false;
                            }

                            if ((savedCloudEntryOnedrive = cloudHandler.findEntry(OpenMode.ONEDRIVE)) != null) {
                                // we already have the entry and saved state, get it
                                try {
                                    cloudStorageOnedrive.loadAsString(savedCloudEntryOnedrive.getPersistData());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    // we need to persist data again

                                    cloudStorageOnedrive.login();
                                    cloudEntryOnedrive = new CloudEntry(OpenMode.ONEDRIVE, cloudStorageOnedrive.saveAsString());
                                    cloudHandler.updateEntry(OpenMode.ONEDRIVE, cloudEntryOnedrive);
                                }
                            } else {
                                cloudStorageOnedrive.login();
                                cloudEntryOnedrive = new CloudEntry(OpenMode.ONEDRIVE, cloudStorageOnedrive.saveAsString());
                                cloudHandler.addEntry(cloudEntryOnedrive);
                            }

                            dataUtils.addAccount(cloudStorageOnedrive);
                            hasUpdatedDrawer = true;
                        } catch (CloudPluginException e) {
                            e.printStackTrace();
                            if (mainActivityWeakReference.get() != null) {
                                AppConfig.toast(mainActivityWeakReference.get(),
                                        mainActivityWeakReference.get().getResources().getString(R.string.cloud_error_plugin));
                                mainActivityWeakReference.get().deleteConnection(OpenMode.ONEDRIVE);
                            } else cancel(true);
                            return false;
                        } catch (AuthenticationException e) {
                            e.printStackTrace();
                            if (mainActivityWeakReference.get() != null) {
                                AppConfig.toast(mainActivityWeakReference.get(),
                                        mainActivityWeakReference.get().getResources().getString(R.string.cloud_fail_authenticate));
                                mainActivityWeakReference.get().deleteConnection(OpenMode.ONEDRIVE);
                            } else cancel(true);
                            return false;
                        } catch (Exception e) {
                            // any other exception due to network conditions or other error
                            e.printStackTrace();
                            if (mainActivityWeakReference.get() != null) {
                                AppConfig.toast(mainActivityWeakReference.get(),
                                        mainActivityWeakReference.get().getResources().getString(R.string.failed_cloud_new_connection));
                                mainActivityWeakReference.get().deleteConnection(OpenMode.ONEDRIVE);
                            } else cancel(true);
                            return false;
                        }
                        break;
                    default:
                        if (mainActivityWeakReference.get() != null) {
                            Toast.makeText(mainActivityWeakReference.get(),
                                    mainActivityWeakReference.get().getResources().getString(R.string.cloud_error_failed_restart),
                                    Toast.LENGTH_LONG).show();
                        } else cancel(true);
                        return false;
                }
            } while (data.moveToNext());
        }
        return hasUpdatedDrawer;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if (mainActivityWeakReference.get() != null) {
            mainActivityWeakReference.get().getSupportLoaderManager().destroyLoader(MainActivity.REQUEST_CODE_CLOUD_LIST_KEY);
            mainActivityWeakReference.get().getSupportLoaderManager().destroyLoader(MainActivity.REQUEST_CODE_CLOUD_LIST_KEYS);
        }
    }

    @Override
    public void onPostExecute(@NonNull Boolean result) {
        if (result) {
            if (mainActivityWeakReference.get() != null) {
                mainActivityWeakReference.get().getDrawer().refreshDrawer();
            }
        }
    }
}
