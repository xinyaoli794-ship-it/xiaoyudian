package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 设置：头像、昵称、外观、字号、视图，并带实时预览 */
public class SettingsScreen extends FrameLayout {

    public interface Host {
        void closeSettings();
        void pickAvatar(SettingsScreen s);
        void applyTheme(int mode);   // 需要重建界面
        void applyFont(int level);   // 需要重建界面
        void applyView(int mode);
        void editNickname();
        void editSignature();
        void showAbout();
        void editLanguage();
        void openData();
    }

    /** recreate 之后是否需要重新打开设置页 */
    public static boolean reopen = false;

    private final Host host;
    private final LinearLayout col;
    private final ScrollView scroll;
    private final Note demo = new Note();
    private NoteCard previewCard;

    private static final String QUOTE = "当我无法安慰你，或你不再关怀我，请千万记住，\n"
            + "在我们菲薄的流年里，曾有十二只白鹭鸶飞过秋天的湖泊。";

    public SettingsScreen(Context c, Host host) {
        super(c);
        this.host = host;
        setWillNotDraw(false);

        col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        addView(col, new FrameLayout.LayoutParams(-1, -1));

        FrameLayout bar = new FrameLayout(c);
        col.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dpi(54)));

        FrameLayout back = new FrameLayout(c);
        back.addView(new Ic(c, Ic.BACK, Ui.TEXT),
                new FrameLayout.LayoutParams(Ui.dpi(20), Ui.dpi(20), Gravity.CENTER));
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(Ui.dpi(46), Ui.dpi(46),
                Gravity.START | Gravity.CENTER_VERTICAL);
        blp.leftMargin = Ui.dpi(6);
        bar.addView(back, blp);
        Ui.tap(back, new Runnable() {
            @Override public void run() { SettingsScreen.this.host.closeSettings(); }
        });

        TextView title = new TextView(c);
        title.setText(Str.s("settings"));
        title.setTextSize(16f);
        title.setTypeface(Ui.med());
        title.setTextColor(Ui.TEXT);
        title.setIncludeFontPadding(false);
        bar.addView(title, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));

        scroll = new ScrollView(c);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dpi(14), Ui.dpi(4), Ui.dpi(14), Ui.dpi(40));
        scroll.addView(box, new ScrollView.LayoutParams(-1, -2));
        col.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        rebuild(box);
    }

    public void setInsets(int top, int bottom) {
        col.setPadding(0, top, 0, bottom);
    }

    public void rebuild() {
        rebuild((LinearLayout) scroll.getChildAt(0));
    }

    private void rebuild(LinearLayout box) {
        box.removeAllViews();
        Context c = getContext();

        // ---------- 个人资料 ----------
        LinearLayout profile = card(c, box, Str.s("nickname"));

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dpi(6), 0, Ui.dpi(6));
        profile.addView(row, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout avatar = new FrameLayout(c);
        String av = Settings.avatar(c);
        if (av != null) {
            RoundImage ri = new RoundImage(c);
            ri.setRadius(23);
            ri.setPath(av);
            avatar.addView(ri, new FrameLayout.LayoutParams(-1, -1));
        } else {
            GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{Ui.ACCENT, Ui.ACCENT_2});
            bg.setShape(GradientDrawable.OVAL);
            avatar.setBackground(bg);
            avatar.addView(new Ic(c, Ic.AVATAR, Ui.DARK ? 0xFF11162B : 0xFFFFFFFF),
                    new FrameLayout.LayoutParams(Ui.dpi(22), Ui.dpi(22), Gravity.CENTER));
        }
        row.addView(avatar, new LinearLayout.LayoutParams(Ui.dpi(48), Ui.dpi(48)));
        Ui.tap(avatar, new Runnable() {
            @Override public void run() { SettingsScreen.this.host.pickAvatar(SettingsScreen.this); }
        });

        LinearLayout nameCol = new LinearLayout(c);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, -2, 1f);
        nlp.leftMargin = Ui.dpi(14);
        row.addView(nameCol, nlp);

        TextView nick = new TextView(c);
        nick.setText(Settings.nickname(c));
        nick.setTextSize(15.5f);
        nick.setTextColor(Ui.TEXT);
        nick.setTypeface(Ui.med());
        nick.setIncludeFontPadding(false);
        nameCol.addView(nick);

        TextView hint = new TextView(c);
        hint.setText(Str.s("change_nickname"));
        TextView sign = new TextView(c);
        sign.setText(Str.s("signature") + " · " + Settings.signature(c));
        sign.setTextSize(11.5f);
        sign.setTextColor(Ui.TEXT_3);
        sign.setIncludeFontPadding(false);
        sign.setMaxLines(1);
        sign.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams sgl = new LinearLayout.LayoutParams(-1, -2);
        sgl.topMargin = Ui.dpi(7);
        box.addView(sign, sgl);
        Ui.tap(sign, new Runnable() {
            @Override public void run() { SettingsScreen.this.host.editSignature(); }
        });
        hint.setTextSize(11f);
        hint.setTextColor(Ui.TEXT_4);
        hint.setIncludeFontPadding(false);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-2, -2);
        hlp.topMargin = Ui.dpi(4);
        nameCol.addView(hint, hlp);

        Ui.tap(row, new Runnable() {
            @Override public void run() { SettingsScreen.this.host.editNickname(); }
        });

        // ---------- 外观（分段控制器：滑块 + 文字颜色过渡） ----------
        LinearLayout theme = card(c, box, Str.s("appearance"));
        theme.addView(new Segmented(c, new String[]{Str.s("follow_system"), Str.s("light"), Str.s("dark"), Str.s("custom")},
                Settings.themeMode(c), new Segmented.OnPick() {
                    @Override public void onPick(final int i) {
                        Settings.setThemeMode(getContext(), i);
                        SettingsScreen.reopen = true;
                        if (i == Settings.THEME_CUSTOM && Settings.accent(getContext()) == 0) {
                            Settings.setAccent(getContext(), 0xFF7FD1C1);
                        }
                        if (i != Settings.THEME_CUSTOM && Settings.accent(getContext()) != 0) {
                            Settings.setAccent(getContext(), 0);
                        }
                        SettingsScreen.this.postDelayed(new Runnable() {
                            @Override public void run() { host.applyTheme(i); }
                        }, 240);
                    }
                }), new LinearLayout.LayoutParams(-1, Ui.dpi(40)));

        if (Settings.themeMode(c) == Settings.THEME_CUSTOM) {
            theme.addView(swatches(c), new LinearLayout.LayoutParams(-1, Ui.dpi(46)));
            int base = Settings.accent(c) == 0 ? 0xFF7FD1C1 : Settings.accent(c);
            theme.addView(new BrightnessBar(c, base, Settings.accentValue(c),
                    new BrightnessBar.OnChange() {
                        @Override public void onColor(int color, float value) {
                            Settings.setAccent(getContext(), color);
                            Settings.setAccentValue(getContext(), value);
                            SettingsScreen.reopen = true;
                            host.applyTheme(Settings.THEME_CUSTOM);
                        }
                    }), new LinearLayout.LayoutParams(-1, Ui.dpi(42)));
        }
        // ---------- 字号 ----------
        LinearLayout font = card(c, box, Str.s("font_size"));
        font.addView(new Segmented(c, new String[]{Str.s("font_s"), Str.s("font_m"), Str.s("font_l"), Str.s("font_xl")},
                Settings.fontLevel(c), new Segmented.OnPick() {
                    @Override public void onPick(final int i) {
                        Settings.setFontLevel(getContext(), i);
                        SettingsScreen.reopen = true;
                        SettingsScreen.this.postDelayed(new Runnable() {
                            @Override public void run() { host.applyFont(i); }
                        }, 240);
                    }
                }), new LinearLayout.LayoutParams(-1, Ui.dpi(40)));

        TextView sample = new TextView(c);
        sample.setText(Str.s("sample_text"));
        sample.setTextSize(15f);
        sample.setTextColor(Ui.TEXT_2);
        sample.setIncludeFontPadding(false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.topMargin = Ui.dpi(14);
        font.addView(sample, slp);

        // ---------- 预览（完整展示，不缩略、不省略） ----------
        TextView plabel = new TextView(c);
        plabel.setText(Str.s("preview"));
        plabel.setTextSize(11f);
        plabel.setTextColor(Ui.TEXT_4);
        plabel.setIncludeFontPadding(false);
        plabel.setPadding(Ui.dpi(6), Ui.dpi(18), Ui.dpi(6), Ui.dpi(8));
        box.addView(plabel, new LinearLayout.LayoutParams(-1, -2));

        GlassBox pv = new GlassBox(c).style(20f, Ui.VEIL, Ui.BORDER, false);
        LinearLayout pinner = new LinearLayout(c);
        pinner.setOrientation(LinearLayout.VERTICAL);
        pinner.setPadding(Ui.dpi(16), Ui.dpi(14), Ui.dpi(16), Ui.dpi(14));
        pv.addView(pinner, new FrameLayout.LayoutParams(-1, -2));

        TextView pbody = new TextView(c);
        pbody.setText(Str.s("preview_text"));
        pbody.setTextSize(13.5f);
        pbody.setTextColor(Ui.TEXT);
        pbody.setLineSpacing(Ui.dp(4f), 1f);
        pbody.setIncludeFontPadding(false);
        pinner.addView(pbody, new LinearLayout.LayoutParams(-1, -2));

        TextView psign = new TextView(c);
        psign.setText(Str.lang == Str.JA ? "— 簡媜" : "——简媜");
        psign.setTextSize(12f);
        psign.setTextColor(Ui.TEXT_3);
        psign.setIncludeFontPadding(false);
        LinearLayout.LayoutParams psgp = new LinearLayout.LayoutParams(-1, -2);
        psgp.topMargin = Ui.dpi(10);
        pinner.addView(psign, psgp);
        box.addView(pv, new LinearLayout.LayoutParams(-1, -2));
        // ---------- 数据管理 ----------
        LinearLayout dataCard = card(c, box, Str.s("data_mgmt"));
        TextView dataRow = new TextView(c);
        dataRow.setText(Str.s("data_mgmt"));
        dataRow.setTextSize(13.5f);
        dataRow.setTextColor(Ui.TEXT);
        dataRow.setIncludeFontPadding(false);
        dataRow.setPadding(Ui.dpi(18), Ui.dpi(14), Ui.dpi(18), Ui.dpi(14));
        dataCard.addView(dataRow, new LinearLayout.LayoutParams(-1, -2));
        Ui.tap(dataRow, new Runnable() {
            @Override public void run() { SettingsScreen.this.host.openData(); }
        });

        // ---------- 语言 ----------
        LinearLayout langCard = card(c, box, Str.s("language"));
        TextView langRow = new TextView(c);
        langRow.setText(Lang.name(Settings.lang(c)));
        langRow.setTextSize(13.5f);
        langRow.setTextColor(Ui.TEXT);
        langRow.setIncludeFontPadding(false);
        langRow.setPadding(Ui.dpi(18), Ui.dpi(14), Ui.dpi(18), Ui.dpi(14));
        langCard.addView(langRow, new LinearLayout.LayoutParams(-1, -2));
        Ui.tap(langRow, new Runnable() {
            @Override public void run() { SettingsScreen.this.host.editLanguage(); }
        });

        // ---------- 关于 ----------
        LinearLayout aboutCard = card(c, box, Str.s("about"));
        TextView aboutRow = new TextView(c);
        aboutRow.setText(Str.s("about_entry"));
        aboutRow.setTextSize(13.5f);
        aboutRow.setTextColor(Ui.TEXT);
        aboutRow.setIncludeFontPadding(false);
        aboutRow.setPadding(Ui.dpi(18), Ui.dpi(14), Ui.dpi(18), Ui.dpi(14));
        aboutCard.addView(aboutRow, new LinearLayout.LayoutParams(-1, -2));
        Ui.tap(aboutRow, new Runnable() {
            @Override public void run() { SettingsScreen.this.host.showAbout(); }
        });

        TextView about = new TextView(c);
        about.setText("小雨点 " + Ui.VERSION + " · " + Str.s("local_save"));
        about.setTextSize(10.5f);
        about.setTextColor(Ui.alpha(Ui.TEXT, 0.30f));
        about.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, -2);
        alp.topMargin = Ui.dpi(18);
        box.addView(about, alp);
    }

    /** 自定义主题色板 */
    private View swatches(Context c) {
        final int[] cols = {0xFF8FA9FF, 0xFF7FD1C1, 0xFFFFB86B, 0xFFFF8FA3,
                0xFFB99CFF, 0xFF9BE87F, 0xFF6BC6FF};
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dpi(14), 0, 0);
        int cur = Settings.accent(c);
        for (int i = 0; i < cols.length; i++) {
            final int col = cols[i];
            FrameLayout dot = new FrameLayout(c);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(col);
            if (cur == col) g.setStroke(Ui.dpi(2), Ui.TEXT);
            dot.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dpi(28), Ui.dpi(28));
            lp.rightMargin = Ui.dpi(10);
            row.addView(dot, lp);
            Ui.tap(dot, new Runnable() {
                @Override public void run() {
                    Settings.setAccent(getContext(), col);
                    SettingsScreen.reopen = true;
                    host.applyTheme(Settings.THEME_CUSTOM);
                }
            });
        }
        return row;
    }

    private LinearLayout card(Context c, LinearLayout parent, String title) {
        TextView t = new TextView(c);
        t.setText(title);
        t.setTextSize(11f);
        t.setTextColor(Ui.TEXT_4);
        t.setIncludeFontPadding(false);
        t.setPadding(Ui.dpi(6), Ui.dpi(18), Ui.dpi(6), Ui.dpi(8));
        parent.addView(t, new LinearLayout.LayoutParams(-1, -2));

        GlassBox box = new GlassBox(c).style(22f, Ui.VEIL, Ui.BORDER, false);
        LinearLayout inner = new LinearLayout(c);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(Ui.dpi(16), Ui.dpi(14), Ui.dpi(16), Ui.dpi(16));
        box.addView(inner, new FrameLayout.LayoutParams(-1, -2));
        parent.addView(box, new LinearLayout.LayoutParams(-1, -2));
        return inner;
    }

    @Override
    protected void dispatchDraw(Canvas c) {
        Glass.page(c, getWidth(), getHeight());
        Paint line = new Paint();
        line.setColor(Ui.alpha(Ui.TEXT, 0.10f));
        c.drawRect(0, 0, getWidth(), Math.max(1f, Ui.dp(1f)), line);
        super.dispatchDraw(c);
    }
}
