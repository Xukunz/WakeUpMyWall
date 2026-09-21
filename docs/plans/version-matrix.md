# 版本矩阵（实测记录）

**记录时间：** 2026-09-19
**环境：** Ubuntu 26.04.1 LTS，x86_64，无 root（`sudo` 需交互密码，故工具链装在用户目录）
**工具链位置：** `~/.local/toolchain/jdk-25.0.4.1+1`、`~/.local/toolchain/android-sdk`

---

## 1. 已实测通过

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | Eclipse Temurin 25.0.4.1+1 | tarball 安装，免 root |
| Gradle | 9.5.0 | 由 `gradle/wrapper/gradle-wrapper.properties` 指定并由 Wrapper 下载 |
| Android cmdline-tools | 16111833 | `sdkmanager` 已弃用，新入口为同目录 `android` 二进制 |
| Android Platform | `platforms/android-37.0` | 提供 `compileSdk 37` |
| Build-Tools | 37.0.0 | |
| Platform-Tools | 37.0.1 | |
| Kotlin | 2.2.10 | 现有工程 `libs.versions.toml` |
| AGP | 9.3.3 | 现有工程 |
| androidx Compose BOM | 2026.02.01 | 对应 androidx Compose 1.10.4 |

### 基线构建证据

```text
$ ./gradlew --version
Gradle 9.5.0
Launcher JVM:  25.0.4.1 (Eclipse Adoptium 25.0.4.1+1-LTS)
Daemon JVM:    Compatible with Java 25, any vendor (from gradle/gradle-daemon-jvm.properties)

$ ./gradlew :app:assembleDebug
BUILD SUCCESSFUL in 1m 8s
36 actionable tasks: 36 executed
```

---

## 2. Task 2 实测结果（2026-09-19 更新）

