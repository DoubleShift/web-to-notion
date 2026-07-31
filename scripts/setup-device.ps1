#Requires -Version 7.0
<#
.SYNOPSIS
    一键推送 web-to-notion APK 到手机并自动填入 Notion Token / Database ID。

.DESCRIPTION
    1. 把 APK 推送到手机存储（默认 Download 目录）。
    2. 提示用户在手机上点击 APK 手动安装（绕过国产系统的 USB 安装限制）。
    3. 安装完成后按任意键继续，脚本自动启动 App。
    4. 发送广播把配置写入 App 的 DataStore。

    敏感配置（Notion Token / Database ID）请写在同目录的 secrets.ps1 中，
    该文件已被 .gitignore 排除，不会提交到 GitHub。

.PARAMETER ApkPath
    APK 文件路径。默认查找 dist\**\*.apk。

.PARAMETER AdbPath
    adb 可执行文件路径。默认使用 D:\Dev\Lib\adb\adb.exe，找不到则尝试环境变量中的 adb。

.PARAMETER AdbInstall
    强制使用 ADB 直接安装（旧行为）。如果国产系统反复拦截 USB 安装，不建议使用。

.EXAMPLE
    .\scripts\setup-device.ps1
    .\scripts\setup-device.ps1 -ApkPath "D:\apk\app-release.apk"
    .\scripts\setup-device.ps1 -AdbInstall
#>
[CmdletBinding()]
param(
    [string] $ApkPath,
    [string] $AdbPath = "D:\Dev\Lib\adb\adb.exe",
    [switch] $AdbInstall
)

$PackageName = "io.trae.webtonotion"
$MainActivity = "$PackageName/.MainActivity"
$Receiver = "$PackageName/.receiver.ConfigReceiver"
$PhoneApkPath = "/sdcard/Download/web-to-notion.apk"

# 加载本地敏感配置
$secretsPath = Join-Path $PSScriptRoot "secrets.ps1"
if (-not (Test-Path $secretsPath)) {
    Write-Error @"
找不到本地配置文件: $secretsPath

请复制 scripts/secrets.template.ps1 为 scripts/secrets.ps1，并填入你的真实 Token。
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

# 颜色输出
function Write-Step {
    param([string]$Message)
    Write-Host "`n$Message" -ForegroundColor Yellow
}
function Write-Ok {
    param([string]$Message)
    Write-Host "  ✅ $Message" -ForegroundColor Green
}
function Write-Info {
    param([string]$Message)
    Write-Host "  ℹ️  $Message" -ForegroundColor Cyan
}

# 定位 APK
if (-not $ApkPath) {
    $scriptRoot = Split-Path -Parent $PSScriptRoot
    $candidate = Get-ChildItem -Path "$scriptRoot\dist" -Recurse -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $candidate) {
        Write-Error "找不到 APK。请指定 -ApkPath，或先运行 GitHub Actions 下载 APK 到 dist 目录。"
        exit 1
    }
    $ApkPath = $candidate.FullName
}
if (-not (Test-Path $ApkPath)) {
    Write-Error "APK 不存在: $ApkPath"
    exit 1
}

Write-Info "APK: $ApkPath"
Write-Info "ADB: $AdbPath"

# 检查设备连接
Write-Step "[1/4] 检查设备连接..."
$devices = & $AdbPath devices | Select-String -Pattern "device$" | ForEach-Object { $_.Line }
if (-not $devices) {
    Write-Error "没有检测到已连接的 Android 设备，请插上手机并开启 USB 调试。"
    exit 1
}
$devices | ForEach-Object { Write-Ok "已连接: $_" }

if ($AdbInstall) {
    # 旧行为：ADB 直接安装
    Write-Step "[2/4] 通过 ADB 安装 APK..."
    Invoke-Adb "uninstall $PackageName" | Out-Null
    $installCode = Invoke-Adb "install `"$ApkPath`""
    if ($installCode -ne 0) {
        Write-Error "安装失败。请在手机上允许 USB 安装后重试，或去掉 -AdbInstall 使用推送安装。"
        exit 1
    }
    Write-Ok "安装成功"
}
else {
    # 默认行为：推送到手机，用户手动安装
    Write-Step "[2/4] 推送 APK 到手机 Download 目录..."
    Invoke-Adb "push `"$ApkPath`" $PhoneApkPath" | Out-Null
    Write-Ok "已推送到手机：Download/web-to-notion.apk"

    Write-Host "`n" -NoNewline
    Write-Host "────────────────────────────────────────" -ForegroundColor Cyan
    Write-Host "  请在手机上完成以下操作：" -ForegroundColor Cyan
    Write-Host "  1. 打开文件管理器 → Download 文件夹" -ForegroundColor Cyan
    Write-Host "  2. 点击 web-to-notion.apk 安装" -ForegroundColor Cyan
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
Write-Step "[3/4] 启动 App..."
Invoke-Adb "shell am start -n $MainActivity" | Out-Null
Start-Sleep -Seconds 2
Write-Ok "已启动"

# 发送配置广播
Write-Step "[4/4] 写入 Notion 配置..."
$broadcastArgs = "shell am broadcast -a io.trae.webtonotion.SET_CONFIG -n $Receiver --es notion_token `"$Global:NotionToken`" --es database_id `"$Global:DatabaseId`""
Invoke-Adb $broadcastArgs | Out-Null
Write-Ok "配置已发送"

Write-Host "`n✅ 完成。打开 App → 设置，即可看到 Token 和 Database ID 已填入。" -ForegroundColor Cyan
