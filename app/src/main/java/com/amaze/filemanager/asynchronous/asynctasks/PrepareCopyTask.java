package com.amaze.filemanager.asynchronous.asynctasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.asynchronous.services.CopyService;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OnFileFound;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.files.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by arpitkh996 on 12-01-2016, modified by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 *
 *  This AsyncTask works by creating a tree where each folder that can be fusioned together with
 *  another in the destination is a node (CopyNode). While the tree is being created an indeterminate
 *  ProgressDialog is shown. Each node is copied when the conflicts are dealt with
 *  (the dialog is shown, and the tree is walked via a BFS).
 *  If the process is cancelled (via the button in the dialog) the dialog closes without any more code
 *  to be executed, finishCopying() is never executed so no changes are made.
 */
public class PrepareCopyTask extends AsyncTask<ArrayList<HybridFileParcelable>, String, PrepareCopyTask.CopyNode> {

    private enum DO_FOR_ALL_ELEMENTS {
        DO_NOT_REPLACE,
        REPLACE
    }

    private MainFragment mainFrag;
    private String path;
    private Boolean move;
    private int counter = 0;
    private MainActivity mainActivity;
    private Context context;
    private ProgressDialog dialog;
    private boolean rootMode = false;
    private OpenMode openMode = OpenMode.FILE;
    private DO_FOR_ALL_ELEMENTS dialogState = null;

    //causes folder containing filesToCopy to be deleted
    private ArrayList<File> deleteCopiedFolder = null;
    private CopyNode copyFolder;
    private final ArrayList<String> paths = new ArrayList<>();
    private final ArrayList<ArrayList<HybridFileParcelable>> filesToCopyPerFolder = new ArrayList<>();
    private ArrayList<HybridFileParcelable> filesToCopy;    // a copy of params sent to this

    public PrepareCopyTask(MainFragment ma, String path, Boolean move, MainActivity con, boolean rootMode) {
        mainFrag = ma;
        this.move = move;
        mainActivity = con;
        context = con;
        openMode = mainFrag.openMode;
        this.rootMode = rootMode;

        this.path = path;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = ProgressDialog.show(context, "", context.getString(R.string.processing), true);
    }

    @Override
    public void onProgressUpdate(String... message) {
        Toast.makeText(context, message[0], Toast.LENGTH_LONG).show();
    }

    @Override
    protected CopyNode doInBackground(ArrayList<HybridFileParcelable>... params) {
        filesToCopy = params[0];
        long totalBytes = 0;

        if (openMode == OpenMode.OTG ||
                openMode == OpenMode.DROPBOX
                || openMode == OpenMode.BOX
                || openMode == OpenMode.GDRIVE
                || openMode == OpenMode.ONEDRIVE
                ) {
            // no helper method for OTG to determine storage space
            return null;
        }

        totalBytes = FileUtils.getTotalBytes(filesToCopy, context);

        HybridFile destination = new HybridFile(openMode, path);
        if (destination.getUsableSpace() < totalBytes) {
            publishProgress(context.getResources().getString(R.string.in_safe));
            return null;
        }

        copyFolder = new CopyNode(path, filesToCopy);

        return copyFolder;
    }

