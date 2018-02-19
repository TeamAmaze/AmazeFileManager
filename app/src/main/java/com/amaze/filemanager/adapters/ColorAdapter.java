package com.amaze.filemanager.adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorUsage;

import java.util.List;

public class ColorAdapter extends ArrayAdapter<Integer> implements AdapterView.OnItemClickListener {

    private LayoutInflater inflater;
    private ColorUsage usage;
    private @ColorInt int selectedColor;
    private OnColorSelected onColorSelected;

    /**
     * Constructor for adapter that handles the view creation of color chooser dialog in preferences
     *
     * @param context the context
     * @param colors  array list of color hex values in form of string; for the views
     * @param usage   the preference usage for setting new selected color preference value
     * @param selectedColor currently selected color
     * @param l OnColorSelected listener for when a color is selected
     */
    public ColorAdapter(Context context, List<Integer> colors, ColorUsage usage,
                        @ColorInt int selectedColor, OnColorSelected l) {
        super(context, R.layout.rowlayout, colors);
        this.usage = usage;
        this.selectedColor = selectedColor;
        this.onColorSelected = l;

        inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @ColorRes
    private int getColorResAt(int position) {
        Integer item = getItem(position);

        if (item == null) {
            return usage.getDefaultColor();
        } else {
            return item;
        }
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        View rowView;
        if(convertView != null) {
            rowView = convertView;
        } else {
            rowView = inflater.inflate(R.layout.dialog_grid_item, parent, false);
        }

        @ColorInt int color = Utils.getColor(getContext(), getColorResAt(position));

        ImageView imageView = rowView.findViewById(R.id.icon);
        if (color == selectedColor) {
            imageView.setImageResource(R.drawable.ic_checkmark_selected);
        }
        ((GradientDrawable) imageView.getBackground()).setColor(color);

        return rowView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onColorSelected.onColorSelected(getColorResAt(position));
    }

    public interface OnColorSelected {
        void onColorSelected(int color);
    }
}