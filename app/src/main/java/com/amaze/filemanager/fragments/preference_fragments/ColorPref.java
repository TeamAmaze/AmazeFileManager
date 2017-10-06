package com.amaze.filemanager.fragments.preference_fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.PreferencesActivity;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorPreference;
import com.amaze.filemanager.utils.color.ColorUsage;

import java.util.List;

/**
 * This class uses two sections, so that there doesn't need to be two different Fragments.
 * For sections info check switchSections() below.
 *
 * Created by Arpit on 21-06-2015 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com>
 */
public class ColorPref extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final int SECTION_0 = 0, SECTION_1 = 1;

    private static final String[] PREFERENCE_KEYS_SECTION_0 = {"colorednavigation",
            "selectcolorconfig", "random_checkbox"};
    private static final String[] PREFERENCE_KEYS_SECTION_1 = {"skin", "skin_two", "accent_skin", "icon_skin"};

    private int currentSection = SECTION_0;

    private MaterialDialog dialog;
    private SharedPreferences sharedPref;
    private PreferencesActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.color_prefs);

        activity = (PreferencesActivity) getActivity();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (Build.VERSION.SDK_INT >= 21) {
            findPreference("colorednavigation").setEnabled(true);
        }

        reloadListeners();
    }

    @Override
    public void onPause() {
        if (dialog != null) dialog.dismiss();
        super.onPause();
    }

    /**
     * Deal with the "up" button going to last fragment, instead of section 0.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && currentSection != SECTION_0) {
            switchSections();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean onBackPressed() {
        if(currentSection != SECTION_0) {
            switchSections();
            return true;//dealt with click
        } else {
            return false;
        }
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        if (activity != null) activity.setChanged();

        switch(preference.getKey()) {
            case "random_checkbox":
                if (activity != null) activity.setChanged();
                Toast.makeText(getActivity(), R.string.setRandom, Toast.LENGTH_LONG).show();
                return true;
            case "colorednavigation":
                if (activity != null) activity.setChanged();
                return true;
            case "skin":
            case "skin_two":
            case "accent_skin":
            case "icon_skin":
                final ColorUsage usage = ColorUsage.fromString(preference.getKey());
                if (usage != null) {
                    ColorAdapter adapter = new ColorAdapter(getActivity(), ColorPreference.availableColors, usage);

                    GridView v = (GridView) getActivity().getLayoutInflater().inflate(R.layout.dialog_grid, null);
                    v.setAdapter(adapter);
                    v.setOnItemClickListener(adapter);

                    int fab_skin = activity.getColorPreference().getColor(ColorUsage.ACCENT);
                    dialog = new MaterialDialog.Builder(getActivity())
                            .positiveText(R.string.cancel)
                            .title(R.string.choose_color)
                            .theme(activity.getAppTheme().getMaterialDialogTheme())
                            .autoDismiss(true)
                            .positiveColor(fab_skin)
                            .neutralColor(fab_skin)
                            .neutralText(R.string.defualt)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onNeutral(MaterialDialog dialog) {
                                    super.onNeutral(dialog);
                                    activity.getColorPreference()
                                            .setRes(usage, usage.getDefaultColor())
                                            .saveToPreferences(sharedPref);
                                }
                            })
                            .customView(v, false)
                            .build();

                    adapter.setDialog(dialog);
                    dialog.show();
                }
            return false;
            case "selectcolorconfig":
                switchSections();
                return true;
        }

        return false;
    }

    private void switchSections() {
        getPreferenceScreen().removeAll();

        if(currentSection == SECTION_0) {
            currentSection = SECTION_1;
            addPreferencesFromResource(R.xml.conficolor_prefs);
        } else if(currentSection == SECTION_1) {
            currentSection = SECTION_0;
            addPreferencesFromResource(R.xml.color_prefs);
        }

        reloadListeners();
    }

    private void reloadListeners() {
        for (final String PREFERENCE_KEY :
                (currentSection == SECTION_0? PREFERENCE_KEYS_SECTION_0:PREFERENCE_KEYS_SECTION_1)) {
            findPreference(PREFERENCE_KEY).setOnPreferenceClickListener(this);
        }
    }

    private class ColorAdapter extends ArrayAdapter<Integer> implements AdapterView.OnItemClickListener {
        private String prefKey;
        private ColorUsage usage;
        @ColorInt
        private int selectedColor;
        private MaterialDialog dialog;

        public void setDialog(MaterialDialog b) {
            this.dialog = b;
        }

        /**
         * Constructor for adapter that handles the view creation of color chooser dialog in preferences
         *
         * @param context the context
         * @param colors  array list of color hex values in form of string; for the views
         * @param usage   the preference usage for setting new selected color preference value
         */
        ColorAdapter(Context context, List<Integer> colors, ColorUsage usage) {
            super(context, R.layout.rowlayout, colors);
            this.prefKey = usage.asString();
            this.usage = usage;
            this.selectedColor = activity.getColorPreference().getColor(usage);
        }

        @ColorInt
        private int getColor(@ColorRes int colorRes) {
            return Utils.getColor(getContext(), colorRes);
        }

        @ColorRes
        private int getColorResAt(int position) {
            Integer item = getItem(position);

            if (item == null) {
                return usage.getDefaultColor();
            } else {
                return item;
            }
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //TODO solve unconditional layout inflation
            View rowView = inflater.inflate(R.layout.dialog_grid_item, parent, false);

            int color = getColor(getColorResAt(position));

            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
            if (color == selectedColor)
                imageView.setImageResource(R.drawable.ic_checkmark_selected);
            GradientDrawable gradientDrawable = (GradientDrawable) imageView.getBackground();

            gradientDrawable.setColor(color);

            return rowView;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int selectedColorRes = getColorResAt(position);

            activity.getColorPreference().setRes(usage, selectedColorRes).saveToPreferences(sharedPref);

            if (dialog != null) dialog.dismiss();
        }
    }
}
