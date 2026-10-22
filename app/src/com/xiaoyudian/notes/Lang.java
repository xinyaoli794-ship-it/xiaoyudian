package com.xiaoyudian.notes;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

/** 语言：把用户选择的语言应用到 App 的资源与全局 Locale（系统语言一并改变） */
public final class Lang {

    public static int current;

    public static void apply(Context c) {
        int l = Settings.lang(c);
        current = l;
        Str.lang = l;
        Locale loc = localeOf(l);
        Locale.setDefault(loc);
        try {
            Configuration cfg = new Configuration(c.getResources().getConfiguration());
            cfg.setLocale(loc);
            c.getResources().updateConfiguration(cfg, c.getResources().getDisplayMetrics());
        } catch (Throwable ignored) { }
    }

    public static Locale localeOf(int l) {
        switch (l) {
            case Str.TW: return Locale.TRADITIONAL_CHINESE;
            case Str.EN: return Locale.ENGLISH;
            case Str.JA: return Locale.JAPANESE;
            case Str.KO: return Locale.KOREAN;
            default: return Locale.SIMPLIFIED_CHINESE;
        }
    }

    public static String name(int l) {
        switch (l) {
            case Str.TW: return "繁體中文";
            case Str.EN: return "English";
            case Str.JA: return "日本語";
            case Str.KO: return "한국어";
            default: return "简体中文";
        }
    }

    private Lang() { }
}
