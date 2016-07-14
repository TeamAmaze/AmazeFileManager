package com.amaze.filemanager.utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.fragments.SearchAsyncHelper;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.fragments.TabFragment;
import com.amaze.filemanager.services.DeleteTask;
import com.amaze.filemanager.services.ExtractService;
import com.amaze.filemanager.services.ZipTask;
import com.amaze.filemanager.ui.dialogs.SmbSearchDialog;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by root on 11/22/15.
 */
public class MainActivityHelper {
    MainActivity mainActivity;
    Futils utils;

    /*
     * A static string which saves the last searched query. Used to retain search task after
     * user presses back button from pressing on any list item of search results
     */
    public static String SEARCH_TEXT;

    // reserved characters by OS, shall not be allowed in file names
    private static final String FOREWARD_SLASH = "/";
    private static final String BACKWARD_SLASH = "\\";
    private static final String COLON = ":";
    private static final String ASTERISK = "*";
    private static final String QUESTION_MARK = "?";
    private static final String QUOTE = "\"";
    private static final String GREATER_THAN = ">";
    private static final String LESS_THAN = "<";

    public MainActivityHelper(MainActivity mainActivity){
        this.mainActivity=mainActivity;
        utils=new Futils();
    }
    public void showFailedOperationDialog(ArrayList<BaseFile> failedOps, boolean move, Context contextc){
        MaterialDialog.Builder mat=new MaterialDialog.Builder(contextc);
        mat.title("Operation Unsuccessful");
        if(mainActivity.theme1==1)mat.theme(Theme.DARK);
        mat.positiveColor(Color.parseColor(BaseActivity.accentSkin));
        mat.positiveText(R.string.cancel);
        String content="Following files were not "+(move?"moved":"copied")+" successfully";
        int k=1;
        for(BaseFile s:failedOps){
            content=content+ "\n" + (k) + ". " + s.getName();
            k++;
        }
        mat.content(content);
        mat.build().show();
    }
    public final BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
                    Toast.makeText( mainActivity, "Media Mounted", Toast.LENGTH_SHORT).show();
                    String a = intent.getData().getPath();
                    if (a != null && a.trim().length() != 0 && new File(a).exists() && new File(a).canExecute()) {
                        DataUtils.storages.add(a);
                        mainActivity.refreshDrawer();
                    } else {
                        mainActivity.refreshDrawer();
                    }
                } else if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {

                    mainActivity.refreshDrawer();
                }
            }
        }
    };
    void mkdir(final int openMode,final String path,final Main ma){
        final MaterialDialog materialDialog=utils.showNameDialog(mainActivity,new String[]{utils.getString(mainActivity, R.string.entername), "",utils.getString(mainActivity,R.string.newfolder),utils.getString(mainActivity, R.string.create),utils.getString(mainActivity,R.string.cancel),null});
        materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String a = materialDialog.getInputEditText().getText().toString();
                if (validateFileName(new HFile(openMode,path + "/" + a), true)) {

                    mkDir(new HFile(openMode,path + "/" + a),ma);
                } else Toast.makeText(mainActivity, R.string.invalid_name, Toast.LENGTH_SHORT).show();
                materialDialog.dismiss();
            }
        });
        materialDialog.show();
    }
    void mkfile(final int openMode,final String path,final Main ma){
        final MaterialDialog materialDialog=utils.showNameDialog(mainActivity,new String[]{utils.getString(mainActivity, R.string.entername), "",utils.getString(mainActivity,R.string.newfile),utils.getString(mainActivity, R.string.create),utils.getString(mainActivity,R.string.cancel),null});
        materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String a = materialDialog.getInputEditText().getText().toString();
                if (validateFileName(new HFile(openMode,path + "/" + a), false)) {

                    mkFile(new HFile(openMode,path + "/" + a),ma);
                } else Toast.makeText(mainActivity, R.string.invalid_name, Toast.LENGTH_SHORT).show();
                materialDialog.dismiss();
            }
        });
        materialDialog.show();
    }
    public void add(int pos) {
        final Main ma = (Main) ((TabFragment) mainActivity.getSupportFragmentManager().findFragmentById(R.id.content_frame)).getTab();
        switch (pos) {

            case 0:
                final String path = ma.CURRENT_PATH;
                mkdir(ma.openMode,path,ma);
                break;
            case 1:
                final String path1 = ma.CURRENT_PATH;
                mkfile(ma.openMode,path1,ma);
                break;
            case 2:
                SmbSearchDialog smbDialog=new SmbSearchDialog();
                smbDialog.show(mainActivity.getFragmentManager(),"tab");
                break;
            /*case 3:
                mainActivity.bindDrive();
                break;*/
        }
    }

    public String getIntegralNames(String path){
        String newPath="";
        switch (Integer.parseInt(path)){
            case 0:
                newPath=mainActivity.getResources().getString(R.string.images);
                break;
            case 1:
                newPath=mainActivity.getResources().getString(R.string.videos);
                break;
            case 2:
                newPath=mainActivity.getResources().getString(R.string.audio);
                break;
            case 3:
                newPath=mainActivity.getResources().getString(R.string.documents);
                break;
            case 4:
                newPath=mainActivity.getResources().getString(R.string.apks);
                break;
            case 5:
                newPath=mainActivity.getResources().getString(R.string.quick);
                break;
            case 6:
                newPath=mainActivity.getResources().getString(R.string.recent);
                break;
        }
        return newPath;
    }
    public void guideDialogForLEXA(String path) {
        final MaterialDialog.Builder x = new MaterialDialog.Builder(mainActivity);
        if (mainActivity.theme1 == 1) x.theme(Theme.DARK);
        x.title(R.string.needsaccess);
        LayoutInflater layoutInflater = (LayoutInflater) mainActivity.getSystemService(mainActivity.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.lexadrawer, null);
        x.customView(view, true);
        // textView
        TextView textView = (TextView) view.findViewById(R.id.description);
        textView.setText(utils.getString(mainActivity, R.string.needsaccesssummary) + path + utils.getString(mainActivity, R.string.needsaccesssummary1));
        ((ImageView) view.findViewById(R.id.icon)).setImageResource(R.drawable.sd_operate_step);
        x.positiveText(R.string.open);
        x.negativeText(R.string.cancel);
        x.positiveColor(Color.parseColor(BaseActivity.accentSkin));
        x.negativeColor(Color.parseColor(BaseActivity.accentSkin));
        x.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                triggerStorageAccessFramework();
            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {
                Toast.makeText(mainActivity, R.string.error, Toast.LENGTH_SHORT).show();
            }
        });
        final MaterialDialog y = x.build();
        y.show();
    }

    private void triggerStorageAccessFramework() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        mainActivity.startActivityForResult(intent, 3);
    }
    public void rename(int mode, String f, String f1, final Activity context, boolean rootmode) {
        final Toast toast=Toast.makeText(context, R.string.renaming, Toast.LENGTH_LONG);
        toast.show();
        Operations.rename(new HFile(mode, f), new HFile(mode, f1), rootmode, context, new Operations.ErrorCallBack() {
            @Override
            public void exists(HFile file) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (toast != null) toast.cancel();
                        Toast.makeText(mainActivity, (R.string.fileexist), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void launchSAF(HFile file) {

            }

            @Override
            public void launchSAF(final HFile file, final HFile file1) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (toast != null) toast.cancel();
                        mainActivity.oppathe = file.getPath();
                        mainActivity.oppathe1 = file1.getPath();
                        mainActivity.operation = DataUtils.RENAME;
                        guideDialogForLEXA(mainActivity.oppathe1);
                    }
                });
            }

            @Override
            public void done(HFile hFile, final boolean b) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (toast != null) toast.cancel();
                        if (b) {
                            Intent intent = new Intent("loadlist");
                            mainActivity.sendBroadcast(intent);
                        } else
                            Toast.makeText(context, R.string.operationunsuccesful, Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
    }

    public int checkFolder(final File folder, Context context) {
        boolean lol= Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP,ext=FileUtil.isOnExtSdCard(folder, context);
        if (lol && ext) {
            if (!folder.exists() || !folder.isDirectory()) {
                return 0;
            }

            // On Android 5, trigger storage access framework.
            if (!FileUtil.isWritableNormalOrSaf(folder, context)) {
                guideDialogForLEXA(folder.getPath());
                return 2;
            }
            return 1;
        } else if (Build.VERSION.SDK_INT == 19 && FileUtil.isOnExtSdCard(folder, context)) {
            // Assume that Kitkat workaround works
            return 1;
        } else if (FileUtil.isWritable(new File(folder, "DummyFile"))) {
            return 1;
        } else {
            return 0;
        }
    }

    public void compressFiles(File file, ArrayList<BaseFile> b) {
        int mode = checkFolder(file.getParentFile(), mainActivity);
        if (mode == 2) {
            mainActivity.oppathe = (file.getPath());
            mainActivity.operation = DataUtils.COMPRESS;
            mainActivity.oparrayList = b;
        } else if (mode == 1) {
            Intent intent2 = new Intent(mainActivity, ZipTask.class);
            intent2.putExtra("name", file.getPath());
            intent2.putExtra("files", b);
            mainActivity.startService(intent2);
        } else Toast.makeText(mainActivity, R.string.not_allowed, Toast.LENGTH_SHORT).show();
    }


    public void mkFile(final HFile path,final Main ma) {
        final Toast toast=Toast.makeText(ma.getActivity(), R.string.creatingfile, Toast.LENGTH_LONG);
        toast.show();
        Operations.mkfile(path, ma.getActivity(), ma.ROOT_MODE, new Operations.ErrorCallBack() {
            @Override
            public void exists(final HFile file) {
                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(toast!=null)toast.cancel();
                        Toast.makeText(mainActivity, (R.string.fileexist), Toast.LENGTH_SHORT).show();
                        if(ma!=null && ma.getActivity()!=null)
                            mkfile(file.getMode(),file.getPath(),ma);

                    }
                });
            }

            @Override
            public void launchSAF(HFile file) {

                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(toast!=null)toast.cancel();
                        mainActivity.oppathe = path.getPath();
                        mainActivity.operation = DataUtils.NEW_FOLDER;
                        guideDialogForLEXA(mainActivity.oppathe);
                    }});

            }

            @Override
            public void launchSAF(HFile file, HFile file1) {

            }

            @Override
            public void done(HFile hFile,final boolean b) {
                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(toast!=null)toast.cancel();
                        if(b){
                            ma.updateList();
                        }
                        else Toast.makeText(ma.getActivity(),"Operation Failed",Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
    }
    public void mkDir(final HFile path,final Main ma) {
        final Toast toast=Toast.makeText(ma.getActivity(), R.string.creatingfolder, Toast.LENGTH_LONG);
        toast.show();
        Operations.mkdir(path, ma.getActivity(), ma.ROOT_MODE, new Operations.ErrorCallBack() {
            @Override
            public void exists(final HFile file) {
                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (toast != null) toast.cancel();
                        Toast.makeText(mainActivity, (R.string.fileexist), Toast.LENGTH_SHORT).show();
                        if (ma != null && ma.getActivity() != null)
                            mkdir(file.getMode(), file.getPath(), ma);
                    }
                });
            }

            @Override
            public void launchSAF(HFile file) {
                if (toast != null) toast.cancel();
                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.oppathe = path.getPath();
                        mainActivity.operation = DataUtils.NEW_FOLDER;
                        guideDialogForLEXA(mainActivity.oppathe);
                    }
                });

            }

            @Override
            public void launchSAF(HFile file, HFile file1) {

            }

            @Override
            public void done(HFile hFile, final boolean b) {
                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (toast != null) toast.cancel();
                        if (b) {
                            ma.updateList();
                        } else
                            Toast.makeText(ma.getActivity(), R.string.operationunsuccesful, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void deleteFiles(ArrayList<BaseFile> files) {
        if (files == null) return;
        if (files.get(0).isSmb()) {
            new DeleteTask(null, mainActivity).execute((files));
            return;
        }
        int mode = checkFolder(new File(files.get(0).getPath()).getParentFile(), mainActivity);
        if (mode == 2) {
            mainActivity.oparrayList = (files);
            mainActivity.operation = DataUtils.DELETE;
        } else if (mode == 1 || mode == 0)
            new DeleteTask(null, mainActivity).execute((files));
    }

    public void extractFile(File file) {
        int mode = checkFolder(file.getParentFile(), mainActivity);
        if (mode == 2) {
            mainActivity.oppathe = (file.getPath());
            mainActivity.operation = DataUtils.EXTRACT;
        } else if (mode == 1) {
            Intent intent = new Intent(mainActivity, ExtractService.class);
            intent.putExtra("zip", file.getPath());
            mainActivity.startService(intent);
        } else Toast.makeText(mainActivity, R.string.not_allowed, Toast.LENGTH_SHORT).show();
    }

    public String parseSmbPath(String a) {
        if (a.contains("@"))
            return "smb://" + a.substring(a.indexOf("@") + 1, a.length());
        else return a;
    }

    /**
     * Creates a fragment which will handle the search AsyncTask {@link SearchAsyncHelper}
     * @param query the text query entered the by user
     */
    public void search(String query) {
        TabFragment tabFragment=mainActivity.getFragment();
        if(tabFragment==null)return;
        final Main ma = (Main) tabFragment.getTab();
        final String fpath = ma.CURRENT_PATH;

        /*SearchTask task = new SearchTask(ma.searchHelper, ma, query);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fpath);*/
        //ma.searchTask = task;
        SEARCH_TEXT = query;
        mainActivity.mainFragment = (Main) mainActivity.getFragment().getTab();
        FragmentManager fm = mainActivity.getSupportFragmentManager();
        SearchAsyncHelper fragment =
                (SearchAsyncHelper) fm.findFragmentByTag(MainActivity.TAG_ASYNC_HELPER);

        if (fragment != null) {

            if (fragment.mSearchTask.getStatus() == AsyncTask.Status.RUNNING) {

                fragment.mSearchTask.cancel(true);
            }
            fm.beginTransaction().remove(fragment).commit();
        }

        addSearchFragment(fm, new SearchAsyncHelper(), fpath, query, ma.openMode, ma.ROOT_MODE,
                mainActivity.Sp.getBoolean(SearchAsyncHelper.KEY_REGEX, false),
                mainActivity.Sp.getBoolean(SearchAsyncHelper.KEY_REGEX_MATCHES, false));
    }

    /**
     * Adds a search fragment that can persist it's state on config change
     * @param fragmentManager fragmentManager
     * @param fragment current fragment
     * @param path current path
     * @param input query typed by user
     * @param openMode dunno
     * @param rootMode is root enabled
     * @param regex is regular expression search enabled
     * @param matches is matches enabled for patter matching
     */
    public static void addSearchFragment(FragmentManager fragmentManager, Fragment fragment,
                                         String path, String input, int openMode, boolean rootMode,
                                         boolean regex, boolean matches) {
        Bundle args = new Bundle();
        args.putString(SearchAsyncHelper.KEY_INPUT, input);
        args.putString(SearchAsyncHelper.KEY_PATH, path);
        args.putInt(SearchAsyncHelper.KEY_OPEN_MODE, openMode);
        args.putBoolean(SearchAsyncHelper.KEY_ROOT_MODE, rootMode);
        args.putBoolean(SearchAsyncHelper.KEY_REGEX, regex);
        args.putBoolean(SearchAsyncHelper.KEY_REGEX_MATCHES, matches);

        fragment.setArguments(args);
        fragmentManager.beginTransaction().add(fragment,
                MainActivity.TAG_ASYNC_HELPER).commit();
    }

    /**
     * Validates file name at the time of creation
     * special reserved characters shall not be allowed in the file names
     * @param file the file which needs to be validated
     * @param isDir if the file is a directory, in case it shall not be named same as the parent
     * @return boolean if the file name is valid or invalid
     */
    public static boolean validateFileName(HFile file, boolean isDir) {

        StringBuilder builder = new StringBuilder(file.getPath());
        String newName = builder.substring(builder.lastIndexOf("/")+1, builder.length());

        if (newName.contains(ASTERISK) || newName.contains(BACKWARD_SLASH) ||
                newName.contains(COLON) || newName.contains(FOREWARD_SLASH) ||
                newName.contains(GREATER_THAN) || newName.contains(LESS_THAN) ||
                newName.contains(QUESTION_MARK) || newName.contains(QUOTE)) {
            return false;
        } else if (isDir) {

            // new directory name shall not be equal to parent directory name
            StringBuilder parentPath = new StringBuilder(builder.substring(0,
                    builder.length()-(newName.length()+1)));
            String parentName = parentPath.substring(parentPath.lastIndexOf("/")+1,
                    parentPath.length());
            if (newName.equals(parentName)) return false;
        }
        return true;
    }
}
