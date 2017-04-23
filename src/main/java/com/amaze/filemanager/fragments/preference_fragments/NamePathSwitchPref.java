package com.amaze.filemanager.fragments.preference_fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.ui.views.preference.NamePathSwitchPreference;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;

/**
 * @author Emmanuel
 *         on 19/4/2017, at 12:24.
 */

public class NamePathSwitchPref extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    SharedPreferences sharedPref;
    com.amaze.filemanager.activities.Preferences preferences;
    BaseActivity activity;
    UtilitiesProviderInterface utilsProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (BaseActivity) getActivity();
        utilsProvider = (UtilitiesProviderInterface) getActivity();

        // Load the preferences from an XML resource
        addPreferencesFromResource(getPreferenceResource());
        preferences = (com.amaze.filemanager.activities.Preferences) getActivity();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++)
            getPreferenceScreen().getPreference(i).setOnPreferenceClickListener(this);
    }

    public int getPreferenceResource() {
        return 0;
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        if (preferences != null) preferences.setChanged();
        if (preference instanceof NamePathSwitchPreference) {
            NamePathSwitchPreference p = (NamePathSwitchPreference) preference;
            switch (p.getLastItemClicked()) {
                case NamePathSwitchPreference.EDIT:
                    loadEditDialog();
                    break;
                case NamePathSwitchPreference.SWITCH:
                    //Deactivation is automatically dealt with by the Preference
                    break;
                case NamePathSwitchPreference.DELETE:
                    loadDeleteDialog(preference);
                    break;
            }
        }
        return false;
    }

    private void loadEditDialog() {
        int fab_skin = activity.getColorPreference().getColor(ColorUsage.ACCENT);

        LayoutInflater li = LayoutInflater.from(activity);
        View customView = li.inflate(R.layout.dialog_twoedittexts, null);
        ((TextInputLayout) customView.findViewById(R.id.text_input1)).setHint(getString(R.string.name));
        ((TextInputLayout) customView.findViewById(R.id.text_input2)).setHint(getString(R.string.directory));

        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.create_quick_access)
                .theme(utilsProvider.getAppTheme().getMaterialDialogTheme())
                .positiveColor(fab_skin)
                .positiveText(R.string.create)
                .negativeColor(fab_skin)
                .negativeText(R.string.cancel)
                .customView(customView, false)
                .build();

        dialog.getActionButton(DialogAction.POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO: 23/4/2017 modif element in shared prefs
                        dialog.dismiss();
                    }
                });

        dialog.show();
    }

    private void loadDeleteDialog(final Preference p) {
        int fab_skin = activity.getColorPreference().getColor(ColorUsage.ACCENT);

        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.questiondelete)
                .theme(utilsProvider.getAppTheme().getMaterialDialogTheme())
                .positiveColor(fab_skin)
                .positiveText(R.string.yes)
                .negativeColor(fab_skin)
                .negativeText(R.string.no)
                .build();

        dialog.getActionButton(DialogAction.POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getPreferenceScreen().removePreference(p);
                        // TODO: 23/4/2017 delete element from shared prefs
                        dialog.dismiss();
                    }
                });

        dialog.show();
    }

}
