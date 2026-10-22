# 生成 "小雨点" 图标：从 tools/icon_master.png 输出各密度资源
# 关键：图案不铺满画布，四周留出安全边距，避免被系统裁切后显得"过度放大"
import os
import sys
from PIL import Image, ImageDraw, ImageFilter

OUT = sys.argv[1]
HERE = os.path.dirname(os.path.abspath(__file__))
MASTER = os.path.join(HERE, "icon_master.png")
BG = (59, 49, 77)          # 图标自带背景的深紫
LEGACY_FIT = 0.88          # 旧版图标里图案占画布比例
FG_FIT = 0.78              # 自适应前景里图案占画布比例（遮罩会裁掉四周）


def load_master():
    img = Image.open(MASTER).convert("RGB")
    side = min(img.size)
    left = (img.size[0] - side) // 2
    top = (img.size[1] - side) // 2
    img = img.crop((left, top, left + side, top + side))
    return img.resize((1024, 1024), Image.LANCZOS)


def place(img, canvas_size, fit, bg=None):
    """把图案按 fit 比例居中放进画布。
    留白处用图案自身的放大模糊版填充，避免出现一圈色差边框。"""
    inner = max(1, int(canvas_size * fit))
    if bg is None:
        canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    else:
        canvas = Image.new("RGBA", (canvas_size, canvas_size), bg + (255,))
    art = img.resize((inner, inner), Image.LANCZOS).convert("RGBA")
    if fit < 1.0:
        # 用整体放大 + 模糊的画面垫底，边缘颜色与图案本身连续
        pad = img.resize((canvas_size, canvas_size), Image.LANCZOS).convert("RGBA")
        pad = pad.filter(ImageFilter.GaussianBlur(max(1.0, canvas_size * 0.03)))
        if bg is not None:
            pad = Image.alpha_composite(
                Image.new("RGBA", (canvas_size, canvas_size), bg + (255,)), pad)
        canvas = pad if bg is None else Image.alpha_composite(canvas, pad)
    off = (canvas_size - inner) // 2
    if fit < 1.0:
        # 图案边缘做羽化，和垫底自然融合
        m = Image.new("L", (inner * 4, inner * 4), 0)
        ImageDraw.Draw(m).rounded_rectangle(
            [0, 0, inner * 4 - 1, inner * 4 - 1], radius=int(inner * 4 * 0.20), fill=255)
        m = m.resize((inner, inner), Image.LANCZOS)
        m = m.filter(ImageFilter.GaussianBlur(max(1.0, inner * 0.018)))
        canvas.paste(art, (off, off), m)
    else:
        canvas.paste(art, (off, off), art)
    return canvas


def mask_shape(img, kind, ratio=0.225):
    size = img.size[0]
    big = size * 4
    mask = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(mask)
    if kind == "round":
        d.ellipse([0, 0, big - 1, big - 1], fill=255)
    else:
        d.rounded_rectangle([0, 0, big - 1, big - 1], radius=int(big * ratio), fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


master = load_master()
os.makedirs(OUT, exist_ok=True)

# 旧版图标（48/72/96/144/192）与圆形图标
legacy = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for name, size in legacy.items():
    d = os.path.join(OUT, "mipmap-" + name)
    os.makedirs(d, exist_ok=True)
    base = place(master, size, LEGACY_FIT, BG)
    mask_shape(base, "square").save(os.path.join(d, "ic_launcher.webp"), "WEBP", quality=92, method=6)
    mask_shape(base, "round").save(os.path.join(d, "ic_launcher_round.webp"), "WEBP", quality=92, method=6)

# 自适应图标前景：108dp，透明底 + 居中图案（底色由 background 层提供）
fg = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
for name, size in fg.items():
    d = os.path.join(OUT, "mipmap-" + name)
    os.makedirs(d, exist_ok=True)
    place(master, size, FG_FIT).save(os.path.join(d, "ic_launcher_fg.webp"), "WEBP", quality=92, method=6)

# 展示图
mask_shape(place(master, 512, LEGACY_FIT, BG), "square").save(
    os.path.join(OUT, "icon_512.webp"), "WEBP", quality=93, method=6)

# 清掉旧的 PNG，避免同密度重复
for dirpath, _, files in os.walk(OUT):
    for f in files:
        if f.endswith(".png") and ("ic_launcher" in f or f == "icon_512.png"):
            try:
                os.remove(os.path.join(dirpath, f))
            except OSError:
                pass
print("icons ok")
