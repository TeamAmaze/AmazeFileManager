package com.amaze.filemanager.ui.dialogs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.activities.superclasses.BasicActivity;
import com.amaze.filemanager.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.adapters.HiddenAdapter;
import com.amaze.filemanager.asynchronous.asynctasks.CountItemsOrAndSizeTask;
import com.amaze.filemanager.asynchronous.asynctasks.GenerateHashesTask;
import com.amaze.filemanager.asynchronous.asynctasks.LoadFolderSpaceDataTask;
import com.amaze.filemanager.exceptions.CryptException;
import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.fragments.AppsListFragment;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.fragments.preference_fragments.PrefFrag;
import com.amaze.filemanager.ui.LayoutElementParcelable;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.FingerprintHandler;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.files.EncryptDecryptUtils;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.os.Build.VERSION_CODES.M;
import static com.amaze.filemanager.utils.files.FileUtils.toHybridFileArrayList;

/**
 * Here are a lot of function that create material dialogs
 *
 * @author Emmanuel
 *         on 17/5/2017, at 13:27.
 */

public class GeneralDialogCreation {

    public static MaterialDialog showBasicDialog(BasicActivity m, String[] texts) {
        int accentColor = m.getColorPreference().getColor(ColorUsage.ACCENT);
        MaterialDialog.Builder a = new MaterialDialog.Builder(m)
                .content(texts[0])
                .widgetColor(accentColor)
                .theme(m.getAppTheme().getMaterialDialogTheme())
                .title(texts[1])
                .positiveText(texts[2])
                .positiveColor(accentColor)
                .negativeText(texts[3])
                .negativeColor(accentColor);
        if (texts[4] != (null)) {
            a.neutralText(texts[4]).neutralColor(accentColor);
        }
        return a.build();
    }

    public static MaterialDialog showNameDialog(final MainActivity m, String[] texts) {
        int accentColor = m.getColorPreference().getColor(ColorUsage.ACCENT);
        MaterialDialog.Builder a = new MaterialDialog.Builder(m);
        a.input(texts[0], texts[1], false,
                (materialDialog, charSequence) -> {});
        a.widgetColor(accentColor);

        a.theme(m.getAppTheme().getMaterialDialogTheme());
        a.title(texts[2]);

        a.positiveText(texts[3]);

        if(texts[4] != null) {
            a.neutralText(texts[4]);
        }

        if (texts[5] != null) {
            a.negativeText(texts[5]);
            a.negativeColor(accentColor);
        }
        return a.build();
    }

