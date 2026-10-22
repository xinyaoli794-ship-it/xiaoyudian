# 把 dex 合入 aapt2 产出的 base.apk，并保证 resources.arsc 不被压缩（targetSdk>=30 的安装要求）
import sys, os, zipfile

base, dexdir, out = sys.argv[1], sys.argv[2], sys.argv[3]
if os.path.exists(out):
    os.remove(out)

with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out, "w") as zout:
    for item in zin.infolist():
        name = item.filename
        if name == "classes.dex" or name.startswith("META-INF/"):
            continue
        if item.is_dir():
            continue
        data = zin.read(name)
        zi = zipfile.ZipInfo(name, date_time=item.date_time)
        zi.external_attr = item.external_attr
        if name.endswith(".arsc"):
            zi.compress_type = zipfile.ZIP_STORED
        else:
            zi.compress_type = zipfile.ZIP_DEFLATED
        zout.writestr(zi, data)
    for f in sorted(os.listdir(dexdir)):
        if f.endswith(".dex"):
            zout.write(os.path.join(dexdir, f), f, compress_type=zipfile.ZIP_DEFLATED)
print("packed", out)
