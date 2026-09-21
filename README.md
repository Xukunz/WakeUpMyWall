# Wake Up My Wall

Desktop Companion——把手机/平板变成桌面控制面板：远端唤醒、电源控制、指标监控与桌搭外观都在一块屏上完成。

仓库是 monorepo：移动端（`mobile/`）、PC Agent（`agent/`，C# / .NET 10）与设计/计划文档放在一起。

## 项目状态

**Phase 5（PC Monitor 实时化）已全部完成**（5A 指标端点 / 5B 手机端实时化 / 5C 流式通道），Phase 0–4 也已交付（见 [路标](docs/superpowers/plans/2026-09-19-roadmap.md)）。仍有**需要你在真机上执行**的验收：Phase 3 的"对目标 PC 连续 10 次开机"（[Phase 3 计划](docs/superpowers/plans/2026-09-20-phase3-wol.md) Task 5 Step 3）、Phase 4A 的"Windows 上四种电源操作 + 服务自启"（[Phase 4A 计划](docs/superpowers/plans/2026-09-20-phase4a-pc-agent.md) Task A4 Step 3），以及 Phase 5A 的"读数与任务管理器一致"（[agent/README.md](agent/README.md) 的清单）。已完成：

- Kotlin Multiplatform 工程骨架（`commonMain` 不依赖任何 Android API）
- 领域模型与 PC 状态机（纯函数 + 单元测试）
- 网络抽象（Ktor + 请求超时 + 统一错误映射）与存储抽象（内存实现 + DataStore 落盘）
- 五个屏幕（Dashboard / Monitor / StandBy / Device Setup / Wallpaper & Personalization）、三工作空间导航、暗色主题与 6 套强调色
- Power Rail 五态与连接条、60 秒指标 Ring Buffer、Settings 表单校验与 Live Preview
- 响应式外壳：墙面屏/内屏走 72/28 侧栏，手机 20:9、折叠外屏 21.1:9 走底部常驻栏
- 内置壁纸 7 张（默认 Dusk Lake）、11+1 类天气图标、3 张设备封面、电源发光环（代码绘制）
- Android 真机证据：Dashboard / Monitor / StandBy 三张 2560×1600 帧 + 刘海安全区实测（见 [docs/plans/screenshots/](docs/plans/screenshots/)）
- 设备系统：设备从 Mock 常量变成持久化数据 —— `KeyValueStore` → `JsonSettingsStorage`（bad data 退回默认值）→ `DeviceRepository`（增删改 + 唯一默认设备 + 首次播种），落盘在 DataStore Preferences
- Device Setup 接真实设备：Port 步进器、Saved Computers 变成选择器（点行=切换当前设备、`✕`=删除、`+ Add Device`=新增），Rail / Dashboard / 表单都跟随仓库里的当前设备
- `Test Connection` 给出具体失败原因：连不上 / 被拒 / 超时 / 返回 404（"这不是 WakeUpMyWall Agent"）/ 401 / 流中断，六类都有可读结论，没有静默 `Failed`
- Android 真机验收：冷启动后设备列表与当前设备原样恢复（`files/datastore/settings.preferences_pb`）、删除当前设备后默认设备自动回落、四类连接结论实测（见 [Phase 2 计划](docs/superpowers/plans/2026-09-20-phase2-device-system.md) §4 验收实录）
- WOL：102 字节魔包（6×`0xFF` + MAC×16，纯函数 + 逐字节单测）、UDP 广播 ×3（JDK `DatagramSocket`，`broadcast = true`）、`WAKING` 期间每 2 秒轮询 `GET /api/v1/status`，应答即 `ONLINE`，预算耗尽回落 `WOL_READY` 并给出 `Sent 3 wake packets — no answer …` 这类解释行
- 状态派生：`Test Connection` 的结论会喂给状态机 —— Agent 不可达但设备有 MAC/广播（`PcDevice.isWakeable`）时落 `WOL_READY`（主环可点），否则 `OFFLINE`
- WOL 真机验收（模拟器）：logcat `sent 102 bytes x3 to 192.168.1.255:9`、rail `Waking PC…` → 60 s 后带原因回落、stub Agent 应答后回到 `Online`（见 [Phase 3 计划](docs/superpowers/plans/2026-09-20-phase3-wol.md) §4.2、`docs/plans/screenshots/phase3-*.png`）
- PC Agent（C# / .NET 10）：`GET /api/v1/status`（免鉴权）、`POST /api/v1/pairing`（6 位一次性配对码换 Token）、`POST /api/v1/power/{sleep,shutdown,restart,lock}`（命令走参数数组，未知动作 404 且不执行）、`GET /api/v1/actions`（白名单）、全部受保护端点缺 Token 一律 401
- 手机端接入 Agent：Keystore AES-GCM 存 Token（明文不落盘）、Device Setup 的 `Advanced / Agent` 配对区、每 5 秒探一次 `/api/v1/status` 判断"PC 是否开着"、三种电源动作走真实接口（未配对时给出可读提示）
- **指标端点（Phase 5A）**：`GET /api/v1/system` 返回 spec §8 的全部指标（CPU 使用率/温度/频率/核心、GPU 使用率/温度/显存/风扇、内存、系统盘、主板与 SSD 温度、机箱风扇、上下行 Mbps、Uptime），字段名与单位见 [agent-api.md](docs/plans/agent-api.md)；取不到的传感器给 `null` 而不是 0；Windows 走 LibreHardwareMonitor 0.9.6，非 Windows 走 `--fake-metrics` 合成读数（契约一致，端到端可验）
- **Monitor 实时化（Phase 5B）**：手机端每 2 秒取一次指标，Monitor 的身份卡/四张指标卡/温度风扇/网络/运行时长全部接真数据，60 点 Ring Buffer 喂 sparkline；**取不到的读数显示 `—`（不是 0）**，掉线 3 次后标注 `Last update …` + `No fresh metrics` 并给出原因，未配对时明确提示且不发请求
- **流式通道（Phase 5C）**：`WS /ws/v1/metrics` 每秒推一帧（握手 Bearer 鉴权），手机端 1 Hz 收帧、**ACTIVE 1s / IDLE 5s** 自适应（60 秒无触摸转 IDLE），掉线指数退避重连（1→8 秒封顶）并在断流期间用 HTTP 轮询兜底；老 Agent（没有该端点）自动降级为 60 秒一探
- GitHub Actions CI：单元测试 + Compose UI 测试 + Debug 组装

