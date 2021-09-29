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

package com.amaze.filemanager.ui.dialogs;

import static android.os.Build.VERSION_CODES.M;
import static com.amaze.filemanager.filesystem.files.FileUtils.toHybridFileArrayList;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SORTBY_ONLY_THIS;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.afollestad.materialdialogs.internal.MDButton;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.HiddenAdapter;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.asynchronous.asynctasks.CountItemsOrAndSizeTask;
import com.amaze.filemanager.asynchronous.asynctasks.GenerateHashesTask;
import com.amaze.filemanager.asynchronous.asynctasks.LoadFolderSpaceDataTask;
import com.amaze.filemanager.asynchronous.services.EncryptService;
import com.amaze.filemanager.database.SortHandler;
import com.amaze.filemanager.database.models.explorer.Sort;
import com.amaze.filemanager.databinding.DialogSigninWithGoogleBinding;
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.FileProperties;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.files.CryptUtil;
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.root.ChangeFilePermissionsCommand;
import com.amaze.filemanager.ui.ExtensionsKt;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.ui.fragments.MainFragment;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.ui.views.WarnableTextInputLayout;
import com.amaze.filemanager.ui.views.WarnableTextInputValidator;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.FingerprintHandler;
import com.amaze.filemanager.utils.SimpleTextWatcher;
import com.amaze.filemanager.utils.Utils;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.android.material.textfield.TextInputEditText;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatButton;
import androidx.preference.PreferenceManager;

/**
 * Here are a lot of function that create material dialogs
 *
 * @author Emmanuel on 17/5/2017, at 13:27.
 */
public class GeneralDialogCreation {
  private static final String TAG = "GeneralDialogCreation";

  public static MaterialDialog showBasicDialog(
      ThemedActivity themedActivity,
      @StringRes int content,
      @StringRes int title,
      @StringRes int postiveText,
      @StringRes int negativeText) {
    int accentColor = themedActivity.getAccent();
    MaterialDialog.Builder a =
        new MaterialDialog.Builder(themedActivity)
            .content(content)
            .widgetColor(accentColor)
            .theme(themedActivity.getAppTheme().getMaterialDialogTheme())
            .title(title)
            .positiveText(postiveText)
            .positiveColor(accentColor)
            .negativeText(negativeText)
            .negativeColor(accentColor);
    return a.build();
  }

  public static MaterialDialog showNameDialog(
      final MainActivity m,
      String hint,
      String prefill,
      String title,
      String positiveButtonText,
      String neutralButtonText,
      String negativeButtonText,
      MaterialDialog.SingleButtonCallback positiveButtonAction,
      WarnableTextInputValidator.OnTextValidate validator) {
    int accentColor = m.getAccent();
    MaterialDialog.Builder builder = new MaterialDialog.Builder(m);

    View dialogView = m.getLayoutInflater().inflate(R.layout.dialog_singleedittext, null);
    EditText textfield = dialogView.findViewById(R.id.singleedittext_input);
    textfield.setHint(hint);
    textfield.setText(prefill);

    WarnableTextInputLayout tilTextfield =
        dialogView.findViewById(R.id.singleedittext_warnabletextinputlayout);

    dialogView.post(() -> ExtensionsKt.openKeyboard(textfield, m.getApplicationContext()));

    builder
        .customView(dialogView, false)
        .widgetColor(accentColor)
        .theme(m.getAppTheme().getMaterialDialogTheme())
        .title(title)
        .positiveText(positiveButtonText)
        .onPositive(positiveButtonAction);

    if (neutralButtonText != null) {
      builder.neutralText(neutralButtonText);
    }

    if (negativeButtonText != null) {
      builder.negativeText(negativeButtonText);
      builder.negativeColor(accentColor);
    }

    MaterialDialog dialog = builder.show();

    WarnableTextInputValidator textInputValidator =
        new WarnableTextInputValidator(
            builder.getContext(),
            textfield,
            tilTextfield,
            dialog.getActionButton(DialogAction.POSITIVE),
            validator);

    if (!TextUtils.isEmpty(prefill)) textInputValidator.afterTextChanged(textfield.getText());

    return dialog;
  }

  @SuppressWarnings("ConstantConditions")
  public static void deleteFilesDialog(
      @NonNull final Context context,
      @NonNull final MainActivity mainActivity,
      @NonNull final List<LayoutElementParcelable> positions,
      @NonNull AppTheme appTheme) {

    final ArrayList<HybridFileParcelable> itemsToDelete = new ArrayList<>();
    int accentColor = mainActivity.getAccent();

    // Build dialog with custom view layout and accent color.
    MaterialDialog dialog =
        new MaterialDialog.Builder(context)
            .title(context.getString(R.string.dialog_delete_title))
            .customView(R.layout.dialog_delete, true)
            .theme(appTheme.getMaterialDialogTheme())
            .negativeText(context.getString(R.string.cancel).toUpperCase())
            .positiveText(context.getString(R.string.delete).toUpperCase())
            .positiveColor(accentColor)
            .negativeColor(accentColor)
            .onPositive(
                (dialog1, which) -> {
                  Toast.makeText(context, context.getString(R.string.deleting), Toast.LENGTH_SHORT)
                      .show();
                  mainActivity.mainActivityHelper.deleteFiles(itemsToDelete);
                })
            .build();

    // Get views from custom layout to set text values.
    final TextView categoryDirectories =
        dialog.getCustomView().findViewById(R.id.category_directories);
    final TextView categoryFiles = dialog.getCustomView().findViewById(R.id.category_files);
    final TextView listDirectories = dialog.getCustomView().findViewById(R.id.list_directories);
    final TextView listFiles = dialog.getCustomView().findViewById(R.id.list_files);
    final TextView total = dialog.getCustomView().findViewById(R.id.total);

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

        listFiles.setText(context.getString(R.string.loading));
        listDirectories.setText(context.getString(R.string.loading));
        total.setText(context.getString(R.string.loading));
      }

