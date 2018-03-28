package com.amaze.filemanager.ui.views.drawer;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.util.AttributeSet;
import android.view.MenuItem;

import com.amaze.filemanager.utils.application.AppConfig;

/**
 * This class if for intercepting item selections so that they can be saved and restored.
 */
public class CustomNavigationView extends NavigationView
        implements NavigationView.OnNavigationItemSelectedListener {

    private OnNavigationItemSelectedListener subclassListener;
    private int checkedId = -1;

    public CustomNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);

        super.setNavigationItemSelectedListener(this);
    }



    @Override
    public void setNavigationItemSelectedListener(@Nullable OnNavigationItemSelectedListener listener) {
        subclassListener = listener;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if(subclassListener != null) {
            boolean shouldBeSelected = subclassListener.onNavigationItemSelected(item);

            if(shouldBeSelected) {
                onItemChecked(item);
            }

            return shouldBeSelected;
        } else {
            onItemChecked(item);
            return true;
        }
    }

    private void onItemChecked(MenuItem item) {
        checkedId = item.getItemId();
    }

    public void setCheckedItem(MenuItem item) {
        this.checkedId = item.getItemId();
        item.setChecked(true);
    }

    public void deselectItems() {
        checkedId = -1;
    }

    public @Nullable MenuItem getSelected() {
        if(checkedId == -1) return null;
        return getMenu().findItem(checkedId);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        //begin boilerplate code that allows parent classes to save state
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        //end

        ss.selectedId = this.checkedId;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        //begin boilerplate code so parent classes can restore state
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState)state;
        super.onRestoreInstanceState(ss.getSuperState());
        //end

        this.checkedId = ss.selectedId;
    }

    static class SavedState extends BaseSavedState {
        int selectedId;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.selectedId = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.selectedId);
        }

        //required field that makes Parcelables from a Parcel
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
