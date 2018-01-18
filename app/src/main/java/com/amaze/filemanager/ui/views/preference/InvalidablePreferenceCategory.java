package com.amaze.filemanager.ui.views.preference;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.View;

import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.color.ColorPreference;
import com.amaze.filemanager.utils.color.ColorUsage;

/**
 * @author Emmanuel
 *         on 15/10/2017, at 20:46.
 */

public class InvalidablePreferenceCategory extends PreferenceCategory {

    private int titleColor;

    public InvalidablePreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        AppCompatTextView title = view.findViewById(android.R.id.title);
        title.setTextColor(titleColor);
    }

    public void invalidate(ColorPreference colorPreference) {
        titleColor = PreferenceUtils.getStatusColor(colorPreference.getColor(ColorUsage.ACCENT));
        notifyChanged();
    }
}
