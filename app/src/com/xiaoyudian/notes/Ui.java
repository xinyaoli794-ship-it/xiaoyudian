package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** 全局设计常量、主题调色板与小工具（纯白 / 纯黑两套实色主题） */
public final class Ui {

    public static final Interpolator EASE    = new PathInterpolator(0.16f, 1f, 0.3f, 1f);
    public static final Interpolator EASE_IO = new PathInterpolator(0.4f, 0f, 0.2f, 1f);
    public static final Interpolator EASE_IN = new PathInterpolator(0.4f, 0f, 1f, 1f);
    public static final Interpolator SPRING  = new PathInterpolator(0.2f, 1.2f, 0.3f, 1f);
    /** 先快后慢（easeOutCubic），用于回顶、收敛类动画 */
    public static final Interpolator EASE_OUT_CUBIC = new PathInterpolator(0.215f, 0.61f, 0.355f, 1f);

    // ---- 手感参数 ----
    /** 抽屉跟手比例：1.0 = 手指移动多少抽屉跟多少 */
    public static final float DRAG_FOLLOW = 1.0f;
    /** 列表惯性：越小滑得越远 */
    public static final float FLING_FRICTION = 0.0105f;
    /** 列表惯性初速倍率：越大同样手势甩得越快 */
    public static final float FLING_VELOCITY = 1.08f;
    /** 抽屉松手后的惯性判定速度（px/s） */
    public static final float DRAWER_FLING = 850f;

    public static final String VERSION = "v1.0.1";
    public static int CUSTOM = 0;        // 自定义主色（色相），0 表示默认
    public static float CUSTOM_LEVEL = 0.55f;   // 自定义明度 0 浅 → 1 深

    private static float density = 0f;
    private static Typeface REG, MED, SERIF, SERIF_MED;

    // ---- 主题调色板 ----
    public static boolean DARK = true;
    public static int TEXT, TEXT_2, TEXT_3, TEXT_4;
    public static int ACCENT, ACCENT_2;
    public static int BG;          // 页面底色：纯黑 / 纯白
    public static int CARD;        // 卡片表面
    public static int CARD_2;      // 次级表面（输入框、按钮）
    public static int LINE;        // 分隔线 / 描边
    public static int FAB_BG, FAB_ICON;
    public static int SCRIM;

    // 兼容旧调用点
    public static int BG1;
    public static float VEIL = 0f, BORDER = 1f, SHEEN = 0f;

    private Ui() {}

    public static void init(Context c) {
        CUSTOM = Settings.accent(c);
        CUSTOM_LEVEL = Settings.accentValue(c);
        if (density <= 0f) {
            density = c.getResources().getDisplayMetrics().density;
            REG = Typeface.create("sans-serif", Typeface.NORMAL);
            MED = Typeface.create("sans-serif-medium", Typeface.NORMAL);
            SERIF = Typeface.create("serif", Typeface.NORMAL);
            SERIF_MED = Typeface.create("serif", Typeface.BOLD);
        }
        applyPalette(Settings.isDark(c));
    }

