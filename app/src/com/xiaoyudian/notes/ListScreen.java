package com.xiaoyudian.notes;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** 首页：置顶分区 + 便签列表（列表/详情视图）+ 底部雨滴按钮 */
public class ListScreen extends FrameLayout implements NoteRow.Listener {

    public interface Host {
        void openEditor(long noteId, long folderId);
        void openDrawer();
        void noteMenu(Note n);
        void pin(Note n);
        void move(Note n);
        void delete(Note n);
        void switchView(int mode);
        void openSearch();
    }

    private static final String H_PIN = "@@pin@@";
    private static final String H_DIV = "@@divider@@";

    private final Host host;
    private final List<Object> rows = new ArrayList<>();
    private final List<Note> notes = new ArrayList<>();
    private final Adapter adapter = new Adapter();
    private final FrameLayout headerWrap;
    private final LinearLayout headerRow;
    private final TextView title, subtitle;
    private final Ic viewIcon;
    private final ListView list;
    private final LinearLayout empty;
    private final TextView emptyTitle, emptySub;
    private final DropFab fab;

    private long folderId = 0L;
    private int viewMode = Settings.VIEW_LIST;
    private NoteRow openRow;
    private int lastFirst;
    private int settleTries = 0;
    private int lastFirstSeen = -1;

    public ListScreen(Context c, Host host) {
        super(c);
        this.host = host;
        this.viewMode = Settings.viewMode(c);

        headerWrap = new FrameLayout(c);
        // 顶部整块用实色：内容不会从标题下方透出，点标题也不会误触列表
        headerWrap.setBackgroundColor(Ui.BG);
        headerWrap.setClickable(true);
        addView(headerWrap, new FrameLayout.LayoutParams(-1, Ui.dpi(112)));

        headerRow = new LinearLayout(c);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(Ui.dpi(10), Ui.dpi(6), Ui.dpi(12), 0);
        headerWrap.addView(headerRow, new FrameLayout.LayoutParams(-1, -1));

        FrameLayout menuBtn = new FrameLayout(c);
        menuBtn.addView(new Ic(c, Ic.MENU, Ui.TEXT),
                new FrameLayout.LayoutParams(Ui.dpi(21), Ui.dpi(21), Gravity.CENTER));
        headerRow.addView(menuBtn, new LinearLayout.LayoutParams(Ui.dpi(44), Ui.dpi(44)));
        Ui.tap(menuBtn, new Runnable() {
            @Override public void run() { ListScreen.this.host.openDrawer(); }
        });

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1f);
        clp.leftMargin = Ui.dpi(6);
        headerRow.addView(col, clp);

        title = new TextView(c);
        title.setTextSize(19f);
        title.setTypeface(Ui.serifMed());
        title.setTextColor(Ui.TEXT);
        title.setIncludeFontPadding(false);
        col.addView(title);

        subtitle = new TextView(c);
        subtitle.setTextSize(10.5f);
        subtitle.setTextColor(Ui.TEXT_3);
        subtitle.setIncludeFontPadding(false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-2, -2);
        slp.topMargin = Ui.dpi(3);
        col.addView(subtitle, slp);

        FrameLayout searchBtn = new FrameLayout(c);
        searchBtn.addView(new Ic(c, Ic.SEARCH, Ui.TEXT_2),
                new FrameLayout.LayoutParams(Ui.dpi(19), Ui.dpi(19), Gravity.CENTER));
        headerRow.addView(searchBtn, new LinearLayout.LayoutParams(Ui.dpi(40), Ui.dpi(40)));
        Ui.tap(searchBtn, new Runnable() {
            @Override public void run() { ListScreen.this.host.openSearch(); }
        });

        FrameLayout viewBtn = new FrameLayout(c);
        viewIcon = new Ic(c, viewMode == Settings.VIEW_DETAIL ? Ic.LINES : Ic.GRID, Ui.TEXT_2);
        viewBtn.addView(viewIcon, new FrameLayout.LayoutParams(Ui.dpi(19), Ui.dpi(19), Gravity.CENTER));
        headerRow.addView(viewBtn, new LinearLayout.LayoutParams(Ui.dpi(40), Ui.dpi(40)));
        Ui.tap(viewBtn, new Runnable() {
            @Override public void run() {
                int next = ListScreen.this.viewMode == Settings.VIEW_LIST
                        ? Settings.VIEW_DETAIL : Settings.VIEW_LIST;
                ListScreen.this.host.switchView(next);
            }
        });

