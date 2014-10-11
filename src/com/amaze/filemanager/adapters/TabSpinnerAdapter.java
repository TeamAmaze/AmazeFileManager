package com.amaze.filemanager.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.Tab;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.fragments.Main;

import java.util.ArrayList;

/**
 * Created by Vishal on 10/8/2014.
 */
public class TabSpinnerAdapter extends ArrayAdapter<String> {
    ArrayList<String> items;
    Context context;
    FragmentTransaction fragmentTransaction;

    public TabSpinnerAdapter(Context context, int resource, ArrayList<String> items, FragmentTransaction fragmentTransaction) {
        super(context, resource, items);
        this.items = items;
        this.context = context;
        this.fragmentTransaction = fragmentTransaction;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.spinner_layout, parent, false);

        TextView textView = (TextView) row.findViewById(R.id.spinnerText);
        textView.setText(items.get(position));

        return row;
    }

    @Override
    public View getDropDownView(final int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.spinner_dropdown_layout, parent, false);

        final TextView textView = (TextView) row.findViewById(R.id.spinnerText);
        final SharedPreferences sharedPreferences1 = PreferenceManager.getDefaultSharedPreferences(context);
        String skin = sharedPreferences1.getString("skin_color", "#673ab7");
        final int spinner_current = sharedPreferences1.getInt("spinner_selected", 0);
        ImageButton imageButton = (ImageButton) row.findViewById(R.id.spinnerButton);

        textView.setText(items.get(position));
        imageButton.setBackgroundColor(Color.parseColor(skin));

        if (position == spinner_current) {

            textView.setTextColor(Color.parseColor(skin));
            textView.setTypeface(null, Typeface.BOLD);

        }

        textView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if (position == spinner_current) {

                }
                else {

                    TabHandler tabHandler1 = new TabHandler(context, null, null, 1);
                    Tab tab = tabHandler1.findTab(position);
                    String name  = tab.getPath();
                    //Toast.makeText(getActivity(), name, Toast.LENGTH_SHORT).show();
                    sharedPreferences1.edit().putString("current", name).apply();
                    sharedPreferences1.edit().putInt("spinner_selected", position).apply();
                    fragmentTransaction.replace(R.id.content_frame, new Main());
                    fragmentTransaction.commit();

                    MainActivity mainActivity = (MainActivity) getContext();
                    mainActivity.tabsSpinner.invalidate();
                }
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (position > spinner_current) {

                    TabHandler tabHandler = new TabHandler(context, null, null, 1);
                    Tab tab = tabHandler.findTab(position);
                    String label = tab.getLabel();
                    tabHandler.deleteTab(label);
                    items.remove(position);
                } else {
                    Toast.makeText(getContext(), "Not working", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return row;
    }
}
