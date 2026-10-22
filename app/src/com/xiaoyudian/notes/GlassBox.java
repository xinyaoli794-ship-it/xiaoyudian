package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

/** 一块实色面板（带发丝描边与真实投影） */
public class GlassBox extends FrameLayout {

    private final RectF rect = new RectF();
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float radiusDp = 24f;
    private float borderAlpha = 0.9f;

    public GlassBox(Context c) {
        super(c);
        setWillNotDraw(false);
        border.setStyle(Paint.Style.STROKE);
        setElevation(Ui.dp(12));
        setClipToOutline(false);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), Ui.dp(radiusDp));
            }
        });
    }

    public GlassBox style(float radiusDp, float veil, float border, boolean shadow) {
        this.radiusDp = radiusDp;
        this.borderAlpha = border;
        setElevation(Ui.dp(shadow ? 14 : 0));
        invalidateOutline();
        return this;
    }

    @Override
    protected void dispatchDraw(Canvas c) {
        rect.set(0, 0, getWidth(), getHeight());
        Glass.fill(c, rect, Ui.dp(radiusDp), Ui.CARD, borderAlpha);
        super.dispatchDraw(c);
    }
}