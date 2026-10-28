package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** 回收站：可还原，也可彻底删除 */
public class TrashScreen extends FrameLayout {

    public interface Host {
        void closeTrash();
        void requestEmptyTrash();
    }

    private final Host host;
    private final List<Note> data = new ArrayList<>();
    private final Adapter adapter = new Adapter();
    private final ListView list;
    private final LinearLayout empty;
    private final TextView clear;
    private final LinearLayout col;

    private void say(String key) {
        try {
            android.widget.Toast.makeText(getContext(), Str.s(key), android.widget.Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) { }
    }

    public TrashScreen(Context c, Host host) {
        super(c);
        this.host = host;
        setWillNotDraw(false);

        col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        addView(col, new FrameLayout.LayoutParams(-1, -1));
        // 常驻半透明“返回”胶囊（和主页“回到顶部”同一套视觉）
        TextView backPill = new TextView(c);
        backPill.setText(Str.s("back"));
        backPill.setTextSize(15f);
        backPill.setTextColor(Ui.TEXT);
        backPill.setGravity(Gravity.CENTER);
        backPill.setIncludeFontPadding(false);
        int ph = Ui.dpi(22);
        backPill.setPadding(ph, Ui.dpi(13), ph, Ui.dpi(13));
        backPill.setAlpha(0.92f);
        backPill.setBackground(new Segmented.FrostDrawable(-1f));
        backPill.setClickable(true);
        backPill.setElevation(Ui.dp(6f));
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        plp.bottomMargin = Ui.dpi(30);
        addView(backPill, plp);
        Ui.tap(backPill, new Runnable() {
            @Override public void run() { TrashScreen.this.host.closeTrash(); }
        });


        FrameLayout bar = new FrameLayout(c);
        int sb = 0;
        try {
            int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (id > 0) sb = getResources().getDimensionPixelSize(id);
        } catch (Throwable ignored) { }
        bar.setPadding(0, sb, 0, 0);   // 顶栏让开状态栏，保证返回键可点
        col.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dpi(56) + sb));


