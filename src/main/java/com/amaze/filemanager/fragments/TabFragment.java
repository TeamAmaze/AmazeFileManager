package com.amaze.filemanager.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.TabSpinnerAdapter;
import com.amaze.filemanager.database.Tab;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.utils.CustomViewPager;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.PreferenceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Arpit on 15-12-2014.
 */
public class TabFragment extends android.support.v4.app.Fragment {

    public  List<Fragment> fragments = new ArrayList<Fragment>();
    public ScreenSlidePagerAdapter mSectionsPagerAdapter;
    Futils utils = new Futils();
    public CustomViewPager mViewPager;
    SharedPreferences Sp;
    String path;
    public int currenttab;
    MainActivity mainActivity;
    TabSpinnerAdapter tabSpinnerAdapter;
    public ArrayList<String> tabs=new ArrayList<String>();
    public int theme1;
    private Animation hideAnimation, showAnimation;
    View buttons;
    View mToolBarContainer;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tabfragment,
                container, false);
        mToolBarContainer=getActivity().findViewById(R.id.lin);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int theme=Integer.parseInt(Sp.getString("theme","0"));
        theme1 = theme==2 ? PreferenceUtils.hourOfDay() : theme;
        mViewPager = (CustomViewPager) rootView.findViewById(R.id.pager);
        if (getArguments() != null){
            path = getArguments().getString("path");
        }
        buttons=getActivity().findViewById(R.id.buttons);
        mainActivity = ((MainActivity)getActivity());
        mainActivity.supportInvalidateOptionsMenu();
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            public void onPageScrolled(int p1, float p2, int p3) {

            }

            public void onPageSelected(int p1) {
               mToolBarContainer.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();

                currenttab=p1;
                mainActivity.supportInvalidateOptionsMenu();
                try {
                    updateSpinner();
                } catch (Exception e) {
                   // e.printStackTrace();
                }
                String name=fragments.get(p1).getClass().getName();
                if(name.contains("Main")){
                    Main ma = ((Main) fragments.get(p1));
                    tabHandler = new TabHandler(getActivity(), null, null, 1);
                    if (ma.current != null) {
                        try {
                            mainActivity.updateDrawer(ma.current);
                            mainActivity.updatePath(ma.current,true);
                        if(buttons.getVisibility()==View.VISIBLE){
                            mainActivity.bbar(ma);
                        }
                        } catch (Exception e) {
                            //       e.printStackTrace();5
                        }
                    }
                }

            }

            public void onPageScrollStateChanged(int p1) {
                // TODO: Implement this method
            }
        });
        if (savedInstanceState == null) {
            int l=Sp.getInt("currenttab",1);
            mSectionsPagerAdapter = new ScreenSlidePagerAdapter(
                    getActivity().getSupportFragmentManager());
            TabHandler tabHandler=new TabHandler(getActivity(),null,null,1);
            List<Tab> tabs1=tabHandler.getAllTabs();
            int i=tabs1.size();
            if(i==0) {
                addTab(new Tab(1,"","/","/"),1,"");
                addTab(new Tab(2,"",mainActivity.list.get(0),mainActivity.list.get(0)),2,"");
            }
            else{
                if(path!=null && path.length()!=0){
                    Tab tab=tabHandler.findTab(l+1);
                    tab.setPath(path);
                    addTab(tab,l+1,"");
                    int k;
                    if(l==0)k=2;
                    else k=1;
                    addTab(tabHandler.findTab(k),k,"");
                }
                else
                {   addTab(tabHandler.findTab(1),1,"");
                 addTab(tabHandler.findTab(2),2,"");
            }}



            mViewPager.setAdapter(mSectionsPagerAdapter);

                try {
                    mViewPager.setCurrentItem(l,true);
                } catch (Exception e) {
                    e.printStackTrace();
                }

        } else {
            fragments.clear();
            tabs= savedInstanceState.getStringArrayList("tabs");
            for(int i=0;i<tabs.size();i++){
                try {
                    fragments.add(i, getActivity().getSupportFragmentManager().getFragment(savedInstanceState, "tab"+i));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mSectionsPagerAdapter = new ScreenSlidePagerAdapter(
                    getActivity().getSupportFragmentManager());

            mViewPager.setAdapter(mSectionsPagerAdapter);
            int pos1=savedInstanceState.getInt("pos",0);
            mViewPager.setCurrentItem(pos1);
            mSectionsPagerAdapter.notifyDataSetChanged();

        }
        Main main = ((Main) fragments.get(currenttab));
        main.showButtonOnStart=true;

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }
    @Override
    public void onDestroyView(){
        Sp.edit().putInt("currenttab",currenttab).apply();
        super.onDestroyView();
        try {
            tabHandler.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    TabHandler tabHandler;

    public void updatepaths() {

        tabHandler = new TabHandler(getActivity(), null, null, 1);
        int i=1;
        ArrayList<String> items=new ArrayList<String>();

        // Getting old path from database before clearing

        tabHandler.clear();
        for(Fragment fragment:fragments) {
            if(fragment.getClass().getName().contains("Main")){
                Main m=(Main)fragment;
                items.add(m.current);
                tabHandler.addTab(new Tab(i,m.current,m.current,m.home));
                i++;
            }
        }
        try {
            tabSpinnerAdapter=new TabSpinnerAdapter(mainActivity.getSupportActionBar().getThemedContext(), R.layout.rowlayout,items,mainActivity.tabsSpinner,this);
            mainActivity.tabsSpinner.setAdapter(tabSpinnerAdapter);
            mainActivity.tabsSpinner.setSelection(mViewPager.getCurrentItem());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mainActivity.updatePath(tabHandler.findTab(currenttab+1).getPath(),true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            int i = 0;
            if (fragments != null && fragments.size() !=0) {
                for (Fragment fragment : fragments) {
                    getActivity().getSupportFragmentManager().putFragment(outState, "tab" + i, fragment);
                    i++;
                }
                outState.putStringArrayList("tabs", tabs);
                outState.putInt("pos", mViewPager.getCurrentItem());
            }
            Sp.edit().putInt("currenttab",currenttab).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        @Override
        public int getItemPosition (Object object)
        {int index = fragments.indexOf ((Fragment)object);
            if (index == -1)
                return POSITION_NONE;
            else
                return index;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            try {
                String name=fragments.get(position).getClass().getName();
                if(name.contains("Main")){
                Main ma = ((Main) fragments.get(position));
                if (ma.results) {
                    return utils.getString(getActivity(), R.string.searchresults);
                } else {
                    if (ma.current.equals("/")) {
                        return "Root";
                    } else {
                        return new File(ma.current).getName();
                    }
                }}else if(name.contains("ZipViewer")) {
                    ZipViewer ma = ((ZipViewer) fragments.get(position));

                    try {
                        return ma.f.getName();
                    } catch (Exception e) {
                        return "ZipViewer";
                      //  e.printStackTrace();
                    }

                }
                return fragments.get(position).getClass().getName();
            } catch (Exception e) {
                e.printStackTrace();
                return "";
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

    public void addTab(Tab text,int pos,String path) {
        android.support.v4.app.Fragment main = new Main();
        Bundle b = new Bundle();
        if (path != null && path.trim().length() != 0) {
            b.putString("path", path);

        }
        b.putString("lastpath",text.getPath());
        b.putString("home", text.getHome());
        b.putInt("no", pos);
        main.setArguments(b);
        fragments.add(main);
        tabs.add(main.getClass().getName());
        mSectionsPagerAdapter.notifyDataSetChanged();
        mViewPager.setOffscreenPageLimit(4);
    }
    public Fragment getTab() {
        return fragments.get(mViewPager.getCurrentItem());
    }
    public Fragment getTab1() {
        Fragment man = ( fragments.get(mViewPager.getCurrentItem()));
        return man;
    }
    public void updateSpinner(){

        ArrayList<String> items=new ArrayList<String>();
        items.add(((Main)fragments.get(0)).current);
        items.add(((Main)fragments.get(1)).current);
        tabSpinnerAdapter=new TabSpinnerAdapter(mainActivity.getSupportActionBar().getThemedContext(), R.layout.rowlayout,items,mainActivity.tabsSpinner,this);
        mainActivity.tabsSpinner.setAdapter(tabSpinnerAdapter);
        mainActivity.tabsSpinner.setSelection(mViewPager.getCurrentItem());
    }
}