        list = new ListView(c);
        list.setDivider(null);
        list.setDividerHeight(Ui.dpi(7));
        list.setSelector(new ColorDrawable(0));
        list.setCacheColorHint(0);
        list.setVerticalScrollBarEnabled(false);
        list.setOverScrollMode(OVER_SCROLL_ALWAYS);   // 保留边缘回弹
        list.setAdapter(adapter);
        list.setPadding(Ui.dpi(14), 0, Ui.dpi(14), Ui.dpi(120));
        // clipToPadding=false：内容可以画进 padding 区域，一路延伸到屏幕底边，
        // 从悬浮按钮下方滑过；padding 只作为"可滚动余量"，不再形成实体底栏
        list.setClipToPadding(false);
        list.setScrollingCacheEnabled(false);
        try {
            list.setFriction(Ui.FLING_FRICTION);
            list.setVelocityScale(Ui.FLING_VELOCITY);
        } catch (Throwable ignored) { }
        addView(list, new FrameLayout.LayoutParams(-1, -1));

        empty = new LinearLayout(c);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(Ui.dpi(32), 0, Ui.dpi(32), Ui.dpi(90));
        empty.setVisibility(GONE);
        Ic ghost = new Ic(c, Ic.NOTE, Ui.alpha(Ui.TEXT, 0.22f));
        empty.addView(ghost, new LinearLayout.LayoutParams(Ui.dpi(46), Ui.dpi(46)));

        emptyTitle = new TextView(c);
        emptyTitle.setTextSize(15f);
        emptyTitle.setTextColor(Ui.TEXT_2);
        emptyTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(-2, -2);
        elp.topMargin = Ui.dpi(16);
        empty.addView(emptyTitle, elp);

