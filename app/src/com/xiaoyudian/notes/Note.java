package com.xiaoyudian.notes;

/** 一条便签 */
public class Note {
    public long id;
    public String content = "";
    public String spans;      // 富文本片段（加粗/斜体/下划线/删除线）
    public long folderId = 0L;
    public long created;
    public long updated;
    public boolean deleted;
    public long deletedAt;
    public boolean pinned;
    public String locName;
    public double lat, lng;
    public String thumb;
    public String folderName;
    public int attCount;

    public String title() {
        String t = content == null ? "" : content.trim();
        if (t.isEmpty()) return "";
        int nl = t.indexOf('\n');
        return (nl >= 0 ? t.substring(0, nl) : t).trim();
    }

    public String preview() {
        String t = content == null ? "" : content.trim();
        int nl = t.indexOf('\n');
        if (nl < 0) return "";
        return t.substring(nl + 1).trim();
    }

    public boolean hasLocation() { return locName != null && locName.length() > 0; }
}
