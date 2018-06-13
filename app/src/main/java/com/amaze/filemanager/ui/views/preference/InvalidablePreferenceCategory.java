package com.amaze.filemanager.ui.views.preference;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.support.annotation.ColorInt;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.View;

import com.amaze.filemanager.utils.PreferenceUtils;

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

    public void invalidate(@ColorInt int accentColor) {
        titleColor = PreferenceUtils.getStatusColor(accentColor);
        notifyChanged();
    }
}