        emptySub = new TextView(c);
        emptySub.setTextSize(12.5f);
        emptySub.setTextColor(Ui.TEXT_4);
        emptySub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams elp2 = new LinearLayout.LayoutParams(-2, -2);
        elp2.topMargin = Ui.dpi(7);
        empty.addView(emptySub, elp2);
        addView(empty, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        headerWrap.bringToFront();

        fab = new DropFab(c, new DropFab.Host() {
            @Override public void onFabTap(boolean topMode) {
                if (topMode) scrollToTop();
                else ListScreen.this.host.openEditor(-1, ListScreen.this.folderId);
            }
        });
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(Ui.dpi(60), Ui.dpi(60),
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        flp.bottomMargin = Ui.dpi(30);
        addView(fab, flp);

        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView v, int s) { }

            @Override public void onScroll(AbsListView v, int first, int visible, int total) {
                View child = v.getChildAt(0);
                int pt = v.getPaddingTop();
                boolean atTop = first == 0 && (child == null || child.getTop() >= pt - 2);
                if (atTop) {
                    fab.setMode(DropFab.MODE_DROP, true);
                    fab.show();
                } else if (first > lastFirst) {
                    fab.hide();
                } else if (first < lastFirst) {
                    fab.setMode(DropFab.MODE_TOP, true);
                    fab.show();
                }
                lastFirst = first;
            }
        });
    }

    public long folderId() { return folderId; }

    public void setInsets(int top, int bottom) {
        ViewGroup.LayoutParams lp = headerWrap.getLayoutParams();
        lp.height = top + Ui.dpi(104);
        headerWrap.setLayoutParams(lp);
        headerRow.setPadding(Ui.dpi(10), top + Ui.dpi(8), Ui.dpi(12), 0);
        // 顶部留出标题区，底部只留"能让最后一条完整露在按钮上方"的滚动余量
        list.setPadding(Ui.dpi(14), top + Ui.dpi(108), Ui.dpi(14), bottom + Ui.dpi(88));
        FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) fab.getLayoutParams();
        flp.bottomMargin = bottom + Ui.dpi(22);
        fab.setLayoutParams(flp);
    }

    public void setFolder(long id, boolean animate) {
        folderId = id;
        reload(animate);
    }

    public void setViewMode(int mode) {
        viewMode = mode;
        Settings.setViewMode(getContext(), mode);
        viewIcon.setKind(mode == Settings.VIEW_DETAIL ? Ic.LINES : Ic.GRID);
        adapter.notifyDataSetChanged();
        list.post(new Runnable() {
            @Override public void run() {
                if (openRow != null) { openRow.collapse(false); openRow = null; }
                animateIn();
            }
        });
    }

    public void reload(boolean animate) {
        notes.clear();
        notes.addAll(Db.get(getContext()).notes(folderId, false));
        rows.clear();
        boolean hasPin = false;
        for (Note n : notes) if (n.pinned) { hasPin = true; break; }
        if (hasPin) {
            rows.add(H_PIN);
            for (Note n : notes) if (n.pinned) rows.add(n);
            rows.add(H_DIV);
        }
        for (Note n : notes) if (!n.pinned) rows.add(n);
        adapter.notifyDataSetChanged();
        refreshHeader();
        if (openRow != null) { openRow.collapse(false); openRow = null; }
        boolean isAll = folderId == 0L;
        if (notes.isEmpty()) {
            empty.setVisibility(VISIBLE);
            list.setVisibility(GONE);
            emptyTitle.setText(isAll ? Str.s("empty_all") : Str.s("empty_folder"));
            emptySub.setText(Str.s("empty_hint"));
            empty.setAlpha(0f);
            empty.animate().alpha(1f).setDuration(280).start();
        } else {
            empty.setVisibility(GONE);
            list.setVisibility(VISIBLE);
            if (animate) animateIn();
        }
    }

    private void refreshHeader() {
        String name = folderId == 0L ? Str.s("all_notes") : Db.get(getContext()).folderName(folderId);
        title.setText(folderId == 0L ? "小雨点" : name);
        subtitle.setText(folderId == 0L
                ? (notes.isEmpty() ? Str.s("empty_hint") : Str.f("notes_count", notes.size()))
                : Str.f("category_count", notes.size()));
    }

    /**
     * 回到顶部：一镜到底。
     * 1) 先掐断正在进行的惯性滑动，避免两股力打架；
     * 2) 用平台滚动器一次性滚到位（先快后慢、500ms），不做逐帧追赶；
     * 3) 结束后只做 ≤2px 的亚像素修正，绝不出现肉眼可见的瞬移。
     */
    public void scrollToTop() {
        try {
            // 用一次 0 距离的平滑滚动顶掉正在跑的惯性/回弹（内部会替换旧 scroller）
            list.smoothScrollBy(0, 1);
        } catch (Throwable ignored) { }

        View first = list.getChildAt(0);
        if (first == null) return;
        int pt = list.getPaddingTop();
        if (pt - first.getTop() <= 1) return;   // 已经在顶部

        list.smoothScrollToPositionFromTop(0, pt, 500);
        list.postDelayed(new Runnable() {
            @Override public void run() { settleTop(); }
        }, 520);
    }

    /**
     * 落地修正：滚动器还没跑完就继续等；只有真的差了几像素才做一次 140ms 的缓出微调，
     * 任何情况下都不做 setSelectionFromTop 这种肉眼可见的瞬移。
     */
    private void settleTop() {
        try {
            View f = list.getChildAt(0);
            if (f == null) return;
            int pt = list.getPaddingTop();
            int cur = pt - f.getTop();
            int firstPos = list.getFirstVisiblePosition();
            boolean stillMoving = firstPos != lastFirstSeen;
            lastFirstSeen = firstPos;

            if (Math.abs(cur) <= 2 && firstPos == 0) return;   // 已经到位

            if (stillMoving && settleTries < 5) {
                settleTries++;
                list.postDelayed(new Runnable() {
                    @Override public void run() { settleTop(); }
                }, 130);
                return;
            }

            if (firstPos == 0) {
                if (Math.abs(cur) <= 2) return;
                ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
                a.setDuration(140);
                a.setInterpolator(Ui.EASE_OUT_CUBIC);
                a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    private int done = 0;
                    @Override public void onAnimationUpdate(ValueAnimator anim) {
                        View f2 = list.getChildAt(0);
                        if (f2 == null) return;
                        float t = (Float) anim.getAnimatedValue();
                        int remain = list.getPaddingTop() - f2.getTop();
                        int want = (int) (remain * (1f - t));
                        int d = remain - want;
                        if (Math.abs(d) >= 1) list.scrollListBy(-d);
                    }
                });
                a.start();
            } else if (settleTries < 8) {
                settleTries++;
                list.smoothScrollToPositionFromTop(0, pt, 260);
                list.postDelayed(new Runnable() {
                    @Override public void run() { settleTop(); }
                }, 300);
            }
        } catch (Throwable ignored) { }
    }
    private void animateIn() {
        list.post(new Runnable() {
            @Override public void run() {
                int n = Math.min(list.getChildCount(), 9);
                for (int i = 0; i < n; i++) {
                    View v = list.getChildAt(i);
                    v.setAlpha(0f);
                    v.setTranslationY(Ui.dp(14));
                    v.animate().alpha(1f).translationY(0f).setStartDelay(i * 38L)
                            .setDuration(320).setInterpolator(Ui.EASE).start();
                }
            }
        });
    }

    /** 数据变化后的轻量刷新：只重排数据，不做任何透明度动画 */
    public void softRefresh() {
        reload(false);
    }

    /** 从编辑页返回：只有内容真的变了才重排，否则完全不碰列表 */
    public void onBackFromEditor(boolean changed) {
        if (changed) reload(false);
    }

    public void collapseRows() {
        if (openRow != null) { openRow.collapse(true); openRow = null; }
    }

    // ---------- NoteRow.Listener ----------

    @Override public void onOpen(Note n) { host.openEditor(n.id, folderId); }
    @Override public void onMenu(Note n) { host.noteMenu(n); }
    @Override public void onPin(Note n) { host.pin(n); }
    @Override public void onMove(Note n) { host.move(n); }
    @Override public void onDelete(Note n) { host.delete(n); }

    @Override
    public void onRowOpened(NoteRow row) {
        if (openRow != null && openRow != row) openRow.collapse(true);
        openRow = row;
    }

    private View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(Ui.alpha(Ui.TEXT, 0.10f));
        LinearLayout wrap = new LinearLayout(c);
        wrap.setPadding(Ui.dpi(4), Ui.dpi(10), Ui.dpi(4), Ui.dpi(4));
        wrap.addView(v, new LinearLayout.LayoutParams(-1, Math.max(1, Ui.dpi(1))));
        return wrap;
    }

    private View sectionHeader(Context c) {
        TextView t = new TextView(c);
        t.setText(Str.s("pinned"));
        t.setTextSize(11f);
        t.setTypeface(Ui.serif());
        t.setTextColor(Ui.TEXT_4);
        t.setIncludeFontPadding(false);
        t.setPadding(Ui.dpi(4), Ui.dpi(6), Ui.dpi(4), Ui.dpi(2));
        return t;
    }

    private class Adapter extends BaseAdapter {
        @Override public int getCount() { return rows.size(); }
        @Override public Object getItem(int i) { return rows.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override public int getViewTypeCount() { return 2; }
        @Override public int getItemViewType(int pos) { return rows.get(pos) instanceof String ? 0 : 1; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            Object item = rows.get(pos);
            if (item instanceof String) {
                if (H_DIV.equals(item)) return divider(getContext());
                return sectionHeader(getContext());
            }
            NoteRow row;
            if (convert instanceof NoteRow) {
                row = (NoteRow) convert;
            } else {
                row = new NoteRow(getContext(), ListScreen.this);
                row.setLayoutParams(new ListView.LayoutParams(-1, -2));
            }
            row.bind((Note) item, viewMode == Settings.VIEW_DETAIL);
            return row;
        }
    }
}
