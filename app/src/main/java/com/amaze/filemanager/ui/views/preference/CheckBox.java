package com.amaze.filemanager.ui.views.preference;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

/**
 * Created by Arpit on 10/18/2015 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com>
 */
public class CheckBox extends SwitchPreference {

    public CheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        // Clean listener before invoke SwitchPreference.onBindView
        clearListenerInViewGroup((ViewGroup) view);
        super.onBindView(view);
    }

    /**
     * Clear listener in Switch for specify ViewGroup.
     *
     * @param viewGroup The ViewGroup that will need to clear the listener.
     */
    private void clearListenerInViewGroup(ViewGroup viewGroup) {
        if (null == viewGroup) {
            return;
        }

        int count = viewGroup.getChildCount();
        for (int n = 0; n < count; ++n) {
            View childView = viewGroup.getChildAt(n);
            if (childView instanceof Switch) {
                final Switch switchView = (Switch) childView;
                switchView.setOnCheckedChangeListener(null);
                return;
            } else if (childView instanceof ViewGroup) {
                ViewGroup childGroup = (ViewGroup) childView;
                clearListenerInViewGroup(childGroup);
            }
        }
    }

}
