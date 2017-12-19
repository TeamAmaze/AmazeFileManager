package com.amaze.filemanager.ui.drawer;

import android.graphics.drawable.Drawable;

public class EntryItem implements Item {

	public final String title;
	public final String path;
	public final Drawable icon;

	public EntryItem(String title, String path, Drawable icon) {
		this.title = title;
		this.path = path;
		this.icon = icon;
	}

	@Override
	public boolean isSection() {
		return false;
	}
}
