package com.amaze.filemanager.ui.colors;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;

import com.amaze.filemanager.R;

public final class UserColorPreferences implements Parcelable {

    public final @ColorInt int primaryFirstTab, primarySecondTab, accent, iconSkin;

    public UserColorPreferences(@ColorInt int primaryFirstTab, @ColorInt int primarySecondTab,
                                @ColorInt int accent, @ColorInt int iconSkin) {
        this.primaryFirstTab = primaryFirstTab;
        this.primarySecondTab = primarySecondTab;
        this.accent = accent;
        this.iconSkin = iconSkin;
    }

    private UserColorPreferences(Parcel in) {
        primaryFirstTab = in.readInt();
        primarySecondTab = in.readInt();
        accent = in.readInt();
        iconSkin = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(primaryFirstTab);
        dest.writeInt(primarySecondTab);
        dest.writeInt(accent);
        dest.writeInt(iconSkin);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserColorPreferences> CREATOR = new Creator<UserColorPreferences>() {
        @Override
        public UserColorPreferences createFromParcel(Parcel in) {
            return new UserColorPreferences(in);
        }

        @Override
        public UserColorPreferences[] newArray(int size) {
            return new UserColorPreferences[size];
        }
    };
}