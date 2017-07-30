package com.amaze.filemanager.utils;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.models.EncryptedEntry;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.fragments.CloudSheetFragment;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.fragments.TabFragment;
import com.amaze.filemanager.services.DeleteTask;
import com.amaze.filemanager.services.ExtractService;
import com.amaze.filemanager.services.ZipTask;
import com.amaze.filemanager.services.loaders.SearchLoader;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.files.Futils;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by root on 11/22/15, modified by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 */
public class MainActivityHelper {

    public static final String KEY_PATH = "path";
    public static final String KEY_INPUT = "input";
    public static final String KEY_OPEN_MODE = "open_mode";
    public static final String KEY_ROOT_MODE = "root_mode";
    public static final String KEY_REGEX = "regex";
    public static final String KEY_REGEX_MATCHES = "matches";

    public static final int NEW_FOLDER = 0, NEW_FILE = 1, NEW_SMB = 2, NEW_CLOUD = 3;

    private static final int SEARCH_LOADER = 50600;

    private MainActivity mainActivity;
    private Futils utils;
    private DataUtils dataUtils = DataUtils.getInstance();

    /*
     * A static string which saves the last searched query. Used to retain search task after
     * user presses back button from pressing on any list item of search results
     */
    public static String SEARCH_TEXT;

    public MainActivityHelper(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.utils = mainActivity.getFutils();
    }

