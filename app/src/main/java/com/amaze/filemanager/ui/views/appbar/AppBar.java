package com.amaze.filemanager.ui.views.appbar;

import android.content.SharedPreferences;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.Toolbar;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;

import static android.os.Build.VERSION.SDK_INT;

/**
 * layout_appbar.xml contains the layout for AppBar and BottomBar
 *
 * This is a class containing containing methods to each section of the AppBar,
 * creating the object loads the views.
 *
 * @author Emmanuel
 *         on 2/8/2017, at 23:27.
 */

public class AppBar {

    private int TOOLBAR_START_INSET;

    private Toolbar toolbar;
    private SearchView searchView;
    private BottomBar bottomBar;

    private AppBarLayout appbarLayout;

    public AppBar(MainActivity a, SharedPreferences sharedPref, SearchView.SearchListener searchListener) {
        toolbar = a.findViewById(R.id.action_bar);
        searchView = new SearchView(this, a, searchListener);
        bottomBar = new BottomBar(this, a);

        appbarLayout = a.findViewById(R.id.lin);

        if (SDK_INT >= 21) toolbar.setElevation(0);
        /* For SearchView, see onCreateOptionsMenu(Menu menu)*/
        TOOLBAR_START_INSET = toolbar.getContentInsetStart();

        if (!sharedPref.getBoolean(PreferencesConstants.PREFERENCE_INTELLI_HIDE_TOOLBAR, true)) {
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
            params.setScrollFlags(0);
            appbarLayout.setExpanded(true, true);
        }
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    public SearchView getSearchView() {
        return searchView;
    }

    public BottomBar getBottomBar() {
        return bottomBar;
    }

    public AppBarLayout getAppbarLayout() {
        return appbarLayout;
    }

    public void setTitle(String title) {
        if (toolbar != null) toolbar.setTitle(title);
    }

    public void setTitle(@StringRes int title) {
        if (toolbar != null) toolbar.setTitle(title);
    }

}
