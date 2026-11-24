package com.xiaoyudian.notes;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * 极简文件共享 Provider：把 cache/share 里的图片以 content:// 暴露给其它 App。
 * （没有 AndroidX，所以不用 FileProvider，自己实现一个。）
 */
public class ShareProvider extends ContentProvider {

    public static final String AUTHORITY = "com.xiaoyudian.notes.share";

    public static File dir(Context c) {
        File d = new File(c.getCacheDir(), "share");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static Uri uriFor(Context c, File f) {
        return Uri.parse("content://" + AUTHORITY + "/" + Uri.encode(f.getName()));
    }

    @Override public boolean onCreate() { return true; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        try {
            String name = uri.getLastPathSegment();
            if (name == null) throw new FileNotFoundException("bad uri");
            File f = new File(dir(getContext()), name);
            if (!f.getCanonicalPath().startsWith(dir(getContext()).getCanonicalPath())) {
                throw new FileNotFoundException("bad path");
            }
            return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override public String getType(Uri uri) { return "image/png"; }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder) { return null; }

    @Override public Uri insert(Uri uri, ContentValues values) { return null; }

    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }

    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { return 0; }
}
