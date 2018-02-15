package com.amaze.filemanager.utils.menu;

import android.support.design.widget.NavigationView;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 31/12/2017, at 15:42.
 */

public class MenuLongClickHelper {

    public static void setLongClickListeners(Window window, NavigationView navView, OnNavigationItemLongClickListener listener) {
        for (int i = 0; i < navView.getMenu().size(); i++) {
            setLongClickListener(window, navView.getMenu().getItem(i), listener);
        }
    }

    /**
     * Sets long click on menu items.
     * @author https://stackoverflow.com/users/4669617/ceph3us
     * from https://stackoverflow.com/a/37899386/3124150
     */
    public static void setLongClickListener(Window window, MenuItem item, OnNavigationItemLongClickListener listener) {
        //try get its action view
        View actionView = item.getActionView();
        //check if action view is already set?
        if (actionView == null) {
            //get item id  to comparte later in observer listener
            final int itemId = item.getItemId();
            //if not set on top most window an layout changes listener
            window.getDecorView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                                           int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    // try get view by id we have stored few line up
                    View viewById = v.getRootView().findViewById(itemId);
                    //check if we have any result
                    if (viewById != null) {
                        // set our listener
                        viewById.setOnLongClickListener(view -> listener.onNavigationItemLongClick(item));
                        // remove layout observer listener
                        v.removeOnLayoutChangeListener(this);
                    }
                }
            });
        } else {
            // if set we can add our on long click listener
            actionView.setOnLongClickListener(view -> listener.onNavigationItemLongClick(item));
        }
    }

    public interface OnNavigationItemLongClickListener {
        boolean onNavigationItemLongClick(MenuItem item);
    }
}