    public static void applyPalette(boolean dark) {
        // 自定义主题：由明度滑块决定整套界面的明暗（左浅右深），色相只决定色调
        if (CUSTOM != 0) {
            dark = CUSTOM_LEVEL > 0.55f;
        }
        DARK = dark;
        if (dark) {
            BG = 0xFF000000;
            CARD = 0xFF141416;
            CARD_2 = 0xFF1D1D20;
            LINE = 0xFF2C2C31;
            TEXT = 0xFFFFFFFF;
            TEXT_2 = 0xB3FFFFFF;
            TEXT_3 = 0x80FFFFFF;
            TEXT_4 = 0x59FFFFFF;
            ACCENT = 0xFF8FA9FF;
            ACCENT_2 = 0xFFB99CFF;
            FAB_BG = 0xCC1C1C21;
            FAB_ICON = 0xFFEDF1FF;
            SCRIM = 0xCC000000;
        } else {
            BG = 0xFFFFFFFF;
            CARD = 0xFFF6F6F8;
            CARD_2 = 0xFFEFEFF3;
            LINE = 0xFFE3E3E9;
            TEXT = 0xFF0B0B0F;
            TEXT_2 = 0xB30B0B0F;
            TEXT_3 = 0x7A0B0B0F;
            TEXT_4 = 0x4D0B0B0F;
            ACCENT = 0xFF3D5BD9;
            ACCENT_2 = 0xFF7B5BD9;
            FAB_BG = 0xD9FFFFFF;
            FAB_ICON = 0xFF2A3358;
            SCRIM = 0x66000000;
        }
        if (CUSTOM != 0) {
            float l = Math.max(0f, Math.min(1f, CUSTOM_LEVEL));
            ACCENT = dark ? CUSTOM : shade(CUSTOM, 0.62f);
            ACCENT_2 = dark ? shade(CUSTOM, 1.20f) : shade(CUSTOM, 0.48f);
            // 底色：白 → 近黑 的连续过渡，再叠上色相，明度条一拉整屏就变
            int baseBg = blend(0xFFFFFFFF, 0xFF050507, l);
            int baseCard = blend(0xFFFFFFFF, 0xFF141418, l);
            int baseCard2 = blend(0xFFFFFFFF, 0xFF1E1E23, l);
            int baseLine = blend(0xFFEDEDF1, 0xFF2E2E34, l);
            float tint = 0.10f + 0.26f * l;          // 越深，色调越明显
            BG = blend(baseBg, CUSTOM, tint * 0.75f);
            CARD = blend(baseCard, CUSTOM, tint);
            CARD_2 = blend(baseCard2, CUSTOM, tint * 1.1f);
            LINE = blend(baseLine, CUSTOM, 0.18f + 0.30f * l);
            FAB_BG = blend(dark ? 0xEE18181C : 0xF2FFFFFF, CUSTOM, tint);
        }
        BG1 = BG;
    }

    /** 在 a 和 b 之间混合（保留 a 的透明度） */
    public static int blend(int a, int b, float f) {
        if (f <= 0f) return a;
        if (f >= 1f) return (Color.alpha(a) << 24) | (b & 0x00FFFFFF);
        int r = Math.round(Color.red(a) * (1 - f) + Color.red(b) * f);
        int g = Math.round(Color.green(a) * (1 - f) + Color.green(b) * f);
        int bl = Math.round(Color.blue(a) * (1 - f) + Color.blue(b) * f);
        return Color.argb(Color.alpha(a), r, g, bl);
    }

    /**
     * 省 + 市 的简写，例如 "中国大陆 四川省达州市通川区…" → "四川省 达州市"
     * 解析不出来时退回原文（截断保证不撑破 UI）
     */
    public static String shortLoc(String full) {
        if (full == null) return "";
        String s = full.trim();
        if (s.isEmpty()) return "";
        String rest = s.replaceFirst("^(中国大陆|中华人民共和国|中国)\\s*", "");
        String prov = null, city = null;
        java.util.regex.Matcher m1 = java.util.regex.Pattern
                .compile("^(.{2,10}?(?:省|自治区|特别行政区))").matcher(rest);
        if (m1.find()) {
            prov = m1.group(1);
            rest = rest.substring(prov.length()).trim();
        }
        java.util.regex.Matcher m2 = java.util.regex.Pattern
                .compile("^(.{2,10}?(?:市|自治州|地区|盟))").matcher(rest);
        if (m2.find()) city = m2.group(1);
        if (prov != null && city != null) return prov + " " + city;
        if (city != null) return city;
        if (prov != null) return prov;
        return s.length() > 14 ? s.substring(0, 14) : s;
    }

    /** 只取城市名，例如 "中国大陆 四川省达州市通川区…" → "达州市" */
    public static String cityOnly(String full) {
        if (full == null) return "";
        String s = full.trim();
        if (s.isEmpty()) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("([^\\s]{2,10}?(?:市|自治州|地区|盟))").matcher(s);
        if (m.find()) return m.group(1);
        return shortLoc(s);
    }

    /** 亮度缩放：>1 变亮，<1 变暗 */
    public static int shade(int color, float k) {
        int r = Math.min(255, Math.round(Color.red(color) * k));
        int g = Math.min(255, Math.round(Color.green(color) * k));
        int b = Math.min(255, Math.round(Color.blue(color) * k));
        return Color.argb(Color.alpha(color), r, g, b);
    }