      @Override
      protected Void doInBackground(Void... params) {

        for (int i = 0; i < positions.size(); i++) {
          final LayoutElementParcelable layoutElement = positions.get(i);
          itemsToDelete.add(layoutElement.generateBaseFile());

          // Build list of directories to delete.
          if (layoutElement.isDirectory) {
            // Don't add newline between category and list.
            if (counterDirectories != 0) {
              directories.append("\n");
            }

            long sizeDirectory = layoutElement.generateBaseFile().folderSize(context);

            directories
                .append(++counterDirectories)
                .append(". ")
                .append(layoutElement.title)
                .append(" (")
                .append(Formatter.formatFileSize(context, sizeDirectory))
                .append(")");
            sizeTotal += sizeDirectory;
            // Build list of files to delete.
          } else {
            // Don't add newline between category and list.
            if (counterFiles != 0) {
              files.append("\n");
            }

            files
                .append(++counterFiles)
                .append(". ")
                .append(layoutElement.title)
                .append(" (")
                .append(layoutElement.size)
                .append(")");
            sizeTotal += layoutElement.longSize;
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

        updateViews(
            tempSizeTotal,
            tempFilesStringBuilder,
            tempDirectoriesStringBuilder,
            tempCounterFiles,
            tempCounterDirectories);
      }

      @Override
      protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        updateViews(sizeTotal, files, directories, counterFiles, counterDirectories);
      }

      private void updateViews(
          long tempSizeTotal,
          StringBuilder filesStringBuilder,
          StringBuilder directoriesStringBuilder,
          int... values) {

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

        if (tempCounterDirectories != 0 || tempCounterFiles != 0) {
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
          StringBuilder builderTotal =
              new StringBuilder()
                  .append(context.getString(R.string.total))
                  .append(" ")
                  .append(Formatter.formatFileSize(context, tempSizeTotal));
          total.setText(builderTotal);
          if (total.getVisibility() != View.VISIBLE) total.setVisibility(View.VISIBLE);
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

  public static void showPropertiesDialogWithPermissions(
      @NonNull HybridFileParcelable baseFile,
      @Nullable final String permissions,
      @NonNull MainActivity themedActivity,
      @NonNull MainFragment mainFragment,
      boolean isRoot,
      @NonNull AppTheme appTheme) {
    showPropertiesDialog(
        baseFile, themedActivity, mainFragment, permissions, isRoot, appTheme, false);
  }

  public static void showPropertiesDialogWithoutPermissions(
      @NonNull final HybridFileParcelable f,
      @NonNull ThemedActivity themedActivity,
      @NonNull AppTheme appTheme) {
    showPropertiesDialog(f, themedActivity, null, null, false, appTheme, false);
  }

  public static void showPropertiesDialogForStorage(
      @NonNull final HybridFileParcelable f,
      @NonNull MainActivity themedActivity,
      @NonNull AppTheme appTheme) {
    showPropertiesDialog(f, themedActivity, null, null, false, appTheme, true);
  }

  private static void showPropertiesDialog(
      @NonNull final HybridFileParcelable baseFile,
      @NonNull ThemedActivity themedActivity,
      @Nullable MainFragment mainFragment,
      @Nullable final String permissions,
      boolean isRoot,
      @NonNull AppTheme appTheme,
      boolean forStorage) {
    final ExecutorService executor = Executors.newFixedThreadPool(3);
    final Context c = themedActivity.getApplicationContext();
    int accentColor = themedActivity.getAccent();
    long last = baseFile.getDate();
    final String date = Utils.getDate(themedActivity, last),
        items = c.getString(R.string.calculating),
        name = baseFile.getName(c),
        parent = baseFile.getReadablePath(baseFile.getParent(c));

    File nomediaFile =
        baseFile.isDirectory() ? new File(baseFile.getPath() + "/" + FileUtils.NOMEDIA_FILE) : null;

    MaterialDialog.Builder builder = new MaterialDialog.Builder(themedActivity);
    builder.title(c.getString(R.string.properties));
    builder.theme(appTheme.getMaterialDialogTheme());

    View v = themedActivity.getLayoutInflater().inflate(R.layout.properties_dialog, null);
    TextView itemsText = v.findViewById(R.id.t7);
    CheckBox nomediaCheckBox = v.findViewById(R.id.nomediacheckbox);

    /*View setup*/
    {
      TextView mNameTitle = v.findViewById(R.id.title_name);
      mNameTitle.setTextColor(accentColor);

      TextView mDateTitle = v.findViewById(R.id.title_date);
      mDateTitle.setTextColor(accentColor);

      TextView mSizeTitle = v.findViewById(R.id.title_size);
      mSizeTitle.setTextColor(accentColor);

      TextView mLocationTitle = v.findViewById(R.id.title_location);
      mLocationTitle.setTextColor(accentColor);

      TextView md5Title = v.findViewById(R.id.title_md5);
      md5Title.setTextColor(accentColor);

      TextView sha256Title = v.findViewById(R.id.title_sha256);
      sha256Title.setTextColor(accentColor);

      ((TextView) v.findViewById(R.id.t5)).setText(name);
      ((TextView) v.findViewById(R.id.t6)).setText(parent);
      itemsText.setText(items);
      ((TextView) v.findViewById(R.id.t8)).setText(date);

      if (baseFile.isDirectory() && baseFile.isLocal()) {
        nomediaCheckBox.setVisibility(View.VISIBLE);
        if (nomediaFile != null) {
          nomediaCheckBox.setChecked(nomediaFile.exists());
        }
      }

      LinearLayout mNameLinearLayout = v.findViewById(R.id.properties_dialog_name);
      LinearLayout mLocationLinearLayout = v.findViewById(R.id.properties_dialog_location);
      LinearLayout mSizeLinearLayout = v.findViewById(R.id.properties_dialog_size);
      LinearLayout mDateLinearLayout = v.findViewById(R.id.properties_dialog_date);

      // setting click listeners for long press
      mNameLinearLayout.setOnLongClickListener(
          v1 -> {
            FileUtils.copyToClipboard(c, name);
            Toast.makeText(
                    c,
                    c.getString(R.string.name)
                        + " "
                        + c.getString(R.string.properties_copied_clipboard),
                    Toast.LENGTH_SHORT)
                .show();
            return false;
          });
      mLocationLinearLayout.setOnLongClickListener(
          v12 -> {
            FileUtils.copyToClipboard(c, parent);
            Toast.makeText(
                    c,
                    c.getString(R.string.location)
                        + " "
                        + c.getString(R.string.properties_copied_clipboard),
                    Toast.LENGTH_SHORT)
                .show();
            return false;
          });
      mSizeLinearLayout.setOnLongClickListener(
          v13 -> {
            FileUtils.copyToClipboard(c, items);
            Toast.makeText(
                    c,
                    c.getString(R.string.size)
                        + " "
                        + c.getString(R.string.properties_copied_clipboard),
                    Toast.LENGTH_SHORT)
                .show();
            return false;
          });
      mDateLinearLayout.setOnLongClickListener(
          v14 -> {
            FileUtils.copyToClipboard(c, date);
            Toast.makeText(
                    c,
                    c.getString(R.string.date)
                        + " "
                        + c.getString(R.string.properties_copied_clipboard),
                    Toast.LENGTH_SHORT)
                .show();
            return false;
          });
    }

    CountItemsOrAndSizeTask countItemsOrAndSizeTask =
        new CountItemsOrAndSizeTask(c, itemsText, baseFile, forStorage);
    countItemsOrAndSizeTask.executeOnExecutor(executor);

    GenerateHashesTask hashGen = new GenerateHashesTask(baseFile, c, v);
    hashGen.executeOnExecutor(executor);

    /*Chart creation and data loading*/
    {
      boolean isRightToLeft = c.getResources().getBoolean(R.bool.is_right_to_left);
      boolean isDarkTheme = appTheme.getMaterialDialogTheme() == Theme.DARK;
      PieChart chart = v.findViewById(R.id.chart);

      chart.setTouchEnabled(false);
      chart.setDrawEntryLabels(false);
      chart.setDescription(null);
      chart.setNoDataText(c.getString(R.string.loading));
      chart.setRotationAngle(!isRightToLeft ? 0f : 180f);
      chart.setHoleColor(Color.TRANSPARENT);
      chart.setCenterTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);

      chart.getLegend().setEnabled(true);
      chart.getLegend().setForm(Legend.LegendForm.CIRCLE);
      chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
      chart.getLegend().setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
      chart.getLegend().setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);

      chart.animateY(1000);

      if (forStorage) {
        final String[] LEGENDS =
            new String[] {c.getString(R.string.used), c.getString(R.string.free)};
        final int[] COLORS = {
          Utils.getColor(c, R.color.piechart_red), Utils.getColor(c, R.color.piechart_green)
        };

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
        pieData.setValueTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);

        String totalSpaceFormatted = Formatter.formatFileSize(c, totalSpace);

        chart.setCenterText(
            new SpannableString(c.getString(R.string.total) + "\n" + totalSpaceFormatted));
        chart.setData(pieData);
      } else {
        LoadFolderSpaceDataTask loadFolderSpaceDataTask =
            new LoadFolderSpaceDataTask(c, appTheme, chart, baseFile);
        loadFolderSpaceDataTask.executeOnExecutor(executor);
      }

      chart.invalidate();
    }

    if (!forStorage && permissions != null && mainFragment != null) {
      AppCompatButton appCompatButton = v.findViewById(R.id.permissionsButton);
      appCompatButton.setAllCaps(true);

      final View permissionsTable = v.findViewById(R.id.permtable);
      final View button = v.findViewById(R.id.set);
      if (isRoot && permissions.length() > 6) {
        appCompatButton.setVisibility(View.VISIBLE);
        appCompatButton.setOnClickListener(
            v15 -> {
              if (permissionsTable.getVisibility() == View.GONE) {
                permissionsTable.setVisibility(View.VISIBLE);
                button.setVisibility(View.VISIBLE);
                setPermissionsDialog(
                    permissionsTable, button, baseFile, permissions, c, mainFragment);
              } else {
                button.setVisibility(View.GONE);
                permissionsTable.setVisibility(View.GONE);
              }
            });
      }
    }

    builder.customView(v, true);
    builder.positiveText(themedActivity.getString(R.string.ok));
    builder.positiveColor(accentColor);
    builder.dismissListener(dialog -> executor.shutdown());
    builder.onPositive(
        (dialog, which) -> {
          if (baseFile.isDirectory() && nomediaFile != null) {
            if (nomediaCheckBox.isChecked()) {
              // checkbox is checked, create .nomedia
              try {
                if (!nomediaFile.createNewFile()) {
                  // failed operation
                  Log.w(TAG, "'.nomedia' file creation in " + baseFile.getPath() + " failed!");
                }
              } catch (IOException e) {
                Log.e(TAG, "Error creating file", e);
              }
            } else {
              // checkbox is unchecked, delete .nomedia
              if (!nomediaFile.delete()) {
                // failed operation
                Log.w(TAG, "'.nomedia' file deletion in " + baseFile.getPath() + " failed!");
              }
            }
          }
        });

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
    public String getFormattedValue(
        float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
      String prefix =
          entry.getData() != null && entry.getData() instanceof String
              ? (String) entry.getData()
              : "";

      return prefix + Formatter.formatFileSize(context, (long) value);
    }
  }

