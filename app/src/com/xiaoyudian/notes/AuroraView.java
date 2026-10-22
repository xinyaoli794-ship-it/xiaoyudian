package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** 纯色页面背景（去掉渐变与模糊，滚动时零开销） */
public class AuroraView extends View {

    private final Paint paint = new Paint();

    public AuroraView(Context c) {
        super(c);
        setBackgroundColor(0);
    }

    public void refresh() {
        paint.setColor(Ui.BG);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(Ui.BG);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }
}
