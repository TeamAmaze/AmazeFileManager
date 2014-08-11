package com.amaze.filemanager.utils;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

public class Layoutelements implements Parcelable {
    public Layoutelements(Parcel im) {
        Bitmap bitmap = (Bitmap) im.readParcelable(getClass().getClassLoader());
        // Convert Bitmap to Drawable:
        imageId = new BitmapDrawable(bitmap);
        title = im.readString();
        desc = im.readString();
    }

    public static final Parcelable.Creator<Layoutelements> CREATOR =
            new Parcelable.Creator<Layoutelements>() {
                public Layoutelements createFromParcel(Parcel in) {
                    return new Layoutelements(in);
                }

                public Layoutelements[] newArray(int size) {
                    return new Layoutelements[size];
                }
            };

    public int describeContents() {
        // TODO: Implement this method
        return 0;
    }

    public void writeToParcel(Parcel p1, int p2) {
        p1.writeString(title);
        p1.writeString(desc);
        p1.writeParcelable(((BitmapDrawable) imageId).getBitmap(), p2);
        // TODO: Implement this method
    }

    private Drawable imageId;
    private String title;
    private String desc;
    private String permissions;
    private String symlink;

    public Layoutelements(Drawable imageId, String title, String desc,String permissions,String symlink) {
        this.imageId = imageId;
        this.title = title;
        this.desc = desc;
        this.permissions=permissions;
        this.symlink=symlink;
    }

    public Drawable getImageId() {
        return imageId;
    }

    public void setImageId(Drawable imageId) {
        this.imageId = imageId;
    }

    public String getDesc() {
        return desc.toString();
    }

    public void setDesc(String desc) {
        this.desc = desc.toString();
    }

    public String getTitle() {
        return title.toString();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPermissions() {
        return permissions;
    }
    public String getSymlink() {
        return symlink;
    }
    public boolean hasSymlink(){if(getSymlink()!=null && getSymlink().length()!=0){return true;}else return false;}
    @Override
    public String toString() {
        return title + "\n" + desc;
    }
}
