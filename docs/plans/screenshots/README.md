# 截图证据目录

这些 PNG 是**真实渲染帧**（不是设计稿、不是示意图），由无头桌面渲染管线产出：

```bash
JAVA_HOME=<jdk25> ./gradlew :composeApp:desktopTest --tests "*AppScreenshotTest*"
# 产物先落到 mobile/composeApp/build/screenshots/（文件名见 AppScreenshotTest），再复制到这里并改名：
#   dashboard-default.png     → dashboard.png
#   monitor-default.png       → monitor.png
#   standby-default.png       → standby.png
#   settings-default.png      → settings.png
#   appearance-live-preview.png → personalization.png
#   dashboard-compact.png / dashboard-minimal.png 同名复制
#   phone-portrait-*.png      → phone-*.png
#   fold-outer-dashboard.png  → fold-outer.png
#   fold-inner-dashboard.png  → fold-inner.png
#   fold-inner-portrait-dashboard.png → fold-inner-portrait.png
#   fold-inner-portrait-monitor.png / fold-inner-portrait-settings.png 同名复制
```

## 帧尺寸

| 文件 | 帧尺寸 | 说明 |
| --- | --- | --- |
| `dashboard.png` / `monitor.png` / `standby.png` / `settings.png` / `personalization.png` | 1280×720 | 墙面屏形态（2560×1440 @320dpi 的 dp 尺寸） |
| `dashboard-compact.png` | 800×720 | 窄屏兜底形态（主区 576dp → 2 列 + 纵向滚动） |
| `dashboard-minimal.png` | 1280×720 | 另一张内置壁纸下的同一屏 |
| `phone-dashboard.png` / `phone-monitor.png` / `phone-settings.png` | 412×915 | 主流安卓 20:9（底部常驻控制栏） |
| `fold-outer.png` | 412×965 | 折叠外屏 21.1:9（底部常驻控制栏） |
| `fold-inner.png` / `fold-inner-portrait.png` | 790×700 / 700×790 | 内屏 4:3.55 横放 / 竖放 |
| `fold-inner-portrait-monitor.png` / `fold-inner-portrait-settings.png` | 700×790 | 内屏竖放的 Monitor / Settings：内屏比手机宽（700dp vs 412dp），内部重排走的是另一条分支 |

> `device-setup.png` 已于 2026-09-20 删除：它是 00:22 那一批留下的孤儿文件，没有对应的抓取用例，
> 屏幕内容与 `settings.png`（Settings 工作空间默认就停在 Device Setup 段）重复。留两个同内容的文件
> 会重演 `android-standby.png` 那次的错误——一张没人维护的图被当成当轮证据引用。需要时从
> `fold-inner-portrait-settings.png` / `settings.png` 取。
> 图片素材接入的复核记录见 [2026-09-20-ui-asset-integration.md](../2026-09-20-ui-asset-integration.md)；
> 响应式与概念图对齐见 [2026-09-20-responsive-and-concept-alignment.md](../2026-09-20-responsive-and-concept-alignment.md)。

尺寸由 `AppScreenshotTest` 里的 `FrameWidth`/`FrameHeight` 显式指定，并用断言守住：产出的 PNG 尺寸与声明的渲染条件不一致时测试直接失败。

> 2026-09-20 第四批（连接条去重，见[对齐文档](../2026-09-20-responsive-and-concept-alignment.md) §9）重出了
> 13 张**含常驻栏**的帧：`dashboard.png` / `monitor.png` / `settings.png` / `dashboard-compact.png` /
> `dashboard-minimal.png` / `phone-*.png` / `fold-*.png`。不含常驻栏的 `standby.png` 与 `personalization.png`
> 的 MD5 与上一批完全相同——这也是一道旁证：改动只落在栏上，没有波及其他屏幕。

> 这条守卫是补上的：2026-09-19 那批截图用的是默认的 1024×768 测试窗口，`Modifier.size(1280.dp, 720.dp)` 被入参约束压回 1024 宽，于是"按 1280dp 设计"的布局实际是在 737dp 主区里渲染的，而复核文档当时写的渲染条件是 1280×720 —— 图和文档对不上。2026-09-20 已按 1280×720 重出并加断言。

## 与概念图的对应关系

| 截图 | 比对基准 |
| --- | --- |
| `dashboard.png` | 概念图 `imgs/concept/0499d5bc-….png`（逐项比对） |
| `monitor.png` | 概念图 `imgs/concept/cb576a75-….png`（逐项比对） |
| `standby.png` | 权威规格 D 表（概念图 `image-gen-5.png` 由设计评审转写成表） |
| `settings.png` | Settings 工作空间骨架（左导航 + Device Setup 内容） |
| `personalization.png` | 权威规格 F 表 |

逐项结论见 [phase1-visual-review.md](../phase1-visual-review.md)。

## Android 侧

`android-dashboard.png` / `android-monitor.png` 是无头 Nexus 10 模拟器（API 37）上的真实帧截图，用于"桌面渲染与 Android 真机均正常"这条验收标准。

> **这两张帧现在标注为过期**：它们是 2026-09-19 23:30 抓的，早于 2026-09-20 的素材接入与响应式重写，
> 不能作为当前代码在 Android 上的证据。重抓在 2026-09-20 复检时**仍然做不到**，原因见下。
>
> 原来还有一张 `android-standby.png`，它与 `android-monitor.png` **逐字节相同**（同一 MD5），说明抓图时 App 还停在 Monitor 形态，是无效证据，2026-09-20 已删除。StandBy 在 Android 上的截图因此**仍缺**。
>
> 重抓被环境挡住（2026-09-20 复检，证据逐字如下）：`/dev/kvm` **不存在**（`ls /dev/kvm` → `No such file or directory`），
> `emulator -accel-check` 输出 `accel:` / `8` / `/dev/kvm is not found: VT disabled in BIOS or KVM kernel module not loaded`；
> 强行 `-accel off` 启动 `wall` AVD 在 90 秒内以 `FATAL | A snapshot operation for 'wall' is pending and timeout has expired` 退出。
> AVD 镜像是 `abi.type=x86_64`，没有 KVM 就等于纯软件模拟，跑不动。恢复需要宿主机开 VT 或加载 KVM 模块（`sudo` 层操作，容器内无法自助）。
>
> 另：无头模拟器转不到横屏（Android 12L+ 大屏忽略旋转请求），所以 Android 截图是 1600×2560 竖屏，与桌面 1280×720 的横屏形态不是同一个断点区间。详见 [version-matrix.md](../version-matrix.md) 第 6 节。
