package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** 左滑抽屉：头像昵称 / 全部便签 / 自定义分类 / 回收站 / 设置 */
public class DrawerPanel extends FrameLayout {

    public interface Host {
        void selectFolder(long id);
        void openTrash();
        void newFolder();
        void folderMenu(Folder f);
        void openSettings();
    }

    private final Host host;
    private final LinearLayout col;
    private final Path panel = new Path();
    private final RectF tmpRect = new RectF();
    private final float[] radii = new float[8];

    private long current = 0L;

    public DrawerPanel(Context c, Host h) {
        super(c);
        this.host = h;
        setWillNotDraw(false);
        // 吃掉面板空白处的点击，防止穿透到背后的遮罩而误关闭
        setClickable(true);

        col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dpi(12), Ui.dpi(30), Ui.dpi(12), Ui.dpi(16));
        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(OVER_SCROLL_NEVER);
        sv.addView(col, new ScrollView.LayoutParams(-1, -2));
        addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    public void setCurrent(long id) {
        current = id;
        rebuild();
    }

    public void setInsets(int top, int bottom) {
        col.setPadding(Ui.dpi(12), top + Ui.dpi(18), Ui.dpi(12), bottom + Ui.dpi(14));
    }

    public void rebuild() {
        col.removeAllViews();
        Context c = getContext();
        Db db = Db.get(c);

        // ---------- 头像 / 昵称 ----------
        LinearLayout brand = new LinearLayout(c);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.setPadding(Ui.dpi(8), Ui.dpi(4), Ui.dpi(4), Ui.dpi(18));
        col.addView(brand, new LinearLayout.LayoutParams(-1, -2));

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
            avatar.addView(new Ic(c, Ic.DROP, Ui.DARK ? 0xFF11162B : 0xFFFFFFFF),
                    new FrameLayout.LayoutParams(Ui.dpi(22), Ui.dpi(22), Gravity.CENTER));
        }
        brand.addView(avatar, new LinearLayout.LayoutParams(Ui.dpi(46), Ui.dpi(46)));

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1f);
        tlp.leftMargin = Ui.dpi(12);
        brand.addView(texts, tlp);

        TextView name = new TextView(c);
        name.setText(Settings.nickname(c));
        name.setTextSize(17f);
        name.setTypeface(Ui.med());
        name.setTextColor(Ui.TEXT);
        name.setIncludeFontPadding(false);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(name);

        TextView tag = new TextView(c);
        tag.setText(Settings.signature(c));
        tag.setTextSize(10.5f);
        tag.setTextColor(Ui.TEXT_4);
        tag.setIncludeFontPadding(false);
        LinearLayout.LayoutParams tl2 = new LinearLayout.LayoutParams(-2, -2);
        tl2.topMargin = Ui.dpi(4);
        texts.addView(tag, tl2);

        FrameLayout gear = new FrameLayout(c);
        gear.addView(new Ic(c, Ic.GEAR, Ui.TEXT_2),
                new FrameLayout.LayoutParams(Ui.dpi(19), Ui.dpi(19), Gravity.CENTER));
        brand.addView(gear, new LinearLayout.LayoutParams(Ui.dpi(40), Ui.dpi(40)));
        Ui.tap(gear, new Runnable() {
            @Override public void run() { DrawerPanel.this.host.openSettings(); }
        });
        Ui.tap(brand, new Runnable() {
            @Override public void run() { DrawerPanel.this.host.openSettings(); }
        });

        // ---------- 全部便签 ----------
        col.addView(row(c, Ic.NOTE, Str.s("all_notes"), db.countAll(), current == 0L, false,
                new Runnable() { @Override public void run() { host.selectFolder(0L); } }, null),
                new LinearLayout.LayoutParams(-1, Ui.dpi(46)));

        // ---------- 分类 ----------
        col.addView(section(c, Str.s("folders")), new LinearLayout.LayoutParams(-1, -2));
        List<Folder> folders = db.folders();
        for (final Folder f : folders) {
            View frow = row(c, Ic.FOLDER, f.name, f.count, current == f.id, false,
                    new Runnable() { @Override public void run() { host.selectFolder(f.id); } },
                    new Runnable() { @Override public void run() { host.folderMenu(f); } });
            if (f.color != 0) tintIcon(frow, f.color);
            col.addView(frow, new LinearLayout.LayoutParams(-1, Ui.dpi(46)));
        }

        col.addView(row(c, Ic.PLUS, Str.s("new_folder"), -1, false, true,
                new Runnable() { @Override public void run() { host.newFolder(); } }, null),
                new LinearLayout.LayoutParams(-1, Ui.dpi(46)));

        // ---------- 回收站 ----------
        col.addView(new View(c), new LinearLayout.LayoutParams(-1, Ui.dpi(14)));

        View divider = new View(c);
        divider.setBackgroundColor(Ui.alpha(Ui.TEXT, 0.08f));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, Math.max(1, Ui.dpi(1)));
        dlp.leftMargin = Ui.dpi(10);
        dlp.rightMargin = Ui.dpi(10);
        col.addView(divider, dlp);
        col.addView(new View(c), new LinearLayout.LayoutParams(-1, Ui.dpi(8)));

        int trash = db.countTrash();
        col.addView(row(c, Ic.TRASH, Str.s("trash"), trash, false, false,
                new Runnable() { @Override public void run() { host.openTrash(); } }, null),
                new LinearLayout.LayoutParams(-1, Ui.dpi(46)));

        col.addView(new View(c), new LinearLayout.LayoutParams(-1, Ui.dpi(22)));

        TextView foot = new TextView(c);
        foot.setText(Ui.VERSION + " · " + Str.s("local_only"));
        foot.setTextSize(10f);
        foot.setTextColor(Ui.alpha(Ui.TEXT, 0.25f));
        foot.setGravity(Gravity.CENTER);
        col.addView(foot, new LinearLayout.LayoutParams(-1, -2));
    }

    /** 递归把这一行里的图标染成指定颜色 */
    private void tintIcon(View v, int color) {
        if (v instanceof Ic) {
            ((Ic) v).setColor(color);
            return;
        }
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) tintIcon(g.getChildAt(i), color);
        }
    }

    private View section(Context c, String label) {
        TextView v = new TextView(c);
        v.setText(label);
        v.setTextSize(10.5f);
        v.setTextColor(Ui.TEXT_4);
        v.setIncludeFontPadding(false);
        v.setPadding(Ui.dpi(12), Ui.dpi(18), Ui.dpi(12), Ui.dpi(7));
        return v;
    }

    private View row(Context c, int icon, String label, int count, boolean selected,
                     boolean accent, final Runnable click, final Runnable longClick) {
        FrameLayout box = new FrameLayout(c);
        box.setBackground(bgFor(selected));

        LinearLayout inner = new LinearLayout(c);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(Ui.dpi(12), 0, Ui.dpi(12), 0);
        box.addView(inner, new FrameLayout.LayoutParams(-1, -1));

        int iconColor = accent ? Ui.ACCENT
                : (selected ? Ui.TEXT : Ui.alpha(Ui.TEXT, 0.70f));
        inner.addView(new Ic(c, icon, iconColor), new LinearLayout.LayoutParams(Ui.dpi(17), Ui.dpi(17)));

        TextView t = new TextView(c);
        t.setText(label);
        t.setTextSize(14f);
        t.setTextColor(accent ? Ui.ACCENT : (selected ? Ui.TEXT : Ui.TEXT_2));
        t.setMaxLines(1);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setIncludeFontPadding(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.leftMargin = Ui.dpi(12);
        inner.addView(t, lp);

        if (count >= 0) {
            TextView c2 = new TextView(c);
            c2.setText(String.valueOf(count));
            c2.setTextSize(11f);
            c2.setTextColor(Ui.TEXT_4);
            c2.setIncludeFontPadding(false);
            inner.addView(c2, new LinearLayout.LayoutParams(-2, -2));
        }

        box.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { click.run(); }
        });
        if (longClick != null) {
            box.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) { longClick.run(); return true; }
            });
        }
        return box;
    }

    private StateListDrawable bgFor(boolean selected) {
        StateListDrawable s = new StateListDrawable();
        GradientDrawable pressed = new GradientDrawable();
        pressed.setCornerRadius(Ui.dp(13));
        pressed.setColor(selected ? Ui.alpha(Ui.TEXT, 0.18f) : Ui.alpha(Ui.TEXT, 0.10f));
        s.addState(new int[]{android.R.attr.state_pressed}, pressed);

        GradientDrawable normal = new GradientDrawable();
        normal.setCornerRadius(Ui.dp(13));
        normal.setColor(selected ? Ui.alpha(Ui.TEXT, 0.12f) : 0x00000000);
        s.addState(new int[]{}, normal);
        return s;
    }

    @Override
    protected void dispatchDraw(Canvas c) {
        tmpRect.set(0, 0, getWidth(), getHeight());
        float r = Ui.dp(28);
        radii[0] = 0; radii[1] = 0;
        radii[2] = r; radii[3] = r;
        radii[4] = r; radii[5] = r;
        radii[6] = 0; radii[7] = 0;
        panel.reset();
        panel.addRoundRect(tmpRect, radii, Path.Direction.CW);
        Glass.panelPath(c, panel, this, Ui.VEIL * 1.3f, Ui.BORDER, false);
        super.dispatchDraw(c);
    }
}