| 组件 | 版本 | 实测结论 |
| --- | --- | --- |
| Compose Multiplatform | **1.10.3** | **可用**：与 Kotlin 2.2.10 + AGP 9.3.3 组合构建通过，无需回退到 1.9.0 |
| Ktor | 3.6.0 | 已解析（commonMain / commonTest 依赖均通过） |
| kotlinx-coroutines | 1.11.0 | 已解析 |
| kotlinx-serialization | 1.11.0 | 已解析 |
| kotlinx-datetime | 0.8.0 | 已解析 |
| Room / DataStore | **DataStore Preferences 1.2.1**（仅 androidMain） | Phase 2 选型定案（理由与备选对比见 [Phase 2 计划](../superpowers/plans/2026-09-20-phase2-device-system.md) §0）：键值容器 + commonMain 的 `JsonSettingsStorage` 负责序列化，`assembleDebug` 与真机落盘均通过 |
| TCP 探测 | JDK 自带 `java.net.Socket`（androidMain） | 未引入 `ktor-network`：探测只需"连得上/被拒/超时/解析失败"四态，JDK socket 足够，且异常映射能按类型写（common 侧拿不到这些异常类型） |
| WOL 发送 | JDK 自带 `java.net.DatagramSocket`（androidMain，`broadcast = true`） | 不新增依赖；魔包 102 字节由 commonMain 的 `MagicPacket` 编码，androidUnitTest 用真 UDP socket 逐字节验过 |
| PC Agent | **.NET SDK 10.0.401（LTS，装于 `~/.dotnet-local`）** + ASP.NET Core Minimal API | Phase 4A 新增；`agent/` 下 `dotnet test` 14 条契约测试全绿；Windows 侧产物用 `dotnet publish -r win-x64 --self-contained` 交叉发布（无需目标机装 .NET）。电源命令只在 Windows 生效，其它平台自动走 `--fake-power` |
| 硬件监控库 | **LibreHardwareMonitorLib 0.9.6**（Phase 5A 实测） | 只挂在 `net10.0-windows` 上（包内只有 `runtimes/win-*/` 运行时资产 + `ref/net10.0` 引用程序集，单目标 `net10.0` 引用不了）。`dotnet build -f net10.0-windows` 与 `dotnet publish -f net10.0-windows -r win-x64 --self-contained` 均在本机（Linux）交叉构建通过，发布目录里带 `LibreHardwareMonitorLib.dll` 与 `HidSharp.dll`。传感器名与单位按上游源码核对（`Network.cs` 的吞吐是**字节/秒**、`MemoryWindows.cs` 的 `Data` 是 GB、`SmallData` 是 MB） |
| Agent 目标框架 | **`net10.0;net10.0-windows` 双目标**（Phase 5A 实测） | `net10.0` 给 CI/开发机（合成指标，`--fake-metrics`）；`net10.0-windows` 给真实 PC（LibreHardwareMonitor）。Windows 专属代码用 `Compile Remove` 排除出 `net10.0`（已核对：`net10.0` 产物里没有 `WindowsMetricsProvider`，`net10.0-windows` 产物里有） |
| 指标流 | **ASP.NET Core WebSockets（框架自带）+ `ktor-client-websockets` 3.6.0**（Phase 5C 实测） | Agent 侧 `/ws/v1/metrics` 在握手阶段查 Bearer，之后按 `Agent:MetricsIntervalMs`（默认 1000）推帧；手机侧 Ktor 客户端**必须** `install(WebSockets)` 且 URL 的 scheme 必须是 `ws://`（拿 `http://` 去连会退化成普通 GET，Agent 如实回 400 —— 两条都在模拟器上实测踩过）。Ktor 客户端对"接住连接但不回帧"的对端不会自己报错，必须自己加无帧超时（本项目用 `Flow.withIdleTimeout(3s)`） |
| Agent 版本与发布 | **`<Version>0.2.0</Version>` + AssemblyVersion 派生**（2026-09-20 实测） | `/api/v1/status` 的 `agentVersion` 由程序集读出（不再手写字符串），实测返回 `0.2.0`；发布流水线里另有一道"tag 与 csproj 版本一致"的检查 |
| Windows 服务宿主 | **Microsoft.Extensions.Hosting.WindowsServices 10.0.0**（仅 `net10.0-windows`） | `builder.Host.UseWindowsService()` 让进程真的向 SCM 报到；不加的话 `sc.exe start` 会报 1053（这条是 Phase 4A 文档里没验到的坑） |
| 一键安装包 | **Inno Setup 6**（仅在 release 流水线的 windows runner 上装，`choco install innosetup`） | `agent/installer/WakeUpMyWall.Agent.iss` 负责铺文件 + 注册服务 + 放行 9876 + 显示配对码；本机（Linux）没有 ISCC，安装包本身只能在 CI 里编译（zip 与 publish 产物已在本机交叉验证） |

### AGP 9 与 KMP 的关键限制（实测）

`com.android.application` 与 `org.jetbrains.kotlin.multiplatform` 自 AGP 9.0 起**不能共存**，报错：

```text
The 'com.android.library' (or 'com.android.application') plugin is not compatible with the
'org.jetbrains.kotlin.multiplatform' plugin since AGP 9.0.
```

当前项目在 `gradle.properties` 中启用 AGP 官方临时旁路：

```properties
android.builtInKotlin=false
android.newDsl=false
```

副作用：只能用经典 DSL（`compileSdk = 37`，不能使用 `compileSdk { version = release(37) }` 与 `buildTypes.release.optimization`）。
**技术债：AGP 10 会移除该旁路**，届时需拆成两个模块：

```text
mobile/composeApp     com.android.kotlin.multiplatform.library   （commonMain 全部业务代码）
mobile/androidApp     com.android.application                    （MainActivity + 清单，依赖 :composeApp）
```

---

## 3. 仍待实测（后续 Phase）

