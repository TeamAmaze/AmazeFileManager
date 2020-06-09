package com.amaze.filemanager.ui.views.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.views.CircularColorsView;

/**
 * This is the external notification that shows some text and a CircularColorsView.
 *
 * @author Emmanuel
 *         on 6/10/2017, at 15:36.
 */

public class SelectedColorsPreference extends DialogPreference {

    private int[] colors = {Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT, Color.TRANSPARENT};
    private int backgroundColor;
    private int visibility = View.VISIBLE;

    public SelectedColorsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        setWidgetLayoutResource(R.layout.selectedcolors_preference);
        return super.onCreateView(parent);
    }

    @SuppressLint("WrongConstant")
    @Override
    protected void onBindView(View view) {
        super.onBindView(view);//Keep this before things that need changing what's on screen

        CircularColorsView colorsView = view.findViewById(R.id.colorsection);
        colorsView.setColors(colors[0], colors[1], colors[2], colors[3]);
        colorsView.setDividerColor(backgroundColor);
        colorsView.setVisibility(visibility);
    }

    public void setColorsVisibility(int visibility) {
        this.visibility = visibility;
        notifyChanged();
    }

    public void setDividerColor(int color) {
        backgroundColor = color;
    }

    public void setColors(int color, int color1, int color2, int color3) {
        colors = new int[]{color, color1, color2, color3};
        notifyChanged();
    }

    public void invalidateColors() {
        notifyChanged();
    }

}
