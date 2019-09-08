package com.amaze.filemanager.ui.views;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.util.AttributeSet;

import com.amaze.filemanager.activities.superclasses.BasicActivity;
import com.amaze.filemanager.utils.theme.AppTheme;

/**
 * Created by vishal on 18/2/17.
 *
 * A custom image view which adds an extra attribute to determine a source image when in material
 * dark preference
 */

public class ThemedImageView extends androidx.appcompat.widget.AppCompatImageView {

    public ThemedImageView(Context context) {
        this(context, null, 0);
    }

    public ThemedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThemedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        BasicActivity a = (BasicActivity) getActivity();

        // dark preference found
        if (a != null && (a.getAppTheme().equals(AppTheme.DARK) || a.getAppTheme().equals(AppTheme.BLACK))) {
            setColorFilter(Color.argb(255, 255, 255, 255)); // White Tint
        } else if (a == null) {
            throw new IllegalStateException("Could not get activity! Can't show correct icon color!");
        }

    }

    private Activity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }

}
