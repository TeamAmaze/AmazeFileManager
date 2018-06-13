package com.amaze.filemanager.ui.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.Pair;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.colors.ColorPreferenceHelper;
import com.amaze.filemanager.ui.colors.UserColorPreferences;
import com.amaze.filemanager.ui.views.CircularColorsView;
import com.amaze.filemanager.ui.views.preference.SelectedColorsPreference;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.theme.AppTheme;

/**
 * This is only the dialog, that shows a list of color combinations and
 * a customization and random one.
 *
 * @author Emmanuel
 *         on 11/10/2017, at 12:48.
 */

public class ColorPickerDialog extends SelectedColorsPreference {

    public static final int DEFAULT = 0;
    public static final int NO_DATA = -1;
    public static final int CUSTOM_INDEX = -2;
    public static final int RANDOM_INDEX = -3;

    /**
     * ONLY add new elements to the end of the array
     */
    private static final ColorItemPair[] COLORS = new ColorItemPair[]{
            new ColorItemPair(R.string.defualt,
                    new int[]{R.color.primary_indigo, R.color.primary_indigo, R.color.primary_pink, R.color.accent_pink}),
            new ColorItemPair(R.string.orange,
                    new int[]{R.color.primary_orange, R.color.primary_orange, R.color.primary_deep_orange, R.color.accent_amber}),
            new ColorItemPair(R.string.blue,
                    new int[]{R.color.primary_blue, R.color.primary_blue, R.color.primary_deep_purple, R.color.accent_light_blue}),
            new ColorItemPair(R.string.green,
                    new int[]{R.color.primary_green, R.color.primary_green, R.color.primary_teal_900, R.color.accent_light_green})};

    private SharedPreferences sharedPrefs;
    private ColorPreferenceHelper colorPreferenceHelper;
    private UserColorPreferences colorPref;
    private AppTheme appTheme;
    private OnAcceptedConfig listener;
    private View selectedItem = null;
    private int selectedIndex = -1;

