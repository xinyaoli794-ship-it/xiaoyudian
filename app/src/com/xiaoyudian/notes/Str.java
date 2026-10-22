package com.xiaoyudian.notes;

import java.util.HashMap;
import java.util.Map;

/**
 * 界面文案表：简体中文 / 繁体中文 / English / 日本語 / 한국어
 * 由 Lang.apply() 在启动时写入 lang。
 */
public final class Str {

    public static final int ZH = 1, TW = 2, EN = 3, JA = 4, KO = 5;
    public static int lang = ZH;

    private static final Map<String, String[]> T = new HashMap<>();

    private static void p(String k, String zh, String tw, String en, String ja, String ko) {
        T.put(k, new String[]{zh, tw, en, ja, ko});
    }

    static {
        // ---------- 主界面 ----------
        p("all_notes", "全部便签", "全部便籤", "All notes", "すべてのメモ", "전체 메모");
        p("notes_count", "%d 条便签", "%d 條便籤", "%d notes", "%d 件のメモ", "%d개의 메모");
        p("empty_hint", "随手记下每一个念头", "隨手記下每一個念頭", "Jot down every thought",
                "思いついたことを書き留めよう", "떠오른 생각을 적어 두세요");
        p("category_count", "分类 · %d 条", "分類 · %d 條", "Folder · %d", "フォルダー · %d件", "폴더 · %d개");
        p("pinned", "置顶便签", "置頂便籤", "Pinned", "ピン留め", "고정됨");
        p("search_hint", "搜索便签", "搜尋便籤", "Search notes", "メモを検索", "메모 검색");
        p("back_to_top", "回到顶部", "回到頂部", "To top", "トップへ", "맨 위로");

        p("empty_all", "还没有便签", "還沒有便籤", "No notes yet", "メモはまだありません", "메모가 없습니다");
        p("empty_folder", "这个分类还是空的", "這個分類還是空的", "This folder is empty",
                "このフォルダーは空です", "이 폴더는 비어 있습니다");
        p("search_content", "搜索便签内容", "搜尋便籤內容", "Search notes", "メモを検索", "메모 검색");
        p("no_result", "没有找到相关便签", "沒有找到相關便籤", "No matching notes",
                "該当するメモがありません", "일치하는 메모가 없습니다");
        p("restore", "还原", "還原", "Restore", "復元", "복원");
        p("delete_forever", "彻底删除", "徹底刪除", "Delete permanently", "完全に削除", "완전 삭제");
        p("clear_all", "清空", "清空", "Empty", "空にする", "비우기");
        p("trash_hint", "删除的便签会留在这里，可随时还原", "刪除的便籤會留在這裡，可隨時還原",
                "Deleted notes stay here until you restore them",
                "削除したメモはここに残り、いつでも復元できます", "삭제한 메모는 여기에 남아 복원할 수 있습니다");
        p("trash_empty", "回收站是空的", "垃圾桶是空的", "Trash is empty", "ゴミ箱は空です", "휴지통이 비어 있습니다");

        // ---------- 抽屉 ----------
        p("new_folder", "新建分类", "新增分類", "New folder", "フォルダー作成", "새 폴더");
        p("trash", "回收站", "垃圾桶", "Trash", "ゴミ箱", "휴지통");
        p("folders", "分类", "分類", "Folders", "フォルダー", "폴더");
        p("local_only", "所有内容仅保存在本机", "所有內容僅保存在本機", "All data stays on this device",
                "すべて端末内に保存", "모든 내용은 기기에만 저장");
        p("default_signature", "本地便签 · 随手记", "本地便籤 · 隨手記", "Local notes · quick jots",
                "ローカルメモ · ひとこと", "로컬 메모 · 한 줄");
        p("change_nickname", "点头像换图片，点这里改昵称", "點頭像換圖片，點這裡改暱稱",
                "Tap the avatar to change it, tap here for a nickname",
                "アイコンをタップで画像変更、ここでニックネーム", "아이콘을 눌러 사진 변경, 여기를 눌러 닉네임");

        // ---------- 编辑器 ----------
        p("editor_hint", "写点什么…", "寫點什麼…", "Write something…", "何か書いてみましょう…", "무언가 적어 보세요…");
        p("last_update", "最后更新", "最後更新", "Updated ", "最終更新", "마지막 수정 ");
        p("chars", "%d 字", "%d 字", "%d chars", "%d 文字", "%d자");
        p("pin_note", "置顶便签", "置頂便籤", "Pin note", "ピン留め", "메모 고정");
        p("move_note", "移动便签", "移動便籤", "Move note", "移動", "메모 이동");
        p("delete_note", "删除便签", "刪除便籤", "Delete note", "削除", "메모 삭제");
        p("delete_loc", "删除定位", "刪除定位", "Remove location", "位置情報を削除", "위치 삭제");

        // ---------- 设置 ----------
        p("settings", "设置", "設定", "Settings", "設定", "설정");
        p("appearance", "界面外观", "介面外觀", "Appearance", "外観", "화면 설정");
        p("follow_system", "跟随系统", "跟隨系統", "System", "システム", "시스템");
        p("light", "浅色", "淺色", "Light", "ライト", "라이트");
        p("dark", "深色", "深色", "Dark", "ダーク", "다크");
        p("custom", "自定义", "自訂", "Custom", "カスタム", "사용자 지정");
        p("font_size", "字体大小", "字體大小", "Text size", "文字サイズ", "글자 크기");
        p("language", "语言", "語言", "Language", "言語", "언어");
        p("about", "关于", "關於", "About", "このアプリについて", "정보");
        p("about_entry", "关于小雨点", "關於小雨點", "About Xiaoyudian", "小雨点について", "샤오위뎬 정보");
        p("preview", "预览", "預覽", "Preview", "プレビュー", "미리보기");
        p("signature", "个性签名", "個性簽名", "Signature", "ひとこと", "소개 문구");
        p("signature_hint", "最多 14 字", "最多 14 字", "Up to 14 characters", "14 文字まで", "14자까지");
        p("nickname", "昵称", "暱稱", "Nickname", "ニックネーム", "닉네임");

        p("about_intro", "小雨点 —— 记录每一个念头。\n纯本地运行，不申请网络权限，数据只保存在这台设备上。\n支持富文本、插图与位置标注，并可随时导出备份。",
                "小雨點 —— 記錄每一個念頭。\n純本地執行，不申請網路權限，資料只保存在這台裝置上。\n支援富文字、插圖與位置標註，並可隨時匯出備份。",
                "Xiaoyudian - a place for every passing thought.\nRuns fully offline with no network permission; your data stays on this device.\nRich text, images and location, with export and backup any time.",
                "小雨点 — ひとつひとつの思いを記録する場所。\n完全オフラインで動作し、ネットワーク権限を要求しません。データはこの端末にのみ保存されます。\n書式・画像・位置情報に対応し、いつでも書き出せます。",
                "샤오위뎬 — 떠오르는 생각을 기록하는 곳.\n완전히 오프라인으로 동작하며 네트워크 권한을 요청하지 않습니다. 데이터는 이 기기에만 저장됩니다.\n서식, 사진, 위치를 지원하고 언제든 내보낼 수 있습니다.");
        p("privacy_policy", "隐私政策", "隱私政策", "Privacy Policy", "プライバシーポリシー", "개인정보 처리방침");
        p("legal_lang_note", "以下条款以简体中文版本为准。", "以下條款以簡體中文版本為準。", "The Simplified Chinese version of these terms prevails.", "以下の条項は簡体字中国語版が優先します。", "아래 약관은 중국어 간체 버전이 우선합니다.");
        p("terms", "用户协议", "使用者協議", "Terms of Use", "利用規約", "이용약관");
        // ---------- 条款页：非中文界面的说明 ----------
        p("legal_title", "隐私政策与用户协议", "隱私權政策與服務條款",
                "Privacy Policy & Terms of Service", "プライバシーポリシーと利用規約",
                "개인정보 처리방침 및 이용약관");
        p("legal_body",
                "本应用为完全离线的本地笔记工具，不请求网络权限，不收集任何数据，也不包含任何第三方 SDK。",
                "本應用程式為完全離線的本地筆記工具，不請求網路權限，不收集任何資料，亦不包含任何第三方 SDK。",
                "This app is a completely offline local note-taking tool. It does not request network permissions, does not collect any data, and does not contain third-party SDKs.",
                "本アプリは完全にオフラインのローカルメモツールです。ネットワーク権限を要求せず、いかなるデータも収集せず、サードパーティの SDK も含まれていません。",
                "본 앱은 완전히 오프라인인 로컬 메모 도구입니다. 네트워크 권한을 요청하지 않으며, 어떠한 데이터도 수집하지 않고, 타사 SDK도 포함하지 않습니다.");
        p("legal_disclaimer",
                "完整法律条款以简体中文版本为准。",
                "完整法律條款以簡體中文提供；如有任何歧異，概以簡體中文版本為準。",
                "The complete legal terms are provided in Simplified Chinese. In case of any discrepancy, the Simplified Chinese version shall prevail.",
                "完全な法的規約は簡体字中国語で提供されます。相違が生じた場合は、簡体字中国語版が優先されます。",
                "전체 법적 약관은 중국어 간체로 제공됩니다. 차이가 있을 경우 중국어 간체 버전이 우선합니다.");
        p("legal_hint",
                "如需查看完整条款，请将语言设置为简体中文。",
                "如需查看完整條款，請將語言設定為簡體中文。",
                "Please set the language to Simplified Chinese to view the full terms.",
                "完全な規約を表示するには、言語を簡体字中国語に設定してください。",
                "전체 약관을 보시려면 언어를 중국어 간체로 설정해 주세요.");

        p("apply_title", "已导入备份", "已匯入備份", "Backup imported", "バックアップを復元しました", "백업을 가져왔습니다");
        p("apply_body", "备份中的语言、主题或字号与当前不同。是否立即重启应用以完全恢复备份中的设置？",
                "備份中的語言、主題或字號與目前不同。是否立即重新啟動應用程式，以完整還原備份中的設定？",
                "The backup's language, theme or text size differs from now. Restart the app to fully apply everything from the backup?",
                "バックアップの言語・テーマ・文字サイズが現在と異なります。再起動してバックアップの設定を完全に反映しますか？",
                "백업의 언어, 테마, 글자 크기가 현재와 다릅니다. 앱을 다시 시작해 백업 설정을 완전히 적용할까요?");
        p("apply_now", "立即重启", "立即重新啟動", "Restart now", "今すぐ再起動", "지금 다시 시작");

        p("rename", "重命名", "重新命名", "Rename", "名前を変更", "이름 변경");
        p("icon_color", "图标颜色", "圖示顏色", "Icon colour", "アイコンの色", "아이콘 색상");
        p("delete_folder", "删除分类", "刪除分類", "Delete folder", "フォルダーを削除", "폴더 삭제");
        p("delete_folder_q", "删除「%s」", "刪除「%s」", "Delete “%s”", "「%s」を削除", "“%s” 삭제");
        p("delete_folder_body", "分类里的便签会回到「全部便签」，不会被删除。",
                "分類裡的便籤會回到「全部便籤」，不會被刪除。",
                "Notes in this folder move back to All notes and are not deleted.",
                "このフォルダーのメモは「すべてのメモ」に戻り、削除されません。",
                "이 폴더의 메모는 ‘전체 메모’로 돌아가며 삭제되지 않습니다.");
        p("color_blue", "蓝", "藍", "Blue", "青", "파랑");
        p("color_teal", "青", "青", "Teal", "ティール", "청록");
        p("color_orange", "橙", "橙", "Orange", "オレンジ", "주황");
        p("color_pink", "粉", "粉", "Pink", "ピンク", "분홍");
        p("color_purple", "紫", "紫", "Purple", "紫", "보라");
        p("color_green", "绿", "綠", "Green", "緑", "초록");
        p("color_default", "默认", "預設", "Default", "既定", "기본");
        p("folder_new_title", "新建分类", "新增分類", "New folder", "フォルダー作成", "새 폴더");
        p("folder_rename_title", "重命名分类", "重新命名分類", "Rename folder", "フォルダー名を変更", "폴더 이름 변경");
        p("folder_name", "分类名称", "分類名稱", "Folder name", "フォルダー名", "폴더 이름");
        p("unpin", "取消置顶", "取消置頂", "Unpin", "ピン留め解除", "고정 해제");
        p("pin", "置顶", "置頂", "Pin", "ピン留め", "고정");
        p("move_to_folder", "移动到分类", "移動到分類", "Move to folder", "フォルダーへ移動", "폴더로 이동");
        p("copy_text", "复制文字", "複製文字", "Copy text", "テキストをコピー", "텍스트 복사");
        p("pinned_toast", "已置顶", "已置頂", "Pinned", "ピン留めしました", "고정했습니다");
        p("unpinned_toast", "已取消置顶", "已取消置頂", "Unpinned", "ピン留めを解除しました", "고정을 해제했습니다");
        p("uncategorized", "未分类", "未分類", "Unfiled", "未分類", "미분류");
        p("move_to", "移动到", "移動到", "Move to", "移動先", "이동");
        p("delete_note_body", "便签会移入回收站，可以随时还原。", "便籤會移入垃圾桶，可以隨時還原。",
                "The note goes to Trash and can be restored later.", "メモはゴミ箱に移動し、いつでも復元できます。",
                "메모는 휴지통으로 이동하며 언제든 복원할 수 있습니다.");
        p("copied_clip", "已复制到剪贴板", "已複製到剪貼簿", "Copied to clipboard", "クリップボードにコピーしました", "클립보드에 복사했습니다");
        p("no_album", "没有找到可用的相册", "找不到可用的相簿", "No gallery app found", "利用できるギャラリーがありません", "사용 가능한 갤러리가 없습니다");
        p("nickname_title", "修改昵称", "修改暱稱", "Nickname", "ニックネームを変更", "닉네임 변경");
        p("image_failed", "图片导入失败，请换一张试试", "圖片匯入失敗，請換一張試試", "Could not import the image, try another one", "画像を読み込めませんでした。別の画像をお試しください", "이미지를 가져오지 못했습니다. 다른 이미지를 시도해 주세요.");
        p("image_show_failed", "图片显示失败", "圖片顯示失敗", "Image failed to display", "画像を表示できません", "이미지를 표시할 수 없습니다");
        p("image_load_failed", "图片载入失败", "圖片載入失敗", "Image failed to load", "画像を読み込めません", "이미지를 불러올 수 없습니다");
        p("locating", "正在定位…", "正在定位…", "Locating…", "位置情報を取得中…", "위치 확인 중…");
        p("locating_body", "正在获取当前位置，请稍候片刻", "正在取得目前位置，請稍候", "Getting your current location, please wait", "現在地を取得しています。少々お待ちください", "현재 위치를 가져오는 중입니다. 잠시만 기다려 주세요.");
        p("loc_failed", "没能获取到位置", "無法取得位置", "Could not get the location", "位置情報を取得できませんでした", "위치를 가져오지 못했습니다");
        p("loc_failed_body", "只支持本地精确定位。请确认定位服务已开启、并在空旷处再试一次。",
                "僅支援本地精確定位。請確認定位服務已開啟，並在空曠處再試一次。",
                "Only on-device precise location is supported. Check that location services are on and try again outdoors.",
                "端末内の高精度測位のみ対応しています。位置情報サービスが有効か確認し、見通しのよい場所でもう一度お試しください。",
                "기기 내 정밀 위치만 지원합니다. 위치 서비스가 켜져 있는지 확인하고 탁 트인 곳에서 다시 시도해 주세요.");
        p("retry", "重试", "重試", "Retry", "再試行", "다시 시도");
        p("loc_perm", "需要定位权限", "需要定位權限", "Location permission needed", "位置情報の権限が必要です", "위치 권한이 필요합니다");
        p("loc_perm_body", "允许定位后才能记录「当前位置」。", "允許定位後才能記錄「目前位置」。",
                "Allow location to record your current position.", "許可すると「現在地」を記録できます。", "권한을 허용해야 ‘현재 위치’를 기록할 수 있습니다.");
        p("go_settings", "去设置", "前往設定", "Open settings", "設定を開く", "설정 열기");
        p("clear_trash", "清空回收站", "清空垃圾桶", "Empty trash", "ゴミ箱を空にする", "휴지통 비우기");
        p("clear_trash_body", "这里的内容将被彻底删除，无法恢复。", "這裡的內容將被徹底刪除，無法復原。",
                "Everything here will be permanently deleted.", "ここの内容は完全に削除され、復元できません。", "여기의 내용은 완전히 삭제되어 복구할 수 없습니다.");
        p("note_image", "图片便签", "圖片便籤", "Image note", "画像メモ", "사진 메모");
        p("note_empty", "空便签", "空便籤", "Empty note", "空のメモ", "빈 메모");
        p("note_image_br", "（图片便签）", "（圖片便籤）", "(Image note)", "（画像メモ）", "(사진 메모)");
        p("default_nick", "雨点", "雨點", "Droplet", "しずく", "빗방울");
        p("lang_hint", "之后可在「设置 › 语言」中随时更改", "之後可在「設定 › 語言」中隨時變更",
                "You can change this later in Settings › Language", "あとで「設定 › 言語」から変更できます",
                "나중에 ‘설정 › 언어’에서 변경할 수 있습니다");
        p("back", "返回", "返回", "Back", "戻る", "뒤로");
        p("copied", "邮箱已复制", "信箱已複製", "Email copied", "メールをコピーしました", "이메일을 복사했습니다");
        p("current_version", "当前版本：", "目前版本：", "Version: ", "バージョン：", "버전: ");

        // ---------- 数据管理 ----------
        p("data_mgmt", "数据管理", "資料管理", "Data", "データ管理", "데이터 관리");
        p("export", "导出", "匯出", "Export", "エクスポート", "내보내기");
        p("export_all", "一键导出全部数据", "一鍵匯出全部資料", "Export all data",
                "すべてのデータを書き出す", "모든 데이터 내보내기");
        p("export_hint", "包含便签正文、格式、时间戳、图片、定位与分类，导出后可直接复制到其它设备。",
                "包含便籤正文、格式、時間戳、圖片、定位與分類，匯出後可直接複製到其它裝置。",
                "Includes text, formatting, timestamps, images, location and folders. Copy the file to another device to restore.",
                "本文・書式・日時・画像・位置情報・フォルダーを含みます。他の端末にコピーして復元できます。",
                "본문, 서식, 시간, 사진, 위치, 폴더가 포함됩니다. 다른 기기로 복사해 복원할 수 있습니다.");
        p("import", "导入", "匯入", "Import", "インポート", "가져오기");
        p("import_all", "从备份文件导入", "從備份檔案匯入", "Import from a backup",
                "バックアップから復元", "백업에서 가져오기");
        p("import_hint", "导入会完整覆盖当前所有数据，请确认已有备份。",
                "匯入會完整覆蓋目前所有資料，請確認已有備份。",
                "Importing replaces all current data. Make sure you have a backup first.",
                "インポートは現在のデータをすべて置き換えます。先にバックアップを確認してください。",
                "가져오기는 현재 모든 데이터를 덮어씁니다. 먼저 백업을 확인하세요.");
        p("auto_backup", "自动备份", "自動備份", "Auto backup", "自動バックアップ", "자동 백업");
        p("backup_switch", "开启自动备份", "開啟自動備份", "Enable auto backup",
                "自動バックアップを有効化", "자동 백업 사용");
        p("off", "关", "關", "Off", "オフ", "끔");
        p("on", "开", "開", "On", "オン", "켬");
        p("backup_mode", "备份时机", "備份時機", "When", "タイミング", "시점");
        p("backup_record", "每次记录后", "每次記錄後", "After each edit", "記録のたび", "기록할 때마다");
        p("backup_launch", "进入应用时", "進入應用時", "On app open", "起動時", "앱 실행 시");
        p("backup_gap", "最短间隔（分钟）", "最短間隔（分鐘）", "Min interval (min)", "最短間隔（分）", "최소 간격(분)");
        p("last_backup", "上次备份", "上次備份", "Last backup", "前回のバックアップ", "마지막 백업");
        p("never", "尚未备份", "尚未備份", "never", "未実行", "없음");
        p("backup_overwrite_warn", "自动备份采用覆盖模式：只保留最新一份，旧备份会被替换。",
                "自動備份採用覆蓋模式：只保留最新一份，舊備份會被替換。",
                "Auto backup overwrites: only the newest copy is kept.",
                "自動バックアップは上書き方式です。最新の 1 件のみ保持されます。",
                "자동 백업은 덮어쓰기 방식으로, 최신 1개만 보관됩니다.");
        p("storage_warn", "自动备份保存在应用专属目录；导出/导入通过系统文件选择器，均不需要额外存储权限。卸载应用会一并删除应用目录内的备份，重要数据请另行导出。",
                "自動備份保存在應用專屬目錄；匯出/匯入透過系統檔案選擇器，均不需要額外儲存權限。解除安裝會一併刪除應用目錄內的備份，重要資料請另行匯出。",
                "Auto backups live in the app's private folder; export/import use the system file picker, so no extra storage permission is needed. Uninstalling removes app-folder backups - export anything important.",
                "自動バックアップはアプリ専用フォルダーに保存されます。書き出し／復元はシステムのファイル選択を使うため、追加のストレージ権限は不要です。アンインストールでアプリ内のバックアップも削除されます。",
                "자동 백업은 앱 전용 폴더에 저장됩니다. 내보내기/가져오기는 시스템 파일 선택기를 사용하므로 추가 저장 권한이 필요 없습니다. 앱을 삭제하면 앱 폴더의 백업도 함께 삭제됩니다.");
        p("backup_done", "备份完成", "備份完成", "Backup complete", "バックアップ完了", "백업 완료");
        p("export_done", "已导出", "已匯出", "Exported", "書き出しました", "내보냈습니다");
        p("import_done", "已导入 %d 条便签", "已匯入 %d 條便籤", "Imported %d notes",
                "%d 件を復元しました", "%d개를 가져왔습니다");
        p("import_failed", "导入失败", "匯入失敗", "Import failed", "復元に失敗しました", "가져오기 실패");
        p("export_failed", "导出失败", "匯出失敗", "Export failed", "書き出しに失敗", "내보내기 실패");

        // ---------- 通用 ----------
        p("ok", "知道啦", "知道了", "Got it", "OK", "확인");
        p("cancel", "取消", "取消", "Cancel", "キャンセル", "취소");
        p("delete", "删除", "刪除", "Delete", "削除", "삭제");
        p("restart_hint", "重启应用后生效", "重新啟動後生效", "Takes effect after restart",
                "再起動後に反映されます", "앱을 다시 시작하면 적용됩니다");

        // ---------- 预览示例文本 ----------
        p("preview_text", "当我无法安慰你，或你不再关怀我，请千万记住，\n在我们菲薄的流年里，曾有十二只白鹭鸶飞过秋天的湖泊。",
                "當我無法安慰你，或你不再關懷我，請千萬記住，\n在我們菲薄的流年裡，曾有十二隻白鷺鷥飛過秋天的湖泊。",
                "When I can no longer comfort you, or when you no longer care for me, please always remember: in the fleeting years of our fragile lives, twelve white egrets once flew over the autumn lake.",
                "私があなたを慰めることができなくなっても、あなたが私を思いやらなくなっても、どうか忘れないでください。私たちのあまりに短い歳月の中で、かつて十二羽の白鷺が秋の湖の上を飛び去ったことを。",
                "내가 더 이상 너를 위로해 줄 수 없게 되더라도, 네가 더 이상 나를 아끼지 않게 되더라도, 제발 잊지 말아 줘. 우리의 덧없이 짧은 세월 속에서, 한때 열두 마리 백로가 가을 호수를 날아갔던 것을.");

        p("font_s", "小", "小", "S", "小", "작게");
        p("font_m", "标准", "標準", "M", "標準", "보통");
        p("font_l", "大", "大", "L", "大", "크게");
        p("font_xl", "特大", "特大", "XL", "特大", "아주 크게");
        p("sample_text", "打开雨点，记下此刻。", "打開雨點，記下此刻。",
                "Open a drop, note this moment.", "雨粒を開いて、今を記す。", "빗방울을 열어 지금을 적어 보세요.");
        p("local_save", "本地保存，不联网", "本地保存，不聯網", "Saved locally, no network",
                "ローカル保存・ネット接続なし", "로컬 저장, 네트워크 없음");

        // ---------- 关于正文 ----------
        p("about_body",
                "小雨点是一个面向广大笔记用户的简约风格APP，为个人制作，目前尚无联网功能，所有内容均保存本地，注重隐私安全。\n\n如有任何建议或意见，可反馈邮箱2868684570@qq.com，感谢体验",
                "小雨點是一個面向廣大筆記用戶的簡約風格APP，為個人製作，目前尚無聯網功能，所有內容均保存本地，注重隱私安全。\n\n如有任何建議或意見，可回饋郵箱2868684570@qq.com，感謝體驗",
                "Xiaoyudian is a minimal note-taking app made by an individual for note users. It has no network features; everything is stored locally with privacy in mind.\n\nFor any suggestions, email 2868684570@qq.com. Thanks for trying it.",
                "小雨点は、ノートを取るすべての人に向けたシンプルなメモアプリです。個人が制作しており、ネット接続機能はなく、すべての内容は端末内に保存され、プライバシーを重視しています。\n\nご意見・ご提案は 2868684570@qq.com まで。ご利用ありがとうございます。",
                "샤오위뎬은 메모 사용자를 위한 미니멀 스타일 앱으로, 개인이 제작했습니다. 네트워크 기능이 없으며 모든 내용은 기기에 저장되어 개인정보를 중요하게 다룹니다.\n\n의견이나 제안은 2868684570@qq.com 으로 보내 주세요. 이용해 주셔서 감사합니다.");
    }

    public static String s(String key) {
        String[] a = T.get(key);
        if (a == null) return key;
        int i = Math.max(0, Math.min(a.length - 1, lang - 1));
        return a[i];
    }

    public static String f(String key, Object... args) {
        try {
            return String.format(s(key), args);
        } catch (Throwable t) {
            return s(key);
        }
    }

    private Str() { }
}
