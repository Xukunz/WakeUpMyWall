<#
.SYNOPSIS
  一键安装 WakeUpMyWall PC Agent（Windows x64，自包含，不需要预装 .NET）。

.DESCRIPTION
  适合"已经从 Releases 下载了 zip"的场景。做四件事：
  铺文件到 %ProgramFiles%\WakeUpMyWall → 注册并启动 Windows 服务 → 放行 TCP 9876（专用/域网络）→ 打印配对码。
  需要管理员 PowerShell。

.EXAMPLE
  # 在下载目录里（脚本与 zip 放一起，或显式指定 zip）：
  powershell -ExecutionPolicy Bypass -File .\install.ps1 -ZipPath .\WakeUpMyWall-Agent-win-x64-v0.2.0.zip
#>
[CmdletBinding()]
param(
    [string]$ZipPath,
    [string]$InstallDir = (Join-Path $env:ProgramFiles 'WakeUpMyWall'),
    [string]$ServiceName = 'WakeUpMyWallAgent',
    [int]$Port = 9876
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

Write-Host "== 1/5 解开 $ZipPath → $InstallDir" -ForegroundColor Cyan
if (Test-Path $InstallDir) {
    # 停掉旧服务再覆盖文件，否则 exe 被占用会失败。
    & sc.exe stop $ServiceName | Out-Null
    & sc.exe delete $ServiceName | Out-Null
    Start-Sleep -Seconds 2
    Remove-Item -Recurse -Force $InstallDir
}
New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
Expand-Archive -Path $ZipPath -DestinationPath $InstallDir -Force

$exe = Join-Path $InstallDir 'WakeUpMyWall.Agent.exe'
if (-not (Test-Path $exe)) { throw "解压后没看到 $exe（zip 内容不对？）" }

Write-Host "== 2/5 注册 Windows 服务 $ServiceName" -ForegroundColor Cyan
& sc.exe create $ServiceName binPath= "`"$exe`"" start= auto DisplayName= "WakeUpMyWall Agent" | Out-Null

Write-Host "== 3/5 放行 TCP $Port（专用/域网络）" -ForegroundColor Cyan
& netsh.exe advfirewall firewall delete rule name="WakeUpMyWall Agent" | Out-Null
& netsh.exe advfirewall firewall add rule name="WakeUpMyWall Agent" dir=in action=allow protocol=TCP localport=$Port profile=private,domain | Out-Null

Write-Host "== 4/5 启动服务" -ForegroundColor Cyan
& sc.exe start $ServiceName | Out-Null
Start-Sleep -Seconds 3

Write-Host "== 5/5 取配对码" -ForegroundColor Cyan
$pairingFile = Join-Path $env:ProgramData 'WakeUpMyWall\pairing.txt'
if (Test-Path $pairingFile) {
    $code = (Get-Content $pairingFile -Raw).Trim()
    Write-Host ''
    Write-Host "  配对码：$code（5 分钟有效、一次性）" -ForegroundColor Green
    Write-Host "  手机端：Device Setup → Agent 区输入它 → Pair" -ForegroundColor Green
} else {
    Write-Warning "没读到 $pairingFile；先手动跑一次 $exe 看配对码。"
}

Write-Host ''
Write-Host "服务状态：$((& sc.exe query $ServiceName | Select-String 'STATE').ToString().Trim())"
Write-Host '卸载：sc.exe stop WakeUpMyWallAgent; sc.exe delete WakeUpMyWallAgent; 删除安装目录'