        TextView title = new TextView(c);
        title.setText(Str.s("trash"));
        title.setTextSize(16f);
        title.setTypeface(Ui.med());
        title.setTextColor(Ui.TEXT);
        title.setIncludeFontPadding(false);
        bar.addView(title, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));

        clear = new TextView(c);
        clear.setText(Str.s("clear_all"));
        clear.setTextSize(12.5f);
        clear.setTextColor(0xFFFF9E9E);
        clear.setIncludeFontPadding(false);
        clear.setPadding(Ui.dpi(12), Ui.dpi(10), Ui.dpi(14), Ui.dpi(10));
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(-2, -2, Gravity.END | Gravity.CENTER_VERTICAL);
        clp.rightMargin = Ui.dpi(8);
        bar.addView(clear, clp);
        Ui.tap(clear, new Runnable() {
            @Override public void run() { TrashScreen.this.host.requestEmptyTrash(); }
        });

        TextView hint = new TextView(c);
        hint.setText(Str.s("trash_hint"));
        hint.setTextSize(11f);
        hint.setTextColor(Ui.TEXT_4);
        hint.setPadding(Ui.dpi(22), 0, Ui.dpi(22), Ui.dpi(10));
        col.addView(hint, new LinearLayout.LayoutParams(-1, -2));

        list = new ListView(c);
        list.setDivider(null);
        list.setDividerHeight(Ui.dpi(10));
        list.setSelector(new ColorDrawable(0));
        list.setCacheColorHint(0);
        list.setVerticalScrollBarEnabled(false);
        list.setOverScrollMode(OVER_SCROLL_NEVER);
        list.setAdapter(adapter);
        list.setPadding(Ui.dpi(16), 0, Ui.dpi(16), Ui.dpi(30));
        list.setClipToPadding(false);
        col.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        empty = new LinearLayout(c);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(GONE);
        Ic ghost = new Ic(c, Ic.TRASH, 0x40F3F6FF);
        empty.addView(ghost, new LinearLayout.LayoutParams(Ui.dpi(40), Ui.dpi(40)));
        TextView et = new TextView(c);
        et.setText(Str.s("trash_empty"));
        et.setTextSize(14f);
        et.setTextColor(Ui.TEXT_3);
        et.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(-2, -2);
        elp.topMargin = Ui.dpi(14);
        empty.addView(et, elp);
        addView(empty, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        empty.setOnClickListener(null);
        reload();
    }

    public void setInsets(int top, int bottom) {
        col.setPadding(0, top, 0, bottom);
    }

    @Override
    protected void dispatchDraw(Canvas c) {
        Glass.page(c, getWidth(), getHeight());
        super.dispatchDraw(c);
    }

    public void reload() {
        data.clear();
        data.addAll(Db.get(getContext()).notes(0, true));
        adapter.notifyDataSetChanged();
        clear.setVisibility(data.isEmpty() ? INVISIBLE : VISIBLE);
        empty.setVisibility(data.isEmpty() ? VISIBLE : GONE);
        list.setVisibility(data.isEmpty() ? GONE : VISIBLE);
    }

    private View action(String label, int color, final Runnable r) {
        TextView t = new TextView(getContext());
        t.setText(label);
        t.setTextSize(11.5f);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setPadding(Ui.dpi(13), Ui.dpi(6), Ui.dpi(13), Ui.dpi(6));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(Ui.dp(18));
        g.setColor(color == 0xFFFF9E9E ? 0x1AFF7A7A : 0x1AFFFFFF);
        g.setStroke(Ui.dpi(1), color == 0xFFFF9E9E ? 0x33FF8A8A : 0x33FFFFFF);
        t.setBackground(g);
        Ui.tap(t, r);
        return t;
    }

    private class Row extends FrameLayout {
        final LinearLayout box;
        final TextView title, preview, time;
        final LinearLayout actions;

        Row(Context c) {
            super(c);
            setWillNotDraw(false);
            box = new LinearLayout(c);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(Ui.dpi(17), Ui.dpi(15), Ui.dpi(17), Ui.dpi(16));
            addView(box, new FrameLayout.LayoutParams(-1, -2));

            title = new TextView(c);
            title.setTextSize(15f);
            title.setTypeface(Ui.med());
            title.setTextColor(Ui.TEXT_2);
            title.setMaxLines(1);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            title.setIncludeFontPadding(false);
            box.addView(title);

            preview = new TextView(c);
            preview.setTextSize(12.5f);
            preview.setTextColor(Ui.TEXT_4);
            preview.setMaxLines(1);
            preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
            preview.setIncludeFontPadding(false);
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, -2);
            plp.topMargin = Ui.dpi(6);
            box.addView(preview, plp);

            LinearLayout bottom = new LinearLayout(c);
            bottom.setOrientation(LinearLayout.HORIZONTAL);
            bottom.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams blp2 = new LinearLayout.LayoutParams(-1, -2);
            blp2.topMargin = Ui.dpi(12);
            box.addView(bottom, blp2);

            time = new TextView(c);
            time.setTextSize(11f);
            time.setTextColor(Ui.TEXT_4);
            time.setIncludeFontPadding(false);
            bottom.addView(time, new LinearLayout.LayoutParams(0, -2, 1f));

            actions = new LinearLayout(c);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            bottom.addView(actions, new LinearLayout.LayoutParams(-2, -2));
        }

        void bind(final Note n) {
            String t = n.title();
            title.setText(t.isEmpty() ? (n.attCount > 0 ? Str.s("note_image") : Str.s("note_empty")) : t);
            preview.setText(n.preview());
            preview.setVisibility(n.preview().isEmpty() ? GONE : VISIBLE);
            time.setText(Ui.formatTime(n.deletedAt > 0 ? n.deletedAt : n.updated));
            actions.removeAllViews();
            actions.addView(action(Str.s("restore"), 0xFFBFD0FF, new Runnable() {
                @Override public void run() {
                    Db.get(getContext()).restore(n.id);
        say("toast_restored");
                    reload();
                }
            }));
            LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(Ui.dpi(8), -2);
            actions.addView(new View(getContext()), gap);
            actions.addView(action(Str.s("delete_forever"), 0xFFFF9E9E, new Runnable() {
                @Override public void run() {
                    Db.get(getContext()).purge(n.id);
        say("toast_purged");
                    reload();
                }
            }));
        }

        @Override
        protected void dispatchDraw(Canvas c) {
            Glass.panel(c, new android.graphics.RectF(0, 0, getWidth(), getHeight()),
                    Ui.dp(20), this, Ui.VEIL * 0.85f, Ui.BORDER * 0.8f, false);
            super.dispatchDraw(c);
        }
    }

    private class Adapter extends BaseAdapter {
        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int i) { return data.get(i); }
        @Override public long getItemId(int i) { return data.get(i).id; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            Row r;
            if (convert instanceof Row) r = (Row) convert;
            else {
                r = new Row(getContext());
                r.setLayoutParams(new ListView.LayoutParams(-1, -2));
            }
            r.bind(data.get(pos));
            return r;
        }
    }
}
