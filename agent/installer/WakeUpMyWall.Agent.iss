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
; 自带中文语言文件：choco 装的 Inno Setup 不装翻译组件（Languages\ 目录里只有 Default.isl），
; 引用 compiler:Languages\ChineseSimplified.isl 会在 CI 上直接编译失败（实测踩过）。
Name: "chinesesimplified"; MessagesFile: "languages\ChineseSimplified.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
; 自包含发布：机器上不需要预先装 .NET。
Source: "..\..\publish\win-x64\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Tasks]
Name: "desktopicon"; Description: "创建桌面快捷方式"; GroupDescription: "附加任务："; Flags: checkedonce
Name: "trayautostart"; Description: "登录时自动启动（托盘运行：右键图标可看配对码 / 退出）"; GroupDescription: "启动方式："; Flags: checkedonce
Name: "serviceinstall"; Description: "安装为 Windows 服务（开机自启，但没有托盘图标）"; GroupDescription: "启动方式："

[Icons]
; exe 自带图标（csproj 的 ApplicationIcon），快捷方式直接取它。
Name: "{autodesktop}\WakeUpMyWall Agent"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon; Comment: "局域网唤醒 / 电源控制 / 指标"
Name: "{userstartup}\WakeUpMyWall Agent"; Filename: "{app}\{#MyAppExeName}"; Tasks: trayautostart; Check: not ServiceMode

[UninstallRun]
Filename: "{sys}\sc.exe"; Parameters: "stop {#ServiceName}"; Flags: runhidden waituntilterminated
Filename: "{sys}\sc.exe"; Parameters: "delete {#ServiceName}"; Flags: runhidden waituntilterminated
Filename: "{sys}\netsh.exe"; Parameters: "advfirewall firewall delete rule name=""{#FirewallRule}"""; Flags: runhidden waituntilterminated

[Code]
/** 选了服务模式就不再建"登录时托盘自启"：两者都会去占 9876，同时存在会互相抢端口。 */
function ServiceMode: Boolean;
begin
  Result := WizardIsTaskSelected('serviceinstall');
end;

function RunTool(const FileName, Parameters: String): Integer;
var
  ExitCode: Integer;
begin
  Exec(FileName, Parameters, '', SW_HIDE, ewWaitUntilTerminated, ExitCode);
  Result := ExitCode;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  Sc, Netsh, ExePath, PairingFile, Hint, FirewallRule, How: String;
  Pairing: AnsiString;
  ExitCode: Integer;
begin
  if CurStep <> ssPostInstall then
    Exit;

  Sc := ExpandConstant('{sys}\sc.exe');
  Netsh := ExpandConstant('{sys}\netsh.exe');
  ExePath := ExpandConstant('{app}\{#MyAppExeName}');
  FirewallRule := 'advfirewall firewall add rule name="{#FirewallRule}" dir=in action=allow protocol=TCP localport={#AgentPort} profile=private,domain';

  // 无论哪种模式，先把旧服务收掉：0.2.x 装过服务的机器上，它会一直占着 9876，
  // 托盘实例会因为端口被占而退出（实测的升级路径）。
  RunTool(Sc, 'stop {#ServiceName}');
  RunTool(Sc, 'delete {#ServiceName}');
  // 只对专用/域网络放行：公共网络下不主动开洞（用户要自己决定）。
  RunTool(Netsh, FirewallRule);

  if ServiceMode then
  begin
    How := '作为 Windows 服务在后台运行（开机自启，无托盘图标）';
    RunTool(Sc, 'create {#ServiceName} binPath= "' + ExePath + '" start= auto DisplayName= "WakeUpMyWall Agent"');
    RunTool(Sc, 'start {#ServiceName}');
  end
  else
  begin
    How := '以托盘方式启动（右下角通知区域，右键图标可看配对码或退出）';
    // 立即跑一次（下次登录由启动项拉起）；ewNoWait：不阻塞安装程序。
    Exec(ExePath, '', '', SW_SHOWNORMAL, ewNoWait, ExitCode);
  end;

  // Agent 把配对码写到 %ProgramData%\WakeUpMyWall\pairing.txt，等它起来再读。
  Sleep(3000);
  PairingFile := ExpandConstant('{commonappdata}\WakeUpMyWall\pairing.txt');

  if LoadStringFromFile(PairingFile, Pairing) then
    Hint := '安装完成：Agent ' + How + '。' + #13#10 + #13#10 +
            '手机端配对码：' + String(Pairing) + '（5 分钟有效、一次性）' + #13#10 + #13#10 +
            '在手机的 Device Setup → Agent 区输入这个码即可配对；' + #13#10 +
            '托盘模式下：右键通知区域的图标 → 「配对码」查看当前码，' + #13#10 +
            '「退出」停止 Agent（需要新码时退出再启动即可）。' + #13#10 + #13#10 +
            '手机连不上时检查：与这台 PC 在同一网段、防火墙放行 TCP {#AgentPort}（本安装包只对专用网络放行）。'
  else
    Hint := '安装完成：Agent ' + How + '，但没能读到配对码文件：' + #13#10 +
            PairingFile + #13#10 + #13#10 +
            '请手动运行一次 ' + ExePath + ' 获取配对码（或用 Get-Content 看该文件）。';

  MsgBox(Hint, mbInformation, MB_OK);
end;
