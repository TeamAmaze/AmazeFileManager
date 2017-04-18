package com.amaze.filemanager.ui.views.preference;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.amaze.filemanager.R;

/**
 * @author Emmanuel
 *         on 17/4/2017, at 22:22.
 */

public class NamePathSwitchPreference extends Preference {

    public NamePathSwitchPreference(Context context) {
        super(context);
    }

    public NamePathSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NamePathSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected View onCreateView(ViewGroup parent) {
        setWidgetLayoutResource(R.layout.namepathswich_preference);
        return super.onCreateView(parent);
    }

}
