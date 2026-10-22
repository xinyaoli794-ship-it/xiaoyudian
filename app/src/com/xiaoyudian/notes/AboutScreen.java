package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 关于页（独立页面）：品牌图标 + 名称 + 简介 + 隐私政策 / 用户协议 入口 + 版本号。
 * 条款正文在本页内切换显示，返回键先回关于页。
 */
public class AboutScreen extends FrameLayout {

    public interface Host { void closeAbout(); void toast(String msg); }

    private final Host host;
    private final LinearLayout col;
    private final TextView barTitle;
    private final ScrollView sv;
    private final int padTop;
    private int page;   // 0=关于 1=隐私政策 2=用户协议

    public AboutScreen(Context c, Host host, int insetTop, int insetBottom) {
        super(c);
        this.host = host;
        this.padTop = insetTop;
        setBackgroundColor(Ui.BG);

        sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(OVER_SCROLL_ALWAYS);
        addView(sv, new FrameLayout.LayoutParams(-1, -1));

        col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        int h = Ui.dpi(22);
        col.setPadding(h, insetTop + Ui.dpi(70), h, insetBottom + Ui.dpi(36));
        sv.addView(col, new FrameLayout.LayoutParams(-1, -2));

        // 顶栏
        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dpi(16), insetTop + Ui.dpi(10), Ui.dpi(16), 0);

        FrameLayout back = new FrameLayout(c);
        back.setBackground(new Segmented.FrostDrawable(-1f));
        back.addView(new Ic(c, Ic.BACK, Ui.TEXT),
                new FrameLayout.LayoutParams(Ui.dpi(18), Ui.dpi(18), Gravity.CENTER));
        bar.addView(back, new LinearLayout.LayoutParams(Ui.dpi(40), Ui.dpi(40)));
        Ui.tap(back, new Runnable() {
            @Override public void run() { goBack(); }
        });