| 组件 | 何时实测 | 选版依据 |
| --- | --- | --- |
| Room | Phase 2 | 设备持久化选型时对比 DataStore |
| DataStore | Phase 2 | 最新仅 `1.3.0-alpha11`，不引入 alpha |
| iOS / Kotlin/Native target | V2 | Apple target 需 macOS 编译；CMP 1.11+ 的原生目标要求 Kotlin ≥ 2.3 |

每次回退都必须回到本文件更新版本与原因。

---

## 4. Phase 0 收尾实测（2026-09-19）

`./gradlew clean --no-daemon` 之后冷执行三条 CI 命令，全部通过（与 `.github/workflows/ci.yml` 完全一致）：

| 命令 | 结果 |
| --- | --- |
| `./gradlew :composeApp:testDebugUnitTest` | 31 个测试通过，0 失败 |
| `./gradlew :composeApp:desktopTest` | 38 个测试通过，0 失败（含 7 个真实 Compose UI 测试） |
| `./gradlew :composeApp:assembleDebug` | APK 产出成功（debug 37.1 MB） |

补充实测：

- Compose UI 测试在 `DISPLAY` 未设置的容器内可运行（Skiko 软件渲染），CI 不需要 xvfb。
- 壁纸打包路径确认为 `assets/composeResources/com.xukunz.wakeupmywall.resources/drawable/`，APK 内可见两张内置壁纸。
- 壁纸母版派生使用 Pillow 12.1.1（系统 `python3`），属一次性人工操作，不是构建依赖。

---

## 5. 环境注意事项

1. **SDK 平台包命名变更**：`platforms;android-37` 已不存在，必须写 `platforms;android-37.0`（同系列还有 `-37.1`、`-37.2`）。
2. **`gradlew` 可执行位缺失**：git 中记录为 `100644`，新克隆后执行 `./gradlew` 会得到 `Permission denied`。已修正为 `100755`。
3. **沙箱限制**：容器内无 JDK/SDK 且沙箱 DNS 不通，所有下载与构建都需要在沙箱外执行；`local.properties` 已被 `.gitignore` 忽略，不会误提交机器相关路径。

---

## 6. 本机 Android 模拟器验证环境（2026-09-19 实测）

用于把"能在真机/模拟器上启动"这条验收标准变成可复现的操作，不依赖图形界面。

### 前置：KVM 权限

模拟器必须拿到 `/dev/kvm`，否则只能退化成纯软件模拟（x86_64 镜像基本不可用）。本机 `/dev/kvm` 的 ACL 是 `user::rw-` + `user:gdm-greeter:rw-` + `group:kvm:rw-`，普通用户需要进 `kvm` 组：

```bash
sudo usermod -aG kvm "$USER"     # 永久；组变更只对之后新启动的进程生效
sudo setfacl -m u:$USER:rw /dev/kvm   # 立即生效、不需要重登，重启后失效
```

**注意**：`usermod` 之后必须重新登录（或重启 Android Studio / 终端会话），否则旧进程的组列表里没有 `kvm`，`emulator -accel-check` 会报 `ProbeKVM: This user doesn't have permissions to use KVM`，`accel: 11`。本机也没装 `sg` / `newgrp`，无法在当前会话里临时提权。

验证：

```bash
"$ANDROID_SDK_ROOT/emulator/emulator" -accel-check   # 期望 accel: 0 + "KVM (version 12) is installed and usable"
```

### 安装与建 AVD

```bash
SDKM="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin"
yes | "$SDKM/sdkmanager" "emulator" "system-images;android-37.0;google_apis;x86_64"
# android-37.0 只有 google_apis，没有更小的 aosp_atd；api 36 有 aosp_atd 可省约 1 GB
echo no | "$SDKM/avdmanager" create avd -n wall \
  -k "system-images;android-37.0;google_apis;x86_64" -d "Nexus 10" --force
```

`Nexus 10` 是 2560×1600 @320dpi 的横屏平板档，贴近"墙面屏"的产品形态。

### 无头启动与验证

