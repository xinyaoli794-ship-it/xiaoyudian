package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 便签卡片：玻璃面板 + 标题 / 摘要 / 置顶标记 / 时间 / 地点 / 缩略图 */
public class NoteCard extends FrameLayout {

    private final RectF rect = new RectF();
    private final TextView title;
    private final TextView preview;
    private final TextView time;
    private final TextView locText;
    private final LinearLayout locChip;
    private final LinearLayout thumbRow;
    private final LinearLayout pinChip;
    private final LinearLayout folderRow;
    private final TextView folderText;
    private final LinearLayout col;

    public NoteCard(Context c) {
        super(c);
        setWillNotDraw(false);

        col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dpi(15), Ui.dpi(13), Ui.dpi(15), Ui.dpi(12));
        addView(col, new FrameLayout.LayoutParams(-1, -2));

        title = new TextView(c);
        title.setTextSize(15.5f);
        title.setTypeface(Ui.med());
        title.setTextColor(Ui.TEXT);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setIncludeFontPadding(false);
        col.addView(title, lp(0));

        preview = new TextView(c);
        preview.setTextSize(13.5f);
        preview.setTextColor(Ui.TEXT_2);
        preview.setMaxLines(3);
        preview.setLineSpacing(Ui.dp(3f), 1f);
        preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        preview.setIncludeFontPadding(false);
        col.addView(preview, lp(7));

        thumbRow = new LinearLayout(c);
        thumbRow.setOrientation(LinearLayout.HORIZONTAL);
        thumbRow.setGravity(Gravity.CENTER_VERTICAL);
        col.addView(thumbRow, lp(11));

        LinearLayout footer = new LinearLayout(c);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        col.addView(footer, lp(11));

