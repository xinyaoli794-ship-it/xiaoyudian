package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/** 支持双指缩放与拖动的图片视图；缩放状态下"返回"先还原缩放 */
public class ZoomImageView extends View {

    private Bitmap bmp;
    private float scale = 1f, minScale = 1f;
    private float tx, ty;
    private float baseScale = 1f;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Matrix m = new Matrix();
    private final ScaleGestureDetector sg;
    private float lastX, lastY;
    private boolean dragging;
    public Runnable onZoomOut;

    public ZoomImageView(Context c) {
        super(c);
        sg = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector d) {
                float s = scale * d.getScaleFactor();
                scale = Math.max(1f, Math.min(5f, s));
                if (scale <= 1.001f) { tx = 0; ty = 0; }
                invalidate();
                return true;
            }
        });
    }

    public void setBitmap(Bitmap b) {
        bmp = b;
        requestLayout();
        invalidate();
    }

    public boolean isZoomed() { return scale > 1.02f; }

    public void resetZoom() {
        scale = 1f;
        tx = 0;
        ty = 0;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (bmp != null) {
            baseScale = Math.min(w / (float) bmp.getWidth(), h / (float) bmp.getHeight());
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        if (bmp == null) return;
        float s = baseScale * scale;
        m.reset();
        m.postScale(s, s);
        m.postTranslate((getWidth() - bmp.getWidth() * s) / 2f + tx,
                (getHeight() - bmp.getHeight() * s) / 2f + ty);
        c.drawBitmap(bmp, m, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        sg.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = e.getX();
                lastY = e.getY();
                dragging = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (scale > 1.02f && !sg.isInProgress()) {
                    tx += e.getX() - lastX;
                    ty += e.getY() - lastY;
                    lastX = e.getX();
                    lastY = e.getY();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                // 缩回原始比例并松手：视为返回，自动退出查看
                if (scale <= 1.02f && onZoomOut != null) {
                    Runnable r = onZoomOut;
                    onZoomOut = null;
                    if (!sg.isInProgress()) r.run();
                }
                return true;
        }
        return true;
    }
}
