package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;

/** 底部弹出的玻璃面板：菜单 / 确认 / 输入 */
public final class Sheet {

    public interface OnText { void onText(String text); }

    private final FrameLayout overlay;
    private final GlassBox card;
    private final LinearLayout body;
    private final ViewGroup parent;
    private WeakReference<EditText> focusEdit = new WeakReference<>(null);

    public static boolean centerTitle = false;

    private Sheet(ViewGroup parent) {
        this.parent = parent;
        Context c = parent.getContext();

        overlay = new FrameLayout(c);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        overlay.setBackgroundColor(0xA6070A16);
        overlay.setAlpha(0f);
        overlay.setClickable(true);
        overlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dismiss(); }
        });

        card = new GlassBox(c).style(26f, 0.13f, 0.32f, true);
        card.setClickable(true);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        clp.leftMargin = Ui.dpi(10);
        clp.rightMargin = Ui.dpi(10);
        clp.bottomMargin = Ui.dpi(12);
        overlay.addView(card, clp);
        card.setTranslationY(Ui.dp(52));

        body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dpi(18), Ui.dpi(10), Ui.dpi(18), Ui.dpi(18));
        card.addView(body, new FrameLayout.LayoutParams(-1, -2));

        // 顶部小把手
        FrameLayout grab = new FrameLayout(c);
        View bar = new View(c);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(Ui.dp(3));
        g.setColor(0x33FFFFFF);
        bar.setBackground(g);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(Ui.dpi(36), Ui.dpi(4), Gravity.CENTER);
        grab.addView(bar, blp);
        body.addView(grab, new LinearLayout.LayoutParams(-1, Ui.dpi(16)));

        overlay.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int nav = 0, ime = 0;
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    nav = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                    ime = insets.getInsets(WindowInsets.Type.ime()).bottom;
                } else {
                    nav = insets.getSystemWindowInsetBottom();
                }
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) card.getLayoutParams();
                lp.bottomMargin = Math.max(nav, ime) + Ui.dpi(12);
                card.setLayoutParams(lp);
                return insets;
            }
        });
    }

    // ---------------- 构建 ----------------

    public static Sheet menu(ViewGroup parent, String title, String[] items, Runnable[] actions) {
        return menu(parent, title, items, actions, -1);
    }

    /** dangerIndex 指定的那一项用红色文字（如删除） */
    public static Sheet menu(ViewGroup parent, String title, String[] items, Runnable[] actions, int dangerIndex) {
        Sheet s = new Sheet(parent);
        Context c = parent.getContext();
        if (title != null) s.body.addView(s.title(c, title));
        for (int i = 0; i < items.length; i++) {
            if (i > 0) s.body.addView(s.line(c));
            int color = i == dangerIndex ? 0xFFFF8A8A : Ui.TEXT;
            s.body.addView(s.item(c, items[i], color, i == items.length - 1 ? 14 : 0, actions[i]));
        }
        s.show();
        return s;
    }

    public static Sheet confirm(ViewGroup parent, String title, String msg,
                                String okLabel, final boolean danger, final Runnable onOk) {
        Sheet s = new Sheet(parent);
        Context c = parent.getContext();
        s.body.addView(s.title(c, title));
        if (msg != null) s.body.addView(s.message(c, msg));
        LinearLayout row = s.buttons(c);
        row.addView(s.textButton(c, Str.s("cancel"), Ui.TEXT_3, false, new Runnable() {
            @Override public void run() { s.dismiss(); }
        }));
        row.addView(s.gap(c, 10));
        row.addView(s.textButton(c, okLabel, danger ? 0xFFFF9E9E : 0xFFC3D0FF, true, new Runnable() {
            @Override public void run() {
                s.dismiss();
                if (onOk != null) onOk.run();
            }
        }));
        s.show();
        return s;
    }

    public static Sheet input(ViewGroup parent, String title, String hint, String initial, final OnText cb) {
        final Sheet s = new Sheet(parent);
        Context c = parent.getContext();
        s.body.addView(s.title(c, title));

        final EditText ed = new EditText(c);
        ed.setText(initial == null ? "" : initial);
        ed.setHint(hint == null ? "" : hint);
        ed.setHintTextColor(0x4DF3F6FF);
        ed.setTextSize(15f);
        ed.setTextColor(Ui.TEXT);
        ed.setSingleLine(true);
        ed.setInputType(InputType.TYPE_CLASS_TEXT);
        ed.setBackground(null);
        ed.setPadding(0, Ui.dpi(10), 0, Ui.dpi(10));
        s.focusEdit = new WeakReference<>(ed);

        FrameLayout field = new FrameLayout(c);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(Ui.dp(14));
        g.setColor(0x14FFFFFF);
        g.setStroke(Ui.dpi(1), 0x26FFFFFF);
        field.setBackground(g);
        field.setPadding(Ui.dpi(14), 0, Ui.dpi(14), 0);
        field.addView(ed, new FrameLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(-1, -2);
        flp.topMargin = Ui.dpi(14);
        s.body.addView(field, flp);

        LinearLayout row = s.buttons(c);
        row.addView(s.textButton(c, Str.s("cancel"), Ui.TEXT_3, false, new Runnable() {
            @Override public void run() { s.dismiss(); }
        }));
        row.addView(s.gap(c, 10));
        row.addView(s.textButton(c, Str.s("ok"), 0xFFC3D0FF, true, new Runnable() {
            @Override public void run() {
                String t = ed.getText().toString().trim();
                s.dismiss();
                if (cb != null) cb.onText(t);
            }
        }));
        s.show();
        return s;
    }

    public static Sheet message(ViewGroup parent, String title, String msg) {
        Sheet s = new Sheet(parent);
        Context c = parent.getContext();
        s.body.addView(s.title(c, title));
        s.body.addView(s.message(c, msg));
        s.show();
        return s;
    }

    // ---------------- 组件 ----------------

    private TextView title(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(16f);
        t.setTypeface(Ui.med());
        t.setTextColor(Ui.TEXT);
        t.setIncludeFontPadding(false);
        t.setGravity(android.view.Gravity.CENTER);
        t.setPadding(Ui.dpi(6), Ui.dpi(6), Ui.dpi(6), Ui.dpi(12));
        return t;
    }

    private TextView message(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(13f);
        t.setTextColor(Ui.TEXT_2);
        t.setLineSpacing(Ui.dp(3), 1f);
        t.setIncludeFontPadding(false);
        t.setPadding(Ui.dpi(6), 0, Ui.dpi(6), Ui.dpi(18));
        return t;
    }

    private View line(Context c) {
        View v = new View(c);
        v.setBackgroundColor(0x12FFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, Math.max(1, Ui.dpi(1)));
        lp.leftMargin = Ui.dpi(6);
        v.setLayoutParams(lp);
        return v;
    }

    private View item(Context c, String text, int color, int bottomGap, final Runnable action) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(15f);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(Ui.dpi(6), Ui.dpi(15), Ui.dpi(6), Ui.dpi(15));
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dismiss();
                if (action != null) v.postDelayed(new Runnable() {
                    @Override public void run() { action.run(); }
                }, 120);
            }
        });
        if (bottomGap > 0) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = Ui.dpi(bottomGap);
            t.setLayoutParams(lp);
        }
        return t;
    }

    private LinearLayout buttons(Context c) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.dpi(8);
        row.setLayoutParams(lp);
        body.addView(row);
        return row;
    }

    private View gap(Context c, int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(Ui.dpi(dp), 1));
        return v;
    }

    private TextView textButton(Context c, String text, int color, boolean filled, final Runnable action) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(13.5f);
        t.setTextColor(filled ? 0xFF141A33 : color);
        t.setTypeface(Ui.med());
        t.setIncludeFontPadding(false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dpi(filled ? 20 : 14), Ui.dpi(9), Ui.dpi(filled ? 20 : 14), Ui.dpi(9));
        if (filled) {
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(Ui.dp(20));
            int c1 = color == 0xFFFF9E9E ? 0xFFFFB4B4 : 0xFFB9CCFF;
            g.setColors(new int[]{c1, color == 0xFFFF9E9E ? 0xFFFF8F8F : 0xFFB99CFF});
            g.setOrientation(GradientDrawable.Orientation.TL_BR);
            t.setBackground(g);
        }
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).withEndAction(new Runnable() {
                    @Override public void run() {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                        if (action != null) action.run();
                    }
                }).start();
            }
        });
        return t;
    }

    // ---------------- 生命周期 ----------------

    private void show() {
        parent.addView(overlay);
        overlay.post(new Runnable() {
            @Override public void run() {
                overlay.animate().alpha(1f).setDuration(200).start();
                card.animate().translationY(0f).setDuration(280).setInterpolator(Ui.EASE).start();
                EditText ed = focusEdit.get();
                if (ed != null) {
                    ed.requestFocus();
                    ed.postDelayed(new Runnable() {
                        @Override public void run() {
                            InputMethodManager imm = (InputMethodManager)
                                    overlay.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) imm.showSoftInput(focusEdit.get(), InputMethodManager.SHOW_IMPLICIT);
                        }
                    }, 320);
                }
            }
        });
    }

    public void dismiss() {
        EditText ed = focusEdit.get();
        if (ed != null) {
            InputMethodManager imm = (InputMethodManager)
                    overlay.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(ed.getWindowToken(), 0);
        }
        card.animate().translationY(Ui.dp(46)).setDuration(150).start();
        overlay.animate().alpha(0f).setDuration(170).withEndAction(new Runnable() {
            @Override public void run() {
                parent.removeView(overlay);
            }
        }).start();
    }

    public static boolean isShowing(ViewGroup layer) {
        return layer != null && layer.getChildCount() > 0;
    }

    public static void dismissTop(ViewGroup layer) {
        if (layer != null && layer.getChildCount() > 0) {
            View v = layer.getChildAt(layer.getChildCount() - 1);
            v.animate().alpha(0f).setDuration(140).withEndAction(new Runnable() {
                @Override public void run() { layer.removeView(v); }
            }).start();
        }
    }
}
