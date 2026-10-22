package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 数据管理：导出 / 导入 / 自动备份（独立页面，避免设置页过长） */
public class DataScreen extends FrameLayout {

    public interface Host {
        void exportFile();
        void importFile();
        void closeData();
        void toast(String msg);
    }

    private final Host host;
    private final LinearLayout col;
    private final int padTop;

    public DataScreen(Context c, Host host, int insetTop, int insetBottom) {
        super(c);
        this.host = host;
        this.padTop = insetTop;
        setBackgroundColor(Ui.BG);

        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(OVER_SCROLL_ALWAYS);
        addView(sv, new FrameLayout.LayoutParams(-1, -1));

        col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        int h = Ui.dpi(18);
        col.setPadding(h, insetTop + Ui.dpi(64), h, insetBottom + Ui.dpi(40));
        sv.addView(col, new FrameLayout.LayoutParams(-1, -2));

        build();
        addTopBar();
    }

    private void addTopBar() {
        LinearLayout bar = new LinearLayout(getContext());
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        int lp = Ui.dpi(18);
        bar.setPadding(lp, padTop + Ui.dpi(10), lp, 0);

        FrameLayout back = new FrameLayout(getContext());
        back.setBackground(new Segmented.FrostDrawable(-1f));
        back.addView(new Ic(getContext(), Ic.BACK, Ui.TEXT),
                new FrameLayout.LayoutParams(Ui.dpi(18), Ui.dpi(18), Gravity.CENTER));
        bar.addView(back, new LinearLayout.LayoutParams(Ui.dpi(40), Ui.dpi(40)));
        Ui.tap(back, new Runnable() {
            @Override public void run() { host.closeData(); }
        });

        TextView title = new TextView(getContext());
        title.setText(Str.s("data_mgmt"));
        title.setTextSize(17f);
        title.setTypeface(Ui.serifMed());
        title.setTextColor(Ui.TEXT);
        title.setIncludeFontPadding(false);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-2, -2);
        tlp.leftMargin = Ui.dpi(14);
        bar.addView(title, tlp);

        addView(bar, new FrameLayout.LayoutParams(-1, Ui.dpi(56) + padTop,
                Gravity.TOP));
        bar.setPadding(lp, padTop, lp, 0);
    }

    private void build() {
        // ---------- 导出 ----------
        LinearLayout ex = card(Str.s("export"));
        ex.addView(row(Str.s("export_all"), new Runnable() {
            @Override public void run() { host.exportFile(); }
        }));
        ex.addView(note(Str.s("export_hint")));

        // ---------- 导入 ----------
        LinearLayout im = card(Str.s("import"));
        im.addView(row(Str.s("import_all"), new Runnable() {
            @Override public void run() { host.importFile(); }
        }));
        im.addView(note(Str.s("import_hint")));

        // ---------- 自动备份 ----------
        LinearLayout ab = card(Str.s("auto_backup"));
        boolean on = Settings.backupAuto(getContext());
        ab.addView(labelled(Str.s("backup_switch"),
                new Segmented(getContext(), new String[]{Str.s("off"), Str.s("on")},
                        on ? 1 : 0, new Segmented.OnPick() {
                    @Override public void onPick(int i) {
                        Settings.setBackupAuto(getContext(), i == 1);
                        rebuild();
                    }
                })));

        if (on) {
            ab.addView(labelled(Str.s("backup_mode"),
                    new Segmented(getContext(),
                            new String[]{Str.s("backup_record"), Str.s("backup_launch")},
                            Settings.backupMode(getContext()), new Segmented.OnPick() {
                        @Override public void onPick(int i) {
                            Settings.setBackupMode(getContext(), i);
                        }
                    })));
            ab.addView(labelled(Str.s("backup_gap"),
                    new Segmented(getContext(), new String[]{"5", "15", "30", "60"},
                            gapIndex(Settings.backupInterval(getContext())), new Segmented.OnPick() {
                        @Override public void onPick(int i) {
                            Settings.setBackupInterval(getContext(), new int[]{5, 15, 30, 60}[i]);
                        }
                    })));

            long last = Settings.lastBackup(getContext());
            String when = last <= 0 ? Str.s("never")
                    : new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(last));
            ab.addView(note(Str.s("last_backup") + " " + when));
            ab.addView(warn(Str.s("backup_overwrite_warn")));
        }
        ab.addView(note(Str.s("storage_warn")));
    }

    private int gapIndex(int min) {
        if (min == 15) return 1;
        if (min == 30) return 2;
        if (min == 60) return 3;
        return 0;
    }

    private void rebuild() {
        col.removeAllViews();
        build();
    }

    // ---------------- 组件 ----------------

    private LinearLayout card(String title) {
        Context c = getContext();
        TextView t = new TextView(c);
        t.setText(title);
        t.setTextSize(11f);
        t.setTextColor(Ui.TEXT_4);
        t.setIncludeFontPadding(false);
        t.setPadding(Ui.dpi(6), Ui.dpi(18), Ui.dpi(6), Ui.dpi(8));
        col.addView(t, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(Ui.dp(18));
        g.setColor(Ui.CARD);
        g.setStroke(Math.max(1, Ui.dpi(0.75f)), Ui.alpha(Ui.TEXT, 0.07f));
        box.setBackground(g);
        box.setPadding(0, Ui.dpi(4), 0, Ui.dpi(4));
        col.addView(box, new LinearLayout.LayoutParams(-1, -2));
        return box;
    }

    private View row(String label, final Runnable click) {
        TextView t = new TextView(getContext());
        t.setText(label);
        t.setTextSize(13.5f);
        t.setTextColor(Ui.TEXT);
        t.setIncludeFontPadding(false);
        t.setPadding(Ui.dpi(18), Ui.dpi(14), Ui.dpi(18), Ui.dpi(14));
        t.setClickable(true);
        t.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));   // 整行玻璃区域都可点
        Ui.tap(t, click);
        return t;
    }

    private View note(String text) {
        TextView t = new TextView(getContext());
        t.setText(text);
        t.setTextSize(11f);
        t.setTextColor(Ui.TEXT_3);
        t.setLineSpacing(Ui.dp(3f), 1f);
        t.setIncludeFontPadding(false);
        t.setPadding(Ui.dpi(18), 0, Ui.dpi(18), Ui.dpi(12));
        return t;
    }

    private View warn(String text) {
        TextView t = new TextView(getContext());
        t.setText(text);
        t.setTextSize(11f);
        t.setTextColor(0xFFFF9E9E);
        t.setLineSpacing(Ui.dp(3f), 1f);
        t.setIncludeFontPadding(false);
        t.setPadding(Ui.dpi(18), 0, Ui.dpi(18), Ui.dpi(12));
        return t;
    }

    private View labelled(String label, View control) {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dpi(18), Ui.dpi(12), Ui.dpi(18), Ui.dpi(6));
        TextView t = new TextView(getContext());
        t.setText(label);
        t.setTextSize(12f);
        t.setTextColor(Ui.TEXT_3);
        t.setIncludeFontPadding(false);
        box.addView(t, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, Ui.dpi(38));
        clp.topMargin = Ui.dpi(8);
        box.addView(control, clp);
        return box;
    }
}
