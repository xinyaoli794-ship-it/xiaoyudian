package com.xiaoyudian.notes;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * 底部中央的液态玻璃按钮：
 *  - 常态：圆形玻璃 + 雨滴（点击新建）
 *  - 上滑列表：平滑变形为椭圆玻璃 + “回到顶部”
 *  - 下滑列表：自动隐藏
 */
public class DropFab extends FrameLayout {

    public interface Host { void onFabTap(boolean topMode); }

    public static final int MODE_DROP = 0;
    public static final int MODE_TOP = 1;

    private final Host host;
    private final RectF rect = new RectF();
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Ic icon;
    private final TextView label;
    private int mode = MODE_DROP;
    private float radius;
    private float downX, downY;
    private ValueAnimator morph;
    private boolean moved;

    public DropFab(Context c, Host host) {
        super(c);
        this.host = host;
        setWillNotDraw(false);
        borderPaint.setStyle(Paint.Style.STROKE);
        setElevation(Ui.dp(9));
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        setClipToOutline(false);

        icon = new Ic(c, Ic.DROP, Ui.FAB_ICON);
        addView(icon, new FrameLayout.LayoutParams(Ui.dpi(23), Ui.dpi(23), Gravity.CENTER));

        label = new TextView(c);
        label.setText(Str.s("back_to_top"));
        label.setTextSize(12f);
        label.setTypeface(Ui.med());
        label.setTextColor(Ui.FAB_ICON);
        label.setIncludeFontPadding(false);
        label.setGravity(Gravity.CENTER);
        label.setAlpha(0f);
        addView(label, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
    }

    public int mode() { return mode; }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        radius = Math.min(w, h) / 2f;
    }

    private void setSize(int w, int h) {
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp == null) return;
        lp.width = w;
        lp.height = h;
        setLayoutParams(lp);
        radius = Math.min(w, h) / 2f;
        invalidateOutline();
        invalidate();
    }

    public void setMode(final int m, boolean animate) {
        if (mode == m) return;
        mode = m;
        if (morph != null) { morph.cancel(); morph = null; }
        final int toW = Ui.dpi(m == MODE_TOP ? 104 : 60);
        final int toH = Ui.dpi(m == MODE_TOP ? 44 : 60);

        if (!animate || getWidth() <= 0) {
            setSize(toW, toH);
            icon.setAlpha(m == MODE_TOP ? 0f : 1f);
            label.setAlpha(m == MODE_TOP ? 1f : 0f);
            return;
        }

        final int fromW = getWidth(), fromH = getHeight();
        final boolean toTop = m == MODE_TOP;
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(260);
        a.setInterpolator(Ui.SPRING);
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator anim) {
                float t = (Float) anim.getAnimatedValue();
                if (t < 0f) t = 0f;
                setSize((int) (fromW + (toW - fromW) * t), (int) (fromH + (toH - fromH) * t));
                icon.setAlpha(toTop ? Math.max(0f, 1f - t * 2.2f) : Math.min(1f, t * 2.2f));
                label.setAlpha(toTop ? Math.max(0f, (t - 0.45f) / 0.55f) : Math.max(0f, 1f - t * 2.2f));
                icon.setScaleX(1f - 0.2f * t);
                icon.setScaleY(1f - 0.2f * t);
            }
        });
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                morph = null;
                setSize(toW, toH);
                icon.setAlpha(toTop ? 0f : 1f);
                label.setAlpha(toTop ? 1f : 0f);
                icon.setScaleX(1f);
                icon.setScaleY(1f);
            }
        });
        morph = a;
        a.start();
    }

    public void show() {
        if (getVisibility() != VISIBLE) setVisibility(VISIBLE);
        animate().cancel();
        animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(220).setInterpolator(Ui.EASE).start();
    }

    public void hide() {
        animate().cancel();
        animate().alpha(0f).translationY(Ui.dp(26)).scaleX(0.9f).scaleY(0.9f)
                .setDuration(180).setInterpolator(Ui.EASE_IN).start();
    }

    @Override
    protected void dispatchDraw(Canvas c) {
        rect.set(0, 0, getWidth(), getHeight());
        fillPaint.setColor(Ui.FAB_BG);
        c.drawRoundRect(rect, radius, radius, fillPaint);
        sheenPaint.setShader(new LinearGradient(0, 0, 0, getHeight() * 0.7f,
                Ui.DARK ? 0x22FFFFFF : 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(rect, radius, radius, sheenPaint);
        borderPaint.setColor(Ui.alpha(Ui.LINE, 0.9f));
        borderPaint.setStrokeWidth(Math.max(1f, Ui.dp(1f)));
        c.drawRoundRect(rect, radius, radius, borderPaint);
        super.dispatchDraw(c);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (getAlpha() < 0.5f) return false;
        return super.dispatchTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                moved = false;
                animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).start();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.hypot(e.getX() - downX, e.getY() - downY) > Ui.dp(12)) moved = true;
                return true;
            case MotionEvent.ACTION_CANCEL:
                animate().scaleX(1f).scaleY(1f).setDuration(160).start();
                return true;
            case MotionEvent.ACTION_UP:
                animate().scaleX(1f).scaleY(1f).setDuration(200).setInterpolator(Ui.SPRING).start();
                if (!moved) host.onFabTap(mode == MODE_TOP);
                return true;
        }
        return true;
    }
}
