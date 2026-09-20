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
| Room / DataStore | 仍未引入 | 按计划推迟到 Phase 2 选型 |

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

### 2026-09-20 复检：整套模拟器目前不可用

本轮想重抓 Android 帧（含 StandBy）时复检了一次，阻塞点从"权限"变成了"设备就不在"：

```text
$ ls /dev/kvm
ls: cannot access '/dev/kvm': No such file or directory
$ "$ANDROID_SDK_ROOT/emulator/emulator" -accel-check
accel:
8
/dev/kvm is not found: VT disabled in BIOS or KVM kernel module not loaded
$ "$ANDROID_SDK_ROOT/emulator/emulator" -avd wall -no-window -no-audio -no-snapshot -accel off -gpu swiftshader_indirect
FATAL        | A snapshot operation for 'wall' is pending and timeout has expired. Exiting...
```

`wall` AVD 的 `config.ini` 是 `abi.type=x86_64`，无 KVM 时退化成 TCG 软件模拟，90 秒内起不来。
要恢复得在宿主机开 VT 或加载 `kvm` 模块（容器内无法自助，需要 `sudo`）。**在此之前，Android 侧
只能靠桌面渲染管线 + 尺寸断言兜底，不能声称已在 Android 上验证。**

### 已知限制（不是 App 缺陷，但会影响后续阶段）

1. **无头模拟器转不到横屏**：`settings put system user_rotation 1`、`cmd window user-rotation lock 1`、`adb emu rotate` 都试过，`mCurrentOrientation=1` 但显示设备始终 `rotation 0`——Android 12L+ 的大屏设备默认忽略旋转请求。截图因此是 1600×2560 竖屏。
2. **App 目前没有声明屏幕方向**：`AndroidManifest.xml` 里没有 `android:screenOrientation`。Phase 1 全局约束要求"横屏锁定 72/28"，所以 Task 5/8 落地 AppShell 时需要决定是写入清单还是走运行时策略（Phase 8 的 Display & Reliability 也会碰这块）。
