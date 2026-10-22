package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

/** 便签内的滚动条：平时隐藏，滑动时淡入，可拖拽 */
public class ScrollBar extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF thumb = new RectF();
    private EditText target;
    private boolean dragging;
    private float downY;
    private float downTop;

    private final Runnable fadeOut = new Runnable() {
        @Override public void run() {
            if (!dragging) animate().alpha(0f).setDuration(240).start();
        }
    };

    public ScrollBar(Context c) {
        super(c);
        setAlpha(0f);
        paint.setColor(Ui.alpha(Ui.TEXT, 0.28f));
    }

    public void attach(final EditText e) {
        target = e;
        e.getViewTreeObserver().addOnScrollChangedListener(new android.view.ViewTreeObserver.OnScrollChangedListener() {
            @Override public void onScrollChanged() {
                refresh();
                if (!dragging) {
                    animate().cancel();
                    animate().alpha(1f).setDuration(120).start();
                    removeCallbacks(fadeOut);
                    postDelayed(fadeOut, 900);
                }
            }
        });
        refresh();
    }

    private void refresh() {
        if (target == null) return;
        int contentH = 0;
        if (target.getLayout() != null) contentH = target.getLayout().getHeight();
        int viewH = target.getHeight() - target.getPaddingTop() - target.getPaddingBottom();
        if (contentH <= viewH || viewH <= 0) {
            setAlpha(0f);
            return;
        }
        float trackH = getHeight();
        float minH = Ui.dp(46);
        float h = Math.max(minH, trackH * viewH / (float) contentH);
        float maxScroll = contentH - viewH;
        float p = maxScroll <= 0 ? 0 : target.getScrollY() / maxScroll;
        if (p < 0) p = 0;
        if (p > 1) p = 1;
        thumb.set(0, p * (trackH - h), getWidth(), p * (trackH - h) + h);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (thumb.height() <= 0) return;
        paint.setColor(Ui.alpha(Ui.TEXT, 0.26f));
        float r = Math.min(getWidth(), thumb.height()) / 2f;
        c.drawRoundRect(thumb, r, r, paint);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (getAlpha() < 0.2f && e.getActionMasked() == MotionEvent.ACTION_DOWN) return false;
        return super.dispatchTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (target == null) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragging = true;
                downY = e.getY();
                downTop = thumb.top;
                animate().cancel();
                setAlpha(1f);
                removeCallbacks(fadeOut);
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE: {
                float trackH = getHeight();
                float h = thumb.height();
                float span = trackH - h;
                float top = downTop + (e.getY() - downY);
                if (top < 0) top = 0;
                if (top > span) top = span;
                int contentH = target.getLayout() == null ? 0 : target.getLayout().getHeight();
                int viewH = target.getHeight() - target.getPaddingTop() - target.getPaddingBottom();
                int maxScroll = Math.max(0, contentH - viewH);
                float p = span <= 0 ? 0 : top / span;
                target.scrollTo(0, (int) (p * maxScroll));
                refresh();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                removeCallbacks(fadeOut);
                postDelayed(fadeOut, 700);
                return true;
        }
        return false;
    }
}