计划与验收标准：

- Phase 1：[2026-09-19-phase1-design-system-and-mock-ui.md](docs/superpowers/plans/2026-09-19-phase1-design-system-and-mock-ui.md)、视觉复核 [phase1-visual-review.md](docs/plans/phase1-visual-review.md)
- Phase 2（已完成）：[2026-09-20-phase2-device-system.md](docs/superpowers/plans/2026-09-20-phase2-device-system.md)
- Phase 3（代码与模拟器验收已完成，真机 10/10 待执行）：[2026-09-20-phase3-wol.md](docs/superpowers/plans/2026-09-20-phase3-wol.md)
- Phase 4A（Agent 服务端，Windows 自验待执行）：[2026-09-20-phase4a-pc-agent.md](docs/superpowers/plans/2026-09-20-phase4a-pc-agent.md)
- Phase 4B（手机端接入 Agent）、Phase 5A（Agent 指标端点，已完成）：[2026-09-20-phase4b-app-agent-integration.md](docs/superpowers/plans/2026-09-20-phase4b-app-agent-integration.md)、[2026-09-20-phase5a-agent-metrics.md](docs/superpowers/plans/2026-09-20-phase5a-agent-metrics.md)
- 全阶段路标：[2026-09-19-roadmap.md](docs/superpowers/plans/2026-09-19-roadmap.md)

## 技术栈

- Kotlin 2.2.10 / Kotlin Multiplatform
- Compose Multiplatform 1.10.3 / Material 3
- Android Gradle Plugin 9.3.3 / Gradle 9.5.0
- Ktor 3.6.0、kotlinx-serialization 1.11.0、kotlinx-coroutines 1.11.0、kotlinx-datetime 0.8.0
- DataStore Preferences 1.2.1（仅 androidMain；设备列表与外观设置落盘）、JDK `java.net.Socket`（androidMain 的 TCP 探测）
- Android：minSdk 30 / targetSdk 37 / compileSdk 37

