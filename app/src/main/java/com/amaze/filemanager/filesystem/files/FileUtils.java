/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.filesystem.files;

import static com.amaze.filemanager.filesystem.EditableFileAbstraction.Scheme.CONTENT;

import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.fileoperations.filesystem.OpenMode;
import com.amaze.filemanager.fileoperations.filesystem.smbstreamer.Streamer;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.ui.activities.DatabaseViewerActivity;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.activities.superclasses.PermissionsActivity;
import com.amaze.filemanager.ui.activities.superclasses.PreferenceActivity;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.dialogs.OpenFileDialogFragment;
import com.amaze.filemanager.ui.dialogs.share.ShareTask;
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnProgressUpdate;
import com.amaze.filemanager.utils.PackageInstallValidation;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.CloudMetaData;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.voidvalue.VoidValue;
import com.openmobilehub.android.storage.core.model.OmhStorageMetadata;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.documentfile.provider.DocumentFile;

import jcifs.smb.SmbFile;
import kotlin.collections.ArraysKt;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;

/** Functions that deal with files */
public class FileUtils {

  private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

  private static final String[] COMPRESSED_FILE_EXTENSIONS =
      new String[] {
        "zip", "rar", "cab", "bz2", "ace", "bz", "gz", "7z", "jar", "apk", "xz", "lzma", "Z"
      };

  public static final String FILE_PROVIDER_PREFIX = "storage_root";
  public static final String NOMEDIA_FILE = ".nomedia";
  public static final String DUMMY_FILE = ".DummyFile";

  public static long folderSize(File directory, OnProgressUpdate<Long> updateState) {
    long length = 0;
    try {
      for (File file : directory.listFiles()) {
        if (file.isFile()) length += file.length();
        else
          length +=
              folderSize(file, null); // null because updateState would be called for children dirs

        if (updateState != null) updateState.onUpdate(length);
      }
    } catch (Exception e) {
      LOG.warn("failed to get folder size", e);
    }
    return length;
  }

  public static long folderSize(HybridFile directory, OnProgressUpdate<Long> updateState) {
    if (directory.isSimpleFile()) return folderSize(new File(directory.getPath()), updateState);
    else return directory.folderSize(AppConfig.getInstance());
  }

  public static long folderSize(SmbFile directory) {
    long length = 0;
    try {
      for (SmbFile file : directory.listFiles()) {

        if (file.isFile()) length += file.length();
        else length += folderSize(file);
      }
    } catch (Exception e) {
      LOG.warn("failed to get folder size", e);
    }
    return length;
  }

  /**
   * Use recursive <code>ls</code> to get folder size.
   *
   * <p>It is slow, it is stupid, and may be inaccurate (because of permission problems). Only for
   * fallback use when <code>du</code> is not available.
   *
   * @see HybridFile#folderSize(Context)
   * @return Folder size in bytes
   */
  public static Long folderSizeSftp(SFTPClient client, String remotePath) {
    Long retval = 0L;
    try {
      for (RemoteResourceInfo info : client.ls(remotePath)) {
        if (info.isDirectory()) retval += folderSizeSftp(client, info.getPath());
        else retval += info.getAttributes().getSize();
      }
    } catch (SFTPException e) {
      // Usually happens when permission denied listing files in directory
      LOG.error("folderSizeSftp", "Problem accessing " + remotePath, e);
    } finally {
      return retval;
    }
  }

  public static long folderSizeCloud(OpenMode openMode, OmhStorageMetadata sourceFileMeta) {

    DataUtils dataUtils = DataUtils.getInstance();
    long length = 0;
    CloudStorage cloudStorage = dataUtils.getAccount(openMode);
    for (CloudMetaData metaData :
        cloudStorage.getChildren(CloudUtil.stripPath(openMode, sourceFileMeta.getPath()))) {

      if (metaData.getFolder()) {
        length += folderSizeCloud(openMode, metaData);
      } else {
        length += metaData.getSize();
      }
    }

    return length;
  }

  /** Helper method to get size of an otg folder */
  public static long otgFolderSize(String path, final Context context) {
    final AtomicLong totalBytes = new AtomicLong(0);
    OTGUtil.getDocumentFiles(
        path, context, file -> totalBytes.addAndGet(getBaseFileSize(file, context)));
    return totalBytes.longValue();
  }

