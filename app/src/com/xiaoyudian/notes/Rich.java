package com.xiaoyudian.notes;

import android.text.Spannable;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.graphics.Typeface;

/**
 * 富文本（加粗/斜体/下划线/删除线）的存取。
 * 用"逐字符掩码"序列化，避免 span 拆分导致的各种错乱：
 *   格式形如 "0,12,1|13,20,4"（起,止,掩码）
 */
public final class Rich {

    public static final int BOLD = 1;
    public static final int ITALIC = 2;
    public static final int UNDERLINE = 4;
    public static final int STRIKE = 8;

    private Rich() {}

    /** 把当前 Span 拍平成逐字符掩码 */
    public static int[] masks(Spanned s) {
        int n = s.length();
        int[] m = new int[n];
        StyleSpan[] ss = s.getSpans(0, n, StyleSpan.class);
        for (StyleSpan sp : ss) {
            int a = s.getSpanStart(sp), b = s.getSpanEnd(sp);
            int bit = sp.getStyle() == Typeface.BOLD ? BOLD
                    : (sp.getStyle() == Typeface.ITALIC ? ITALIC : 0);
            if (bit == 0) continue;
            for (int i = Math.max(0, a); i < Math.min(n, b); i++) m[i] |= bit;
        }
        UnderlineSpan[] us = s.getSpans(0, n, UnderlineSpan.class);
        for (UnderlineSpan sp : us) {
            for (int i = Math.max(0, s.getSpanStart(sp)); i < Math.min(n, s.getSpanEnd(sp)); i++) m[i] |= UNDERLINE;
        }
        StrikethroughSpan[] ds = s.getSpans(0, n, StrikethroughSpan.class);
        for (StrikethroughSpan sp : ds) {
            for (int i = Math.max(0, s.getSpanStart(sp)); i < Math.min(n, s.getSpanEnd(sp)); i++) m[i] |= STRIKE;
        }
        return m;
    }

    public static String serialize(Spanned s) {
        int[] m = masks(s);
        StringBuilder b = new StringBuilder();
        int i = 0;
        while (i < m.length) {
            int v = m[i];
            int j = i;
            while (j < m.length && m[j] == v) j++;
            if (v != 0) {
                if (b.length() > 0) b.append('|');
                b.append(i).append(',').append(j).append(',').append(v);
            }
            i = j;
        }
        return b.toString();
    }

    /** 依据序列化数据重建样式（会先清掉已有样式） */
    public static void restore(Spannable s, String data) {
        clear(s);
        if (data == null || data.isEmpty()) return;
        int n = s.length();
        for (String part : data.split("\\|")) {
            if (part.isEmpty()) continue;
            String[] f = part.split(",");
            if (f.length != 3) continue;
            try {
                int a = Integer.parseInt(f[0]), b = Integer.parseInt(f[1]), m = Integer.parseInt(f[2]);
                a = Math.max(0, Math.min(n, a));
                b = Math.max(a, Math.min(n, b));
                if (a >= b) continue;
                applyRange(s, a, b, m);
            } catch (NumberFormatException ignored) { }
        }
    }

    public static void clear(Spannable s) {
        for (Object o : s.getSpans(0, s.length(), Object.class)) {
            if (o instanceof StyleSpan || o instanceof UnderlineSpan || o instanceof StrikethroughSpan) {
                s.removeSpan(o);
            }
        }
    }

    private static void applyRange(Spannable s, int a, int b, int m) {
        if ((m & BOLD) != 0) s.setSpan(new StyleSpan(Typeface.BOLD), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if ((m & ITALIC) != 0) s.setSpan(new StyleSpan(Typeface.ITALIC), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if ((m & UNDERLINE) != 0) s.setSpan(new UnderlineSpan(), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if ((m & STRIKE) != 0) s.setSpan(new StrikethroughSpan(), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /** 重建成指定掩码（先清空） */
    public static void rebuild(Spannable s, int[] m) {
        clear(s);
        int n = Math.min(m.length, s.length());
        int i = 0;
        while (i < n) {
            int v = m[i];
            int j = i;
            while (j < n && m[j] == v) j++;
            if (v != 0) applyRange(s, i, j, v);
            i = j;
        }
    }

    /** 对 [a,b) 切换某个样式位；返回切换后该位是否生效 */
    public static boolean toggle(Spannable s, int a, int b, int bit) {
        int n = s.length();
        a = Math.max(0, Math.min(n, a));
        b = Math.max(a, Math.min(n, b));
        if (a >= b) return false;
        int[] m = masks(s);
        boolean all = true;
        for (int i = a; i < b; i++) if ((m[i] & bit) == 0) { all = false; break; }
        for (int i = a; i < b; i++) {
            if (all) m[i] &= ~bit;
            else m[i] |= bit;
        }
        rebuild(s, m);
        return !all;
    }

    /** 选区为空时，判断光标处是否已有某样式（用于按钮高亮） */
    public static boolean hasAt(Spanned s, int pos, int bit) {
        if (s.length() == 0) return false;
        int p = Math.max(0, Math.min(s.length() - 1, pos));
        return (masks(s)[p] & bit) != 0 || (p > 0 && (masks(s)[p - 1] & bit) != 0);
    }
}
