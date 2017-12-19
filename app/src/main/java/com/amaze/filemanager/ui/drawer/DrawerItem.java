package com.amaze.filemanager.ui.drawer;

import android.graphics.drawable.Drawable;

public final class DrawerItem {

	public static final int ITEM_SECTION = 0, ITEM_ENTRY = 1, ITEM_INTENT = 2;

	public final int type;
	public final String title;
	public final String path;
	public final Drawable icon;
	public final OnClickListener onClickListener;

	public DrawerItem(int type) {
	    if(type != ITEM_SECTION) throw new IllegalArgumentException("This constructor is for ITEM_SECTION!");
	    this.type = type;
        this.title = null;
        this.path = null;
        this.icon = null;
        this.onClickListener = null;
	}

    public DrawerItem(String title, String path, Drawable icon) {
        this.type = ITEM_ENTRY;
        this.title = title;
        this.path = path;
        this.icon = icon;
        this.onClickListener = null;
    }

    public DrawerItem(String title, Drawable icon, OnClickListener onClickListener) {
        this.type = ITEM_INTENT;
        this.title = title;
        this.icon = icon;
        this.onClickListener = onClickListener;
        this.path = null;
    }

    public boolean isSection() {
	    return type == ITEM_SECTION;
    }

    public interface OnClickListener {
        void onClick();
    }

}