    public static float dp(float v) { return v * (density <= 0f ? 3f : density); }
    public static int dpi(float v) { return Math.round(dp(v)); }
    public static Typeface reg() { return REG; }
    public static Typeface med() { return MED; }
    /** 用于标题/分区标签，和正文字体形成区分 */
    public static Typeface serif() { return SERIF; }
    public static Typeface serifMed() { return SERIF_MED; }

    public static Paint text(int color, float sp, boolean medium) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(dp(sp));
        p.setTypeface(medium ? MED : REG);
        return p;
    }

    public static int alpha(int color, float a) {
        return Color.argb(Math.max(0, Math.min(255, Math.round(a * 255))),
                Color.red(color), Color.green(color), Color.blue(color));
    }

    public static String formatTime(long ts) {
        Calendar now = Calendar.getInstance();
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(ts);
        boolean sameDay = now.get(Calendar.YEAR) == t.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == t.get(Calendar.DAY_OF_YEAR);
        String hm = new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(ts));
        if (sameDay) return "今天 " + hm;
        Calendar y = Calendar.getInstance();
        y.add(Calendar.DAY_OF_YEAR, -1);
        boolean isY = y.get(Calendar.YEAR) == t.get(Calendar.YEAR)
                && y.get(Calendar.DAY_OF_YEAR) == t.get(Calendar.DAY_OF_YEAR);
        if (isY) return "昨天 " + hm;
        if (now.get(Calendar.YEAR) == t.get(Calendar.YEAR))
            return (t.get(Calendar.MONTH) + 1) + "-" + t.get(Calendar.DAY_OF_MONTH);
        return t.get(Calendar.YEAR) + "-" + (t.get(Calendar.MONTH) + 1) + "-" + t.get(Calendar.DAY_OF_MONTH);
    }

    public static String formatFull(long ts) {
        return new SimpleDateFormat("yyyy-M-d HH:mm", Locale.CHINA).format(new Date(ts));
    }

    /** 今天21:53 / 昨天14:12 / 3天前 / 最后更新2024-10-8 */
    public static String stamp(long ts) {
        Calendar now = Calendar.getInstance();
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(ts);
        long dayDiff = dayIndex(now) - dayIndex(t);
        String time = new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(ts));
        if (Str.lang != Str.ZH && Str.lang != Str.TW) {
            // 非中文界面不混入中文日期词
            if (dayDiff <= 0) return time;
            return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(ts));
        }
        if (dayDiff <= 0) return time;                       // 今天：只留时间
        if (dayDiff == 1) return "昨天" + time;
        if (dayDiff < 7) return dayDiff + "天前";
        return t.get(Calendar.YEAR) + "-"
                + (t.get(Calendar.MONTH) + 1) + "-" + t.get(Calendar.DAY_OF_MONTH);
    }

    private static long dayIndex(Calendar c) {
        Calendar d = (Calendar) c.clone();
        d.set(Calendar.HOUR_OF_DAY, 0);
        d.set(Calendar.MINUTE, 0);
        d.set(Calendar.SECOND, 0);
        d.set(Calendar.MILLISECOND, 0);
        return d.getTimeInMillis() / 86400000L;
    }

    public static int charCount(CharSequence s) {
        if (s == null) return 0;
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (!Character.isWhitespace(ch) && ch != '\n' && ch != '\u200b') n++;
        }
        return n;
    }

    public static String group(int v) {
        String s = String.valueOf(v);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) b.append(',');
            b.append(s.charAt(i));
        }
        return b.toString();
    }

    public static String statusLine(long ts, int chars) {
        return Str.s("last_update") + stamp(ts) + "\u2002\u2002" + Str.f("chars", chars);
    }

    public static void tap(final View v, final Runnable action) {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, android.view.MotionEvent e) {
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90)
                                .setInterpolator(EASE_IO).setListener(null).start();
                        return true;
                    case android.view.MotionEvent.ACTION_CANCEL:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(EASE).start();
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(EASE).start();
                        float dx = e.getX() - view.getWidth() / 2f;
                        float dy = e.getY() - view.getHeight() / 2f;
                        if (Math.hypot(dx, dy) < view.getWidth() * 0.75f && action != null) action.run();
                        return true;
                }
                return false;
            }
        });
        v.setClickable(true);
    }
}