    private ArrayList<HybridFileParcelable> checkConflicts(final ArrayList<HybridFileParcelable> filesToCopy, HybridFile destination) {
        final ArrayList<HybridFileParcelable> conflictingFiles = new ArrayList<>();
        destination.forEachChildrenFile(context, rootMode, new OnFileFound() {
            @Override
            public void onFileFound(HybridFileParcelable file) {
                for (HybridFileParcelable j : filesToCopy) {
                    if (file.getName().equals((j).getName())) {
                        conflictingFiles.add(j);
                    }
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
                || openMode == OpenMode.ONEDRIVE) {

            startService(filesToCopy, path, openMode);
        } else {

            if (copyFolder == null) {
                // not starting service as there's no sufficient space
                return;
            }

            onEndDialog(null, null, null);
        }

        dialog.dismiss();
    }

    private void startService(ArrayList<HybridFileParcelable> sourceFiles, String target, OpenMode openmode) {
        Intent intent = new Intent(context, CopyService.class);
        intent.putParcelableArrayListExtra(CopyService.TAG_COPY_SOURCES, sourceFiles);
        intent.putExtra(CopyService.TAG_COPY_TARGET, target);
        intent.putExtra(CopyService.TAG_COPY_OPEN_MODE, openmode.ordinal());
        intent.putExtra(CopyService.TAG_COPY_MOVE, move);
        ServiceWatcherUtil.runService(context, intent);
    }

    private void showDialog(final String path, final ArrayList<HybridFileParcelable> filesToCopy,
                            final ArrayList<HybridFileParcelable> conflictingFiles) {
        int accentColor = mainActivity.getColorPreference().getColor(ColorUsage.ACCENT);
        final MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(context);
        LayoutInflater layoutInflater =
                (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.copy_dialog, null);
        dialogBuilder.customView(view, true);
        // textView
        TextView textView = (TextView) view.findViewById(R.id.textView);
        textView.setText(context.getResources().getString(R.string.fileexist) + "\n" + conflictingFiles.get(counter).getName());

        // checkBox
        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBox);
        Utils.setTint(context, checkBox, accentColor);
        dialogBuilder.theme(mainActivity.getAppTheme().getMaterialDialogTheme());
        dialogBuilder.title(context.getResources().getString(R.string.paste));
        dialogBuilder.positiveText(R.string.skip);
        dialogBuilder.negativeText(R.string.overwrite);
        dialogBuilder.neutralText(R.string.cancel);
        dialogBuilder.positiveColor(accentColor);
        dialogBuilder.negativeColor(accentColor);
        dialogBuilder.neutralColor(accentColor);
        dialogBuilder.onPositive((dialog, which) -> {
            if (checkBox.isChecked())
                dialogState = DO_FOR_ALL_ELEMENTS.DO_NOT_REPLACE;
            doNotReplaceFiles(path, filesToCopy, conflictingFiles);
        });
        dialogBuilder.onNegative((dialog, which) -> {
            if (checkBox.isChecked())
                dialogState = DO_FOR_ALL_ELEMENTS.REPLACE;
            replaceFiles(path, filesToCopy, conflictingFiles);
        });

        final MaterialDialog dialog = dialogBuilder.build();
        dialog.show();
        if (filesToCopy.get(0).getParent().equals(path)) {
            View negative = dialog.getActionButton(DialogAction.NEGATIVE);
            negative.setEnabled(false);
        }
    }

    private void onEndDialog(String path, ArrayList<HybridFileParcelable> filesToCopy,
                             ArrayList<HybridFileParcelable> conflictingFiles) {
        if (conflictingFiles != null && counter != conflictingFiles.size() && conflictingFiles.size() > 0) {
            if (dialogState == null)
                showDialog(path, filesToCopy, conflictingFiles);
            else if (dialogState == DO_FOR_ALL_ELEMENTS.DO_NOT_REPLACE)
                doNotReplaceFiles(path, filesToCopy, conflictingFiles);
            else if (dialogState == DO_FOR_ALL_ELEMENTS.REPLACE)
                replaceFiles(path, filesToCopy, conflictingFiles);
        } else {
            CopyNode c = !copyFolder.hasStarted()? copyFolder.startCopy():copyFolder.goToNextNode();

            if (c != null) {
                counter = 0;

                paths.add(c.getPath());
                filesToCopyPerFolder.add(c.filesToCopy);

                if (dialogState == null)
                    onEndDialog(c.path, c.filesToCopy, c.conflictingFiles);
                else if (dialogState == DO_FOR_ALL_ELEMENTS.DO_NOT_REPLACE)
                    doNotReplaceFiles(c.path, c.filesToCopy, c.conflictingFiles);
                else if (dialogState == DO_FOR_ALL_ELEMENTS.REPLACE)
                    replaceFiles(c.path, c.filesToCopy, c.conflictingFiles);
            } else {
                finishCopying(paths, filesToCopyPerFolder);
            }
        }
    }

    private void doNotReplaceFiles(String path, ArrayList<HybridFileParcelable> filesToCopy, ArrayList<HybridFileParcelable> conflictingFiles) {
        if (counter < conflictingFiles.size()) {
            if (dialogState != null) {
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

    private void replaceFiles(String path, ArrayList<HybridFileParcelable> filesToCopy, ArrayList<HybridFileParcelable> conflictingFiles) {
        if (counter < conflictingFiles.size()) {
            if (dialogState != null) {
                counter++;
            } else {
                counter = conflictingFiles.size();
            }
        }

        onEndDialog(path, filesToCopy, conflictingFiles);
    }

    private void finishCopying(ArrayList<String> paths, ArrayList<ArrayList<HybridFileParcelable>> filesToCopyPerFolder) {
        for (int i = 0; i < filesToCopyPerFolder.size(); i++) {
            if (filesToCopyPerFolder.get(i) == null || filesToCopyPerFolder.get(i).size() == 0) {
                filesToCopyPerFolder.remove(i);
                paths.remove(i);
                i--;
            }
        }

        if (filesToCopyPerFolder.size() != 0) {
            int mode = mainActivity.mainActivityHelper.checkFolder(new File(path), context);
            if (mode == MainActivityHelper.CAN_CREATE_FILES && !path.contains("otg:/")) {
                //This is used because in newer devices the user has to accept a permission,
                // see MainActivity.onActivityResult()
                mainActivity.oparrayListList = filesToCopyPerFolder;
                mainActivity.oparrayList = null;
                mainActivity.operation = move ? DataUtils.MOVE : DataUtils.COPY;
                mainActivity.oppatheList = paths;
            } else {
                if (!move) {
                    for (int i = 0; i < filesToCopyPerFolder.size(); i++) {

                        startService(filesToCopyPerFolder.get(i), paths.get(i), openMode);
                    }
                } else {
                    new MoveFiles(filesToCopyPerFolder, mainFrag, context, openMode)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, paths);
                }
            }
        } else {
            Toast.makeText(context, context.getResources().getString(R.string.no_file_overwrite), Toast.LENGTH_SHORT).show();
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
                    if (deleteCopiedFolder == null)
                        deleteCopiedFolder = new ArrayList<>();

                    deleteCopiedFolder.add(new File(conflictingFiles.get(i).getPath()));

                    nextNodes.add(new CopyNode(path + "/"
                            + conflictingFiles.get(i).getName(),
                            conflictingFiles.get(i).listFiles(context, rootMode)));

                    filesToCopy.remove(filesToCopy.indexOf(conflictingFiles.get(i)));
                    conflictingFiles.remove(i);
                    i--;
                }
            }
        }

        /**
         * The next 2 methods are a BFS that runs through one node at a time.
         */
        private LinkedList<CopyNode> queue = null;
        private Set<CopyNode> visited = null;

        CopyNode startCopy() {
            queue = new LinkedList<>();
            visited = new HashSet<>();

            queue.add(this);
            visited.add(this);
            return this;
        }

        /**
         * @return true if there are no more nodes
         */
        CopyNode goToNextNode() {
            if (queue.isEmpty())
                return null;
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