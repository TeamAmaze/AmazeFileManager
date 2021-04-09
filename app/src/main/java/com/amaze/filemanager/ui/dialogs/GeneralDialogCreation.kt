/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.dialogs

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.fingerprint.FingerprintManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.text.InputType
import android.text.SpannableString
import android.text.TextUtils
import android.text.TextWatcher
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatButton
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.DialogCallback
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.HiddenAdapter
import com.amaze.filemanager.adapters.data.LayoutElementParcelable
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.CountItemsOrAndSizeTask
import com.amaze.filemanager.asynchronous.asynctasks.GenerateHashesTask
import com.amaze.filemanager.asynchronous.asynctasks.LoadFolderSpaceDataTask
import com.amaze.filemanager.asynchronous.services.EncryptService
import com.amaze.filemanager.database.SortHandler
import com.amaze.filemanager.database.models.explorer.Sort
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.FileProperties
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.RootHelper
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import com.amaze.filemanager.filesystem.files.CryptUtil
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils.DecryptButtonCallbackInterface
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils.EncryptButtonCallbackInterface
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.filesystem.root.ChangeFilePermissionsCommand
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity
import com.amaze.filemanager.ui.fragments.MainFragment
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants
import com.amaze.filemanager.ui.theme.AppTheme
import com.amaze.filemanager.ui.views.WarnableTextInputLayout
import com.amaze.filemanager.ui.views.WarnableTextInputValidator
import com.amaze.filemanager.ui.views.WarnableTextInputValidator.ReturnState
import com.amaze.filemanager.utils.DataUtils
import com.amaze.filemanager.utils.FingerprintHandler
import com.amaze.filemanager.utils.SimpleTextWatcher
import com.amaze.filemanager.utils.Utils
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.concurrent.Executors

object GeneralDialogCreation {

    private const val TAG = "GeneralDialogCreation"

    @JvmStatic
    fun showBasicDialog(
        themedActivity: ThemedActivity,
        @StringRes content: Int,
        @StringRes title: Int,
        @StringRes positiveText: Int,
        @StringRes negativeText: Int,
        positiveCallback: DialogCallback? = null,
        negativeCallback: DialogCallback? = null
    ): MaterialDialog {
        val accentColor: Int = themedActivity.getAccent()
        return MaterialDialog(themedActivity).show {
            title(title)
            message(content)
            positiveButton(positiveText, click = positiveCallback)
            negativeButton(negativeText, click = negativeCallback)
            getActionButton(WhichButton.POSITIVE).setTextColor(accentColor)
            getActionButton(WhichButton.NEGATIVE).setTextColor(accentColor)
        }
    }

    @JvmStatic
    fun showNameDialog(
        m: MainActivity,
        hint: String,
        prefill: String,
        title: String,
        positiveButtonText: String,
        neutralButtonText: String?,
        negativeButtonText: String,
        positiveButtonAction: DialogCallback,
        validator: WarnableTextInputValidator.OnTextValidate
    ): MaterialDialog {
        val accentColor = m.accent
        return MaterialDialog(m).show {
            title(text = title)
            customView(R.layout.dialog_singleedittext)
            positiveButton(text = positiveButtonText, click = positiveButtonAction)
            negativeButton(text = negativeButtonText)
            neutralButton(text = neutralButtonText)
            getActionButton(WhichButton.POSITIVE).setTextColor(accentColor)
            getActionButton(WhichButton.NEGATIVE).setTextColor(accentColor)

            val view = this.getCustomView()
            val textfield: EditText = view.findViewById(R.id.singleedittext_input)
            textfield.hint = hint
            textfield.setText(prefill)

            val tilTextfield: WarnableTextInputLayout =
                view.findViewById(R.id.singleedittext_warnabletextinputlayout)

            val textInputValidator = WarnableTextInputValidator(
                m,
                textfield,
                tilTextfield,
                getActionButton(WhichButton.POSITIVE),
                validator
            )

            if (!TextUtils.isEmpty(prefill)) {
                textInputValidator.afterTextChanged(textfield.text)
            }
        }
    }

