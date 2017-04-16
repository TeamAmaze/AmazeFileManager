package com.amaze.filemanager.ui.views;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

/**
 * Created by Arpit on 10/18/2015.
 */
public class CheckBox extends SwitchPreference {
    public CheckBox(Context context) {
        super(context);
    }

    public CheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        // Clean listener before invoke SwitchPreference.onBindView
        ViewGroup viewGroup= (ViewGroup)view;
        clearListenerInViewGroup(viewGroup);
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
        for(int n = 0; n < count; ++n) {
            View childView = viewGroup.getChildAt(n);
            if(childView instanceof Switch) {
                final Switch switchView = (Switch) childView;
                switchView.setOnCheckedChangeListener(null);
                return;
            } else if (childView instanceof ViewGroup){
                ViewGroup childGroup = (ViewGroup)childView;
                clearListenerInViewGroup(childGroup);
            }
        }
    }

}


