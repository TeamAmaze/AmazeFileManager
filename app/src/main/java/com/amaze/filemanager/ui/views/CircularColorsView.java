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

    private static final int DISTANCE_EDGE = 10;
    private static final int DISTANCE = 10;
    private static final int DIAMETER = 25;
    private static final int RADIUS = DIAMETER/2;
    private static final int SEMICIRCLE_LINE_WIDTH = 0;

    private boolean paintInitialized = false;
    private Paint dividerPaint = new Paint();
    private Paint[] colors = {new Paint(), new Paint(), new Paint(), new Paint()};
    private RectF semicicleRect = new RectF();

    public CircularColorsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

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

        int centerY = canvas.getHeight()/2;
        int[] positionX = {canvas.getWidth() - DISTANCE_EDGE - DIAMETER - DISTANCE - DIAMETER - DISTANCE - RADIUS,
                canvas.getWidth() - DISTANCE_EDGE - DIAMETER - DISTANCE - RADIUS,
                canvas.getWidth() - DISTANCE_EDGE - RADIUS};
        semicicleRect.set(positionX[0]- RADIUS, centerY- RADIUS, positionX[0]+ RADIUS, centerY+ RADIUS);

        canvas.drawArc(semicicleRect, 90, 180, true, colors[0]);
        canvas.drawArc(semicicleRect, 270, 180, true, colors[1]);

        canvas.drawLine(semicicleRect.centerX(), semicicleRect.top, semicicleRect.centerX(),
                semicicleRect.bottom, dividerPaint);

        canvas.drawCircle(positionX[1], centerY, RADIUS, colors[2]);
        canvas.drawCircle(positionX[2], centerY, RADIUS, colors[3]);
    }

}
