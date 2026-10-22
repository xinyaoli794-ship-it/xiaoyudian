package com.xiaoyudian.notes;

import android.content.Context;
import android.content.SharedPreferences;

/** 本地设置：外观、字号、视图、昵称头像 */
public final class Settings {

    private static final String FILE = "xiaoyudian_settings";

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;
    public static final int THEME_CUSTOM = 3;

    public static final int VIEW_LIST = 0;
    public static final int VIEW_DETAIL = 1;

    private Settings() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int themeMode(Context c) {
        return p(c).getInt("theme", THEME_SYSTEM);
    }

    public static void setThemeMode(Context c, int v) {
        p(c).edit().putInt("theme", v).apply();
    }

    public static boolean isDark(Context c) {
        int m = themeMode(c);
        if (m == THEME_DARK) return true;
        if (m == THEME_LIGHT) return false;
        int night = c.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return night == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    /** 0..3 → 0.9 / 1.0 / 1.12 / 1.25 */
    public static int fontLevel(Context c) {
        return Math.max(0, Math.min(3, p(c).getInt("font", 1)));
    }

    public static void setFontLevel(Context c, int v) {
        p(c).edit().putInt("font", Math.max(0, Math.min(3, v))).apply();
    }

    public static float fontScale(Context c) {
        switch (fontLevel(c)) {
            case 0: return 0.90f;
            case 2: return 1.12f;
            case 3: return 1.26f;
            default: return 1.00f;
        }
    }

    public static int viewMode(Context c) {
        return p(c).getInt("view", VIEW_LIST);
    }

    public static void setViewMode(Context c, int v) {
        p(c).edit().putInt("view", v).apply();
    }

    public static String signature(Context c) {
        String s = p(c).getString("signature", null);
        return (s == null || s.trim().isEmpty()) ? Str.s("default_signature") : s;
    }

    public static void setSignature(Context c, String s) {
        if (s == null) s = "";
        s = s.trim();
        if (s.length() > 14) s = s.substring(0, 14);   // 限字数，保证抽屉排版
        p(c).edit().putString("signature", s).apply();
    }

    public static boolean backupAuto(Context c) { return p(c).getBoolean("bkAuto", false); }

    public static void setBackupAuto(Context c, boolean v) { p(c).edit().putBoolean("bkAuto", v).apply(); }

    public static int backupMode(Context c) { return p(c).getInt("bkMode", 0); }

    public static void setBackupMode(Context c, int m) { p(c).edit().putInt("bkMode", m).apply(); }

    public static int backupInterval(Context c) { return p(c).getInt("bkGap", 5); }

    public static void setBackupInterval(Context c, int m) { p(c).edit().putInt("bkGap", m).apply(); }

    public static long lastBackup(Context c) { return p(c).getLong("bkLast", 0); }

    public static void setLastBackup(Context c, long t) { p(c).edit().putLong("bkLast", t).apply(); }

    public static boolean langChosen(Context c) { return p(c).getBoolean("langChosen", false); }

    public static void setLangChosen(Context c, boolean v) { p(c).edit().putBoolean("langChosen", v).apply(); }

    public static int lang(Context c) {
        return p(c).getInt("lang", Str.ZH);
    }

    public static void setLang(Context c, int l) {
        p(c).edit().putInt("lang", l).apply();
    }

    public static int accent(Context c) {
        return p(c).getInt("accent", 0);
    }

    public static float accentValue(Context c) {
        return p(c).getFloat("accentValue", 0.55f);
    }

    public static void setAccentValue(Context c, float v) {
        p(c).edit().putFloat("accentValue", v).apply();
    }

    public static void setAccent(Context c, int color) {
        p(c).edit().putInt("accent", color).apply();
    }

    public static String nickname(Context c) {
        String s = p(c).getString("nick", null);
        return (s == null || s.trim().isEmpty()) ? Str.s("default_nick") : s;
    }

    public static void setNickname(Context c, String s) {
        p(c).edit().putString("nick", s).apply();
    }

    public static String avatar(Context c) {
        return p(c).getString("avatar", null);
    }

    public static void setAvatar(Context c, String path) {
        p(c).edit().putString("avatar", path).apply();
    }
}
