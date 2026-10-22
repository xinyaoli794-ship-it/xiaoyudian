package com.xiaoyudian.notes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** 本地 SQLite：完全离线，不联网 */
public class Db extends SQLiteOpenHelper {

    private static final String NAME = "xiaoyudian.db";
    private static final int VER = 3;
    private static Db inst;

    public static synchronized Db get(Context c) {
        if (inst == null) inst = new Db(c.getApplicationContext());
        return inst;
    }

    private Db(Context c) { super(c, NAME, null, VER); }

    @Override
    public void onCreate(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE folders (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " name TEXT NOT NULL, sort INTEGER DEFAULT 0, color INTEGER DEFAULT 0, created INTEGER)");
        d.execSQL("CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " content TEXT DEFAULT '', spans TEXT, folder_id INTEGER DEFAULT 0,"
                + " created INTEGER, updated INTEGER, deleted INTEGER DEFAULT 0,"
                + " deleted_at INTEGER DEFAULT 0, pinned INTEGER DEFAULT 0,"
                + " loc_name TEXT, loc_lat REAL DEFAULT 0, loc_lng REAL DEFAULT 0)");
        d.execSQL("CREATE TABLE atts (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " note_id INTEGER, path TEXT, kind INTEGER DEFAULT 1, created INTEGER)");
        d.execSQL("CREATE INDEX idx_notes_updated ON notes(deleted, updated)");
        d.execSQL("CREATE INDEX idx_atts_note ON atts(note_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase d, int o, int n) {
        if (o < 3) {
            try { d.execSQL("ALTER TABLE folders ADD COLUMN color INTEGER DEFAULT 0"); } catch (Exception ignored) { }
        }
        if (o < 2) {
            try { d.execSQL("ALTER TABLE notes ADD COLUMN spans TEXT"); } catch (Exception ignored) { }
            try { d.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER DEFAULT 0"); } catch (Exception ignored) { }
        }
    }

    // ---------- 便签 ----------

    private static final String NOTE_SELECT =
            "SELECT n._id, n.content, n.spans, n.folder_id, n.created, n.updated, n.deleted,"
                    + " n.deleted_at, n.pinned, n.loc_name, n.loc_lat, n.loc_lng,"
                    + " (SELECT a.path FROM atts a WHERE a.note_id = n._id ORDER BY a._id LIMIT 1) AS thumb,"
                    + " (SELECT COUNT(*) FROM atts a2 WHERE a2.note_id = n._id) AS natt,"
                    + " (SELECT f.name FROM folders f WHERE f._id = n.folder_id) AS fname"
                    + " FROM notes n ";

    public List<Note> notes(long folderId, boolean deleted) {
        List<Note> out = new ArrayList<>();
        String where = deleted ? " WHERE n.deleted = 1" : " WHERE n.deleted = 0";
        if (!deleted && folderId > 0) where += " AND n.folder_id = " + folderId;
        String order = deleted
                ? " ORDER BY n.deleted_at DESC"
                : " ORDER BY n.pinned DESC, n.updated DESC";
        Cursor c = getReadableDatabase().rawQuery(NOTE_SELECT + where + order, null);
        while (c.moveToNext()) out.add(readNote(c));
        c.close();
        return out;
    }

    public List<Note> search(String q) {
        List<Note> out = new ArrayList<>();
        String like = "%" + (q == null ? "" : q.replace("%", "")) + "%";
        Cursor c = getReadableDatabase().rawQuery(
                NOTE_SELECT + " WHERE n.deleted = 0 AND n.content LIKE ?"
                        + " ORDER BY n.pinned DESC, n.updated DESC", new String[]{like});
        while (c.moveToNext()) out.add(readNote(c));
        c.close();
        return out;
    }

    public Note note(long id) {
        Cursor c = getReadableDatabase().rawQuery(NOTE_SELECT + " WHERE n._id = " + id, null);
        Note n = c.moveToFirst() ? readNote(c) : null;
        c.close();
        return n;
    }

    private Note readNote(Cursor c) {
        Note n = new Note();
        n.id = c.getLong(c.getColumnIndexOrThrow("_id"));
        n.content = str(c, "content");
        n.spans = str(c, "spans");
        n.folderId = c.getLong(c.getColumnIndexOrThrow("folder_id"));
        n.created = c.getLong(c.getColumnIndexOrThrow("created"));
        n.updated = c.getLong(c.getColumnIndexOrThrow("updated"));
        n.deleted = c.getLong(c.getColumnIndexOrThrow("deleted")) == 1;
        n.deletedAt = c.getLong(c.getColumnIndexOrThrow("deleted_at"));
        n.pinned = c.getLong(c.getColumnIndexOrThrow("pinned")) == 1;
        n.locName = str(c, "loc_name");
        n.lat = c.getDouble(c.getColumnIndexOrThrow("loc_lat"));
        n.lng = c.getDouble(c.getColumnIndexOrThrow("loc_lng"));
        n.thumb = str(c, "thumb");
        n.attCount = c.getInt(c.getColumnIndexOrThrow("natt"));
        return n;
    }

    private static String str(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i < 0 || c.isNull(i)) return null;
        return c.getString(i);
    }

    public long insertNote(long folderId) {
        long now = System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put("content", "");
        v.put("spans", "");
        v.put("folder_id", folderId);
        v.put("created", now);
        v.put("updated", now);
        v.put("pinned", 0);
        return getWritableDatabase().insert("notes", null, v);
    }

    public void saveNote(long id, String content) {
        saveNote(id, content, null);
    }

    /** 只存内容、不刷新最后修改时间（自动保存用） */
    public void saveNoteNoTouch(long id, String content, String spans) {
        ContentValues v = new ContentValues();
        v.put("content", content == null ? "" : content);
        if (spans != null) v.put("spans", spans);
        getWritableDatabase().update("notes", v, "_id = ?", new String[]{String.valueOf(id)});
    }

    public void saveNote(long id, String content, String spans) {
        ContentValues v = new ContentValues();
        v.put("content", content == null ? "" : content);
        if (spans != null) v.put("spans", spans);
        v.put("updated", System.currentTimeMillis());
        getWritableDatabase().update("notes", v, "_id = ?", new String[]{String.valueOf(id)});
    }

    public void setPinned(long id, boolean pinned) {
        ContentValues v = new ContentValues();
        v.put("pinned", pinned ? 1 : 0);
        getWritableDatabase().update("notes", v, "_id = ?", new String[]{String.valueOf(id)});
    }

    public void setFolder(long id, long folderId) {
        ContentValues v = new ContentValues();
        v.put("folder_id", folderId);
        v.put("updated", System.currentTimeMillis());
        getWritableDatabase().update("notes", v, "_id = ?", new String[]{String.valueOf(id)});
    }

    public void setLocation(long id, String name, double lat, double lng) {
        ContentValues v = new ContentValues();
        v.put("loc_name", name);
        v.put("loc_lat", lat);
        v.put("loc_lng", lng);
        v.put("updated", System.currentTimeMillis());
        getWritableDatabase().update("notes", v, "_id = ?", new String[]{String.valueOf(id)});
    }

    public void clearLocation(long id) {
        ContentValues v = new ContentValues();
        v.putNull("loc_name");
        v.put("loc_lat", 0);
        v.put("loc_lng", 0);
        v.put("updated", System.currentTimeMillis());
        getWritableDatabase().update("notes", v, "_id = ?", new String[]{String.valueOf(id)});
    }

    public void softDelete(long id) {
        ContentValues v = new ContentValues();
        v.put("deleted", 1);
        v.put("deleted_at", System.currentTimeMillis());
        v.put("pinned", 0);
        getWritableDatabase().update("notes", v, "_id = ?", new String[]{String.valueOf(id)});
    }

    public void restore(long id) {
        ContentValues v = new ContentValues();
        v.put("deleted", 0);
        v.put("deleted_at", 0);
        getWritableDatabase().update("notes", v, "_id = ?", new String[]{String.valueOf(id)});
    }

    public List<String> atts(long noteId) {
        List<String> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT path FROM atts WHERE note_id = " + noteId + " ORDER BY _id", null);
        while (c.moveToNext()) out.add(c.getString(0));
        c.close();
        return out;
    }

    public void addAtt(long noteId, String path) {
        ContentValues v = new ContentValues();
        v.put("note_id", noteId);
        v.put("path", path);
        v.put("created", System.currentTimeMillis());
        getWritableDatabase().insert("atts", null, v);
    }

    public void clearAtts(long noteId) {
        getWritableDatabase().delete("atts", "note_id = ?", new String[]{String.valueOf(noteId)});
    }

    public void removeAtt(long noteId, String path) {
        getWritableDatabase().delete("atts", "note_id = ? AND path = ?",
                new String[]{String.valueOf(noteId), path});
    }

    public void purge(long id) {
        List<String> files = atts(id);
        getWritableDatabase().delete("atts", "note_id = ?", new String[]{String.valueOf(id)});
        getWritableDatabase().delete("notes", "_id = ?", new String[]{String.valueOf(id)});
        for (String f : files) try { new java.io.File(f).delete(); } catch (Exception ignored) { }
    }

    public void purgeAllTrash() {
        for (Note n : notes(0, true)) purge(n.id);
    }

    // ---------- 分类 ----------

    public List<Folder> folders() {
        List<Folder> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT f._id, f.name, (SELECT COUNT(*) FROM notes n WHERE n.folder_id = f._id AND n.deleted = 0), f.color"
                        + " FROM folders f ORDER BY f.sort, f._id", null);
        while (c.moveToNext()) {
            Folder f = new Folder();
            f.id = c.getLong(0);
            f.name = c.getString(1);
            f.count = c.getInt(2);
            f.color = c.getInt(3);
            out.add(f);
        }
        c.close();
        return out;
    }

