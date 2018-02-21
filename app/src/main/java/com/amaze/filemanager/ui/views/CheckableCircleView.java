package com.amaze.filemanager.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.amaze.filemanager.R;

/**
 * This is a circle taht can have a check (√) in the middle
 */
public class CheckableCircleView extends View {

    private static final int CHECK_MARGIN = 15;

    private Drawable check;
    private Paint paint = new Paint();
    private boolean checked;

    public CheckableCircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        check = context.getResources().getDrawable(R.drawable.ic_check_white_24dp);
    }

    public void setColor(@ColorInt int color) {
        paint.setColor(color);
        paint.setAntiAlias(true);
        invalidate();
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(getWidth()/2f, getHeight()/2f, getHeight()/2f, paint);

        if(checked) {
            check.setBounds(CHECK_MARGIN, CHECK_MARGIN, getWidth() - CHECK_MARGIN,
                    getHeight() - CHECK_MARGIN);
            check.draw(canvas);
        }
    }
}
