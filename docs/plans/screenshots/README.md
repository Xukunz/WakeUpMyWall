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
| `android-dashboard.png` / `android-monitor.png` / `android-standby.png` / `android-notch-dashboard.png` | 2560×1600 | 无头 Nexus 10 模拟器（API 37）上的真机帧，见下节 |

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

`android-dashboard.png` / `android-monitor.png` / `android-standby.png` 是无头 Nexus 10 模拟器（API 37，
2560×1600 横屏）上的真实帧，用于"桌面渲染与 Android 真机均正常"这条验收标准。

**2026-09-20 二轮：三张帧已按当前代码重抓，"过期"标注撤销。** 抓帧时模拟器进程带 kvm 组（硬件加速，
冷启动 19.8 秒），App 是提交 `1f5ab94` 的构建，帧由 `adb exec-out screencap -p` 直接取得：

```bash
ADB="$ANDROID_SDK_ROOT/platform-tools/adb"
"$ADB" -s emulator-5554 shell am force-stop com.xukunz.wakeupmywall
"$ADB" -s emulator-5554 shell am start -n com.xukunz.wakeupmywall/.MainActivity && sleep 8
"$ADB" -s emulator-5554 exec-out screencap -p > android-dashboard.png
"$ADB" -s emulator-5554 shell input swipe 1500 700 500 700 300   # 左滑前进一态 → Monitor
sleep 3; "$ADB" -s emulator-5554 exec-out screencap -p > android-monitor.png
"$ADB" -s emulator-5554 shell input swipe 1500 700 500 700 300   # 再前进一态 → StandBy
sleep 3; "$ADB" -s emulator-5554 exec-out screencap -p > android-standby.png
```

三张帧 MD5 互不相同。历史上 `android-standby.png` 曾与 `android-monitor.png` **逐字节相同**（抓图时 App 还停在
Monitor），那张无效证据已删除；**这次能拍出 StandBy 帧本身就说明三态切换修好了**——在此之前 App 的滑动手势
只映射 Dashboard↔Monitor，`HomeModeController` 的三态逻辑只被测试调用、没接进应用，StandBy 在真机上不可达。
修复见 `1f5ab94`（守卫：`AppUiTest` 的"两次左滑走到 StandBy、两次右滑退回"与 `HomeSurfaceTest` 的单步断言）。

**已知偏差（不变）：** StandBy 帧里的 PC 浮层卡渲染的是 ONLINE 形态（`PC Online` / `AGENT CONNECTED`），
概念图是 WOL_READY 形态（`Power On` / `WAKE YOUR PC` + 右上 `>`）。这条差异记在
[phase1-visual-review.md](../phase1-visual-review.md) §7.3，属未打磨项，不影响"真机上能起来、能出帧"这条结论。

**环境历史（供后来者避坑）：** 09-19 抓的旧帧是 1600×2560 竖屏；2026-09-20 早先记录的"模拟器不可用"结论是
Codex 沙箱造成的假象（沙箱 `/dev` 是 bwrap 私有 devtmpfs，看不到宿主机 `/dev/kvm`）。宿主 `/dev/kvm` 一直在，
真正的阻塞是"启动者会话没有 kvm 组"与"09-19 09:40 起的旧无头实例锁住 `wall` AVD"。逐条证据与恢复步骤见
[version-matrix.md](../version-matrix.md) §6。旋转*请求*仍被忽略（`user_rotation` 读到 1，显示设备始终
2560×1600 / rotation 0），但 Nexus 10 原生就是横屏，所以出帧方向与桌面 1280×720 落在同一个断点区间。

**安全区旁证帧 `android-notch-dashboard.png`（刘海 / 手势条）：** 用 Android 自带的刘海模拟 overlay 在原生几何下
造出 48dp 刘海，验证 `windowInsetsPadding(WindowInsets.safeDrawing)` 真的跟着系统 inset 走：

```bash
"$ADB" -s emulator-5554 shell cmd overlay enable-exclusive --category com.android.internal.display.cutout.emulation.tall
"$ADB" -s emulator-5554 shell dumpsys window displays | grep -m1 mDisplayCutout   # insets=Rect(0, 96 - 0, 0)
"$ADB" -s emulator-5554 exec-out screencap -p > android-notch-dashboard.png
```

- 无刘海时装饰文案 `SAME ROOM` 顶边在 y=304，48dp 刘海下移到 y=352 —— 正好是 safeDrawing 顶边从 24dp（状态栏）
  增到 48dp（刘海）的差值（+24dp = +48px）。StandBy 的时钟同样下移 48px（101 → 149）。
- 手机几何（`wm size 1080x2400` + `wm density 420`）下底栏最后一行文字在 y=2265、手势条在 y=2364，不重叠。
- 反例记录：`wm size` 覆盖会把模拟刘海的 dp 高度一起缩放（41px@420dpi ≈ 15.6dp），小于状态栏的 24dp，
  所以"手机几何 + 刘海"这一组量不出位移——有效的对比是原生几何下的一组，别的组合只会得到假阴性。
