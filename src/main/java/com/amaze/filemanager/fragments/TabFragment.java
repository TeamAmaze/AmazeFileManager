package com.amaze.filemanager.fragments;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.Tab;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.ui.drawer.EntryItem;
import com.amaze.filemanager.ui.views.CustomViewPager;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.MainActivityHelper;
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
    public int theme1;
    View buttons;
    View mToolBarContainer;
    boolean savepaths;
    FragmentManager fragmentManager;
    ImageView indicator1,indicator2;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tabfragment,
                container, false);
        fragmentManager=getActivity().getSupportFragmentManager();
        mToolBarContainer=getActivity().findViewById(R.id.lin);
        indicator1=(ImageView)getActivity().findViewById(R.id.tab_indicator1);
        indicator2=(ImageView)getActivity().findViewById(R.id.tab_indicator2);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        savepaths=Sp.getBoolean("savepaths", true);
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
                updateIndicator(p1);
                currenttab=p1;
                Fragment fragment=fragments.get(p1);
                if(fragment!=null) {
                    String name = fragments.get(p1).getClass().getName();
                    if (name!=null && name.contains("Main")) {
                        Main ma = ((Main) fragments.get(p1));
                        if (ma.CURRENT_PATH != null) {
                            try {
                                mainActivity.updateDrawer(ma.CURRENT_PATH);
                                mainActivity.updatePath(ma.CURRENT_PATH,  ma.results,ma.openMode,
                                        ma.folder_count, ma.file_count);
                                if (buttons.getVisibility() == View.VISIBLE) {
                                    mainActivity.bbar(ma);
                                }
                            } catch (Exception e) {
                                //       e.printStackTrace();5
                            }
                        }
                    }
                }
            }

            public void onPageScrollStateChanged(int p1) {
                // TODO: Implement this method
            }
        });

        mSectionsPagerAdapter = new ScreenSlidePagerAdapter(
                getActivity().getSupportFragmentManager());
        if (savedInstanceState == null) {
            int l=Sp.getInt("currenttab",1);
            TabHandler tabHandler=new TabHandler(getActivity(),null,null,1);
            List<Tab> tabs1=tabHandler.getAllTabs();
            int i=tabs1.size();
            if(i==0) {
                if (mainActivity.storage_count>1)
                    addTab(new Tab(1,"",((EntryItem)mainActivity.list.get(1)).getPath(),"/"),1,"");
                else
                addTab(new Tab(1,"","/","/"),1,"");
                if(!mainActivity.list.get(0).isSection()){
                String pa=((EntryItem)mainActivity.list.get(0)).getPath();
                addTab(new Tab(2,"",pa,pa),2,"");}
                else     addTab(new Tab(2,"",((EntryItem)mainActivity.list.get(1)).getPath(),"/"),2,"");
            }
            else{
                if(path!=null && path.length()!=0){
                    if(l==1)
                        addTab(tabHandler.findTab(1),1,"");
                    addTab(tabHandler.findTab(l+1),l+1,path);
                    if(l==0)
                        addTab(tabHandler.findTab(2),2,"");
                }
                else
                {addTab(tabHandler.findTab(1),1,"");
                 addTab(tabHandler.findTab(2),2,"");
            }}



            mViewPager.setAdapter(mSectionsPagerAdapter);

                try {
                    mViewPager.setCurrentItem(l,true);
                    updateIndicator(mViewPager.getCurrentItem());
                } catch (Exception e) {
                    e.printStackTrace();
                }

        } else {
            fragments.clear();
                try {
                    if(fragmentManager==null)fragmentManager=getActivity().getSupportFragmentManager();
                    fragments.add(0,fragmentManager.getFragment(savedInstanceState, "tab"+0));
                    fragments.add(1, fragmentManager.getFragment(savedInstanceState, "tab"+1));
                } catch (Exception e) {
                    e.printStackTrace();
            }
            mSectionsPagerAdapter = new ScreenSlidePagerAdapter(
                    getActivity().getSupportFragmentManager());

            mViewPager.setAdapter(mSectionsPagerAdapter);
            int pos1=savedInstanceState.getInt("pos",0);
            mViewPager.setCurrentItem(pos1);
            mSectionsPagerAdapter.notifyDataSetChanged();

        }

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
            if(tabHandler!=null)
            tabHandler.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    TabHandler tabHandler;

    public void updatepaths(int pos) {
        if(tabHandler==null)
        tabHandler = new TabHandler(getActivity(), null, null, 1);
        int i=1;
        ArrayList<String> items=new ArrayList<String>();

        // Getting old path from database before clearing

        tabHandler.clear();
        for(Fragment fragment:fragments) {
            if(fragment.getClass().getName().contains("Main")){
                Main m=(Main)fragment;
                items.add(parsePathForName(m.CURRENT_PATH,m.openMode));
                if(i-1==currenttab && i==pos){
                    mainActivity.updatePath(m.CURRENT_PATH,m.results,m.openMode,m
                            .folder_count,m.file_count);
                    mainActivity.updateDrawer(m.CURRENT_PATH);
                }
                if(m.openMode==0) {
                    tabHandler.addTab(new Tab(i, m.CURRENT_PATH, m.CURRENT_PATH, m.home));
                }else
                    tabHandler.addTab(new Tab(i, m.home, m.home, m.home));

                i++;
            }
        }
    }
    String parseSmbPath(String a) {
        if (a.contains("@"))
            return "smb://" + a.substring(a.indexOf("@") + 1, a.length());
        else return a;
    }
    String parsePathForName(String path,int openmode){
        Resources resources=getActivity().getResources();
        if("/".equals(path))
            return resources.getString(R.string.rootdirectory);
        else if(openmode==1 && path.startsWith("smb:/"))
            return (new File(parseSmbPath(path)).getName());
        else if("/storage/emulated/0".equals(path))
            return resources.getString(R.string.storage);
        else if(openmode==2)
            return new MainActivityHelper(mainActivity).getIntegralNames(path);
        else
            return new File(path).getName();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            int i = 0;
            Sp.edit().putInt("currenttab",currenttab).commit();
            if (fragments != null && fragments.size() !=0) {
                if(fragmentManager==null)return;
                for (Fragment fragment : fragments) {
                    fragmentManager.putFragment(outState, "tab" + i, fragment);
                    i++;
                }
                outState.putInt("pos", mViewPager.getCurrentItem());
            }
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
        if(text==null)return;
        android.support.v4.app.Fragment main = new Main();
        Bundle b = new Bundle();
        if(path!=null && path.length()!=0)
            b.putString("lastpath",path);
        else
            b.putString("lastpath",text.getOriginalPath(savepaths));
        b.putString("home", text.getHome());
        b.putInt("no", pos);
        main.setArguments(b);
        fragments.add(main);
        mSectionsPagerAdapter.notifyDataSetChanged();
        mViewPager.setOffscreenPageLimit(4);
    }
    public Fragment getTab() {
        if(fragments.size()==2)
        return fragments.get(mViewPager.getCurrentItem());
        else return null;
    }

    void updateIndicator(int index) {
        if (index != 0 && index != 1) return;
        if (indicator1 == null || indicator2 == null) return;
        /*if (!mainActivity.isDrawerLocked){
            if (index == 0) {
                indicator1.setImageResource(R.drawable.square_filled);
                indicator2.setImageResource(R.drawable.square_hollow);
                indicator1.setColorFilter(Color.parseColor(mainActivity.fabskin));
                if(theme1==0)
                indicator2.setColorFilter(Color.GRAY);
                else
                    indicator2.setColorFilter(null);

            } else {
                indicator2.setImageResource(R.drawable.square_filled);
                indicator1.setImageResource(R.drawable.square_hollow);
                indicator2.setColorFilter(Color.parseColor(mainActivity.fabskin));
                if(theme1==0)
                    indicator1.setColorFilter(Color.GRAY);
                else
                    indicator1.setColorFilter(null);
            }

        return;
        }*/

        if (index == 0) {
            indicator1.setImageDrawable(new ColorDrawable(Color.parseColor(mainActivity.fabskin)));
            indicator2.setImageDrawable(new ColorDrawable(Color.GRAY));
            return;
        } else {
            indicator2.setImageDrawable(new ColorDrawable(Color.parseColor(mainActivity.fabskin)));
            indicator1.setImageDrawable(new ColorDrawable(Color.GRAY));
            return;
        }

    }
    public Fragment getTab(int pos) {
        if(fragments.size()==2 && pos<2)
            return fragments.get(pos);
        else return null;
    }
}
