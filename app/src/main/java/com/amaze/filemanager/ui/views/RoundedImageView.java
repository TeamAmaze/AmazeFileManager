package com.amaze.filemanager.ui.views;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.Utils;


/**
 * Created by Arpit on 23-01-2015
 *    edited by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 */
public class RoundedImageView extends ImageView {

    private static final float BACKGROUND_CIRCLE_MARGIN_PERCENTUAL = 0.015f;

    private float[] relativeSize = null;
    private Paint background = new Paint();

    public RoundedImageView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);

        background.setColor(Color.TRANSPARENT);
        background.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() == null || getWidth() == 0 || getHeight() == 0) return;

        Drawable drawable = getDrawable();
        int w = getWidth(), h = getHeight();

        if(!isImageAnIcon()) {
            Bitmap b = drawableToBitmap(drawable);
            Bitmap bitmap = b.copy(Bitmap.Config.ARGB_8888, true);

            Bitmap roundBitmap = getRoundedCroppedBitmap(bitmap, w);
            canvas.drawBitmap(roundBitmap, 0, 0, null);
        } else {
            float min = Math.min(w, h);
            float backgroundCircleMargin = min * BACKGROUND_CIRCLE_MARGIN_PERCENTUAL;
            float radius = min/2f - backgroundCircleMargin*2;
            Bitmap bitmap = drawableToBitmapRelative(drawable, radius);

            canvas.drawCircle(w/2, h/2, radius, background);
            canvas.drawBitmap(bitmap, w/2 - bitmap.getWidth()/2, h/2 - bitmap.getHeight()/2, null);
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        background.setColor(color);
    }

    /**
     * Prevents vectors from getting bigger than their intended size. You MUST NOT provide bigger
     * dimensions than the view!
     *
     * The size is relative to the smallest between width and height
     */
    public void setRelativeSize(float width, float height) {
        if(width > 2 || height > 2) throw new UnsupportedOperationException("Can't make image bigger! Accepted values are [2; 0)");
        this.relativeSize = new float[]{width, height};
    }

    private boolean isImageAnIcon() {
        return relativeSize != null;
    }

    public Bitmap getRoundedCroppedBitmap(Bitmap bitmap, int radius) {
        Bitmap finalBitmap;
        if (bitmap.getWidth() != radius || bitmap.getHeight() != radius) {
            finalBitmap = Bitmap.createScaledBitmap(bitmap, radius, radius,
                    false);
        } else {
            finalBitmap = bitmap;
        }

        Bitmap output = Bitmap.createBitmap(finalBitmap.getWidth(),
                finalBitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, finalBitmap.getWidth(),
                finalBitmap.getHeight());

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        paint.setColor(Utils.getColor(getContext(), R.color.roundedimagepaint));
        canvas.drawCircle(finalBitmap.getWidth() / 2 + 0.7f,
                finalBitmap.getHeight() / 2 + 0.7f,
                finalBitmap.getWidth() / 2 + 0.1f, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(finalBitmap, rect, rect, paint);

        return output;
    }

    /**
     * Converts a {@link Drawable} to {@link Bitmap}
     * A drawable can be drawn on a {@link Canvas} and a Canvas can be backed by a Bitmap.
     * Hence the conversion
     */
    public Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap;

        if (!isImageAnIcon() && drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Converts a {@link Drawable} to {@link Bitmap}
     * A drawable can be drawn on a {@link Canvas} and a Canvas can be backed by a Bitmap.
     * Hence the conversion
     */
    public Bitmap drawableToBitmapRelative(Drawable drawable, float radius) {
        Bitmap bitmap;

        if (!isImageAnIcon() && drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        int sizeW = (int) (radius*relativeSize[0]);
        int sizeH = (int) (radius*relativeSize[1]);

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            throw new IllegalStateException("Solid colors cannot be represented as images! Use RoundedImageView.setBackgroundColor()");
        } else {
            bitmap = Bitmap.createBitmap(sizeW, sizeH, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, sizeW, sizeH);
        drawable.draw(canvas);
        return bitmap;
    }
}


