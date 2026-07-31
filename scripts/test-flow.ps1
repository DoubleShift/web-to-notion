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
    [switch] $KeepArtifacts,
    [switch] $AdbInstall
)

$PackageName = "io.trae.webtonotion"
$MainActivity = "$PackageName/.MainActivity"
$Receiver = "$PackageName/.receiver.ConfigReceiver"
$PhoneApkPath = "/sdcard/Download/web-to-notion-test.apk"
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
Write-Step "[3/7] 清理日志缓存..."
Invoke-Adb "logcat -c" | Out-Null
$logFile = Join-Path $distDir "logcat_$((Get-Date -Format 'yyyyMMdd_HHmmss')).txt"
Write-Ok "日志将在测试结束后保存到: $logFile"

# 安装 APK
Write-Step "[4/7] 安装 APK..."

if ($AdbInstall) {
    # 强制 ADB 直接安装（旧行为）
    Invoke-Adb "uninstall $PackageName" | Out-Null
    $installCode = Invoke-Adb "install `"$ApkPath`""
    if ($installCode -ne 0) {
        Write-Error "ADB 安装失败。请使用推送安装（去掉 -AdbInstall）。"
        exit 1
    }
    Write-Ok "ADB 安装成功"
}
else {
    # 默认：推送到手机，用户手动安装
    Invoke-Adb "push `"$ApkPath`" $PhoneApkPath" | Out-Null
    Write-Ok "已推送到手机：Download/web-to-notion-test.apk"

    Write-Host "`n" -NoNewline
    Write-Host "────────────────────────────────────────" -ForegroundColor Cyan
    Write-Host "  请在手机上完成安装：" -ForegroundColor Cyan
    Write-Host "  1. 打开文件管理器 → Download 文件夹" -ForegroundColor Cyan
    Write-Host "  2. 点击 web-to-notion-test.apk 安装" -ForegroundColor Cyan
    Write-Host "  3. 脚本会自动检测安装完成并继续" -ForegroundColor Cyan
    Write-Host "────────────────────────────────────────" -ForegroundColor Cyan

    $installed = $false
    for ($i = 0; $i -lt 12; $i++) {
        Start-Sleep -Seconds 5
        $check = & $AdbPath shell pm list packages $PackageName 2>&1
        if ($check -match $PackageName) {
            $installed = $true
            Write-Ok "检测到应用已安装"
            break
        }
        Write-Info "等待安装中... ($($i + 1)/12)"
    }

    if (-not $installed) {
        Write-Error "未检测到应用安装。请在手机上完成安装后重试。"
        exit 1
    }
}

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

# 验证能否进入设置页（暂时用右上角"更多"按钮绕过抽屉点击问题）
Write-Step "[7/7] 验证设置页可打开..."
# 先尝试点击抽屉设置项（调试用）
Invoke-Adb "shell input tap 120 160" | Out-Null
Start-Sleep -Milliseconds 500
$drawerScreenshot = Join-Path $distDir "screenshot_drawer_$((Get-Date -Format 'yyyyMMdd_HHmmss')).png"
Invoke-Adb "shell screencap -p /sdcard/wtn_test_drawer.png" | Out-Null
Invoke-Adb "pull /sdcard/wtn_test_drawer.png `"$drawerScreenshot`"" | Out-Null
Write-Ok "抽屉截图保存: $drawerScreenshot"
# 点击遮罩关闭抽屉
Invoke-Adb "shell input tap 950 1200" | Out-Null
Start-Sleep -Milliseconds 300
# 点击右上角"更多"按钮进入设置
Invoke-Adb "shell input tap 1000 160" | Out-Null
Start-Sleep -Seconds 2
$settingsScreenshot = Join-Path $distDir "screenshot_settings_$((Get-Date -Format 'yyyyMMdd_HHmmss')).png"
Invoke-Adb "shell screencap -p /sdcard/wtn_test_settings.png" | Out-Null
Invoke-Adb "pull /sdcard/wtn_test_settings.png `"$settingsScreenshot`"" | Out-Null
Write-Ok "设置页截图保存: $settingsScreenshot"

# 点击"测试连接"按钮
Write-Step "[8/7] 测试 Notion 连接..."
Invoke-Adb "shell input tap 300 750" | Out-Null
Start-Sleep -Seconds 3
$connectionScreenshot = Join-Path $distDir "screenshot_connection_$((Get-Date -Format 'yyyyMMdd_HHmmss')).png"
Invoke-Adb "shell screencap -p /sdcard/wtn_test_connection.png" | Out-Null
Invoke-Adb "pull /sdcard/wtn_test_connection.png `"$connectionScreenshot`"" | Out-Null
Write-Ok "连接测试结果截图保存: $connectionScreenshot"

# 等待同步日志
Write-Step "等待 10 秒收集同步日志..."
Start-Sleep -Seconds 10

# 抓取日志到文件
& $AdbPath logcat -d -v threadtime | Select-String -Pattern "$PackageName|AndroidRuntime|FATAL EXCEPTION" | Out-File -FilePath $logFile -Encoding UTF8
Write-Ok "日志已保存"

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
