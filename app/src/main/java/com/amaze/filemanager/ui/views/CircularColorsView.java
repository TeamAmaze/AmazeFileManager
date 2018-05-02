package com.amaze.filemanager.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author Emmanuel
 *         on 6/10/2017, at 15:41.
 */

public class CircularColorsView extends View {

    private static final float DISTANCE_PERCENTUAL = 0.08f;
    private static final float DIAMETER_PERCENTUAL = 0.65f;
    private static final int SEMICIRCLE_LINE_WIDTH = 0;

    private boolean paintInitialized = false;
    private Paint dividerPaint = new Paint();
    private Paint[] colors = {new Paint(), new Paint(), new Paint(), new Paint()};
    private RectF semicicleRect = new RectF();

    public CircularColorsView(Context context) {
        super(context);
        init();
    }


    public CircularColorsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dividerPaint.setColor(Color.BLACK);
        dividerPaint.setStyle(Paint.Style.STROKE);
        dividerPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setStrokeWidth(SEMICIRCLE_LINE_WIDTH);
    }

    public void setDividerColor(int color) {
        dividerPaint.setColor(color);
    }

    public void setColors(int color, int color1, int color2, int color3) {
        colors[0].setColor(color);
        colors[1].setColor(color1);
        colors[2].setColor(color2);
        colors[3].setColor(color3);

        for (Paint p : colors) p.setFlags(Paint.ANTI_ALIAS_FLAG);

        paintInitialized = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(isInEditMode()) setColors(Color.CYAN, Color.RED, Color.GREEN, Color.BLUE);
        if(!paintInitialized) throw new IllegalStateException("Paint has not actual color!");

        float distance = getWidth() * DISTANCE_PERCENTUAL;

        float diameterByHeight = getHeight()* DIAMETER_PERCENTUAL;
        float diameterByWidth = (getWidth() - distance*2)/3f* DIAMETER_PERCENTUAL;
        float diameter = Math.min(diameterByHeight, diameterByWidth);

        float radius = diameter/2f;

        int centerY = getHeight()/2;
        float[] positionX = {getWidth()- diameter - distance - diameter - distance - radius,
                getWidth() - diameter - distance - radius,
                getWidth() - radius};
        semicicleRect.set(positionX[0]- radius, centerY- radius, positionX[0]+ radius, centerY+ radius);

        canvas.drawArc(semicicleRect, 90, 180, true, colors[0]);
        canvas.drawArc(semicicleRect, 270, 180, true, colors[1]);

        canvas.drawLine(semicicleRect.centerX(), semicicleRect.top, semicicleRect.centerX(),
                semicicleRect.bottom, dividerPaint);

        canvas.drawCircle(positionX[1], centerY, radius, colors[2]);
        canvas.drawCircle(positionX[2], centerY, radius, colors[3]);
    }

}
