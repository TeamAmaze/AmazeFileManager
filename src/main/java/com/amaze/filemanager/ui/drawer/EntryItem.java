package com.amaze.filemanager.ui.drawer;

import android.graphics.drawable.Drawable;

public class EntryItem implements Item {

	private final String title;
	private final String subtitle;
	private Drawable icon1;

	public EntryItem(String title, String path, Drawable icon1) {
		this.title = title;
		this.subtitle = path;
		this.icon1 = icon1;
	}

	@Override
	public boolean isSection() {
		return false;
	}

	public Drawable getIcon() {
		return icon1;
	}

	public String getPath() {
		return subtitle;
	}

	public String getTitle() {
		return title;
	}
}
