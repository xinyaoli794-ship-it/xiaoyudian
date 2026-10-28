package com.xiaoyudian.notes;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** 编辑页：内嵌图片、顶部时间戳+字数+定位、撤回恢复、格式与待办工具条 */
public class EditorScreen extends FrameLayout {

    public interface Host {
        void closeEditor(EditorScreen s);
        void pickImages(EditorScreen s);
        void pickLocation(EditorScreen s);
        void previewImage(String path);
        void deleteNote(EditorScreen s);
        void pinNote(EditorScreen s);
        void moveNote(EditorScreen s);
        void showMore(EditorScreen s);
    }

    private static final int MAX_HISTORY = 80;
    private static final char PH = '\uFFFC';   // 图片占位符

    private final Host host;
    public final long noteId;
    public final long folderId;
    public final boolean isNew;

    private final RichEdit input;
    private final TextView status;
    private final FrameLayout undoBtn, redoBtn, moreBtn;
    private final LinearLayout col, headerRow, locChip, quickBar, formatRow, folderChip;
    private LinearLayout locRow;          // 定位单独一行，放在时间戳下方
    private String locFull = "";          // 完整地址
    private final TextView folderName;
    private final TextView locName;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean closed, applying = false, imeShown = false, formatMode = false;
    private boolean dirtyOnce = false;
    private int pendingMask = 0;
    private long updatedAt;
    private String originalText = "";
    private String originalSpans = "";
    private int padTop;
    private ValueAnimator padAnim;

    private final List<String> imgPaths = new ArrayList<String>();
    private boolean insertingImage = false;
    private int lastAttCount = -1;

    private final ArrayList<Snap> history = new ArrayList<Snap>();
    private int histIndex = -1;

    private static final int[] FMT_BITS = {Rich.BOLD, Rich.ITALIC, Rich.UNDERLINE, Rich.STRIKE};
    private final TextView[] fmtText = new TextView[4];
    private final FrameLayout[] fmtBox = new FrameLayout[4];
    private GradientDrawable fmtOn, fmtOff;
    private final Runnable onSelChanged = new Runnable() {
        @Override public void run() { updateFormatButtons(); }
    };

    private final Runnable pushSnapshot = new Runnable() {
        @Override public void run() { snapshot(); }
    };
    private final Runnable saveTask = new Runnable() {
        @Override public void run() { saveNow(); }
    };

    private static class Snap {
        String text, spans;
        int selStart, selEnd;
        Snap(String t, String s, int a, int b) { text = t; spans = s; selStart = a; selEnd = b; }
    }

