package com.xiaoyudian.notes;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements ListScreen.Host, DrawerPanel.Host,
        EditorScreen.Host, TrashScreen.Host, SettingsScreen.Host, SearchScreen.Host {

    private static final int REQ_IMAGES = 1001;
    private static final int REQ_LOCATION = 1002;
    private static final int REQ_AVATAR = 1003;

    private Root root;
    private FrameLayout contentHost;
    private View scrim;
    private DrawerPanel drawer;
    private FrameLayout sheetLayer;
    private ListScreen list;
    private EditorScreen editor;
    private TrashScreen trash;
    private SettingsScreen settings;
    private DataScreen data;
    private AboutScreen about;
    private SearchScreen search;
    private static final int REQ_EXPORT = 1201, REQ_IMPORT = 1202;
    private ZoomImageView zoomView;

    private int insetTop, insetBottom, imeBottom;
    private int drawerWidth;
    private boolean drawerOpen;
    private ValueAnimator drawerAnim;

    private EditorScreen pendingLocation;
    private Sheet locationSheet;
    private LocationListener locListener;
    private boolean pickAvatar;

    @Override
    protected void attachBaseContext(Context base) {
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        float s = Settings.fontScale(base);
        if (Math.abs(cfg.fontScale - s) > 0.001f) cfg.fontScale = s;
        super.attachBaseContext(base.createConfigurationContext(cfg));
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        installCrashLog();
        Lang.apply(this);
        Ui.init(this);

        Window w = getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) w.setNavigationBarContrastEnforced(false);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            w.setAttributes(lp);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Ui.BG));
        root = new Root(this);
        root.setBackgroundColor(Ui.BG);

        root.addView(new AuroraView(this), new FrameLayout.LayoutParams(-1, -1));

        contentHost = new FrameLayout(this);
        contentHost.setClipChildren(true);
        root.addView(contentHost, new FrameLayout.LayoutParams(-1, -1));

        scrim = new View(this);
        scrim.setBackgroundColor(Ui.SCRIM);
        scrim.setAlpha(0f);
        scrim.setVisibility(View.GONE);
        scrim.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { animateDrawer(false); }
        });
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        drawerWidth = Math.min((int) (getResources().getDisplayMetrics().widthPixels * 0.72f), Ui.dpi(300));
        drawer = new DrawerPanel(this, this);
        root.addView(drawer, new FrameLayout.LayoutParams(drawerWidth, -1));
        drawer.setTranslationX(-drawerWidth);

        sheetLayer = new FrameLayout(this);
        root.addView(sheetLayer, new FrameLayout.LayoutParams(-1, -1));

        list = new ListScreen(this, this);
        contentHost.addView(list, new FrameLayout.LayoutParams(-1, -1));
        list.reload(true);
        drawer.rebuild();

        setContentView(root);
        applySystemBars();
        root.post(new Runnable() {
            @Override public void run() { updateGestureExclusion(); }
        });

        if (SettingsScreen.reopen) {
            SettingsScreen.reopen = false;
            openSettings(true);   // 同步、无淡入：第一帧就是设置页，不会透出主界面
        }
        playThemeTransition();    // 主题/字号切换后的颜色过渡
    }

    private static boolean crashLogInstalled = false;

    /** 崩溃时把堆栈写到外部应用目录，方便反馈问题 */
    private void installCrashLog() {
        if (crashLogInstalled) return;
        crashLogInstalled = true;
        final Thread.UncaughtExceptionHandler def = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                try {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    e.printStackTrace(new java.io.PrintWriter(sw));
                    File dir = getExternalFilesDir(null);
                    if (dir != null) {
                        File f = new File(dir, "crash.txt");
                        FileOutputStream fo = new FileOutputStream(f, false);
                        fo.write((new java.util.Date() + "\n" + sw.toString() + "\n").getBytes("UTF-8"));
                        fo.close();
                    }
                } catch (Throwable ignored) { }
                if (def != null) def.uncaughtException(t, e);
            }
        });
    }

    private void applySystemBars() {        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                c.setSystemBarsAppearance(Ui.DARK ? 0 : mask, mask);
            }
        } else {
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (!Ui.DARK) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void onRootInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            insetTop = bars.top;
            insetBottom = bars.bottom;
            imeBottom = insets.getInsets(WindowInsets.Type.ime()).bottom;
        } else {
            insetTop = insets.getSystemWindowInsetTop();
            insetBottom = insets.getSystemWindowInsetBottom();
            imeBottom = 0;
        }
        applyInsets();
    }

    private void applyInsets() {
        list.setInsets(insetTop, insetBottom);
        drawer.setInsets(insetTop, insetBottom);
        if (editor != null) editor.setInsets(insetTop, Math.max(insetBottom, imeBottom), imeBottom);
        if (trash != null) trash.setInsets(insetTop, insetBottom);
        if (settings != null) settings.setInsets(insetTop, insetBottom);
        if (search != null) search.setInsets(insetTop, insetBottom);
    }

    // ==================== 抽屉 ====================

    private void setDrawerTx(float tx) {
        if (tx > 0f) tx = 0f;
        if (tx < -drawerWidth) tx = -drawerWidth;
        drawer.setTranslationX(tx);
        float p = 1f + tx / drawerWidth;
        if (p < 0f) p = 0f;
        if (p > 1f) p = 1f;
        scrim.setAlpha(p * 0.72f);
        scrim.setVisibility(p > 0.004f ? View.VISIBLE : View.GONE);
        // 注意：contentHost 不做任何位移/缩放。否则新页面会在变换状态下插入，
        // 一旦动画被打断就会整页偏移、被裁切（横向偏倚/溢出）。
        contentHost.setTranslationX(0f);
        contentHost.setScaleX(1f);
        contentHost.setScaleY(1f);
    }

    /** 打开/关闭抽屉。任何时候都以"动画结束后强制落到终点"收尾，避免停在半路。 */
    private void animateDrawer(final boolean open) {
        if (drawerAnim != null) {
            drawerAnim.cancel();
            drawerAnim = null;
        }
        final float from = drawer.getTranslationX();
        final float to = open ? 0f : -drawerWidth;
        drawerOpen = open;
        if (Math.abs(from - to) < 1f) {
            setDrawerTx(to);
            updateGestureExclusion();
            return;
        }
        if (open) drawer.rebuild();
        float frac = Math.abs(to - from) / Math.max(1f, drawerWidth);
        ValueAnimator a = ValueAnimator.ofFloat(from, to);
        a.setDuration(Math.max(130, (long) (150 + 190 * frac)));
        a.setInterpolator(open ? Ui.EASE : Ui.EASE_IO);
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator anim) {
                setDrawerTx((Float) anim.getAnimatedValue());
            }
        });
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                setDrawerTx(to);
                drawerAnim = null;
                updateGestureExclusion();
            }
        });
        drawerAnim = a;
        a.start();
        updateGestureExclusion();
    }

    /** 不再占用左侧边缘，系统返回手势恢复原样 */
    private void updateGestureExclusion() {
        if (Build.VERSION.SDK_INT < 29 || root == null) return;
        root.setSystemGestureExclusionRects(java.util.Collections.<android.graphics.Rect>emptyList());
    }

    @Override public void openDrawer() { animateDrawer(true); }

    /** 切页前把抽屉立即归位，保证新页面插入时抽屉已经彻底让开 */
    private void snapDrawerClosed() {
        if (drawerAnim != null) {
            drawerAnim.cancel();
            drawerAnim = null;
        }
        drawerOpen = false;
        setDrawerTx(-drawerWidth);
        updateGestureExclusion();
    }

    /** 清掉同类型的旧页面，避免叠了两层导致"看起来只显示一半" */
    private void removeStale(Class<?> cls) {
        for (int i = contentHost.getChildCount() - 1; i >= 0; i--) {
            View v = contentHost.getChildAt(i);
            if (cls.isInstance(v)) contentHost.removeView(v);
        }
    }

    private void normalize(View v) {
        v.setTranslationX(0f);
        v.setTranslationY(0f);
        v.setScaleX(1f);
        v.setScaleY(1f);
    }

    @Override public void selectFolder(long id) {
        animateDrawer(false);
        drawer.setCurrent(id);
        list.setFolder(id, true);
    }

    @Override public void openTrash() {
        snapDrawerClosed();
        if (trash != null) return;
        if (settings != null) closeSettings();
        removeStale(TrashScreen.class);
        trash = new TrashScreen(this, this);
        trash.setInsets(insetTop, insetBottom);
        normalize(trash);
        trash.setAlpha(0f);
        contentHost.addView(trash, new FrameLayout.LayoutParams(-1, -1));
        trash.animate().alpha(1f).setDuration(220).setInterpolator(Ui.EASE).start();
    }

    @Override public void newFolder() {
        snapDrawerClosed();
        Sheet.input(sheetLayer, Str.s("folder_new_title"), Str.s("folder_name"), null, new Sheet.OnText() {
            @Override public void onText(String text) {
                if (text == null || text.isEmpty()) return;
                Db.get(MainActivity.this).addFolder(text);
                drawer.rebuild();
                list.reload(false);
            }
        });
    }

    @Override public void folderMenu(final Folder f) {
        Sheet.centerTitle = true;
        Sheet.menu(sheetLayer, f.name, new String[]{Str.s("rename"), Str.s("icon_color"), Str.s("delete_folder")}, new Runnable[]{
                new Runnable() {
                    @Override public void run() {
                        Sheet.input(sheetLayer, Str.s("folder_rename_title"), Str.s("folder_name"), f.name, new Sheet.OnText() {
                            @Override public void onText(String text) {
                                if (text == null || text.isEmpty()) return;
                                Db.get(MainActivity.this).renameFolder(f.id, text);
                                drawer.rebuild();
                                list.reload(false);
                            }
                        });
                    }
                },
                new Runnable() {
                    @Override public void run() {
                        final String[] names = {Str.s("color_blue"), Str.s("color_teal"), Str.s("color_orange"),
                        Str.s("color_pink"), Str.s("color_purple"), Str.s("color_green"), Str.s("color_default")};
                        final int[] colors = {0xFF8FA9FF, 0xFF7FD1C1, 0xFFFFB86B,
                                0xFFFF8FA3, 0xFFB99CFF, 0xFF9BE87F, 0};
                        Runnable[] acts = new Runnable[names.length];
                        for (int i = 0; i < names.length; i++) {
                            final int col = colors[i];
                            acts[i] = new Runnable() {
                                @Override public void run() {
                                    Db.get(MainActivity.this).setFolderColor(f.id, col);
                                    drawer.rebuild();
                                }
                            };
                        }
                        Sheet.menu(sheetLayer, Str.s("icon_color"), names, acts);
                    }
                },
                new Runnable() {
                    @Override public void run() {
                        Sheet.confirm(sheetLayer, Str.f("delete_folder_q", f.name),
                                Str.s("delete_folder_body"),
                                Str.s("delete_folder"), true, new Runnable() {
                                    @Override public void run() {
                                        Db.get(MainActivity.this).deleteFolder(f.id);
                                        if (list.folderId() == f.id) list.setFolder(0, true);
                                        drawer.setCurrent(list.folderId());
                                        drawer.rebuild();
                                        list.reload(false);
                                    }
                                });
                    }
                }
        });
    }

    // ==================== 列表操作 ====================

    @Override
    public void openEditor(long noteId, long folderId) {
        if (editor != null) return;
        // 列表里可能还有行在播入场动画，先掐掉，避免和编辑页动画抢帧
        try {
            for (int i = 0; i < list.getChildCount(); i++) {
                list.getChildAt(i).animate().cancel();
            }
        } catch (Throwable ignored) { }
        removeStale(EditorScreen.class);
        final EditorScreen e = new EditorScreen(this, noteId, folderId, this);
        editor = e;
        e.setInsets(insetTop, Math.max(insetBottom, imeBottom), imeBottom);
        normalize(e);
        e.setTranslationY(Ui.dp(46));
        e.setAlpha(0f);
        contentHost.addView(e, new FrameLayout.LayoutParams(-1, -1));
        // 等首帧完成布局后再开始动画，避免动画丢失或首帧卡顿
        e.post(new Runnable() {
            @Override public void run() {
                if (editor != e) return;
                e.animate().translationY(0f).alpha(1f).setDuration(300).setInterpolator(Ui.EASE)
                        .withEndAction(new Runnable() {
                            @Override public void run() {
                                if (editor != null) normalize(editor);
                            }
                        }).start();
            }
        });
    }

    @Override
    public void closeEditor(EditorScreen s) {
        final EditorScreen target = s;
        final boolean changed = s.close();
        s.animate().translationY(Ui.dp(40)).alpha(0f).setDuration(210).setInterpolator(Ui.EASE_IN)
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        contentHost.removeView(target);
                        if (editor == target) editor = null;
                        list.setScaleX(1f);
                        list.setScaleY(1f);
                        list.setAlpha(1f);
                        // 只有内容真的变了才重排；否则完全不碰列表，避免返回瞬间整屏闪烁
                        list.onBackFromEditor(changed);
                        if (changed) Backup.maybeAuto(MainActivity.this, true);
                        drawer.rebuild();
                    }
                }).start();
    }

    @Override
    public void noteMenu(final Note n) {
        // 不在操作菜单里显示便签正文（只列动作）
        Sheet.menu(sheetLayer, null,
                new String[]{n.pinned ? Str.s("unpin") : Str.s("pin"), Str.s("move_to_folder"), Str.s("copy_text"), Str.s("delete_note")},
                new Runnable[]{
                        new Runnable() { @Override public void run() { pin(n); } },
                        new Runnable() { @Override public void run() { move(n); } },
                        new Runnable() { @Override public void run() { copyText(n); } },
                        new Runnable() { @Override public void run() { delete(n); } }
                });
    }

    @Override
    public void pin(final Note n) {
        Db.get(this).setPinned(n.id, !n.pinned);
        list.softRefresh();
        toast(n.pinned ? Str.s("unpinned_toast") : Str.s("pinned_toast"));
    }

    @Override
    public void move(final Note n) {
        List<Folder> folders = Db.get(this).folders();
        String[] items = new String[folders.size() + 1];
        final long[] ids = new long[folders.size() + 1];
        items[0] = Str.s("uncategorized");
        ids[0] = 0L;
        for (int i = 0; i < folders.size(); i++) {
            items[i + 1] = folders.get(i).name;
            ids[i + 1] = folders.get(i).id;
        }
        Runnable[] acts = new Runnable[items.length];
        for (int i = 0; i < items.length; i++) {
            final long fid = ids[i];
            acts[i] = new Runnable() {
                @Override public void run() {
                    Db.get(MainActivity.this).setFolder(n.id, fid);
                    list.reload(true);
                    drawer.rebuild();
                }
            };
        }
        Sheet.menu(sheetLayer, Str.s("move_to"), items, acts);
    }

    @Override
    public void delete(final Note n) {
        Sheet.confirm(sheetLayer, "删除便签", Str.s("delete_note_body"), Str.s("delete"), true,
                new Runnable() {
                    @Override public void run() {
                        Db.get(MainActivity.this).softDelete(n.id);
                        list.reload(true);
                        drawer.rebuild();
                    }
                });
    }

    @Override
    public void switchView(int mode) {
        Settings.setViewMode(this, mode);
        list.setViewMode(mode);
    }

    private void copyText(Note n) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("note", n.content));
        toast(Str.s("copied_clip"));
    }

    @Override
    public void pinNote(EditorScreen s) {
        Note n = Db.get(this).note(s.noteId);
        if (n != null) pin(n);
    }

    @Override
    public void moveNote(EditorScreen s) {
        Note n = Db.get(this).note(s.noteId);
        if (n != null) move(n);
    }

    @Override
    public void showMore(final EditorScreen s) {
        if (Db.get(this).note(s.noteId) == null) return;
        Sheet.menu(sheetLayer, null,
                new String[]{Str.s("pin_note"), Str.s("move_note"), Str.s("delete_note")},
                new Runnable[]{
                        new Runnable() { @Override public void run() { pinNote(s); } },
                        new Runnable() { @Override public void run() { moveNote(s); } },
                        new Runnable() { @Override public void run() { deleteNote(s); } }
                }, 2);
    }

    @Override
    public void deleteNote(final EditorScreen s) {
        Sheet.confirm(sheetLayer, "删除便签", Str.s("delete_note_body"), Str.s("delete"), true,
                new Runnable() {
                    @Override public void run() {
                        Db.get(MainActivity.this).softDelete(s.noteId);
                        editor = null;
                        contentHost.removeView(s);
                        list.setScaleX(1f);
                        list.setScaleY(1f);
                        list.animate().alpha(1f).setDuration(200).start();
                        list.reload(true);
                        drawer.rebuild();
                    }
                });
    }

    // ==================== 回收站 ====================

    @Override
    public void closeTrash() {
        if (trash == null) return;
        final TrashScreen t = trash;
        trash = null;
        t.setTranslationX(0f);
        t.animate().alpha(0f).setDuration(180).setInterpolator(Ui.EASE_IN)
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        if (t.getParent() != null) contentHost.removeView(t);
                        list.reload(false);
                        drawer.rebuild();
                    }
                }).start();
        t.postDelayed(new Runnable() {
            @Override public void run() {
                if (t.getParent() != null) contentHost.removeView(t);
            }
        }, 420);
    }

    @Override
    public void requestEmptyTrash() {
        Sheet.confirm(sheetLayer, Str.s("clear_trash"), Str.s("clear_trash_body"), Str.s("clear_all"), true,
                new Runnable() {
                    @Override public void run() {
                        Db.get(MainActivity.this).purgeAllTrash();
                        if (trash != null) trash.reload();
                        drawer.rebuild();
                    }
                });
    }

    // ==================== 设置 ====================

    // ==================== 搜索 ====================

    @Override
    public void openSearch() {
        snapDrawerClosed();
        if (search != null) return;
        removeStale(SearchScreen.class);
        search = new SearchScreen(this, this);
        search.setInsets(insetTop, insetBottom);
        normalize(search);
        search.setAlpha(0f);
        contentHost.addView(search, new FrameLayout.LayoutParams(-1, -1));
        search.animate().alpha(1f).setDuration(200).setInterpolator(Ui.EASE).start();
    }

    @Override
    public void closeSearch() {
        if (search == null) return;
        final SearchScreen s = search;
        search = null;
        try {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(s.getWindowToken(), 0);
        } catch (Throwable ignored) { }
        s.animate().alpha(0f).setDuration(160).withEndAction(new Runnable() {
            @Override public void run() {
                if (s.getParent() != null) contentHost.removeView(s);
            }
        }).start();
    }

    @Override
    public void openResult(long noteId) {
        closeSearch();
        openEditor(noteId, 0L);
    }

    public void openSettings() { openSettings(false); }

    /** instant=true 时不做淡入，用于重建后立刻恢复设置页 */
    public void openSettings(boolean instant) {
        snapDrawerClosed();
        if (settings != null) return;
        if (trash != null) closeTrash();
        removeStale(SettingsScreen.class);
        settings = new SettingsScreen(this, this);
        settings.setInsets(insetTop, insetBottom);
        normalize(settings);
        contentHost.addView(settings, new FrameLayout.LayoutParams(-1, -1));
        if (instant) {
            settings.setAlpha(1f);
        } else {
            settings.setAlpha(0f);
            settings.animate().alpha(1f).setDuration(220).setInterpolator(Ui.EASE).start();
        }
    }

    @Override
    public void openData() {
        if (data != null) return;
        final DataScreen d = new DataScreen(this, new DataScreen.Host() {
            @Override public void exportFile() {
                try {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/zip");
                    intent.putExtra(Intent.EXTRA_TITLE, "xiaoyudian-backup.zip");
                    startActivityForResult(intent, REQ_EXPORT);
                } catch (Throwable t) {
                    toast(Str.s("export_failed"));
                }
            }

            @Override public void importFile() {
                Sheet.confirm(sheetLayer, Str.s("import"), Str.s("import_hint"),
                        Str.s("import"), true, new Runnable() {
                            @Override public void run() {
                                try {
                                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                                    intent.setType("*/*");
                                    startActivityForResult(intent, REQ_IMPORT);
                                } catch (Throwable t) {
                                    toast(Str.s("import_failed"));
                                }
                            }
                        });
            }

            @Override public void closeData() { MainActivity.this.closeData(); }

            @Override public void toast(String msg) { MainActivity.this.toast(msg); }
        }, insetTop, insetBottom);
        data = d;
        normalize(d);
        contentHost.addView(d, new FrameLayout.LayoutParams(-1, -1));
        d.setAlpha(0f);
        d.animate().alpha(1f).setDuration(200).setInterpolator(Ui.EASE).start();
    }

    public void closeData() {
        if (data == null) return;
        final DataScreen d = data;
        data = null;
        d.animate().alpha(0f).setDuration(160).setInterpolator(Ui.EASE_IN)
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        if (d.getParent() != null) contentHost.removeView(d);
                        if (settings != null) settings.rebuild();
                    }
                }).start();
    }

    @Override
    public void showAbout() {
        if (about != null) return;
        final AboutScreen a = new AboutScreen(this, new AboutScreen.Host() {
            @Override public void closeAbout() { MainActivity.this.closeAbout(); }
            @Override public void toast(String msg) { MainActivity.this.toast(msg); }
        }, insetTop, insetBottom);
        about = a;
        normalize(a);
        contentHost.addView(a, new FrameLayout.LayoutParams(-1, -1));
        a.setAlpha(0f);
        a.animate().alpha(1f).setDuration(200).setInterpolator(Ui.EASE).start();
    }

    public void closeAbout() {
        if (about == null) return;
        final AboutScreen a = about;
        about = null;
        a.animate().alpha(0f).setDuration(160).setInterpolator(Ui.EASE_IN)
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        if (a.getParent() != null) contentHost.removeView(a);
                    }
                }).start();
    }

    @Override
    public void closeSettings() {
        if (settings == null) return;
        final SettingsScreen s = settings;
        settings = null;
        s.setTranslationX(0f);
        s.animate().alpha(0f).setDuration(180).setInterpolator(Ui.EASE_IN)
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        if (s.getParent() != null) contentHost.removeView(s);
                        list.reload(false);
                        drawer.rebuild();
                    }
                }).start();
        s.postDelayed(new Runnable() {
            @Override public void run() {
                if (s.getParent() != null) contentHost.removeView(s);
            }
        }, 420);
    }

    @Override
    public void pickAvatar(SettingsScreen s) {
        pickAvatar = true;
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(i, REQ_AVATAR);
        } catch (Exception e) {
            toast(Str.s("no_album"));
        }
    }

    private static Bitmap themeSnapshot;

    @Override
    public void applyTheme(int mode) {
        themeSnapshot = captureScreen();
        Ui.applyPalette(Settings.isDark(this));
        recreate();
    }

    @Override
    public void applyFont(int level) {
        themeSnapshot = captureScreen();
        recreate();
    }

    /** 把当前画面抓成位图，重建后叠在上面淡出，做"颜色缓慢过渡" */
    private Bitmap captureScreen() {
        try {
            int w = root.getWidth(), h = root.getHeight();
            if (w <= 0 || h <= 0) return null;
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            root.draw(c);
            return b;
        } catch (Throwable t) {
            return null;
        }
    }

    private void playThemeTransition() {
        final Bitmap snap = themeSnapshot;
        themeSnapshot = null;
        if (snap == null || snap.isRecycled() || root == null) return;
        final ImageView layer = new ImageView(this);
        layer.setScaleType(ImageView.ScaleType.FIT_XY);
        layer.setImageBitmap(snap);
        root.addView(layer, new FrameLayout.LayoutParams(-1, -1));
        layer.post(new Runnable() {
            @Override public void run() {
                layer.animate().alpha(0f).setDuration(420).setInterpolator(Ui.EASE_IO)
                        .withEndAction(new Runnable() {
                            @Override public void run() {
                                root.removeView(layer);
                                layer.setImageDrawable(null);
                                if (!snap.isRecycled()) snap.recycle();
                            }
                        }).start();
            }
        });
    }

    @Override
    public void applyView(int mode) {
        list.setViewMode(mode);
    }

    public void editNickname() {
        Sheet.input(sheetLayer, Str.s("nickname_title"), Str.s("nickname"), Settings.nickname(this), new Sheet.OnText() {
            @Override public void onText(String text) {
                if (text == null || text.trim().isEmpty()) return;
                Settings.setNickname(MainActivity.this, text.trim());
                if (settings != null) settings.rebuild();
                drawer.rebuild();
            }
        });
    }

    @Override
    public void editSignature() {
        Sheet.input(sheetLayer, Str.s("signature"), Str.s("signature_hint"), Settings.signature(this), new Sheet.OnText() {
            @Override public void onText(String text) {
                Settings.setSignature(MainActivity.this, text);
                if (settings != null) settings.rebuild();
                drawer.rebuild();
            }
        });
    }

    @Override
    public void editLanguage() {
        final int[] ids = {Str.ZH, Str.TW, Str.EN, Str.JA, Str.KO};
        String[] names = new String[ids.length];
        for (int i = 0; i < ids.length; i++) names[i] = Lang.name(ids[i]);
        Runnable[] acts = new Runnable[ids.length];
        for (int i = 0; i < ids.length; i++) {
            final int id = ids[i];
            acts[i] = new Runnable() {
                @Override public void run() {
                    Settings.setLang(MainActivity.this, id);
                    toast(Str.s("restart_hint"));
                    if (settings != null) settings.rebuild();
                }
            };
        }
        Sheet.menu(sheetLayer, Str.s("language"), names, acts);
    }


    // ==================== 图片 ====================

    @Override
    public void pickImages(EditorScreen s) {
        pickAvatar = false;
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(i, REQ_IMAGES);
        } catch (Exception e) {
            toast(Str.s("no_album"));
        }
    }

    @Override
    public void previewImage(String path) {
        final FrameLayout layer = new FrameLayout(this);
        layer.setBackgroundColor(0xF0070A16);
        layer.setAlpha(0f);
        layer.setClickable(true);
        final ZoomImageView iv = new ZoomImageView(this);
        zoomView = iv;
        iv.onZoomOut = new Runnable() {
            @Override public void run() {
                if (layer.getParent() != null) {
                    zoomView = null;
                    layer.animate().alpha(0f).setDuration(160).withEndAction(new Runnable() {
                        @Override public void run() { sheetLayer.removeView(layer); }
                    }).start();
                }
            }
        };
        layer.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout close = new FrameLayout(this);
        close.addView(new Ic(this, Ic.CLOSE, 0xFFF3F6FF),
                new FrameLayout.LayoutParams(Ui.dpi(20), Ui.dpi(20), Gravity.CENTER));
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(Ui.dpi(46), Ui.dpi(46),
                Gravity.TOP | Gravity.END);
        clp.topMargin = insetTop + Ui.dpi(4);
        clp.rightMargin = Ui.dpi(10);
        layer.addView(close, clp);
        Ui.tap(close, new Runnable() {
            @Override public void run() {
                zoomView = null;
                layer.animate().alpha(0f).setDuration(160).withEndAction(new Runnable() {
                    @Override public void run() { sheetLayer.removeView(layer); }
                }).start();
            }
        });
        sheetLayer.addView(layer, new FrameLayout.LayoutParams(-1, -1));
        layer.animate().alpha(1f).setDuration(200).start();
        Pics.load(path, 1600, new Pics.Callback() {
            @Override public void onBitmap(Bitmap b) {
                if (b != null) iv.setBitmap(b);
            }
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null) return;
        if (req == REQ_EXPORT) {
            final Uri uri = data.getData();
            if (uri == null) return;
            new Thread(new Runnable() {
                @Override public void run() {
                    boolean ok = false;
                    try {
                        java.io.OutputStream out = getContentResolver().openOutputStream(uri);
                        Backup.exportTo(MainActivity.this, out);
                        out.close();
                        ok = true;
                    } catch (Throwable ignored) { }
                    final boolean done = ok;
                    runOnUiThread(new Runnable() {
                        @Override public void run() { toast(Str.s(done ? "export_done" : "export_failed")); }
                    });
                }
            }).start();
            return;
        }
        if (req == REQ_IMPORT) {
            final Uri uri = data.getData();
            if (uri == null) return;
            new Thread(new Runnable() {
                @Override public void run() {
                    int n = -1;
                    try {
                        java.io.InputStream in = getContentResolver().openInputStream(uri);
                        n = Backup.importFrom(MainActivity.this, in);
                        in.close();
                    } catch (Throwable ignored) { }
                    final int count = n;
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            if (count < 0) {
                                toast(Str.s("import_failed"));
                                return;
                            }
                            toast(Str.f("import_done", count));
                            showRestartPage();
                            list.reload(true);
                            drawer.rebuild();
                            if (settings != null) settings.rebuild();
                        }
                    });
                }
            }).start();
            return;
        }


        if (req == REQ_AVATAR) {
            Uri uri = data.getData();
            if (uri == null) return;
            String path = copyImage(uri, "avatar_" + System.currentTimeMillis());
            if (path != null) {
                Settings.setAvatar(this, path);
                drawer.rebuild();
                if (settings != null) settings.rebuild();
            }
            return;
        }
        if (req != REQ_IMAGES) return;

        final List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            ClipData cd = data.getClipData();
            for (int i = 0; i < cd.getItemCount(); i++) uris.add(cd.getItemAt(i).getUri());
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        if (uris.isEmpty()) return;
        final EditorScreen target = editor;
        if (target == null) return;
        new Thread(new Runnable() {
            @Override public void run() {
                final List<String> paths = new ArrayList<>();
                for (int i = 0; i < uris.size(); i++) {
                    String p = copyImage(uris.get(i), "p_" + System.currentTimeMillis() + "_" + i);
                    if (p != null) paths.add(p);
                }
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (paths.isEmpty()) {
                            toast(Str.s("image_failed"));
                            return;
                        }
                        if (target.getParent() != null) target.addImageFiles(paths);
                    }
                });
            }
        }).start();
    }

    private String copyImage(Uri uri, String name) {
        File dir = new File(getFilesDir(), "pics");
        if (!dir.exists() && !dir.mkdirs()) return null;
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            File f = new File(dir, name + ".jpg");
            out = new FileOutputStream(f);
            byte[] buf = new byte[16 * 1024];
            int r;
            while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
            out.flush();
            return f.getAbsolutePath();
        } catch (Exception e) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) { }
            try { if (out != null) out.close(); } catch (Exception ignored) { }
        }
    }

    // ==================== 定位（仅本地精确定位） ====================

    @Override
    public void pickLocation(final EditorScreen s) {
        pendingLocation = s;
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            return;
        }
        locationSheet = Sheet.message(sheetLayer, Str.s("locating"), Str.s("locating_body"));
        locate(s);
    }

    private void locate(final EditorScreen target) {
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) { locationFailed(); return; }
        Location last = null;
        try {
            for (String p : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (last == null || l.getTime() > last.getTime())) last = l;
            }
        } catch (SecurityException ignored) { }
        if (last != null && System.currentTimeMillis() - last.getTime() < 3 * 60 * 1000) {
            resolve(last, target);
            return;
        }
        final Handler h = new Handler(Looper.getMainLooper());
        final boolean[] done = new boolean[]{false};
        locListener = new LocationListener() {
            @Override public void onLocationChanged(Location l) {
                if (done[0]) return;
                done[0] = true;
                cleanupLocation(lm);
                resolve(l, target);
            }
            @Override public void onStatusChanged(String p, int s, Bundle b) { }
            @Override public void onProviderEnabled(String p) { }
            @Override public void onProviderDisabled(String p) { }
        };
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener, Looper.getMainLooper());
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locListener, Looper.getMainLooper());
        } catch (SecurityException e) { locationFailed(); return; }
        h.postDelayed(new Runnable() {
            @Override public void run() {
                if (done[0]) return;
                done[0] = true;
                cleanupLocation(lm);
                locationFailed();
            }
        }, 15000);
    }

    private void cleanupLocation(LocationManager lm) {
        if (locListener != null) {
            try { lm.removeUpdates(locListener); } catch (Exception ignored) { }
            locListener = null;
        }
    }

    private void resolve(final Location loc, final EditorScreen target) {
        new Thread(new Runnable() {
            @Override public void run() {
                String name = null;
                try {
                    Geocoder g = new Geocoder(MainActivity.this, Locale.CHINA);
                    List<Address> as = g.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                    if (as != null && !as.isEmpty()) {
                        Address a = as.get(0);
                        String line = a.getAddressLine(0);
                        if (line != null && !line.trim().isEmpty()) name = line.trim();
                        else if (a.getFeatureName() != null) name = a.getFeatureName();
                    }
                } catch (Throwable ignored) { }
                if (name == null || name.isEmpty()) {
                    name = String.format(Locale.CHINA, "%.5f, %.5f", loc.getLatitude(), loc.getLongitude());
                }
                final String result = name;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (locationSheet != null) { locationSheet.dismiss(); locationSheet = null; }
                        if (target.getParent() != null) {
                            target.applyLocation(result, loc.getLatitude(), loc.getLongitude());
                        }
                    }
                });
            }
        }).start();
    }

    private void locationFailed() {
        if (locationSheet != null) { locationSheet.dismiss(); locationSheet = null; }
        Sheet.confirm(sheetLayer, Str.s("loc_failed"),
                Str.s("loc_failed_body"),
                Str.s("retry"), false, new Runnable() {
                    @Override public void run() {
                        if (pendingLocation != null) {
                            locationSheet = Sheet.message(sheetLayer, Str.s("locating"), Str.s("locating_body"));
                            locate(pendingLocation);
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] grants) {
        super.onRequestPermissionsResult(req, perms, grants);
        if (req != REQ_LOCATION) return;
        boolean ok = grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED;
        if (ok && pendingLocation != null) {
            locationSheet = Sheet.message(sheetLayer, Str.s("locating"), Str.s("locating_body"));
            locate(pendingLocation);
        } else {
            Sheet.confirm(sheetLayer, Str.s("loc_perm"), Str.s("loc_perm_body"),
                    Str.s("go_settings"), false, new Runnable() {
                        @Override public void run() {
                            try {
                                startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + getPackageName())));
                            } catch (Exception ignored) { }
                        }
                    });
        }
    }

    // ==================== 其他 ====================

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private boolean backupChecked;

    @Override
    protected void onResume() {
        super.onResume();
        if (!backupChecked) {
            backupChecked = true;
            Backup.maybeAuto(this, false);
        if (!Settings.langChosen(this)) { showFirstRunLang(); return; }
        }
    }

    /** 首次启动：先让用户选语言 */
    private void showFirstRunLang() {
        if (findViewById(91001) != null) return;
        FrameLayout ov = new FrameLayout(this);
        ov.setId(91001);
        ov.setBackgroundColor(Ui.BG);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(Ui.dpi(32), 0, Ui.dpi(32), 0);
        ov.addView(box, new FrameLayout.LayoutParams(-1, -1));

        TextView t = new TextView(this);
        t.setText("Language · 语言 · 言語 · 언어");
        t.setTextSize(17f);
        t.setTypeface(Ui.serifMed());
        t.setTextColor(Ui.TEXT);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        box.addView(t, new LinearLayout.LayoutParams(-1, -2));

        final int[] ids = {Str.ZH, Str.TW, Str.EN, Str.JA, Str.KO};
        for (int i = 0; i < ids.length; i++) {
            final int id = ids[i];
            TextView b = new TextView(this);
            b.setText(Lang.name(id));
            b.setTextSize(14f);
            b.setTextColor(Ui.TEXT);
            b.setGravity(Gravity.CENTER);
            b.setIncludeFontPadding(false);
            b.setPadding(0, Ui.dpi(13), 0, Ui.dpi(13));
            b.setBackground(new Segmented.FrostDrawable(Ui.dp(16)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = Ui.dpi(10);
            box.addView(b, lp);
            Ui.tap(b, new Runnable() {
                @Override public void run() {
                    Settings.setLang(MainActivity.this, id);
                    Settings.setLangChosen(MainActivity.this, true);
                    recreate();
                }
            });
        }

        TextView hint = new TextView(this);
        hint.setText(Str.s("lang_hint"));
        hint.setTextSize(11.5f);
        hint.setTextColor(Ui.TEXT_3);
        hint.setGravity(Gravity.CENTER);
        hint.setLineSpacing(Ui.dp(3f), 1f);
        hint.setIncludeFontPadding(false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = Ui.dpi(22);
        box.addView(hint, hp);

        sheetLayer.addView(ov, new FrameLayout.LayoutParams(-1, -1));
    }

    /** 导入完成后：整屏提示需要重启，仅一个确认按钮 */
    private void showRestartPage() {
        final FrameLayout ov = new FrameLayout(this);
        ov.setBackgroundColor(Ui.BG);
        ov.setClickable(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(Ui.dpi(36), 0, Ui.dpi(36), 0);
        ov.addView(box, new FrameLayout.LayoutParams(-1, -1));

        TextView t = new TextView(this);
        t.setText(Str.s("apply_title"));
        t.setTextSize(20f);
        t.setTypeface(Ui.serifMed());
        t.setTextColor(Ui.TEXT);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        box.addView(t, new LinearLayout.LayoutParams(-1, -2));

        TextView b = new TextView(this);
        b.setText(Str.s("apply_body"));
        b.setTextSize(13f);
        b.setLineSpacing(Ui.dp(6f), 1f);
        b.setTextColor(Ui.TEXT_2);
        b.setGravity(Gravity.CENTER);
        b.setIncludeFontPadding(false);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = Ui.dpi(18);
        box.addView(b, blp);

        TextView btn = new TextView(this);
        btn.setText(Str.s("apply_now"));
        btn.setTextSize(14f);
        btn.setTextColor(Ui.TEXT);
        btn.setGravity(Gravity.CENTER);
        btn.setIncludeFontPadding(false);
        btn.setPadding(0, Ui.dpi(14), 0, Ui.dpi(14));
        btn.setBackground(new Segmented.FrostDrawable(Ui.dp(16)));
        btn.setClickable(true);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.topMargin = Ui.dpi(34);
        box.addView(btn, clp);
        Ui.tap(btn, new Runnable() {
            @Override public void run() { recreate(); }
        });

        sheetLayer.addView(ov, new FrameLayout.LayoutParams(-1, -1));
    }

    @Override
    public void onBackPressed() {
        if (about != null) { about.goBack(); return; }
        if (data != null) { closeData(); return; }
        if (zoomView != null && zoomView.isZoomed()) {
            zoomView.resetZoom();   // 放大状态下"返回"= 还原缩放
            return;
        }
        if (Sheet.isShowing(sheetLayer)) { zoomView = null; Sheet.dismissTop(sheetLayer); return; }
        if (search != null) { closeSearch(); return; }
        if (settings != null) { closeSettings(); return; }
        if (editor != null) { closeEditor(editor); return; }
        if (trash != null) { closeTrash(); return; }
        if (drawerOpen || drawer.getTranslationX() > -drawerWidth + 1f) {
            animateDrawer(false);
            return;
        }
        super.onBackPressed();
    }

    // ==================== 手势根容器 ====================

    private class Root extends FrameLayout {
        private final int touchSlop;
        private final float edge;
        private float downX, downY, startTx, vel;
        private boolean watching, dragging;
        private android.view.VelocityTracker vt;

        Root(Context c) {
            super(c);
            touchSlop = ViewConfiguration.get(c).getScaledTouchSlop();
            edge = Ui.dp(28);
        }

        @Override
        public WindowInsets onApplyWindowInsets(WindowInsets insets) {
            onRootInsets(insets);
            return super.onApplyWindowInsets(insets);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent e) {
            if (sheetLayer != null && sheetLayer.getChildCount() > 0) return false;
            if (drawerAnim != null) return false;
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downX = e.getX();
                downY = e.getY();
                startTx = drawer.getTranslationX();
                watching = false;
                dragging = false;
                if (vt != null) vt.recycle();
                vt = android.view.VelocityTracker.obtain();
                vt.addMovement(e);
                // 只保留"打开态左滑关闭"；关闭态不再响应边缘右滑（只用左上角按钮打开）
                if (drawerOpen) watching = true;
                return false;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (vt != null) vt.addMovement(e);
                if (watching) {
                    float dx = e.getX() - downX, dy = e.getY() - downY;
                    float need = Math.max(touchSlop * 3f, Ui.dp(18));
                    if (drawerOpen && Math.abs(dx) > need && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                        watching = false;
                        dragging = true;
                        return true;
                    }
                }
                return dragging;
            }
            return dragging;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (vt != null) vt.addMovement(e);
                    if (dragging) {
                        setDrawerTx(startTx + (e.getX() - downX) * Ui.DRAG_FOLLOW);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (vt != null) {
                        vt.addMovement(e);
                        vt.computeCurrentVelocity(1000);
                        vel = vt.getXVelocity();
                        vt.recycle();
                        vt = null;
                    }
                    if (dragging) {
                        boolean open;
                        float moved = Math.abs(drawer.getTranslationX() - startTx);
                        if (moved > Ui.dp(40) && Math.abs(vel) > Ui.DRAWER_FLING) open = vel > 0f;
                        else open = drawer.getTranslationX() > -drawerWidth * 0.5f;
                        animateDrawer(open);
                        dragging = false;
                    }
                    return true;
                }
            }
            return true;
        }
    }
}
