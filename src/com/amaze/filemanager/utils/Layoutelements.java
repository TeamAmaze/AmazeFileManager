package com.amaze.filemanager.utils;
import android.graphics.drawable.*;

public class Layoutelements {
	private Drawable imageId;
	private String title;
	private String desc;
	private boolean selected;
    
	
	public Layoutelements(Drawable imageId, String title, String desc) {
		this .imageId = imageId;
		this .title = title;
		this .desc = desc;
		
		selected = false;
	}
	public Drawable getImageId() {
		return imageId;
	}
	public void setImageId(Drawable imageId) {
		this .imageId = imageId;
	}
	public String getDesc() {
		return desc.toString();
	}
	public void setDesc(String desc) {
		this .desc = desc.toString();
	}

	public String getTitle() {
		return title.toString();
	}
	public void setTitle(String title) {
		this .title = title;
	}public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	@Override
	public String toString() {
		return title + "\n" + desc;
	}
}
