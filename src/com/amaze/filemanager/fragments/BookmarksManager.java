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

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.BooksAdapter;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.IconUtils;
import com.amaze.filemanager.utils.Shortcuts;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class BookmarksManager extends ListFragment {
    Futils utils = new Futils();
    Shortcuts s = new Shortcuts();
    BooksAdapter b;
    SharedPreferences Sp;
    public IconUtils icons;
    ArrayList<File> bx;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
        setRetainInstance(false);
        ((LinearLayout) getActivity().findViewById(R.id.buttons))
                .setVisibility(View.GONE);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        icons = new IconUtils(Sp, getActivity());
        ListView vl = getListView();
//		float scale = getResources().getDisplayMetrics().density;
//		int dpAsPixels = (int) (10*scale + 0.5f);
//	    getListView().setPadding(dpAsPixels,0, dpAsPixels, 0);
//	    getListView().setDivider(null);
//		getListView().setDividerHeight(dpAsPixels);
//		vl.setCacheColorHint(android.R.color.transparent);
//		vl.setSelector(android.R.color.transparent);
//		vl.setHeaderDividersEnabled(true);
//		View divider=getActivity().getLayoutInflater().inflate(R.layout.divider,null);
//		vl.addFooterView(divider);
//		vl.addHeaderView(divider);
//		vl.setFooterDividersEnabled(true);
        vl.setFastScrollEnabled(true);
        if (savedInstanceState == null)
            refresh();
        else {
            refresh(utils.toFileArray(savedInstanceState.getStringArrayList("bx")));
            getListView().setSelectionFromTop(savedInstanceState.getInt("index"), savedInstanceState.getInt("top"));
        }

    }

    @Override
    public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putStringArrayList("bx", utils.toStringArray(bx));
        int index = getListView().getFirstVisiblePosition();
        View vi = getListView().getChildAt(0);
        int top = (vi == null) ? 0 : vi.getTop();
        b.putInt("index", index);
        b.putInt("top", top);
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
        menu.findItem(R.id.item5).setIcon(icons.getNewDrawable());
    }

    private void hideOption(int id, Menu menu) {
        MenuItem item = menu.findItem(id);
        item.setVisible(false);
    }

    public boolean onOptionsItemSelected(MenuItem item) { // Handleitem
        // selection
        switch (item.getItemId()) {
            case R.id.item5:

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
                                Crouton.makeText(getActivity(), utils.getString(getActivity(), R.string.success), Style.CONFIRM).show();
                            } else {
                                Crouton.makeText(getActivity(), utils.getString(getActivity(), R.string.filenotexists), Style.ALERT).show();
                            }
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            Crouton.makeText(getActivity(), utils.getString(getActivity(), R.string.error), Style.ALERT).show();
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