        barTitle = new TextView(c);
        barTitle.setTextSize(17f);
        barTitle.setTypeface(Ui.serifMed());
        barTitle.setTextColor(Ui.TEXT);
        barTitle.setIncludeFontPadding(false);
        barTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1f);
        tlp.leftMargin = Ui.dpi(6);
        tlp.rightMargin = Ui.dpi(40);
        bar.addView(barTitle, tlp);

        addView(bar, new FrameLayout.LayoutParams(-1, Ui.dpi(56) + padTop, Gravity.TOP));

        showAbout();
    }

    public boolean isDoc() { return page != 0; }

    /** 返回：条款页先回到关于页 */
    public void goBack() {
        if (page != 0) showAbout();
        else host.closeAbout();
    }

    public void showAbout() {
        page = 0;
        barTitle.setText(Str.s("about_entry"));
        col.removeAllViews();

        // 品牌标记：半透明图案 + 玻璃边
        FrameLayout mark = new FrameLayout(getContext());
        mark.setBackground(new Segmented.FrostDrawable(Ui.dp(28)));
        mark.setPadding(Ui.dpi(7), Ui.dpi(7), Ui.dpi(7), Ui.dpi(7));
        android.widget.ImageView mv = new android.widget.ImageView(getContext());
        mv.setImageResource(R.drawable.about_mark);
        mv.setAlpha(0.94f);
        mv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        GradientDrawable mg = new GradientDrawable();
        mg.setCornerRadius(Ui.dp(24));
        mv.setBackground(mg);
        mv.setClipToOutline(true);
        mark.addView(mv, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(Ui.dpi(104), Ui.dpi(104));
        mlp.gravity = Gravity.CENTER_HORIZONTAL;
        mlp.topMargin = Ui.dpi(16);
        col.addView(mark, mlp);

        TextView name = new TextView(getContext());
        name.setText("小雨点");
        name.setTextSize(19f);
        name.setTypeface(Ui.serifMed());
        name.setTextColor(Ui.TEXT);
        name.setIncludeFontPadding(false);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-1, -2);
        nlp.topMargin = Ui.dpi(16);
        col.addView(name, nlp);

        TextView intro = new TextView(getContext());
        intro.setText(Str.s("about_intro"));
        intro.setTextSize(13f);
        intro.setLineSpacing(Ui.dp(6f), 1f);
        intro.setTextColor(Ui.TEXT_2);
        intro.setIncludeFontPadding(false);
        intro.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(-1, -2);
        ilp.topMargin = Ui.dpi(18);
        col.addView(intro, ilp);

        // 条款入口
        col.addView(linkRow(Str.s("privacy_policy"), new Runnable() {
            @Override public void run() { showDoc(1); }
        }, Ui.dpi(26)));
        col.addView(linkRow(Str.s("terms"), new Runnable() {
            @Override public void run() { showDoc(2); }
        }, Ui.dpi(14)));

        TextView ver = new TextView(getContext());
        ver.setText(Str.s("current_version") + " " + Ui.VERSION);
        ver.setTextSize(11.5f);
        ver.setTextColor(Ui.TEXT_3);
        ver.setGravity(Gravity.CENTER);
        ver.setIncludeFontPadding(false);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(-1, -2);
        vlp.topMargin = Ui.dpi(28);
        col.addView(ver, vlp);
    }

    private View linkRow(String label, final Runnable click, int topMargin) {
        TextView t = new TextView(getContext());
        t.setText(label);
        t.setTextSize(14.5f);
        t.setTextColor(Ui.TEXT);
        t.setIncludeFontPadding(false);
        t.setPadding(0, Ui.dpi(16), 0, Ui.dpi(16));
        GradientDrawable g = new GradientDrawable();
        g.setColor(Ui.alpha(Ui.TEXT, 0.0f));
        t.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = topMargin;
        t.setLayoutParams(lp);
        Ui.tap(t, click);
        return t;
    }

    public void showDoc(int which) {
        page = which;
        barTitle.setText(which == 1 ? Str.s("privacy_policy") : Str.s("terms"));
        col.removeAllViews();

        if (Str.lang == Str.ZH) {
            String text = which == 1 ? Legal.PRIVACY : Legal.TERMS;
            android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
            for (String line : text.split("\n", -1)) {
                boolean head = line.matches("^[一二三四五六七八九十]+、.*")
                        || line.matches("^第[一二三四五六七八九十]+条.*")
                        || line.startsWith("生效日期");
                int st = sb.length();
                sb.append(line).append("\n");
                if (head) {
                    sb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                            st, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new android.text.style.RelativeSizeSpan(1.14f),
                            st, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new android.text.style.ForegroundColorSpan(Ui.TEXT),
                            st, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            TextView body = new TextView(getContext());
            body.setText(sb);
            body.setTextSize(13f);
            body.setLineSpacing(Ui.dp(7f), 1f);
            body.setTextColor(Ui.TEXT_2);
            body.setIncludeFontPadding(false);
            col.addView(body, new LinearLayout.LayoutParams(-1, -2));
        } else {
            TextView t = new TextView(getContext());
            t.setText(Str.s("legal_title"));
            t.setTextSize(17f);
            t.setTypeface(Ui.serifMed());
            t.setTextColor(Ui.TEXT);
            t.setIncludeFontPadding(false);
            col.addView(t, new LinearLayout.LayoutParams(-1, -2));

            TextView b = new TextView(getContext());
            b.setText(Str.s("legal_body"));
            b.setTextSize(13f);
            b.setLineSpacing(Ui.dp(7f), 1f);
            b.setTextColor(Ui.TEXT_2);
            b.setIncludeFontPadding(false);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
            blp.topMargin = Ui.dpi(14);
            col.addView(b, blp);

            TextView dis = new TextView(getContext());
            dis.setText(Str.s("legal_disclaimer"));
            dis.setTextSize(12.5f);
            dis.setLineSpacing(Ui.dp(6f), 1f);
            dis.setTextColor(Ui.TEXT_2);
            dis.setIncludeFontPadding(false);
            dis.setTypeface(Ui.reg(), android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, -2);
            dlp.topMargin = Ui.dpi(16);
            col.addView(dis, dlp);

            TextView hint = new TextView(getContext());
            hint.setText(Str.s("legal_hint"));
            hint.setTextSize(12.5f);
            hint.setLineSpacing(Ui.dp(6f), 1f);
            hint.setTextColor(Ui.ACCENT);
            hint.setIncludeFontPadding(false);
            LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, -2);
            hlp.topMargin = Ui.dpi(12);
            col.addView(hint, hlp);
        }

        final TextView mail = new TextView(getContext());
        mail.setText(Legal.CONTACT);
        mail.setTextSize(12f);
        mail.setTextColor(Ui.TEXT);
        mail.setGravity(Gravity.CENTER);
        mail.setIncludeFontPadding(false);
        mail.setPadding(Ui.dpi(16), Ui.dpi(11), Ui.dpi(16), Ui.dpi(11));
        mail.setBackground(new Segmented.FrostDrawable(Ui.dp(14)));
        Ui.tap(mail, new Runnable() {
            @Override public void run() {
                try {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager)
                            getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("email", Legal.CONTACT));
                    }
                    host.toast(Str.s("copied"));
                } catch (Throwable ignored) { }
            }
        });
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, -2);
        mlp.topMargin = Ui.dpi(22);
        col.addView(mail, mlp);
        sv.scrollTo(0, 0);
    }
}