    public ColorPickerDialog(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.dialog_colorpicker);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }

    public void setColorPreference(ColorPreferenceHelper colorPreferenceHelper, UserColorPreferences color, AppTheme theme) {
        this.colorPreferenceHelper = colorPreferenceHelper;
        colorPref = color;
        appTheme = theme;
    }

    public void setListener(OnAcceptedConfig l) {
        listener = l;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    public CharSequence getSummary() {
        return "";//Always empty
    }

    @Override
    public void onBindDialogView(View view) {
        sharedPrefs = getSharedPreferences();
        int accentColor = colorPref.accent;
        if(selectedIndex == NO_DATA) {//if instance was restored the value is already set
            boolean isUsingDefault = sharedPrefs.getInt(PreferencesConstants.PREFERENCE_COLOR_CONFIG, NO_DATA) == NO_DATA
                    && sharedPrefs.getInt(PreferencesConstants.PREFERENCE_SKIN, R.color.primary_indigo) == R.color.primary_indigo
                    && sharedPrefs.getInt(PreferencesConstants.PREFERENCE_SKIN_TWO, R.color.primary_indigo) == R.color.primary_indigo
                    && sharedPrefs.getInt(PreferencesConstants.PREFERENCE_ACCENT, R.color.primary_pink) == R.color.primary_pink
                    && sharedPrefs.getInt(PreferencesConstants.PREFERENCE_ICON_SKIN, R.color.primary_pink) == R.color.primary_pink;

            if(isUsingDefault) {
                sharedPrefs.edit().putInt(PreferencesConstants.PREFERENCE_COLOR_CONFIG, DEFAULT).apply();
            }

            if(sharedPrefs.getBoolean("random_checkbox", false)) {
                sharedPrefs.edit().putInt(PreferencesConstants.PREFERENCE_COLOR_CONFIG, RANDOM_INDEX).apply();
            }
            sharedPrefs.edit().remove("random_checkbox").apply();
            selectedIndex = sharedPrefs.getInt(PreferencesConstants.PREFERENCE_COLOR_CONFIG, CUSTOM_INDEX);
        }

        LinearLayout container = view.findViewById(R.id.container);
        for(int i = 0; i < COLORS.length; i++) {
            View child = inflateItem(container, i, accentColor);

            if(selectedIndex == i) {
                selectedItem = child;
                select(selectedItem, true);
            }

            ((TextView) child.findViewById(R.id.text)).setText(COLORS[i].first);
            CircularColorsView colorsView = child.findViewById(R.id.circularColorsView);
            colorsView.setColors(getColor(i, 0), getColor(i, 1), getColor(i, 2), getColor(i, 3));
            if (appTheme.getMaterialDialogTheme() == Theme.LIGHT) colorsView.setDividerColor(Color.WHITE);
            else colorsView.setDividerColor(Color.BLACK);
            container.addView(child);
        }
        /*CUSTOM*/ {
            View child = inflateItem(container, CUSTOM_INDEX, accentColor);

            if(selectedIndex == CUSTOM_INDEX) {
                selectedItem = child;
                select(selectedItem, true);
            }

            ((TextView) child.findViewById(R.id.text)).setText(R.string.custom);
            child.findViewById(R.id.circularColorsView).setVisibility(View.INVISIBLE);
            container.addView(child);
        }
        /*RANDOM*/ {
            View child = inflateItem(container, RANDOM_INDEX, accentColor);

            if(selectedIndex == RANDOM_INDEX) {
                selectedItem = child;
                select(selectedItem, true);
            }

            ((TextView) child.findViewById(R.id.text)).setText(R.string.random);
            child.findViewById(R.id.circularColorsView).setVisibility(View.INVISIBLE);
            container.addView(child);
        }
        super.onBindDialogView(view);
    }

    private void select(View listChild, boolean checked) {
        RadioButton button = listChild.findViewById(R.id.select);
        button.setChecked(checked);
    }

    private View inflateItem(LinearLayout container, final int index, int accentColor) {
        View.OnClickListener clickListener = v -> {
            if(!v.isSelected()) {
                select(selectedItem, false);
                select(v, true);
                selectedItem = v;
                selectedIndex = index;
            }
        };

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.item_colorpicker, container, false);
        child.setOnClickListener(clickListener);

        RadioButton radio = child.findViewById(R.id.select);
        radio.setOnClickListener(clickListener);
        if(Build.VERSION.SDK_INT >= 21) {
            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{
                            {-android.R.attr.state_enabled}, //disabled
                            {android.R.attr.state_enabled} //enabled
                    }, new int[] {accentColor, accentColor}
            );
            radio.setButtonTintList(colorStateList);
        }
        return child;
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        Resources res = getContext().getResources();
        Window window = getDialog().getWindow();
        int accentColor = colorPref.accent;

        // Button views
        ((TextView) window.findViewById(res.getIdentifier("button1", "id", "android")))
                .setTextColor(accentColor);
        ((TextView) window.findViewById(res.getIdentifier("button2", "id", "android")))
                .setTextColor(accentColor);

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            sharedPrefs.edit().putInt(PreferencesConstants.PREFERENCE_COLOR_CONFIG, selectedIndex).apply();

            if(selectedIndex != CUSTOM_INDEX && selectedIndex != RANDOM_INDEX) {
                colorPreferenceHelper.saveColorPreferences(sharedPrefs,
                        new UserColorPreferences(getColor(selectedIndex, 0),
                        getColor(selectedIndex, 1), getColor(selectedIndex, 2),
                        getColor(selectedIndex, 3)));
            }

            listener.onAcceptedConfig();
        } else {
            selectedIndex = sharedPrefs.getInt(PreferencesConstants.PREFERENCE_COLOR_CONFIG, NO_DATA);
        }
    }

    private int getColor(int i, int pos) {
        return Utils.getColor(getContext(), COLORS[i].second[pos]);
    }

    /**
     * typedef Pair<int, int[]> ColorItemPair
     */
    private static class ColorItemPair extends Pair<Integer, int[]> {

        /**
         * Constructor for a Pair.
         *
         * @param first  the first object in the Pair
         * @param second the second object in the pair
         */
        public ColorItemPair(Integer first, int[] second) {
            super(first, second);
        }
    }

    public interface OnAcceptedConfig {
        void onAcceptedConfig();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final SavedState myState = new SavedState(super.onSaveInstanceState());
        myState.selectedItem = selectedIndex;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        selectedIndex = myState.selectedItem;
        super.onRestoreInstanceState(myState.getSuperState());//onBindDialogView(View view)
        select(selectedItem, true);
    }

    private static class SavedState extends BaseSavedState {
        int selectedItem;

        public SavedState(Parcel source) {
            super(source);
            selectedItem = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(selectedItem);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

}
