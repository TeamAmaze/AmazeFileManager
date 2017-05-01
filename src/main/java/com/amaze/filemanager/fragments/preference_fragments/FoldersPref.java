package com.amaze.filemanager.fragments.preference_fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.PreferencesActivity;
import com.amaze.filemanager.ui.views.preference.NamePathSwitchPreference;
import com.amaze.filemanager.utils.BookSorter;
import com.amaze.filemanager.utils.TinyDB;
import com.amaze.filemanager.utils.color.ColorUsage;

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

    private SharedPreferences preferences;
    private PreferencesActivity activity;
    private Map<Preference, Integer> position = new HashMap<>();
    private ArrayList<Trio> currentValue;
    private Preference.OnPreferenceClickListener onPreferenceClickListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (PreferencesActivity) getActivity();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.folders_prefs);

        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        currentValue = castStringListToTrioList(TinyDB.getList(preferences, String.class, KEY,
                getValue()));

        onPreferenceClickListener = this;

        findPreference(KEY_SHORTCUT_PREF).setOnPreferenceClickListener(onPreferenceClickListener);

        for (int i = 0; i < currentValue.size(); i++) {
            NamePathSwitchPreference p = new NamePathSwitchPreference(getActivity());
            p.setTitle(currentValue.get(i).first);
            p.setSummary(currentValue.get(i).second);
            p.setChecked(currentValue.get(i).third);
            p.setOnPreferenceClickListener(onPreferenceClickListener);

            position.put(p, i);
            getPreferenceScreen().addPreference(p);
        }
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        if (preferences != null) activity.setChanged();

        if (preference instanceof NamePathSwitchPreference) {
            NamePathSwitchPreference p = (NamePathSwitchPreference) preference;
            switch (p.getLastItemClicked()) {
                case NamePathSwitchPreference.EDIT:
                    loadEditDialog((NamePathSwitchPreference) preference);
                    break;
                case NamePathSwitchPreference.SWITCH:
                    Trio trio = new Trio(p.getTitle().toString(),  p.getSummary().toString(),
                            p.isChecked());

                    dataUtils.getBooks().set(position.get(p), new String[] {trio.first, trio.second});

                    currentValue.set(position.get(p), trio);
                    TinyDB.putList(preferences, KEY, castTrioListToStringList(currentValue));
                    break;
                case NamePathSwitchPreference.DELETE:
                    loadDeleteDialog(preference);
                    break;
            }
        } else if(preference.getKey().equals(KEY_SHORTCUT_PREF)) {
            loadCreateDialog();
        }

        return false;
    }

    public static ArrayList<Trio> castStringListToTrioList(ArrayList<String> arrayList) {
        ArrayList<Trio> newList = new ArrayList<>(arrayList.size());
        for(String s : arrayList) {
            newList.add(new Trio(s));
        }
        return newList;
    }

    protected ArrayList<String> castTrioListToStringList(ArrayList<Trio> arrayList) {
        ArrayList<String> newList = new ArrayList<>(arrayList.size());
        for(Trio s : arrayList) {
            newList.add(s.toRestorableString());
        }
        return newList;
    }

    private ArrayList<String> getValue() {
        ArrayList<String> dflt = new ArrayList<>();

        ArrayList<String[]> books = dataUtils.getBooks();
        if (books != null && books.size() > 0) {
            Collections.sort(books, new BookSorter());

            for (String[] file : books) {
                dflt.add(new Trio(file[0], file[1], true).toRestorableString());
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

        dialog.getActionButton(DialogAction.POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        NamePathSwitchPreference p = new NamePathSwitchPreference(getActivity());
                        p.setTitle(editText1.getText());
                        p.setSummary(editText2.getText());
                        p.setOnPreferenceClickListener(onPreferenceClickListener);

                        position.put(p, currentValue.size());
                        getPreferenceScreen().addPreference(p);

                        Trio trio = new Trio(editText1.getText().toString(),
                                editText2.getText().toString(), true);

                        dataUtils.addBook(new String[] {trio.first, trio.second});

                        currentValue.add(trio);
                        TinyDB.putList(preferences, KEY, castTrioListToStringList(currentValue));
                        dialog.dismiss();
                    }
                });

        dialog.show();
    }

    private void loadEditDialog(final NamePathSwitchPreference p) {
        int fab_skin = activity.getColorPreference().getColor(ColorUsage.ACCENT);

        LayoutInflater li = LayoutInflater.from(activity);
        final View v = li.inflate(R.layout.dialog_twoedittexts, null);// TODO: 29/4/2017 make this null not null
        ((TextInputLayout) v.findViewById(R.id.text_input1)).setHint(getString(R.string.name));
        ((TextInputLayout) v.findViewById(R.id.text_input2)).setHint(getString(R.string.directory));

        final AppCompatEditText editText1 = ((AppCompatEditText) v.findViewById(R.id.text1)),
                editText2 = ((AppCompatEditText) v.findViewById(R.id.text2));
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
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        p.setTitle(editText1.getText());
                        p.setSummary(editText2.getText());

                        Trio trio = new Trio(editText1.getText().toString(),
                                editText2.getText().toString(),
                                p.isChecked());

                        dataUtils.getBooks().set(position.get(p), new String[] {trio.first, trio.second});

                        currentValue.set(position.get(p), trio);
                        TinyDB.putList(preferences, KEY, castTrioListToStringList(currentValue));
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
                        dataUtils.removeBook(position.get(p));
                        getPreferenceScreen().removePreference(p);
                        currentValue.remove((int) position.get(p));
                        TinyDB.putList(preferences, KEY, castTrioListToStringList(currentValue));
                        dialog.dismiss();
                    }
                });

        dialog.show();
    }

    /**
     * Contains 2 Strings and a boolean
     */
    public static class Trio {
        /**
         * For explanation check TinyDB.DIVIDER
         */
        static final String DIVIDER = "‚‗‗‚";

        static final String TRUE = "T", FALSE = "F";

        public final String first;
        public final String second;
        public final boolean third;

        Trio(String first, String second, boolean third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        Trio(String divided) {
            String[] div = TextUtils.split(divided, DIVIDER);

            this.first = div[0];
            this.second = div[1];
            this.third = div[2].equals(TRUE);
        }

        String toRestorableString() {
            return first + DIVIDER + second + DIVIDER + (third? TRUE:FALSE);
        }

        public String toString() {
            return "(" + first + ", " + second + ", " + third + ")";
        }
    }

}
