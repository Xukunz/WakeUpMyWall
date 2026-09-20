# Wake Up My Wall

Desktop Companion——把手机/平板变成桌面控制面板：远端唤醒、电源控制、指标监控与桌搭外观都在一块屏上完成。

仓库是 monorepo：移动端、PC Agent（Phase 4 引入）与设计/计划文档放在一起。

## 项目状态

当前处于 **Phase 0（KMP 工程基础）**，已完成：

- Kotlin Multiplatform 工程骨架（`commonMain` 不依赖任何 Android API）
- 领域模型与 PC 状态机（纯函数 + 单元测试）
- 网络抽象（Ktor + 请求超时 + 统一错误映射）与存储抽象（内存实现，真实后端留到 Phase 2）
- 三个工作空间的导航骨架、暗色主题与 6 套强调色令牌
- 内置壁纸（默认 Dusk Lake），并带资源级 UI 测试
- GitHub Actions CI：单元测试 + Compose UI 测试 + Debug 组装
- 内置壁纸 7 张（默认 Dusk Lake）、11+1 类天气图标、3 张设备封面、电源发光环（代码绘制）
- 响应式外壳：墙面屏/内屏走 72/28 侧栏，手机 20:9、折叠外屏 21.1:9 走底部常驻栏

详细计划与验收标准见 [docs/superpowers/plans/2026-09-19-phase0-kmp-foundation.md](docs/superpowers/plans/2026-09-19-phase0-kmp-foundation.md)。

## 技术栈

- Kotlin 2.2.10 / Kotlin Multiplatform
- Compose Multiplatform 1.10.3 / Material 3
- Android Gradle Plugin 9.3.3 / Gradle 9.5.0
- Ktor 3.6.0、kotlinx-serialization 1.11.0、kotlinx-coroutines 1.11.0、kotlinx-datetime 0.8.0
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