    public long addFolder(String name) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("created", System.currentTimeMillis());
        return getWritableDatabase().insert("folders", null, v);
    }

    public void setFolderColor(long id, int color) {
        ContentValues v = new ContentValues();
        v.put("color", color);
        getWritableDatabase().update("folders", v, "_id = ?", new String[]{String.valueOf(id)});
    }

    public void renameFolder(long id, String name) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        getWritableDatabase().update("folders", v, "_id = ?", new String[]{String.valueOf(id)});
    }

    public void deleteFolder(long id) {
        ContentValues v = new ContentValues();
        v.put("folder_id", 0);
        getWritableDatabase().update("notes", v, "folder_id = ?", new String[]{String.valueOf(id)});
        getWritableDatabase().delete("folders", "_id = ?", new String[]{String.valueOf(id)});
    }

    public int countAll() { return scalar("SELECT COUNT(*) FROM notes WHERE deleted = 0"); }
    public int countTrash() { return scalar("SELECT COUNT(*) FROM notes WHERE deleted = 1"); }

    private int scalar(String sql) {
        Cursor c = getReadableDatabase().rawQuery(sql, null);
        int v = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return v;
    }

    public String folderName(long id) {
        if (id <= 0) return Str.s("all_notes");
        Cursor c = getReadableDatabase().rawQuery("SELECT name FROM folders WHERE _id = " + id, null);
        String s = c.moveToFirst() ? c.getString(0) : Str.s("all_notes");
        c.close();
        return s;
    }
}
