package com.amaze.filemanager.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.amaze.filemanager.R;

/**
 * Created by Arpit on 30-07-2015.
 */
public class SizeDrawable extends View {
    Paint mPaint, mPaint1, mPaint2;
    RectF rectF;
    float startangle = -90, startangle1 = -90, startangle2 = -90,
            angle = 0, angle1 = 0, angle2 = 0;


    public SizeDrawable(Context context) {
        super(context);
    }

    int twenty;

    public SizeDrawable(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        int strokeWidth = dpToPx(40);
        rectF = new RectF(getLeft(), getTop(), getRight(), getBottom());
        //rectF = new RectF(dpToPx(0), dpToPx(0), dpToPx(200), dpToPx(200));
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(getResources().getColor(R.color.accent_indigo));
        // mPaint.setStrokeCap(Paint.Cap.BUTT);
        mPaint.setStrokeWidth(strokeWidth);
        mPaint1 = new Paint();
        mPaint1.setAntiAlias(true);
        mPaint1.setStyle(Paint.Style.FILL);
        mPaint1.setColor(getResources().getColor(R.color.accent_red));
        //  mPaint1.setStrokeCap(Paint.Cap.BUTT);
        mPaint1.setStrokeWidth(strokeWidth);
        mPaint2 = new Paint();
        mPaint2.setAntiAlias(true);
        mPaint2.setStyle(Paint.Style.FILL);
        mPaint2.setColor(getResources().getColor(R.color.accent_green));
        // mPaint2.setStrokeCap(Paint.Cap.BUTT);
        mPaint2.setStrokeWidth(strokeWidth);
        twenty = dpToPx(10);
    }


    DisplayMetrics displayMetrics;

    public int dpToPx(int dp) {
        if (displayMetrics == null) displayMetrics = getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // canvas.drawLine((getWidth() - twenty)-2,0,(getWidth() - twenty),0,mPaint1);
        if (angle2 != 0)
            canvas.drawLine(0, getHeight() - (getHeight() * angle1), 0, getHeight() - (getHeight() * angle2), mPaint2);
        canvas.drawLine(0, getHeight(), 0, getHeight() - (getHeight() * angle), mPaint);
        if (angle1 != 0)
            canvas.drawLine(0, getHeight() - (getHeight() * angle), 0, getHeight() - (getHeight() * angle1), mPaint1);
       /* Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setTextSize(20);
        canvas.drawText(Math.round(angle * 100)+"%",(getWidth() - twenty)*angle/2, 25,paint);
        if(angle1>0.20)canvas.drawText(Math.round((angle1-angle)*100)+"%",(getWidth() - twenty)*angle+(getWidth() - twenty)*(angle1-angle)/2, 25,paint);
        if(angle2>0.20)canvas.drawText(Math.round((angle2-angle1)*100)+"%",(getWidth() - twenty)*angle1+(getWidth() - twenty)*(angle2-angle1)/2, 25,paint);


        canvas.drawArc(rectF, startangle, angle, true, mPaint);
        canvas.drawArc(rectF, startangle1, angle1, true, mPaint1);
        canvas.drawArc(rectF, startangle2, angle2, true, mPaint2);
*/
    }

    public void setAngle(float angle, float startangle) {
        this.angle = angle;
        this.startangle = startangle;
    }

    public void setAngle1(float angle, float startangle1) {
        this.angle1 = angle;
        this.startangle1 = startangle1;
    }

    public void setAngle2(float angle2, float startangle2) {
        this.angle2 = angle2;
        this.startangle2 = startangle2;
    }

}
