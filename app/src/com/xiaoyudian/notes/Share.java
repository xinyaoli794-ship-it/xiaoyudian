package com.xiaoyudian.notes;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 分享：复制全文 / 生成图片分享 */
public final class Share {

    private static final char PH = '\uFFFC';

    /** 把正文里的图片占位符换成可读文字 */
    public static String plain(CharSequence s) {
        if (s == null) return "";
        String t = s.toString().replace(String.valueOf(PH), "［图片］");
        return t.replaceAll("\n{3,}", "\n\n").trim();
    }

    public static void copyText(Context c, CharSequence text) {
        try {
            ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("note", plain(text)));
            toast(c, Str.s("copied_clip"));
        } catch (Throwable ignored) { }
    }

    /** 渲染成一张长图并调起分享 */
    /** 分享图字号预设：标准 / 大 / 特大 */
    public static final float[] SIZES = {32f, 40f, 50f};

    public static void shareImage(Context c, String title, CharSequence body, long updatedAt) {
        shareImage(c, title, body, updatedAt, 1);
    }

    public static void shareImage(Context c, String title, CharSequence body, long updatedAt, int sizePreset) {
        try {
            File f = render(c, title, body, updatedAt, sizePreset);
            if (f == null) {
                toast(c, Str.s("share_failed"));
                return;
            }
            Uri uri = ShareProvider.uriFor(c, f);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("image/png");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.putExtra(Intent.EXTRA_SUBJECT, Str.s("share_image"));
            // 关键：把 uri 放进 ClipData，部分应用（如 QQ）只从 ClipData 取授权
            i.setClipData(ClipData.newUri(c.getContentResolver(), "note", uri));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Intent chooser = Intent.createChooser(i, Str.s("share_image"));
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(chooser);
        } catch (Throwable t) {
            toast(c, Str.s("share_failed"));
        }
    }

    /** 画一张 1080 宽的分享卡：正文 + 时间 + 品牌落款 */
    private static File render(Context c, String title, CharSequence body, long updatedAt, int sizePreset) {
        try {
            int W = 1080;
            int pad = 72;
            int textW = W - pad * 2;

            TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tp.setTextSize(SIZES[Math.max(0, Math.min(2, sizePreset))]);
            tp.setColor(Ui.TEXT);
            tp.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

            String text = plain(body);
            if (TextUtils.isEmpty(text)) text = Str.s("note_empty");

            StaticLayout layout = StaticLayout.Builder
                    .obtain(text, 0, text.length(), tp, textW)
                    .setLineSpacing(12f, 1.06f)
                    .setIncludePad(false)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build();

            TextPaint meta = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            meta.setTextSize(26f);
            meta.setColor(Ui.TEXT_3);

            TextPaint foot = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            foot.setTextSize(26f);
            foot.setColor(Ui.alpha(Ui.TEXT, 0.35f));

            String stamp = Str.s("last_update")
                    + new SimpleDateFormat("yyyy-M-d HH:mm", Locale.getDefault()).format(new Date(updatedAt));
            String brand = "小雨点 · " + stamp;

            int headH = 0;   // 不再画标题：正文首行与标题重复，直接出正文
            int H = pad + layout.getHeight() + 96 + 40 + pad;

            Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bmp);
            cv.drawColor(Ui.BG);

            int y = pad;

            cv.save();
            cv.translate(pad, y);
            layout.draw(cv);
            cv.restore();
            y += layout.getHeight() + 40;

            Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
            line.setColor(Ui.alpha(Ui.TEXT, 0.10f));
            cv.drawRect(pad, y, W - pad, y + Math.max(1, Ui.dpi(0.75f)), line);
            y += 36;

            cv.drawText(ellipsize(brand, foot, textW), pad, y + 26, foot);

            File f = new File(ShareProvider.dir(c), "note_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            bmp.recycle();
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String ellipsize(String s, TextPaint p, int maxW) {
        return TextUtils.ellipsize(s == null ? "" : s, p, maxW, TextUtils.TruncateAt.END).toString();
    }

    private static void toast(Context c, String msg) {
        try {
            Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) { }
    }

    private Share() { }
}
