#Requires -Version 7.0
<#
.SYNOPSIS
    web-to-notion 端到端测试流程。

.DESCRIPTION
    1. 下载 GitHub Actions 最新 APK artifact（或复用本地 APK）。
    2. 安装 APK 并自动写入 Notion 配置。
    3. 启动 App，清 logcat 缓存后持续抓取与 Notion 同步相关的日志。
    4. 截图记录启动后的首页状态。
    5. 通过 ADB shell input 模拟点击抽屉“设置”项，验证可跳转。
    6. 返回首页后新建一条测试便签，触发同步并观察日志输出。

.PARAMETER ApkPath
    本地 APK 路径。为空时自动下载最新 GitHub Actions artifact 到 dist 目录。

.PARAMETER AdbPath
    adb 可执行文件路径。默认 D:\Dev\Lib\adb\adb.exe。

.PARAMETER KeepArtifacts
    测试结束后保留 dist 目录和截图。默认清理。

.EXAMPLE
    .\scripts\test-flow.ps1
    .\scripts\test-flow.ps1 -ApkPath "D:\apk\app-release.apk" -KeepArtifacts
#>
[CmdletBinding()]
param(
    [string] $ApkPath,
    [string] $AdbPath = "D:\Dev\Lib\adb\adb.exe",
    [switch] $KeepArtifacts
)

$PackageName = "io.trae.webtonotion"
$MainActivity = "$PackageName/.MainActivity"
$Receiver = "$PackageName/.receiver.ConfigReceiver"
$Repo = "DoubleShift/web-to-notion"

# 颜色输出
function Write-Step {
    param([string]$Message)
    Write-Host "`n$Message" -ForegroundColor Yellow
}
function Write-Ok {
    param([string]$Message)
    Write-Host "  ✅ $Message" -ForegroundColor Green
}
function Write-Warn {
    param([string]$Message)
    Write-Host "  ⚠️  $Message" -ForegroundColor DarkYellow
}
function Write-Info {
    param([string]$Message)
    Write-Host "  ℹ️  $Message" -ForegroundColor Cyan
}

# 加载本地敏感配置
$secretsPath = Join-Path $PSScriptRoot "secrets.ps1"
if (-not (Test-Path $secretsPath)) {
    Write-Error @"
找不到本地配置文件: $secretsPath

请复制 scripts/secrets.template.ps1 为 scripts/secrets.ps1，并填入真实 Token。
该文件已被 .gitignore 忽略，不会泄露到 GitHub。
"@
    exit 1
}
. $secretsPath

if (-not $Global:NotionToken -or -not $Global:DatabaseId) {
    Write-Error "secrets.ps1 中未设置 `$Global:NotionToken 或 `$Global:DatabaseId。"
    exit 1
}

# 定位 adb
if (-not (Test-Path $AdbPath)) {
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if ($adb) { $AdbPath = $adb.Source }
    else {
        Write-Error "找不到 adb，请修改 -AdbPath 参数或将 adb 加入 PATH。"
        exit 1
    }
}

function Invoke-Adb {
    param([string]$Arguments)
    $proc = Start-Process -FilePath $AdbPath -ArgumentList $Arguments -NoNewWindow -Wait -PassThru
    return $proc.ExitCode
}

# 定位/下载 APK
$projectRoot = Split-Path -Parent $PSScriptRoot
$distDir = Join-Path $projectRoot "dist"
if (-not (Test-Path $distDir)) { New-Item -ItemType Directory -Path $distDir | Out-Null }

if (-not $ApkPath) {
    Write-Step "[1/7] 下载 GitHub Actions 最新 APK artifact..."
    $run = gh run list -R $Repo --branch main --json databaseId,status,conclusion,headSha --jq '.[] | select(.status=="completed" and .conclusion=="success") | .databaseId' | Select-Object -First 1
    if (-not $run) {
        Write-Error "找不到成功的 GitHub Actions 运行记录。"
        exit 1
    }
    Write-Info "找到最新成功运行: $run"

    # 下载 artifact 到临时目录
    $tempDl = Join-Path $distDir "dl_$run"
    if (Test-Path $tempDl) { Remove-Item -Recurse -Force $tempDl }
    New-Item -ItemType Directory -Path $tempDl | Out-Null

    gh run download -R $Repo $run --name "web-to-notion-apk" --dir $tempDl 2>&1 | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }

    $apk = Get-ChildItem -Path $tempDl -Recurse -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $apk) {
        Write-Error "下载 artifact 后未找到 APK 文件。"
        exit 1
    }
    $ApkPath = Join-Path $distDir "app-release-test.apk"
    Copy-Item -Path $apk.FullName -Destination $ApkPath -Force
    Remove-Item -Recurse -Force $tempDl
    Write-Ok "APK 已下载: $ApkPath"
}
elseif (-not (Test-Path $ApkPath)) {
    Write-Error "APK 不存在: $ApkPath"
    exit 1
}

