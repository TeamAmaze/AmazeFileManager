package com.amaze.filemanager.fragments.preference_fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.activities.PreferencesActivity;
import com.amaze.filemanager.ui.views.preference.PathSwitchPreference;
import com.amaze.filemanager.utils.BookSorter;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.SimpleTextWatcher;
import com.amaze.filemanager.utils.TinyDB;
import com.amaze.filemanager.utils.color.ColorUsage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.amaze.filemanager.activities.MainActivity.dataUtils;

/**
 * @author Emmanuel
 *         on 17/4/2017, at 22:49.
 */

public class FoldersPref extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    public static final String KEY = "name path list";
    public static final String KEY_SHORTCUT_PREF = "add_shortcut";

    private SharedPreferences sharedPrefs;
    private PreferencesActivity activity;
    private Map<Preference, Integer> position = new HashMap<>();
    private ArrayList<Shortcut> currentValue;
    private String numbPreferenceListener = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (PreferencesActivity) getActivity();

        //If dataUtils is null then there's no simple way to restore it,
        //except letting MainActivity deal with it
        if(dataUtils == null) {
            Intent i = new Intent(getActivity(), MainActivity.class);
            startActivity(i);
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.folders_prefs);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        currentValue = castStringListToTrioList(TinyDB.getList(sharedPrefs, String.class, KEY,
                getValue()));

        findPreference(KEY_SHORTCUT_PREF).setOnPreferenceClickListener(this);

        for (int i = 0; i < currentValue.size(); i++) {
            PathSwitchPreference p = new PathSwitchPreference(getActivity());
            p.setTitle(currentValue.get(i).name);
            p.setSummary(currentValue.get(i).directory);
            p.setChecked(currentValue.get(i).enabled);
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
                case PathSwitchPreference.SWITCH:
                    if(numbPreferenceListener != null
                            && numbPreferenceListener.equals(p.toString())) {
                        numbPreferenceListener = null;
                        return false;
                    }

                    Shortcut currentShortcut = currentValue.get(position.get(p));

                    if(currentShortcut.autodisabled) {
                        if(isRoot(currentShortcut.directory)) {
                            boolean showIfRoot = sharedPrefs.getBoolean(Preffrag.PREFERENCE_ROOTMODE, false);

                            if(!showIfRoot) {
                                Toast.makeText(getActivity(), R.string.rootfailure, Toast.LENGTH_SHORT).show();
                                numbPreferenceListener = p.toString();
                                p.setChecked(false);
                            } else {
                                currentShortcut = new Shortcut(p.getTitle().toString(),
                                        p.getSummary().toString(), Shortcut.FALSE);
                            }
                        } else if(isHidden(currentShortcut.directory)) {
                            boolean showIfHidden = sharedPrefs.getBoolean(Preffrag.PREFERENCE_SHOW_HIDDENFILES, false);

                            if(!showIfHidden) {
                                Toast.makeText(getActivity(), R.string.hiddenfailure, Toast.LENGTH_SHORT).show();
                                numbPreferenceListener = p.toString();
                                p.setChecked(false);
                            } else {
                                currentShortcut = new Shortcut(p.getTitle().toString(),
                                        p.getSummary().toString(), Shortcut.FALSE);
                            }
                        } else {
                            currentShortcut = new Shortcut(p.getTitle().toString(),
                                    p.getSummary().toString(), Shortcut.FALSE);
                        }
                    }

                    if(!currentShortcut.autodisabled) {
                        Shortcut shortcut = new Shortcut(p.getTitle().toString(), p.getSummary().toString(),
                                p.isChecked() ? Shortcut.TRUE : Shortcut.FALSE);

                        currentValue.set(position.get(p), shortcut);
                        TinyDB.putList(sharedPrefs, KEY, castTrioListToStringList(currentValue));
                    }
                    break;
                case PathSwitchPreference.DELETE:
                    loadDeleteDialog(preference);
                    break;
            }
        } else if(preference.getKey().equals(KEY_SHORTCUT_PREF)) {
            if(getPreferenceScreen().getPreferenceCount() >= findPreference(KEY_SHORTCUT_PREF).getOrder())
                findPreference(KEY_SHORTCUT_PREF).setOrder(getPreferenceScreen().getPreferenceCount()+10);

            loadCreateDialog();
        }

        return false;
    }

    public static ArrayList<Shortcut> castStringListToTrioList(ArrayList<String> arrayList) {
        ArrayList<Shortcut> newList = new ArrayList<>(arrayList.size());
        for(String s : arrayList) {
            newList.add(new Shortcut(s));
        }
        return newList;
    }

    protected static ArrayList<String> castTrioListToStringList(ArrayList<Shortcut> arrayList) {
        ArrayList<String> newList = new ArrayList<>(arrayList.size());
        for(Shortcut s : arrayList) {
            newList.add(s.toRestorableString());
        }
        return newList;
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

    private static boolean isHidden(String dir) {
        // TODO: 2/5/2017 use another system that doesn't create new object
        File f = new File(dir);
        return f.isHidden();
    }

    private ArrayList<String> getValue() {
        ArrayList<String> dflt = new ArrayList<>();

        ArrayList<String[]> books = dataUtils.getBooks();
        if (books != null && books.size() > 0) {
            Collections.sort(books, new BookSorter());

            for (String[] file : books) {
                dflt.add(new Shortcut(file[0], file[1], Shortcut.TRUE).toRestorableString());
            }
        }

        return dflt;
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

                        Shortcut shortcut = new Shortcut(editText1.getText().toString(),
                                editText2.getText().toString(), Shortcut.TRUE);

                        currentValue.add(shortcut);
                        TinyDB.putList(sharedPrefs, KEY, castTrioListToStringList(currentValue));
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
                        p.setTitle(editText1.getText());
                        p.setSummary(editText2.getText());

                        Shortcut shortcut = new Shortcut(editText1.getText().toString(),
                                editText2.getText().toString(),
                                p.isChecked()?Shortcut.TRUE:Shortcut.FALSE);

                        dataUtils.getBooks().set(position.get(p), new String[] {shortcut.name, shortcut.directory});

                        currentValue.set(position.get(p), shortcut);
                        TinyDB.putList(sharedPrefs, KEY, castTrioListToStringList(currentValue));
                        dialog.dismiss();
                    }
                });

        dialog.show();
    }

    private void loadDeleteDialog(final Preference p) {
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
                        TinyDB.putList(sharedPrefs, KEY, castTrioListToStringList(currentValue));
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

    /**
     * Contains 2 Strings and a boolean
     */
    public static class Shortcut {
        /**
         * For explanation check TinyDB.DIVIDER
         */
        static final String DIVIDER = "‚‗‗‚";

        /**
         * AUTOFALSE is set when a value has been changed to false not by the user but by changing
         * a setting that doesn't allow access to the folder anymore. MUST NOT let user change from
         * AUTOFALSE to TRUE or FALSE.
         */
        public static final String TRUE = "T", FALSE = "F", AUTOFALSE = "AF";

        public final String name;
        public final String directory;
        public final boolean enabled;
        public final boolean autodisabled;

        Shortcut(String name, String directory, String enabled) {
            this.name = name;
            this.directory = directory;
            this.enabled = enabled.equals(TRUE);
            this.autodisabled = enabled.equals(AUTOFALSE);
        }

        Shortcut(String divided) {
            String[] div = TextUtils.split(divided, DIVIDER);

            this.name = div[0];
            this.directory = div[1];
            this.enabled = div[2].equals(TRUE);
            this.autodisabled = div[2].equals(AUTOFALSE);
        }

        String toRestorableString() {
            return name + DIVIDER + directory + DIVIDER + (enabled ? TRUE:(autodisabled? AUTOFALSE:FALSE));
        }

        public String toString() {
            return "(" + name + ", " + directory + ", " + enabled + (autodisabled? "[AUTODISABLED]":"") + ")";
        }
    }

}