  /** Helper method to calculate source files size */
  public static long getTotalBytes(ArrayList<HybridFileParcelable> files, Context context) {
    long totalBytes = 0L;
    for (HybridFileParcelable file : files) {
      totalBytes += getBaseFileSize(file, context);
    }
    return totalBytes;
  }

  public static long getBaseFileSize(HybridFileParcelable baseFile, Context context) {
    if (baseFile.isDirectory(context)) {
      return baseFile.folderSize(context);
    } else {
      return baseFile.length(context);
    }
  }

  public static void crossfade(View buttons, final View pathbar) {
    // Set the content view to 0% opacity but visible, so that it is visible
    // (but fully transparent) during the animation.
    buttons.setAlpha(0f);
    buttons.setVisibility(View.VISIBLE);

    // Animate the content view to 100% opacity, and clear any animation
    // listener set on the view.
    buttons.animate().alpha(1f).setDuration(100).setListener(null);
    pathbar
        .animate()
        .alpha(0f)
        .setDuration(100)
        .setListener(
            new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                pathbar.setVisibility(View.GONE);
              }
            });
    // Animate the loading view to 0% opacity. After the animation ends,
    // set its visibility to GONE as an optimization step (it won't
    // participate in layout passes, etc.)
  }

  public static void crossfadeInverse(final View buttons, final View pathbar) {
    // Set the content view to 0% opacity but visible, so that it is visible
    // (but fully transparent) during the animation.

    pathbar.setAlpha(0f);
    pathbar.setVisibility(View.VISIBLE);

    // Animate the content view to 100% opacity, and clear any animation
    // listener set on the view.
    pathbar.animate().alpha(1f).setDuration(500).setListener(null);
    buttons
        .animate()
        .alpha(0f)
        .setDuration(500)
        .setListener(
            new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                buttons.setVisibility(View.GONE);
              }
            });
    // Animate the loading view to 0% opacity. After the animation ends,
    // set its visibility to GONE as an optimization step (it won't
    // participate in layout passes, etc.)
  }

  public static void shareCloudFile(String path, final OpenMode openMode, final Context context) {
    new AsyncTask<String, Void, String>() {

      @Override
      protected String doInBackground(String... params) {
        String shareFilePath = params[0];
        CloudStorage cloudStorage = DataUtils.getInstance().getAccount(openMode);
        return cloudStorage.createShareLink(CloudUtil.stripPath(openMode, shareFilePath));
      }

      @Override
      protected void onPostExecute(String s) {
        super.onPostExecute(s);

        FileUtils.copyToClipboard(context, s);
        Toast.makeText(context, context.getString(R.string.cloud_share_copied), Toast.LENGTH_LONG)
            .show();
      }
    }.execute(path);
  }

  public static void shareCloudFiles(
      ArrayList<LayoutElementParcelable> files, final OpenMode openMode, final Context context) {
    String[] paths = new String[files.size()];
    for (int i = 0; i < files.size(); i++) {
      paths[i] = files.get(i).desc;
    }
    new AsyncTask<String, Void, String>() {
      @Override
      protected String doInBackground(String... params) {
        CloudStorage cloudStorage = DataUtils.getInstance().getAccount(openMode);
        StringBuilder links = new StringBuilder();
        links.append(cloudStorage.createShareLink(CloudUtil.stripPath(openMode, params[0])));
        for (int i = 1; i < params.length; i++) {
          links.append('\n');
          links.append(cloudStorage.createShareLink(CloudUtil.stripPath(openMode, params[i])));
        }
        return links.toString();
      }

      @Override
      protected void onPostExecute(String s) {
        super.onPostExecute(s);

        FileUtils.copyToClipboard(context, s);
        Toast.makeText(context, context.getString(R.string.cloud_share_copied), Toast.LENGTH_LONG)
            .show();
      }
    }.execute(paths);
  }

  public static void shareFiles(
      ArrayList<File> files, Activity activity, AppTheme appTheme, int fab_skin) {

    ArrayList<Uri> uris = new ArrayList<>();
    boolean isGenericFileType = false;

    String mime =
        files.size() > 1
            ? MimeTypes.getMimeType(files.get(0).getPath(), files.get(0).isDirectory())
            : null;

    for (File f : files) {
      uris.add(FileProvider.getUriForFile(activity, activity.getPackageName(), f));
      if (!isGenericFileType
          && (mime == null || !mime.equals(MimeTypes.getMimeType(f.getPath(), f.isDirectory())))) {
        isGenericFileType = true;
      }
    }

    if (isGenericFileType || mime == null) mime = MimeTypes.ALL_MIME_TYPES;

    try {
      new ShareTask(activity, uris, appTheme, fab_skin).execute(mime);
    } catch (Exception e) {
      LOG.warn("failed to get share files", e);
    }
  }

  public static float readableFileSizeFloat(long size) {
    if (size <= 0) return 0;
    return (float) (size / (1024 * 1024));
  }

  /**
   * Install .apk file.
   *
   * @param permissionsActivity needed to ask for {@link
   *     Manifest.permission#REQUEST_INSTALL_PACKAGES} permission
   */
  public static void installApk(
      final @NonNull File f, final @NonNull PermissionsActivity permissionsActivity) {

    try {
      PackageInstallValidation.validatePackageInstallability(f);
    } catch (PackageInstallValidation.PackageCannotBeInstalledException e) {
      Toast.makeText(
              permissionsActivity,
              R.string.error_google_play_cannot_update_myself,
              Toast.LENGTH_LONG)
          .show();
      return;
    } catch (IllegalStateException e) {
      Toast.makeText(
              permissionsActivity,
              permissionsActivity.getString(
                  R.string.error_cannot_get_package_info, f.getAbsolutePath()),
              Toast.LENGTH_LONG)
          .show();
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        && !permissionsActivity.getPackageManager().canRequestPackageInstalls()) {
      permissionsActivity.requestInstallApkPermission(
          () -> installApk(f, permissionsActivity), true);
    }

    Intent intent = new Intent(Intent.ACTION_VIEW);
    String type = "application/vnd.android.package-archive";

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      Uri downloadedApk =
          FileProvider.getUriForFile(
              permissionsActivity.getApplicationContext(), permissionsActivity.getPackageName(), f);
      intent.setDataAndType(downloadedApk, type);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } else {
      intent.setDataAndType(Uri.fromFile(f), type);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    try {
      permissionsActivity.startActivity(intent);
    } catch (Exception e) {
      LOG.warn("failed to install apk", e);
      Toast.makeText(permissionsActivity, R.string.failed_install_apk, Toast.LENGTH_SHORT).show();
    }
  }

  private static void openUnknownInternal(
      Uri contentUri, String type, MainActivity c, boolean forcechooser, boolean useNewStack) {
    Intent chooserIntent = new Intent();
    chooserIntent.setAction(Intent.ACTION_VIEW);
    chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    if (type != null && type.trim().length() != 0 && !type.equals(MimeTypes.ALL_MIME_TYPES)) {
      chooserIntent.setDataAndType(contentUri, type);
      Intent activityIntent;
      if (forcechooser) {
        if (useNewStack) applyNewDocFlag(chooserIntent);
        activityIntent = Intent.createChooser(chooserIntent, c.getString(R.string.open_with));
      } else {
        activityIntent = chooserIntent;
        if (useNewStack) applyNewDocFlag(chooserIntent);
      }

      try {
        c.startActivity(activityIntent);
      } catch (ActivityNotFoundException e) {
        LOG.error(e.getMessage(), e);
        Toast.makeText(c, R.string.no_app_found, Toast.LENGTH_SHORT).show();
        openWith(contentUri, c, useNewStack);
      }
    } else {
      openWith(contentUri, c, useNewStack);
    }
  }

  private static void applyNewDocFlag(Intent i) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
    } else {
      i.setFlags(
          Intent.FLAG_ACTIVITY_NEW_TASK
              | Intent.FLAG_ACTIVITY_CLEAR_TASK
              | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
    }
  }

  /** Method supports showing a UI to ask user to open a file without any extension/mime */
  public static void openWith(
      final File f, final PreferenceActivity activity, final boolean useNewStack) {
    openWith(
        FileProvider.getUriForFile(activity, activity.getPackageName(), f), activity, useNewStack);
  }

  public static void openWith(
      final DocumentFile f, final PreferenceActivity activity, final boolean useNewStack) {
    openWith(f.getUri(), activity, useNewStack);
  }

  public static void openWith(
      final Uri uri, final PreferenceActivity activity, final boolean useNewStack) {
    MaterialDialog.Builder a = new MaterialDialog.Builder(activity);
    a.title(activity.getString(R.string.open_as));
    String[] items =
        new String[] {
          activity.getString(R.string.text),
          activity.getString(R.string.image),
          activity.getString(R.string.video),
          activity.getString(R.string.audio),
          activity.getString(R.string.database),
          activity.getString(R.string.other)
        };

    a.items(items)
        .itemsCallback(
            (materialDialog, view, i, charSequence) -> {
              String mimeType = null;
              Intent intent = null;

              switch (i) {
                case 0:
                  mimeType = "text/*";
                  break;
                case 1:
                  mimeType = "image/*";
                  break;
                case 2:
                  mimeType = "video/*";
                  break;
                case 3:
                  mimeType = "audio/*";
                  break;
                case 4:
                  intent = new Intent(activity, DatabaseViewerActivity.class);
                  intent.setAction(Intent.ACTION_VIEW);
                  intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS);
                  }
                  // DatabaseViewerActivity only accepts java.io.File paths, need to strip the URI
                  // to file's absolute path
                  intent.putExtra(
                      "path",
                      uri.getPath()
                          .substring(
                              uri.getPath().indexOf(FILE_PROVIDER_PREFIX) - 1,
                              FILE_PROVIDER_PREFIX.length() + 1));
                  break;
                case 5:
                  mimeType = MimeTypes.getMimeType(uri.getPath(), false);
                  if (mimeType == null) mimeType = MimeTypes.ALL_MIME_TYPES;
                  break;
              }
              try {
                if (intent != null) {
                  activity.startActivity(intent);
                } else {
                  OpenFileDialogFragment.Companion.openFileOrShow(
                      uri, mimeType, useNewStack, activity, true);
                }
              } catch (Exception e) {
                Toast.makeText(activity, R.string.no_app_found, Toast.LENGTH_SHORT).show();
                openWith(uri, activity, useNewStack);
              }
            });

    a.build().show();
  }

  /** Method determines if there is something to go back to */
  public static boolean canGoBack(Context context, HybridFile currentFile) {
    switch (currentFile.getMode()) {
        // we're on main thread and can't list the cloud files
      case DROPBOX:
      case BOX:
      case GDRIVE:
      case ONEDRIVE:
      case OTG:
      case SFTP:
        return true;
      default:
        return true; // TODO: 29/9/2017 there might be nothing to go back to (check parent)
    }
  }

  public static long[] getSpaces(
      HybridFile hFile, Context context, final OnProgressUpdate<Long[]> updateState) {
    long totalSpace = hFile.getTotal(context);
    long freeSpace = hFile.getUsableSpace();
    long fileSize = 0l;

    if (hFile.isDirectory(context)) {
      fileSize = hFile.folderSize(context);
    } else {
      fileSize = hFile.length(context);
    }
    return new long[] {totalSpace, freeSpace, fileSize};
  }

  public static boolean copyToClipboard(Context context, String text) {
    try {
      android.content.ClipboardManager clipboard =
          (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
      android.content.ClipData clip =
          android.content.ClipData.newPlainText(
              context.getString(R.string.clipboard_path_copy), text);
      clipboard.setPrimaryClip(clip);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static String[] getFolderNamesInPath(String path) {
    if (!path.endsWith("/")) path += "/";
    @Nullable Pair<String, String> splitUri = splitUri(path);
    if (splitUri != null) {
      path = splitUri.second;
    }
    return ("root" + path).split("/");
  }

  /**
   * Parse a given path to a string array of the &quot;steps&quot; to target.
   *
   * <p>For local paths, output will be like <code>
   * ["/", "/storage", "/storage/emulated", "/storage/emulated/0", "/storage/emulated/0/Download", "/storage/emulated/0/Download/file.zip"]
   * </code> For URI paths, output will be like <code>
   * ["smb://user;workgroup:passw0rd@12.3.4", "smb://user;workgroup:passw0rd@12.3.4/user", "smb://user;workgroup:passw0rd@12.3.4/user/Documents", "smb://user;workgroup:passw0rd@12.3.4/user/Documents/flare.doc"]
   * </code>
   *
   * @param path
   * @return string array of incremental path segments
   */
  public static String[] getPathsInPath(String path) {
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    path = path.trim();

    ArrayList<String> paths = new ArrayList<>();
    @Nullable String urlPrefix = null;
    @Nullable Pair<String, String> splitUri = splitUri(path);
    if (splitUri != null) {
      urlPrefix = splitUri.first;
      path = splitUri.second;
    }

    if (!path.startsWith("/")) {
      path = "/" + path;
    }

    while (path.length() > 0) {
      if (urlPrefix != null) {
        paths.add(urlPrefix + path);
      } else {
        paths.add(path);
      }
      if (path.contains("/")) {
        path = path.substring(0, path.lastIndexOf('/'));
      } else {
        break;
      }
    }

    if (urlPrefix != null) {
      paths.add(urlPrefix);
    } else {
      paths.add("/");
    }
    Collections.reverse(paths);

    return paths.toArray(new String[0]);
  }

  /**
   * Splits a given path to URI prefix (if exists) and path.
   *
   * @param path
   * @return {@link Pair} tuple if given path is URI (scheme is not null). Tuple contains:
   *     <ul>
   *       <li>First: URI section of the given path, if given path is an URI
   *       <li>Second: Path section of the given path. Never null
   *     </ul>
   */
  public static @Nullable Pair<String, String> splitUri(@NonNull final String path) {
    Uri uri = Uri.parse(path);
    if (uri.getScheme() != null) {
      String urlPrefix = uri.getScheme() + "://" + uri.getEncodedAuthority();
      String retPath = path.substring(urlPrefix.length());
      return new Pair<>(urlPrefix, retPath);
    } else {
      return null;
    }
  }

  public static boolean canListFiles(File f) {
    return f.canRead() && f.isDirectory();
  }

  public static void openFile(
      @NonNull final File f,
      @NonNull final MainActivity mainActivity,
      @NonNull final SharedPreferences sharedPrefs) {
    boolean useNewStack =
        sharedPrefs.getBoolean(PreferencesConstants.PREFERENCE_TEXTEDITOR_NEWSTACK, false);
    boolean defaultHandler = isSelfDefault(f, mainActivity);

    if (f.getName().toLowerCase().endsWith(".apk")) {
      GeneralDialogCreation.showPackageDialog(f, mainActivity);
    } else if (defaultHandler && CompressedHelper.isFileExtractable(f.getPath())) {
      GeneralDialogCreation.showArchiveDialog(f, mainActivity);
    } else if (defaultHandler && f.getName().toLowerCase().endsWith(".db")) {
      Intent intent = new Intent(mainActivity, DatabaseViewerActivity.class);
      intent.setType(MimeTypes.getMimeType(f.getPath(), false));
      intent.putExtra("path", f.getPath());
      mainActivity.startActivity(intent);
    } else {
      try {
        openFileDialogFragmentFor(f, mainActivity, useNewStack);
      } catch (Exception e) {
        Toast.makeText(
                mainActivity, mainActivity.getString(R.string.no_app_found), Toast.LENGTH_LONG)
            .show();
        openWith(f, mainActivity, useNewStack);
      }
    }
  }

  private static void openFileDialogFragmentFor(
      @NonNull File file, @NonNull MainActivity mainActivity, @NonNull Boolean useNewStack) {
    openFileDialogFragmentFor(
        file, mainActivity, MimeTypes.getMimeType(file.getAbsolutePath(), false), useNewStack);
  }

  private static void openFileDialogFragmentFor(
      @NonNull File file,
      @NonNull MainActivity mainActivity,
      @NonNull String mimeType,
      @NonNull Boolean useNewStack) {
    OpenFileDialogFragment.Companion.openFileOrShow(
        FileProvider.getUriForFile(mainActivity, mainActivity.getPackageName(), file),
        mimeType,
        useNewStack,
        mainActivity,
        false);
  }

  private static void openFileDialogFragmentFor(
      @NonNull DocumentFile file,
      @NonNull MainActivity mainActivity,
      @NonNull Boolean useNewStack) {
    openFileDialogFragmentFor(
        file.getUri(),
        mainActivity,
        MimeTypes.getMimeType(file.getUri().toString(), false),
        useNewStack);
  }

  private static void openFileDialogFragmentFor(
      @NonNull Uri uri,
      @NonNull MainActivity mainActivity,
      @NonNull String mimeType,
      @NonNull Boolean useNewStack) {
    OpenFileDialogFragment.Companion.openFileOrShow(
        uri, mimeType, useNewStack, mainActivity, false);
  }

  private static boolean isSelfDefault(File f, Context c) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(Uri.fromFile(f), MimeTypes.getMimeType(f.getPath(), f.isDirectory()));
    ResolveInfo info =
        c.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
    if (info != null && info.activityInfo != null) {
      return info.activityInfo.packageName.equals(c.getPackageName());
    } else {
      return true;
    }
  }

  /** Support file opening for {@link DocumentFile} (eg. OTG) */
  public static void openFile(
      final DocumentFile f, final MainActivity m, SharedPreferences sharedPrefs) {
    boolean useNewStack =
        sharedPrefs.getBoolean(PreferencesConstants.PREFERENCE_TEXTEDITOR_NEWSTACK, false);
    try {
      openFileDialogFragmentFor(f, m, useNewStack);
    } catch (Exception e) {
      Toast.makeText(m, m.getString(R.string.no_app_found), Toast.LENGTH_LONG).show();
      openWith(f, m, useNewStack);
    }
  }

  public static void launchSMB(final HybridFile baseFile, final Activity activity) {
    final Streamer s = Streamer.getInstance();
    new Thread() {
      public void run() {
        try {
          /*
          List<SmbFile> subtitleFiles = new ArrayList<SmbFile>();

          // finding subtitles
          for (Layoutelements layoutelement : LIST_ELEMENTS) {
              SmbFile smbFile = new SmbFile(layoutelement.getDesc());
              if (smbFile.getName().contains(smbFile.getName())) subtitleFiles.add(smbFile);
          }
          */

          s.setStreamSrc(baseFile.getSmbFile(), baseFile.length(activity));
          activity.runOnUiThread(
              () -> {
                try {
                  Uri uri =
                      Uri.parse(
                          Streamer.URL
                              + Uri.fromFile(new File(Uri.parse(baseFile.getPath()).getPath()))
                                  .getEncodedPath());
                  Intent i = new Intent(Intent.ACTION_VIEW);
                  i.setDataAndType(
                      uri,
                      MimeTypes.getMimeType(baseFile.getPath(), baseFile.isDirectory(activity)));
                  PackageManager packageManager = activity.getPackageManager();
                  List<ResolveInfo> resInfos = packageManager.queryIntentActivities(i, 0);
                  if (resInfos != null && resInfos.size() > 0) activity.startActivity(i);
                  else
                    Toast.makeText(
                            activity,
                            activity.getResources().getString(R.string.smb_launch_error),
                            Toast.LENGTH_SHORT)
                        .show();
                } catch (ActivityNotFoundException e) {
                  LOG.warn("Failed to launch smb file due to no activity", e);
                }
              });

        } catch (Exception e) {
          LOG.warn("failed to launch smb file", e);
        }
      }
    }.start();
  }

  public static ArrayList<HybridFile> toHybridFileConcurrentRadixTree(
      ConcurrentRadixTree<VoidValue> a) {
    ArrayList<HybridFile> b = new ArrayList<>();
    for (CharSequence o : a.getKeysStartingWith("")) {
      HybridFile hFile = new HybridFile(OpenMode.UNKNOWN, o.toString());
      hFile.generateMode(null);
      b.add(hFile);
    }
    return b;
  }

  public static ArrayList<HybridFile> toHybridFileArrayList(LinkedList<String> a) {
    ArrayList<HybridFile> b = new ArrayList<>();
    for (String s : a) {
      HybridFile hFile = new HybridFile(OpenMode.UNKNOWN, s);
      hFile.generateMode(null);
      b.add(hFile);
    }
    return b;
  }

  /**
   * We're parsing a line returned from a stdout of shell.
   *
   * @param line must be the line returned from 'ls' or 'stat' command
   */
  public static HybridFileParcelable parseName(String line, boolean isStat) {
    boolean linked = false;
    StringBuilder name = new StringBuilder();
    StringBuilder link = new StringBuilder();
    String size = "-1";
    String date = "";
    String[] array = line.split(" +");
    if (array.length < 6) return null;
    for (String anArray : array) {
      if (anArray.contains("->") && array[0].startsWith("l")) {
        linked = true;
        break;
      }
    }
    int p = getColonPosition(array);
    if (p != -1 && (p + 1) != array.length) {
      date = array[p - 1] + " | " + array[p];
      size = array[p - 2];
    } else if (isStat) {
      date = array[5];
      size = array[4];
      p = 5;
    }
    if (!linked) {
      for (int i = p + 1; i < array.length; i++) {
        name.append(" ").append(array[i]);
      }
      name = new StringBuilder(name.toString().trim());
    } else {
      int q = getLinkPosition(array);
      for (int i = p + 1; i < q; i++) {
        name.append(" ").append(array[i]);
      }
      // Newer *boxes may introduce full path during stat. Trim down to the very last /
      if (name.lastIndexOf("/") > 0) {
        name.delete(0, name.lastIndexOf("/") + 1);
      }
      name = new StringBuilder(name.toString().trim());
      for (int i = q + 1; i < array.length; i++) {
        link.append(" ").append(array[i]);
      }
      link = new StringBuilder(link.toString().trim());
    }
    long Size;
    if (size == null || size.trim().length() == 0) {
      Size = -1;
    } else {
      try {
        Size = Long.parseLong(size);
      } catch (NumberFormatException ifItIsNotANumber) {
        Size = -1;
      }
    }
    if (date.trim().length() > 0 && !isStat) {
      ParsePosition pos = new ParsePosition(0);
      SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd | HH:mm", Locale.US);
      Date stringDate = simpledateformat.parse(date, pos);
      if (stringDate == null) {
        LOG.warn("parseName: unable to parse datetime string [" + date + "]");
      }
      HybridFileParcelable baseFile =
          new HybridFileParcelable(
              name.toString(), array[0], stringDate != null ? stringDate.getTime() : 0, Size, true);
      baseFile.setLink(link.toString());
      return baseFile;
    } else if (isStat) {
      HybridFileParcelable baseFile =
          new HybridFileParcelable(
              name.toString(), array[0], Long.parseLong(date) * 1000, Size, true);
      baseFile.setLink(link.toString());
      return baseFile;
    } else {
      HybridFileParcelable baseFile =
          new HybridFileParcelable(
              name.toString(), array[0], new File("/").lastModified(), Size, true);
      baseFile.setLink(link.toString());
      return baseFile;
    }
  }

  private static int getLinkPosition(String[] array) {
    for (int i = 0; i < array.length; i++) {
      if (array[i].contains("->")) return i;
    }
    return 0;
  }

  private static int getColonPosition(String[] array) {
    for (int i = 0; i < array.length; i++) {
      if (array[i].contains(":")) return i;
    }
    return -1;
  }

  public static ArrayList<Boolean[]> parse(String permLine) {
    ArrayList<Boolean[]> arrayList = new ArrayList<>(3);
    Boolean[] read =
        new Boolean[] {
          permLine.charAt(1) == 'r', permLine.charAt(4) == 'r', permLine.charAt(7) == 'r'
        };

    Boolean[] write =
        new Boolean[] {
          permLine.charAt(2) == 'w', permLine.charAt(5) == 'w', permLine.charAt(8) == 'w'
        };

    Boolean[] execute =
        new Boolean[] {
          permLine.charAt(3) == 'x', permLine.charAt(6) == 'x', permLine.charAt(9) == 'x'
        };

    arrayList.add(read);
    arrayList.add(write);
    arrayList.add(execute);
    return arrayList;
  }

  public static boolean isStorage(String path) {
    for (String s : DataUtils.getInstance().getStorages()) if (s.equals(path)) return true;
    return false;
  }

  public static boolean isPathAccessible(String dir, SharedPreferences pref) {
    File f = new File(dir);
    boolean showIfHidden = pref.getBoolean(PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES, false),
        isDirSelfOrParent = dir.endsWith("/.") || dir.endsWith("/.."),
        showIfRoot = pref.getBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, false);

    return f.exists()
        && f.isDirectory()
        && (!f.isHidden() || (showIfHidden && !isDirSelfOrParent))
        && (!isRoot(dir) || showIfRoot);

    // TODO: 2/5/2017 use another system that doesn't create new object
  }

  public static boolean isRoot(
      String dir) { // TODO: 5/5/2017 hardcoding root might lead to problems down the line
    return !dir.contains(OTGUtil.PREFIX_OTG)
        && !dir.startsWith(OTGUtil.PREFIX_MEDIA_REMOVABLE)
        && !dir.startsWith("/storage");
  }

  /** Convenience method to return if a path points to a compressed file. */
  public static boolean isCompressedFile(String path) {
    @Nullable String extension = MimeTypes.getExtension(path);
    return ArraysKt.indexOf(COMPRESSED_FILE_EXTENSIONS, extension) > -1;
  }

  /** Converts ArrayList of HybridFileParcelable to ArrayList of File */
  public static ArrayList<File> hybridListToFileArrayList(ArrayList<HybridFileParcelable> a) {
    ArrayList<File> b = new ArrayList<>();
    for (int i = 0; i < a.size(); i++) {
      b.add(new File(a.get(i).getPath()));
    }
    return b;
  }

  /** Checks whether path for bookmark exists If path is not found, empty directory is created */
  public static void checkForPath(Context context, String path, boolean isRootExplorer) {
    // TODO: Add support for SMB and OTG in this function
    if (!new File(path).exists()) {
      Toast.makeText(context, context.getString(R.string.bookmark_lost), Toast.LENGTH_SHORT).show();
      Operations.mkdir(
          new HybridFile(OpenMode.FILE, path),
          RootHelper.generateBaseFile(new File(path), true),
          context,
          isRootExplorer,
          new Operations.ErrorCallBack() {
            // TODO empty
            @Override
            public void exists(HybridFile file) {}

            @Override
            public void launchSAF(HybridFile file) {}

            @Override
            public void launchSAF(HybridFile file, HybridFile file1) {}

            @Override
            public void done(HybridFile hFile, boolean b) {}

            @Override
            public void invalidName(HybridFile file) {}
          });
    }
  }

  public static File fromContentUri(@NonNull Uri uri) {
    if (!CONTENT.name().equalsIgnoreCase(uri.getScheme())) {
      LOG.warn("URI must start with content://. URI was [" + uri + "]");
    }
    File pathFile = new File(uri.getPath().substring(FILE_PROVIDER_PREFIX.length() + 1));
    if (!pathFile.exists()) {
      LOG.warn("Failed to navigate to the initial path: {}", pathFile.getPath());
      pathFile = new File(uri.getPath());
      LOG.warn("Attempting to navigate to the fallback path: {}", pathFile.getPath());
    }
    return pathFile;
  }

  /**
   * Uninstalls a given package
   *
   * @param pkg packge
   * @param context context
   * @return success
   */
  public static boolean uninstallPackage(String pkg, Context context) {
    try {
      Intent intent = new Intent(Intent.ACTION_DELETE);
      intent.setData(Uri.parse("package:" + pkg));
      context.startActivity(intent);
    } catch (Exception e) {
      Toast.makeText(context, "" + e, Toast.LENGTH_SHORT).show();
      LOG.warn("failed to uninstall apk", e);
      return false;
    }
    return true;
  }

  /** Determines the specified path is beyond storage level, i.e should require root access. */
  @SuppressWarnings("PMD.DoNotHardCodeSDCard")
  public static boolean isRunningAboveStorage(@NonNull String path) {
    return !path.startsWith("/storage") && !path.startsWith("/sdcard");
  }
}
