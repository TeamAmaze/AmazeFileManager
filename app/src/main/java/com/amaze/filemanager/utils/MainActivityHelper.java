package com.amaze.filemanager.utils;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.activities.superclasses.BasicActivity;
import com.amaze.filemanager.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.asynchronous.services.ExtractService;
import com.amaze.filemanager.asynchronous.services.ZipService;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.models.EncryptedEntry;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.fragments.CloudSheetFragment;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.fragments.SearchWorkerFragment;
import com.amaze.filemanager.fragments.TabFragment;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.files.CryptUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by root on 11/22/15, modified by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 */
public class MainActivityHelper {

    public static final int NEW_FOLDER = 0, NEW_FILE = 1, NEW_SMB = 2, NEW_CLOUD = 3;

    private MainActivity mainActivity;
    private DataUtils dataUtils = DataUtils.getInstance();
    private int accentColor;

    /*
     * A static string which saves the last searched query. Used to retain search task after
     * user presses back button from pressing on any list item of search results
     */
    public static String SEARCH_TEXT;

    public MainActivityHelper(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        accentColor = mainActivity.getColorPreference().getColor(ColorUsage.ACCENT);
    }

    public void showFailedOperationDialog(ArrayList<HybridFileParcelable> failedOps, boolean move, Context contextc) {
        MaterialDialog.Builder mat=new MaterialDialog.Builder(contextc);
        mat.title(contextc.getString(R.string.operationunsuccesful));
        mat.theme(mainActivity.getAppTheme().getMaterialDialogTheme());
        mat.positiveColor(accentColor);
        mat.positiveText(R.string.cancel);
        String content = contextc.getResources().getString(R.string.operation_fail_following);
        int k=1;
        for(HybridFileParcelable s:failedOps){
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
                    Toast.makeText(mainActivity, "Media Mounted", Toast.LENGTH_SHORT).show();
                    String a = intent.getData().getPath();
                    if (a != null && a.trim().length() != 0 && new File(a).exists() && new File(a).canExecute()) {
                        dataUtils.getStorages().add(a);
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

    /**
     * Prompt a dialog to user to input directory name
     *
     * @param openMode
     * @param path     current path at which directory to create
     * @param ma       {@link MainFragment} current fragment
     */
    void mkdir(final OpenMode openMode, final String path, final MainFragment ma) {
        mk(R.string.newfolder, materialDialog -> {
            String a = materialDialog.getInputEditText().getText().toString();
            mkDir(new HybridFile(openMode, path + "/" + a), ma);
            materialDialog.dismiss();
        });
    }

    /**
     * Prompt a dialog to user to input file name
     *
     * @param openMode
     * @param path     current path at which file to create
     * @param ma       {@link MainFragment} current fragment
     */
    void mkfile(final OpenMode openMode, final String path, final MainFragment ma) {
        mk(R.string.newfile, materialDialog -> {
            String a = materialDialog.getInputEditText().getText().toString();
            mkFile(new HybridFile(openMode, path + "/" + a), ma);
            materialDialog.dismiss();
        });
    }

    private void mk(@StringRes int newText, final OnClickMaterialListener l) {
        final MaterialDialog materialDialog = GeneralDialogCreation.showNameDialog(mainActivity,
                new String[]{mainActivity.getResources().getString(R.string.entername),
                        "",
                        mainActivity.getResources().getString(newText),
                        mainActivity.getResources().getString(R.string.create),
                        mainActivity.getResources().getString(R.string.cancel),
                        null});

        materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(v -> l.onClick(materialDialog));
        materialDialog.show();
    }

    private interface OnClickMaterialListener {
        void onClick(MaterialDialog materialDialog);
    }

    public void add(int pos) {
        final MainFragment ma = (MainFragment) ((TabFragment) mainActivity.getSupportFragmentManager().findFragmentById(R.id.content_frame)).getCurrentTabFragment();
        final String path = ma.getCurrentPath();

        switch (pos) {
            case NEW_FOLDER:
                mkdir(ma.openMode, path, ma);
                break;
            case NEW_FILE:
                mkfile(ma.openMode, path, ma);
                break;
            case NEW_CLOUD:
                BottomSheetDialogFragment fragment = new CloudSheetFragment();
                fragment.show(ma.getActivity().getSupportFragmentManager(),
                        CloudSheetFragment.TAG_FRAGMENT);
                break;
        }
    }

    public String getIntegralNames(String path) {
        String newPath = "";
        switch (Integer.parseInt(path)) {
            case 0:
                newPath = mainActivity.getResources().getString(R.string.images);
                break;
            case 1:
                newPath = mainActivity.getResources().getString(R.string.videos);
                break;
            case 2:
                newPath = mainActivity.getResources().getString(R.string.audio);
                break;
            case 3:
                newPath = mainActivity.getResources().getString(R.string.documents);
                break;
            case 4:
                newPath = mainActivity.getResources().getString(R.string.apks);
                break;
            case 5:
                newPath = mainActivity.getResources().getString(R.string.quick);
                break;
            case 6:
                newPath = mainActivity.getResources().getString(R.string.recent);
                break;
        }
        return newPath;
    }

    public void guideDialogForLEXA(String path) {
        final MaterialDialog.Builder x = new MaterialDialog.Builder(mainActivity);
        x.theme(mainActivity.getAppTheme().getMaterialDialogTheme());
        x.title(R.string.needsaccess);
        LayoutInflater layoutInflater = (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.lexadrawer, null);
        x.customView(view, true);
        // textView
        TextView textView = (TextView) view.findViewById(R.id.description);
        textView.setText(mainActivity.getResources().getString(R.string.needsaccesssummary) + path + mainActivity.getResources().getString(R.string.needsaccesssummary1));
        ((ImageView) view.findViewById(R.id.icon)).setImageResource(R.drawable.sd_operate_step);
        x.positiveText(R.string.open);
        x.negativeText(R.string.cancel);
        x.positiveColor(accentColor);
        x.negativeColor(accentColor);
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

    public void rename(OpenMode mode, final String oldPath, final String newPath,
                       final Activity context, boolean rootmode) {
        final Toast toast=Toast.makeText(context, context.getString(R.string.renaming),
                Toast.LENGTH_SHORT);
        toast.show();
        Operations.rename(new HybridFile(mode, oldPath), new HybridFile(mode, newPath), rootmode, context, new Operations.ErrorCallBack() {
            @Override
            public void exists(HybridFile file) {
                context.runOnUiThread(() -> {
                    if (toast != null) toast.cancel();
                    Toast.makeText(mainActivity, context.getString(R.string.fileexist),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void launchSAF(HybridFile file) {

            }

            @Override
            public void launchSAF(final HybridFile file, final HybridFile file1) {
                context.runOnUiThread(() -> {
                    if (toast != null) toast.cancel();
                    mainActivity.oppathe = file.getPath();
                    mainActivity.oppathe1 = file1.getPath();
                    mainActivity.operation = DataUtils.RENAME;
                    guideDialogForLEXA(mainActivity.oppathe1);
                });
            }

            @Override
            public void done(final HybridFile hFile, final boolean b) {
                context.runOnUiThread(() -> {
                    if (b) {
                        Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);

                        intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, hFile.getParent(context));
                        mainActivity.sendBroadcast(intent);

                        // update the database entry to reflect rename for encrypted file
                        if (oldPath.endsWith(CryptUtil.CRYPT_EXTENSION)) {
                            try {

                                CryptHandler cryptHandler = new CryptHandler(context);
                                EncryptedEntry oldEntry = cryptHandler.findEntry(oldPath);
                                EncryptedEntry newEntry = new EncryptedEntry();
                                newEntry.setId(oldEntry.getId());
                                newEntry.setPassword(oldEntry.getPassword());
                                newEntry.setPath(newPath);
                                cryptHandler.updateEntry(oldEntry, newEntry);
                            } catch (Exception e) {
                                e.printStackTrace();
                                // couldn't change the entry, leave it alone
                            }
                        }
                    } else
                        Toast.makeText(context, context.getString(R.string.operationunsuccesful),
                                Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void invalidName(final HybridFile file) {
                context.runOnUiThread(() -> {
                    if (toast != null) toast.cancel();
                    Toast.makeText(context, context.getString(R.string.invalid_name) + ": "
                            + file.getName(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    public static final int DOESNT_EXIST = 0;
    public static final int WRITABLE_OR_ON_SDCARD = 1;
    //For Android 5
    public static final int CAN_CREATE_FILES = 2;

    public int checkFolder(final File folder, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (FileUtil.isOnExtSdCard(folder, context)) {
                if (!folder.exists() || !folder.isDirectory()) {
                    return DOESNT_EXIST;
                }

                // On Android 5, trigger storage access framework.
                if (!FileUtil.isWritableNormalOrSaf(folder, context)) {
                    guideDialogForLEXA(folder.getPath());
                    return CAN_CREATE_FILES;
                }

                return WRITABLE_OR_ON_SDCARD;
            } else if (FileUtil.isWritable(new File(folder, "DummyFile"))) {
                return WRITABLE_OR_ON_SDCARD;
            } else return DOESNT_EXIST;
        } else if (Build.VERSION.SDK_INT == 19) {
            if (FileUtil.isOnExtSdCard(folder, context)) {
                // Assume that Kitkat workaround works
                return WRITABLE_OR_ON_SDCARD;
            } else if (FileUtil.isWritable(new File(folder, "DummyFile"))) {
                return WRITABLE_OR_ON_SDCARD;
            } else return DOESNT_EXIST;
        } else if (FileUtil.isWritable(new File(folder, "DummyFile"))) {
            return WRITABLE_OR_ON_SDCARD;
        } else {
            return DOESNT_EXIST;
        }
    }

    /**
     * Helper method to start Compress service
     *
     * @param file the new compressed file
     * @param baseFiles list of {@link HybridFileParcelable} to be compressed
     */
    public void compressFiles(File file, ArrayList<HybridFileParcelable> baseFiles) {
        int mode = checkFolder(file.getParentFile(), mainActivity);
        if (mode == 2) {
            mainActivity.oppathe = (file.getPath());
            mainActivity.operation = DataUtils.COMPRESS;
            mainActivity.oparrayList = baseFiles;
        } else if (mode == 1) {
            Intent intent2 = new Intent(mainActivity, ZipService.class);
            intent2.putExtra(ZipService.KEY_COMPRESS_PATH, file.getPath());
            intent2.putExtra(ZipService.KEY_COMPRESS_FILES, baseFiles);
            ServiceWatcherUtil.runService(mainActivity, intent2);
        } else Toast.makeText(mainActivity, R.string.not_allowed, Toast.LENGTH_SHORT).show();
    }


    public void mkFile(final HybridFile path, final MainFragment ma) {
        final Toast toast = Toast.makeText(ma.getActivity(), ma.getString(R.string.creatingfile),
                Toast.LENGTH_SHORT);
        toast.show();
        Operations.mkfile(path, ma.getActivity(), ThemedActivity.rootMode, new Operations.ErrorCallBack() {
            @Override
            public void exists(final HybridFile file) {
                ma.getActivity().runOnUiThread(() -> {
                    if (toast != null) toast.cancel();
                    Toast.makeText(mainActivity, mainActivity.getString(R.string.fileexist),
                            Toast.LENGTH_SHORT).show();
                    if (ma != null && ma.getActivity() != null) {
                        // retry with dialog prompted again
                        mkfile(file.getMode(), file.getParent(), ma);
                    }

                });
            }

            @Override
            public void launchSAF(HybridFile file) {

                ma.getActivity().runOnUiThread(() -> {
                    if (toast != null) toast.cancel();
                    mainActivity.oppathe = path.getPath();
                    mainActivity.operation = DataUtils.NEW_FILE;
                    guideDialogForLEXA(mainActivity.oppathe);
                });

            }

            @Override
            public void launchSAF(HybridFile file, HybridFile file1) {

            }

            @Override
            public void done(HybridFile hFile, final boolean b) {
                ma.getActivity().runOnUiThread(() -> {
                    if (b) {
                        ma.updateList();
                    } else {
                        Toast.makeText(ma.getActivity(), ma.getString(R.string.operationunsuccesful),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void invalidName(final HybridFile file) {
                ma.getActivity().runOnUiThread(() -> {
                    if (toast != null) toast.cancel();
                    Toast.makeText(ma.getActivity(), ma.getString(R.string.invalid_name)
                            + ": " + file.getName(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    public void mkDir(final HybridFile path, final MainFragment ma) {
        final Toast toast = Toast.makeText(ma.getActivity(), ma.getString(R.string.creatingfolder),
                Toast.LENGTH_SHORT);
        toast.show();
        Operations.mkdir(path, ma.getActivity(), ThemedActivity.rootMode, new Operations.ErrorCallBack() {
            @Override
            public void exists(final HybridFile file) {
                ma.getActivity().runOnUiThread(() -> {
                    if (toast != null) toast.cancel();
                    Toast.makeText(mainActivity, mainActivity.getString(R.string.fileexist),
                            Toast.LENGTH_SHORT).show();
                    if (ma != null && ma.getActivity() != null) {
                        // retry with dialog prompted again
                        mkdir(file.getMode(), file.getParent(), ma);
                    }
                });
            }

            @Override
            public void launchSAF(HybridFile file) {
                if (toast != null) toast.cancel();
                ma.getActivity().runOnUiThread(() -> {
                    mainActivity.oppathe = path.getPath();
                    mainActivity.operation = DataUtils.NEW_FOLDER;
                    guideDialogForLEXA(mainActivity.oppathe);
                });

            }

            @Override
            public void launchSAF(HybridFile file, HybridFile file1) {

            }

            @Override
            public void done(HybridFile hFile, final boolean b) {
                ma.getActivity().runOnUiThread(() -> {
                    if (b) {
                        ma.updateList();
                    } else {
                        Toast.makeText(ma.getActivity(), ma.getString(R.string.operationunsuccesful),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void invalidName(final HybridFile file) {
                ma.getActivity().runOnUiThread(() -> {

                    if (toast != null) toast.cancel();
                    Toast.makeText(ma.getActivity(), ma.getString(R.string.invalid_name)
                            + ": " + file.getName(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    public void deleteFiles(ArrayList<HybridFileParcelable> files) {
        if (files == null || files.size() == 0) return;
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
        else Toast.makeText(mainActivity, R.string.not_allowed, Toast.LENGTH_SHORT).show();
    }

    public void extractFile(File file) {
        int mode = checkFolder(file.getParentFile(), mainActivity);
        if (mode == 2) {
            mainActivity.oppathe = (file.getPath());
            mainActivity.operation = DataUtils.EXTRACT;
        } else if (mode == 1) {
            Intent intent = new Intent(mainActivity, ExtractService.class);
            intent.putExtra(ExtractService.KEY_PATH_ZIP, file.getPath());
            ServiceWatcherUtil.runService(mainActivity, intent);
        } else Toast.makeText(mainActivity, R.string.not_allowed, Toast.LENGTH_SHORT).show();
    }

    public String parseSmbPath(String a) {
        if (a.contains("@"))
            return "smb://" + a.substring(a.indexOf("@") + 1, a.length());
        else return a;
    }

    /**
     * Retrieve a path with {@link OTGUtil#PREFIX_OTG} as prefix
     * @param path
     * @return
     */
    public String parseOTGPath(String path) {
        if (path.contains(OTGUtil.PREFIX_OTG))
            return path;
        else return OTGUtil.PREFIX_OTG + path.substring(path.indexOf(":") + 1, path.length());
    }

    public String parseCloudPath(OpenMode serviceType, String path) {
        switch (serviceType) {
            case DROPBOX:
                if (path.contains(CloudHandler.CLOUD_PREFIX_DROPBOX)) return path;
                else
                    return CloudHandler.CLOUD_PREFIX_DROPBOX
                            + path.substring(path.indexOf(":") + 1, path.length());
            case BOX:
                if (path.contains(CloudHandler.CLOUD_PREFIX_BOX)) return path;
                else return CloudHandler.CLOUD_PREFIX_BOX
                        + path.substring(path.indexOf(":") + 1, path.length());
            case GDRIVE:
                if (path.contains(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE)) return path;
                else return CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE
                        + path.substring(path.indexOf(":") + 1, path.length());
            case ONEDRIVE:
                if (path.contains(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)) return path;
                else
                    return CloudHandler.CLOUD_PREFIX_ONE_DRIVE + path.substring(path.indexOf(":") + 1,
                            path.length());
            default:
                return path;
        }
    }

    /**
     * Creates a fragment which will handle the search AsyncTask {@link SearchWorkerFragment}
     *
     * @param query the text query entered the by user
     */
    public void search(SharedPreferences sharedPrefs, String query) {
        TabFragment tabFragment = mainActivity.getTabFragment();
        if (tabFragment == null) return;
        final MainFragment ma = (MainFragment) tabFragment.getCurrentTabFragment();
        final String fpath = ma.getCurrentPath();

        /*SearchTask task = new SearchTask(ma.searchHelper, ma, query);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fpath);*/
        //ma.searchTask = task;
        SEARCH_TEXT = query;
        mainActivity.mainFragment = (MainFragment) mainActivity.getTabFragment().getCurrentTabFragment();
        FragmentManager fm = mainActivity.getSupportFragmentManager();
        SearchWorkerFragment fragment =
                (SearchWorkerFragment) fm.findFragmentByTag(MainActivity.TAG_ASYNC_HELPER);

        if (fragment != null) {
            if (fragment.mSearchAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                fragment.mSearchAsyncTask.cancel(true);
            }
            fm.beginTransaction().remove(fragment).commit();
        }

        addSearchFragment(fm, new SearchWorkerFragment(), fpath, query, ma.openMode, ThemedActivity.rootMode,
                sharedPrefs.getBoolean(SearchWorkerFragment.KEY_REGEX, false),
                sharedPrefs.getBoolean(SearchWorkerFragment.KEY_REGEX_MATCHES, false));
    }

    /**
     * Adds a search fragment that can persist it's state on config change
     *
     * @param fragmentManager fragmentManager
     * @param fragment        current fragment
     * @param path            current path
     * @param input           query typed by user
     * @param openMode        defines the file type
     * @param rootMode        is root enabled
     * @param regex           is regular expression search enabled
     * @param matches         is matches enabled for patter matching
     */
    public static void addSearchFragment(FragmentManager fragmentManager, Fragment fragment,
                                         String path, String input, OpenMode openMode, boolean rootMode,
                                         boolean regex, boolean matches) {
        Bundle args = new Bundle();
        args.putString(SearchWorkerFragment.KEY_INPUT, input);
        args.putString(SearchWorkerFragment.KEY_PATH, path);
        args.putInt(SearchWorkerFragment.KEY_OPEN_MODE, openMode.ordinal());
        args.putBoolean(SearchWorkerFragment.KEY_ROOT_MODE, rootMode);
        args.putBoolean(SearchWorkerFragment.KEY_REGEX, regex);
        args.putBoolean(SearchWorkerFragment.KEY_REGEX_MATCHES, matches);

        fragment.setArguments(args);
        fragmentManager.beginTransaction().add(fragment, MainActivity.TAG_ASYNC_HELPER).commit();
    }

    /**
     * Check whether creation of new directory is inside the same directory with the same name or not
     * Directory inside the same directory with similar filename shall not be allowed
     * Doesn't work at an OTG path
     *
     * @param file
     * @return
     */
    public static boolean isNewDirectoryRecursive(HybridFile file) {
        return file.getName().equals(file.getParentName());
    }



    public static boolean checkAccountsPermission(Context context) {
        // Verify that all required contact permissions have been granted.
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    public static void requestAccountsPermission(final BasicActivity activity) {
        final String[] PERMISSIONS = {Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.INTERNET};
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.GET_ACCOUNTS) || ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.INTERNET)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example, if the request has been denied previously.

            final MaterialDialog materialDialog = GeneralDialogCreation.showBasicDialog(activity,
                    new String[] {
                            activity.getResources().getString(R.string.grantgplus),
                            activity.getResources().getString(R.string.grantper),
                            activity.getResources().getString(R.string.grant),
                            activity.getResources().getString(R.string.cancel), null
                    });
            materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(v -> {
                ActivityCompat.requestPermissions(activity,PERMISSIONS, 66);
                materialDialog.dismiss();
            });
            materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(v -> activity.finish());
            materialDialog.setCancelable(false);
            materialDialog.show();

        } else {
            // Contact permissions have not been granted yet. Request them directly.
            ActivityCompat.requestPermissions(activity, PERMISSIONS, 66);
        }
    }
}
