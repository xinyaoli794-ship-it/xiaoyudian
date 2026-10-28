package com.xiaoyudian.notes;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 分段控制器：只有选中项有一块常驻的毛玻璃，滑动时跟随。
 * 滑块直接画在控件自身上（不是独立子 View），因此不受布局抖动/动画竞争影响，
 * 任何时刻打开都必然可见。
 */
public class Segmented extends FrameLayout {

    public interface OnPick { void onPick(int index); }

    private final LinearLayout row;
    private final TextView[] items;
    private final ArgbEvaluator argb = new ArgbEvaluator();
    private final int pad;
    private final FrostDrawable frost;
    private final RectF box = new RectF();
    private int selected;
    private float offset;
    private float itemW = 1f;
    private ValueAnimator slide;

    public Segmented(Context c, String[] labels, int selected, final OnPick cb) {
        super(c);
        this.selected = Math.max(0, Math.min(labels.length - 1, selected));
        this.offset = this.selected;
        pad = Ui.dpi(3);
        setPadding(pad, pad, pad, pad);
        setBackground(null);
        setWillNotDraw(false);
        frost = new FrostDrawable(Ui.dp(9));

        row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        addView(row, new FrameLayout.LayoutParams(-1, -1));

        items = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView t = new TextView(c);
            t.setText(labels[i]);
            t.setTextSize(12.5f);
            t.setGravity(Gravity.CENTER);
            t.setIncludeFontPadding(false);
            t.setTextColor(i == this.selected ? Ui.ACCENT : Ui.TEXT_3);
            t.setTypeface(i == this.selected ? Ui.med() : Ui.reg());
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (idx == Segmented.this.selected) return;
                    setSelected(idx, true);
                    if (cb != null) cb.onPick(idx);
                }
            });
            items[i] = t;
            row.addView(t, new LinearLayout.LayoutParams(0, -1, 1f));
        }
    }

    public int getSelected() { return selected; }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        itemW = Math.max(1f, (w - 2 * pad) / (float) items.length);
        offset = selected;
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas c) {
        float left = pad + offset * itemW;
        box.set(left, pad, left + itemW, getHeight() - pad);
        frost.setBounds((int) box.left, (int) box.top, (int) box.right, (int) box.bottom);
        frost.draw(c);
        super.dispatchDraw(c);
    }

    public void setSelected(int index, boolean animate) {
        if (index < 0 || index >= items.length) return;
        final int old = selected;
        selected = index;
        if (index == old) return;

        for (int i = 0; i < items.length; i++) {
            items[i].setTypeface(i == index ? Ui.med() : Ui.reg());
        }

        if (!animate || getWidth() <= 0) {
            offset = index;
            for (int i = 0; i < items.length; i++) {
                items[i].setTextColor(i == index ? Ui.ACCENT : Ui.TEXT_3);
            }
            invalidate();
            return;
        }

        if (slide != null) slide.cancel();
        final float from = offset;
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(260);
        va.setInterpolator(Ui.EASE);
        final int selColor = Ui.ACCENT;
        final int unselColor = Ui.TEXT_3;
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                float t = (Float) a.getAnimatedValue();
                offset = from + (index - from) * t;
                invalidate();
                for (int i = 0; i < items.length; i++) {
                    boolean was = i == old, now = i == index;
                    if (was == now) continue;
                    int c1 = was ? selColor : unselColor;
                    int c2 = now ? selColor : unselColor;
                    items[i].setTextColor((Integer) argb.evaluate(t, c1, c2));
                }
            }
        });
        slide = va;
        va.start();
    }

    /** 毛玻璃：带主色调的半透明底 + 顶部高光 + 发丝描边 */
    static class FrostDrawable extends Drawable {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final float radius;

        FrostDrawable(float radius) {
            this.radius = radius;
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(Math.max(1f, Ui.dp(0.6f)));
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            sheen.setShader(new LinearGradient(0, bounds.top, 0, bounds.top + bounds.height() * 0.72f,
                    Ui.DARK ? 0x33FFFFFF : 0x8CFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(Canvas c) {
            rect.set(getBounds());
            float rr = radius < 0 ? Math.min(rect.width(), rect.height()) / 2f : radius;
            fill.setColor(Ui.shade(Ui.blend(Ui.DARK ? 0xFF1C1C20 : 0xFFFFFFFF, Ui.ACCENT, Ui.DARK ? 0.26f : 0.16f), 1.06f));
            c.drawRoundRect(rect, rr, rr, fill);
            c.drawRoundRect(rect, rr, rr, sheen);
            border.setColor(Ui.DARK ? 0x33FFFFFF : 0x16000000);
            // 亮边带厚度 + 内侧折射高光
            border.setColor(Ui.DARK ? 0x59FFFFFF : 0x40FFFFFF);
            border.setStrokeWidth(Math.max(1f, Ui.dp(1f)));
            android.graphics.RectF inner = new android.graphics.RectF(rect);
            inner.inset(0.6f, 0.6f);
            c.drawRoundRect(inner, rr, rr, border);
            border.setStrokeWidth(Math.max(1f, Ui.dp(0.6f)));
            border.setColor(Ui.DARK ? 0x33FFFFFF : 0x16000000);
            c.drawRoundRect(rect, rr, rr, border);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(android.graphics.ColorFilter cf) { }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }
}
