package com.xiaoyudian.notes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * 统一的表面绘制：实色圆角 + 发丝描边。
 * 无渐变色板、无位图采样、无每帧对象分配 —— 滚动时几乎没有额外开销。
 */
public final class Glass {

    private static final Paint FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        STROKE.setStyle(Paint.Style.STROKE);
        STROKE.setStrokeJoin(Paint.Join.ROUND);
    }

    private Glass() {}

    /** 兼容旧签名的表面绘制（veil 参数已不再使用） */
    public static void panel(Canvas c, RectF r, float radius, View v,
                             float veil, float border, boolean shadow) {
        fill(c, r, radius, Ui.CARD, border);
    }

    public static void panel(Canvas c, RectF r, float radius, float vx, float vy,
                             float veil, float border, boolean shadow) {
        fill(c, r, radius, Ui.CARD, border);
    }

    public static void fill(Canvas c, RectF r, float radius, int color, float borderAlpha) {
        FILL.setColor(color);
        c.drawRoundRect(r, radius, radius, FILL);
        stroke(c, r, radius, borderAlpha);
    }

    public static void stroke(Canvas c, RectF r, float radius, float borderAlpha) {
        if (borderAlpha <= 0.01f) return;
        STROKE.setColor(Ui.alpha(Ui.LINE, Math.min(1f, borderAlpha)));
        STROKE.setStrokeWidth(Math.max(1f, Ui.dp(0.75f)));
        c.drawRoundRect(r, radius, radius, STROKE);
    }

    /** 页面上没有任何圆角的全屏底色 */
    public static void page(Canvas c, int w, int h) {
        FILL.setColor(Ui.BG);
        c.drawRect(0, 0, w, h, FILL);
    }

    public static void panelPath(Canvas c, Path path, View v,
                                 float veil, float border, boolean shadow) {
        FILL.setColor(Ui.CARD);
        c.drawPath(path, FILL);
        if (border > 0.01f) {
            STROKE.setColor(Ui.alpha(Ui.LINE, Math.min(1f, border)));
            STROKE.setStrokeWidth(Math.max(1f, Ui.dp(0.75f)));
            c.drawPath(path, STROKE);
        }
    }
}
