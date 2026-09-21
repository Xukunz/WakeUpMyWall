; WakeUpMyWall PC Agent —— Windows 一键安装包（Inno Setup 6）
;
; 构建（发布流水线 .github/workflows/release.yml 里跑的就是这条）：
;   dotnet publish agent/src/WakeUpMyWall.Agent -f net10.0-windows -c Release -r win-x64 --self-contained true -o publish/win-x64
;   ISCC.exe agent/installer/WakeUpMyWall.Agent.iss /DMyAppVersion=0.2.0
;
; 装完会做四件事：铺文件 → 注册并启动 Windows 服务 → 放行 9876 → 把配对码显示给用户。
; 为什么不用 sc.exe 命令行搞定全部：装成服务后没有控制台，配对码只在盘上（%ProgramData%\WakeUpMyWall\pairing.txt），
; 所以最后一步必须由安装程序读出来告诉用户，否则他装完根本没法配对。

#ifndef MyAppVersion
  #define MyAppVersion "0.0.0"
#endif

#define MyAppName "WakeUpMyWall Agent"
#define MyAppExeName "WakeUpMyWall.Agent.exe"
#define ServiceName "WakeUpMyWallAgent"
#define FirewallRule "WakeUpMyWall Agent"
#define AgentPort "9876"

[Setup]
AppId={{8F3D2A64-9C41-4E7B-A1D0-5B7E9C4F2A31}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher=WakeUpMyWall
DefaultDirName={autopf}\WakeUpMyWall
DisableProgramGroupPage=yes
PrivilegesRequired=admin
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir=..\..\dist
OutputBaseFilename=WakeUpMyWall-Agent-Setup-{#MyAppVersion}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
MinVersion=10.0
UninstallDisplayName={#MyAppName}

[Languages]
Name: "chinesesimplified"; MessagesFile: "compiler:Languages\ChineseSimplified.isl"

[Files]
; 自包含发布：机器上不需要预先装 .NET。
Source: "..\..\publish\win-x64\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[UninstallRun]
Filename: "{sys}\sc.exe"; Parameters: "stop {#ServiceName}"; Flags: runhidden waituntilterminated
Filename: "{sys}\sc.exe"; Parameters: "delete {#ServiceName}"; Flags: runhidden waituntilterminated
Filename: "{sys}\netsh.exe"; Parameters: "advfirewall firewall delete rule name=""{#FirewallRule}"""; Flags: runhidden waituntilterminated

[Code]
function RunTool(const FileName, Parameters: String): Integer;
var
  ExitCode: Integer;
begin
  Exec(FileName, Parameters, '', SW_HIDE, ewWaitUntilTerminated, ExitCode);
  Result := ExitCode;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  Sc, Netsh, ExePath, PairingFile, Hint: String;
  Pairing: AnsiString;
begin
  if CurStep <> ssPostInstall then
    Exit;

  Sc := ExpandConstant('{sys}\sc.exe');
  Netsh := ExpandConstant('{sys}\netsh.exe');
  ExePath := ExpandConstant('{app}\{#MyAppExeName}');

  // 先收掉可能存在的旧服务（升级/重装场景），再按当前路径重新注册。
  RunTool(Sc, 'stop {#ServiceName}');
  RunTool(Sc, 'delete {#ServiceName}');
  RunTool(Sc, 'create {#ServiceName} binPath= "' + ExePath + '" start= auto DisplayName= "WakeUpMyWall Agent"');
  // 只对专用/域网络放行：公共网络下不主动开洞（用户要自己决定）。
  RunTool(Netsh, 'advfirewall firewall add rule name="{#FirewallRule}" dir=in action=allow protocol=TCP localport={#AgentPort} profile=private,domain');
  RunTool(Sc, 'start {#ServiceName}');

  // 服务把配对码写到 %ProgramData%\WakeUpMyWall\pairing.txt，等它起来再读。
  Sleep(2500);
  PairingFile := ExpandConstant('{commonappdata}\WakeUpMyWall\pairing.txt');

  if LoadStringFromFile(PairingFile, Pairing) then
    Hint := '安装完成：Agent 已在后台作为 Windows 服务运行（开机自启）。' + #13#10 + #13#10 +
            '手机端配对码：' + String(Pairing) + '（5 分钟有效、一次性）' + #13#10 + #13#10 +
            '在手机的 Device Setup → Agent 区输入这个码即可配对；' + #13#10 +
            '需要新码时重启服务即可（sc.exe stop/start {#ServiceName}）。' + #13#10 + #13#10 +
            '手机连不上时检查：与这台 PC 在同一网段、防火墙放行 TCP {#AgentPort}（本安装包只对专用网络放行）。'
  else
    Hint := '安装完成：Agent 已在后台作为 Windows 服务运行（开机自启），但没能读到配对码文件：' + #13#10 +
            PairingFile + #13#10 + #13#10 +
            '请手动运行一次 ' + ExePath + ' 获取配对码（或用 Get-Content 看该文件）。';

  MsgBox(Hint, mbInformation, MB_OK);
end;
