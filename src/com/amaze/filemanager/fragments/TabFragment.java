package com.amaze.filemanager.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.Shortcuts;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Arpit on 15-12-2014.
 */
public class TabFragment extends android.support.v4.app.Fragment {
   public  List<Fragment> fragments = new ArrayList<Fragment>();
    public PagerAdapter mSectionsPagerAdapter;
    android.support.v4.view.PagerTitleStrip STRIP;
    Futils utils = new Futils();
    ViewPager mViewPager;
    SharedPreferences Sp;
    TabFragment t = this;
    String path = "",path1="",path0="";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tabfragment,
                container, false);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
       path0= Sp.getString("tab0","");
        path1= Sp.getString("tab1","");
       // Toast.makeText(getActivity(),path0,Toast.LENGTH_LONG).show();
        mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
        STRIP = ((android.support.v4.view.PagerTitleStrip) rootView
                .findViewById(R.id.pager_title_strip));
        if (getArguments() != null)
            path = getArguments().getString("path");
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            public void onPageScrolled(int p1, float p2, int p3) {
                // TODO: Implement this method
            }

            public void onPageSelected(int p1) { // TODO: Implement this								// method
                Main ma = ((Main) fragments.get(p1));
                if (ma.current != null) {
                    ma.bbar(ma.current);
                    ((TextView) STRIP.getChildAt(p1)).setText(ma.current);
                }
            }

            public void onPageScrollStateChanged(int p1) {
                // TODO: Implement this method
            }
        });
        mViewPager.setOffscreenPageLimit(2);
        if (savedInstanceState == null) {
            mSectionsPagerAdapter = new ScreenSlidePagerAdapter(
                    getActivity().getSupportFragmentManager());
            if(path!=null && path.trim().length()!=0)
            {addTab(path);
            addTab(path1);}
            else{addTab(path0);addTab(path1);}
        } else {
            fragments.clear();
            fragments.add(0, getActivity().getSupportFragmentManager().getFragment(savedInstanceState, "tab0"));
            fragments.add(1, getActivity().getSupportFragmentManager().getFragment(savedInstanceState, "tab1"));
            mSectionsPagerAdapter = new ScreenSlidePagerAdapter(
                    getActivity().getSupportFragmentManager());
        }
        mViewPager.setAdapter(mSectionsPagerAdapter);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getActivity().getSupportFragmentManager().putFragment(outState, "tab0", fragments.get(0));
        getActivity().getSupportFragmentManager().putFragment(outState, "tab1", fragments.get(1));
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        @Override
        public CharSequence getPageTitle(int position) {
            Main ma = ((Main) fragments.get(position));
            if (ma.results) {
                return utils.getString(getActivity(), R.string.searchresults);
            } else {
                if (ma.current.equals("/")) {
                    return "Root";
                } else {
                    return new File(ma.current).getName();
                }
            }
        }

        public int getCount() {
            // TODO: Implement this method
            return fragments.size();
        }

        public ScreenSlidePagerAdapter(android.support.v4.app.FragmentManager fm) {
            super(fm);
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            android.support.v4.app.Fragment f;
            f = fragments.get(position);
            return f;
        }

    }

    public void addTab(String text) {
        android.support.v4.app.Fragment main = new Main();
        int p = fragments.size();

        if (text != null && text.trim().length() != 0) {
            Bundle b = new Bundle();
            b.putString("path", text);
            main.setArguments(b);
        }
        fragments.add(main);

        mSectionsPagerAdapter.notifyDataSetChanged();
    }

    public Main getTab() {
        Main man = ((Main) fragments.get(mViewPager.getCurrentItem()));
        return man;
    }
}
