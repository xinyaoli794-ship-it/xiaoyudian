package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 一条便签：卡片 + 左滑露出的「置顶 / 移动 / 删除」 */
public class NoteRow extends FrameLayout {

    public interface Listener {
        void onOpen(Note n);
        void onMenu(Note n);
        void onPin(Note n);
        void onMove(Note n);
        void onDelete(Note n);
        void onRowOpened(NoteRow row);
    }

    private static final int N = 3;
    private final NoteCard card;
    private final LinearLayout actions;
    private final Ic[] icons = new Ic[N];
    private final TextView[] labels = new TextView[N];
    private final Listener listener;
    private final int actionW;
    private final int slop;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Note note;
    private float downX, downY, startTx;
    private boolean claimed, longFired, openState;
    private final Runnable longPress;

    public NoteRow(Context c, Listener l) {
        super(c);
        this.listener = l;
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        actionW = Ui.dpi(62 * N);

        longPress = new Runnable() {
            @Override public void run() {
                if (!claimed && !openState && note != null) {
                    longFired = true;
                    listener.onMenu(note);
                }
            }
        };

        actions = new LinearLayout(c);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(Ui.dp(21));
        g.setColor(Ui.CARD_2);
        g.setStroke(Ui.dpi(1), Ui.LINE);
        actions.setBackground(g);
        FrameLayout.LayoutParams alp = new FrameLayout.LayoutParams(actionW, -1, Gravity.END);
        addView(actions, alp);

        int[][] spec = {
        {Ic.UP, 0xFFC9D7FF, 0x1AFFFFFF},
                {Ic.FOLDER, 0xFFC9D7FF, 0x1AFFFFFF},
                {Ic.TRASH, 0xFFFF9E9E, 0x1AFF7A7A},
        };
        String[] text = {Str.s("pin"), Str.s("move_note"), Str.s("delete")};
        for (int i = 0; i < N; i++) {
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -1, 1f);
            actions.addView(col, clp);

            icons[i] = new Ic(c, spec[i][0], spec[i][1]);
            col.addView(icons[i], new LinearLayout.LayoutParams(Ui.dpi(19), Ui.dpi(19)));

            labels[i] = new TextView(c);
            labels[i].setText(text[i]);
            labels[i].setTextSize(10.5f);
            labels[i].setTextColor(spec[i][1]);
            labels[i].setIncludeFontPadding(false);
            labels[i].setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-2, -2);
            tlp.topMargin = Ui.dpi(6);
            col.addView(labels[i], tlp);
        }

        card = new NoteCard(c);
        addView(card, new FrameLayout.LayoutParams(-1, -2));
    }

    public Note bind(Note n, boolean detailed) {
        note = n;
        card.bind(n, detailed);
        icons[0].setKind(Ic.UP);
        labels[0].setText(n.pinned ? Str.s("unpin") : Str.s("pin"));
        collapse(false);
        return n;
    }

    public boolean isOpen() { return openState; }

    public void collapse(boolean animate) {
        openState = false;
        if (animate) {
            card.animate().translationX(0f).setDuration(200).setInterpolator(Ui.EASE).start();
        } else {
            card.animate().cancel();
            card.setTranslationX(0f);
        }
    }

    private void settle(boolean open) {
        openState = open;
        float target = open ? -actionW : 0f;
        card.animate().translationX(target)
                .setDuration(open ? 240 : 200)
                .setInterpolator(open ? Ui.EASE : Ui.EASE_IO)
                .withEndAction(null).start();
        if (open) listener.onRowOpened(this);
    }

    private void fire(int idx) {
        if (note == null) return;
        switch (idx) {
            case 0: listener.onPin(note); break;
            case 1: listener.onMove(note); break;
            default: listener.onDelete(note); break;
        }
        collapse(true);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                startTx = card.getTranslationX();
                claimed = false;
                longFired = false;
                handler.removeCallbacks(longPress);
                handler.postDelayed(longPress, 480);
                return true;

            case MotionEvent.ACTION_MOVE: {
                float dx = e.getX() - downX, dy = e.getY() - downY;
                if (!claimed && Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * 1.3f) {
                    claimed = true;
                    handler.removeCallbacks(longPress);
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (claimed) {
                    float tx = startTx + dx;
                    if (tx > 0f) tx = 0f;
                    if (tx < -actionW) tx = -actionW;
                    card.setTranslationX(tx);
                }
                return true;
            }

            case MotionEvent.ACTION_UP: {
                handler.removeCallbacks(longPress);
                if (claimed) {
                    settle(card.getTranslationX() < -actionW * 0.4f);
                    return true;
                }
                float dx = e.getX() - downX, dy = e.getY() - downY;
                if (Math.abs(dx) < slop && Math.abs(dy) < slop && !longFired) {
                    if (openState) {
                        if (e.getX() > getWidth() - actionW) {
                            int idx = (int) ((e.getX() - (getWidth() - actionW)) / (actionW / (float) N));
                            fire(Math.max(0, Math.min(N - 1, idx)));
                        } else {
                            settle(false);
                        }
                    } else if (note != null) {
                        listener.onOpen(note);
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPress);
                if (claimed) settle(card.getTranslationX() < -actionW * 0.4f);
                return true;
        }
        return true;
    }
}
