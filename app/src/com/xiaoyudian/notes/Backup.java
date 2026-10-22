package com.xiaoyudian.notes;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 数据备份 / 导出导入。
 * 一个 zip 内含：data.json（分类、便签、附件清单）+ img/ 下的原始图片。
 * 导入为「完全覆盖」，会还原时间戳、置顶、回收站、定位、分类与图片。
 */
public final class Backup {

    public static final String DIR = "backups";
    private static final String DATA = "data.json";
    private static final int FORMAT = 1;

    // ---------------- 定时/触发备份 ----------------

    /** @param onRecord true=记录后触发，false=进入应用触发 */
    public static void maybeAuto(final Context c, final boolean onRecord) {
        if (!Settings.backupAuto(c)) return;
        int mode = Settings.backupMode(c);
        if (mode == 0 && !onRecord) return;
        if (mode == 1 && onRecord) return;
        long now = System.currentTimeMillis();
        long gap = Math.max(1, Settings.backupInterval(c)) * 60_000L;
        if (now - Settings.lastBackup(c) < gap) return;
        Settings.setLastBackup(c, now);
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    File f = autoFile(c);
                    FileOutputStream out = new FileOutputStream(f);
                    exportTo(c, out);
                    out.close();
                } catch (Throwable ignored) { }
            }
        }).start();
    }

    public static File autoFile(Context c) {
        File d = new File(c.getFilesDir(), DIR);
        if (!d.exists()) d.mkdirs();
        return new File(d, "latest.zip");
    }

    // ---------------- 导出 ----------------

    public static void exportTo(Context c, OutputStream os) throws Exception {
        SQLiteDatabase d = Db.get(c).getWritableDatabase();
        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("app", Ui.VERSION);
        root.put("exported", System.currentTimeMillis());

        JSONArray folders = new JSONArray();
        Cursor fc = d.rawQuery("SELECT _id,name,sort,color,created FROM folders ORDER BY _id", null);
        while (fc.moveToNext()) {
            JSONObject o = new JSONObject();
            o.put("id", fc.getLong(0));
            o.put("name", fc.getString(1));
            o.put("sort", fc.getInt(2));
            o.put("color", fc.getInt(3));
            o.put("created", fc.getLong(4));
            folders.put(o);
        }
        fc.close();
        root.put("folders", folders);

        JSONArray notes = new JSONArray();
        File pics = new File(c.getFilesDir(), "pics");
        ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(os));
        zip.putNextEntry(new ZipEntry(DATA));
        // 图片条目会边读边写，所以 data.json 先写占位，最后覆盖写入
        zip.closeEntry();

        Cursor nc = d.rawQuery("SELECT _id,content,spans,folder_id,created,updated,deleted,"
                + "deleted_at,pinned,loc_name,loc_lat,loc_lng FROM notes ORDER BY _id", null);
        while (nc.moveToNext()) {
            long id = nc.getLong(0);
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("content", nc.getString(1) == null ? "" : nc.getString(1));
            o.put("spans", nc.getString(2) == null ? "" : nc.getString(2));
            o.put("folder", nc.getLong(3));
            o.put("created", nc.getLong(4));
            o.put("updated", nc.getLong(5));
            o.put("deleted", nc.getInt(6));
            o.put("deletedAt", nc.getLong(7));
            o.put("pinned", nc.getInt(8));
            o.put("loc", nc.getString(9) == null ? "" : nc.getString(9));
            o.put("lat", nc.getDouble(10));
            o.put("lng", nc.getDouble(11));

            JSONArray imgs = new JSONArray();
            Cursor ac = d.rawQuery("SELECT path FROM atts WHERE note_id = ? ORDER BY _id",
                    new String[]{String.valueOf(id)});
            int idx = 0;
            while (ac.moveToNext()) {
                String path = ac.getString(0);
                File f = path == null ? null : new File(path);
                if (f != null && f.exists()) {
                    String name = "img/" + id + "_" + idx + ".jpg";
                    zip.putNextEntry(new ZipEntry(name));
                    copy(new FileInputStream(f), zip);
                    zip.closeEntry();
                    imgs.put(name);
                    idx++;
                } else if (path != null) {
                    imgs.put("");   // 占位，保持顺序
                }
            }
            ac.close();
            o.put("imgs", imgs);
            notes.put(o);
        }
        nc.close();
        root.put("notes", notes);

        // 个人资料（昵称、个性签名），随备份一起迁移
        JSONObject prefs = new JSONObject();
        prefs.put("nickname", Settings.nickname(c));
        prefs.put("signature", Settings.signature(c));

        String avatarPath = Settings.avatar(c);
        boolean hasAvatar = false;
        if (avatarPath != null) {
            File af = new File(avatarPath);
            if (af.exists()) {
                zip.putNextEntry(new ZipEntry("img/avatar.jpg"));
                copy(new FileInputStream(af), zip);
                zip.closeEntry();
                hasAvatar = true;
            }
        }
        prefs.put("avatar", hasAvatar);
        // 界面偏好：语言、主题、字号、强调色、视图
        prefs.put("lang", Settings.lang(c));
        prefs.put("theme", Settings.themeMode(c));
        prefs.put("fontLevel", Settings.fontLevel(c));
        prefs.put("fontScale", Settings.fontScale(c));
        prefs.put("viewMode", Settings.viewMode(c));
        prefs.put("accent", Settings.accent(c));
        prefs.put("accentValue", Settings.accentValue(c));
        prefs.put("backupAuto", Settings.backupAuto(c));
        prefs.put("backupMode", Settings.backupMode(c));
        prefs.put("backupInterval", Settings.backupInterval(c));
        root.put("prefs", prefs);

        // 用新条目重写 data.json
        zip.putNextEntry(new ZipEntry(DATA + ".tmp"));
        zip.write(root.toString().getBytes("UTF-8"));
        zip.closeEntry();
        zip.finish();
        zip.flush();
    }

    // ---------------- 导入 ----------------

    public static int importFrom(Context c, InputStream is) throws Exception {
        byte[] data = null;
        List<String> names = new ArrayList<>();
        List<byte[]> blobs = new ArrayList<>();

        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(is));
        ZipEntry e;
        while ((e = zip.getNextEntry()) != null) {
            String n = e.getName();
            if (DATA.equals(n) || (DATA + ".tmp").equals(n)) {
                data = readAll(zip);
            } else if (n.startsWith("img/")) {
                names.add(n);
                blobs.add(readAll(zip));
            }
            zip.closeEntry();
        }
        zip.close();
        if (data == null) throw new Exception("no data");
        JSONObject root = new JSONObject(new String(data, "UTF-8"));

        File pics = new File(c.getFilesDir(), "pics");
        if (!pics.exists()) pics.mkdirs();
        String stamp = String.valueOf(System.currentTimeMillis());

        // 先把图片落盘，再改数据库
        String[] localPaths = new String[names.size()];
        for (int i = 0; i < names.size(); i++) {
            File f = new File(pics, "imp_" + stamp + "_" + i + ".jpg");
            FileOutputStream out = new FileOutputStream(f);
            out.write(blobs.get(i));
            out.close();
            localPaths[i] = f.getAbsolutePath();
        }
        java.util.Map<String, String> imgMap = new java.util.HashMap<>();
        for (int i = 0; i < names.size(); i++) imgMap.put(names.get(i), localPaths[i]);

        SQLiteDatabase d = Db.get(c).getWritableDatabase();
        d.beginTransaction();
        try {
            d.execSQL("DELETE FROM atts");
            d.execSQL("DELETE FROM notes");
            d.execSQL("DELETE FROM folders");

            JSONArray folders = root.optJSONArray("folders");
            for (int i = 0; folders != null && i < folders.length(); i++) {
                JSONObject o = folders.getJSONObject(i);
                d.execSQL("INSERT INTO folders (_id,name,sort,color,created) VALUES (?,?,?,?,?)",
                        new Object[]{o.optLong("id"), o.optString("name"), o.optInt("sort"),
                                o.optInt("color"), o.optLong("created")});
            }

            JSONArray notes = root.optJSONArray("notes");
            int count = 0;
            for (int i = 0; notes != null && i < notes.length(); i++) {
                JSONObject o = notes.getJSONObject(i);
                long id = o.optLong("id");
                d.execSQL("INSERT INTO notes (_id,content,spans,folder_id,created,updated,deleted,"
                                + "deleted_at,pinned,loc_name,loc_lat,loc_lng) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                        new Object[]{id, o.optString("content"), o.optString("spans"),
                                o.optLong("folder"), o.optLong("created"), o.optLong("updated"),
                                o.optInt("deleted"), o.optLong("deletedAt"), o.optInt("pinned"),
                                o.optString("loc"), o.optDouble("lat"), o.optDouble("lng")});

                JSONArray imgs = o.optJSONArray("imgs");
                for (int k = 0; imgs != null && k < imgs.length(); k++) {
                    String key = imgs.optString(k);
                    if (key == null || key.isEmpty()) continue;
                    String path = imgMap.get(key);
                    if (path == null) continue;
                    d.execSQL("INSERT INTO atts (note_id,path,kind,created) VALUES (?,?,1,?)",
                            new Object[]{id, path, System.currentTimeMillis()});
                }
                count++;
            }
            d.setTransactionSuccessful();

            JSONObject prefs = root.optJSONObject("prefs");
            if (prefs != null) {
                String nick = prefs.optString("nickname", "");
                if (!nick.isEmpty()) Settings.setNickname(c, nick);
                Settings.setSignature(c, prefs.optString("signature", ""));
                if (prefs.optBoolean("avatar", false)) {
                    String ap = imgMap.get("img/avatar.jpg");
                    if (ap != null) Settings.setAvatar(c, ap);
                }
                if (prefs.has("lang")) Settings.setLang(c, prefs.optInt("lang", Str.ZH));
                if (prefs.has("theme")) Settings.setThemeMode(c, prefs.optInt("theme", 0));
                if (prefs.has("fontLevel")) Settings.setFontLevel(c, prefs.optInt("fontLevel", 1));
                if (prefs.has("viewMode")) Settings.setViewMode(c, prefs.optInt("viewMode", 0));
                if (prefs.has("accent")) Settings.setAccent(c, prefs.optInt("accent", 0));
                if (prefs.has("accentValue")) Settings.setAccentValue(c, (float) prefs.optDouble("accentValue", 0.55));
                if (prefs.has("backupAuto")) Settings.setBackupAuto(c, prefs.optBoolean("backupAuto", false));
                if (prefs.has("backupMode")) Settings.setBackupMode(c, prefs.optInt("backupMode", 0));
                if (prefs.has("backupInterval")) Settings.setBackupInterval(c, prefs.optInt("backupInterval", 5));
            }
            return count;
        } finally {
            d.endTransaction();
        }
    }

    // ---------------- 工具 ----------------

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16 * 1024];
        int r;
        while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
        return out.toByteArray();
    }

    private static void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[16 * 1024];
        int r;
        while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
        in.close();
    }

    private Backup() { }
}
