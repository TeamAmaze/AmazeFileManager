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

import static android.os.Build.VERSION.SDK_INT;
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SORTBY_ONLY_THIS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.asynchronous.asynctasks.CountItemsOrAndSizeTask;
import com.amaze.filemanager.asynchronous.asynctasks.LoadFolderSpaceDataTask;
import com.amaze.filemanager.asynchronous.asynctasks.TaskKt;
import com.amaze.filemanager.asynchronous.asynctasks.hashcalculator.CalculateHashTask;
import com.amaze.filemanager.database.SortHandler;
import com.amaze.filemanager.database.models.explorer.Sort;
import com.amaze.filemanager.databinding.DialogSigninWithGoogleBinding;
import com.amaze.filemanager.fileoperations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.fileoperations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.FileProperties;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.files.sort.SortBy;
import com.amaze.filemanager.filesystem.files.sort.SortOrder;
import com.amaze.filemanager.filesystem.files.sort.SortType;
import com.amaze.filemanager.filesystem.root.ChangeFilePermissionsCommand;
import com.amaze.filemanager.ui.ExtensionsKt;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.ui.fragments.MainFragment;
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.ui.views.WarnableTextInputLayout;
import com.amaze.filemanager.ui.views.WarnableTextInputValidator;
import com.amaze.filemanager.utils.Utils;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;
import androidx.preference.PreferenceManager;

/**
 * Here are a lot of function that create material dialogs
 *
 * @author Emmanuel on 17/5/2017, at 13:27.
 */
public class GeneralDialogCreation {

  private static final Logger LOG = LoggerFactory.getLogger(GeneralDialogCreation.class);

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
    AppCompatEditText textfield = dialogView.findViewById(R.id.singleedittext_input);
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
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean needConfirmation =
        sharedPreferences.getBoolean(
            PreferencesConstants.PREFERENCE_DELETE_CONFIRMATION,
            PreferencesConstants.DEFAULT_PREFERENCE_DELETE_CONFIRMATION);
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
    final AppCompatTextView categoryDirectories =
        dialog.getCustomView().findViewById(R.id.category_directories);
    final AppCompatTextView categoryFiles =
        dialog.getCustomView().findViewById(R.id.category_files);
    final AppCompatTextView listDirectories =
        dialog.getCustomView().findViewById(R.id.list_directories);
    final AppCompatTextView listFiles = dialog.getCustomView().findViewById(R.id.list_files);
    final AppCompatTextView total = dialog.getCustomView().findViewById(R.id.total);

    new AsyncTask<Void, Object, Void>() {

      long sizeTotal = 0;
      StringBuilder files = new StringBuilder();
      StringBuilder directories = new StringBuilder();
      int counterDirectories = 0;
      int counterFiles = 0;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        if (needConfirmation) {
          listFiles.setText(context.getString(R.string.loading));
          listDirectories.setText(context.getString(R.string.loading));
          total.setText(context.getString(R.string.loading));
        }
      }