  public static void showCloudDialog(
      final MainActivity mainActivity, AppTheme appTheme, final OpenMode openMode) {
    int accentColor = mainActivity.getAccent();
    final MaterialDialog.Builder builder = new MaterialDialog.Builder(mainActivity);

    switch (openMode) {
      case DROPBOX:
        builder.title(mainActivity.getString(R.string.cloud_dropbox));
        break;
      case BOX:
        builder.title(mainActivity.getString(R.string.cloud_box));
        break;
      case GDRIVE:
        builder.title(mainActivity.getString(R.string.cloud_drive));
        break;
      case ONEDRIVE:
        builder.title(mainActivity.getString(R.string.cloud_onedrive));
        break;
    }

    builder.theme(appTheme.getMaterialDialogTheme());
    builder.content(mainActivity.getString(R.string.cloud_remove));

    builder.positiveText(mainActivity.getString(R.string.yes));
    builder.positiveColor(accentColor);
    builder.negativeText(mainActivity.getString(R.string.no));
    builder.negativeColor(accentColor);

    builder.onPositive((dialog, which) -> mainActivity.deleteConnection(openMode));

    builder.onNegative((dialog, which) -> dialog.cancel());

    builder.show();
  }

  public static void showEncryptWarningDialog(
      final Intent intent,
      final MainFragment main,
      AppTheme appTheme,
      final EncryptDecryptUtils.EncryptButtonCallbackInterface encryptButtonCallbackInterface) {
    int accentColor = main.getMainActivity().getAccent();
    final SharedPreferences preferences =
        PreferenceManager.getDefaultSharedPreferences(main.getContext());
    final MaterialDialog.Builder builder = new MaterialDialog.Builder(main.getActivity());
    builder.title(main.getString(R.string.warning));
    builder.content(main.getString(R.string.crypt_warning_key));
    builder.theme(appTheme.getMaterialDialogTheme());
    builder.negativeText(main.getString(R.string.warning_never_show));
    builder.positiveText(main.getString(R.string.warning_confirm));
    builder.positiveColor(accentColor);

    builder.onPositive(
        (dialog, which) -> {
          try {
            encryptButtonCallbackInterface.onButtonPressed(intent);
          } catch (Exception e) {
            e.printStackTrace();

            Toast.makeText(
                    main.getActivity(),
                    main.getString(R.string.crypt_encryption_fail),
                    Toast.LENGTH_LONG)
                .show();
          }
        });

    builder.onNegative(
        (dialog, which) -> {
          preferences
              .edit()
              .putBoolean(PreferencesConstants.PREFERENCE_CRYPT_WARNING_REMEMBER, true)
              .apply();
          try {
            encryptButtonCallbackInterface.onButtonPressed(intent);
          } catch (Exception e) {
            e.printStackTrace();

            Toast.makeText(
                    main.getActivity(),
                    main.getString(R.string.crypt_encryption_fail),
                    Toast.LENGTH_LONG)
                .show();
          }
        });

    builder.show();
  }

