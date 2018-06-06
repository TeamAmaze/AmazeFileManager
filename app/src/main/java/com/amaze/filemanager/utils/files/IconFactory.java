package com.amaze.filemanager.utils.files;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import com.amaze.filemanager.ui.icons.Icons;

public class IconFactory {
    public Uri getUriInstance(int mimeType, String volume) {
        switch (mimeType) {
            case Icons.IMAGE:
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            case Icons.VIDEO:
                return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            case Icons.AUDIO:
                return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            default:
                return MediaStore.Files.getContentUri(volume);

        }
    }

    public String[] getProjectionInstance(int mimeType) {
        switch (mimeType) {
            case Icons.IMAGE:
            case Icons.VIDEO:
            case Icons.AUDIO:
                return new String[]{BaseColumns._ID};
            default:
                return new String[]{BaseColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE};
        }
    }
}
