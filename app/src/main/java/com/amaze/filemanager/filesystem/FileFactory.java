package com.amaze.filemanager.filesystem;

import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;

import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.filesystem.file_types.BoxFile;
import com.amaze.filemanager.filesystem.file_types.DropboxFile;
import com.amaze.filemanager.filesystem.file_types.GDriveFile;
import com.amaze.filemanager.filesystem.file_types.OTGFile;
import com.amaze.filemanager.filesystem.file_types.OneDriveFile;
import com.amaze.filemanager.filesystem.file_types.RootFile;
import com.amaze.filemanager.filesystem.file_types.SFTPFile;
import com.amaze.filemanager.filesystem.file_types.SMBFile;
import com.amaze.filemanager.filesystem.file_types.SimpleFile;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;

import java.io.File;

/**
 * Created by Rustam Khadipash on 4/6/2018.
 */
public class FileFactory {
    public HybridFile getInstance(Context context, String path) {
        if (path.startsWith("smb://")) {
            return new SMBFile(OpenMode.SMB, path);
        } else if (path.startsWith("ssh://")) {
            return new SFTPFile(OpenMode.SFTP, path);
        } else if (path.startsWith(OTGUtil.PREFIX_OTG)) {
            return new OTGFile(OpenMode.OTG, path);
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_BOX)) {
            return new BoxFile(OpenMode.BOX, path);
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)) {
            return new OneDriveFile(OpenMode.ONEDRIVE, path);
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE)) {
            return new GDriveFile(OpenMode.GDRIVE, path);
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX)) {
            return new DropboxFile(OpenMode.DROPBOX, path);
        } else if(context == null) {
            return new SimpleFile(OpenMode.FILE, path);
        } else {
            boolean rootmode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, false);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                if (rootmode && !getFile(path).canRead()) {
                    return new RootFile(OpenMode.ROOT, path);
                }
                else{
                    return new SimpleFile(OpenMode.FILE, path);
                }
            } else {
                if (FileUtil.isOnExtSdCard(getFile(path), context)) {
                    return new SimpleFile(OpenMode.FILE, path);
                } else if (rootmode && !getFile(path).canRead()) {
                    return new RootFile(OpenMode.ROOT, path);
                }

                return new SimpleFile(OpenMode.FILE, path);
            }
        }
    }

    private File getFile(String path) {
        return new File(path);
    }
}
