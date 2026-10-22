param(
    [switch]$Clean,
    [switch]$Icons
)
$ErrorActionPreference = "Continue"
$root = $PSScriptRoot
$app  = Join-Path $root "app"
$out  = Join-Path $root "build"
$tc   = Join-Path $root "toolchain"
$jdk  = Join-Path $tc "jdk"
$bt   = Join-Path $tc "sdk\build-tools\35.0.1"
$aj   = Join-Path $tc "sdk\platforms\android-35\android.jar"
$py   = "C:\Users\Lenovo\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"

$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$bt;$env:PATH"
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if ($Clean) { if (Test-Path $out) { Remove-Item $out -Recurse -Force } }
foreach ($d in @("gen", "classes", "dex", "res")) {
    New-Item -ItemType Directory -Force -Path (Join-Path $out $d) | Out-Null
}

function Step($name) { Write-Host "`n>>> $name" -ForegroundColor Cyan }
function Check($what) {
    if ($LASTEXITCODE -ne 0) { Write-Host "$what FAILED (exit $LASTEXITCODE)" -ForegroundColor Red; throw "$what failed" }
}

if ($Icons -or -not (Test-Path (Join-Path $app "res\mipmap-xxhdpi\ic_launcher.png"))) {
    Step "生成图标"
    & $py (Join-Path $root "tools\icons.py") (Join-Path $app "res") 2>&1
    Check "icons"
}

Step "aapt2 compile"
Remove-Item (Join-Path $out "res\res.zip") -ErrorAction SilentlyContinue
& "$bt\aapt2.exe" compile --dir (Join-Path $app "res") -o (Join-Path $out "res\res.zip") 2>&1
Check "aapt2 compile"

Step "aapt2 link"
Remove-Item (Join-Path $out "res\base.apk") -ErrorAction SilentlyContinue
& "$bt\aapt2.exe" link -o (Join-Path $out "res\base.apk") -I $aj `
    --manifest (Join-Path $app "AndroidManifest.xml") `
    --min-sdk-version 24 --target-sdk-version 35 `
    --version-code 33 --version-name "1.0.0" `
    -R (Join-Path $out "res\res.zip") --java (Join-Path $out "gen") --auto-add-overlay 2>&1
Check "aapt2 link"

Step "javac"
Remove-Item (Join-Path $out "classes\*") -Recurse -Force -ErrorAction SilentlyContinue
$srcs = @()
$srcs += Get-ChildItem -Recurse -Filter *.java -Path (Join-Path $app "src") | ForEach-Object { $_.FullName }
$srcs += Get-ChildItem -Recurse -Filter *.java -Path (Join-Path $out "gen") | ForEach-Object { $_.FullName }
$argfile = Join-Path $out "sources.txt"
$lines = $srcs | ForEach-Object { '"' + ($_ -replace '\\', '/') + '"' }
[System.IO.File]::WriteAllLines($argfile, $lines, (New-Object System.Text.UTF8Encoding($false)))
& "$jdk\bin\javac.exe" -encoding UTF-8 --release 11 -nowarn -Xlint:none `
    -cp $aj -d (Join-Path $out "classes") "@$argfile" 2>&1
Check "javac"

Step "jar"
Remove-Item (Join-Path $out "app2.jar") -ErrorAction SilentlyContinue
& "$jdk\bin\jar.exe" cf (Join-Path $out "app2.jar") -C (Join-Path $out "classes") . 2>&1
Check "jar"

Step "d8"
Remove-Item (Join-Path $out "dex\*") -Recurse -Force -ErrorAction SilentlyContinue
& "$bt\d8.bat" --release --min-api 24 --lib $aj --output (Join-Path $out "dex") (Join-Path $out "app2.jar") 2>&1
Check "d8"

Step "打包"
& $py (Join-Path $root "tools\pack.py") (Join-Path $out "res\base.apk") (Join-Path $out "dex") (Join-Path $out "unsigned.apk") 2>&1
Check "pack"

Step "zipalign"
Remove-Item (Join-Path $out "aligned.apk") -ErrorAction SilentlyContinue
& "$bt\zipalign.exe" -f -p 4 (Join-Path $out "unsigned.apk") (Join-Path $out "aligned.apk") 2>&1
Check "zipalign"

Step "签名"
$ksPass = if ($env:KS_PASS) { $env:KS_PASS } else { "change-me" }
$ks = Join-Path $root "keystore\xiaoyudian.keystore"
if (-not (Test-Path $ks)) {
    New-Item -ItemType Directory -Force -Path (Split-Path $ks) | Out-Null
    & "$jdk\bin\keytool.exe" -genkeypair -v -keystore $ks -alias xiaoyudian `
        -keyalg RSA -keysize 2048 -validity 10950 `
        -storepass $ksPass -keypass $ksPass `
        -dname "CN=Xiaoyudian, O=Local, OU=Notes, L=CN, ST=CN, C=CN" 2>&1 | Out-Null
    Check "keytool"
}
$signed = Join-Path $out "signed.apk"
Remove-Item $signed -ErrorAction SilentlyContinue
& "$bt\apksigner.bat" sign --ks $ks --ks-key-alias xiaoyudian `
    --ks-pass pass:xiaoyudian --key-pass pass:xiaoyudian `
    --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true `
    --out $signed (Join-Path $out "aligned.apk") 2>&1
Check "apksigner"

Step "校验"
& "$bt\apksigner.bat" verify --print-certs $signed 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { throw "verify failed" }
Write-Host "签名校验通过" -ForegroundColor Green
& "$bt\aapt2.exe" dump badging $signed 2>&1 | Select-String -Pattern "package:|sdkVersion|application-label" | Select-Object -First 4

$final = Join-Path $root "小雨点.apk"
Copy-Item $signed $final -Force
$size = [math]::Round((Get-Item $final).Length / 1KB, 1)
Write-Host "`n=== 构建成功：$final ($size KB) ===" -ForegroundColor Green
