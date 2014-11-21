/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
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

package com.amaze.filemanager.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.BooksAdapter;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.IconUtils;
import com.amaze.filemanager.utils.Shortcuts;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.xml.parsers.ParserConfigurationException;


public class BookmarksManager extends ListFragment {
    Futils utils = new Futils();
    Shortcuts s = new Shortcuts();
    BooksAdapter b;
    SharedPreferences Sp;
    public IconUtils icons;
    ArrayList<File> bx;
ListView vl;int theme,theme1;
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
        setRetainInstance(false);
        Calendar calendar = Calendar.getInstance();
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        theme=Integer.parseInt(Sp.getString("theme","0"));
        theme1 = theme;
        if (theme == 2) {
            if(hour<=6 || hour>=18) {
                theme1 = 1;
            } else
                theme1 = 0;
        }
        getListView().setDivider(null);
        getActivity().findViewById(R.id.buttonbarframe).setVisibility(View.GONE);
        getActivity().findViewById(R.id.action_overflow).setVisibility(View.GONE);
        getActivity().findViewById(R.id.search).setVisibility(View.INVISIBLE);
        getActivity().findViewById(R.id.paste).setVisibility(View.INVISIBLE);
        getActivity().findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final MaterialDialog.Builder ba1 = new MaterialDialog.Builder(getActivity());
                ba1.title(utils.getString(getActivity(), R.string.addbook));
                View v = getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
                final EditText edir = (EditText) v.findViewById(R.id.newname);
                edir.setHint(utils.getString(getActivity(), R.string.enterpath));
                ba1.customView(v);
                if(theme1==1)ba1.theme(Theme.DARK);
                ba1.negativeText(R.string.cancel);
                ba1.positiveText(R.string.create);
                ba1.callback(new MaterialDialog.Callback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        try {
                            File a = new File(edir.getText().toString());
                            if (a.exists()) {
                                s.addS(a);
                                b.items.add(a);
                                b.notifyDataSetChanged();
                                Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.success), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.filenotexists), Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.error), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onNegative(MaterialDialog materialDialog) {

                    }
                });
                ba1.build().show();

            }
        });

        ((LinearLayout) getActivity().findViewById(R.id.buttons))
                .setVisibility(View.GONE);
        icons = new IconUtils(Sp, getActivity());
         vl = getListView();
        vl.setFastScrollEnabled(true);
        if (savedInstanceState == null)
            refresh();
        else {bx=utils.toFileArray(savedInstanceState.getStringArrayList("bx"));
            refresh(bx);
            vl.setSelectionFromTop(savedInstanceState.getInt("index"), savedInstanceState.getInt("top"));
        }

    }

    @Override
    public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        if (vl != null) {
            b.putStringArrayList("bx", utils.toStringArray(bx));
            int index = vl.getFirstVisiblePosition();
            View vi = vl.getChildAt(0);
            int top = (vi == null) ? 0 : vi.getTop();
            b.putInt("index", index);
            b.putInt("top", top);
        }
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.activity_extra, menu);
        hideOption(R.id.item3, menu);
        hideOption(R.id.item4, menu);
        hideOption(R.id.item9, menu);
        hideOption(R.id.item11, menu);
        hideOption(R.id.item10, menu);
    }

    private void hideOption(int id, Menu menu) {
        MenuItem item = menu.findItem(id);
        item.setVisible(false);
    }

    public boolean onOptionsItemSelected(MenuItem item) { // Handleitem
        // selection
        switch (item.getItemId()) {
            default:

                AlertDialog.Builder ba1 = new AlertDialog.Builder(getActivity());
                ba1.setTitle(utils.getString(getActivity(), R.string.addbook));
                View v = getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
                final EditText edir = (EditText) v.findViewById(R.id.newname);
                edir.setHint(utils.getString(getActivity(), R.string.enterpath));
                ba1.setView(v);
                ba1.setNegativeButton(utils.getString(getActivity(), R.string.cancel), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface p1, int p2) {
                        // TODO: Implement this method
                    }
                });
                ba1.setPositiveButton(utils.getString(getActivity(), R.string.create), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface p1, int p2) {
                        try {
                            File a = new File(edir.getText().toString());
                            if (a.exists()) {
                                s.addS(a);
                                b.items.add(a);
                                b.notifyDataSetChanged();
                                Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.success), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.filenotexists), Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.error), Toast.LENGTH_LONG).show();
                        }
                        // TODO: Implement this method
                    }
                });
                ba1.show();

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void refresh() {
        try {
            bx = s.readS();
            b = new BooksAdapter(getActivity(), R.layout.bookmarkrow, bx, this);
            setListAdapter(b);
        } catch (IOException e) {
        } catch (SAXException e) {
        } catch (ParserConfigurationException e) {
        }
    }

    public void refresh(ArrayList<File> f) {
        b = new BooksAdapter(getActivity(), R.layout.bookmarkrow, f, this);
        setListAdapter(b);
    }
}