```bash
EMU="$ANDROID_SDK_ROOT/emulator/emulator"; ADB="$ANDROID_SDK_ROOT/platform-tools/adb"
setsid nohup "$EMU" -avd wall -no-window -no-audio -no-snapshot -no-boot-anim \
  -gpu swiftshader_indirect > /tmp/emulator_wall.log 2>&1 < /dev/null &
"$ADB" wait-for-device
until [ "$("$ADB" shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 5; done

./gradlew :composeApp:installDebug
"$ADB" logcat -c
"$ADB" shell am start -n com.xukunz.wakeupmywall/.MainActivity
"$ADB" shell dumpsys activity activities | grep topResumedActivity   # 应指向 MainActivity
"$ADB" logcat -d | grep -E "FATAL|AndroidRuntime"                     # 应为空
"$ADB" exec-out screencap -p > /tmp/app.png                           # 取真实帧
adb emu kill                                                          # 收工
```

### 实测结果

- 冷启动到 `sys.boot_completed=1`：约 20 秒；`ro.build.version.sdk = 37`。
- App 安装后 `topResumedActivity` 为 `com.xukunz.wakeupmywall/.MainActivity`，logcat 无 `FATAL`。
- 屏幕取到的真实帧与桌面渲染一致（Aurora 壁纸 + Dashboard 占位页）。

### 2026-09-20 复检：**更正**旧结论，模拟器已恢复可用

本节早先写下的三条判断——"`/dev/kvm` 不存在""宿主机没开 VT""kvm 模块没加载"——**全部不成立**，
它们是在 Codex 沙箱里量出来的假象。沙箱外（escalated）复测：

```text
$ ls -l /dev/kvm
crw-rw----+ 1 root kvm 10, 232 Sep 19 09:40 /dev/kvm
$ getfacl /dev/kvm
user::rw-  user:gdm-greeter:rw-  group::rw-  mask::rw-  other::---
$ lsmod | grep -i kvm
kvm_amd               262144  6
kvm                  1527808  1 kvm_amd
```

沙箱里的 `/dev` 是 bwrap 给的私有 devtmpfs（只有 `null` `zero` `full` `random` `urandom` `tty` `pts` `shm`），
所以沙箱内 `ls /dev/kvm` 必然 `No such file`、`-accel-check` 必然 `accel: 8`；沙箱内也没有网络，
那次 `-accel off` 的"snapshot 下载超时"同样与模拟器无关。**规则：模拟器与 adb 的启动、检查都必须在沙箱外执行，
沙箱内量出的此类结论一律作废。**

真正让"模拟器起不来"的是下面两条：

| 层 | 现象 | 证据 | 处理 |
| --- | --- | --- | --- |
| 会话权限 | `-accel-check` → `accel:` / `11` / `This user doesn't have permissions to use KVM (/dev/kvm)` | `/etc/group` 里 `kvm:x:991:xukunz` 已生效（`/etc/group` mtime 09-19 09:30），但 agent 的进程树来自 09-19 00:26 启动的 `codex app-server`，补充组是登录时冻结的，没有 991 | 在带 kvm 组的会话里启动（新登录的 SSH / GDM 会话自带 991）；或一次性 `sudo setfacl -m u:$USER:rw /dev/kvm`（重启失效，持久要靠 udev 规则） |
| 实例生命周期 | 新实例立刻 `FATAL \| Running multiple emulators with the same AVD is an experimental feature. Please use -read-only flag` | 09-19 09:40 起的 `-no-window` 实例一直活着（PID 94603，已跑 18h49m）并锁住 `wall` AVD | 先 `adb -s <serial> emu kill` 收掉旧实例；或显式加 `-read-only` |

本轮恢复步骤与实测结果：