    @SuppressWarnings("ConstantConditions")
    public static void deleteFilesDialog(final Context c, final ArrayList<LayoutElementParcelable> layoutElements,
                                         final MainActivity mainActivity, final List<LayoutElementParcelable> positions,
                                         AppTheme appTheme) {

        final ArrayList<HybridFileParcelable> itemsToDelete = new ArrayList<>();
        int accentColor = mainActivity.getColorPreference().getColor(ColorUsage.ACCENT);

        // Build dialog with custom view layout and accent color.
        MaterialDialog dialog = new MaterialDialog.Builder(c)
                .title(c.getString(R.string.dialog_delete_title))
                .customView(R.layout.dialog_delete, true)
                .theme(appTheme.getMaterialDialogTheme())
                .negativeText(c.getString(R.string.cancel).toUpperCase())
                .positiveText(c.getString(R.string.delete).toUpperCase())
                .positiveColor(accentColor)
                .negativeColor(accentColor)
                .onPositive((dialog1, which) -> {
                    Toast.makeText(c, c.getString(R.string.deleting), Toast.LENGTH_SHORT).show();
                    mainActivity.mainActivityHelper.deleteFiles(itemsToDelete);
                })
                .build();

        // Get views from custom layout to set text values.
        final TextView categoryDirectories = (TextView) dialog.getCustomView().findViewById(R.id.category_directories);
        final TextView categoryFiles = (TextView) dialog.getCustomView().findViewById(R.id.category_files);
        final TextView listDirectories = (TextView) dialog.getCustomView().findViewById(R.id.list_directories);
        final TextView listFiles = (TextView) dialog.getCustomView().findViewById(R.id.list_files);
        final TextView total = (TextView) dialog.getCustomView().findViewById(R.id.total);

        // Parse items to delete.

        new AsyncTask<Void, Object, Void>() {

            long sizeTotal = 0;
            StringBuilder files = new StringBuilder();
            StringBuilder directories = new StringBuilder();
            int counterDirectories = 0;
            int counterFiles = 0;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                listFiles.setText(c.getString(R.string.loading));
                listDirectories.setText(c.getString(R.string.loading));
                total.setText(c.getString(R.string.loading));
            }

            @Override
            protected Void doInBackground(Void... params) {

                for (int i = 0; i < positions.size(); i++) {
                    final LayoutElementParcelable layoutElement = positions.get(i);
                    itemsToDelete.add(layoutElement.generateBaseFile());

                    // Build list of directories to delete.
                    if (layoutElement.isDirectory()) {
                        // Don't add newline between category and list.
                        if (counterDirectories != 0) {
                            directories.append("\n");
                        }

                        long sizeDirectory = layoutElement.generateBaseFile().folderSize(c);

                        directories.append(++counterDirectories)
                                .append(". ")
                                .append(layoutElement.getTitle())
                                .append(" (")
                                .append(Formatter.formatFileSize(c, sizeDirectory))
                                .append(")");
                        sizeTotal += sizeDirectory;
                        // Build list of files to delete.
                    } else {
                        // Don't add newline between category and list.
                        if (counterFiles != 0) {
                            files.append("\n");
                        }

                        files.append(++counterFiles)
                                .append(". ")
                                .append(layoutElement.getTitle())
                                .append(" (")
                                .append(layoutElement.getSize())
                                .append(")");
                        sizeTotal += layoutElement.getlongSize();
                    }

                    publishProgress(sizeTotal, counterFiles, counterDirectories, files, directories);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Object... result) {
                super.onProgressUpdate(result);

                int tempCounterFiles = (int) result[1];
                int tempCounterDirectories = (int) result[2];
                long tempSizeTotal = (long) result[0];
                StringBuilder tempFilesStringBuilder = (StringBuilder) result[3];
                StringBuilder tempDirectoriesStringBuilder = (StringBuilder) result[4];

                updateViews(tempSizeTotal, tempFilesStringBuilder, tempDirectoriesStringBuilder,
                        tempCounterFiles, tempCounterDirectories);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                updateViews(sizeTotal, files, directories, counterFiles, counterDirectories);
            }

            private void updateViews(long tempSizeTotal, StringBuilder filesStringBuilder,
                                     StringBuilder directoriesStringBuilder, int... values) {

                int tempCounterFiles = values[0];
                int tempCounterDirectories = values[1];

                // Hide category and list for directories when zero.
                if (tempCounterDirectories == 0) {

                    if (tempCounterDirectories == 0) {

                        categoryDirectories.setVisibility(View.GONE);
                        listDirectories.setVisibility(View.GONE);
                    }
                    // Hide category and list for files when zero.
                }

                if (tempCounterFiles == 0) {


                    categoryFiles.setVisibility(View.GONE);
                    listFiles.setVisibility(View.GONE);
                }

                if (tempCounterDirectories != 0 || tempCounterFiles !=0) {
                    listDirectories.setText(directoriesStringBuilder);
                    if (listDirectories.getVisibility() != View.VISIBLE && tempCounterDirectories != 0)
                        listDirectories.setVisibility(View.VISIBLE);
                    listFiles.setText(filesStringBuilder);
                    if (listFiles.getVisibility() != View.VISIBLE && tempCounterFiles != 0)
                        listFiles.setVisibility(View.VISIBLE);

                    if (categoryDirectories.getVisibility() != View.VISIBLE && tempCounterDirectories != 0)
                        categoryDirectories.setVisibility(View.VISIBLE);
                    if (categoryFiles.getVisibility() != View.VISIBLE && tempCounterFiles != 0)
                        categoryFiles.setVisibility(View.VISIBLE);
                }

                // Show total size with at least one directory or file and size is not zero.
                if (tempCounterFiles + tempCounterDirectories > 1 && tempSizeTotal > 0) {
                    StringBuilder builderTotal = new StringBuilder()
                            .append(c.getString(R.string.total))
                            .append(" ")
                            .append(Formatter.formatFileSize(c, tempSizeTotal));
                    total.setText(builderTotal);
                    if (total.getVisibility() != View.VISIBLE)
                        total.setVisibility(View.VISIBLE);
                } else {
                    total.setVisibility(View.GONE);
                }
            }
        }.execute();

        // Set category text color for Jelly Bean (API 16) and later.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            categoryDirectories.setTextColor(accentColor);
            categoryFiles.setTextColor(accentColor);
        }