    public void showFailedOperationDialog(ArrayList<BaseFile> failedOps, boolean move,
                                          Context contextc) {
        MaterialDialog.Builder mat=new MaterialDialog.Builder(contextc);
        mat.title(contextc.getString(R.string.operationunsuccesful));
        mat.theme(mainActivity.getAppTheme().getMaterialDialogTheme());
        mat.positiveColor(Color.parseColor(BaseActivity.accentSkin));
        mat.positiveText(R.string.cancel);
        String content = contextc.getResources().getString(R.string.operation_fail_following);
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
        mk(R.string.newfolder, new OnClickMaterialListener() {
            @Override
            public void onClick(MaterialDialog materialDialog) {
                String a = materialDialog.getInputEditText().getText().toString();
                mkDir(new HFile(openMode, path + "/" + a), ma);
                materialDialog.dismiss();
            }
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
        mk(R.string.newfile, new OnClickMaterialListener() {
            @Override
            public void onClick(MaterialDialog materialDialog) {
                String a = materialDialog.getInputEditText().getText().toString();
                mkFile(new HFile(openMode, path + "/" + a), ma);
                materialDialog.dismiss();
            }
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

        materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                l.onClick(materialDialog);
            }
        });
        materialDialog.show();
    }

    private interface OnClickMaterialListener {
        void onClick(MaterialDialog materialDialog);
    }

    public void add(int pos) {
        final MainFragment ma = (MainFragment) ((TabFragment) mainActivity.getSupportFragmentManager().findFragmentById(R.id.content_frame)).getTab();
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

    public void rename(OpenMode mode, final String oldPath, final String newPath,
                       final Activity context, boolean rootmode) {
        final Toast toast=Toast.makeText(context, context.getString(R.string.renaming),
                Toast.LENGTH_SHORT);
        toast.show();
        Operations.rename(new HFile(mode, oldPath), new HFile(mode, newPath), rootmode, context, new Operations.ErrorCallBack() {
            @Override
            public void exists(HFile file) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (toast != null) toast.cancel();
                        Toast.makeText(mainActivity, context.getString(R.string.fileexist),
                                Toast.LENGTH_SHORT).show();
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
                        if (b) {
                            Intent intent = new Intent("loadlist");
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

                    }
                });
            }

            @Override
            public void invalidName(final HFile file) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (toast != null) toast.cancel();
                        Toast.makeText(context, context.getString(R.string.invalid_name) + ": "
                                + file.getName(), Toast.LENGTH_LONG).show();
                    }
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
     * @param baseFiles list of {@link BaseFile} to be compressed
     */
    public void compressFiles(File file, ArrayList<BaseFile> baseFiles) {
        int mode = checkFolder(file.getParentFile(), mainActivity);
        if (mode == 2) {
            mainActivity.oppathe = (file.getPath());
            mainActivity.operation = DataUtils.COMPRESS;
            mainActivity.oparrayList = baseFiles;
        } else if (mode == 1) {
            Intent intent2 = new Intent(mainActivity, ZipTask.class);
            intent2.putExtra(ZipTask.KEY_COMPRESS_PATH, file.getPath());
            intent2.putExtra(ZipTask.KEY_COMPRESS_FILES, baseFiles);
            ServiceWatcherUtil.runService(mainActivity, intent2);
        } else Toast.makeText(mainActivity, R.string.not_allowed, Toast.LENGTH_SHORT).show();
    }


    public void mkFile(final HFile path, final MainFragment ma) {
        final Toast toast = Toast.makeText(ma.getActivity(), ma.getString(R.string.creatingfile),
                Toast.LENGTH_SHORT);
        toast.show();
        Operations.mkfile(path, ma.getActivity(), BaseActivity.rootMode, new Operations.ErrorCallBack() {
            @Override
            public void exists(final HFile file) {
                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (toast != null) toast.cancel();
                        Toast.makeText(mainActivity, mainActivity.getString(R.string.fileexist),
                                Toast.LENGTH_SHORT).show();
                        if (ma != null && ma.getActivity() != null) {
                            // retry with dialog prompted again
                            mkfile(file.getMode(), file.getParent(), ma);
                        }

                    }
                });
            }

            @Override
            public void launchSAF(HFile file) {

                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (toast != null) toast.cancel();
                        mainActivity.oppathe = path.getPath();
                        mainActivity.operation = DataUtils.NEW_FILE;
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

                        if (b) {
                            ma.updateList();
                        } else
                            Toast.makeText(ma.getActivity(), ma.getString(R.string.operationunsuccesful),
                                    Toast.LENGTH_SHORT).show();

                    }
                });
            }

            @Override
            public void invalidName(final HFile file) {
                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (toast != null) toast.cancel();
                        Toast.makeText(ma.getActivity(), ma.getString(R.string.invalid_name)
                                + ": " + file.getName(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    public void mkDir(final HFile path, final MainFragment ma) {
        final Toast toast = Toast.makeText(ma.getActivity(), ma.getString(R.string.creatingfolder),
                Toast.LENGTH_SHORT);
        toast.show();
        Operations.mkdir(path, ma.getActivity(), BaseActivity.rootMode, new Operations.ErrorCallBack() {
            @Override
            public void exists(final HFile file) {
                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (toast != null) toast.cancel();
                        Toast.makeText(mainActivity, mainActivity.getString(R.string.fileexist),
                                Toast.LENGTH_SHORT).show();
                        if (ma != null && ma.getActivity() != null) {
                            // retry with dialog prompted again
                            mkdir(file.getMode(), file.getParent(), ma);
                        }
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

                        if (b) {
                            ma.updateList();
                        } else
                            Toast.makeText(ma.getActivity(), ma.getString(R.string.operationunsuccesful),
                                    Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void invalidName(final HFile file) {
                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (toast != null) toast.cancel();
                        Toast.makeText(ma.getActivity(), ma.getString(R.string.invalid_name)
                                + ": " + file.getName(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    public void deleteFiles(ArrayList<BaseFile> files) {
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
     * Creates a SearchLoader and starts a AsyncTask
     *
     * @param query the text query entered the by user
     */
    public void search(String query) {
        TabFragment tabFragment = mainActivity.getFragment();
        if (tabFragment == null) return;
        final MainFragment ma = (MainFragment) tabFragment.getTab();
        final String fpath = ma.getCurrentPath();

        SEARCH_TEXT = query;
        mainActivity.mainFragment = (MainFragment) mainActivity.getFragment().getTab();

        createSearch(mainActivity, query, fpath, ma.openMode, BaseActivity.rootMode,
                mainActivity.sharedPref.getBoolean(KEY_REGEX, false),
                mainActivity.sharedPref.getBoolean(KEY_REGEX_MATCHES, false));
    }

    public static void createSearch(final MainActivity mainActivity, final String query,
                                    final String directoryToSearchPath, final OpenMode openMode,
                                    final boolean root, final boolean regex, final boolean matches) {
        LoaderManager.LoaderCallbacks<Void> searchCallbacks = new LoaderManager.LoaderCallbacks<Void>() {
            Handler fileHandler;

            @Override
            public Loader<Void> onCreateLoader(int id, Bundle args) {
                final MainFragment mainFragment = mainActivity.mainFragment;

                mainFragment.mSwipeRefreshLayout.setRefreshing(true);
                mainFragment.onSearchPreExecute(query);

                SearchLoader.OnCancelledListener cancelledListener = new SearchLoader.OnCancelledListener() {
                    @Override
                    public void onCancelled() {
                        mainActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                mainFragment.createViews(mainFragment.getLayoutElements(), false,
                                        mainFragment.getCurrentPath(), mainFragment.openMode, false,
                                        !mainFragment.IS_LIST);
                                mainFragment.mSwipeRefreshLayout.setRefreshing(false);
                            }
                        });

                    }
                };

                fileHandler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message inputMessage) {
                        final Pair<String, BaseFile> queryToResult = (Pair<String, BaseFile>) inputMessage.obj;

                        mainActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                mainFragment.addSearchResult(queryToResult.second, queryToResult.first);
                            }
                        });
                    }
                };

                SearchLoader searchLoader = new SearchLoader(mainActivity, cancelledListener, fileHandler);
                searchLoader.loadParameters( query, directoryToSearchPath, openMode, root, regex, matches);

                return searchLoader;
            }

            @Override
            public void onLoadFinished(Loader<Void> loader, Void data) {
                mainActivity.mainFragment.onSearchCompleted(query);
                mainActivity.mainFragment.mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onLoaderReset(Loader<Void> loader) {
                ((SearchLoader) loader).loadParameters(query, directoryToSearchPath, openMode,
                        root, regex, matches);
            }
        };

        LoaderManager loaderManager = mainActivity.getSupportLoaderManager();

        if(loaderManager.getLoader(SEARCH_LOADER) == null) {
            loaderManager.initLoader(SEARCH_LOADER, null, searchCallbacks).forceLoad();
        } else {
            loaderManager.restartLoader(SEARCH_LOADER, null, searchCallbacks).forceLoad();
        }

    }

    public static void cancelSearch(MainActivity mainActivity) {
        mainActivity.getSupportLoaderManager().destroyLoader(SEARCH_LOADER);
    }

    /**
     * Check whether creation of new directory is inside the same directory with the same name or not
     * Directory inside the same directory with similar filename shall not be allowed
     * Doesn't work at an OTG path
     *
     * @param file
     * @return
     */
    public static boolean isNewDirectoryRecursive(HFile file) {
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

    public static void requestAccountsPermission(final Activity activity) {
        final String[] PERMISSIONS = {Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.INTERNET};
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.GET_ACCOUNTS) || ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.INTERNET)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example, if the request has been denied previously.

            String fab_skin = (BaseActivity.accentSkin);
            final MaterialDialog materialDialog = GeneralDialogCreation.showBasicDialog(activity, fab_skin,
                    ((UtilitiesProviderInterface) activity).getAppTheme(),
                    new String[] {
                            activity.getResources().getString(R.string.grantgplus),
                            activity.getResources().getString(R.string.grantper),
                            activity.getResources().getString(R.string.grant),
                            activity.getResources().getString(R.string.cancel), null
                    });
            materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(activity,PERMISSIONS, 66);
                    materialDialog.dismiss();
                }
            });
            materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.finish();
                }
            });
            materialDialog.setCancelable(false);
            materialDialog.show();

        } else {
            // Contact permissions have not been granted yet. Request them directly.
            ActivityCompat.requestPermissions(activity, PERMISSIONS, 66);
        }
    }
}