    @JvmStatic
    fun deleteFilesDialog(
        c: Context,
        layoutElements: ArrayList<LayoutElementParcelable>,
        mainActivity: MainActivity,
        positions: List<LayoutElementParcelable>,
        appTheme: AppTheme
    ) {

        val itemsToDelete = ArrayList<HybridFileParcelable>()
        val accentColor = mainActivity.accent
        // Build dialog with custom view layout and accent color.
        val dialog = MaterialDialog(c).show {
            title(R.string.dialog_delete_title)
            customView(R.layout.dialog_delete)
            negativeButton(text = c.getString(R.string.cancel).uppercase())
            positiveButton(
                text = c.getString(R.string.delete).uppercase(),
                click = {
                    Toast.makeText(c, R.string.deleting, Toast.LENGTH_SHORT).show()
                    mainActivity.mainActivityHelper.deleteFiles(itemsToDelete)
                }
            )
            getActionButton(WhichButton.POSITIVE).setTextColor(accentColor)
            getActionButton(WhichButton.NEGATIVE).setTextColor(accentColor)
        }

        // Get views from custom layout to set text values.
        val categoryDirectories: TextView =
            dialog.getCustomView().findViewById(R.id.category_directories)
        val categoryFiles: TextView =
            dialog.getCustomView().findViewById(R.id.category_files)
        val listDirectories: TextView =
            dialog.getCustomView().findViewById(R.id.list_directories)
        val listFiles: TextView =
            dialog.getCustomView().findViewById(R.id.list_files)
        val total: TextView =
            dialog.getCustomView().findViewById(R.id.total)

        // Parse items to delete.
        object : AsyncTask<Unit, Any?, Unit>() {
            var sizeTotal: Long = 0
            var files = StringBuilder()
            var directories = StringBuilder()
            var counterDirectories = 0
            var counterFiles = 0
            override fun onPreExecute() {
                super.onPreExecute()
                listFiles.text = c.getString(R.string.loading)
                listDirectories.text = c.getString(R.string.loading)
                total.text = c.getString(R.string.loading)
            }

            override fun doInBackground(vararg params: Unit) {
                for (i in positions.indices) {
                    val layoutElement = positions[i]
                    itemsToDelete.add(layoutElement.generateBaseFile())

                    // Build list of directories to delete.
                    sizeTotal += if (layoutElement.isDirectory) {
                        // Don't add newline between category and list.
                        if (counterDirectories != 0) {
                            directories.append("\n")
                        }
                        val sizeDirectory = layoutElement.generateBaseFile().folderSize(c)
                        directories
                            .append(++counterDirectories)
                            .append(". ")
                            .append(layoutElement.title)
                            .append(" (")
                            .append(Formatter.formatFileSize(c, sizeDirectory))
                            .append(")")
                        sizeDirectory
                        // Build list of files to delete.
                    } else {
                        // Don't add newline between category and list.
                        if (counterFiles != 0) {
                            files.append("\n")
                        }
                        files
                            .append(++counterFiles)
                            .append(". ")
                            .append(layoutElement.title)
                            .append(" (")
                            .append(layoutElement.size)
                            .append(")")
                        layoutElement.longSize
                    }
                    publishProgress(sizeTotal, counterFiles, counterDirectories, files, directories)
                }
            }

            override fun onProgressUpdate(vararg result: Any?) {
                super.onProgressUpdate(*result)
                val tempCounterFiles = result[1] as Int
                val tempCounterDirectories = result[2] as Int
                val tempSizeTotal = result[0] as Long
                val tempFilesStringBuilder = result[3] as StringBuilder
                val tempDirectoriesStringBuilder = result[4] as StringBuilder
                updateViews(
                    tempSizeTotal,
                    tempFilesStringBuilder,
                    tempDirectoriesStringBuilder,
                    tempCounterFiles,
                    tempCounterDirectories
                )
            }

            protected override fun onPostExecute(result: Unit) {
                super.onPostExecute(result)
                updateViews(sizeTotal, files, directories, counterFiles, counterDirectories)
            }

            private fun updateViews(
                tempSizeTotal: Long,
                filesStringBuilder: StringBuilder,
                directoriesStringBuilder: StringBuilder,
                vararg values: Int
            ) {
                val tempCounterFiles = values[0]
                val tempCounterDirectories = values[1]

                // Hide category and list for directories when zero.
                if (tempCounterDirectories == 0) {
                    if (tempCounterDirectories == 0) {
                        categoryDirectories.visibility = View.GONE
                        listDirectories.visibility = View.GONE
                    }
                    // Hide category and list for files when zero.
                }
                if (tempCounterFiles == 0) {
                    categoryFiles.visibility = View.GONE
                    listFiles.visibility = View.GONE
                }
                if (tempCounterDirectories != 0 || tempCounterFiles != 0) {
                    listDirectories.text = directoriesStringBuilder
                    if (listDirectories.visibility != View.VISIBLE &&
                        tempCounterDirectories != 0
                    ) {
                        listDirectories.visibility = View.VISIBLE
                    }
                    listFiles.text = filesStringBuilder
                    if (listFiles.visibility != View.VISIBLE && tempCounterFiles != 0) {
                        listFiles.visibility = View.VISIBLE
                    }
                    if (categoryDirectories.visibility != View.VISIBLE &&
                        tempCounterDirectories != 0
                    ) {
                        categoryDirectories.visibility = View.VISIBLE
                    }
                    if (categoryFiles.visibility != View.VISIBLE && tempCounterFiles != 0) {
                        categoryFiles.visibility = View.VISIBLE
                    }
                }

                // Show total size with at least one directory or file and size is not zero.
                if (tempCounterFiles + tempCounterDirectories > 1 && tempSizeTotal > 0) {
                    val builderTotal = StringBuilder()
                        .append(c.getString(R.string.total))
                        .append(" ")
                        .append(Formatter.formatFileSize(c, tempSizeTotal))
                    total.text = builderTotal
                    if (total.visibility != View.VISIBLE) total.visibility = View.VISIBLE
                } else {
                    total.visibility = View.GONE
                }
            }
        }.execute()
    }

    @JvmStatic
    fun showPropertiesDialogWithPermissions(
        baseFile: HybridFileParcelable,
        permissions: String?,
        activity: ThemedActivity,
        isRoot: Boolean,
        appTheme: AppTheme
    ) {
        showPropertiesDialog(baseFile, permissions, activity, isRoot, appTheme, true, false)
    }

    @JvmStatic
    fun showPropertiesDialogWithoutPermissions(
        f: HybridFileParcelable,
        activity: ThemedActivity,
        appTheme: AppTheme
    ) {
        showPropertiesDialog(f, null, activity, false, appTheme, false, false)
    }

    @JvmStatic
    fun showPropertiesDialogForStorage(
        f: HybridFileParcelable,
        activity: ThemedActivity,
        appTheme: AppTheme
    ) {
        showPropertiesDialog(f, null, activity, false, appTheme, false, true)
    }

