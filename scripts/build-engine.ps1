# Builds the shogi engine used by the "AI" button in post-game review and puts it in
# app/src/main/jniLibs/<abi>/libyaneuraou.so.
#
#   engine : YaneuraOu (GPLv3)  https://github.com/yaneurao/YaneuraOu
#   eval   : Suisho5 (20211123), taken from the GPLv3 source bundle of
#            https://github.com/mizar/YaneuraOu/releases/tag/v7.6.3
#
# Requires: git, the Android NDK (Android Studio > SDK Manager > SDK Tools > NDK), Windows 10 or later.
# Usage:    powershell -ExecutionPolicy Bypass -File scripts\build-engine.ps1
#
# NOTE: keep this file ASCII only (Windows PowerShell 5.1 reads scripts without BOM as ANSI).
param(
    [string[]]$Abi = @("arm64-v8a", "x86_64"),
    [string]$WorkDir = (Join-Path $PSScriptRoot "..\build\engine")
)
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$EngineRepo = "https://github.com/yaneurao/YaneuraOu.git"
$EngineCommit = "c1b80eaa09fe13d5f12b1599d1ae4d53c224de30"
$EvalBundleUrl = "https://github.com/mizar/YaneuraOu/releases/download/v7.6.3/Suisho5-YaneuraOu-v7.6.3-android.zip"
$EvalSha256 = "768068F0D534A0603A5D38BCD143DE6BBCA820D5F1C95A14D40863E5B7892D76"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
New-Item -ItemType Directory -Force $WorkDir | Out-Null
$work = (Resolve-Path $WorkDir).Path

