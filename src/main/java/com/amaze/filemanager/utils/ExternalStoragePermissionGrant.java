/*
 * Copyright (C) 2015 JRummy Apps, Inc. - All Rights Reserved
 *
 * Unauthorized copying or redistribution of this file in source and binary forms via any medium
 * is strictly prohibited.
 *
 * Created by Jared Rummler <jared.rummler@gmail.com>, Mar 28, 2015
 */
package com.amaze.filemanager.utils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.provider.DocumentFile;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ExternalStoragePermissionGrant {

    public static final int GRANT_CODE = 44;

    private static volatile ExternalStoragePermissionGrant instance;

    private static final String TAG = "ExternalStoragePermissionGrant";

    public static ExternalStoragePermissionGrant getInstance(Context c) {
        if (instance == null) {
            synchronized (ExternalStoragePermissionGrant.class) {
                if (instance == null) {
                    instance = new ExternalStoragePermissionGrant(c);
                }
            }
        }
        return instance;
    }

    private WeakReference<Activity> mActivityRef;

    private final HashMap<String, DocumentFile> mCache;

    private final Context mContext;

    private final Handler mHandler;

    private final UriHolder mLock;

    private ExternalStoragePermissionGrant(Context context) {
        mLock = new UriHolder();
        mHandler = new Handler(Looper.getMainLooper());
        mCache = new HashMap<>();
        mContext = context;
    }

    private Uri checkPermission(final UriPermission uriPermission, final String string) {
        if (!uriPermission.isWritePermission()) {
            return null;
        }
        return checkUri(uriPermission.getUri(), string);
    }

    private Uri checkUri(final Uri uri, final String path) {
        final DocumentFile documentFile = DocumentFile.fromTreeUri(mContext, uri);
        if (documentFile == null) {
            return null;
        }
        final File file = new File(path);
        if (!file.getName().equals(documentFile.getName())) {
            return null;
        }
        if (file.lastModified() == documentFile.lastModified()) {
            return uri;
        }
        return null;
    }
    public  String getStorageDirectories(MainActivity m,String path) {
        for(int i=0;i<m.list.size()-m.booksize;i++){
            if(path.contains(m.list.get(i)))return m.list.get(i);


        }return m.list.get(0);
    }

    public DocumentFile findFile(final String path,MainActivity mainActivity) {
        final String mountPoint = getStorageDirectories(mainActivity,path);
        final DocumentFile documentFile = getDocumentRoot(mountPoint);
        if (documentFile == null) {
            return null;
        }
        if (path.equals(mountPoint)) {
            return documentFile;
        }
        DocumentFile documentFile2 = documentFile;
        final String[] filenames = path.substring(1 + mountPoint.length()).split("/");
        for (int i = 0, len = filenames.length; i < len; ++i) {
            if ((documentFile2 = documentFile2.findFile(filenames[i])) != null) {
                continue;
            }
            return null;
        }
        return documentFile2;
    }

    private Uri findGrantedUri(final String mountPoint) {
        final List<UriPermission> list = mContext.getContentResolver().getPersistedUriPermissions();
        final Iterator<UriPermission> iterator = list.iterator();
        while (iterator.hasNext()) {
            final Uri uri = checkPermission(iterator.next(), mountPoint);
            if (uri == null) {
                continue;
            }
            return uri;
        }
        return null;
    }

    public DocumentFile getDocumentRoot(final String mountPoint) {
        if (mCache.containsKey(mountPoint)) {
            return mCache.get(mountPoint);
        }
        final Uri grantedUri = findGrantedUri(mountPoint);
        if (grantedUri != null) {
            return persistAndCreateRoot(grantedUri, mountPoint);
        }
        int resId = R.string.sd_card_grant_first;
        do {
            mHandler.post(new DialogPrompt(resId));
            try {
                synchronized (mLock) {
                    mLock.wait();
                    if (mLock.uri == null) {
                        throw new InterruptedException();
                    }
                    final Uri checkedUri = checkUri(mLock.uri, mountPoint);
                    if (checkedUri != null) {
                        return persistAndCreateRoot(checkedUri, mountPoint);
                    }
                   resId = R.string.sd_card_grant_next;
                }
            } catch (final InterruptedException e) {
                return null;
            }
        } while (true);
    }

    public void onNewActivity(final Activity activity) {
        mActivityRef = new WeakReference<Activity>(activity);
    }

    public void onUriReceived(final Uri uri) {
        synchronized (mLock) {
            mLock.uri = uri;
            mLock.notify();
            return;
        }
    }

    DocumentFile persistAndCreateRoot(final Uri uri, final String path) {
        mContext.getContentResolver().takePersistableUriPermission(uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        final DocumentFile documentFile = DocumentFile.fromTreeUri(mContext, uri);
        mCache.put(path, documentFile);
        return documentFile;
    }

    public void startGrantActivity() {
        final Activity activity = mActivityRef.get();
        if (activity == null) {
            onUriReceived(null);
        }
        try {
            activity.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),
                GRANT_CODE);
        } catch (final ActivityNotFoundException e) {
            onUriReceived(null);

        }
    }

    class DialogPrompt implements Runnable {

        int message;

        public DialogPrompt(final int resId) {
            message = resId;
        }

        @Override
        public void run() {
            if (mActivityRef.get() == null) {
                onUriReceived(null);
                return;
            }
            new AlertDialog.Builder(mActivityRef.get())
                    .setTitle("external_storage_permission")
                    .setMessage(message)
                    .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                onUriReceived(null);
                                dialog.dismiss();
                            }
                        })
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            dialog.dismiss();
                            startGrantActivity();
                        }
                    }).show();
        }

    }

    class UriHolder {

        Uri uri;

        UriHolder() {
        }
    }
}
