<#
.SYNOPSIS
  一键安装 WakeUpMyWall PC Agent（Windows x64，自包含，不需要预装 .NET）。

.DESCRIPTION
  适合"已经从 Releases 下载了 zip"的场景。默认与一键安装包一致：
  铺文件到 %ProgramFiles%\WakeUpMyWall → 放行 TCP 9876（专用/域网络）→ 以**托盘模式**启动 → 打印配对码；
  可选创建桌面快捷方式、登录自启、或改成服务模式。
  需要管理员 PowerShell。

.EXAMPLE
  # 在下载目录里（脚本与 zip 放一起，或显式指定 zip）：
  powershell -ExecutionPolicy Bypass -File .\install.ps1 -ZipPath .\WakeUpMyWall-Agent-win-x64-v0.2.0.zip

.EXAMPLE
  # 不要桌面快捷方式、不要登录自启，改成后台服务：
  powershell -ExecutionPolicy Bypass -File .\install.ps1 -DesktopShortcut:$false -TrayAutostart:$false -Service
#>
[CmdletBinding()]
param(
    [string]$ZipPath,
    [string]$InstallDir = (Join-Path $env:ProgramFiles 'WakeUpMyWall'),
    [string]$ServiceName = 'WakeUpMyWallAgent',
    [int]$Port = 9876,
    [bool]$DesktopShortcut = $true,
    [bool]$TrayAutostart = $true,
    [switch]$Service
)

$ErrorActionPreference = 'Stop'

function Assert-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw '需要管理员权限：请用"以管理员身份运行"的 PowerShell 再执行一次。'
    }
}

Assert-Administrator

if (-not $ZipPath) {
    $candidate = Get-ChildItem -Path $PSScriptRoot -Filter 'WakeUpMyWall-Agent-win-x64-*.zip' -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $candidate) {
        throw "没找到 zip：请用 -ZipPath 指定（在 Releases 页面下载 WakeUpMyWall-Agent-win-x64-*.zip）。"
    }
    $ZipPath = $candidate.FullName
}

if (-not (Test-Path $ZipPath)) { throw "zip 不存在：$ZipPath" }

Write-Host "== 1/6 解开 $ZipPath → $InstallDir" -ForegroundColor Cyan
if (Test-Path $InstallDir) {
    # 停掉旧服务/旧托盘进程再覆盖文件，否则 exe 被占用会失败。
    & sc.exe stop $ServiceName | Out-Null
    & sc.exe delete $ServiceName | Out-Null
    Get-Process -Name 'WakeUpMyWall.Agent' -ErrorAction SilentlyContinue | Stop-Process -Force
    Start-Sleep -Seconds 2
    Remove-Item -Recurse -Force $InstallDir
}
New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
Expand-Archive -Path $ZipPath -DestinationPath $InstallDir -Force

$exe = Join-Path $InstallDir 'WakeUpMyWall.Agent.exe'
if (-not (Test-Path $exe)) { throw "解压后没看到 $exe（zip 内容不对？）" }

Write-Host "== 2/6 放行 TCP $Port（专用/域网络）" -ForegroundColor Cyan
& netsh.exe advfirewall firewall delete rule name="WakeUpMyWall Agent" | Out-Null
& netsh.exe advfirewall firewall add rule name="WakeUpMyWall Agent" dir=in action=allow protocol=TCP localport=$Port profile=private,domain | Out-Null

Write-Host "== 3/6 快捷方式" -ForegroundColor Cyan
$shell = New-Object -ComObject WScript.Shell
if ($DesktopShortcut) {
    $link = $shell.CreateShortcut((Join-Path ([Environment]::GetFolderPath('Desktop')) 'WakeUpMyWall Agent.lnk'))
    $link.TargetPath = $exe; $link.Save()
    Write-Host "  桌面快捷方式已创建"
}
if ($TrayAutostart -and -not $Service) {
    $startup = [Environment]::GetFolderPath('Startup')
    $link = $shell.CreateShortcut((Join-Path $startup 'WakeUpMyWall Agent.lnk'))
    $link.TargetPath = $exe; $link.Save()
    Write-Host "  已加入登录启动（托盘模式）"
}

Write-Host "== 4/6 启动方式" -ForegroundColor Cyan
if ($Service) {
    & sc.exe create $ServiceName binPath= "`"$exe`"" start= auto DisplayName= "WakeUpMyWall Agent" | Out-Null
    & sc.exe start $ServiceName | Out-Null
    Write-Host "  已注册为 Windows 服务（无托盘图标）"
} else {
    Start-Process -FilePath $exe
    Write-Host "  已以托盘方式启动（右下角通知区域）"
}
Start-Sleep -Seconds 3

Write-Host "== 5/6 取配对码" -ForegroundColor Cyan
$pairingFile = Join-Path $env:ProgramData 'WakeUpMyWall\pairing.txt'
if (Test-Path $pairingFile) {
    $code = (Get-Content $pairingFile -Raw).Trim()
    Write-Host ''
    Write-Host "  配对码：$code（5 分钟有效、一次性）" -ForegroundColor Green
    Write-Host "  手机端：Device Setup → Agent 区输入它 → Pair" -ForegroundColor Green
} else {
    Write-Warning "没读到 $pairingFile；先手动跑一次 $exe 看配对码。"
}

Write-Host '== 6/6 完成 ==' -ForegroundColor Cyan
if ($Service) {
    Write-Host "服务状态：$((& sc.exe query $ServiceName | Select-String 'STATE').ToString().Trim())"
    Write-Host '退出：sc.exe stop WakeUpMyWallAgent（再 sc.exe start 会换新配对码）'
} else {
    Write-Host '托盘图标在右下角通知区域：右键 → 查看配对码 / 退出'
    Write-Host '（想让它开机自启但这次没加进启动项：把桌面快捷方式拖进 shell:startup）'
}
Write-Host "日志：$env:ProgramData\WakeUpMyWall\agent.log"
