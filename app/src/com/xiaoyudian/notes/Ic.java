package com.xiaoyudian.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** 手绘线性图标（不依赖任何图标库） */
public class Ic extends View {

    public static final int MENU = 0, BACK = 1, PLUS = 2, IMAGE = 3, PIN = 4, TRASH = 5,
            RESTORE = 6, CLOSE = 7, FOLDER = 8, CHECK = 9, SEARCH = 10, NOTE = 11, MORE = 12, DROP = 13,
            GRID = 14, LINES = 15, UP = 16, GEAR = 17, UNDO = 18, REDO = 19, BOLD = 20, ITALIC = 21,
            UNDERLINE = 22, STRIKE = 23, NUM = 24, BOX = 25, AVATAR = 26;

    private int kind;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF tmp = new RectF();
    private float[] dots;

    public Ic(Context c, int kind, int color) {
        super(c);
        this.kind = kind;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(color);
        setWillNotDraw(false);
    }

    public void setColor(int color) { p.setColor(color); invalidate(); }
    public void setKind(int k) { kind = k; invalidate(); }

    @Override
    protected void onDraw(Canvas cv) {
        float s = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = s / 2f;
        p.setStrokeWidth(s * 0.085f);
        cv.save();
        cv.translate(cx, cy);
        path.reset();
        dots = null;
        switch (kind) {
            case MENU:
                path.moveTo(-0.62f * r, -0.42f * r); path.lineTo(0.62f * r, -0.42f * r);
                path.moveTo(-0.62f * r, 0f);        path.lineTo(0.26f * r, 0f);
                path.moveTo(-0.62f * r, 0.42f * r); path.lineTo(0.48f * r, 0.42f * r);
                break;
            case BACK:
                path.moveTo(0.20f * r, -0.60f * r);
                path.lineTo(-0.30f * r, 0f);
                path.lineTo(0.20f * r, 0.60f * r);
                break;
            case PLUS:
                path.moveTo(0f, -0.55f * r); path.lineTo(0f, 0.55f * r);
                path.moveTo(-0.55f * r, 0f); path.lineTo(0.55f * r, 0f);
                break;
            case IMAGE:
                tmp.set(-0.74f * r, -0.60f * r, 0.74f * r, 0.60f * r);
                path.addRoundRect(tmp, 0.20f * r, 0.20f * r, Path.Direction.CW);
                path.moveTo(-0.74f * r, 0.20f * r);
                path.lineTo(-0.20f * r, -0.28f * r);
                path.lineTo(0.16f * r, 0.06f * r);
                path.lineTo(0.40f * r, -0.14f * r);
                path.lineTo(0.74f * r, 0.20f * r);
                dots = new float[]{0.34f * r, -0.30f * r, 0.10f * r};
                break;
            case PIN:
                path.addCircle(0f, -0.20f * r, 0.40f * r, Path.Direction.CW);
                path.moveTo(-0.28f * r, 0.02f * r);
                path.lineTo(0f, 0.78f * r);
                path.lineTo(0.28f * r, 0.02f * r);
                break;
            case TRASH:
                path.moveTo(-0.62f * r, -0.42f * r); path.lineTo(0.62f * r, -0.42f * r);
                path.moveTo(-0.20f * r, -0.42f * r);
                path.lineTo(-0.20f * r, -0.62f * r);
                path.lineTo(0.20f * r, -0.62f * r);
                path.lineTo(0.20f * r, -0.42f * r);
                path.moveTo(-0.46f * r, -0.20f * r);
                path.lineTo(-0.38f * r, 0.66f * r);
                path.lineTo(0.38f * r, 0.66f * r);
                path.lineTo(0.46f * r, -0.20f * r);
                break;
            case RESTORE:
                tmp.set(-0.56f * r, -0.56f * r, 0.56f * r, 0.56f * r);
                path.addArc(tmp, -60f, 280f);
                path.moveTo(0.10f * r, -0.74f * r);
                path.lineTo(0.44f * r, -0.52f * r);
                path.lineTo(0.14f * r, -0.26f * r);
                break;
            case CLOSE:
                path.moveTo(-0.42f * r, -0.42f * r); path.lineTo(0.42f * r, 0.42f * r);
                path.moveTo(0.42f * r, -0.42f * r);  path.lineTo(-0.42f * r, 0.42f * r);
                break;
            case FOLDER:
                path.moveTo(-0.70f * r, -0.44f * r);
                path.lineTo(-0.26f * r, -0.44f * r);
                path.lineTo(-0.12f * r, -0.24f * r);
                path.lineTo(0.70f * r, -0.24f * r);
                path.lineTo(0.70f * r, 0.54f * r);
                path.lineTo(-0.70f * r, 0.54f * r);
                path.close();
                break;
            case CHECK:
                path.moveTo(-0.50f * r, 0.02f * r);
                path.lineTo(-0.12f * r, 0.40f * r);
                path.lineTo(0.52f * r, -0.40f * r);
                break;
            case SEARCH:
                path.addCircle(-0.08f * r, -0.08f * r, 0.40f * r, Path.Direction.CW);
                path.moveTo(0.22f * r, 0.20f * r);
                path.lineTo(0.60f * r, 0.58f * r);
                break;
            case NOTE:
                tmp.set(-0.58f * r, -0.68f * r, 0.58f * r, 0.68f * r);
                path.addRoundRect(tmp, 0.16f * r, 0.16f * r, Path.Direction.CW);
                path.moveTo(-0.30f * r, -0.26f * r); path.lineTo(0.30f * r, -0.26f * r);
                path.moveTo(-0.30f * r, 0.02f * r);  path.lineTo(0.30f * r, 0.02f * r);
                path.moveTo(-0.30f * r, 0.30f * r);  path.lineTo(0.06f * r, 0.30f * r);
                break;
            case MORE:
                dots = new float[]{-0.44f * r, 0f, 0.13f * r, 0f, 0f, 0.13f * r, 0.44f * r, 0f, 0.13f * r};
                break;
            case DROP:
                path.moveTo(0f, -0.74f * r);
                path.cubicTo(0.30f * r, -0.36f * r, 0.54f * r, -0.10f * r, 0.54f * r, 0.12f * r);
                path.cubicTo(0.54f * r, 0.47f * r, 0.30f * r, 0.70f * r, 0f, 0.70f * r);
                path.cubicTo(-0.30f * r, 0.70f * r, -0.54f * r, 0.47f * r, -0.54f * r, 0.12f * r);
                path.cubicTo(-0.54f * r, -0.10f * r, -0.30f * r, -0.36f * r, 0f, -0.74f * r);
                path.close();
                break;
            case GRID:
                for (int i = 0; i < 4; i++) {
                    float x = (i % 2 == 0) ? -0.56f * r : 0.10f * r;
                    float y = (i < 2) ? -0.56f * r : 0.10f * r;
                    tmp.set(x, y, x + 0.46f * r, y + 0.46f * r);
                    path.addRoundRect(tmp, 0.12f * r, 0.12f * r, Path.Direction.CW);
                }
                break;
            case LINES:
                for (int i = 0; i < 3; i++) {
                    float y = (i - 1) * 0.42f * r;
                    path.moveTo(-0.16f * r, y);
                    path.lineTo(0.62f * r, y);
                }
                dots = new float[]{-0.52f * r, -0.42f * r, 0.10f * r,
                        -0.52f * r, 0f, 0.10f * r,
                        -0.52f * r, 0.42f * r, 0.10f * r};
                break;
            case UP:
                path.moveTo(-0.55f * r, 0.18f * r);
                path.lineTo(0f, -0.38f * r);
                path.lineTo(0.55f * r, 0.18f * r);
                break;
            case GEAR:
                // 滑杆样式：三条横线 + 三个旋钮，比齿轮更易读
                for (int i = 0; i < 3; i++) {
                    float y = (i - 1) * 0.46f * r;
                    path.moveTo(-0.62f * r, y);
                    path.lineTo(0.62f * r, y);
                }
                dots = new float[]{
                        -0.20f * r, -0.46f * r, 0.13f * r,
                        0.24f * r, 0f, 0.13f * r,
                        -0.34f * r, 0.46f * r, 0.13f * r};
                break;
            case UNDO:
                tmp.set(-0.64f * r, -0.62f * r, 0.64f * r, 0.34f * r);
                path.addArc(tmp, 200f, -215f);
                path.moveTo(-0.52f * r, 0.10f * r);
                path.lineTo(-0.62f * r, -0.44f * r);
                path.lineTo(-0.10f * r, -0.52f * r);
                break;
            case REDO:
                tmp.set(-0.64f * r, -0.62f * r, 0.64f * r, 0.34f * r);
                path.addArc(tmp, -20f, 215f);
                path.moveTo(0.52f * r, 0.10f * r);
                path.lineTo(0.62f * r, -0.44f * r);
                path.lineTo(0.10f * r, -0.52f * r);
                break;
            case BOLD:
                path.moveTo(-0.34f * r, -0.62f * r);
                path.lineTo(-0.34f * r, 0.62f * r);
                path.moveTo(-0.34f * r, -0.62f * r);
                path.lineTo(0.14f * r, -0.62f * r);
                path.cubicTo(0.46f * r, -0.62f * r, 0.46f * r, -0.06f * r, 0.14f * r, -0.06f * r);
                path.lineTo(-0.34f * r, -0.06f * r);
                path.moveTo(0.14f * r, -0.06f * r);
                path.lineTo(0.22f * r, -0.06f * r);
                path.cubicTo(0.58f * r, -0.06f * r, 0.58f * r, 0.62f * r, 0.22f * r, 0.62f * r);
                path.lineTo(-0.34f * r, 0.62f * r);
                break;
            case ITALIC:
                path.moveTo(-0.10f * r, -0.58f * r);
                path.lineTo(0.44f * r, -0.58f * r);
                path.moveTo(-0.34f * r, 0.58f * r);
                path.lineTo(0.20f * r, 0.58f * r);
                path.moveTo(0.16f * r, -0.58f * r);
                path.lineTo(-0.10f * r, 0.58f * r);
                break;
            case UNDERLINE:
                path.moveTo(-0.10f * r, -0.58f * r);
                path.lineTo(0.44f * r, -0.58f * r);
                path.moveTo(-0.34f * r, 0.50f * r);
                path.lineTo(0.20f * r, 0.50f * r);
                path.moveTo(0.16f * r, -0.58f * r);
                path.lineTo(-0.10f * r, 0.50f * r);
                path.moveTo(-0.48f * r, 0.80f * r);
                path.lineTo(0.48f * r, 0.80f * r);
                break;
            case STRIKE:
                path.moveTo(-0.10f * r, -0.58f * r);
                path.lineTo(0.44f * r, -0.58f * r);
                path.moveTo(-0.34f * r, 0.58f * r);
                path.lineTo(0.20f * r, 0.58f * r);
                path.moveTo(0.16f * r, -0.58f * r);
                path.lineTo(-0.10f * r, 0.58f * r);
                path.moveTo(-0.55f * r, 0f);
                path.lineTo(0.55f * r, 0f);
                break;
            case NUM:
                path.moveTo(-0.30f * r, -0.52f * r);
                path.lineTo(-0.08f * r, -0.70f * r);
                path.lineTo(-0.08f * r, 0.30f * r);
                path.moveTo(0.22f * r, -0.62f * r);
                path.lineTo(0.46f * r, -0.62f * r);
                path.lineTo(0.22f * r, -0.16f * r);
                path.lineTo(0.50f * r, -0.16f * r);
                path.moveTo(0.22f * r, 0.16f * r);
                path.lineTo(0.48f * r, 0.16f * r);
                path.lineTo(0.30f * r, 0.44f * r);
                path.lineTo(0.50f * r, 0.44f * r);
                path.cubicTo(0.50f * r, 0.72f * r, 0.20f * r, 0.72f * r, 0.22f * r, 0.60f * r);
                break;
            case BOX:
                tmp.set(-0.56f * r, -0.56f * r, 0.56f * r, 0.56f * r);
                path.addRoundRect(tmp, 0.16f * r, 0.16f * r, Path.Direction.CW);
                break;
            case AVATAR:
                path.addCircle(0f, -0.22f * r, 0.34f * r, Path.Direction.CW);
                tmp.set(-0.60f * r, 0.10f * r, 0.60f * r, 1.30f * r);
                path.addArc(tmp, 180f, 180f);
                break;
        }
        cv.drawPath(path, p);
        if (dots != null) {
            Paint fill = new Paint(p);
            fill.setStyle(Paint.Style.FILL);
            for (int i = 0; i + 2 < dots.length; i += 3)
                cv.drawCircle(dots[i], dots[i + 1], dots[i + 2], fill);
        }
        cv.restore();
    }
}
