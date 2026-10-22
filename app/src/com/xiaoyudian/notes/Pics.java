package com.xiaoyudian.notes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 图片解码 + 内存缓存（缩略图，避免 OOM） */
public final class Pics {

    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(12 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);

    private Pics() {}

    public static Bitmap cached(String path, int px) {
        if (path == null) return null;
        return CACHE.get(path + "@" + px);
    }

    public static void load(final String path, final int px, final Callback cb) {
        if (path == null) { cb.onBitmap(null); return; }
        Bitmap hit = CACHE.get(path + "@" + px);
        if (hit != null) { cb.onBitmap(hit); return; }
        POOL.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap b = decode(path, px);
                if (b != null) CACHE.put(path + "@" + px, b);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override public void run() { cb.onBitmap(b); }
                });
            }
        });
    }

    /** 同步解码（带缓存），用于正文内嵌插图 */
    public static Bitmap decodeNow(String path, int px) {
        if (path == null) return null;
        Bitmap hit = CACHE.get(path + "@" + px);
        if (hit != null) return hit;
        Bitmap b = decode(path, px);
        if (b != null) CACHE.put(path + "@" + px, b);
        return b;
    }

    public static void evict(String path) {
        if (path == null) return;
        CACHE.evictAll();
    }

    private static Bitmap decode(String path, int px) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            if (o.outWidth <= 0) return null;
            int sample = 1;
            while (o.outWidth / (sample * 2) >= px && o.outHeight / (sample * 2) >= px) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            o2.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(path, o2);
        } catch (Throwable t) {
            return null;
        }
    }

    public interface Callback { void onBitmap(Bitmap b); }
}
