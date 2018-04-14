package com.amaze.filemanager.ui.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.TextView;

public class MarqueeThemedTextView extends ThemedTextView {

    private boolean isSoftSelection = true;

    public MarqueeThemedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setSelected(boolean selected) {
        if (!isSoftSelection) super.setSelected(selected);
        else return;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (!isSoftSelection) super.onSelectionChanged(selStart, selEnd);
        else return;
    }

    public void setSoftSelection(boolean softSelection) {
        this.isSoftSelection = softSelection;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (!isSoftSelection) super.onFocusChanged(focused, direction, previouslyFocusedRect);
        else return;
    }
}