        pinChip = chip(c, 0x2A9FB8FF, 0x44A8BFFF);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-2, -2);
        plp.rightMargin = Ui.dpi(8);
        footer.addView(pinChip, plp);
        pinChip.addView(new Ic(c, Ic.PIN, 0xFFC9D7FF), new LinearLayout.LayoutParams(Ui.dpi(10), Ui.dpi(10)));
        TextView pinText = new TextView(c);
        pinText.setText(Str.s("pin"));
        pinText.setTextSize(10f);
        pinText.setTextColor(0xFFC9D7FF);
        pinText.setIncludeFontPadding(false);
        LinearLayout.LayoutParams ptl = new LinearLayout.LayoutParams(-2, -2);
        ptl.leftMargin = Ui.dpi(4);
        pinChip.addView(pinText, ptl);

        time = new TextView(c);
        time.setTextSize(11.5f);
        time.setTextColor(Ui.TEXT_3);
        time.setIncludeFontPadding(false);
        footer.addView(time, new LinearLayout.LayoutParams(-2, -2));

        folderRow = new LinearLayout(c);
        folderRow.setOrientation(LinearLayout.HORIZONTAL);
        folderRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams frp = new LinearLayout.LayoutParams(-2, -2);
        frp.leftMargin = Ui.dpi(8);
        footer.addView(folderRow, frp);
        folderRow.addView(new Ic(c, Ic.FOLDER, Ui.alpha(Ui.TEXT, 0.42f)),
                new LinearLayout.LayoutParams(Ui.dpi(11), Ui.dpi(11)));
        folderText = new TextView(c);
        folderText.setTextSize(10.5f);
        folderText.setTextColor(Ui.TEXT_3);
        folderText.setMaxLines(1);
        folderText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        folderText.setMaxWidth(Ui.dpi(110));
        folderText.setIncludeFontPadding(false);
        LinearLayout.LayoutParams ftl = new LinearLayout.LayoutParams(-2, -2);
        ftl.leftMargin = Ui.dpi(4);
        folderRow.addView(folderText, ftl);

        locChip = chip(c, Ui.alpha(Ui.TEXT, 0.07f), Ui.alpha(Ui.TEXT, 0.14f));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
        clp.leftMargin = Ui.dpi(9);
        footer.addView(locChip, clp);
        locChip.addView(new Ic(c, Ic.PIN, Ui.alpha(Ui.TEXT, 0.62f)), new LinearLayout.LayoutParams(Ui.dpi(11), Ui.dpi(11)));

        locText = new TextView(c);
        locText.setTextSize(11f);
        locText.setTextColor(Ui.TEXT_2);
        locText.setMaxLines(1);
        locText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        locText.setIncludeFontPadding(false);
        locText.setMaxWidth(Ui.dpi(150));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-2, -2);
        tlp.leftMargin = Ui.dpi(4);
        locChip.addView(locText, tlp);
    }

    private LinearLayout chip(Context c, int fill, int stroke) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(Ui.dpi(8), Ui.dpi(3), Ui.dpi(9), Ui.dpi(3));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(Ui.dp(20));
        g.setColor(fill);
        g.setStroke(Ui.dpi(1), stroke);
        l.setBackground(g);
        return l;
    }

    private LinearLayout.LayoutParams lp(int topMarginDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = Ui.dpi(topMarginDp);
        return p;
    }

    public void bind(Note n) { bind(n, false); }

    private static String clean(String s) {
        if (s == null) return "";
        return s.replace("\uFFFC", "").replaceAll("[ \t]+", " ").trim();
    }

    public void bind(Note n, boolean detailed) {
        String t = clean(n.title());
        String p = clean(n.preview());
        if (t.isEmpty()) {
            t = n.attCount > 0 ? Str.s("note_image") : Str.s("note_empty");
            preview.setVisibility(GONE);
        } else {
            preview.setVisibility(p.isEmpty() ? GONE : VISIBLE);
            preview.setText(p);
            preview.setMaxLines(detailed ? 8 : 1);
        }
        title.setText(t);
        title.setMaxLines(detailed ? 2 : 1);
        // 列表视图：统一最小高度，各卡片等高
        setMinimumHeight(detailed ? 0 : Ui.dpi(78));
        time.setText(Ui.formatTime(n.updated));

        pinChip.setVisibility(GONE);   // 置顶状态改由列表分区标题表达
        if (n.folderName != null && !n.folderName.isEmpty()) {
            folderRow.setVisibility(VISIBLE);
            folderText.setText(n.folderName);
        } else {
            folderRow.setVisibility(GONE);
        }

        if (n.hasLocation()) {
            locChip.setVisibility(VISIBLE);
            locText.setText(Ui.cityOnly(n.locName));
        } else {
            locChip.setVisibility(GONE);
        }

        thumbRow.removeAllViews();
        if (detailed && n.thumb != null) {   // 列表视图不展示图片，保证各行等高
            thumbRow.setVisibility(VISIBLE);
            int size = Ui.dpi(detailed ? 64 : 46);
            RoundImage ri = new RoundImage(getContext());
            ri.setRadius(12);
            ri.setPath(n.thumb);
            thumbRow.addView(ri, new LinearLayout.LayoutParams(size, size));
            if (n.attCount > 1) {
                TextView more = new TextView(getContext());
                more.setText("+" + (n.attCount - 1));
                more.setTextSize(10.5f);
                more.setTextColor(Ui.TEXT_2);
                more.setIncludeFontPadding(false);
                more.setPadding(Ui.dpi(7), Ui.dpi(2), Ui.dpi(7), Ui.dpi(2));
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(Ui.dp(9));
                bg.setColor(0x1FFFFFFF);
                more.setBackground(bg);
                LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-2, -2);
                mlp.leftMargin = Ui.dpi(7);
                thumbRow.addView(more, mlp);
            }
        } else {
            thumbRow.setVisibility(GONE);
        }
    }

    @Override
    protected void dispatchDraw(Canvas c) {
        rect.set(0, 0, getWidth(), getHeight());
        Glass.panel(c, rect, Ui.dp(21), this, Ui.VEIL, Ui.BORDER, false);
        super.dispatchDraw(c);
    }
}