# ---- NDK ----
$sdk = $env:ANDROID_HOME
$props = Join-Path $root "local.properties"
if (Test-Path $props) {
    $line = Get-Content $props | Where-Object { $_ -match "^sdk\.dir=" } | Select-Object -First 1
    if ($line) { $sdk = ($line -replace "^sdk\.dir=", "") -replace "\\:", ":" -replace "\\\\", "\" }
}
if (-not $sdk -or -not (Test-Path "$sdk\ndk")) { throw "Android NDK not found. Install it from the SDK Manager." }
$ndk = Get-ChildItem "$sdk\ndk" -Directory | Sort-Object { [version]$_.Name } | Select-Object -Last 1
$ndkBuild = Join-Path $ndk.FullName "ndk-build.cmd"
"NDK: $($ndk.FullName)"

# ---- engine source ----
$src = Join-Path $work "YaneuraOu"
if (-not (Test-Path "$src\.git")) {
    git init -q $src
    git -C $src remote add origin $EngineRepo
}
git -C $src fetch -q --depth 1 origin $EngineCommit
git -C $src checkout -q -f FETCH_HEAD
if ($LASTEXITCODE -ne 0) { throw "could not check out the engine source" }

# Recent clang rejects -mfpu on arm64 (NEON is always available there).
$mk = Join-Path $src "script\jni\Android.mk"
$text = [IO.File]::ReadAllText($mk)
$text = $text.Replace("CPPFLAGS += -DIS_64BIT -DUSE_NEON -mfpu=neon", "CPPFLAGS += -DIS_64BIT -DUSE_NEON")
[IO.File]::WriteAllText($mk, $text)

# ---- eval file (nn.bin) ----
$evalDir = Join-Path $work "eval"
$nn = Join-Path $evalDir "nn.bin"
New-Item -ItemType Directory -Force $evalDir | Out-Null
if (-not (Test-Path $nn) -or (Get-FileHash $nn -Algorithm SHA256).Hash -ne $EvalSha256) {
    $bundle = Join-Path $work "suisho5"
    $zip = Join-Path $work "suisho5.zip"
    if (-not (Test-Path $zip)) {
        "Downloading the Suisho5 bundle (about 130MB)..."
        Invoke-WebRequest $EvalBundleUrl -OutFile $zip
    }
    if (Test-Path $bundle) { Remove-Item -Recurse -Force $bundle }
    New-Item -ItemType Directory -Force $bundle | Out-Null
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($zip)
    try {
        $entry = $archive.Entries | Where-Object { $_.FullName -eq "source.tar.xz" }
        [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, (Join-Path $bundle "source.tar.xz"), $true)
    } finally {
        $archive.Dispose()
    }
    "Extracting the eval parameters..."
    # The bundle ships the parameters as a C++ string literal ("\x16\x2f..."); turn it back into nn.bin.
    Push-Location $bundle
    & "$env:SystemRoot\System32\tar.exe" -xf source.tar.xz source/eval/nnue/embedded_nnue.cpp
    Pop-Location
    if ($LASTEXITCODE -ne 0) { throw "could not extract source.tar.xz" }
    Add-Type -TypeDefinition @'
using System;
using System.IO;
public static class Unembed {
    static int Hex(int c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }
    public static long Run(string src, string dst) {
        long n = 0;
        using (var input = new BufferedStream(File.OpenRead(src), 1 << 20))
        using (var output = new BufferedStream(File.Create(dst), 1 << 20)) {
            int c;
            bool inString = false;
            while ((c = input.ReadByte()) >= 0) {
                if (c == '"') { inString = !inString; continue; }
                if (!inString || c != '\\') continue;
                if (input.ReadByte() != 'x') throw new Exception("unexpected escape in embedded_nnue.cpp");
                int hi = Hex(input.ReadByte()), lo = Hex(input.ReadByte());
                if (hi < 0 || lo < 0) throw new Exception("bad hex digit in embedded_nnue.cpp");
                output.WriteByte((byte)(hi * 16 + lo));
                n++;
            }
        }
        return n;
    }
}
'@
    [Unembed]::Run((Join-Path $bundle "source\eval\nnue\embedded_nnue.cpp"), $nn) | Out-Null
    if ((Get-FileHash $nn -Algorithm SHA256).Hash -ne $EvalSha256) { throw "nn.bin does not match the expected checksum" }
    Remove-Item -Recurse -Force $bundle
}

# ---- build (the eval file is embedded into the executable) ----
"Building the engine for: $($Abi -join ', ')"
Remove-Item -Recurse -Force "$src\obj", "$src\libs" -ErrorAction SilentlyContinue
$jobs = [Math]::Min([int]$env:NUMBER_OF_PROCESSORS, 16)
$ErrorActionPreference = "Continue"
# The compiler looks for nn.bin (.incbin) in the current directory.
Push-Location $evalDir
& $ndkBuild "NDK_PROJECT_PATH=$src" "APP_BUILD_SCRIPT=$mk" "NDK_APPLICATION_MK=$src\script\jni\Application.mk" `
    "APP_ABI=$($Abi -join ' ')" "APP_PLATFORM=android-24" "APP_SUPPORT_FLEXIBLE_PAGE_SIZES=true" `
    -j $jobs "YANEURAOU_EDITION=YANEURAOU_ENGINE_NNUE" "EXTRA_CPPFLAGS=-DNNUE_EMBEDDING" |
    Select-String "Install|error:"
Pop-Location
$ErrorActionPreference = "Stop"

# Android only lets an app execute files that were installed as native libraries, hence the lib*.so name.
foreach ($a in $Abi) {
    $built = Join-Path $src "libs\$a\YaneuraOu_NNUE_$a"
    if (-not (Test-Path $built)) { throw "build failed for $a" }
    $dest = Join-Path $root "app\src\main\jniLibs\$a"
    New-Item -ItemType Directory -Force $dest | Out-Null
    Copy-Item $built (Join-Path $dest "libyaneuraou.so") -Force
    "{0}: {1:N1} MB" -f (Join-Path $dest "libyaneuraou.so"), ((Get-Item $built).Length / 1MB)
}
"Done. Rebuild the app to include the engine."
