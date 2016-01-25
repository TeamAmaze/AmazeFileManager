package com.amaze.filemanager.fragments.preference_fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.views.CheckBx;
import com.amaze.filemanager.utils.PreferenceUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Arpit on 21-06-2015.
 */
public class ColorPref extends PreferenceFragment implements Preference.OnPreferenceClickListener  {

    SharedPreferences sharedPref;
    int theme;
    com.amaze.filemanager.activities.Preferences preferences;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.color_prefs);
        preferences=(com.amaze.filemanager.activities.Preferences)getActivity();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final int th1 = Integer.parseInt(sharedPref.getString("theme", "0"));
        theme = th1==2 ? PreferenceUtils.hourOfDay() : th1;

        final CheckBx checkBoxPreference = (CheckBx) findPreference("random_checkbox");
        checkBoxPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if(preferences!=null)preferences.changed=1;
                Toast.makeText(getActivity(), R.string.setRandom, Toast.LENGTH_LONG).show();
                return true;
            }
        });
        CheckBx preference8 = (CheckBx) findPreference("colorednavigation");
        preference8.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if(preferences!=null)preferences.changed=1;
                return true;
            }
        });
        if (Build.VERSION.SDK_INT >= 21)
            preference8.setEnabled(true);

    findPreference("skin").setOnPreferenceClickListener(this);
        findPreference("fab_skin").setOnPreferenceClickListener(this);
        findPreference("icon_skin").setOnPreferenceClickListener(this);
    }
    @Override
    public boolean onPreferenceClick(final Preference preference) {
        if(preferences!=null)preferences.changed=1;
        final MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
        a.positiveText(R.string.cancel);
        a.title(R.string.choose_color);
        if(theme==1)
            a.theme(Theme.DARK);

        a.autoDismiss(true);
        int fab_skin=Color.parseColor(PreferenceUtils.getAccentString(sharedPref));
        int fab_skin_pos=PreferenceUtils.getAccent(sharedPref);
        a.positiveColor(fab_skin);
        a.neutralColor(fab_skin);
        a.neutralText(R.string.defualt);
        a.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onNeutral(MaterialDialog dialog) {
                super.onNeutral(dialog);
                sharedPref.edit().remove(preference.getKey());
            }
        });
        ColorAdapter adapter = null;
        String[] colors=PreferenceUtils.colors;
        List<String> arrayList = Arrays.asList(colors);
        switch (preference.getKey()) {
            case "skin":
                adapter = new ColorAdapter(getActivity(), arrayList, "skin_color_position",PreferenceUtils.getPrimaryColor(sharedPref));
                break;
            case "fab_skin":
                adapter = new ColorAdapter(getActivity(), arrayList, "fab_skin_color_position",fab_skin_pos);
                break;
            case "icon_skin":
                adapter = new ColorAdapter(getActivity(), arrayList, "icon_skin_color_position",PreferenceUtils.getFolderColor(sharedPref));
                break;
        }
        GridView v=(GridView)getActivity().getLayoutInflater().inflate(R.layout.dialog_grid,null);
        v.setAdapter(adapter);
        a.customView(v,false);
        MaterialDialog x=a.build();
        adapter.updateMatDialog(x);
        x.show();
        return false;
    }

    class ColorAdapter extends ArrayAdapter<String> {

        String pref;
        int p;
        MaterialDialog b;
        public void updateMatDialog(MaterialDialog b){this.b=b;}
        public ColorAdapter(Context context, List<String> arrayList, String pref, int pref1) {
            super(context, R.layout.rowlayout, arrayList);
            this.pref = pref;
            this.p=pref1;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater)getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.dialog_grid_item, parent, false);
            ImageView imageView=(ImageView)rowView.findViewById(R.id.icon);
            if(position==p)imageView.setImageResource((R.drawable.abc_ic_cab_done_holo_dark));
            GradientDrawable gradientDrawable = (GradientDrawable) imageView.getBackground();
            gradientDrawable.setColor(Color.parseColor(getItem(position)));
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    p=position;
                    notifyDataSetChanged();
                    sharedPref.edit().putInt(pref, position).apply();
                    if (b != null) b.dismiss();
                }
            });
            return rowView;
        }
    }
}