    private fun showPropertiesDialog(
        baseFile: HybridFileParcelable,
        permissions: String?,
        base: ThemedActivity,
        isRoot: Boolean,
        appTheme: AppTheme,
        showPermissions: Boolean,
        forStorage: Boolean
    ) {
        val executor = Executors.newFixedThreadPool(3)
        val c = base.applicationContext
        val accentColor = base.accent
        val last = baseFile.date
        val date = Utils.getDate(base, last)
        val items = c.getString(R.string.calculating)
        val name = baseFile.getName(c)
        val parent = baseFile.getReadablePath(baseFile.getParent(c))
        val nomediaFile = if (baseFile.isDirectory) {
            File("${baseFile.path}/${FileUtils.NOMEDIA_FILE}")
        } else {
            null
        }
        MaterialDialog(base).show {
            title(R.string.properties)
            customView(R.layout.properties_dialog)
            val view = getCustomView()

            val itemsText = view.findViewById<TextView>(R.id.t7)
            val nomediaCheckBox = view.findViewById<CheckBox>(R.id.nomediacheckbox)

            view.findViewById<TextView>(R.id.title_name).setTextColor(accentColor)
            view.findViewById<TextView>(R.id.title_date).setTextColor(accentColor)
            view.findViewById<TextView>(R.id.title_size).setTextColor(accentColor)
            view.findViewById<TextView>(R.id.title_location).setTextColor(accentColor)
            view.findViewById<TextView>(R.id.title_md5).setTextColor(accentColor)
            view.findViewById<TextView>(R.id.title_sha256).setTextColor(accentColor)
            (view.findViewById<View>(R.id.t5) as TextView).text = name
            (view.findViewById<View>(R.id.t6) as TextView).text = parent
            itemsText.text = items
            (view.findViewById<View>(R.id.t8) as TextView).text = date
            if (baseFile.isDirectory && baseFile.isLocal) {
                nomediaCheckBox.visibility = View.VISIBLE
                if (nomediaFile != null) {
                    nomediaCheckBox.isChecked = nomediaFile.exists()
                }
            }

            // setting click listeners for long press
            view.findViewById<LinearLayout>(R.id.properties_dialog_name)
                .setOnLongClickListener {
                    FileUtils.copyToClipboard(c, name)
                    Toast.makeText(
                        c,
                        c.getString(R.string.name) + " " +
                            c.getString(R.string.properties_copied_clipboard),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    false
                }
            view.findViewById<LinearLayout>(R.id.properties_dialog_location)
                .setOnLongClickListener {
                    FileUtils.copyToClipboard(c, parent)
                    Toast.makeText(
                        c,
                        c.getString(R.string.location) + " " +
                            c.getString(R.string.properties_copied_clipboard),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    false
                }
            view.findViewById<LinearLayout>(R.id.properties_dialog_size)
                .setOnLongClickListener {
                    FileUtils.copyToClipboard(c, items)
                    Toast.makeText(
                        c,
                        c.getString(R.string.size) + " " +
                            c.getString(R.string.properties_copied_clipboard),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    false
                }
            view.findViewById<LinearLayout>(R.id.properties_dialog_date)
                .setOnLongClickListener {
                    FileUtils.copyToClipboard(c, date)
                    Toast.makeText(
                        c,
                        c.getString(R.string.date) + " " +
                            c.getString(R.string.properties_copied_clipboard),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    false
                }

            val countItemsOrAndSizeTask =
                CountItemsOrAndSizeTask(c, itemsText, baseFile, forStorage)
            countItemsOrAndSizeTask.executeOnExecutor(executor)
            val hashGen = GenerateHashesTask(baseFile, c, view)
            hashGen.executeOnExecutor(executor)

            val isRightToLeft = c.resources.getBoolean(R.bool.is_right_to_left)
//            val isDarkTheme = appTheme.materialDialogTheme === Theme.DARK
            val chart: PieChart = (view.findViewById(R.id.chart) as PieChart).also { chart ->
                chart.setTouchEnabled(false)
                chart.setDrawEntryLabels(false)
                chart.description = null
                chart.setNoDataText(c.getString(R.string.loading))
                chart.rotationAngle = if (!isRightToLeft) 0f else 180f
                chart.setHoleColor(Color.TRANSPARENT)
//                chart.setCenterTextColor(if (isDarkTheme) Color.WHITE else Color.BLACK)
                chart.legend.isEnabled = true
                chart.legend.form = Legend.LegendForm.CIRCLE
                chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                chart.legend.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
//                chart.legend.textColor = if (isDarkTheme) Color.WHITE else Color.BLACK
                chart.animateY(1000)
            }
            if (forStorage) {
                val LEGENDS = arrayOf(c.getString(R.string.used), c.getString(R.string.free))
                val COLORS = intArrayOf(
                    Utils.getColor(c, R.color.piechart_red),
                    Utils.getColor(c, R.color.piechart_green)
                )
                val totalSpace = baseFile.getTotal(c)
                val freeSpace = baseFile.usableSpace.toFloat()
                val usedSpace = (totalSpace - freeSpace).toFloat()
                val entries: MutableList<PieEntry> = ArrayList()
                entries.add(PieEntry(usedSpace, LEGENDS[0]))
                entries.add(PieEntry(freeSpace, LEGENDS[1]))
                val set = PieDataSet(entries, null)
                set.setColors(*COLORS)
                set.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                set.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                set.sliceSpace = 5f
                set.setAutomaticallyDisableSliceSpacing(true)
                set.valueLinePart2Length = 1.05f
                set.selectionShift = 0f
                val pieData = PieData(set)
                pieData.setValueFormatter(SizeFormatter(c))
//                pieData.setValueTextColor(if (isDarkTheme) Color.WHITE else Color.BLACK)
                val totalSpaceFormatted = Formatter.formatFileSize(c, totalSpace)
                chart.centerText = SpannableString(
                    """
                    ${c.getString(R.string.total)}
                    $totalSpaceFormatted
                    """.trimIndent()
                )
                chart.data = pieData
            } else {
                val loadFolderSpaceDataTask =
                    LoadFolderSpaceDataTask(c, appTheme, chart, baseFile)
                loadFolderSpaceDataTask.executeOnExecutor(executor)
            }
            chart.invalidate()
            if (!forStorage && showPermissions) {
                val main = (base as MainActivity).currentMainFragment
                val appCompatButton: AppCompatButton = view.findViewById(R.id.permissionsButton)
                appCompatButton.isAllCaps = true
                val permissionsTable = view.findViewById<View>(R.id.permtable)
                val button = view.findViewById<View>(R.id.set)
                if (isRoot && permissions!!.length > 6) {
                    appCompatButton.visibility = View.VISIBLE
                    appCompatButton.setOnClickListener { v15: View? ->
                        if (permissionsTable.visibility == View.GONE) {
                            permissionsTable.visibility = View.VISIBLE
                            button.visibility = View.VISIBLE
                            setPermissionsDialog(
                                permissionsTable,
                                button,
                                baseFile,
                                permissions,
                                c,
                                main
                            )
                        } else {
                            button.visibility = View.GONE
                            permissionsTable.visibility = View.GONE
                        }
                    }
                }
            }

            positiveButton(
                R.string.ok,
                click = {
                    if (baseFile.isDirectory && nomediaFile != null) {
                        if (nomediaCheckBox.isChecked) {
                            // checkbox is checked, create .nomedia
                            try {
                                if (!nomediaFile.createNewFile()) {
                                    // failed operation
                                    Log.w(
                                        TAG,
                                        "'.nomedia' file creation in ${baseFile.path} failed!"
                                    )
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        } else {
                            // checkbox is unchecked, delete .nomedia
                            if (!nomediaFile.delete()) {
                                // failed operation
                                Log.w(
                                    TAG,
                                    "'.nomedia' file deletion in ${baseFile.path} failed!"
                                )
                            }
                        }
                    }
                }
            )
            onDismiss { executor.shutdown() }
            getActionButton(WhichButton.POSITIVE).setTextColor(accentColor)
            getActionButton(WhichButton.NEGATIVE).setTextColor(accentColor)
            getActionButton(WhichButton.NEGATIVE).isEnabled = false
        }
    }

    @JvmStatic
    fun showCloudDialog(
        mainActivity: MainActivity,
        appTheme: AppTheme,
        openMode: OpenMode
    ) {
        val accentColor = mainActivity.accent
        MaterialDialog(mainActivity).show {
            message(R.string.cloud_remove)
            positiveButton(
                R.string.yes,
                click = {
                    mainActivity.deleteConnection(openMode)
                }
            )
            negativeButton(R.string.no, click = { cancel() })
            when (openMode) {
                OpenMode.DROPBOX -> title(R.string.cloud_dropbox)
                OpenMode.BOX -> title(R.string.cloud_box)
                OpenMode.GDRIVE -> title(R.string.cloud_drive)
                OpenMode.ONEDRIVE -> title(R.string.cloud_onedrive)
            }
            getActionButton(WhichButton.POSITIVE).setTextColor(accentColor)
            getActionButton(WhichButton.NEGATIVE).setTextColor(accentColor)
        }
    }

    @JvmStatic
    fun showEncryptWarningDialog(
        intent: Intent?,
        main: MainFragment,
        appTheme: AppTheme,
        encryptButtonCallbackInterface: EncryptButtonCallbackInterface
    ) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(main.context)
        MaterialDialog(main.requireContext()).show {
            title(R.string.warning)
            message(R.string.crypt_warning_key)
            positiveButton(
                R.string.warning_confirm,
                click = {
                    runCatching {
                        encryptButtonCallbackInterface.onButtonPressed(intent)
                    }.onFailure {
                        Toast.makeText(
                            main.activity,
                            main.getString(R.string.crypt_encryption_fail),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            )
            negativeButton(
                R.string.warning_never_show,
                click = {
                    preferences
                        .edit()
                        .putBoolean(PreferencesConstants.PREFERENCE_CRYPT_WARNING_REMEMBER, true)
                        .apply()
                    runCatching {
                        encryptButtonCallbackInterface.onButtonPressed(intent)
                    }.onFailure {
                        Toast.makeText(
                            main.activity,
                            main.getString(R.string.crypt_encryption_fail),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            )
        }
    }

    @JvmStatic
    fun showEncryptWithPresetPasswordSaveAsDialog(
        c: Context,
        main: MainActivity,
        password: String,
        intent: Intent
    ) {
        val intentParcelable: HybridFileParcelable =
            intent.getParcelableExtra(EncryptService.TAG_SOURCE)
        val saveAsDialog = showNameDialog(
            main,
            "", intentParcelable.getName(c) + CryptUtil.CRYPT_EXTENSION,
            c.getString(
                if (intentParcelable.isDirectory) {
                    R.string.encrypt_folder_save_as
                } else {
                    R.string.encrypt_file_save_as
                }
            ),
            c.getString(R.string.ok),
            null,
            c.getString(R.string.cancel),
            { dialog ->
                val textfield: EditText = dialog
                    .getCustomView()
                    .findViewById(R.id.singleedittext_input)
                intent.putExtra(
                    EncryptService.TAG_ENCRYPT_TARGET,
                    textfield.text.toString()
                )
                try {
                    EncryptDecryptUtils.startEncryption(
                        c, intentParcelable.path, password, intent
                    )
                } catch (e: GeneralSecurityException) {
                    e.printStackTrace()
                    Toast.makeText(
                        c,
                        R.string.crypt_encryption_fail,
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(
                        c,
                        R.string.crypt_encryption_fail,
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    dialog.dismiss()
                }
            }
        ) { text: String ->
            if (text.isEmpty()) {
                return@showNameDialog ReturnState(
                    ReturnState.STATE_ERROR, R.string.field_empty
                )
            }
            if (!text.endsWith(CryptUtil.CRYPT_EXTENSION)) {
                return@showNameDialog ReturnState(
                    ReturnState.STATE_ERROR,
                    R.string.encrypt_file_must_end_with_aze
                )
            }
            ReturnState()
        }
        saveAsDialog.getActionButton(WhichButton.POSITIVE).isEnabled = true
    }

    @JvmStatic
    fun showEncryptAuthenticateDialog(
        c: Context,
        intent: Intent,
        main: MainActivity,
        appTheme: AppTheme,
        encryptButtonCallbackInterface: EncryptButtonCallbackInterface
    ) {
        val accentColor = main.accent
        MaterialDialog(c).show {
            title(R.string.crypt_encrypt)
            customView(R.layout.dialog_encrypt_authenticate)
            val passwordEditText: TextInputEditText =
                view.findViewById(R.id.edit_text_dialog_encrypt_password)
            val passwordConfirmEditText: TextInputEditText =
                view.findViewById(R.id.edit_text_dialog_encrypt_password_confirm)
            val encryptSaveAsEditText: TextInputEditText =
                view.findViewById(R.id.edit_text_encrypt_save_as)
            val textInputLayoutPassword: WarnableTextInputLayout =
                view.findViewById(R.id.til_encrypt_password)
            val textInputLayoutPasswordConfirm: WarnableTextInputLayout =
                view.findViewById(R.id.til_encrypt_password_confirm)
            val textInputLayoutEncryptSaveAs: WarnableTextInputLayout =
                view.findViewById(R.id.til_encrypt_save_as)
            val intentParcelable: HybridFileParcelable =
                intent.getParcelableExtra(EncryptService.TAG_SOURCE)
            encryptSaveAsEditText.setText(
                intentParcelable.getName(c) + CryptUtil.CRYPT_EXTENSION
            )
            textInputLayoutEncryptSaveAs.hint = if (intentParcelable.isDirectory) {
                c.getString(R.string.encrypt_folder_save_as)
            } else {
                c.getString(R.string.encrypt_file_save_as)
            }
            passwordEditText.post {
                val imm =
                    main.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(passwordEditText, InputMethodManager.SHOW_IMPLICIT)
            }
            positiveButton(
                R.string.ok,
                click = { dialog ->
                    intent.putExtra(
                        EncryptService.TAG_ENCRYPT_TARGET, encryptSaveAsEditText.text.toString()
                    )
                    try {
                        encryptButtonCallbackInterface.onButtonPressed(
                            intent, passwordEditText.text.toString()
                        )
                    } catch (e: GeneralSecurityException) {
                        e.printStackTrace()
                        Toast.makeText(c, R.string.crypt_encryption_fail, Toast.LENGTH_LONG)
                            .show()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(c, R.string.crypt_encryption_fail, Toast.LENGTH_LONG)
                            .show()
                    } finally {
                        dialog.dismiss()
                    }
                }
            )
            negativeButton(R.string.cancel, click = { dialog -> dialog.cancel() })

            val btnOK = getActionButton(WhichButton.POSITIVE)
            btnOK.isEnabled = false
            val textWatcher: TextWatcher = object : SimpleTextWatcher() {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    btnOK.isEnabled = encryptSaveAsEditText.text.toString().isNotEmpty() &&
                        passwordEditText.text.toString().isNotEmpty() &&
                        passwordConfirmEditText.text.toString().isNotEmpty()
                }
            }
            passwordEditText.addTextChangedListener(textWatcher)
            passwordConfirmEditText.addTextChangedListener(textWatcher)
            encryptSaveAsEditText.addTextChangedListener(textWatcher)
            WarnableTextInputValidator(
                c,
                passwordEditText,
                textInputLayoutPassword,
                btnOK
            ) { text: String ->
                if (text.isEmpty()) {
                    ReturnState(
                        ReturnState.STATE_ERROR, R.string.field_empty
                    )
                }
                ReturnState()
            }
            WarnableTextInputValidator(
                c,
                passwordConfirmEditText,
                textInputLayoutPasswordConfirm,
                btnOK
            ) { text: String ->
                if (text != passwordEditText.text.toString()) {
                    ReturnState(
                        ReturnState.STATE_ERROR, R.string.password_no_match
                    )
                }
                ReturnState()
            }
            WarnableTextInputValidator(
                c,
                encryptSaveAsEditText,
                textInputLayoutEncryptSaveAs,
                btnOK
            ) { text: String ->
                if (text.isEmpty()) {
                    ReturnState(
                        ReturnState.STATE_ERROR, R.string.field_empty
                    )
                }
                if (!text.endsWith(CryptUtil.CRYPT_EXTENSION)) {
                    ReturnState(
                        ReturnState.STATE_ERROR,
                        R.string.encrypt_file_must_end_with_aze
                    )
                }
                ReturnState()
            }
            getActionButton(WhichButton.POSITIVE).setTextColor(accentColor)
            getActionButton(WhichButton.NEGATIVE).setTextColor(accentColor)
        }
    }

    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Throws(IOException::class, GeneralSecurityException::class)
    fun showDecryptFingerprintDialog(
        c: Context,
        main: MainActivity,
        intent: Intent?,
        appTheme: AppTheme,
        decryptButtonCallbackInterface: DecryptButtonCallbackInterface?
    ) {
        val dialog = MaterialDialog(c).show {
            title(R.string.crypt_decrypt)
            customView(R.layout.dialog_decrypt_fingerprint_authentication)
            cancelOnTouchOutside(false)
            view.findViewById<Button>(R.id.button_decrypt_fingerprint_cancel)
                .setOnClickListener {
                    this.cancel()
                }
            getActionButton(WhichButton.POSITIVE).setTextColor(main.accent)
            getActionButton(WhichButton.NEGATIVE).setTextColor(main.accent)
        }
        val manager = c.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
        val `object` = FingerprintManager.CryptoObject(CryptUtil.initCipher(c))
        val handler = FingerprintHandler(c, intent, dialog, decryptButtonCallbackInterface)
        handler.authenticate(manager, `object`)
    }

    @JvmStatic
    fun showDecryptDialog(
        c: Context?,
        main: MainActivity?,
        intent: Intent?,
        appTheme: AppTheme?,
        password: String,
        decryptButtonCallbackInterface: DecryptButtonCallbackInterface
    ) {
        showPasswordDialog(
            c!!,
            main!!,
            appTheme!!,
            R.string.crypt_decrypt,
            R.string.authenticate_password,
            { dialog ->
                val editText: EditText = dialog.view.findViewById(R.id.singleedittext_input)
                if (editText.text.toString() == password) {
                    decryptButtonCallbackInterface.confirm(intent)
                } else {
                    decryptButtonCallbackInterface.failed()
                }
                dialog.dismiss()
            },
            null
        )
    }

    @JvmStatic
    fun showPasswordDialog(
        c: Context,
        main: MainActivity,
        appTheme: AppTheme,
        @StringRes titleText: Int,
        @StringRes promptText: Int,
        positiveCallback: DialogCallback,
        negativeCallback: DialogCallback?
    ) {

        MaterialDialog(c).show {
            customView(R.layout.dialog_singleedittext)
            val wilTextfield: WarnableTextInputLayout =
                view.findViewById(R.id.singleedittext_warnabletextinputlayout)
            val textfield = view.findViewById<EditText>(R.id.singleedittext_input)
            textfield.setHint(promptText)
            textfield.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            noAutoDismiss()
            cancelOnTouchOutside(false)
            title(titleText)
            positiveButton(R.string.ok, click = positiveCallback)
            negativeButton(
                R.string.cancel,
                click = negativeCallback
                    ?: { dialog -> dialog.cancel() }
            )
            WarnableTextInputValidator(
                AppConfig.getInstance().mainActivityContext,
                textfield,
                wilTextfield,
                getActionButton(WhichButton.POSITIVE)
            ) { text: String ->
                if (text.isEmpty()) {
                    ReturnState(
                        ReturnState.STATE_ERROR, R.string.field_empty
                    )
                }
                ReturnState()
            }
            getActionButton(WhichButton.POSITIVE).setTextColor(main.accent)
            getActionButton(WhichButton.NEGATIVE).setTextColor(main.accent)
        }
    }

    @JvmStatic
    fun showSMBHelpDialog(m: Context, accentColor: Int) {
        MaterialDialog(m).show {
            message(R.string.smb_instructions)
            positiveButton(R.string.doit)
            getActionButton(WhichButton.POSITIVE).setTextColor(accentColor)
        }
    }

    @JvmStatic
    fun showPackageDialog(f: File, m: MainActivity) {
        MaterialDialog(m).show {
            title(R.string.package_installer)
            message(R.string.package_installer_text)
            positiveButton(
                R.string.install,
                click = {
                    FileUtils.installApk(f, m)
                }
            )
            negativeButton(
                R.string.view,
                click = {
                    m.openCompressed(f.path)
                }
            )
            neutralButton(R.string.cancel)
            getActionButton(WhichButton.POSITIVE).setTextColor(m.accent)
            getActionButton(WhichButton.NEGATIVE).setTextColor(m.accent)
            getActionButton(WhichButton.NEUTRAL).setTextColor(m.accent)
        }
    }

    @JvmStatic
    fun showArchiveDialog(f: File, m: MainActivity) {
        MaterialDialog(m).show {
            title(R.string.archive)
            message(R.string.archive_text)
            positiveButton(
                R.string.extract,
                click = {
                    m.mainActivityHelper.extractFile(f)
                }
            )
            negativeButton(
                R.string.view,
                click = {
                    m.openCompressed(Uri.fromFile(f).toString())
                }
            )
            neutralButton((R.string.cancel))
            if (!CompressedHelper.isFileExtractable(f.path)) {
                getActionButton(WhichButton.NEGATIVE).isEnabled = false
            }
            getActionButton(WhichButton.POSITIVE).setTextColor(m.accent)
            getActionButton(WhichButton.NEGATIVE).setTextColor(m.accent)
            getActionButton(WhichButton.NEUTRAL).setTextColor(m.accent)
        }
    }

    @JvmStatic
    fun showCompressDialog(
        m: MainActivity,
        b: ArrayList<HybridFileParcelable?>?,
        current: String
    ) {
        MaterialDialog(m).show {
            customView(R.layout.dialog_singleedittext)
            val etFilename = view.findViewById<EditText>(R.id.singleedittext_input)
            etFilename.setHint(R.string.enterzipname)
            etFilename.setText(".zip")
            etFilename.inputType = InputType.TYPE_CLASS_TEXT
            val tilFilename: WarnableTextInputLayout =
                view.findViewById(R.id.singleedittext_warnabletextinputlayout)
            title(R.string.enterzipname)
            positiveButton(
                R.string.create,
                click = {
                    val name = current + "/" + etFilename.text.toString()
                    m.mainActivityHelper.compressFiles(File(name), b)
                }
            )
            negativeButton(R.string.cancel)
            // place cursor at the starting of edit text by posting a runnable to edit text
            // this is done because in case android has not populated the edit text layouts yet,
            // it'll reset calls to selection if not posted in message queue
            etFilename.post { etFilename.setSelection(0) }
            WarnableTextInputValidator(
                m,
                etFilename,
                tilFilename,
                getActionButton(WhichButton.POSITIVE)
            ) { text: String ->
                val isValidFilename = FileProperties.isValidFilename(text)
                if (isValidFilename && text.isNotEmpty() &&
                    !text.lowercase().endsWith(".zip")
                ) {
                    ReturnState(
                        ReturnState.STATE_WARNING,
                        R.string.compress_file_suggest_zip_extension
                    )
                } else {
                    if (!isValidFilename) {
                        ReturnState(
                            ReturnState.STATE_ERROR, R.string.invalid_name
                        )
                    } else if (text.isEmpty()) {
                        ReturnState(
                            ReturnState.STATE_ERROR, R.string.field_empty
                        )
                    }
                }
                ReturnState()
            }
            getActionButton(WhichButton.POSITIVE).setTextColor(m.accent)
            getActionButton(WhichButton.NEGATIVE).setTextColor(m.accent)
            getActionButton(WhichButton.NEUTRAL).setTextColor(m.accent)
        }
    }

    @JvmStatic
    fun showSortDialog(
        m: MainFragment,
        appTheme: AppTheme,
        sharedPref: SharedPreferences
    ) {
        val path = m.currentPath
        val accentColor = m.mainActivity.accent
        val sort = m.resources.getStringArray(R.array.sortby)
        val current = SortHandler.getSortType(m.context, path)
        val sortbyOnlyThis =
            sharedPref.getStringSet(PreferencesConstants.PREFERENCE_SORTBY_ONLY_THIS, emptySet())
        val onlyThisFloders: MutableSet<String> = HashSet(sortbyOnlyThis)
        val onlyThis = onlyThisFloders.contains(path)
        var selectedIndex = -1
        MaterialDialog(m.requireActivity()).show {
            title(R.string.sort_by)
            listItemsSingleChoice(
                items = sort.toList(),
                initialSelection = if (current > 3) {
                    current - 4
                } else {
                    current
                },
                selection = { dialog, index, text ->
                    selectedIndex = index
                }
            )
            checkBoxPrompt(
                R.string.sort_only_this,
                isCheckedDefault = onlyThis,
                onToggle = { isChecked ->
                    if (isChecked) {
                        if (!onlyThisFloders.contains(path)) {
                            onlyThisFloders.add(path)
                        }
                    } else {
                        if (onlyThisFloders.contains(path)) {
                            onlyThisFloders.remove(path)
                        }
                    }
                }
            )
            positiveButton(
                R.string.descending,
                click = {
                    onSortTypeSelected(m, sharedPref, onlyThisFloders, it, selectedIndex, true)
                }
            )
            negativeButton(
                R.string.ascending,
                click = {
                    onSortTypeSelected(m, sharedPref, onlyThisFloders, it, selectedIndex, false)
                }
            )
        }
    }

    private fun onSortTypeSelected(
        m: MainFragment,
        sharedPref: SharedPreferences,
        onlyThisFloders: Set<String>,
        dialog: MaterialDialog,
        selectedIndex: Int,
        desc: Boolean
    ) {
        val sortType: Int = if (desc) selectedIndex + 4 else selectedIndex
        val sortHandler = SortHandler.getInstance()
        if (onlyThisFloders.contains(m.currentPath)) {
            val oldSort = sortHandler.findEntry(m.currentPath)
            val newSort = Sort(m.currentPath, sortType)
            if (oldSort == null) {
                sortHandler.addEntry(newSort)
            } else {
                sortHandler.updateEntry(oldSort, newSort)
            }
        } else {
            sortHandler.clear(m.currentPath)
            sharedPref.edit().putString("sortby", sortType.toString()).apply()
        }
        sharedPref
            .edit()
            .putStringSet(PreferencesConstants.PREFERENCE_SORTBY_ONLY_THIS, onlyThisFloders)
            .apply()
        m.updateList()
        dialog.dismiss()
    }

    @JvmStatic
    fun showHistoryDialog(
        dataUtils: DataUtils,
        sharedPrefs: SharedPreferences,
        m: MainFragment,
        appTheme: AppTheme
    ) {
        MaterialDialog(m.requireActivity()).show {
            positiveButton(R.string.cancel)
            negativeButton(
                R.string.clear,
                click = {
                    dataUtils.clearHistory()
                }
            )
            title(R.string.history)
            customListAdapter(
                HiddenAdapter(
                    m.activity,
                    m,
                    sharedPrefs,
                    FileUtils.toHybridFileArrayList(dataUtils.history),
                    null,
                    true
                ).also {
                    it.updateDialog(this)
                }
            )
        }
    }

    @JvmStatic
    fun showHiddenDialog(
        dataUtils: DataUtils,
        sharedPrefs: SharedPreferences?,
        m: MainFragment?,
        appTheme: AppTheme
    ) {
        m?.run {
            MaterialDialog(m.requireContext()).show {
                positiveButton(R.string.close)
                title(R.string.hiddenfiles)
                onDismiss {
                    m.mainActivity
                        .currentMainFragment!!
                        .loadlist(m.currentPath, false, OpenMode.UNKNOWN)
                }
                customListAdapter(
                    HiddenAdapter(
                        m.activity,
                        m,
                        sharedPrefs,
                        FileUtils.toHybridFileConcurrentRadixTree(dataUtils.hiddenFiles),
                        null,
                        false
                    ).also {
                        it.updateDialog(this)
                    }
                )
            }
        }
    }

    @JvmStatic
    fun setPermissionsDialog(
        v: View,
        but: View,
        file: HybridFile,
        f: String,
        context: Context?,
        mainFrag: MainFragment?
    ) {
        val readown = v.findViewById<CheckBox>(R.id.creadown)
        val readgroup = v.findViewById<CheckBox>(R.id.creadgroup)
        val readother = v.findViewById<CheckBox>(R.id.creadother)
        val writeown = v.findViewById<CheckBox>(R.id.cwriteown)
        val writegroup = v.findViewById<CheckBox>(R.id.cwritegroup)
        val writeother = v.findViewById<CheckBox>(R.id.cwriteother)
        val exeown = v.findViewById<CheckBox>(R.id.cexeown)
        val exegroup = v.findViewById<CheckBox>(R.id.cexegroup)
        val exeother = v.findViewById<CheckBox>(R.id.cexeother)
        if (f.length < 6) {
            v.visibility = View.GONE
            but.visibility = View.GONE
            Toast.makeText(context, R.string.not_allowed, Toast.LENGTH_SHORT).show()
            return
        }
        val arrayList = FileUtils.parse(f)
        val read = arrayList[0]
        val write = arrayList[1]
        val exe = arrayList[2]
        readown.isChecked = read[0]
        readgroup.isChecked = read[1]
        readother.isChecked = read[2]
        writeown.isChecked = write[0]
        writegroup.isChecked = write[1]
        writeother.isChecked = write[2]
        exeown.isChecked = exe[0]
        exegroup.isChecked = exe[1]
        exeother.isChecked = exe[2]
        but.setOnClickListener { v1: View? ->
            val perms = RootHelper.permissionsToOctalString(
                readown.isChecked,
                writeown.isChecked,
                exeown.isChecked,
                readgroup.isChecked,
                writegroup.isChecked,
                exegroup.isChecked,
                readother.isChecked,
                writeother.isChecked,
                exeother.isChecked
            )

            runCatching {
                ChangeFilePermissionsCommand.changeFilePermissions(
                    file.path,
                    perms,
                    file.isDirectory(context)
                ) { isSuccess: Boolean ->
                    if (isSuccess) {
                        Toast.makeText(context, R.string.done, Toast.LENGTH_LONG)
                            .show()
                    } else {
                        Toast.makeText(
                            context,
                            mainFrag!!.getString(R.string.operation_unsuccesful),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            }.onFailure {
                AppConfig.toast(context, R.string.root_failure)
                it.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun showChangePathsDialog(
        mainActivity: MainActivity,
        prefs: SharedPreferences
    ) {
        MaterialDialog(mainActivity).show {
            input(
                hint = null,
                prefill = mainActivity.currentMainFragment!!.currentPath,
                allowEmpty = false,
                waitForPositiveButton = false,
                callback = { dialog, text ->
                    val isAccessible = FileUtils.isPathAccessible(text.toString(), prefs)
                    dialog.getActionButton(WhichButton.POSITIVE).isEnabled = isAccessible
                }
            )
            title(R.string.enterpath)
            positiveButton(
                R.string.go,
                click = {
                    mainActivity
                        .currentMainFragment!!
                        .loadlist(it.getInputField().getText().toString(), false, OpenMode.UNKNOWN)
                }
            )
            negativeButton(R.string.cancel)
            getActionButton(WhichButton.POSITIVE).setTextColor(mainActivity.accent)
            getActionButton(WhichButton.NEGATIVE).setTextColor(mainActivity.accent)
        }
    }

    @JvmStatic
    fun showOtgSafExplanationDialog(
        themedActivity: ThemedActivity,
        positiveCallback: DialogCallback? = null
    ): MaterialDialog {
        return showBasicDialog(
            themedActivity,
            R.string.saf_otg_explanation,
            R.string.otg_access,
            R.string.ok,
            R.string.cancel, positiveCallback
        )
    }

    class SizeFormatter(private val context: Context) : IValueFormatter {
        override fun getFormattedValue(
            value: Float,
            entry: Entry,
            dataSetIndex: Int,
            viewPortHandler: ViewPortHandler
        ): String {
            val prefix = if (entry.data != null && entry.data is String) {
                entry.data as String
            } else {
                ""
            }
            return prefix + Formatter.formatFileSize(context, value.toLong())
        }
    }
}