        // Show dialog on screen.
        dialog.show();
    }

    public static void showPropertiesDialogWithPermissions(HybridFileParcelable baseFile, final String permissions,
                                                           ThemedActivity activity, boolean isRoot, AppTheme appTheme) {
        showPropertiesDialog(baseFile, permissions, activity, isRoot, appTheme, true, false);
    }

    public static void showPropertiesDialogWithoutPermissions(final HybridFileParcelable f, ThemedActivity activity, AppTheme appTheme) {
        showPropertiesDialog(f, null, activity, false, appTheme, false, false);
    }
    public static void showPropertiesDialogForStorage(final HybridFileParcelable f, ThemedActivity activity, AppTheme appTheme) {
        showPropertiesDialog(f, null, activity, false, appTheme, false, true);
    }

    private static void showPropertiesDialog(final HybridFileParcelable baseFile, final String permissions,
                                             ThemedActivity base, boolean isRoot, AppTheme appTheme,
                                             boolean showPermissions, boolean forStorage) {
        final ExecutorService executor = Executors.newFixedThreadPool(3);
        final Context c = base.getApplicationContext();
        int accentColor = base.getColorPreference().getColor(ColorUsage.ACCENT);
        long last = baseFile.getDate();
        final String date = Utils.getDate(last),
                items = c.getString(R.string.calculating),
                name  = baseFile.getName(),
                parent = baseFile.getReadablePath(baseFile.getParent(c));

        MaterialDialog.Builder builder = new MaterialDialog.Builder(base);
        builder.title(c.getString(R.string.properties));
        builder.theme(appTheme.getMaterialDialogTheme());

        View v = base.getLayoutInflater().inflate(R.layout.properties_dialog, null);
        TextView itemsText = (TextView) v.findViewById(R.id.t7);

        /*View setup*/ {
            TextView mNameTitle = (TextView) v.findViewById(R.id.title_name);
            mNameTitle.setTextColor(accentColor);

            TextView mDateTitle = (TextView) v.findViewById(R.id.title_date);
            mDateTitle.setTextColor(accentColor);

            TextView mSizeTitle = (TextView) v.findViewById(R.id.title_size);
            mSizeTitle.setTextColor(accentColor);

            TextView mLocationTitle = (TextView) v.findViewById(R.id.title_location);
            mLocationTitle.setTextColor(accentColor);

            TextView md5Title = (TextView) v.findViewById(R.id.title_md5);
            md5Title.setTextColor(accentColor);

            TextView sha256Title = (TextView) v.findViewById(R.id.title_sha256);
            sha256Title.setTextColor(accentColor);

            ((TextView) v.findViewById(R.id.t5)).setText(name);
            ((TextView) v.findViewById(R.id.t6)).setText(parent);
            itemsText.setText(items);
            ((TextView) v.findViewById(R.id.t8)).setText(date);

            LinearLayout mNameLinearLayout = (LinearLayout) v.findViewById(R.id.properties_dialog_name);
            LinearLayout mLocationLinearLayout = (LinearLayout) v.findViewById(R.id.properties_dialog_location);
            LinearLayout mSizeLinearLayout = (LinearLayout) v.findViewById(R.id.properties_dialog_size);
            LinearLayout mDateLinearLayout = (LinearLayout) v.findViewById(R.id.properties_dialog_date);

            // setting click listeners for long press
            mNameLinearLayout.setOnLongClickListener(v1 -> {
                FileUtils.copyToClipboard(c, name);
                Toast.makeText(c, c.getResources().getString(R.string.name) + " " +
                        c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                return false;
            });
            mLocationLinearLayout.setOnLongClickListener(v12 -> {
                FileUtils.copyToClipboard(c, parent);
                Toast.makeText(c, c.getResources().getString(R.string.location) + " " +
                        c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                return false;
            });
            mSizeLinearLayout.setOnLongClickListener(v13 -> {
                FileUtils.copyToClipboard(c, items);
                Toast.makeText(c, c.getResources().getString(R.string.size) + " " +
                        c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                return false;
            });
            mDateLinearLayout.setOnLongClickListener(v14 -> {
                FileUtils.copyToClipboard(c, date);
                Toast.makeText(c, c.getResources().getString(R.string.date) + " " +
                        c.getResources().getString(R.string.properties_copied_clipboard), Toast.LENGTH_SHORT).show();
                return false;
            });
        }

        CountItemsOrAndSizeTask countItemsOrAndSizeTask = new CountItemsOrAndSizeTask(c, itemsText, baseFile, forStorage);
        countItemsOrAndSizeTask.executeOnExecutor(executor);


        GenerateHashesTask hashGen = new GenerateHashesTask(baseFile, c, v);
        hashGen.executeOnExecutor(executor);

        /*Chart creation and data loading*/ {
            boolean isRightToLeft = c.getResources().getBoolean(R.bool.is_right_to_left);
            boolean isDarkTheme = appTheme.getMaterialDialogTheme() == Theme.DARK;
            PieChart chart = (PieChart) v.findViewById(R.id.chart);

            chart.setTouchEnabled(false);
            chart.setDrawEntryLabels(false);
            chart.setDescription(null);
            chart.setNoDataText(c.getString(R.string.loading));
            chart.setRotationAngle(!isRightToLeft? 0f:180f);
            chart.setHoleColor(Color.TRANSPARENT);
            chart.setCenterTextColor(isDarkTheme? Color.WHITE:Color.BLACK);

            chart.getLegend().setEnabled(true);
            chart.getLegend().setForm(Legend.LegendForm.CIRCLE);
            chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            chart.getLegend().setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            chart.getLegend().setTextColor(isDarkTheme? Color.WHITE:Color.BLACK);

            chart.animateY(1000);

            if(forStorage) {
                final String[] LEGENDS = new String[]{c.getString(R.string.used), c.getString(R.string.free)};
                final int[] COLORS = {Utils.getColor(c, R.color.piechart_red), Utils.getColor(c, R.color.piechart_green)};

                long totalSpace = baseFile.getTotal(c),
                        freeSpace = baseFile.getUsableSpace(),
                        usedSpace = totalSpace - freeSpace;

                List<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry(usedSpace, LEGENDS[0]));
                entries.add(new PieEntry(freeSpace, LEGENDS[1]));

                PieDataSet set = new PieDataSet(entries, null);
                set.setColors(COLORS);
                set.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
                set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
                set.setSliceSpace(5f);
                set.setAutomaticallyDisableSliceSpacing(true);
                set.setValueLinePart2Length(1.05f);
                set.setSelectionShift(0f);

                PieData pieData = new PieData(set);
                pieData.setValueFormatter(new SizeFormatter(c));
                pieData.setValueTextColor(isDarkTheme? Color.WHITE:Color.BLACK);

                String totalSpaceFormatted = Formatter.formatFileSize(c, totalSpace);

                chart.setCenterText(new SpannableString(c.getString(R.string.total) + "\n" + totalSpaceFormatted));
                chart.setData(pieData);
            } else {
                LoadFolderSpaceDataTask loadFolderSpaceDataTask = new LoadFolderSpaceDataTask(c, appTheme, chart, baseFile);
                loadFolderSpaceDataTask.executeOnExecutor(executor);
            }

            chart.invalidate();
        }

        if(!forStorage && showPermissions) {
            final MainFragment main = ((MainActivity) base).mainFragment;
            AppCompatButton appCompatButton = (AppCompatButton) v.findViewById(R.id.permissionsButton);
            appCompatButton.setAllCaps(true);

            final View permissionsTable = v.findViewById(R.id.permtable);
            final View button = v.findViewById(R.id.set);
            if (isRoot && permissions.length() > 6) {
                appCompatButton.setVisibility(View.VISIBLE);
                appCompatButton.setOnClickListener(v15 -> {
                    if (permissionsTable.getVisibility() == View.GONE) {
                        permissionsTable.setVisibility(View.VISIBLE);
                        button.setVisibility(View.VISIBLE);
                        setPermissionsDialog(permissionsTable, button, baseFile, permissions, c,
                                main);
                    } else {
                        button.setVisibility(View.GONE);
                        permissionsTable.setVisibility(View.GONE);
                    }
                });
            }
        }

        builder.customView(v, true);
        builder.positiveText(base.getResources().getString(R.string.ok));
        builder.positiveColor(accentColor);
        builder.dismissListener(dialog -> executor.shutdown());

        MaterialDialog materialDialog = builder.build();
        materialDialog.show();
        materialDialog.getActionButton(DialogAction.NEGATIVE).setEnabled(false);

        /*
        View bottomSheet = c.findViewById(R.id.design_bottom_sheet);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setPeekHeight(BottomSheetBehavior.STATE_DRAGGING);
        */
    }

    public static class SizeFormatter implements IValueFormatter {

        private Context context;

        public SizeFormatter(Context c) {
            context = c;
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex,
                                        ViewPortHandler viewPortHandler) {
            String prefix = entry.getData() != null && entry.getData() instanceof String?
                    (String) entry.getData():"";

            return prefix + Formatter.formatFileSize(context, (long) value);
        }
    }

    public static void showCloudDialog(final MainActivity mainActivity, AppTheme appTheme, final OpenMode openMode) {
        int accentColor = mainActivity.getColorPreference().getColor(ColorUsage.ACCENT);
        final MaterialDialog.Builder builder = new MaterialDialog.Builder(mainActivity);

        switch (openMode) {
            case DROPBOX:
                builder.title(mainActivity.getResources().getString(R.string.cloud_dropbox));
                break;
            case BOX:
                builder.title(mainActivity.getResources().getString(R.string.cloud_box));
                break;
            case GDRIVE:
                builder.title(mainActivity.getResources().getString(R.string.cloud_drive));
                break;
            case ONEDRIVE:
                builder.title(mainActivity.getResources().getString(R.string.cloud_onedrive));
                break;
        }

        builder.theme(appTheme.getMaterialDialogTheme());
        builder.content(mainActivity.getResources().getString(R.string.cloud_remove));

        builder.positiveText(mainActivity.getResources().getString(R.string.yes));
        builder.positiveColor(accentColor);
        builder.negativeText(mainActivity.getResources().getString(R.string.no));
        builder.negativeColor(accentColor);

        builder.onPositive((dialog, which) -> mainActivity.deleteConnection(openMode));

        builder.onNegative((dialog, which) -> dialog.cancel());

        builder.show();
    }

    public static void showEncryptWarningDialog(final Intent intent, final MainFragment main,
                                                AppTheme appTheme,
                                                final EncryptDecryptUtils.EncryptButtonCallbackInterface
                                                        encryptButtonCallbackInterface) {
        int accentColor = main.getMainActivity().getColorPreference().getColor(ColorUsage.ACCENT);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(main.getContext());
        final MaterialDialog.Builder builder = new MaterialDialog.Builder(main.getActivity());
        builder.title(main.getResources().getString(R.string.warning));
        builder.content(main.getResources().getString(R.string.crypt_warning_key));
        builder.theme(appTheme.getMaterialDialogTheme());
        builder.negativeText(main.getResources().getString(R.string.warning_never_show));
        builder.positiveText(main.getResources().getString(R.string.warning_confirm));
        builder.positiveColor(accentColor);

        builder.onPositive((dialog, which) -> {
            try {
                encryptButtonCallbackInterface.onButtonPressed(intent);
            } catch (Exception e) {
                e.printStackTrace();

                Toast.makeText(main.getActivity(),
                        main.getResources().getString(R.string.crypt_encryption_fail),
                        Toast.LENGTH_LONG).show();
            }
        });

        builder.onNegative((dialog, which) -> {
            preferences.edit().putBoolean(PrefFrag.PREFERENCE_CRYPT_WARNING_REMEMBER, true).apply();
            try {
                encryptButtonCallbackInterface.onButtonPressed(intent);
            } catch (Exception e) {
                e.printStackTrace();

                Toast.makeText(main.getActivity(),
                        main.getResources().getString(R.string.crypt_encryption_fail),
                        Toast.LENGTH_LONG).show();
            }
        });

        builder.show();
    }

    public static void showEncryptAuthenticateDialog(final Context c, final Intent intent,
                                                     final MainActivity main, AppTheme appTheme,
                                                     final EncryptDecryptUtils.EncryptButtonCallbackInterface
                                                             encryptButtonCallbackInterface) {
        int accentColor = main.getColorPreference().getColor(ColorUsage.ACCENT);
        MaterialDialog.Builder builder = new MaterialDialog.Builder(c);
        builder.title(main.getResources().getString(R.string.crypt_encrypt));

        View rootView = View.inflate(c, R.layout.dialog_encrypt_authenticate, null);

        final AppCompatEditText passwordEditText = (AppCompatEditText)
                rootView.findViewById(R.id.edit_text_dialog_encrypt_password);
        final AppCompatEditText passwordConfirmEditText = (AppCompatEditText)
                rootView.findViewById(R.id.edit_text_dialog_encrypt_password_confirm);

        builder.customView(rootView, true);

        builder.positiveText(c.getString(R.string.ok));
        builder.negativeText(c.getString(R.string.cancel));
        builder.theme(appTheme.getMaterialDialogTheme());
        builder.positiveColor(accentColor);
        builder.negativeColor(accentColor);

        builder.onNegative((dialog, which) -> dialog.cancel());

        builder.onPositive((dialog, which) -> {
            if (TextUtils.isEmpty(passwordEditText.getText()) ||
                    TextUtils.isEmpty(passwordConfirmEditText.getText())) {
                dialog.cancel();
                return;
            }

            try {
                encryptButtonCallbackInterface.onButtonPressed(intent, passwordEditText.getText().toString());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(c, c.getString(R.string.crypt_encryption_fail), Toast.LENGTH_LONG).show();
            }
        });

        builder.show();
    }

    @RequiresApi(api = M)
    public static void showDecryptFingerprintDialog(final Context c, MainActivity main,
                                                    final Intent intent, AppTheme appTheme,
                                                    final EncryptDecryptUtils.DecryptButtonCallbackInterface
                                                            decryptButtonCallbackInterface) throws CryptException {

        int accentColor = main.getColorPreference().getColor(ColorUsage.ACCENT);
        MaterialDialog.Builder builder = new MaterialDialog.Builder(c);
        builder.title(c.getString(R.string.crypt_decrypt));

        View rootView = View.inflate(c, R.layout.dialog_decrypt_fingerprint_authentication, null);

        Button cancelButton = (Button) rootView.findViewById(R.id.button_decrypt_fingerprint_cancel);
        cancelButton.setTextColor(accentColor);
        builder.customView(rootView, true);
        builder.canceledOnTouchOutside(false);

        builder.theme(appTheme.getMaterialDialogTheme());

        final MaterialDialog dialog = builder.show();
        cancelButton.setOnClickListener(v -> dialog.cancel());

        FingerprintManager manager = (FingerprintManager) c.getSystemService(Context.FINGERPRINT_SERVICE);
        FingerprintManager.CryptoObject object = new
                FingerprintManager.CryptoObject(CryptUtil.initCipher(c));

        FingerprintHandler handler = new FingerprintHandler(c, intent, dialog, decryptButtonCallbackInterface);
        handler.authenticate(manager, object);
    }

    public static void showDecryptDialog(Context c, final MainActivity main, final Intent intent,
                                         AppTheme appTheme, final String password,
                                         final EncryptDecryptUtils.DecryptButtonCallbackInterface
                                          decryptButtonCallbackInterface) {
        int accentColor = main.getColorPreference().getColor(ColorUsage.ACCENT);
        MaterialDialog.Builder builder = new MaterialDialog.Builder(c);
        builder.title(c.getString(R.string.crypt_decrypt));
        builder.input(c.getString(R.string.authenticate_password), "", false, (dialog, input) -> {});
        builder.theme(appTheme.getMaterialDialogTheme());
        builder.positiveText(c.getString(R.string.ok));
        builder.negativeText(c.getString(R.string.cancel));
        builder.positiveColor(accentColor);
        builder.negativeColor(accentColor);
        builder.onPositive((dialog, which) -> {
            EditText editText = dialog.getInputEditText();

            if (editText.getText().toString().equals(password))
                decryptButtonCallbackInterface.confirm(intent);
            else decryptButtonCallbackInterface.failed();
        });
        builder.onNegative((dialog, which) -> dialog.cancel());
        builder.show();
    }

    public static void showSMBHelpDialog(Context m, int accentColor){
        MaterialDialog.Builder b=new MaterialDialog.Builder(m);
        b.content(m.getText(R.string.smb_instructions));
        b.positiveText(R.string.doit);
        b.positiveColor(accentColor);
        b.build().show();
    }

    public static void showPackageDialog(final SharedPreferences sharedPrefs, final File f, final MainActivity m) {
        int accentColor = m.getColorPreference().getColor(ColorUsage.ACCENT);
        MaterialDialog.Builder mat = new MaterialDialog.Builder(m);
        mat.title(R.string.packageinstaller).content(R.string.pitext)
                .positiveText(R.string.install)
                .negativeText(R.string.view)
                .neutralText(R.string.cancel)
                .positiveColor(accentColor)
                .negativeColor(accentColor)
                .neutralColor(accentColor)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        boolean useNewStack = sharedPrefs.getBoolean(PrefFrag.PREFERENCE_TEXTEDITOR_NEWSTACK, false);
                        FileUtils.openunknown(f, m, false, useNewStack);
                    }

                    @Override
                    public void onNegative(MaterialDialog materialDialog) {
                        m.openZip(f.getPath());
                    }
                })
                .theme(m.getAppTheme().getMaterialDialogTheme())
                .build()
                .show();
    }


    public static void showArchiveDialog(final File f, final MainActivity m) {
        int accentColor = m.getColorPreference().getColor(ColorUsage.ACCENT);
        MaterialDialog.Builder mat = new MaterialDialog.Builder(m);
        mat.title(R.string.archive)
                .content(R.string.archtext)
                .positiveText(R.string.extract)
                .negativeText(R.string.view)
                .neutralText(R.string.cancel)
                .positiveColor(accentColor)
                .negativeColor(accentColor)
                .neutralColor(accentColor)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        m. mainActivityHelper.extractFile(f);
                    }

                    @Override
                    public void onNegative(MaterialDialog materialDialog) {
                        //m.addZipViewTab(f.getPath());
                        if (f.getName().toLowerCase().endsWith(".rar"))
                            m.openRar(Uri.fromFile(f).toString());
                        else
                            m.openZip(Uri.fromFile(f).toString());
                    }
                });
        if (m.getAppTheme().equals(AppTheme.DARK)) mat.theme(Theme.DARK);
        MaterialDialog b = mat.build();

        if (!f.getName().toLowerCase().endsWith(".rar") && !f.getName().toLowerCase().endsWith(".jar") && !f.getName().toLowerCase().endsWith(".apk") && !f.getName().toLowerCase().endsWith(".zip"))
            b.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
        b.show();
    }

    public static void showCompressDialog(final MainActivity m, final ArrayList<HybridFileParcelable> b, final String current) {
        int accentColor = m.getColorPreference().getColor(ColorUsage.ACCENT);
        MaterialDialog.Builder a = new MaterialDialog.Builder(m);
        a.input(m.getResources().getString(R.string.enterzipname), ".zip", false, (materialDialog, charSequence) -> {});
        a.widgetColor(accentColor);
        a.theme(m.getAppTheme().getMaterialDialogTheme());
        a.title(m.getResources().getString(R.string.enterzipname));
        a.positiveText(R.string.create);
        a.positiveColor(accentColor);
        a.onPositive((materialDialog, dialogAction) -> {
            if (materialDialog.getInputEditText().getText().toString().equals(".zip"))
                Toast.makeText(m, m.getResources().getString(R.string.no_name), Toast.LENGTH_SHORT).show();
            else {
                String name = current + "/" + materialDialog.getInputEditText().getText().toString();
                m.mainActivityHelper.compressFiles(new File(name), b);
            }
        });
        a.negativeText(m.getResources().getString(R.string.cancel));
        a.negativeColor(accentColor);
        final MaterialDialog materialDialog = a.build();
        materialDialog.show();

        // place cursor at the starting of edit text by posting a runnable to edit text
        // this is done because in case android has not populated the edit text layouts yet, it'll
        // reset calls to selection if not posted in message queue
        materialDialog.getInputEditText().post(() -> materialDialog.getInputEditText().setSelection(0));
    }

    public static void showSortDialog(final MainFragment m, AppTheme appTheme, final SharedPreferences sharedPref) {
        int accentColor = m.getMainActivity().getColorPreference().getColor(ColorUsage.ACCENT);
        String[] sort = m.getResources().getStringArray(R.array.sortby);
        int current = Integer.parseInt(sharedPref.getString("sortby", "0"));
        MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.theme(appTheme.getMaterialDialogTheme());
        a.items(sort).itemsCallbackSingleChoice(current > 3 ? current - 4 : current,
                (dialog, view, which, text) -> true);
        a.negativeText(R.string.ascending).positiveColor(accentColor);
        a.positiveText(R.string.descending).negativeColor(accentColor);
        a.onNegative((dialog, which) -> {
            sharedPref.edit().putString("sortby", "" + dialog.getSelectedIndex()).commit();
            m.getSortModes();
            m.updateList();
            dialog.dismiss();
        });

        a.onPositive((dialog, which) -> {
            sharedPref.edit().putString("sortby", "" + (dialog.getSelectedIndex() + 4)).commit();
            m.getSortModes();
            m.updateList();
            dialog.dismiss();
        });
        a.title(R.string.sortby);
        a.build().show();
    }

    public static void showSortDialog(final AppsListFragment m, AppTheme appTheme) {
        int accentColor = ((ThemedActivity) m.getActivity()).getColorPreference().getColor(ColorUsage.ACCENT);
        String[] sort = m.getResources().getStringArray(R.array.sortbyApps);
        int current = Integer.parseInt(m.Sp.getString("sortbyApps", "0"));
        MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.theme(appTheme.getMaterialDialogTheme());
        a.items(sort).itemsCallbackSingleChoice(current > 2 ? current - 3 : current,
                (dialog, view, which, text) -> true);
        a.negativeText(R.string.ascending).positiveColor(accentColor);
        a.positiveText(R.string.descending).negativeColor(accentColor);
        a.onNegative((dialog, which) -> {
            m.Sp.edit().putString("sortbyApps", "" + dialog.getSelectedIndex()).commit();
            m.getSortModes();
            m.getLoaderManager().restartLoader(AppsListFragment.ID_LOADER_APP_LIST, null, m);
            dialog.dismiss();
        });

        a.onPositive((dialog, which) -> {
            m.Sp.edit().putString("sortbyApps", "" + (dialog.getSelectedIndex() + 3)).commit();
            m.getSortModes();
            m.getLoaderManager().restartLoader(AppsListFragment.ID_LOADER_APP_LIST, null, m);
            dialog.dismiss();
        });

        a.title(R.string.sortby);
        a.build().show();
    }


    public static void showHistoryDialog(final DataUtils dataUtils, SharedPreferences sharedPrefs,
                                         final MainFragment m, AppTheme appTheme) {
        int accentColor = m.getMainActivity().getColorPreference().getColor(ColorUsage.ACCENT);
        final MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.positiveText(R.string.cancel);
        a.positiveColor(accentColor);
        a.negativeText(R.string.clear);
        a.negativeColor(accentColor);
        a.title(R.string.history);
        a.onNegative((dialog, which) -> dataUtils.clearHistory());
        a.theme(appTheme.getMaterialDialogTheme());

        a.autoDismiss(true);
        HiddenAdapter adapter = new HiddenAdapter(m.getActivity(), m, sharedPrefs, R.layout.bookmarkrow,
                toHybridFileArrayList(dataUtils.getHistory()), null, true);
        a.adapter(adapter, null);

        MaterialDialog x= a.build();
        adapter.updateDialog(x);
        x.show();
    }

    public static void showHiddenDialog(DataUtils dataUtils, SharedPreferences sharedPrefs,
                                        final MainFragment m, AppTheme appTheme) {
        int accentColor = m.getMainActivity().getColorPreference().getColor(ColorUsage.ACCENT);
        final MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
        a.positiveText(R.string.cancel);
        a.positiveColor(accentColor);
        a.title(R.string.hiddenfiles);
        a.theme(appTheme.getMaterialDialogTheme());
        a.autoDismiss(true);
        HiddenAdapter adapter = new HiddenAdapter(m.getActivity(), m, sharedPrefs, R.layout.bookmarkrow,
                FileUtils.toHybridFileConcurrentRadixTree(dataUtils.getHiddenFiles()), null, false);
        a.adapter(adapter, null);
        a.dividerColor(Color.GRAY);
        MaterialDialog x= a.build();
        adapter.updateDialog(x);
        x.show();

    }

    public static void setPermissionsDialog(final View v, View but, final HybridFile file,
                                     final String f, final Context context, final MainFragment mainFrag) {
        final CheckBox readown = (CheckBox) v.findViewById(R.id.creadown);
        final CheckBox readgroup = (CheckBox) v.findViewById(R.id.creadgroup);
        final CheckBox readother = (CheckBox) v.findViewById(R.id.creadother);
        final CheckBox writeown = (CheckBox) v.findViewById(R.id.cwriteown);
        final CheckBox writegroup = (CheckBox) v.findViewById(R.id.cwritegroup);
        final CheckBox writeother = (CheckBox) v.findViewById(R.id.cwriteother);
        final CheckBox exeown = (CheckBox) v.findViewById(R.id.cexeown);
        final CheckBox exegroup = (CheckBox) v.findViewById(R.id.cexegroup);
        final CheckBox exeother = (CheckBox) v.findViewById(R.id.cexeother);
        String perm = f;
        if (perm.length() < 6) {
            v.setVisibility(View.GONE);
            but.setVisibility(View.GONE);
            Toast.makeText(context, R.string.not_allowed, Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<Boolean[]> arrayList = FileUtils.parse(perm);
        Boolean[] read = arrayList.get(0);
        Boolean[] write = arrayList.get(1);
        final Boolean[] exe = arrayList.get(2);
        readown.setChecked(read[0]);
        readgroup.setChecked(read[1]);
        readother.setChecked(read[2]);
        writeown.setChecked(write[0]);
        writegroup.setChecked(write[1]);
        writeother.setChecked(write[2]);
        exeown.setChecked(exe[0]);
        exegroup.setChecked(exe[1]);
        exeother.setChecked(exe[2]);
        but.setOnClickListener(v1 -> {
            int a = 0, b = 0, c = 0;
            if (readown.isChecked()) a = 4;
            if (writeown.isChecked()) b = 2;
            if (exeown.isChecked()) c = 1;
            int owner = a + b + c;
            int d = 0;
            int e = 0;
            int f1 = 0;
            if (readgroup.isChecked()) d = 4;
            if (writegroup.isChecked()) e = 2;
            if (exegroup.isChecked()) f1 = 1;
            int group = d + e + f1;
            int g = 0, h = 0, i = 0;
            if (readother.isChecked()) g = 4;
            if (writeother.isChecked()) h = 2;
            if (exeother.isChecked()) i = 1;
            int other = g + h + i;
            String finalValue = owner + "" + group + "" + other;

            String command = "chmod " + finalValue + " " + file.getPath();
            if (file.isDirectory())
                command = "chmod -R " + finalValue + " \"" + file.getPath() + "\"";

            try {
                RootHelper.runShellCommand(command, (commandCode, exitCode, output) -> {
                    if (exitCode < 0) {
                        Toast.makeText(context, mainFrag.getString(R.string.operationunsuccesful),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(context,
                                mainFrag.getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
                    }
                });
                mainFrag.updateList();
            } catch (RootNotPermittedException e1) {
                Toast.makeText(context, mainFrag.getResources().getString(R.string.rootfailure),
                        Toast.LENGTH_LONG).show();
                e1.printStackTrace();
            }

        });
    }

    public static void showChangePathsDialog(final WeakReference<MainActivity> m, final SharedPreferences prefs) {
        final MaterialDialog.Builder a = new MaterialDialog.Builder(m.get());
        a.input(null, m.get().getCurrentMainFragment().getCurrentPath(), false,
                (dialog, charSequence) -> {
                    boolean isAccessible = FileUtils.isPathAccesible(charSequence.toString(), prefs);
                    dialog.getActionButton(DialogAction.POSITIVE).setEnabled(isAccessible);
                });

        a.alwaysCallInputCallback();

        MainActivity mainActivity = m.get();

        int accentColor = mainActivity.getColorPreference().getColor(ColorUsage.ACCENT);

        a.widgetColor(accentColor);

        a.theme(m.get().getAppTheme().getMaterialDialogTheme());
        a.title(R.string.enterpath);

        a.positiveText(R.string.go);
        a.positiveColor(accentColor);

        a.negativeText(R.string.cancel);
        a.negativeColor(accentColor);

        a.onPositive((dialog, which) -> {
            m.get().getCurrentMainFragment().loadlist(dialog.getInputEditText().getText().toString(),
                    false, OpenMode.UNKNOWN);
        });

        a.show();
    }

}
