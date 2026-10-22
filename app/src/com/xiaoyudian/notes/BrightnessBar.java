package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

/** 自定义主题：用一条横向色带调节主色的明度 */
public class BrightnessBar extends View {

    public interface OnChange { void onColor(int color, float value); }

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knob = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int base;
    private final OnChange cb;
    private float value;

    public BrightnessBar(Context c, int baseColor, float value, OnChange cb) {
        super(c);
        this.base = baseColor;
        this.value = Math.max(0f, Math.min(1f, value));
        this.cb = cb;
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(Math.max(2f, Ui.dp(1.5f)));
        ring.setColor(Ui.alpha(Ui.TEXT, 0.35f));
    }

    /** 轨道颜色 = 该明度下真实的页面底色，所见即所得 */
    private int colorFor(float v) {
        int bg = Ui.blend(Ui.blend(0xFFFFFFFF, 0xFF050507, v), base, (0.10f + 0.26f * v) * 0.75f);
        return bg;
    }

    @Override
    protected void onDraw(Canvas cv) {
        float h = getHeight();
        rect.set(0, h * 0.38f, getWidth(), h * 0.62f);
        float r = rect.height() / 2f;
        track.setShader(new LinearGradient(0, 0, getWidth(), 0,
                colorFor(0f), colorFor(1f), Shader.TileMode.CLAMP));
        cv.drawRoundRect(rect, r, r, track);

        float cx = Math.max(r, Math.min(getWidth() - r, value * getWidth()));
        float cy = h / 2f;
        float kr = Math.min(rect.height() * 1.15f, h * 0.34f);
        knob.setColor(0xFFFFFFFF);
        cv.drawCircle(cx, cy, kr, knob);
        cv.drawCircle(cx, cy, kr, ring);
        knob.setColor(colorFor(value));
        cv.drawCircle(cx, cy, kr * 0.62f, knob);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN
                || e.getActionMasked() == MotionEvent.ACTION_MOVE) {
            value = Math.max(0f, Math.min(1f, e.getX() / Math.max(1, getWidth())));
            invalidate();
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN && getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_UP
                || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            if (cb != null) cb.onColor(base, value);   // 色相不变，只改明度
            return true;
        }
        return super.onTouchEvent(e);
    }
}
