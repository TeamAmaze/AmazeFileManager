package com.amaze.filemanager.filesystem.operations.singlefile;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.exceptions.ShellNotRunningException;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.operations.AbstractOperation;
import com.amaze.filemanager.filesystem.operations.exceptions.ShellNotRunningIOException;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.files.FileUtils;
import com.cloudrail.si.interfaces.CloudStorage;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

public class DeleteOperation extends AbstractOperation {
    @NonNull
    private final Context context;
    private final boolean rootMode;
    @NonNull
    private final HybridFile file;
    private final DataUtils dataUtils = DataUtils.getInstance();

    public DeleteOperation(@NonNull Context context, boolean rootMode, @NonNull HybridFile file) {
        this.context = context;
        this.rootMode = rootMode;

        this.file = file;
    }

    @Override
    protected boolean check() {
        return file.exists(context);
    }

    @Override
    protected void execute() throws IOException {
        boolean hasSucceed = deleteFile();
        if (!hasSucceed) {
            throw new IOException("Delete has failed!");
        }
    }

    @Override
    protected void undo() {
        //You either have the file or you don't,
        //in the first case it is already undone, in the second the operation succeeded,
        //there's nothing to undo
    }

    private boolean deleteFile() throws IOException {
        boolean wasDeleted = true;

        if (file.isOtgFile()) {
            DocumentFile documentFile = OTGUtil.getDocumentFile(file.getPath(), context, false);
            wasDeleted = documentFile.delete();
        } else if (file.isDropBoxFile()) {
            CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
            cloudStorageDropbox.delete(CloudUtil.stripPath(OpenMode.DROPBOX, file.getPath()));
        } else if (file.isBoxFile()) {
            CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
            cloudStorageBox.delete(CloudUtil.stripPath(OpenMode.BOX, file.getPath()));
        } else if (file.isGoogleDriveFile()) {
            CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);
            cloudStorageGdrive.delete(CloudUtil.stripPath(OpenMode.GDRIVE, file.getPath()));
        } else if (file.isOneDriveFile()) {
            CloudStorage cloudStorageOnedrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
            cloudStorageOnedrive.delete(CloudUtil.stripPath(OpenMode.ONEDRIVE, file.getPath()));
        } else {
            try {
                wasDeleted = file.delete(context, rootMode);
            } catch (ShellNotRunningException e) {
                throw new ShellNotRunningIOException(e);
            }
        }

        // delete file from media database
        if (!file.isSmb()) {
            delete(context, file.getPath());
        }

        // delete file entry from encrypted database
        if (file.getName().endsWith(CryptUtil.CRYPT_EXTENSION)) {
            CryptHandler handler = new CryptHandler(context);
            handler.clear(file.getPath());
        }


        return wasDeleted;
    }

    private static void delete(final Context context, final String file) {
        final String where = MediaStore.MediaColumns.DATA + "=?";
        final String[] selectionArgs = new String[]{
                file
        };
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri filesUri = MediaStore.Files.getContentUri("external");
        // Delete the entry from the media database. This will actually delete media files.
        contentResolver.delete(filesUri, where, selectionArgs);
    }
}
