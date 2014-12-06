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

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.services.DeleteTask;
import com.amaze.filemanager.services.asynctasks.ZipHelperTask;
import com.amaze.filemanager.utils.Futils;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

public class ZipViewer extends ListFragment {

    String s;
    public File f;
    public ArrayList<File> files;
    public Boolean results;
    public String current;
    public Futils utils=new Futils();
    public String skin,year;
    public boolean coloriseIcons,showSize,showLastModified;
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        s = getArguments().getString("path");
        f = new File(s);
SharedPreferences Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        coloriseIcons=Sp.getBoolean("coloriseIcons",false);
        Calendar calendar = Calendar.getInstance();
        showSize=Sp.getBoolean("showFileSize",false);
        showLastModified=Sp.getBoolean("showLastModified",true);
        year=(""+calendar.get(Calendar.YEAR)).substring(2,4);
        skin = Sp.getString("skin_color", "#5677fc");
        ((TextView) getActivity().findViewById(R.id.title)).setText(f.getName());
        getListView().setDividerHeight(0);
        FloatingActionButton floatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        floatingActionButton.hide(true);

        getActivity().findViewById(R.id.action_overflow).setVisibility(View.GONE);
        getActivity().findViewById(R.id.search).setVisibility(View.INVISIBLE);
        getActivity().findViewById(R.id.paste).setVisibility(View.INVISIBLE);
        getActivity().findViewById(R.id.title).setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.pathbar).setOnClickListener(null);
        ((TextView)getActivity().findViewById(R.id.pathname)).setText("");
        getActivity().findViewById(R.id.fullpath).setOnClickListener(null);
        new ZipHelperTask(this, 0).execute(f);
        files = new ArrayList<File>();
        results = false;
    }

    public ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        private void hideOption(int id, Menu menu) {
            MenuItem item = menu.findItem(id);
            item.setVisible(false);
        }

        private void showOption(int id, Menu menu) {
            MenuItem item = menu.findItem(id);
            item.setVisible(true);
        }
        View v;
        // called when the action mode is created; startActionMode() was called
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            v=getActivity().getLayoutInflater().inflate(R.layout.actionmode,null);
            mode.setCustomView(v);
            // assumes that you have "contexual.xml" menu resources
            inflater.inflate(R.menu.contextual, menu);
            hideOption(R.id.cpy, menu);
            hideOption(R.id.cut,menu);
            hideOption(R.id.delete,menu);
            hideOption(R.id.addshortcut,menu);
            hideOption(R.id.sethome, menu);
            hideOption(R.id.rename, menu);
            hideOption(R.id.share, menu);
            hideOption(R.id.about, menu);
            hideOption(R.id.openwith, menu);
            hideOption(R.id.ex, menu);
            hideOption(R.id.book, menu);
            hideOption(R.id.compress, menu);
            hideOption(R.id.permissions, menu);
            hideOption(R.id.hide, menu);
            //hideOption(R.id.setringtone,menu);
            mode.setTitle(utils.getString(getActivity(), R.string.select));
            if(Build.VERSION.SDK_INT<19)
                getActivity().findViewById(R.id.action_bar).setVisibility(View.GONE);
            return true;
        }

        // the following method is called each time
        // the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

            return false; // Return false if nothing is done
        }

        // called when the user selects a contextual menu item
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.ex:
                    mode.finish();
                    return true;}return false;}

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {

        }
    };
    @Override
    public void onResume() {
        super.onResume();

        if (files.size()==1) {

            new DeleteTask(getActivity().getContentResolver(), null, getActivity()).execute(files);
        }
    }

    public void goBack() {

        new ZipHelperTask(this, 2, new File(current).getParent()).execute(f);
    }
}
