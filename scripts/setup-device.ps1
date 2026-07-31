#Requires -Version 7.0
<#
.SYNOPSIS
    一键安装 web-to-notion APK 并自动填入 Notion Token / Database ID。

.DESCRIPTION
    1. 通过 ADB 安装 APK（支持自动重试）。
    2. 启动 App。
    3. 发送广播把配置写入 App 的 DataStore。

    敏感配置（Notion Token / Database ID）请写在同目录的 secrets.ps1 中，
    该文件已被 .gitignore 排除，不会提交到 GitHub。

.PARAMETER ApkPath
    APK 文件路径。默认查找 dist\**\*.apk。

.PARAMETER AdbPath
    adb 可执行文件路径。默认使用 D:\Dev\Lib\adb\adb.exe，找不到则尝试环境变量中的 adb。

.EXAMPLE
    .\scripts\setup-device.ps1
    .\scripts\setup-device.ps1 -ApkPath "D:\apk\app-release.apk"
#>
[CmdletBinding()]
param(
    [string] $ApkPath,
    [string] $AdbPath = "D:\Dev\Lib\adb\adb.exe"
)

$PackageName = "io.trae.webtonotion"
$MainActivity = "$PackageName/.MainActivity"
$Receiver = "$PackageName/.receiver.ConfigReceiver"

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
    if ($adb) {
        $AdbPath = $adb.Source
    }
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

Write-Host "APK: $ApkPath" -ForegroundColor Cyan
Write-Host "ADB: $AdbPath" -ForegroundColor Cyan

# 检查设备连接
Write-Host "`n[1/4] 检查设备连接..." -ForegroundColor Yellow
$devices = & $AdbPath devices | Select-String -Pattern "device$" | ForEach-Object { $_.Line }
if (-not $devices) {
    Write-Error "没有检测到已连接的 Android 设备，请插上手机并开启 USB 调试。"
    exit 1
}
$devices | ForEach-Object { Write-Host "  已连接: $_" -ForegroundColor Green }

# 安装 APK
Write-Host "`n[2/4] 安装 APK..." -ForegroundColor Yellow
$uninstallCode = Invoke-Adb "uninstall $PackageName"
if ($uninstallCode -eq 0) {
    Write-Host "  已卸载旧版本" -ForegroundColor Green
}
else {
    Write-Host "  没有旧版本或卸载失败，继续安装" -ForegroundColor DarkGray
}

$installCode = Invoke-Adb "install `"$ApkPath`""
if ($installCode -ne 0) {
    Write-Error "安装失败（可能是魅族/小米等系统需要手动允许 USB 安装）。请在手机上允许后重试。"
    exit 1
}
Write-Host "  安装成功" -ForegroundColor Green

# 启动 App
Write-Host "`n[3/4] 启动 App..." -ForegroundColor Yellow
Invoke-Adb "shell am start -n $MainActivity" | Out-Null
Start-Sleep -Seconds 2
Write-Host "  已启动" -ForegroundColor Green

# 发送配置广播
Write-Host "`n[4/4] 写入 Notion 配置..." -ForegroundColor Yellow
$broadcastArgs = "shell am broadcast -a io.trae.webtonotion.SET_CONFIG -n $Receiver --es notion_token `"$Global:NotionToken`" --es database_id `"$Global:DatabaseId`""
Invoke-Adb $broadcastArgs | Out-Null
Write-Host "  配置已发送" -ForegroundColor Green

Write-Host "`n✅ 完成。打开 App → 设置，即可看到 Token 和 Database ID 已填入。" -ForegroundColor Cyan