  public static void showEncryptWithPresetPasswordSaveAsDialog(
      @NonNull final Context c,
      @NonNull final MainActivity main,
      @NonNull String password,
      @NonNull final Intent intent) {

    HybridFileParcelable intentParcelable = intent.getParcelableExtra(EncryptService.TAG_SOURCE);
    MaterialDialog saveAsDialog =
        showNameDialog(
            main,
            "",
            intentParcelable.getName(c).concat(CryptUtil.CRYPT_EXTENSION),
            c.getString(
                intentParcelable.isDirectory()
                    ? R.string.encrypt_folder_save_as
                    : R.string.encrypt_file_save_as),
            c.getString(R.string.ok),
            null,
            c.getString(R.string.cancel),
            (dialog, which) -> {
              EditText textfield = dialog.getCustomView().findViewById(R.id.singleedittext_input);
              intent.putExtra(EncryptService.TAG_ENCRYPT_TARGET, textfield.getText().toString());
              try {
                EncryptDecryptUtils.startEncryption(
                    c, intentParcelable.getPath(), password, intent);
              } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
                Toast.makeText(c, c.getString(R.string.crypt_encryption_fail), Toast.LENGTH_LONG)
                    .show();
              } finally {
                dialog.dismiss();
              }
            },
            (text) -> {
              if (text.length() < 1) {
                return new WarnableTextInputValidator.ReturnState(
                    WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.field_empty);
              }
              if (!text.endsWith(CryptUtil.CRYPT_EXTENSION)) {
                return new WarnableTextInputValidator.ReturnState(
                    WarnableTextInputValidator.ReturnState.STATE_ERROR,
                    R.string.encrypt_file_must_end_with_aze);
              }
              return new WarnableTextInputValidator.ReturnState();
            });
    saveAsDialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
  }

  public static void showEncryptAuthenticateDialog(
      final Context c,
      final Intent intent,
      final MainActivity main,
      AppTheme appTheme,
      final EncryptDecryptUtils.EncryptButtonCallbackInterface encryptButtonCallbackInterface) {

    int accentColor = main.getAccent();
    MaterialDialog.Builder builder = new MaterialDialog.Builder(c);
    builder.title(main.getString(R.string.crypt_encrypt));

    View rootView = View.inflate(c, R.layout.dialog_encrypt_authenticate, null);

    final TextInputEditText passwordEditText =
        rootView.findViewById(R.id.edit_text_dialog_encrypt_password);
    final TextInputEditText passwordConfirmEditText =
        rootView.findViewById(R.id.edit_text_dialog_encrypt_password_confirm);
    final TextInputEditText encryptSaveAsEditText =
        rootView.findViewById(R.id.edit_text_encrypt_save_as);

    WarnableTextInputLayout textInputLayoutPassword =
        rootView.findViewById(R.id.til_encrypt_password);
    WarnableTextInputLayout textInputLayoutPasswordConfirm =
        rootView.findViewById(R.id.til_encrypt_password_confirm);
    WarnableTextInputLayout textInputLayoutEncryptSaveAs =
        rootView.findViewById(R.id.til_encrypt_save_as);

    HybridFileParcelable intentParcelable = intent.getParcelableExtra(EncryptService.TAG_SOURCE);
    encryptSaveAsEditText.setText(intentParcelable.getName(c).concat(CryptUtil.CRYPT_EXTENSION));
    textInputLayoutEncryptSaveAs.setHint(
        intentParcelable.isDirectory()
            ? c.getString(R.string.encrypt_folder_save_as)
            : c.getString(R.string.encrypt_file_save_as));

    builder
        .customView(rootView, true)
        .positiveText(c.getString(R.string.ok))
        .negativeText(c.getString(R.string.cancel))
        .theme(appTheme.getMaterialDialogTheme())
        .positiveColor(accentColor)
        .negativeColor(accentColor)
        .autoDismiss(false)
        .onNegative((dialog, which) -> dialog.cancel())
        .onPositive(
            (dialog, which) -> {
              intent.putExtra(
                  EncryptService.TAG_ENCRYPT_TARGET, encryptSaveAsEditText.getText().toString());

              try {
                encryptButtonCallbackInterface.onButtonPressed(
                    intent, passwordEditText.getText().toString());
              } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
                Toast.makeText(c, c.getString(R.string.crypt_encryption_fail), Toast.LENGTH_LONG)
                    .show();
              } finally {
                dialog.dismiss();
              }
            });

    MaterialDialog dialog = builder.show();
    MDButton btnOK = dialog.getActionButton(DialogAction.POSITIVE);
    btnOK.setEnabled(false);

    rootView.post(() -> ExtensionsKt.openKeyboard(passwordEditText, main.getApplicationContext()));

    TextWatcher textWatcher =
        new SimpleTextWatcher() {
          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            btnOK.setEnabled(
                encryptSaveAsEditText.getText().toString().length() > 0
                    && passwordEditText.getText().toString().length() > 0
                    && passwordConfirmEditText.getText().toString().length() > 0);
          }
        };

    passwordEditText.addTextChangedListener(textWatcher);
    passwordConfirmEditText.addTextChangedListener(textWatcher);
    encryptSaveAsEditText.addTextChangedListener(textWatcher);

    new WarnableTextInputValidator(
        c,
        passwordEditText,
        textInputLayoutPassword,
        btnOK,
        (text) -> {
          if (text.length() < 1) {
            return new WarnableTextInputValidator.ReturnState(
                WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.field_empty);
          }
          return new WarnableTextInputValidator.ReturnState();
        });

    new WarnableTextInputValidator(
        c,
        passwordConfirmEditText,
        textInputLayoutPasswordConfirm,
        btnOK,
        (text) -> {
          if (!text.equals(passwordEditText.getText().toString())) {
            return new WarnableTextInputValidator.ReturnState(
                WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.password_no_match);
          }
          return new WarnableTextInputValidator.ReturnState();
        });

    new WarnableTextInputValidator(
        c,
        encryptSaveAsEditText,
        textInputLayoutEncryptSaveAs,
        btnOK,
        (text) -> {
          if (text.length() < 1) {
            return new WarnableTextInputValidator.ReturnState(
                WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.field_empty);
          }
          if (!text.endsWith(CryptUtil.CRYPT_EXTENSION)) {
            return new WarnableTextInputValidator.ReturnState(
                WarnableTextInputValidator.ReturnState.STATE_ERROR,
                R.string.encrypt_file_must_end_with_aze);
          }
          return new WarnableTextInputValidator.ReturnState();
        });
  }

  @RequiresApi(api = M)
  public static void showDecryptFingerprintDialog(
      final Context c,
      MainActivity main,
      final Intent intent,
      AppTheme appTheme,
      final EncryptDecryptUtils.DecryptButtonCallbackInterface decryptButtonCallbackInterface)
      throws GeneralSecurityException, IOException {

    int accentColor = main.getAccent();
    MaterialDialog.Builder builder = new MaterialDialog.Builder(c);
    builder.title(c.getString(R.string.crypt_decrypt));

    View rootView = View.inflate(c, R.layout.dialog_decrypt_fingerprint_authentication, null);

    Button cancelButton = rootView.findViewById(R.id.button_decrypt_fingerprint_cancel);
    cancelButton.setTextColor(accentColor);
    builder.customView(rootView, true);
    builder.canceledOnTouchOutside(false);

    builder.theme(appTheme.getMaterialDialogTheme());

    final MaterialDialog dialog = builder.show();
    cancelButton.setOnClickListener(v -> dialog.cancel());

    FingerprintManager manager =
        (FingerprintManager) c.getSystemService(Context.FINGERPRINT_SERVICE);
    FingerprintManager.CryptoObject object =
        new FingerprintManager.CryptoObject(CryptUtil.initCipher(c));

    FingerprintHandler handler =
        new FingerprintHandler(c, intent, dialog, decryptButtonCallbackInterface);
    handler.authenticate(manager, object);
  }

  public static void showDecryptDialog(
      Context c,
      final MainActivity main,
      final Intent intent,
      AppTheme appTheme,
      final String password,
      final EncryptDecryptUtils.DecryptButtonCallbackInterface decryptButtonCallbackInterface) {

    showPasswordDialog(
        c,
        main,
        appTheme,
        R.string.crypt_decrypt,
        R.string.authenticate_password,
        ((dialog, which) -> {
          EditText editText = dialog.getView().findViewById(R.id.singleedittext_input);

          if (editText.getText().toString().equals(password))
            decryptButtonCallbackInterface.confirm(intent);
          else decryptButtonCallbackInterface.failed();

          dialog.dismiss();
        }),
        null);
  }

  public static void showPasswordDialog(
      @NonNull Context c,
      @NonNull final MainActivity main,
      @NonNull AppTheme appTheme,
      @StringRes int titleText,
      @StringRes int promptText,
      @NonNull MaterialDialog.SingleButtonCallback positiveCallback,
      @Nullable MaterialDialog.SingleButtonCallback negativeCallback) {
    int accentColor = main.getAccent();

    MaterialDialog.Builder builder = new MaterialDialog.Builder(c);
    View dialogLayout = View.inflate(main, R.layout.dialog_singleedittext, null);
    WarnableTextInputLayout wilTextfield =
        dialogLayout.findViewById(R.id.singleedittext_warnabletextinputlayout);
    EditText textfield = dialogLayout.findViewById(R.id.singleedittext_input);
    textfield.setHint(promptText);
    textfield.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

    dialogLayout.post(() -> ExtensionsKt.openKeyboard(textfield, main.getApplicationContext()));

    builder
        .customView(dialogLayout, false)
        .theme(appTheme.getMaterialDialogTheme())
        .autoDismiss(false)
        .canceledOnTouchOutside(false)
        .title(titleText)
        .positiveText(R.string.ok)
        .positiveColor(accentColor)
        .onPositive(positiveCallback)
        .negativeText(R.string.cancel)
        .negativeColor(accentColor);

    if (negativeCallback != null) builder.onNegative(negativeCallback);
    else builder.onNegative((dialog, which) -> dialog.cancel());

    MaterialDialog dialog = builder.show();

    new WarnableTextInputValidator(
        AppConfig.getInstance().getMainActivityContext(),
        textfield,
        wilTextfield,
        dialog.getActionButton(DialogAction.POSITIVE),
        (text) -> {
          if (text.length() < 1) {
            return new WarnableTextInputValidator.ReturnState(
                WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.field_empty);
          }
          return new WarnableTextInputValidator.ReturnState();
        });
  }

  public static void showSMBHelpDialog(Context m, int accentColor) {
    MaterialDialog.Builder b = new MaterialDialog.Builder(m);
    b.content(m.getText(R.string.smb_instructions));
    b.positiveText(R.string.doit);
    b.positiveColor(accentColor);
    b.build().show();
  }

  public static void showPackageDialog(final File f, final MainActivity m) {
    int accentColor = m.getAccent();
    MaterialDialog.Builder mat = new MaterialDialog.Builder(m);
    mat.title(R.string.package_installer)
        .content(R.string.package_installer_text)
        .positiveText(R.string.install)
        .negativeText(R.string.view)
        .neutralText(R.string.cancel)
        .positiveColor(accentColor)
        .negativeColor(accentColor)
        .neutralColor(accentColor)
        .onPositive((dialog, which) -> FileUtils.installApk(f, m))
        .onNegative((dialog, which) -> m.openCompressed(f.getPath()))
        .theme(m.getAppTheme().getMaterialDialogTheme())
        .build()
        .show();
  }

  public static void showArchiveDialog(final File f, final MainActivity m) {
    int accentColor = m.getAccent();
    MaterialDialog.Builder mat = new MaterialDialog.Builder(m);
    mat.title(R.string.archive)
        .content(R.string.archive_text)
        .positiveText(R.string.extract)
        .negativeText(R.string.view)
        .neutralText(R.string.cancel)
        .positiveColor(accentColor)
        .negativeColor(accentColor)
        .neutralColor(accentColor)
        .onPositive((dialog, which) -> m.mainActivityHelper.extractFile(f))
        .onNegative((dialog, which) -> m.openCompressed(Uri.fromFile(f).toString()));
    if (m.getAppTheme().equals(AppTheme.DARK) || m.getAppTheme().equals(AppTheme.BLACK))
      mat.theme(Theme.DARK);
    MaterialDialog b = mat.build();

    if (!CompressedHelper.isFileExtractable(f.getPath())) {
      b.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
    }
    b.show();
  }

  public static void showCompressDialog(
      @NonNull final MainActivity mainActivity,
      final ArrayList<HybridFileParcelable> baseFiles,
      final String current) {
    int accentColor = mainActivity.getAccent();
    MaterialDialog.Builder a = new MaterialDialog.Builder(mainActivity);

    View dialogView =
        mainActivity.getLayoutInflater().inflate(R.layout.dialog_singleedittext, null);
    EditText etFilename = dialogView.findViewById(R.id.singleedittext_input);
    etFilename.setHint(R.string.enterzipname);
    etFilename.setText(".zip"); // TODO: Put the file/folder name here
    etFilename.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    etFilename.setSingleLine();
    WarnableTextInputLayout tilFilename =
        dialogView.findViewById(R.id.singleedittext_warnabletextinputlayout);

    dialogView.post(
        () -> ExtensionsKt.openKeyboard(etFilename, mainActivity.getApplicationContext()));

    a.customView(dialogView, false)
        .widgetColor(accentColor)
        .theme(mainActivity.getAppTheme().getMaterialDialogTheme())
        .title(mainActivity.getResources().getString(R.string.enterzipname))
        .positiveText(R.string.create)
        .positiveColor(accentColor)
        .onPositive(
            (materialDialog, dialogAction) -> {
              String name = current + "/" + etFilename.getText().toString();
              mainActivity.mainActivityHelper.compressFiles(new File(name), baseFiles);
            })
        .negativeText(mainActivity.getResources().getString(R.string.cancel))
        .negativeColor(accentColor);

    final MaterialDialog materialDialog = a.build();

    new WarnableTextInputValidator(
        a.getContext(),
        etFilename,
        tilFilename,
        materialDialog.getActionButton(DialogAction.POSITIVE),
        (text) -> {
          boolean isValidFilename = FileProperties.isValidFilename(text);

          if (isValidFilename && text.length() > 0 && !text.toLowerCase().endsWith(".zip")) {
            return new WarnableTextInputValidator.ReturnState(
                WarnableTextInputValidator.ReturnState.STATE_WARNING,
                R.string.compress_file_suggest_zip_extension);
          } else {
            if (!isValidFilename) {
              return new WarnableTextInputValidator.ReturnState(
                  WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.invalid_name);
            } else if (text.length() < 1) {
              return new WarnableTextInputValidator.ReturnState(
                  WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.field_empty);
            }
          }

          return new WarnableTextInputValidator.ReturnState();
        });

    materialDialog.show();

    // place cursor at the starting of edit text by posting a runnable to edit text
    // this is done because in case android has not populated the edit text layouts yet, it'll
    // reset calls to selection if not posted in message queue
    etFilename.post(() -> etFilename.setSelection(0));
  }

  public static void showSortDialog(
      final MainFragment m, AppTheme appTheme, final SharedPreferences sharedPref) {
    final String path = m.getCurrentPath();
    int accentColor = m.getMainActivity().getAccent();
    String[] sort = m.getResources().getStringArray(R.array.sortby);
    int current = SortHandler.getSortType(m.getContext(), path);
    MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
    a.theme(appTheme.getMaterialDialogTheme());
    a.items(sort)
        .itemsCallbackSingleChoice(
            current > 3 ? current - 4 : current, (dialog, view, which, text) -> true);
    final Set<String> sortbyOnlyThis =
        sharedPref.getStringSet(PREFERENCE_SORTBY_ONLY_THIS, Collections.emptySet());
    final Set<String> onlyThisFloders = new HashSet<>(sortbyOnlyThis);
    boolean onlyThis = onlyThisFloders.contains(path);
    a.checkBoxPrompt(
        m.getResources().getString(R.string.sort_only_this),
        onlyThis,
        (buttonView, isChecked) -> {
          if (isChecked) {
            if (!onlyThisFloders.contains(path)) {
              onlyThisFloders.add(path);
            }
          } else {
            if (onlyThisFloders.contains(path)) {
              onlyThisFloders.remove(path);
            }
          }
        });
    a.negativeText(R.string.ascending).positiveColor(accentColor);
    a.positiveText(R.string.descending).negativeColor(accentColor);
    a.onNegative(
        (dialog, which) -> {
          onSortTypeSelected(m, sharedPref, onlyThisFloders, dialog, false);
        });
    a.onPositive(
        (dialog, which) -> {
          onSortTypeSelected(m, sharedPref, onlyThisFloders, dialog, true);
        });
    a.title(R.string.sort_by);
    a.build().show();
  }

  private static void onSortTypeSelected(
      MainFragment m,
      SharedPreferences sharedPref,
      Set<String> onlyThisFloders,
      MaterialDialog dialog,
      boolean desc) {
    final int sortType = desc ? dialog.getSelectedIndex() + 4 : dialog.getSelectedIndex();
    SortHandler sortHandler = SortHandler.getInstance();
    if (onlyThisFloders.contains(m.getCurrentPath())) {
      Sort oldSort = sortHandler.findEntry(m.getCurrentPath());
      Sort newSort = new Sort(m.getCurrentPath(), sortType);
      if (oldSort == null) {
        sortHandler.addEntry(newSort);
      } else {
        sortHandler.updateEntry(oldSort, newSort);
      }
    } else {
      sortHandler.clear(m.getCurrentPath());
      sharedPref.edit().putString("sortby", String.valueOf(sortType)).apply();
    }
    sharedPref.edit().putStringSet(PREFERENCE_SORTBY_ONLY_THIS, onlyThisFloders).apply();
    m.updateList();
    dialog.dismiss();
  }

  public static void showHistoryDialog(
      final DataUtils dataUtils,
      SharedPreferences sharedPrefs,
      final MainFragment m,
      AppTheme appTheme) {
    int accentColor = m.getMainActivity().getAccent();
    final MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
    a.positiveText(R.string.cancel);
    a.positiveColor(accentColor);
    a.negativeText(R.string.clear);
    a.negativeColor(accentColor);
    a.title(R.string.history);
    a.onNegative((dialog, which) -> dataUtils.clearHistory());
    a.theme(appTheme.getMaterialDialogTheme());

    HiddenAdapter adapter =
        new HiddenAdapter(
            m.getActivity(),
            m,
            sharedPrefs,
            toHybridFileArrayList(dataUtils.getHistory()),
            null,
            true);
    a.adapter(adapter, null);

    MaterialDialog x = a.build();
    adapter.updateDialog(x);
    x.show();
  }

  public static void showHiddenDialog(
      DataUtils dataUtils,
      SharedPreferences sharedPrefs,
      final MainFragment mainFragment,
      AppTheme appTheme) {
    if (mainFragment == null || mainFragment.getActivity() == null) {
      return;
    }

    int accentColor = mainFragment.getMainActivity().getAccent();
    final MaterialDialog.Builder builder = new MaterialDialog.Builder(mainFragment.getActivity());
    builder.positiveText(R.string.close);
    builder.positiveColor(accentColor);
    builder.title(R.string.hiddenfiles);
    builder.theme(appTheme.getMaterialDialogTheme());
    builder.autoDismiss(true);
    HiddenAdapter adapter =
        new HiddenAdapter(
            mainFragment.getActivity(),
            mainFragment,
            sharedPrefs,
            FileUtils.toHybridFileConcurrentRadixTree(dataUtils.getHiddenFiles()),
            null,
            false);
    builder.adapter(adapter, null);
    builder.dividerColor(Color.GRAY);
    MaterialDialog materialDialog = builder.build();
    adapter.updateDialog(materialDialog);
    materialDialog.setOnDismissListener(
        dialogInterface ->
            mainFragment.loadlist(mainFragment.getCurrentPath(), false, OpenMode.UNKNOWN));
    materialDialog.show();
  }

  public static void setPermissionsDialog(
      final View v,
      View but,
      final HybridFile file,
      final String f,
      final Context context,
      final MainFragment mainFrag) {
    final CheckBox readown = v.findViewById(R.id.creadown);
    final CheckBox readgroup = v.findViewById(R.id.creadgroup);
    final CheckBox readother = v.findViewById(R.id.creadother);
    final CheckBox writeown = v.findViewById(R.id.cwriteown);
    final CheckBox writegroup = v.findViewById(R.id.cwritegroup);
    final CheckBox writeother = v.findViewById(R.id.cwriteother);
    final CheckBox exeown = v.findViewById(R.id.cexeown);
    final CheckBox exegroup = v.findViewById(R.id.cexegroup);
    final CheckBox exeother = v.findViewById(R.id.cexeother);
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
    but.setOnClickListener(
        v1 -> {
          int perms =
              RootHelper.permissionsToOctalString(
                  readown.isChecked(),
                  writeown.isChecked(),
                  exeown.isChecked(),
                  readgroup.isChecked(),
                  writegroup.isChecked(),
                  exegroup.isChecked(),
                  readother.isChecked(),
                  writeother.isChecked(),
                  exeother.isChecked());

          try {
            ChangeFilePermissionsCommand.INSTANCE.changeFilePermissions(
                file.getPath(),
                perms,
                file.isDirectory(context),
                isSuccess -> {
                  if (isSuccess) {
                    Toast.makeText(context, mainFrag.getString(R.string.done), Toast.LENGTH_LONG)
                        .show();
                  } else {
                    Toast.makeText(
                            context,
                            mainFrag.getString(R.string.operation_unsuccesful),
                            Toast.LENGTH_LONG)
                        .show();
                  }
                  return null;
                });
          } catch (ShellNotRunningException e) {
            Toast.makeText(context, mainFrag.getString(R.string.root_failure), Toast.LENGTH_LONG)
                .show();
            e.printStackTrace();
          }
        });
  }

  public static void showChangePathsDialog(
      final MainActivity mainActivity, final SharedPreferences prefs) {
    final MainFragment mainFragment = mainActivity.getCurrentMainFragment();
    Objects.requireNonNull(mainActivity);
    final MaterialDialog.Builder a = new MaterialDialog.Builder(mainActivity);
    a.input(
        null,
        mainFragment.getCurrentPath(),
        false,
        (dialog, charSequence) -> {
          boolean isAccessible = FileUtils.isPathAccessible(charSequence.toString(), prefs);
          dialog.getActionButton(DialogAction.POSITIVE).setEnabled(isAccessible);
        });

    a.alwaysCallInputCallback();

    int accentColor = mainActivity.getAccent();

    a.widgetColor(accentColor);

    a.theme(mainActivity.getAppTheme().getMaterialDialogTheme());
    a.title(R.string.enterpath);

    a.positiveText(R.string.go);
    a.positiveColor(accentColor);

    a.negativeText(R.string.cancel);
    a.negativeColor(accentColor);

    a.onPositive(
        (dialog, which) -> {
          mainFragment.loadlist(
              dialog.getInputEditText().getText().toString(), false, OpenMode.UNKNOWN);
        });

    a.show();
  }

  public static MaterialDialog showOtgSafExplanationDialog(ThemedActivity themedActivity) {
    return GeneralDialogCreation.showBasicDialog(
        themedActivity,
        R.string.saf_otg_explanation,
        R.string.otg_access,
        R.string.ok,
        R.string.cancel);
  }

  public static void showSignInWithGoogleDialog(@NonNull MainActivity mainActivity) {
    View customView =
        DialogSigninWithGoogleBinding.inflate(LayoutInflater.from(mainActivity)).getRoot();
    int accentColor = mainActivity.getAccent();
    customView
        .findViewById(R.id.signin_with_google)
        .setOnClickListener(
            v -> {
              mainActivity.addConnection(OpenMode.GDRIVE);
            });
    new MaterialDialog.Builder(mainActivity)
        .customView(customView, false)
        .title(R.string.signin_with_google_title)
        .negativeText(android.R.string.cancel)
        .negativeColor(accentColor)
        .onNegative((dialog, which) -> dialog.dismiss())
        .build()
        .show();
  }
}
