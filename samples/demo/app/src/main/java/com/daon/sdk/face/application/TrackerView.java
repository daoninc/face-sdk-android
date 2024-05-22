package com.daon.sdk.face.application;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/**
 * Simple face tracker view
 */
public class TrackerView extends SurfaceView {

    private final Paint paint;

    private float imageWidth = 640.0f;
    private float imageHeight = 480.0f;

    private int x;
    private int y;
    private int width;
    private int height;

    private final SurfaceHolder holder;


    public TrackerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setZOrderOnTop(true);

        holder = getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.LTGRAY);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8.0f);
    }

    public TrackerView(Context context) {
        this(context, null);
    }

    public void setArea(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setPosition(Rect face) {

        if (!holder.getSurface().isValid())
            return;

        final Canvas canvas = holder.lockCanvas();

        if (canvas == null)
            return;

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.drawColor(Color.TRANSPARENT);

        if (!face.isEmpty()) {

            RectF rect = new RectF();

            if (width == 0 || height == 0) {
                width = canvas.getWidth();
                height = canvas.getHeight();
            }

            if (height > width) {
                // Canvas is in portrait orientation height > width
                // The frame image size is in landscape
                float yScale = height / imageWidth;
                float xScale = width / imageHeight;

                rect.left = x + width - face.right * xScale;
                rect.top = y + face.top * yScale;
                rect.right = rect.left + face.width() * xScale;
                rect.bottom = rect.top + face.height() * yScale;


            } else {
                // TBD

            }

            canvas.drawRect(rect, paint);
        }

        holder.unlockCanvasAndPost(canvas);
    }

    public void setImageSize(float width, float height) {
        imageWidth = width;
        imageHeight = height;
    }

}

