package com.amaze.filemanager.adapters;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.views.CheckableCircleView;
import com.amaze.filemanager.utils.Utils;

import java.util.List;

public class ColorAdapter extends ArrayAdapter<Integer> implements AdapterView.OnItemClickListener {

    private LayoutInflater inflater;
    private @ColorInt int selectedColor;
    private OnColorSelected onColorSelected;

    /**
     * Constructor for adapter that handles the view creation of color chooser dialog in preferences
     *
     * @param context the context
     * @param colors  array list of color hex values in form of string; for the views
     * @param selectedColor currently selected color
     * @param l OnColorSelected listener for when a color is selected
     */
    public ColorAdapter(Context context, Integer[] colors, @ColorInt int selectedColor, OnColorSelected l) {
        super(context, R.layout.rowlayout, colors);
        this.selectedColor = selectedColor;
        this.onColorSelected = l;

        inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @ColorRes
    private int getColorResAt(int position) {
        return getItem(position);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        CheckableCircleView colorView;
        if(convertView != null && convertView instanceof CheckableCircleView) {
            colorView = (CheckableCircleView) convertView;
        } else {
            colorView = (CheckableCircleView) inflater.inflate(R.layout.dialog_grid_item, parent,
                    false);
        }

        @ColorInt int color = Utils.getColor(getContext(), getColorResAt(position));

        colorView.setChecked(color == selectedColor);
        colorView.setColor(color);

        return colorView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        this.selectedColor = Utils.getColor(getContext(), getColorResAt(position));
        ((CheckableCircleView) view).setChecked(true);
        onColorSelected.onColorSelected(this.selectedColor);
    }

    public interface OnColorSelected {
        void onColorSelected(@ColorInt int color);
    }
}