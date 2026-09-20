# 响应式布局与概念图对齐（2026-09-20 第二批）

**起因：** 用户确认两点，并追加一条硬要求——

1. 新增素材：`imgs/ui/pc_cover_1.png`、`pc_cover_2.png`（另外两台设备的封面）、
   `weather_unknown.png`（无法归类天气的兜底图标）。
2. 默认壁纸改成 `Dusk Lake`；StandBy 浮层卡、主电源按钮都按概念图实现。
3. **考虑手机真实屏幕比例**：主流安卓 20:9、双折叠外屏 21.1:9、内屏 4:3.55，
   要对不同比例做好兼容。概念图是最终效果目标，可持续迭代逼近。

## 1. 默认壁纸 → Dusk Lake

概念图 F 行把 `Dusk Lake` 标成默认（带对勾）。`BuiltInWallpapers.DefaultId` 由 `aurora` 改为
`dusk_lake`，`Aurora` 降为普通备选。顺带修掉一个真 bug：`byId()` 的兜底原来写死 `?: Aurora`，
默认值一改界面就会悄悄显示成另一张图——现在兜底走 `DefaultId`，且 `DefaultId` 不在编目里时直接炸，
而不是静默回退。

## 2. 电源按钮 → 概念图发光环

`PowerRingButton` 弃用写实旋钮底图，改为纯代码绘制：三层递减外辉光 + 一条实心环 + 环心字形，
环色取 `onlineColor`（概念图三个屏幕里电源环都是"电源绿"，与用于选中态的蓝色 accent 是两套语义）。
任何直径都不糊（Rail 220dp / StandBy 168dp / 底栏 88dp 用同一套绘制），换强调色也跟着变色。

`imgs/ui/power_button_base.png`（写实旋钮）**暂时不进包**：母版保留在 `imgs/ui/`，
`tools/prepare_ui_assets.py` 里留了三行注释，将来要做"实体按钮"风格时取消注释即可恢复打包。

## 3. StandBy 浮层卡 → 概念图 D

逐项对齐 `imgs/concept/image-gen-5.png`：

- 顶行：`A MORE FOCUSED TOMORROW` + 短分割线（左）、透视装饰（右）；
- 时钟 + 长日期；左下 Weather 卡（图标 / 大字温度 / 右侧上下两行高低温 / 天气 / 地点 / 一句话天气）；
- 左下 Next Event 卡（日历图标 + 标题 + 右侧倒计时 + 日程名 / 时间段 / 参与者图标 + 来源）；
- 右侧 PC 浮层卡：`My PC` + `POWER CONTROL` + `>`，发光环，`Power On` / `WAKE YOUR PC`，
  `Night Mode` / `Auto-Dim` 两枚带图标的开关瓦片；
- 底部状态条：显示器图标 + 绿点 + 名称 + 状态（在线色）+ `Last seen …` + `>`。

浮层卡环下的文案仍取自 `PowerRailModel.primaryLabel / primaryCaption`：可唤醒时就是概念图的
`Power On` / `WAKE YOUR PC`，机器已在线时显示状态（`PC Online` / `AGENT CONNECTED`），
不会在已开机时假装还能"唤醒"。抓图用 `WOL_READY`，因为概念图里环是可按的唤醒动作。

## 4. 响应式：侧栏 vs 底栏

`AppShell` 不再锁死横屏。`Breakpoints.usesSideRail(width, height)` 决定形态：

| 屏幕 | 典型 dp | 形态 |
| --- | --- | --- |
| 墙面屏 | 1280×720 | 侧栏（72/28） |
| 内屏横放（4:3.55） | ~790×700 | 侧栏（72/28） |
| 内屏竖放（4:3.55） | ~700×790 | 底栏 |
| 手机（20:9） | 412×915 | 底栏 |
| 折叠外屏（21.1:9） | 412×965 | 底栏 |

底栏是新增的 `PowerRailCompact`：绿点 + 状态 + 通道文案 + 齿轮 → 小环 + 主标签 → 三枚次级动作瓦片。
与侧栏共用同一份 `PowerRailModel` 与同一批 testTag，语义不变。

各屏内部的断点也补齐了：

- Dashboard：竖屏窄机（< 480dp）单列 + 纵向滚动；否则沿用 3 列栅格；
- Monitor：竖屏窄机身份卡与快捷动作不再并排；指标卡仍按 2 列；底行按概念图给不等权重；
- Settings / Appearance：窄窗口左导航改成顶部横向滚动条目条，Appearance 的四段控制条改为纵向排列；
- Device Setup：窄窗口三块（表单 / Saved Computers / Services）纵向堆叠；
- StandBy：竖屏单列纵向滚动。

另外 `App` 最外层加了 `WindowInsets.safeDrawing`：Android 15+ 强制 edge-to-edge，真实手机上
不躲系统栏会出现"齿轮被状态栏压住"。壁纸仍铺满整屏（含刘海区），只把交互内容收进安全区。

## 5. 新增素材的接线

| 素材 | 去向 |
| --- | --- |
| `pc_cover_1.png` / `pc_cover_2.png` | 按设备 id 取封面：`living-room` → cover_1、`workstation` → cover_2，其余回落默认封面 |
| `weather_unknown.png` | `weatherKind()` 认不出的 condition 落到 `WeatherKind.Unknown`（不再假装多云） |

`PowerRailModel` 增加 `deviceId`，`PcCover(deviceId)` 据此选图；`PcSummaryWidget` 与
`MonitorMode` 的身份卡都接上了真实封面（取不到 id 一律回落默认封面）。

## 6. 证据与验证

截图证据覆盖三种真实比例（真实渲染帧，`captureToImage` 在布局崩溃时会直接让测试失败）：

| 帧 | 比例 / 尺寸 |
| --- | --- |
| `dashboard.png` / `monitor.png` / `standby.png` / `settings.png` / `personalization.png` | 墙面屏 1280×720 |
| `dashboard-compact.png` | 800×720 窄屏 |
| `phone-dashboard.png` / `phone-monitor.png` / `phone-settings.png` | 手机 20:9（412×915） |
| `fold-outer.png` | 折叠外屏 21.1:9（412×965） |
| `fold-inner.png` / `fold-inner-portrait.png` | 内屏 4:3.55（790×700 / 700×790） |

新增守卫：

- `AppShellTest` 增加"手机竖屏走底栏""内屏横放走侧栏"两条；
- `BuiltInWallpapersTest` 默认壁纸改为 `dusk_lake`；
- `WeatherKindTest` 兜底从 `Cloudy` 改为 `Unknown`。

完整测试：`./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 全绿。

## 7. 遗留 / 可继续迭代

1. 折叠屏的**内屏竖放**与**外屏**是两种窄窗口，但内屏竖放（~700dp 宽）仍用 3 列 Dashboard——
   比手机宽松，符合预期，只是还没单独抓 Monitor/Settings 的竖屏帧，需要时补。
2. Android 真机的刘海 / 手势条安全区目前靠 `safeDrawing` 统一处理，具体机型还需真机确认。
3. `device-setup.png` 仍是历史遗留（`AppScreenshotTest` 不抓该屏，改用 `settings.png`）。