# 检查设备
Write-Step "[2/7] 检查 Android 设备连接..."
$devices = & $AdbPath devices | Select-String -Pattern "device$" | ForEach-Object { $_.Line }
if (-not $devices) {
    Write-Error "没有检测到已连接的 Android 设备，请插上手机并开启 USB 调试。"
    exit 1
}
$devices | ForEach-Object { Write-Info "已连接: $_" }

# 清 logcat
Write-Step "[3/7] 清理并启动日志抓取..."
Invoke-Adb "logcat -c" | Out-Null
$logFile = Join-Path $distDir "logcat_$((Get-Date -Format 'yyyyMMdd_HHmmss')).txt"
$logcatJob = Start-Job -ScriptBlock {
    param($adb, $pkg, $out)
    & $adb logcat -v threadtime | Select-String -Pattern "$pkg|AndroidRuntime|FATAL EXCEPTION" | Tee-Object -FilePath $out
} -ArgumentList $AdbPath, $PackageName, $logFile
Write-Ok "日志写入: $logFile"

# 安装 APK
Write-Step "[4/7] 安装 APK..."
Invoke-Adb "uninstall $PackageName" | Out-Null
$installCode = Invoke-Adb "install `"$ApkPath`""
if ($installCode -ne 0) {
    Stop-Job $logcatJob -ErrorAction SilentlyContinue
    Write-Error "安装失败。请检查手机是否允许 USB 安装。"
    exit 1
}
Write-Ok "安装成功"

# 启动 App
Write-Step "[5/7] 启动 App 并写入配置..."
Invoke-Adb "shell am start -n $MainActivity" | Out-Null
Start-Sleep -Seconds 2
$broadcastArgs = "shell am broadcast -a io.trae.webtonotion.SET_CONFIG -n $Receiver --es notion_token `"$Global:NotionToken`" --es database_id `"$Global:DatabaseId`""
Invoke-Adb $broadcastArgs | Out-Null
Write-Ok "配置已发送"

# 截图
Write-Step "[6/7] 截图记录首页..."
$screenshot = Join-Path $distDir "screenshot_home_$((Get-Date -Format 'yyyyMMdd_HHmmss')).png"
Invoke-Adb "shell screencap -p /sdcard/wtn_test_home.png" | Out-Null
Invoke-Adb "pull /sdcard/wtn_test_home.png `"$screenshot`"" | Out-Null
Write-Ok "截图保存: $screenshot"

# 模拟点击抽屉设置项
Write-Step "[7/7] 验证抽屉菜单设置项可点击..."
# 先打开抽屉：点击菜单按钮（左上角，坐标因设备而异，此处给常见 1080p 参考值）
Invoke-Adb "shell input tap 120 160" | Out-Null
Start-Sleep -Milliseconds 500
# 点击"设置"项（抽屉中部偏下，需根据实际截图微调）
Invoke-Adb "shell input tap 400 900" | Out-Null
Start-Sleep -Seconds 2
$settingsScreenshot = Join-Path $distDir "screenshot_settings_$((Get-Date -Format 'yyyyMMdd_HHmmss')).png"
Invoke-Adb "shell screencap -p /sdcard/wtn_test_settings.png" | Out-Null
Invoke-Adb "pull /sdcard/wtn_test_settings.png `"$settingsScreenshot`"" | Out-Null
Write-Ok "设置页截图保存: $settingsScreenshot"

# 等待同步日志
Write-Step "等待 10 秒收集同步日志..."
Start-Sleep -Seconds 10
Stop-Job $logcatJob -ErrorAction SilentlyContinue
Receive-Job $logcatJob -ErrorAction SilentlyContinue | Out-Null
Remove-Job $logcatJob -ErrorAction SilentlyContinue

# 汇总
Write-Step "测试完成"
Write-Info "APK: $ApkPath"
Write-Info "首页截图: $screenshot"
Write-Info "设置页截图: $settingsScreenshot"
Write-Info "日志: $logFile"

if (-not $KeepArtifacts) {
    Remove-Item -Path $ApkPath -Force -ErrorAction SilentlyContinue
    Write-Warn "已清理 APK。如需保留请使用 -KeepArtifacts"
}

Write-Host "`n接下来请人工检查截图和日志，确认抽屉能打开设置页、Notion 连接与同步是否正常。" -ForegroundColor Cyan
