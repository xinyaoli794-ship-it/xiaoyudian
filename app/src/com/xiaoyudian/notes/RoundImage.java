package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** 圆角裁切的图片视图（居中裁剪填充） */
public class RoundImage extends View {

    private Bitmap bmp;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint placeholder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF dst = new RectF();
    private final RectF box = new RectF();
    private float radius = 12f;
    private String path;

    public RoundImage(Context c) {
        super(c);
        placeholder.setColor(Ui.alpha(Ui.TEXT, 0.08f));
    }

    public void setRadius(float dp) { radius = Ui.dp(dp); invalidate(); }

    public void setPath(String p) {
        if (p != null && p.equals(path) && bmp != null) return;
        path = p;
        bmp = null;
        invalidate();
        if (p == null) return;
        Bitmap hit = Pics.cached(p, Ui.dpi(120));
        if (hit != null) { bmp = hit; invalidate(); return; }
        final String want = p;
        Pics.load(p, Ui.dpi(120), new Pics.Callback() {
            @Override public void onBitmap(Bitmap b) {
                if (want.equals(path)) { bmp = b; invalidate(); }
            }
        });
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        box.set(0, 0, w, h);
        clip.reset();
        clip.addRoundRect(box, radius, radius, Path.Direction.CW);
        c.save();
        c.clipPath(clip);
        if (bmp != null) {
            float bw = bmp.getWidth(), bh = bmp.getHeight();
            float scale = Math.max(w / bw, h / bh);
            float dw = bw * scale, dh = bh * scale;
            float l = (w - dw) / 2f, t = (h - dh) / 2f;
            dst.set(l, t, l + dw, t + dh);
            c.drawBitmap(bmp, null, dst, paint);
        } else {
            c.drawRect(box, placeholder);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(0x40FFFFFF);
            c.drawCircle(w / 2f, h / 2f, Math.min(w, h) * 0.12f, p);
        }
        c.restore();
    }
}