    public EditorScreen(Context c, long existingId, long folderId, Host host) {
        super(c);
        this.host = host;
        this.folderId = folderId;
        this.isNew = existingId <= 0;
        this.noteId = isNew ? Db.get(c).insertNote(folderId) : existingId;
        setWillNotDraw(false);
        setBackgroundColor(Ui.BG);

        col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        addView(col, new FrameLayout.LayoutParams(-1, -1));

        // ---------- 顶栏：返回 / 撤回 / 恢复 ----------
        FrameLayout bar = new FrameLayout(c);
        col.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dpi(58)));

        FrameLayout back = glassBtn(c, Ic.BACK, new Runnable() {
            @Override public void run() { EditorScreen.this.host.closeEditor(EditorScreen.this); }
        });
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(Ui.dpi(40), Ui.dpi(40),
                Gravity.START | Gravity.CENTER_VERTICAL);
        blp.leftMargin = Ui.dpi(12);
        bar.addView(back, blp);

        LinearLayout right = new LinearLayout(c);
        right.setOrientation(LinearLayout.HORIZONTAL);
        right.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(-2, -1, Gravity.END | Gravity.CENTER_VERTICAL);
        rlp.rightMargin = Ui.dpi(12);
        bar.addView(right, rlp);

        undoBtn = glassBtn(c, Ic.UNDO, new Runnable() {
            @Override public void run() { undo(); }
        });
        redoBtn = glassBtn(c, Ic.REDO, new Runnable() {
            @Override public void run() { redo(); }
        });
        right.addView(undoBtn, new LinearLayout.LayoutParams(Ui.dpi(40), Ui.dpi(40)));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(Ui.dpi(40), Ui.dpi(40));
        gap.leftMargin = Ui.dpi(8);
        right.addView(redoBtn, gap);
        moreBtn = glassBtn(c, Ic.MORE, new Runnable() {
            @Override public void run() { EditorScreen.this.host.showMore(EditorScreen.this); }
        });
        LinearLayout.LayoutParams mgap = new LinearLayout.LayoutParams(Ui.dpi(40), Ui.dpi(40));
        mgap.leftMargin = Ui.dpi(8);
        right.addView(moreBtn, mgap);
        moreBtn.setVisibility(GONE);
        undoBtn.setAlpha(0f);
        redoBtn.setAlpha(0f);

        // ---------- 时间戳 / 字数 / 定位（同一行） ----------
        headerRow = new LinearLayout(c);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(Ui.dpi(22), 0, Ui.dpi(18), Ui.dpi(8));
        col.addView(headerRow, new LinearLayout.LayoutParams(-1, -2));

        locRow = new LinearLayout(c);
        locRow.setOrientation(LinearLayout.HORIZONTAL);
        locRow.setGravity(Gravity.CENTER_VERTICAL);
        locRow.setPadding(Ui.dpi(26), Ui.dpi(2), Ui.dpi(18), Ui.dpi(9));   // 紧凑 + 左侧留白
        locRow.setVisibility(GONE);
        col.addView(locRow, new LinearLayout.LayoutParams(-1, -2));

        View hline = new View(c);
        hline.setBackgroundColor(Ui.alpha(Ui.TEXT, 0.08f));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, Math.max(1, Ui.dpi(0.75f)));
        hlp.leftMargin = Ui.dpi(22);
        hlp.rightMargin = Ui.dpi(22);
        col.addView(hline, hlp);

        status = new TextView(c);
        status.setTextSize(13.5f);
        status.setTextColor(Ui.TEXT_2);
        status.setTypeface(Ui.reg());
        status.setIncludeFontPadding(false);
        status.setSingleLine(true);
        status.setEllipsize(android.text.TextUtils.TruncateAt.END);
        headerRow.addView(status, new LinearLayout.LayoutParams(0, -2, 1f));

        folderChip = new LinearLayout(c);
        folderChip.setOrientation(LinearLayout.HORIZONTAL);
        folderChip.setGravity(Gravity.CENTER_VERTICAL);
        folderChip.setVisibility(GONE);
        LinearLayout.LayoutParams fcp = new LinearLayout.LayoutParams(-2, -2);
        fcp.leftMargin = Ui.dpi(8);
        headerRow.addView(folderChip, fcp);
        folderChip.addView(new Ic(c, Ic.FOLDER, Ui.alpha(Ui.TEXT, 0.45f)),
                new LinearLayout.LayoutParams(Ui.dpi(11), Ui.dpi(11)));
        folderName = new TextView(c);
        folderName.setTextSize(11f);
        folderName.setTextColor(Ui.TEXT_3);
        folderName.setMaxLines(1);
        folderName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        folderName.setMaxWidth(Ui.dpi(90));
        folderName.setIncludeFontPadding(false);
        LinearLayout.LayoutParams fnp = new LinearLayout.LayoutParams(-2, -2);
        fnp.leftMargin = Ui.dpi(4);
        folderChip.addView(folderName, fnp);

        locChip = new LinearLayout(c);
        locChip.setOrientation(LinearLayout.HORIZONTAL);
        locChip.setGravity(Gravity.CENTER_VERTICAL);
        locChip.setPadding(Ui.dpi(12), Ui.dpi(3), Ui.dpi(13), Ui.dpi(3));   // 1.25 倍后的内边距
        locChip.setBackground(new Segmented.FrostDrawable(-1f));
        locChip.addView(new Ic(c, Ic.PIN, Ui.alpha(Ui.TEXT, 0.70f)),
                new LinearLayout.LayoutParams(Ui.dpi(13), Ui.dpi(13)));

        locName = new TextView(c);
        locName.setTextSize(13f);
        locName.setTextColor(Ui.TEXT_2);
        locName.setMaxLines(1);
        locName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        locName.setMaxWidth(Ui.dpi(220));
        locName.setIncludeFontPadding(false);
        LinearLayout.LayoutParams lnlp = new LinearLayout.LayoutParams(-2, -2);
        lnlp.leftMargin = Ui.dpi(4);
        locChip.addView(locName, lnlp);

        locRow.addView(locChip, new LinearLayout.LayoutParams(-2, -2));

        // 平时只显示"省 市"，点开才看完整地址，删除也放在里面
        Ui.tap(locChip, new Runnable() {
            @Override public void run() {
                if (locFull == null || locFull.isEmpty()) return;
                Sheet.centerTitle = true;
                Sheet.menu(EditorScreen.this, locFull, new String[]{Str.s("delete_loc")}, new Runnable[]{
                        new Runnable() {
                            @Override public void run() {
                                Db.get(getContext()).clearLocation(noteId);
                                locFull = "";
                                locName.setText("");
                                locRow.setVisibility(GONE);   // 去掉定位后排版恢复原样
                            }
                        }
                }, 0);
            }
        });

        // ---------- 正文 ----------
        FrameLayout textBox = new FrameLayout(c);
        col.addView(textBox, new LinearLayout.LayoutParams(-1, 0, 1f));

        input = new RichEdit(c);
        input.setBackground(null);
        input.setTextSize(17f);
        input.setTextColor(Ui.TEXT);
        android.text.SpannableString hint = new android.text.SpannableString(Str.s("editor_hint"));
        hint.setSpan(new android.text.style.AbsoluteSizeSpan(Ui.dpi(15)), 0, hint.length(),
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        input.setHint(hint);
        input.setHintTextColor(Ui.alpha(Ui.TEXT, 0.30f));
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setPadding(Ui.dpi(22), Ui.dpi(4), Ui.dpi(22), Ui.dpi(10));
        input.setLineSpacing(Ui.dp(4f), 1.06f);
        input.setLetterSpacing(-0.022f);
        input.getPaint().setSubpixelText(true);
        input.getPaint().setLinearText(false);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        input.setShowSoftInputOnFocus(false);   // 键盘由"轻点"回调显式唤起
        textBox.addView(input, new FrameLayout.LayoutParams(-1, -1));
        input.onSel = onSelChanged;
        input.onImageTap = new RichEdit.ImgTap() {
            @Override public void onImage(String path) {
                EditorScreen.this.host.previewImage(path);
            }
        };
        input.onTap = new Runnable() {
            @Override public void run() {
                if (!input.hasFocus()) input.requestFocus();
                input.setCursorVisible(true);
                showIme();
            }
        };

        ScrollBar sbar = new ScrollBar(c);
        FrameLayout.LayoutParams sbp = new FrameLayout.LayoutParams(Ui.dpi(6), -1, Gravity.END);
        sbp.rightMargin = Ui.dpi(3);
        textBox.addView(sbar, sbp);
        sbar.attach(input);

        // ---------- 输入法工具条 ----------
        quickBar = new LinearLayout(c);
        quickBar.setOrientation(LinearLayout.VERTICAL);
        quickBar.setVisibility(GONE);
        col.addView(quickBar, new LinearLayout.LayoutParams(-1, -2));

        View sep = new View(c);
        sep.setBackgroundColor(Ui.alpha(Ui.TEXT, 0.10f));
        quickBar.addView(sep, new LinearLayout.LayoutParams(-1, Math.max(1, Ui.dpi(1))));

        formatRow = new LinearLayout(c);
        formatRow.setOrientation(LinearLayout.HORIZONTAL);
        formatRow.setPadding(Ui.dpi(12), Ui.dpi(6), Ui.dpi(12), 0);
        formatRow.setVisibility(GONE);
        quickBar.addView(formatRow, new LinearLayout.LayoutParams(-1, -2));

        fmtOff = new GradientDrawable();
        fmtOff.setCornerRadius(Ui.dp(10));
        fmtOff.setColor(0x00000000);
        fmtOn = new GradientDrawable();
        fmtOn.setCornerRadius(Ui.dp(10));
        fmtOn.setColor(Ui.alpha(Ui.ACCENT, 0.18f));
        fmtOn.setStroke(Math.max(1, Ui.dpi(0.75f)), Ui.alpha(Ui.ACCENT, 0.35f));

        for (int i = 0; i < 4; i++) {
            formatRow.addView(fmtBtn(c, i), new LinearLayout.LayoutParams(0, Ui.dpi(40), 1f));
        }

        LinearLayout tools = new LinearLayout(c);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setPadding(Ui.dpi(8), Ui.dpi(4), Ui.dpi(8), Ui.dpi(8));
        quickBar.addView(tools, new LinearLayout.LayoutParams(-1, -2));

        TextView aBtn = new TextView(c);
        aBtn.setText("A");
        aBtn.setTextSize(19f);
        aBtn.setTypeface(Ui.serifMed());
        aBtn.setTextColor(Ui.TEXT);
        aBtn.setGravity(Gravity.CENTER);
        tools.addView(toolSlot(c, aBtn, new Runnable() {
            @Override public void run() { toggleFormatMode(); }
        }), new LinearLayout.LayoutParams(0, Ui.dpi(44), 1f));

        tools.addView(toolSlot(c, new Ic(c, Ic.NUM, Ui.TEXT_2), new Runnable() {
            @Override public void run() { toggleNumbering(); }
        }), new LinearLayout.LayoutParams(0, Ui.dpi(44), 1f));

        tools.addView(toolSlot(c, new Ic(c, Ic.BOX, Ui.TEXT_2), new Runnable() {
            @Override public void run() { toggleCheckbox(); }
        }), new LinearLayout.LayoutParams(0, Ui.dpi(44), 1f));

        tools.addView(toolSlot(c, new Ic(c, Ic.IMAGE, Ui.TEXT_2), new Runnable() {
            @Override public void run() { EditorScreen.this.host.pickImages(EditorScreen.this); }
        }), new LinearLayout.LayoutParams(0, Ui.dpi(44), 1f));

        tools.addView(toolSlot(c, new Ic(c, Ic.PIN, Ui.TEXT_2), new Runnable() {
            @Override public void run() { EditorScreen.this.host.pickLocation(EditorScreen.this); }
        }), new LinearLayout.LayoutParams(0, Ui.dpi(44), 1f));

        // ---------- 载入 ----------
        Note n = Db.get(c).note(noteId);
        if (n != null) {
            imgPaths.addAll(Db.get(c).atts(noteId));
            lastAttCount = imgPaths.size();
            updatedAt = n.updated;
            if (n.folderName != null && !n.folderName.isEmpty()) {
                folderName.setText(n.folderName);
                folderChip.setVisibility(VISIBLE);
            }
            if (n.hasLocation()) {
                locFull = n.locName;
                locName.setText(Ui.cityOnly(n.locName));
                locRow.setVisibility(VISIBLE);
            }
            // 正文延后一帧再填充：编辑页先出现，长文本不会卡住入场
            final Note nn = n;
            post(new Runnable() {
                @Override public void run() {
                    applying = true;
                    try {
                        input.setText(nn.content == null ? "" : nn.content);
                        Editable e = input.getText();
                        if (nn.spans != null && !nn.spans.isEmpty()) Rich.restore(e, nn.spans);
                        attachInlineSpans();
                        if (countPlaceholders() == 0 && !imgPaths.isEmpty()) {
                            StringBuilder rb = new StringBuilder();
                            for (int i = 0; i < imgPaths.size(); i++) rb.append('\n').append(PH).append('\n');
                            e.append(rb);
                            attachInlineSpans();
                        }
                        input.setSelection(0);
                        originalText = e.toString();
                        originalSpans = Rich.serialize(e);
                    } catch (Throwable ignored) { }
                    applying = false;
                    refreshChecks();
                    applyParaSpacing();
                    refreshStatus();
                    history.clear();
                    history.add(currentSnap());
                    histIndex = 0;
                    refreshUndo();
                    fitText();
                    input.scrollTo(0, 0);
                }
            });
        } else {
            updatedAt = System.currentTimeMillis();
            refreshStatus();
            history.add(currentSnap());
            histIndex = 0;
            refreshUndo();
        }

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (applying) return;
                try {
                    if (count > before) {
                        int a = start + before;
                        int b = start + count;
                        if (pendingMask != 0) applyMask(a, b, pendingMask);
                        CharSequence ins = s.subSequence(start, b);
                        for (int i = ins.length() - 1; i >= 0; i--) {
                            if (ins.charAt(i) == '\n') {
                                final int pos = start + i + 1;
                                handler.post(new Runnable() {
                                    @Override public void run() { continueList(pos); }
                                });
                                break;
                            }
                        }
                    }
                    if (!insertingImage && countPlaceholders() != imgPaths.size()) {
                        rebuildPathsFromSpans();
                        attachInlineSpans();
                    }
                    refreshStatus();
                    updateFormatButtons();
                } catch (Throwable ignored) { }
            }
            @Override public void afterTextChanged(Editable e) {
                if (applying) return;
                dirtyOnce = true;
                handler.removeCallbacks(saveTask);
                handler.postDelayed(saveTask, 420);
                handler.removeCallbacks(pushSnapshot);
                handler.postDelayed(pushSnapshot, 520);
                showHistoryButtons();
            }
        });

        input.setOnTouchListener(new View.OnTouchListener() {
            private float y0;
            private boolean dragging;

            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: {
                        y0 = e.getY();
                        dragging = false;
                        int off = offsetAt(e.getX(), e.getY());
                        if (off >= 0 && isCheckboxAt(off)) {
                            toggleCheckboxChar(off);
                            return true;
                        }
                        return false;
                    }
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(e.getY() - y0) > Ui.dp(10)) dragging = true;
                        return false;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (dragging) {
                            dragging = false;
                            // 滑动结束后再压一次输入法，避免滑动误唤起键盘
                            postDelayed(new Runnable() {
                                @Override public void run() { hideIme(); }
                            }, 60);
                        }
                        return false;
                }
                return false;
            }
        });

        if (isNew) {
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    if (closed) return;
                    input.requestFocus();
                    showIme();
                }
            }, 380);
        }
    }

    // ==================== 顶栏按钮 ====================

    private FrameLayout glassBtn(Context c, int kind, final Runnable action) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(new Segmented.FrostDrawable(-1f));   // -1 = 正圆
        f.addView(new Ic(c, kind, Ui.TEXT),
                new FrameLayout.LayoutParams(Ui.dpi(19), Ui.dpi(19), Gravity.CENTER));
        Ui.tap(f, action);
        return f;
    }

    private View toolSlot(Context c, View content, final Runnable action) {
        FrameLayout f = new FrameLayout(c);
        f.addView(content, new FrameLayout.LayoutParams(Ui.dpi(22), Ui.dpi(22), Gravity.CENTER));
        Ui.tap(f, action);
        return f;
    }

    private View fmtBtn(Context c, final int idx) {
        final int bit = FMT_BITS[idx];
        FrameLayout f = new FrameLayout(c);
        TextView t = new TextView(c);
        t.setText(idx == 0 ? "B" : idx == 1 ? "I" : idx == 2 ? "U" : "S");
        t.setTextSize(17f);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        if (idx == 0) {
            t.setTypeface(Ui.med());
        } else if (idx == 1) {
            t.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        } else if (idx == 2) {
            t.setTypeface(Ui.reg());
            t.setPaintFlags(t.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        } else {
            t.setTypeface(Ui.reg());
            t.setPaintFlags(t.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        t.setTextColor(Ui.TEXT_2);
        f.addView(t, new FrameLayout.LayoutParams(Ui.dpi(24), Ui.dpi(24), Gravity.CENTER));
        f.setBackground(fmtOff);
        fmtText[idx] = t;
        fmtBox[idx] = f;
        Ui.tap(f, new Runnable() {
            @Override public void run() { toggleFormat(bit); }
        });
        return f;
    }

    private void updateFormatButtons() {
        refreshChecks();
        if (fmtBox[0] == null) return;
        try {
            int a = Math.max(0, input.getSelectionStart());
            int b = Math.max(0, input.getSelectionEnd());
            if (a > b) { int t = a; a = b; b = t; }
            boolean hasSel = b > a;
            int[] masks = hasSel ? Rich.masks(input.getText()) : null;
            for (int i = 0; i < 4; i++) {
                boolean on;
                if (hasSel) {
                    int bit = FMT_BITS[i];
                    on = true;
                    for (int p = a; p < b && masks != null && p < masks.length; p++) {
                        if ((masks[p] & bit) == 0) { on = false; break; }
                    }
                } else {
                    on = (pendingMask & FMT_BITS[i]) != 0;
                }
                fmtBox[i].setBackground(on ? fmtOn : fmtOff);
                fmtText[i].setTextColor(on ? Ui.ACCENT : Ui.TEXT_2);
            }
        } catch (Throwable ignored) { }
    }

    // ==================== 工具条逻辑 ====================

    private void toggleFormatMode() {
        formatMode = !formatMode;
        if (!formatMode) pendingMask = 0;   // 收起面板时清掉"待定格式"，避免残留
        formatRow.setVisibility(formatMode ? VISIBLE : GONE);
        updateFormatButtons();
        if (!imeShown) showIme();   // 不再收起键盘
        updateBar();
    }

    private void updateBar() {
        quickBar.setVisibility((imeShown || formatMode) ? VISIBLE : GONE);
        boolean typing = imeShown || formatMode || dirtyOnce;
        undoBtn.setVisibility(typing ? VISIBLE : INVISIBLE);
        redoBtn.setVisibility(typing ? VISIBLE : INVISIBLE);
        moreBtn.setVisibility(typing ? GONE : VISIBLE);
    }

    private void hideIme() {
        try {
            InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        } catch (Throwable ignored) { }
    }

    private void showIme() {
        input.requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
    }

    private void toggleFormat(int bit) {
        int a = input.getSelectionStart();
        int b = input.getSelectionEnd();
        Editable e = input.getText();
        if (a > b) { int t = a; a = b; b = t; }
        if (a == b) {
            pendingMask ^= bit;
        } else {
            Rich.toggle(e, a, b, bit);
            pendingMask = 0;
            handler.removeCallbacks(saveTask);
            handler.postDelayed(saveTask, 200);
        }
        updateFormatButtons();
        if (!imeShown) showIme();
    }

    private void applyMask(int a, int b, int mask) {
        Editable e = input.getText();
        int n = e.length();
        a = Math.max(0, Math.min(n, a));
        b = Math.max(a, Math.min(n, b));
        if (a >= b) return;
        if ((mask & Rich.BOLD) != 0)
            e.setSpan(new StyleSpan(Typeface.BOLD), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if ((mask & Rich.ITALIC) != 0)
            e.setSpan(new StyleSpan(Typeface.ITALIC), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if ((mask & Rich.UNDERLINE) != 0)
            e.setSpan(new UnderlineSpan(), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if ((mask & Rich.STRIKE) != 0)
            e.setSpan(new StrikethroughSpan(), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // ==================== 序号 / 方框 ====================

    private void toggleNumbering() {
        Editable e = input.getText();
        int pos = input.getSelectionStart();
        int ls = lineStart(e, pos);
        String line = e.subSequence(ls, e.length()).toString();
        int nl = line.indexOf('\n');
        if (nl >= 0) line = line.substring(0, nl);
        if (line.matches("^\\d+\\.\\s.*") || line.matches("^\\d+\\.$")) {
            int dot = line.indexOf('.');
            int cut = dot + 1;
            if (line.length() > cut && line.charAt(cut) == ' ') cut++;
            e.delete(ls, ls + cut);
            return;
        }
        int num = 1;
        if (ls > 0) {
            int prevStart = lineStart(e, ls - 1);
            String prev = e.subSequence(prevStart, ls).toString().trim();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d+)\\.").matcher(prev);
            if (m.find()) {
                try { num = Integer.parseInt(m.group(1)) + 1; } catch (Exception ignored) { }
            }
        }
        e.insert(ls, num + ". ");
    }

    private void toggleCheckbox() {
        Editable e = input.getText();
        int pos = input.getSelectionStart();
        int ls = lineStart(e, pos);
        if (ls < e.length() && (e.charAt(ls) == '☐' || e.charAt(ls) == '☑')) {
            int cut = 1;
            if (ls + 1 < e.length() && e.charAt(ls + 1) == ' ') cut = 2;
            e.delete(ls, ls + cut);
            return;
        }
        e.insert(ls, "☐ ");
    }

    /** ☑ 之后整行加删除线并变浅；☐ 则清除 */
    static class CheckStrike extends android.text.style.CharacterStyle {
        @Override public void updateDrawState(android.text.TextPaint tp) {
            tp.setStrikeThruText(true);
            tp.setColor(Ui.alpha(Ui.TEXT, 0.42f));
        }
    }

    private boolean calmCheck;

    private void refreshChecks() {
        if (calmCheck) return;
        Editable e = input.getText();
        if (e == null) return;
        calmCheck = true;
        try {
            int i = 0;
            while (i < e.length()) {
                int ls = i;
                int le = e.toString().indexOf('\n', ls);
                if (le < 0) le = e.length();
                if (ls < e.length()) {
                    char c0 = e.charAt(ls);
                    int from = Math.min(le, ls + 2);   // 跳过 "☑ "
                    CheckStrike[] old = e.getSpans(ls, le, CheckStrike.class);
                    for (CheckStrike s : old) e.removeSpan(s);
                    if (c0 == '☑' && from < le) {
                        e.setSpan(new CheckStrike(), from, le, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                i = le + 1;
            }
        } catch (Throwable ignored) { }
        calmCheck = false;
    }

    private void toggleCheckboxChar(int off) {
        Editable e = input.getText();
        int ls = lineStart(e, off);
        if (ls >= e.length()) return;
        char c = e.charAt(ls);
        if (c == '☐') e.replace(ls, ls + 1, "☑");
        else if (c == '☑') e.replace(ls, ls + 1, "☐");
    }

    private boolean isCheckboxAt(int off) {
        Editable e = input.getText();
        int ls = lineStart(e, off);
        if (ls >= e.length()) return false;
        char c = e.charAt(ls);
        if (c != '☐' && c != '☑') return false;
        return off <= ls + 1;
    }

    private int lineStart(CharSequence t, int pos) {
        int p = Math.max(0, Math.min(t.length(), pos));
        int i = p - 1;
        while (i >= 0 && t.charAt(i) != '\n') i--;
        return i + 1;
    }

    private int offsetAt(float x, float y) {
        try {
            return input.getOffsetForPosition(x, y);
        } catch (Exception e) {
            return -1;
        }
    }

    private void continueList(int pos) {
        try {
            continueListInner(pos);
        } catch (Throwable ignored) { }
    }

    /** 回车后自动续写序号 / 复选框 */
    private void continueListInner(int pos) {
        if (closed) return;
        Editable e = input.getText();
        if (pos <= 0 || pos > e.length()) return;
        int nl = pos - 1;
        if (nl >= e.length() || e.charAt(nl) != '\n') return;
        int ls = lineStart(e, nl - 1);
        String prev = e.subSequence(ls, nl).toString();
        String add = null;
        String trimmed = prev.trim();
        if (trimmed.matches("\\d+\\.") || trimmed.equals("☐") || trimmed.equals("☑")) {
            return;   // 空列表项再回车：结束列表，不再续号
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d+)\\.\\s").matcher(prev);
        if (m.find()) {
            try { add = (Integer.parseInt(m.group(1)) + 1) + ". "; } catch (Exception ignored) { }
        } else if (prev.startsWith("☐ ") || prev.startsWith("☑ ")) {
            add = "☐ ";
        }
        if (add == null) return;
        if (input.getSelectionStart() != pos) return;
        e.insert(pos, add);
    }

    // ==================== 内嵌图片 ====================

    private int countPlaceholders() {
        Editable e = input.getText();
        int n = 0;
        for (int i = 0; i < e.length(); i++) if (e.charAt(i) == PH) n++;
        return n;
    }

    public void addImageFiles(List<String> paths) {
        if (paths == null || paths.isEmpty()) return;
        try {
            Editable e = input.getText();
            int at = Math.max(0, input.getSelectionStart());
            int before = 0;
            for (int i = 0; i < at && i < e.length(); i++) if (e.charAt(i) == PH) before++;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paths.size(); i++) sb.append('\n').append(PH).append('\n');
            insertingImage = true;
            try {
                e.insert(at, sb.toString());
            } finally {
                insertingImage = false;
            }
            if (before < 0) before = 0;
            if (before > imgPaths.size()) before = imgPaths.size();
            imgPaths.addAll(before, paths);
            attachInlineSpans();
            input.setSelection(at + sb.length());
            // 自检：占位符上是否真的挂上了图片
            int check = Math.max(0, at);
            if (check < input.getText().length()
                    && input.getText().charAt(check) == PH
                    && input.getText().getSpans(check, check + 1, InlineImageSpan.class).length == 0) {
                android.widget.Toast.makeText(getContext(), Str.s("image_show_failed"), android.widget.Toast.LENGTH_SHORT).show();
            }
            handler.removeCallbacks(saveTask);
            handler.postDelayed(saveTask, 200);
        } catch (Throwable ignored) { }
    }

    private void rebuildPathsFromSpans() {
        try {
            Editable e = input.getText();
            List<String> np = new ArrayList<String>();
            for (int i = 0; i < e.length(); i++) {
                if (e.charAt(i) != PH) continue;
                InlineImageSpan[] sp = e.getSpans(i, i + 1, InlineImageSpan.class);
                if (sp.length > 0) np.add(sp[0].path);
            }
            imgPaths.clear();
            imgPaths.addAll(np);
        } catch (Throwable ignored) { }
    }

    private void attachInlineSpans() {
        try {
            Editable e = input.getText();
            for (InlineImageSpan s : e.getSpans(0, e.length(), InlineImageSpan.class)) e.removeSpan(s);
            int idx = 0;
            for (int i = 0; i < e.length(); i++) {
                if (e.charAt(i) != PH) continue;
                if (idx >= imgPaths.size()) break;
                ImageSpan span = buildImageSpan(imgPaths.get(idx));
                if (span != null) e.setSpan(span, i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                idx++;
            }
        } catch (Throwable ignored) { }
    }

    private ImageSpan buildImageSpan(String path) {
        int avail = Math.max(Ui.dpi(200),
                getWidth() - input.getPaddingLeft() - input.getPaddingRight());
        try {
            Bitmap bmp = Pics.decodeNow(path, Math.min(720, avail));
            if (bmp != null) {
                int maxH = Ui.dpi(240);
                float k = Math.min(avail / (float) bmp.getWidth(), maxH / (float) bmp.getHeight());
                if (k > 1f) k = 1f;
                int w = Math.max(1, Math.round(bmp.getWidth() * k));
                int h = Math.max(1, Math.round(bmp.getHeight() * k));
                InlineImage d = new InlineImage(bmp, w, h, Ui.dp(14));
                d.setBounds(0, 0, w, h);   // ImageSpan 靠 bounds 取尺寸，不设就是 0×0
                return new InlineImageSpan(d, path);
            }
        } catch (Throwable ignored) { }
        int w = Math.max(1, avail);
        int h = Ui.dpi(120);
        FallbackImage fb = new FallbackImage(w, h, Ui.dp(14));
        fb.setBounds(0, 0, w, h);
        return new InlineImageSpan(fb, path);
    }
    static class InlineImageSpan extends ImageSpan {
        final String path;
        InlineImageSpan(Drawable d, String path) { super(d); this.path = path; }
    }

    /** 圆角插图 */
    static class InlineImage extends Drawable {
        private final Bitmap bmp;
        private final int w, h;
        private final float radius;
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private final Path clip = new Path();

        InlineImage(Bitmap b, int w, int h, float radius) {
            this.bmp = b; this.w = w; this.h = h; this.radius = radius;
        }

        @Override public int getIntrinsicWidth() { return w; }
        @Override public int getIntrinsicHeight() { return h; }

        @Override public void draw(Canvas c) {
            box.set(getBounds());
            clip.reset();
            clip.addRoundRect(box, radius, radius, Path.Direction.CW);
            c.save();
            c.clipPath(clip);
            c.drawBitmap(bmp, null, box, paint);
            c.restore();
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter cf) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    private void syncAtts() {
        int n = countPlaceholders();
        while (imgPaths.size() > n) imgPaths.remove(imgPaths.size() - 1);
        if (imgPaths.size() == lastAttCount) return;
        lastAttCount = imgPaths.size();
        Db db = Db.get(getContext());
        db.clearAtts(noteId);
        for (String p : imgPaths) db.addAtt(noteId, p);
    }

    // ==================== 撤回 / 恢复 ====================

    private Snap currentSnap() {
        Editable e = input.getText();
        int a = Math.max(0, input.getSelectionStart());
        int b = Math.max(0, input.getSelectionEnd());
        return new Snap(e.toString(), Rich.serialize(e), a, b);
    }

    private void snapshot() {
        try {
            snapshotInner();
        } catch (Throwable ignored) { }
    }

    private void snapshotInner() {
        if (closed || applying) return;
        Snap s = currentSnap();
        if (histIndex >= 0 && histIndex < history.size()) {
            Snap top = history.get(histIndex);
            if (top.text.equals(s.text) && top.spans.equals(s.spans)) return;
        }
        while (history.size() > histIndex + 1) history.remove(history.size() - 1);
        history.add(s);
        if (history.size() > MAX_HISTORY) history.remove(0);
        histIndex = history.size() - 1;
        refreshUndo();
    }

    private void applySnap(Snap s) {
        applying = true;
        try {
            if (s == null) return;
            Editable e = input.getText();
            // 只清我们自己加的样式 span；clearSpans() 会连 TextView 内部的
            // ChangeWatcher / IME composing span 一起删掉，必然导致编辑器崩溃
            Rich.clear(e);
            e.replace(0, e.length(), s.text);
            Rich.restore(e, s.spans);
            attachInlineSpans();
            int len = e.length();
            int a = Math.max(0, Math.min(s.selStart, len));
            int b = Math.max(0, Math.min(s.selEnd, len));
            if (a > b) { int t = a; a = b; b = t; }
            input.setSelection(a, b);
            refreshStatus();
            refreshUndo();
        } catch (Throwable ignored) {
        } finally {
            applying = false;
        }
    }

    private void undo() {
        try {
            if (histIndex <= 0 || histIndex > history.size() - 1) return;
            histIndex--;
            applySnap(history.get(histIndex));
            handler.removeCallbacks(saveTask);
            handler.postDelayed(saveTask, 300);
        } catch (Throwable ignored) { }
    }

    private void redo() {
        try {
            if (histIndex < 0 || histIndex >= history.size() - 1) return;
            histIndex++;
            applySnap(history.get(histIndex));
            handler.removeCallbacks(saveTask);
            handler.postDelayed(saveTask, 300);
        } catch (Throwable ignored) { }
    }

    private void refreshUndo() {
        boolean canUndo = histIndex > 0;
        boolean canRedo = histIndex < history.size() - 1;
        float dim = dirtyOnce ? 0.22f : 0f;
        undoBtn.animate().cancel();
        redoBtn.animate().cancel();
        undoBtn.setAlpha(canUndo ? 1f : dim);
        redoBtn.setAlpha(canRedo ? 1f : dim);
    }

    private void showHistoryButtons() {
        if (!dirtyOnce) return;
        undoBtn.animate().alpha(1f).setDuration(200).start();
        redoBtn.animate().alpha(history.size() > 1 && histIndex < history.size() - 1 ? 1f : 0.22f)
                .setDuration(200).start();
    }

    // ==================== 状态行 ====================

    private void refreshStatus() {
        status.setText(Ui.statusLine(updatedAt, Ui.charCount(input.getText())));
    }

    // ==================== 边距与保存 ====================

    /** 键盘弹起/收起时底部留白做动画，文字随之上移而不是瞬跳 */
    public void setInsets(int top, int bottom, int ime) {
        boolean wasIme = imeShown;
        imeShown = ime > Ui.dpi(80);
        if (wasIme && !imeShown && !formatMode) input.setCursorVisible(false);
        if (!wasIme && imeShown) input.setCursorVisible(true);
        padTop = top;
        int target = Math.max(bottom, ime);
        int cur = col.getPaddingBottom();
        if (cur == 0 && !imeShown) {
            col.setPadding(0, padTop, 0, target);
        } else if (cur != target) {
            animatePad(target);
        } else {
            col.setPadding(0, padTop, 0, cur);
        }
        updateBar();
        updateFormatButtons();
    }

    private void animatePad(final int to) {
        if (padAnim != null) padAnim.cancel();
        final int from = col.getPaddingBottom();
        ValueAnimator a = ValueAnimator.ofInt(from, to);
        a.setDuration(200);
        a.setInterpolator(Ui.EASE);
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator anim) {
                col.setPadding(0, padTop, 0, (Integer) anim.getAnimatedValue());
            }
        });
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                padAnim = null;
                ensureCursorVisibleAnimated();
            }
        });
        padAnim = a;
        a.start();
    }

    /** 键盘动画结束后，只在需要时把光标行平滑带回可视区，避免文字突兀跳动 */
    private void ensureCursorVisibleAnimated() {
        try {
            android.text.Layout l = input.getLayout();
            if (l == null) return;
            int pos = Math.max(0, input.getSelectionStart());
            if (pos > l.getText().length()) pos = l.getText().length();
            int line = l.getLineForOffset(pos);
            int top = l.getLineTop(line);
            int bottom = l.getLineBottom(line);
            int viewH = input.getHeight() - input.getPaddingTop() - input.getPaddingBottom();
            if (viewH <= 0) return;
            int sy = input.getScrollY();
            int margin = Ui.dpi(10);
            int target = sy;
            if (bottom - sy > viewH - margin) target = bottom - viewH + margin;
            else if (top - sy < margin) target = Math.max(0, top - margin);
            if (target != sy) input.smoothScrollBy(target - sy);
        } catch (Throwable ignored) { }
    }

    public void saveNow() {
        if (closed) return;
        try {
            Editable e = input.getText();
            syncAtts();
            Db.get(getContext()).saveNoteNoTouch(noteId, e.toString(), Rich.serialize(e));
        } catch (Throwable ignored) { }
    }

    public void applyLocation(String name, double lat, double lng) {
        Db.get(getContext()).setLocation(noteId, name, lat, lng);
        locFull = name;
        locName.setText(Ui.cityOnly(name));
        locRow.setVisibility(VISIBLE);
    }

    /** 切到后台时调用：收起键盘、放弃焦点，避免回到前台自动弹输入法 */
    public void putAway() {
        try {
            hideIme();
            input.clearFocus();
            input.setCursorVisible(false);
        } catch (Throwable ignored) { }
    }

    public boolean close() {
        closed = true;
        try {
            hideIme();
            input.clearFocus();
        } catch (Throwable ignored) { }
        handler.removeCallbacks(saveTask);
        handler.removeCallbacks(pushSnapshot);
        try {
            Db db = Db.get(getContext());
            Editable e = input.getText();
            syncAtts();
            String nowText = e.toString();
            String nowSpans = Rich.serialize(e);
            boolean empty = nowText.trim().isEmpty()
                    && imgPaths.isEmpty()
                    && (db.note(noteId) == null || !db.note(noteId).hasLocation());
            if (empty) {
                db.purge(noteId);
                return true;
            }
            boolean changed = !nowText.equals(originalText) || !nowSpans.equals(originalSpans);
            if (changed) db.saveNote(noteId, nowText, nowSpans);      // 真正改过 -> 刷新时间戳
            else db.saveNoteNoTouch(noteId, nowText, nowSpans);       // 只是查看 -> 不动时间
            return changed;
        } catch (Throwable ignored) { }
        return false;
    }

    private int lastWidth;

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        post(new Runnable() {
            @Override public void run() { fitText(); }
        });
        input.post(new Runnable() {
            @Override public void run() { fitText(); }
        });
        super.onSizeChanged(w, h, ow, oh);
        if (w > 0 && w != lastWidth) {
            lastWidth = w;
            post(new Runnable() {
                @Override public void run() { attachInlineSpans(); }
            });
        }
    }

    /** 小字号时：正文区正好 20 字一行、25 行一屏 */
    /** 段落之间的空白行加高，形成明显分界（行间距不变） */
    static class ParaSpace implements android.text.style.LineHeightSpan {
        private final int extra;
        ParaSpace(int extra) { this.extra = extra; }
        @Override public void chooseHeight(CharSequence t, int start, int end,
                                           int spanstartv, int v, Paint.FontMetricsInt fm) {
            fm.ascent -= extra;
            fm.descent += extra;
            fm.top -= extra;
            fm.bottom += extra;
        }
    }

    private void applyParaSpacing() {
        try {
            Editable e = input.getText();
            if (e == null) return;
            ParaSpace[] old = e.getSpans(0, e.length(), ParaSpace.class);
            for (ParaSpace o : old) e.removeSpan(o);
            int extra = Ui.dpi(10);
            for (int i = 1; i < e.length(); i++) {
                if (e.charAt(i) == '\n' && e.charAt(i - 1) == '\n') {
                    e.setSpan(new ParaSpace(extra), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        } catch (Throwable ignored) { }
    }

    private void fitText() {
        try {
            if (Settings.fontLevel(getContext()) != 0) return;
            int w = input.getWidth() - input.getPaddingLeft() - input.getPaddingRight();
            int h = input.getHeight() - input.getPaddingTop() - input.getPaddingBottom();
            if (w <= 0 || h <= 0) return;
            // 同时满足：每行 20 字、每屏 25 行（行高约字号 1.18 倍）
            float byWidth = w / 20f;
            float byHeight = h / 25f / 1.18f;
            float tsPx = Math.min(byWidth, byHeight);
            float sp = tsPx / getResources().getDisplayMetrics().scaledDensity;
            input.setTextSize(Math.max(10f, Math.min(18f, sp)));
            input.setLineSpacing(Math.max(0f, h / 25f - tsPx * 1.18f), 1f);

        } catch (Throwable ignored) { }
    }

    @Override
    protected void dispatchDraw(Canvas c) {
        Glass.page(c, getWidth(), getHeight());
        Paint line = new Paint();
        line.setColor(Ui.alpha(Ui.TEXT, 0.10f));
        c.drawRect(0, 0, getWidth(), Math.max(1f, Ui.dp(1f)), line);
        super.dispatchDraw(c);
    }

    /**
     * 输入框：自己接管"拖动"手势。
     * - 拖动只滚动，绝不触发焦点/光标/键盘
     * - 只有一次没有位移的轻点，才交给父类定位光标并唤起键盘
     * - 滚动做边界夹取，杜绝过度拉出产生的空白/黑屏
     */
    static class RichEdit extends EditText {
        Runnable onSel;
        Runnable onTap;
        ImgTap onImageTap;
        private int downSelStart, downSelEnd;
        private String downImagePath;
        interface ImgTap { void onImage(String path); }
        private final int slop;
        private float downY, lastY;
        private boolean dragging;
        private android.view.VelocityTracker vt;
        private ValueAnimator flingAnim;

        RichEdit(Context c) {
            super(c);
            slop = android.view.ViewConfiguration.get(c).getScaledTouchSlop();
            setOverScrollMode(OVER_SCROLL_ALWAYS);
        }

        @Override
        protected void onSelectionChanged(int selStart, int selEnd) {
            super.onSelectionChanged(selStart, selEnd);
            if (onSel != null) onSel.run();
        }

        private int maxScroll() {
            android.text.Layout l = getLayout();
            if (l == null) return 0;
            int view = getHeight() - getPaddingTop() - getPaddingBottom();
            return Math.max(0, l.getHeight() - view);
        }

        void smoothScrollBy(int dy) {
            if (dy == 0) return;
            if (flingAnim != null) flingAnim.cancel();
            final int total = dy;
            ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
            a.setDuration(180);
            a.setInterpolator(Ui.EASE);
            a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                private int done = 0;
                @Override public void onAnimationUpdate(ValueAnimator anim) {
                    float t = (Float) anim.getAnimatedValue();
                    int want = (int) (total * t);
                    int d = want - done;
                    if (d != 0) {
                        scrollClamped(d);
                        done = want;
                    }
                }
            });
            a.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator animation) {
                    flingAnim = null;
                }
            });
            flingAnim = a;
            a.start();
        }

        /** 精确命中：横向必须落在该图片字符的矩形内，纵向在该行内 */
        private String hitImageAt(float x, float y) {
            try {
                android.text.Layout l = getLayout();
                if (l == null) return null;
                int ly = (int) (y - getPaddingTop() + getScrollY());
                if (ly < 0) ly = 0;
                if (ly > l.getHeight() - 1) ly = Math.max(0, l.getHeight() - 1);
                int line = l.getLineForVertical(ly);
                int ls = l.getLineStart(line);
                int le = Math.min(l.getLineEnd(line), getText().length());
                Editable ed = getText();
                if (ed == null) return null;
                for (int i2 = ls; i2 < le; i2++) {
                    if (ed.charAt(i2) != '\uFFFC') continue;
                    InlineImageSpan[] sp = ed.getSpans(i2, i2 + 1, InlineImageSpan.class);
                    if (sp.length == 0) continue;
                    float a = l.getPrimaryHorizontal(i2);
                    float b = l.getPrimaryHorizontal(i2 + 1);
                    float left = Math.min(a, b), right = Math.max(a, b);
                    if (x >= left && x <= right) return sp[0].path;
                }
            } catch (Throwable ignored) { }
            return null;
        }

        void scrollClamped(int dy) {
            if (dy == 0) return;
            int max = maxScroll();
            int target = getScrollY() + dy;
            if (target < 0) target = 0;
            if (target > max) target = max;
            if (target != getScrollY()) scrollTo(0, target);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downSelStart = getSelectionStart();
                    downSelEnd = getSelectionEnd();
                    downImagePath = hitImageAt(e.getX(), e.getY());
                    if (downImagePath != null) return true;   // 点的是图片：不定位光标
                    downY = lastY = e.getY();
                    dragging = false;
                    if (vt == null) vt = android.view.VelocityTracker.obtain();
                    vt.clear();
                    vt.addMovement(e);
                    super.onTouchEvent(e);
                    return true;

                case MotionEvent.ACTION_MOVE: {
                    if (vt != null) vt.addMovement(e);
                    if (!dragging && Math.abs(e.getY() - downY) > slop) {
                        dragging = true;
                        if (flingAnim != null) { flingAnim.cancel(); flingAnim = null; }
                        // 告诉父类这次手势被取消了，避免残留按下态
                        MotionEvent cancel = MotionEvent.obtain(e);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        super.onTouchEvent(cancel);
                        cancel.recycle();
                    }
                    if (dragging) {
                        scrollClamped((int) (lastY - e.getY()));
                        lastY = e.getY();
                        return true;
                    }
                    return super.onTouchEvent(e);
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (dragging) {
                        dragging = false;
                        if (vt != null) {
                            vt.addMovement(e);
                            vt.computeCurrentVelocity(1000);
                            startFling((int) vt.getYVelocity());
                        }
                        return true;   // 拖动结束：不当作点击
                    }
                    // 图片：直接打开预览，返回后不碰光标
                    if (downImagePath != null) {
                        String p = downImagePath;
                        downImagePath = null;
                        if (onImageTap != null) onImageTap.onImage(p);
                        return true;
                    }
                    boolean handled = super.onTouchEvent(e);
                    if (onTap != null) onTap.run();
                    return handled;
            }
            return super.onTouchEvent(e);
        }

        void startFling(int vy) {
            if (Math.abs(vy) < 120) return;
            if (flingAnim != null) flingAnim.cancel();
            final float total = -vy * 0.72f;   // 滑得更远
            ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
            a.setDuration(Math.min(1500, 780 + (int) (Math.abs(total) / 5f)));
            a.setInterpolator(new android.view.animation.DecelerateInterpolator(1.75f));
            a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                private int done = 0;
                @Override public void onAnimationUpdate(ValueAnimator anim) {
                    float t = (Float) anim.getAnimatedValue();
                    int want = (int) (total * t);
                    int d = want - done;
                    if (d != 0) {
                        scrollClamped(d);
                        done = want;
                    }
                }
            });
            a.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator animation) {
                    flingAnim = null;
                }
            });
            flingAnim = a;
            a.start();
        }
    }

    /** 解码失败时的可见占位 */
    static class FallbackImage extends Drawable {
        private final int w, h;
        private final float radius;
        private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tx = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();

        FallbackImage(int w, int h, float radius) {
            this.w = w; this.h = h; this.radius = radius;
            bg.setColor(Ui.alpha(Ui.TEXT, 0.08f));
            tx.setColor(Ui.alpha(Ui.TEXT, 0.45f));
            tx.setTextSize(Ui.dp(12));
            tx.setTypeface(Ui.reg());
            tx.setTextAlign(Paint.Align.CENTER);
        }

        @Override public int getIntrinsicWidth() { return w; }
        @Override public int getIntrinsicHeight() { return h; }

        @Override public void draw(Canvas c) {
            box.set(getBounds());
            c.drawRoundRect(box, radius, radius, bg);
            c.drawText(Str.s("image_load_failed"), box.centerX(), box.centerY() + Ui.dp(4), tx);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter cf) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
