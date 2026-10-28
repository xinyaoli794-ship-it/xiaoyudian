package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** 搜索页：按内容过滤全部便签 */
public class SearchScreen extends FrameLayout {

    public interface Host {
        void closeSearch();
        void openResult(long noteId);
    }

    private final Host host;
    private final LinearLayout col;
    private final EditText input;
    private final ListView list;
    private final List<Note> data = new ArrayList<Note>();
    private final Adapter adapter = new Adapter();
    private final TextView empty;

    public SearchScreen(Context c, Host host) {
        super(c);
        this.host = host;
        setWillNotDraw(false);

        setBackgroundColor(Ui.BG);
        col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        addView(col, new FrameLayout.LayoutParams(-1, -1));

        FrameLayout bar = new FrameLayout(c);
        col.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dpi(58)));

        FrameLayout back = new FrameLayout(c);
        back.setBackground(new Segmented.FrostDrawable(Ui.dp(12)));
        back.addView(new Ic(c, Ic.BACK, Ui.TEXT),
                new FrameLayout.LayoutParams(Ui.dpi(18), Ui.dpi(18), Gravity.CENTER));
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(Ui.dpi(38), Ui.dpi(38),
                Gravity.START | Gravity.CENTER_VERTICAL);
        blp.leftMargin = Ui.dpi(12);
        bar.addView(back, blp);
        Ui.tap(back, new Runnable() {
            @Override public void run() { SearchScreen.this.host.closeSearch(); }
        });

        input = new EditText(c);
        input.setSingleLine(true);
        input.setTextSize(15f);
        input.setTextColor(Ui.TEXT);
        input.setHint(Str.s("search_content"));
        input.setHintTextColor(Ui.alpha(Ui.TEXT, 0.32f));
        input.setBackground(null);
        input.setPadding(Ui.dpi(12), 0, Ui.dpi(12), 0);
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(-1, Ui.dpi(40),
                Gravity.CENTER_VERTICAL);
        ilp.leftMargin = Ui.dpi(58);
        ilp.rightMargin = Ui.dpi(16);
        bar.addView(input, ilp);

        list = new ListView(c);
        list.setBackgroundColor(Ui.BG);
        list.setDivider(null);
        list.setDividerHeight(Ui.dpi(2));
        list.setSelector(new android.graphics.drawable.ColorDrawable(0));
        list.setCacheColorHint(0);
        list.setVerticalScrollBarEnabled(false);
        list.setOverScrollMode(OVER_SCROLL_ALWAYS);
        list.setAdapter(adapter);
        list.setPadding(Ui.dpi(14), Ui.dpi(6), Ui.dpi(14), Ui.dpi(40));
        col.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        empty = new TextView(c);
        empty.setText(Str.s("no_result"));
        empty.setTextSize(13f);
        empty.setTextColor(Ui.TEXT_4);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(GONE);
        addView(empty, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { query(e.toString().trim()); }
        });

        input.post(new Runnable() {
            @Override public void run() {
                input.requestFocus();
                InputMethodManager imm = (InputMethodManager)
                        getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    public void setInsets(int top, int bottom) {
        col.setPadding(0, top, 0, bottom);
    }

    private void query(String q) {
        data.clear();
        if (!q.isEmpty()) data.addAll(Db.get(getContext()).search(q));
        adapter.notifyDataSetChanged();
        empty.setVisibility(data.isEmpty() ? VISIBLE : GONE);
        list.setVisibility(data.isEmpty() ? GONE : VISIBLE);
    }

    @Override
    protected void dispatchDraw(Canvas c) {
        Glass.page(c, getWidth(), getHeight());
        Paint line = new Paint();
        line.setColor(Ui.alpha(Ui.TEXT, 0.10f));
        c.drawRect(0, 0, getWidth(), Math.max(1f, Ui.dp(1f)), line);
        super.dispatchDraw(c);
    }

    private class Adapter extends BaseAdapter {
        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int i) { return data.get(i); }
        @Override public long getItemId(int i) { return data.get(i).id; }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout box;
            if (convert instanceof LinearLayout) {
                box = (LinearLayout) convert;
            } else {
                box = new LinearLayout(getContext());
                box.setOrientation(LinearLayout.VERTICAL);
                box.setPadding(Ui.dpi(8), Ui.dpi(12), Ui.dpi(8), Ui.dpi(12));
                TextView t = new TextView(getContext());
                t.setTextSize(15f);
                t.setTextColor(Ui.TEXT);
                t.setIncludeFontPadding(false);
                t.setMaxLines(1);
                t.setEllipsize(android.text.TextUtils.TruncateAt.END);
                box.addView(t, new LinearLayout.LayoutParams(-1, -2));
                TextView p = new TextView(getContext());
                p.setTextSize(12.5f);
                p.setTextColor(Ui.TEXT_3);
                p.setIncludeFontPadding(false);
                p.setMaxLines(1);
                LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, -2);
                plp.topMargin = Ui.dpi(5);
                box.addView(p, plp);
            }
            final Note n = data.get(pos);
            String title = n.title().replace("\uFFFC", "").trim();
            String preview = n.preview().replace("\uFFFC", "").replace("\n", " ").trim();
            TextView t = (TextView) box.getChildAt(0);
            TextView p = (TextView) box.getChildAt(1);
            t.setText(title.isEmpty() ? Str.s("note_image_br") : title);
            p.setText(preview.isEmpty() ? Ui.formatTime(n.updated) : preview);
            box.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { host.openResult(n.id); }
            });
            return box;
        }
    }
}
