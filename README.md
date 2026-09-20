# Wake Up My Wall

Desktop Companion——把手机/平板变成桌面控制面板：远端唤醒、电源控制、指标监控与桌搭外观都在一块屏上完成。

仓库是 monorepo：移动端、PC Agent（Phase 4 引入）与设计/计划文档放在一起。

## 项目状态

当前处于 **Phase 2（设备系统）已完成、Phase 3（唤醒与守护）未开始**：Phase 0（KMP 工程基础）、Phase 1（Design System + Mock UI）与 Phase 2 均已交付。已完成：

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
- GitHub Actions CI：单元测试 + Compose UI 测试 + Debug 组装

计划与验收标准：

- Phase 1：[2026-09-19-phase1-design-system-and-mock-ui.md](docs/superpowers/plans/2026-09-19-phase1-design-system-and-mock-ui.md)、视觉复核 [phase1-visual-review.md](docs/plans/phase1-visual-review.md)
- Phase 2（已完成）：[2026-09-20-phase2-device-system.md](docs/superpowers/plans/2026-09-20-phase2-device-system.md)
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
