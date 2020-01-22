package com.amaze.filemanager.database;

import android.content.Context;

import androidx.annotation.NonNull;

import com.amaze.filemanager.database.models.explorer.CloudEntry;
import com.amaze.filemanager.exceptions.CloudPluginException;
import com.amaze.filemanager.fragments.CloudSheetFragment;
import com.amaze.filemanager.utils.OpenMode;

import java.util.Arrays;
import java.util.List;

/**
 * Created by vishal on 18/4/17.
 */
public class CloudHandler {

    public static final String CLOUD_PREFIX_BOX = "box:/";
    public static final String CLOUD_PREFIX_DROPBOX = "dropbox:/";
    public static final String CLOUD_PREFIX_GOOGLE_DRIVE = "gdrive:/";
    public static final String CLOUD_PREFIX_ONE_DRIVE = "onedrive:/";

    public static final String CLOUD_NAME_GOOGLE_DRIVE = "Google Drive";
    public static final String CLOUD_NAME_DROPBOX = "Dropbox";
    public static final String CLOUD_NAME_ONE_DRIVE = "One Drive";
    public static final String CLOUD_NAME_BOX = "Box";

    private final ExplorerDatabase database;
    private final Context context;

    public CloudHandler(@NonNull Context context) {
        this.context = context;
        this.database = ExplorerDatabase.getInstance();
    }

    public void addEntry(CloudEntry cloudEntry) throws CloudPluginException {

        if (!CloudSheetFragment.isCloudProviderAvailable(context))
            throw new CloudPluginException();

        database.cloudEntryDao().insert(cloudEntry);
    }

    public void clear(OpenMode serviceType) {
        database.cloudEntryDao().delete(database.cloudEntryDao().findByServiceType(serviceType.ordinal()));
    }

    public void updateEntry(OpenMode serviceType, CloudEntry newCloudEntry)
            throws CloudPluginException {

        if (!CloudSheetFragment.isCloudProviderAvailable(context))
            throw new CloudPluginException();

        database.cloudEntryDao().update(newCloudEntry);
    }

    public CloudEntry findEntry(OpenMode serviceType) throws CloudPluginException {

        if (!CloudSheetFragment.isCloudProviderAvailable(context))
            throw new CloudPluginException();

        return database.cloudEntryDao().findByServiceType(serviceType.ordinal());
    }

    public List<CloudEntry> getAllEntries() throws CloudPluginException {

        if (!CloudSheetFragment.isCloudProviderAvailable(context))
            throw new CloudPluginException();

        return Arrays.asList(database.cloudEntryDao().list());
    }
}