      @Override
      protected Void doInBackground(Void... params) {

        for (int i = 0; i < positions.size(); i++) {
          final LayoutElementParcelable layoutElement = positions.get(i);
          itemsToDelete.add(layoutElement.generateBaseFile());
          if (needConfirmation) {
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
        }
        return null;
      }

      @Override
      protected void onProgressUpdate(Object... result) {
        super.onProgressUpdate(result);
        if (needConfirmation) {
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
      }

      @Override
      protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (needConfirmation) {
          updateViews(sizeTotal, files, directories, counterFiles, counterDirectories);
        } else {
          Toast.makeText(context, context.getString(R.string.deleting), Toast.LENGTH_SHORT).show();
          mainActivity.mainActivityHelper.deleteFiles(itemsToDelete);
        }
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
    if (SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      categoryDirectories.setTextColor(accentColor);
      categoryFiles.setTextColor(accentColor);
    }

    if (needConfirmation) {
      // Show dialog on screen.
      dialog.show();
    }
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
    AppCompatTextView itemsText = v.findViewById(R.id.t7);
    AppCompatCheckBox nomediaCheckBox = v.findViewById(R.id.nomediacheckbox);

    /*View setup*/
    {
      AppCompatTextView mNameTitle = v.findViewById(R.id.title_name);
      mNameTitle.setTextColor(accentColor);

      AppCompatTextView mDateTitle = v.findViewById(R.id.title_date);
      mDateTitle.setTextColor(accentColor);

      AppCompatTextView mSizeTitle = v.findViewById(R.id.title_size);
      mSizeTitle.setTextColor(accentColor);

      AppCompatTextView mLocationTitle = v.findViewById(R.id.title_location);
      mLocationTitle.setTextColor(accentColor);

      AppCompatTextView md5Title = v.findViewById(R.id.title_md5);
      md5Title.setTextColor(accentColor);

      AppCompatTextView sha256Title = v.findViewById(R.id.title_sha256);
      sha256Title.setTextColor(accentColor);

      ((AppCompatTextView) v.findViewById(R.id.t5)).setText(name);
      ((AppCompatTextView) v.findViewById(R.id.t6)).setText(parent);
      itemsText.setText(items);
      ((AppCompatTextView) v.findViewById(R.id.t8)).setText(date);

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

    TaskKt.fromTask(new CalculateHashTask(baseFile, c, v));

    /*Chart creation and data loading*/
    {
      int layoutDirection = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault());
      boolean isRightToLeft = layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL;
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
                  LOG.warn(".nomedia file creation in {} failed", baseFile.getPath());
                }
              } catch (IOException e) {
                LOG.warn("failed to create file at path {}", baseFile.getPath(), e);
              }
            } else {
              // checkbox is unchecked, delete .nomedia
              if (!nomediaFile.delete()) {
                // failed operation
                LOG.warn(".nomedia file deletion in {} failed", baseFile.getPath());
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
          AppCompatEditText editText = dialog.getView().findViewById(R.id.singleedittext_input);

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
    AppCompatEditText textfield = dialogLayout.findViewById(R.id.singleedittext_input);
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

  public static MaterialDialog showOpenFileDeeplinkDialog(
      final HybridFile file, final MainActivity m, final String content, Runnable openCallback) {
    int accentColor = m.getAccent();
    return new MaterialDialog.Builder(m)
        .title(R.string.confirmation)
        .content(content)
        .positiveText(R.string.open)
        .negativeText(R.string.cancel)
        .positiveColor(accentColor)
        .negativeColor(accentColor)
        .onPositive((dialog, which) -> openCallback.run())
        .onNegative((dialog, which) -> dialog.dismiss())
        .theme(m.getAppTheme().getMaterialDialogTheme())
        .build();
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
      final HybridFileParcelable baseFile,
      final String current) {
    ArrayList<HybridFileParcelable> baseFiles = new ArrayList<>();
    baseFiles.add(baseFile);
    showCompressDialog(mainActivity, baseFiles, current);
  }

  public static void showCompressDialog(
      @NonNull final MainActivity mainActivity,
      final ArrayList<HybridFileParcelable> baseFiles,
      final String current) {
    int accentColor = mainActivity.getAccent();
    MaterialDialog.Builder a = new MaterialDialog.Builder(mainActivity);

    View dialogView =
        mainActivity.getLayoutInflater().inflate(R.layout.dialog_singleedittext, null);
    AppCompatEditText etFilename = dialogView.findViewById(R.id.singleedittext_input);
    etFilename.setHint(R.string.enterzipname);
    etFilename.setText(".zip"); // TODO: Put the file/folder name here
    etFilename.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
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
    SortType current = SortHandler.getSortType(m.getContext(), path);
    MaterialDialog.Builder a = new MaterialDialog.Builder(m.getActivity());
    a.theme(appTheme.getMaterialDialogTheme());
    a.items(sort)
        .itemsCallbackSingleChoice(
            current.getSortBy().getIndex(), (dialog, view, which, text) -> true);
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
          onSortTypeSelected(m, sharedPref, onlyThisFloders, dialog, SortOrder.ASC);
        });
    a.onPositive(
        (dialog, which) -> {
          onSortTypeSelected(m, sharedPref, onlyThisFloders, dialog, SortOrder.DESC);
        });
    a.title(R.string.sort_by);
    a.build().show();
  }

  private static void onSortTypeSelected(
      MainFragment m,
      SharedPreferences sharedPref,
      Set<String> onlyThisFloders,
      MaterialDialog dialog,
      SortOrder sortOrder) {
    final SortType sortType =
        new SortType(SortBy.getDirectorySortBy(dialog.getSelectedIndex()), sortOrder);
    SortHandler sortHandler = SortHandler.getInstance();
    if (onlyThisFloders.contains(m.getCurrentPath())) {
      Sort oldSort = sortHandler.findEntry(m.getCurrentPath());
      if (oldSort == null) {
        sortHandler.addEntry(m.getCurrentPath(), sortType);
      } else {
        sortHandler.updateEntry(oldSort, m.getCurrentPath(), sortType);
      }
    } else {
      sortHandler.clear(m.getCurrentPath());
      sharedPref.edit().putString("sortby", String.valueOf(sortType.toDirectorySortInt())).apply();
    }
    sharedPref.edit().putStringSet(PREFERENCE_SORTBY_ONLY_THIS, onlyThisFloders).apply();
    m.updateList(false);
    dialog.dismiss();
  }

  public static void setPermissionsDialog(
      final View v,
      View but,
      final HybridFile file,
      final String f,
      final Context context,
      final MainFragment mainFrag) {
    final AppCompatCheckBox readown = v.findViewById(R.id.creadown);
    final AppCompatCheckBox readgroup = v.findViewById(R.id.creadgroup);
    final AppCompatCheckBox readother = v.findViewById(R.id.creadother);
    final AppCompatCheckBox writeown = v.findViewById(R.id.cwriteown);
    final AppCompatCheckBox writegroup = v.findViewById(R.id.cwritegroup);
    final AppCompatCheckBox writeother = v.findViewById(R.id.cwriteother);
    final AppCompatCheckBox exeown = v.findViewById(R.id.cexeown);
    final AppCompatCheckBox exegroup = v.findViewById(R.id.cexegroup);
    final AppCompatCheckBox exeother = v.findViewById(R.id.cexeother);
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
            LOG.warn("failed to set permission dialog", e);
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
              dialog.getInputEditText().getText().toString(), false, OpenMode.UNKNOWN, false);
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

    MaterialDialog dialog =
        new MaterialDialog.Builder(mainActivity)
            .customView(customView, false)
            .title(R.string.signin_with_google_title)
            .negativeText(android.R.string.cancel)
            .negativeColor(accentColor)
            .onNegative((dlg, which) -> dlg.dismiss())
            .build();

    customView
        .findViewById(R.id.signin_with_google)
        .setOnClickListener(
            v -> {
              mainActivity.addConnection(OpenMode.GDRIVE);
              dialog.dismiss();
            });

    dialog.show();
  }
}
