package com.amaze.filemanager.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.utils.theme.AppTheme;

/**
 * Created by vishal on 18/2/17.
 *
 * A custom image view which adds an extra attribute to determine a source image when in material
 * dark preference
 */

public class ThemedImageView extends ImageView {

    public ThemedImageView(Context context) {
        this(context, null, 0);
    }

    public ThemedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThemedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Load attributes
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.ImageView, defStyleAttr, 0);

        // dark preference found
        if (((MainActivity) context).getAppTheme().equals(AppTheme.DARK))
            setImageDrawable(array.getDrawable(R.styleable.ImageView_src_dark));

        array.recycle();
    }
}