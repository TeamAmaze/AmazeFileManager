package com.amaze.filemanager.fragments.preference_fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.PreferencesActivity;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.ui.views.preference.PathSwitchPreference;
import com.amaze.filemanager.utils.AppConfig;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.SimpleTextWatcher;
import com.amaze.filemanager.utils.color.ColorUsage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Emmanuel
 *         on 17/4/2017, at 22:49.
 */

public class FoldersPref extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    public static final String KEY_SHORTCUT_PREF = "add_shortcut";

    private SharedPreferences sharedPrefs;
    private PreferencesActivity activity;
    private Map<Preference, Integer> position = new HashMap<>();
    private ArrayList<String[]> currentValue;
    private DataUtils dataUtils;
    private UtilsHandler utilsHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (PreferencesActivity) getActivity();

        utilsHandler = new UtilsHandler(getActivity());
        dataUtils = dataUtils.getInstance();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.folders_prefs);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        currentValue = dataUtils.getBooks();

        findPreference(KEY_SHORTCUT_PREF).setOnPreferenceClickListener(this);

        for (int i = 0; i < currentValue.size(); i++) {
            PathSwitchPreference p = new PathSwitchPreference(getActivity());
            p.setTitle(currentValue.get(i) [0]);
            p.setSummary(currentValue.get(i) [1]);
            p.setOnPreferenceClickListener(this);

            position.put(p, i);
            getPreferenceScreen().addPreference(p);
        }
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        if (sharedPrefs != null) activity.setChanged();

        if (preference instanceof PathSwitchPreference) {
            PathSwitchPreference p = (PathSwitchPreference) preference;
            switch (p.getLastItemClicked()) {
                case PathSwitchPreference.EDIT:
                    loadEditDialog((PathSwitchPreference) preference);
                    break;
                case PathSwitchPreference.DELETE:
                    loadDeleteDialog((PathSwitchPreference) preference);
                    break;
                default:
                    break;
            }
        } else if(preference.getKey().equals(KEY_SHORTCUT_PREF)) {
            if(getPreferenceScreen().getPreferenceCount() >= findPreference(KEY_SHORTCUT_PREF).getOrder())
                findPreference(KEY_SHORTCUT_PREF).setOrder(getPreferenceScreen().getPreferenceCount()+10);

            loadCreateDialog();
        }

        return false;
    }

    public static boolean canShortcutTo(String dir, SharedPreferences pref) {
        File f = new File(dir);
        boolean showIfHidden = pref.getBoolean(Preffrag.PREFERENCE_SHOW_HIDDENFILES, false),
                isDirSelfOrParent = dir.endsWith("/.") || dir.endsWith("/.."),
                showIfRoot = pref.getBoolean(Preffrag.PREFERENCE_ROOTMODE, false);

        return f.exists() && f.isDirectory()
                && (!f.isHidden() || (showIfHidden && !isDirSelfOrParent))
                && (!isRoot(dir) || showIfRoot);

        // TODO: 2/5/2017 use another system that doesn't create new object
    }

    private static boolean isRoot(String dir) {// TODO: 5/5/2017 hardcoding root might lead to problems down the line
        return !dir.contains(OTGUtil.PREFIX_OTG) && !dir.startsWith("/storage");
    }

    private void loadCreateDialog() {
        int fab_skin = activity.getColorPreference().getColor(ColorUsage.ACCENT);

        LayoutInflater li = LayoutInflater.from(activity);
        final View v = li.inflate(R.layout.dialog_twoedittexts, null);// TODO: 29/4/2017 make this null not null
        ((TextInputLayout) v.findViewById(R.id.text_input1)).setHint(getString(R.string.name));
        ((TextInputLayout) v.findViewById(R.id.text_input2)).setHint(getString(R.string.directory));

        final AppCompatEditText editText1 = ((AppCompatEditText) v.findViewById(R.id.text1)),
                editText2 = ((AppCompatEditText) v.findViewById(R.id.text2));

        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.create_shortcut)
                .theme(activity.getAppTheme().getMaterialDialogTheme())
                .positiveColor(fab_skin)
                .positiveText(R.string.create)
                .negativeColor(fab_skin)
                .negativeText(android.R.string.cancel)
                .customView(v, false)
                .build();

        dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);

        disableButtonIfTitleEmpty(editText1, dialog);
        disableButtonIfNotPath(editText2, dialog);

        dialog.getActionButton(DialogAction.POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PathSwitchPreference p = new PathSwitchPreference(getActivity());
                        p.setTitle(editText1.getText());
                        p.setSummary(editText2.getText());
                        p.setOnPreferenceClickListener(FoldersPref.this);

                        position.put(p, currentValue.size());
                        getPreferenceScreen().addPreference(p);

                        String[] values = new String[] {editText1.getText().toString(),
                                editText2.getText().toString()};
                        currentValue.add(values);

                        dataUtils.addBook(values);
                        AppConfig.runInBackground(new Runnable() {
                            @Override
                            public void run() {

                                utilsHandler.addBookmark(editText1.getText().toString(), editText2.getText().toString());
                            }
                        });

                        dialog.dismiss();
                    }
                });

        dialog.show();
    }

    private void loadEditDialog(final PathSwitchPreference p) {
        int fab_skin = activity.getColorPreference().getColor(ColorUsage.ACCENT);

        LayoutInflater li = LayoutInflater.from(activity);
        final View v = li.inflate(R.layout.dialog_twoedittexts, null);// TODO: 29/4/2017 make this null not null
        ((TextInputLayout) v.findViewById(R.id.text_input1)).setHint(getString(R.string.name));
        ((TextInputLayout) v.findViewById(R.id.text_input2)).setHint(getString(R.string.directory));

        final EditText editText1 = ((EditText) v.findViewById(R.id.text1)),
                editText2 = ((EditText) v.findViewById(R.id.text2));
        editText1.setText(p.getTitle());
        editText2.setText(p.getSummary());

        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.edit_shortcut)
                .theme(activity.getAppTheme().getMaterialDialogTheme())
                .positiveColor(fab_skin)
                .positiveText(getString(R.string.edit).toUpperCase())// TODO: 29/4/2017 don't use toUpperCase()
                .negativeColor(fab_skin)
                .negativeText(android.R.string.cancel)
                .customView(v, false)
                .build();

        dialog.getActionButton(DialogAction.POSITIVE)
                .setEnabled(canShortcutTo(editText2.getText().toString(), sharedPrefs));

        disableButtonIfTitleEmpty(editText1, dialog);
        disableButtonIfNotPath(editText2, dialog);

        dialog.getActionButton(DialogAction.POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        final String oldName = p.getTitle().toString();
                        final String oldPath = p.getSummary().toString();

                        p.setTitle(editText1.getText());
                        p.setSummary(editText2.getText());

                        String[] values = new String[] {editText1.getText().toString(),
                                editText2.getText().toString()};

                        currentValue.set(position.get(p), values);

                        dataUtils.removeBook(position.get(p));
                        dataUtils.addBook(new String[] {editText1.getText().toString(),
                                editText2.getText().toString()});
                        AppConfig.runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                utilsHandler.renameBookmark(oldName, oldPath,
                                        editText1.getText().toString(),
                                        editText2.getText().toString());
                            }
                        });
                        dialog.dismiss();
                    }
                });

        dialog.show();
    }

    private void loadDeleteDialog(final PathSwitchPreference p) {
        int fab_skin = activity.getColorPreference().getColor(ColorUsage.ACCENT);

        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.questiondelete_shortcut)
                .theme(activity.getAppTheme().getMaterialDialogTheme())
                .positiveColor(fab_skin)
                .positiveText(getString(R.string.delete).toUpperCase())// TODO: 29/4/2017 don't use toUpperCase()
                .negativeColor(fab_skin)
                .negativeText(android.R.string.cancel)
                .build();

        dialog.getActionButton(DialogAction.POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getPreferenceScreen().removePreference(p);
                        currentValue.remove((int) position.get(p));

                        dataUtils.removeBook(position.get(p));

                        AppConfig.runInBackground(new Runnable() {
                            @Override
                            public void run() {
                                utilsHandler.removeBookmarksPath(p.getTitle().toString(),
                                        p.getSummary().toString());
                            }
                        });
                        dialog.dismiss();
                    }
                });

        dialog.show();
    }

    private void disableButtonIfNotPath(EditText path, final MaterialDialog dialog) {
        path.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                dialog.getActionButton(DialogAction.POSITIVE)
                        .setEnabled(canShortcutTo(s.toString(), sharedPrefs));
            }
        });
    }

    private void disableButtonIfTitleEmpty(final EditText title, final MaterialDialog dialog) {
        title.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                dialog.getActionButton(DialogAction.POSITIVE).setEnabled(title.length() > 0);
            }
        });
    }
}