实测通过的完整版本矩阵与依据见 [docs/plans/version-matrix.md](docs/plans/version-matrix.md)。

## 环境要求

- JDK 25（实测 Temurin 25.0.4.1+1）
- Android SDK：`platforms;android-37.0`、`build-tools;37.0.0`、`platform-tools`
- 本机 SDK 路径写入 `local.properties`（`sdk.dir=...`，该文件不入库）

Gradle 由 Wrapper 自动下载，不需要预装。

## 项目结构

```text
mobile/composeApp/                 # 移动端（KMP）
  src/commonMain/                  # 全部业务逻辑与 UI，禁止 import android.*
  src/commonMain/composeResources/ # 内置壁纸等打包资源
  src/androidMain/                 # MainActivity、清单与 Android 资源
  src/commonTest/                  # 纯逻辑单元测试（两端共用）
  src/desktopTest/                 # Compose UI 测试（PC 上跑，无需模拟器）
agent/                             # PC Agent（Phase 4 引入）
docs/                              # 产品设计、规范、路标与分阶段实施计划
imgs/concept/                      # 概念图
imgs/ui/                           # UI 素材母版（11 个天气图标 / PC 封面 / 电源按钮底图）
imgs/wallpaper/                    # 内置壁纸母版与派生规则
tools/prepare_ui_assets.py         # 素材加工：裁剪透明边、统一留白与尺寸、派生打包资源
```

`jvm("desktop")` target 只用于 Compose UI 测试与设计预览，不是产品形态。

## 构建与运行

```bash
./gradlew :composeApp:assembleDebug   # 产出 APK
./gradlew :composeApp:installDebug    # 安装到已连接的设备或模拟器
```

APK 位于 `mobile/composeApp/build/outputs/apk/debug/`。

### 在 Android Studio 中打开

打开项目后**必须先完成一次 Gradle Sync**。同步成功之前 Android Studio 认不出 Android facet，Run 按钮会一直是灰的——这不是项目坏了。如果你刚克隆仓库就点 Run，先做 Sync。

同步失败时优先看两点：

- **Gradle JDK 必须是 25**。`gradle/gradle-daemon-jvm.properties` 把 daemon JVM 钉在 Java 25（`toolchainVersion=25`），而 Android Studio 自带的 JBR 通常是 21，它会通过 foojay 去下载 25；网络受限时会直接失败，表现为同步报错。设置位置：Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK。
- **Android Studio 版本要能识别 AGP 9.3.3**，过老的版本会直接报 incompatible。

## 测试

```bash
./gradlew :composeApp:testDebugUnitTest   # 单元测试（含 commonTest）
./gradlew :composeApp:desktopTest         # Compose UI 测试（PC 上运行，无需模拟器）
./gradlew :composeApp:assembleDebug       # 组装 Android Debug 包
```

`desktopTest` 在无显示环境（`DISPLAY` 未设置）下也能运行，CI 无需 xvfb。

需要在真机或模拟器上验证启动时，环境配方（KVM 权限、AVD 创建、无头启动与截图命令）见 [docs/plans/version-matrix.md](docs/plans/version-matrix.md) §6。

## 壁纸资源

内置壁纸母版、派生规则与更新流程见 [imgs/wallpaper/README.md](imgs/wallpaper/README.md)。

## UI 素材

`imgs/ui/` 下的 PNG 母版（天气图标、PC 封面、电源按钮底图）**不直接进包**，先由
`python3 tools/prepare_ui_assets.py` 加工到 `mobile/composeApp/src/commonMain/composeResources/drawable/`。
脚本幂等，改素材后重跑即可；缺口清单与接入记录见
[docs/plans/2026-09-20-ui-asset-integration.md](docs/plans/2026-09-20-ui-asset-integration.md)。

## 许可证

本项目采用 [MIT License](LICENSE) 开源。
