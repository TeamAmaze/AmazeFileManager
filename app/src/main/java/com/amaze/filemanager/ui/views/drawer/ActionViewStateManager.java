package com.amaze.filemanager.ui.views.drawer;

import android.content.res.ColorStateList;
import android.support.annotation.ColorInt;
import android.support.design.widget.NavigationView;
import android.view.MenuItem;
import android.widget.ImageButton;

/**
 * This manages to set the color of the selected ActionView
 * and unset the ActionView that is not selected anymore
 */
public class ActionViewStateManager {

    private ImageButton lastItemSelected = null;
    private @ColorInt int idleIconColor;
    private @ColorInt int selectedIconColor;

    public ActionViewStateManager(@ColorInt int idleColor, @ColorInt int accentColor) {
        idleIconColor = idleColor;
        selectedIconColor = accentColor;
    }

    public void deselectCurrentActionView() {
        if(lastItemSelected != null) {
            lastItemSelected.setColorFilter(idleIconColor);
            lastItemSelected = null;
        }
    }

    public void selectActionView(MenuItem item) {
        if(lastItemSelected != null) {
            lastItemSelected.setColorFilter(idleIconColor);
        }
        if(item.getActionView() != null) {
            lastItemSelected = (ImageButton) item.getActionView();
            lastItemSelected.setColorFilter(selectedIconColor);
        }
    }

}
