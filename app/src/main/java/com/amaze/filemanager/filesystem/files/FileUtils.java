/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import java.util.concurrent.atomic.AtomicLong;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.ExternalSdCardOperation;
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
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnProgressUpdate;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.CloudMetaData;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.voidvalue.VoidValue;

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
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
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

  private static final String TAG = FileUtils.class.getSimpleName();

  private static final String[] COMPRESSED_FILE_EXTENSIONS =
      new String[] {"zip", "cab", "bz2", "ace", "bz", "gz", "7z", "jar", "apk", "xz", "lzma", "Z"};

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
      e.printStackTrace();
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
      e.printStackTrace();
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
      Log.e("folderSizeSftp", "Problem accessing " + remotePath, e);
    } finally {
      return retval;
    }
  }

  public static long folderSizeCloud(OpenMode openMode, CloudMetaData sourceFileMeta) {

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

  /**
   * Triggers media scanner for multiple paths. The paths must all belong to same filesystem. It's
   * upto the caller to call the mediastore scan on multiple files or only one source/target
   * directory. Don't use filesystem API directly as files might not be present anymore (eg.
   * move/rename) which may lead to {@link java.io.FileNotFoundException}
   *
   * @param hybridFiles
   * @param context
   */
  public static void scanFile(@NonNull Context context, @NonNull HybridFile[] hybridFiles) {
    AsyncTask.execute(
        () -> {
          if (hybridFiles[0].exists(context) && hybridFiles[0].isLocal()) {
            String[] paths = new String[hybridFiles.length];
            for (int i = 0; i < hybridFiles.length; i++) {
              HybridFile hybridFile = hybridFiles[i];
              paths[i] = hybridFile.getPath();
            }
            MediaScannerConnection.scanFile(context, paths, null, null);
          }
          for (HybridFile hybridFile : hybridFiles) {
            scanFile(hybridFile, context);
          }
        });
  }

  /**
   * Triggers media store for the file path
   *
   * @param hybridFile the file which was changed (directory not supported)
   * @param context given context
   */
  private static void scanFile(@NonNull HybridFile hybridFile, Context context) {

    if ((hybridFile.isLocal() || hybridFile.isOtgFile()) && hybridFile.exists(context)) {

      Uri uri = null;
      if (Build.VERSION.SDK_INT >= 19) {
        DocumentFile documentFile =
            ExternalSdCardOperation.getDocumentFile(
                hybridFile.getFile(), hybridFile.isDirectory(context), context);
        // If FileUtil.getDocumentFile() returns null, fall back to DocumentFile.fromFile()
        if (documentFile == null) documentFile = DocumentFile.fromFile(hybridFile.getFile());
        uri = documentFile.getUri();
      } else {
        if (hybridFile.isLocal()) {
          uri = Uri.fromFile(hybridFile.getFile());
        }
      }
      if (uri != null) {
        FileUtils.scanFile(uri, context);
      }
    }
  }

  /**
   * Triggers {@link Intent#ACTION_MEDIA_SCANNER_SCAN_FILE} intent to refresh the media store.
   *
   * @param uri File's {@link Uri}
   * @param c {@link Context}
   */
  private static void scanFile(@NonNull Uri uri, @NonNull Context c) {
    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
    c.sendBroadcast(mediaScanIntent);
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

  public static void shareFiles(ArrayList<File> a, Activity c, AppTheme appTheme, int fab_skin) {

    ArrayList<Uri> uris = new ArrayList<>();
    boolean b = true;
    for (File f : a) {
      uris.add(FileProvider.getUriForFile(c, c.getPackageName(), f));
    }

    String mime = MimeTypes.getMimeType(a.get(0).getPath(), a.get(0).isDirectory());
    if (a.size() > 1)
      for (File f : a) {
        if (!mime.equals(MimeTypes.getMimeType(f.getPath(), f.isDirectory()))) {
          b = false;
        }
      }

    if (!b || mime == (null)) mime = MimeTypes.ALL_MIME_TYPES;
    try {

      new ShareTask(c, uris, appTheme, fab_skin).execute(mime);
    } catch (Exception e) {
      e.printStackTrace();
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
      e.printStackTrace();
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
        android.util.Log.e(TAG, e.getMessage(), e);
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
        openFileDialogFragmentFor(f, mainActivity);
      } catch (Exception e) {
        Toast.makeText(
                mainActivity, mainActivity.getString(R.string.no_app_found), Toast.LENGTH_LONG)
            .show();
        openWith(f, mainActivity, useNewStack);
      }
    }
  }

  private static void openFileDialogFragmentFor(
      @NonNull File file, @NonNull MainActivity mainActivity) {
    openFileDialogFragmentFor(
        file, mainActivity, MimeTypes.getMimeType(file.getAbsolutePath(), false));
  }

  private static void openFileDialogFragmentFor(
      @NonNull File file, @NonNull MainActivity mainActivity, @NonNull String mimeType) {
    OpenFileDialogFragment.Companion.openFileOrShow(
        FileProvider.getUriForFile(mainActivity, mainActivity.getPackageName(), file),
        mimeType,
        false,
        mainActivity,
        false);
  }

  private static void openFileDialogFragmentFor(
      @NonNull DocumentFile file, @NonNull MainActivity mainActivity) {
    openFileDialogFragmentFor(
        file.getUri(), mainActivity, MimeTypes.getMimeType(file.getUri().toString(), false));
  }

  private static void openFileDialogFragmentFor(
      @NonNull Uri uri, @NonNull MainActivity mainActivity, @NonNull String mimeType) {
    OpenFileDialogFragment.Companion.openFileOrShow(uri, mimeType, false, mainActivity, false);
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
      openFileDialogFragmentFor(f, m);
    } catch (Exception e) {
      Toast.makeText(m, m.getString(R.string.no_app_found), Toast.LENGTH_LONG).show();
      openWith(f, m, useNewStack);
    }
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
    String[] array = line.split(" ");
    if (array.length < 6) return null;
    for (String anArray : array) {
      if (anArray.contains("->") && array[0].startsWith("l")) {
        linked = true;
        break;
      }
    }
    int p = getColonPosition(array);
    if (p != -1) {
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
      name = new StringBuilder(name.toString().trim());
      for (int i = q + 1; i < array.length; i++) {
        link.append(" ").append(array[i]);
      }
      link = new StringBuilder(link.toString().trim());
    }
    long Size = (size == null || size.trim().length() == 0) ? -1 : Long.parseLong(size);
    if (date.trim().length() > 0 && !isStat) {
      ParsePosition pos = new ParsePosition(0);
      SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd | HH:mm");
      Date stringDate = simpledateformat.parse(date, pos);
      if (stringDate == null) {
        Log.w(TAG, "parseName: unable to parse datetime string [" + date + "]");
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
      throw new IllegalArgumentException(
          "URI must start with content://. URI was [" + uri.toString() + "]");
    } else {
      return new File(uri.getPath().substring(FILE_PROVIDER_PREFIX.length() + 1));
    }
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
      e.printStackTrace();
      return false;
    }
    return true;
  }
}
