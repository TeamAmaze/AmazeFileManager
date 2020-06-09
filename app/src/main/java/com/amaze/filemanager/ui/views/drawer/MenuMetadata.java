package com.amaze.filemanager.ui.views.drawer;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 28/12/2017, at 18:01.
 */

public final class MenuMetadata {

    public static final int ITEM_ENTRY = 1, ITEM_INTENT = 2;

    public final int type;
    public final String path;
    public final OnClickListener onClickListener;

    public MenuMetadata(String path) {
        this.type = ITEM_ENTRY;
        this.path = path;
        this.onClickListener = null;
    }

    public MenuMetadata(OnClickListener onClickListener) {
        this.type = ITEM_INTENT;
        this.onClickListener = onClickListener;
        this.path = null;
    }

    public interface OnClickListener {
        void onClick();
    }

}
