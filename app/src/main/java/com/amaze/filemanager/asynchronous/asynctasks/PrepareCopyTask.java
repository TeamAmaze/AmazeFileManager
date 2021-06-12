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

package com.amaze.filemanager.asynchronous.asynctasks;

import static com.amaze.filemanager.file_operations.filesystem.FolderStateKt.CAN_CREATE_FILES;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.COPY;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.MOVE;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.asynchronous.services.CopyService;
import com.amaze.filemanager.databinding.CopyDialogBinding;
import com.amaze.filemanager.file_operations.filesystem.FolderState;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.utils.Utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.IntDef;

/**
 * Created by arpitkh996 on 12-01-2016, modified by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 *
 * <p>This AsyncTask works by creating a tree where each folder that can be fusioned together with
 * another in the destination is a node (CopyNode). While the tree is being created an indeterminate
 * ProgressDialog is shown. Each node is copied when the conflicts are dealt with (the dialog is
 * shown, and the tree is walked via a BFS). If the process is cancelled (via the button in the
 * dialog) the dialog closes without any more code to be executed, finishCopying() is never executed
 * so no changes are made.
 */
public class PrepareCopyTask
    extends AsyncTask<ArrayList<HybridFileParcelable>, String, PrepareCopyTask.CopyNode> {

  private final String path;
  private final Boolean move;
  private final WeakReference<MainActivity> mainActivity;
  private final WeakReference<Context> context;
  private int counter = 0;
  private ProgressDialog dialog;
  private boolean rootMode = false;
  private OpenMode openMode = OpenMode.FILE;
  private @DialogState int dialogState = UNKNOWN;
  private boolean isRenameMoveSupport = false;

  // causes folder containing filesToCopy to be deleted
  private ArrayList<File> deleteCopiedFolder = null;
  private CopyNode copyFolder;
  private final ArrayList<String> paths = new ArrayList<>();
  private final ArrayList<ArrayList<HybridFileParcelable>> filesToCopyPerFolder = new ArrayList<>();
  private ArrayList<HybridFileParcelable> filesToCopy; // a copy of params sent to this

  private static final int UNKNOWN = -1;
  private static final int DO_NOT_REPLACE = 0;
  private static final int REPLACE = 1;

  @IntDef({UNKNOWN, DO_NOT_REPLACE, REPLACE})
  @interface DialogState {}

  public PrepareCopyTask(
      String path, Boolean move, MainActivity con, boolean rootMode, OpenMode openMode) {
    this.move = move;
    mainActivity = new WeakReference<>(con);
    context = new WeakReference<>(con);
    this.openMode = openMode;
    this.rootMode = rootMode;

    this.path = path;
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();
    dialog =
        ProgressDialog.show(context.get(), "", context.get().getString(R.string.processing), true);
  }

  @Override
  public void onProgressUpdate(String... message) {
    Toast.makeText(context.get(), message[0], Toast.LENGTH_LONG).show();
  }

  @Override
  protected CopyNode doInBackground(ArrayList<HybridFileParcelable>... params) {
    filesToCopy = params[0];
    long totalBytes = 0;

    if (openMode == OpenMode.OTG
        || openMode == OpenMode.DROPBOX
        || openMode == OpenMode.BOX
        || openMode == OpenMode.GDRIVE
        || openMode == OpenMode.ONEDRIVE
        || openMode == OpenMode.ROOT) {
      // no helper method for OTG to determine storage space
      return null;
    }

    HybridFile destination = new HybridFile(openMode, path);
    destination.generateMode(context.get());

    if (move
        && destination.getMode() == openMode
        && MoveFiles.getOperationSupportedFileSystem().contains(openMode)) {
      // move/rename supported filesystems, skip checking for space
      isRenameMoveSupport = true;
    }

    totalBytes = FileUtils.getTotalBytes(filesToCopy, context.get());

    if (destination.getUsableSpace() < totalBytes && !isRenameMoveSupport) {
      publishProgress(context.get().getResources().getString(R.string.in_safe));
      return null;
    }

    copyFolder = new CopyNode(path, filesToCopy);

    return copyFolder;
  }

  private ArrayList<HybridFileParcelable> checkConflicts(
      final ArrayList<HybridFileParcelable> filesToCopy, HybridFile destination) {
    final ArrayList<HybridFileParcelable> conflictingFiles = new ArrayList<>();
    destination.forEachChildrenFile(
        context.get(),
        rootMode,
        file -> {
          for (HybridFileParcelable j : filesToCopy) {
            if (file.getName(context.get()).equals((j).getName(context.get()))) {
              conflictingFiles.add(j);
            }
          }
        });
    return conflictingFiles;
  }

  @Override
  protected void onPostExecute(CopyNode copyFolder) {
    super.onPostExecute(copyFolder);
    if (openMode == OpenMode.OTG
        || openMode == OpenMode.GDRIVE
        || openMode == OpenMode.DROPBOX
        || openMode == OpenMode.BOX
        || openMode == OpenMode.ONEDRIVE
        || openMode == OpenMode.ROOT) {

      startService(filesToCopy, path, openMode);
    } else {

      if (copyFolder == null) {
        // not starting service as there's no sufficient space
        dialog.dismiss();
        return;
      }

      onEndDialog(null, null, null);
    }

    dialog.dismiss();
  }

  private void startService(
      ArrayList<HybridFileParcelable> sourceFiles, String target, OpenMode openmode) {
    Intent intent = new Intent(context.get(), CopyService.class);
    intent.putParcelableArrayListExtra(CopyService.TAG_COPY_SOURCES, sourceFiles);
    intent.putExtra(CopyService.TAG_COPY_TARGET, target);
    intent.putExtra(CopyService.TAG_COPY_OPEN_MODE, openmode.ordinal());
    intent.putExtra(CopyService.TAG_COPY_MOVE, move);
    intent.putExtra(CopyService.TAG_IS_ROOT_EXPLORER, rootMode);
    ServiceWatcherUtil.runService(context.get(), intent);
  }

  private void showDialog(
      final String path,
      final ArrayList<HybridFileParcelable> filesToCopy,
      final ArrayList<HybridFileParcelable> conflictingFiles) {
    int accentColor = mainActivity.get().getAccent();
    final MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(context.get());
    CopyDialogBinding copyDialogBinding =
        CopyDialogBinding.inflate(LayoutInflater.from(mainActivity.get()));
    dialogBuilder.customView(copyDialogBinding.getRoot(), true);

    // textView
    copyDialogBinding.fileNameText.setText(conflictingFiles.get(counter).getName(context.get()));

    // checkBox
    final CheckBox checkBox = copyDialogBinding.checkBox;
    Utils.setTint(context.get(), checkBox, accentColor);
    dialogBuilder.theme(mainActivity.get().getAppTheme().getMaterialDialogTheme());
    dialogBuilder.title(context.get().getResources().getString(R.string.paste));
    dialogBuilder.positiveText(R.string.skip);
    dialogBuilder.negativeText(R.string.overwrite);
    dialogBuilder.neutralText(R.string.cancel);
    dialogBuilder.positiveColor(accentColor);
    dialogBuilder.negativeColor(accentColor);
    dialogBuilder.neutralColor(accentColor);
    dialogBuilder.onPositive(
        (dialog, which) -> {
          if (checkBox.isChecked()) dialogState = DO_NOT_REPLACE;
          doNotReplaceFiles(path, filesToCopy, conflictingFiles);
        });
    dialogBuilder.onNegative(
        (dialog, which) -> {
          if (checkBox.isChecked()) dialogState = REPLACE;
          replaceFiles(path, filesToCopy, conflictingFiles);
        });

    final MaterialDialog dialog = dialogBuilder.build();
    dialog.show();
    if (filesToCopy.get(0).getParent(context.get()).equals(path)) {
      View negative = dialog.getActionButton(DialogAction.NEGATIVE);
      negative.setEnabled(false);
    }
  }

  private void onEndDialog(
      String path,
      ArrayList<HybridFileParcelable> filesToCopy,
      ArrayList<HybridFileParcelable> conflictingFiles) {
    if (conflictingFiles != null
        && counter != conflictingFiles.size()
        && conflictingFiles.size() > 0) {
      if (dialogState == UNKNOWN) {
        showDialog(path, filesToCopy, conflictingFiles);
      } else if (dialogState == DO_NOT_REPLACE) {
        doNotReplaceFiles(path, filesToCopy, conflictingFiles);
      } else if (dialogState == REPLACE) {
        replaceFiles(path, filesToCopy, conflictingFiles);
      }
    } else {
      CopyNode c = !copyFolder.hasStarted() ? copyFolder.startCopy() : copyFolder.goToNextNode();

      if (c != null) {
        counter = 0;

        paths.add(c.getPath());
        filesToCopyPerFolder.add(c.filesToCopy);

        if (dialogState == UNKNOWN) {
          onEndDialog(c.path, c.filesToCopy, c.conflictingFiles);
        } else if (dialogState == DO_NOT_REPLACE) {
          doNotReplaceFiles(c.path, c.filesToCopy, c.conflictingFiles);
        } else if (dialogState == REPLACE) {
          replaceFiles(c.path, c.filesToCopy, c.conflictingFiles);
        }
      } else {
        finishCopying(paths, filesToCopyPerFolder);
      }
    }
  }

  private void doNotReplaceFiles(
      String path,
      ArrayList<HybridFileParcelable> filesToCopy,
      ArrayList<HybridFileParcelable> conflictingFiles) {
    if (counter < conflictingFiles.size()) {
      if (dialogState != UNKNOWN) {
        filesToCopy.remove(conflictingFiles.get(counter));
        counter++;
      } else {
        for (int j = counter; j < conflictingFiles.size(); j++) {
          filesToCopy.remove(conflictingFiles.get(j));
        }
        counter = conflictingFiles.size();
      }
    }

    onEndDialog(path, filesToCopy, conflictingFiles);
  }

  private void replaceFiles(
      String path,
      ArrayList<HybridFileParcelable> filesToCopy,
      ArrayList<HybridFileParcelable> conflictingFiles) {
    if (counter < conflictingFiles.size()) {
      if (dialogState != UNKNOWN) {
        counter++;
      } else {
        counter = conflictingFiles.size();
      }
    }

    onEndDialog(path, filesToCopy, conflictingFiles);
  }

  private void finishCopying(
      ArrayList<String> paths, ArrayList<ArrayList<HybridFileParcelable>> filesToCopyPerFolder) {
    for (int i = 0; i < filesToCopyPerFolder.size(); i++) {
      if (filesToCopyPerFolder.get(i) == null || filesToCopyPerFolder.get(i).size() == 0) {
        filesToCopyPerFolder.remove(i);
        paths.remove(i);
        i--;
      }
    }

    if (filesToCopyPerFolder.size() != 0) {
      @FolderState
      int mode = mainActivity.get().mainActivityHelper.checkFolder(path, openMode, context.get());
      if (mode == CAN_CREATE_FILES && !path.contains("otg:/")) {
        // This is used because in newer devices the user has to accept a permission,
        // see MainActivity.onActivityResult()
        mainActivity.get().oparrayListList = filesToCopyPerFolder;
        mainActivity.get().oparrayList = null;
        mainActivity.get().operation = move ? MOVE : COPY;
        mainActivity.get().oppatheList = paths;
      } else {
        if (!move) {
          for (int i = 0; i < filesToCopyPerFolder.size(); i++) {
            startService(filesToCopyPerFolder.get(i), paths.get(i), openMode);
          }
        } else {
          new MoveFiles(filesToCopyPerFolder, rootMode, path, context.get(), openMode)
              .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, paths);
        }
      }
    } else {
      Toast.makeText(
              context.get(),
              context.get().getResources().getString(R.string.no_file_overwrite),
              Toast.LENGTH_SHORT)
          .show();
    }
  }

  class CopyNode {
    private String path;
    private ArrayList<HybridFileParcelable> filesToCopy, conflictingFiles;
    private ArrayList<CopyNode> nextNodes = new ArrayList<>();

    CopyNode(String p, ArrayList<HybridFileParcelable> filesToCopy) {
      path = p;
      this.filesToCopy = filesToCopy;

      HybridFile destination = new HybridFile(openMode, path);
      conflictingFiles = checkConflicts(filesToCopy, destination);

      for (int i = 0; i < conflictingFiles.size(); i++) {
        if (conflictingFiles.get(i).isDirectory()) {
          if (deleteCopiedFolder == null) deleteCopiedFolder = new ArrayList<>();

          deleteCopiedFolder.add(new File(conflictingFiles.get(i).getPath()));

          nextNodes.add(
              new CopyNode(
                  path + "/" + conflictingFiles.get(i).getName(context.get()),
                  conflictingFiles.get(i).listFiles(context.get(), rootMode)));

          filesToCopy.remove(filesToCopy.indexOf(conflictingFiles.get(i)));
          conflictingFiles.remove(i);
          i--;
        }
      }
    }

    /** The next 2 methods are a BFS that runs through one node at a time. */
    private LinkedList<CopyNode> queue = null;

    private Set<CopyNode> visited = null;

    CopyNode startCopy() {
      queue = new LinkedList<>();
      visited = new HashSet<>();

      queue.add(this);
      visited.add(this);
      return this;
    }

    /** @return true if there are no more nodes */
    CopyNode goToNextNode() {
      if (queue.isEmpty()) return null;
      else {
        CopyNode node = queue.element();
        CopyNode child;
        if ((child = getUnvisitedChildNode(visited, node)) != null) {
          visited.add(child);
          queue.add(child);
          return child;
        } else {
          queue.remove();
          return goToNextNode();
        }
      }
    }

    boolean hasStarted() {
      return queue != null;
    }

    String getPath() {
      return path;
    }

    ArrayList<HybridFileParcelable> getFilesToCopy() {
      return filesToCopy;
    }

    ArrayList<HybridFileParcelable> getConflictingFiles() {
      return conflictingFiles;
    }

    private CopyNode getUnvisitedChildNode(Set<CopyNode> visited, CopyNode node) {
      for (CopyNode n : node.nextNodes) {
        if (!visited.contains(n)) {
          return n;
        }
      }

      return null;
    }
  }
}
