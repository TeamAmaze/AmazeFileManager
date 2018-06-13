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
import com.amaze.filemanager.database.models.OperationData;
import com.amaze.filemanager.ui.views.preference.PathSwitchPreference;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.SimpleTextWatcher;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.utils.files.FileUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Emmanuel
 *         on 17/4/2017, at 22:49.
 */

public class FoldersPref extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private SharedPreferences sharedPrefs;
    private PreferencesActivity activity;
    private Map<Preference, Integer> position = new HashMap<>();
    private DataUtils dataUtils;
    private UtilsHandler utilsHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (PreferencesActivity) getActivity();

        utilsHandler = new UtilsHandler(getActivity());
        dataUtils = DataUtils.getInstance();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.folders_prefs);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        findPreference(PreferencesConstants.PREFERENCE_SHORTCUT).setOnPreferenceClickListener(this);

        for (int i = 0; i < dataUtils.getBooks().size(); i++) {
            PathSwitchPreference p = new PathSwitchPreference(getActivity());
            p.setTitle(dataUtils.getBooks().get(i) [0]);
            p.setSummary(dataUtils.getBooks().get(i) [1]);
            p.setOnPreferenceClickListener(this);

            position.put(p, i);
            getPreferenceScreen().addPreference(p);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        onCreate(null);
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
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
        } else if(preference.getKey().equals(PreferencesConstants.PREFERENCE_SHORTCUT)) {
            if(getPreferenceScreen().getPreferenceCount() >= findPreference(PreferencesConstants.PREFERENCE_SHORTCUT).getOrder())
                findPreference(PreferencesConstants.PREFERENCE_SHORTCUT).setOrder(getPreferenceScreen().getPreferenceCount()+10);

            loadCreateDialog();
        }

        return false;
    }

    private void loadCreateDialog() {
        int fab_skin = activity.getAccent();

        LayoutInflater li = LayoutInflater.from(activity);
        final View v = li.inflate(R.layout.dialog_twoedittexts, null);// TODO: 29/4/2017 make this null not null
        ((TextInputLayout) v.findViewById(R.id.text_input1)).setHint(getString(R.string.name));
        ((TextInputLayout) v.findViewById(R.id.text_input2)).setHint(getString(R.string.directory));

        final AppCompatEditText editText1 = v.findViewById(R.id.text1),
                editText2 = v.findViewById(R.id.text2);

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
                .setOnClickListener(view -> {
                    PathSwitchPreference p = new PathSwitchPreference(getActivity());
                    p.setTitle(editText1.getText());
                    p.setSummary(editText2.getText());
                    p.setOnPreferenceClickListener(FoldersPref.this);

                    position.put(p, dataUtils.getBooks().size());
                    getPreferenceScreen().addPreference(p);

                    String[] values = new String[] {editText1.getText().toString(),
                            editText2.getText().toString()};

                    dataUtils.addBook(values);
                    utilsHandler.saveToDatabase(new OperationData(UtilsHandler.Operation.BOOKMARKS,
                            editText2.getText().toString(), editText1.getText().toString()));

                    dialog.dismiss();
                });

        dialog.show();
    }

    private void loadEditDialog(final PathSwitchPreference p) {
        int fab_skin = activity.getAccent();

        LayoutInflater li = LayoutInflater.from(activity);
        final View v = li.inflate(R.layout.dialog_twoedittexts, null);// TODO: 29/4/2017 make this null not null
        ((TextInputLayout) v.findViewById(R.id.text_input1)).setHint(getString(R.string.name));
        ((TextInputLayout) v.findViewById(R.id.text_input2)).setHint(getString(R.string.directory));

        final EditText editText1 = v.findViewById(R.id.text1),
                editText2 = v.findViewById(R.id.text2);
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
                .setEnabled(FileUtils.isPathAccessible(editText2.getText().toString(), sharedPrefs));

        disableButtonIfTitleEmpty(editText1, dialog);
        disableButtonIfNotPath(editText2, dialog);

        dialog.getActionButton(DialogAction.POSITIVE)
                .setOnClickListener(view -> {

                    final String oldName = p.getTitle().toString();
                    final String oldPath = p.getSummary().toString();


                    dataUtils.removeBook(position.get(p));
                    position.remove(p);
                    getPreferenceScreen().removePreference(p);

                    p.setTitle(editText1.getText());
                    p.setSummary(editText2.getText());

                    position.put(p, position.size());
                    getPreferenceScreen().addPreference(p);

                    String[] values = new String[] {editText1.getText().toString(),
                            editText2.getText().toString()};

                    dataUtils.addBook(values);
                    AppConfig.runInBackground(() -> utilsHandler.renameBookmark(oldName, oldPath,
                            editText1.getText().toString(),
                            editText2.getText().toString()));
                    dialog.dismiss();
                });

        dialog.show();
    }

    private void loadDeleteDialog(final PathSwitchPreference p) {
        int fab_skin = activity.getAccent();

        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.questiondelete_shortcut)
                .theme(activity.getAppTheme().getMaterialDialogTheme())
                .positiveColor(fab_skin)
                .positiveText(getString(R.string.delete).toUpperCase())// TODO: 29/4/2017 don't use toUpperCase(), 20/9,2017 why not?
                .negativeColor(fab_skin)
                .negativeText(android.R.string.cancel)
                .build();

        dialog.getActionButton(DialogAction.POSITIVE)
                .setOnClickListener(view -> {

                    dataUtils.removeBook(position.get(p));

                    utilsHandler.removeFromDatabase(new OperationData(UtilsHandler.Operation.BOOKMARKS,
                            p.getTitle().toString(), p.getSummary().toString()));

                    getPreferenceScreen().removePreference(p);
                    position.remove(p);
                    dialog.dismiss();
                });

        dialog.show();
    }

    private void disableButtonIfNotPath(EditText path, final MaterialDialog dialog) {
        path.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                dialog.getActionButton(DialogAction.POSITIVE)
                        .setEnabled(FileUtils.isPathAccessible(s.toString(), sharedPrefs));
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
