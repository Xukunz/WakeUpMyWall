# Wake Up My Wall

Desktop Companion——把手机/平板变成桌面控制面板：远端唤醒、电源控制、指标监控与桌搭外观都在一块屏上完成。

仓库是 monorepo：移动端、PC Agent（Phase 4 引入）与设计/计划文档放在一起。

## 项目状态

当前处于 **Phase 0（KMP 工程基础）**，已完成：

- Kotlin Multiplatform 工程骨架（`commonMain` 不依赖任何 Android API）
- 领域模型与 PC 状态机（纯函数 + 单元测试）
- 网络抽象（Ktor + 请求超时 + 统一错误映射）与存储抽象（内存实现，真实后端留到 Phase 2）
- 三个工作空间的导航骨架、暗色主题与 6 套强调色令牌
- 内置壁纸（默认 Aurora），并带资源级 UI 测试
- GitHub Actions CI：单元测试 + Compose UI 测试 + Debug 组装

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
imgs/wallpaper/                    # 内置壁纸母版与派生规则
```

`jvm("desktop")` target 只用于 Compose UI 测试与设计预览，不是产品形态。

## 构建与运行

```bash
./gradlew :composeApp:assembleDebug   # 产出 APK
./gradlew :composeApp:installDebug    # 安装到已连接的设备或模拟器
```

APK 位于 `mobile/composeApp/build/outputs/apk/debug/`。

## 测试

```bash
./gradlew :composeApp:testDebugUnitTest   # 单元测试（含 commonTest）
./gradlew :composeApp:desktopTest         # Compose UI 测试（PC 上运行，无需模拟器）
./gradlew :composeApp:assembleDebug       # 组装 Android Debug 包
```

`desktopTest` 在无显示环境（`DISPLAY` 未设置）下也能运行，CI 无需 xvfb。

## 壁纸资源

内置壁纸母版、派生规则与更新流程见 [imgs/wallpaper/README.md](imgs/wallpaper/README.md)。

## 许可证

本项目采用 [MIT License](LICENSE) 开源。