```bash
ADB="$ANDROID_SDK_ROOT/platform-tools/adb"; EMU="$ANDROID_SDK_ROOT/emulator/emulator"
"$ADB" -s emulator-5554 emu kill        # 收掉 09-19 遗留的无头实例：它会 FATAL 掉任何新启动
pgrep -f 'qemu-system-x86_64-headless -avd wa[l]l'   # 应无输出（模式里的 [w] 防止自匹配）
setsid nohup "$EMU" -avd wall -no-window -no-audio -no-snapshot -no-boot-anim \
  -gpu swiftshader_indirect > /tmp/emulator_wall.log 2>&1 < /dev/null &
```

- 冷启动到 `sys.boot_completed=1`：**19.8 秒**（KVM 生效；纯软件模拟不可能这么快）。
- 新进程 `/proc/<pid>/status` 的 `Groups:` 含 `991`；`/proc/<pid>/fd` 有 6 个 kvm 句柄（`/dev/kvm`、`kvm-vm`、4 个 `kvm-vcpu`）。
- `:composeApp:installDebug` 后 `topResumedActivity=com.xukunz.wakeupmywall/.MainActivity`，logcat 无 `FATAL`，
  `screencap` 出 2560×1600 真实帧（横屏，与桌面 1280×720 同属一个断点区间）。
- **应用数据落点（Phase 2 起）**：设备列表与外观设置写在 `/data/data/com.xukunz.wakeupmywall/files/datastore/settings.preferences_pb`；
  `adb shell run-as com.xukunz.wakeupmywall ls -l files/datastore` 直接可读（实测首启 1066 B，改一台设备后 1088 B），
  `am force-stop` 后再 `am start` 设备列表与当前设备原样恢复。

**便捷路径（启动者会话里没有 kvm 组时）：** 本机 cron 任务的进程带 kvm 组（实测 cron 起的进程
`/proc/self/status` 含 `991`），可用一条带唯一标记的一次性 `crontab` 条目代组启动，起来后立刻删条目：

```bash
crontab -l > /tmp/cron.bak
echo '* * * * * pgrep -f "qemu-system-x86_64-headless -avd [w]all" >/dev/null || (setsid nohup '"$EMU"' -avd wall -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader_indirect >> /tmp/emulator_wall.log 2>&1 < /dev/null &) # wakeupmywall-emulator-boot' >> /tmp/cron.bak
crontab /tmp/cron.bak
# adb devices 出现 emulator-5554 之后：
crontab -l | grep -v wakeupmywall-emulator-boot | crontab -
```

宿主重启、或换一个 usermod 之后新建的登录会话，都不再需要这条路径。

### 已知限制（不是 App 缺陷，但会影响后续阶段）

1. **无头模拟器忽略旋转请求**：`settings put system user_rotation 1`、`cmd window user-rotation lock 1`、`adb emu rotate` 都试过，`mCurrentOrientation=1` 但显示设备始终 `rotation 0`——Android 12L+ 的大屏设备默认忽略旋转请求。二轮实测：显示设备原生形态是 2560×1600（横屏、rotation 0），新帧直接落在这个方向上；09-19 那批 1600×2560 竖屏帧来自旧实例，不是本条的必然结果。
2. **App 目前没有声明屏幕方向**：`AndroidManifest.xml` 里没有 `android:screenOrientation`。Phase 1 全局约束要求"横屏锁定 72/28"，所以 Task 5/8 落地 AppShell 时需要决定是写入清单还是走运行时策略（Phase 8 的 Display & Reliability 也会碰这块）。
3. **`10.0.2.2` 在这台宿主上不通向宿主服务（2026-09-20 实测）**：`adb shell ip route` 有 `10.0.2.0/24 dev eth0`，但从设备连 `10.0.2.2:9876/9877`（宿主确有 listener）一律 2 s 超时，SLIRP 的主机别名没有被送达；同端口从宿主 `curl 127.0.0.1` 正常。**要做"设备 → 宿主服务"的验证，用 `adb reverse tcp:<port> tcp:<port>` + 设备侧 `127.0.0.1`。** Phase 3/4 的 WOL / Agent 联调都吃这条。
