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
| `fold-inner-portrait-monitor.png` / `fold-inner-portrait-settings.png` | 内屏竖放 700×790（第三批补，§7.1 已结） |

新增守卫：

- `AppShellTest` 增加"手机竖屏走底栏""内屏横放走侧栏"两条；
- `BuiltInWallpapersTest` 默认壁纸改为 `dusk_lake`；
- `WeatherKindTest` 兜底从 `Cloudy` 改为 `Unknown`。

完整测试：`./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest` 全绿。

## 7. 遗留 / 可继续迭代

1. ~~折叠屏内屏竖放还没单独抓 Monitor/Settings 的竖屏帧~~ → **已结**，见 §8.1：
   `fold-inner-portrait-monitor.png` / `fold-inner-portrait-settings.png`（700×790）已入库。
2. **Android 真机的刘海 / 手势条安全区仍未验证**（`safeDrawing` 只在代码层成立）。
   2026-09-20 复检仍是同一阻塞：`/dev/kvm` 不存在，模拟器无法加速，见 §8.2。需要真机或恢复 KVM。
3. ~~`device-setup.png` 仍是历史遗留~~ → **已结**，见 §8.3：该文件已删除（内容与 `settings.png` 重复）。

## 8. 第三批（2026-09-20 收口）

### 8.1 内屏竖放的 Monitor / Settings 帧

内屏竖放是 700dp 宽，比手机（412dp）宽得多，但比横放的 790dp + 侧栏形态窄。700dp 撑满后
主区约 668dp，两个阈值（`stackedRowsMaxWidth = 560dp`、`inlineNavMaxWidth = 620dp`）都**没到**，
所以 Monitor 的身份卡/快捷动作**仍然并排**、Settings 左导航**仍然是 240dp 侧栏**——
它既不是手机分支，也不是墙面屏分支。这正是"必须单独出帧"的理由：靠另外两种比例的帧推不出这一支。

两张新帧都是 `AppScreenshotTest` 里新增的 `capture foldable inner screen portrait monitor/settings`，
尺寸由测试断言（700×790）。**帧内容是滚动的第一屏**：Monitor 在列数 < 5 时开纵向滚动、
Device Setup 表单同样可滚动（`Breakpoints.requiresVerticalScroll` / `DeviceSetupScreen` 的
`verticalScroll`），所以底部卡片被视口切断是滚动视口的正常表现，与 `phone-monitor.png` 一致，不是裁剪缺陷。

### 8.2 Android 侧复检（结论：仍被环境阻塞）

逐条实测证据：

| 检查 | 结果 |
| --- | --- |
| `ls /dev/kvm` | `No such file or directory` |
| `emulator -accel-check` | `accel:` / `8` / `/dev/kvm is not found: VT disabled in BIOS or KVM kernel module not loaded` |
| `emulator -avd wall -no-window -accel off` | 90 秒内 `FATAL \| A snapshot operation for 'wall' is pending and timeout has expired` 后退出 |
| AVD 镜像 ABI | `abi.type=x86_64`（无 KVM = 纯软件模拟，不可用） |

因此这一批**没有**新增 Android 帧；`android-dashboard.png` / `android-monitor.png` 已在
[screenshots/README.md](screenshots/README.md) 标注为过期（2026-09-19 抓的，早于本批重写），
StandBy 的 Android 帧依然缺。恢复路径见 [version-matrix.md](version-matrix.md) §6（宿主开 VT 或加载 KVM 模块）。

### 8.3 过期证据 `device-setup.png`

该文件是 00:22 那批的孤儿：当前 `AppScreenshotTest` 不抓它，而 Settings 工作空间默认就停在
Device Setup 段，屏幕内容与 `settings.png` 重合。留着等于重演 `android-standby.png` 那次的错误
（一张没人维护的图被当成当轮证据），故删除；git 历史里仍可取回。

### 8.4 本轮新发现（待裁决，本批未改）

**连接条有两行同文案**：规格 A5 的连接条是"图标 + 通道文案 + 箭头"一行；实现额外加了
第二行 `statusLine = PcCapabilities.statusText`，而 ONLINE 的 `statusText` 恰好也是
`Agent connected over LAN`、WOL_READY 的也是 `Wake-on-LAN Ready` —— 与第一行（`connectionLabel`）
逐字相同，同一张卡里同一句话出现两次。证据：`monitor.png` 右栏连接条、
`fold-inner-portrait-monitor.png` 底栏（顶行与最底行）。OFFLINE / WAKING 等状态两行内容不同，是有用的。

两个方向都只改渲染，不改模型：(a) 第二行与第一行相同时不渲染（贴合概念图的单行）；
(b) 保留双行，但把第二行换成补充信息（如 `Last seen 1 min ago`）。
这会动到 `PowerRailUiTest` 里"每个元素都有 testTag"那条断言（`powerrail:status`），故留给你裁决。

### 8.5 本批验证

```bash
env JAVA_HOME=<jdk-25> ./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest --offline
# BUILD SUCCESSFUL —— testDebugUnitTest 101 / desktopTest 186（+2 条新截图用例），0 failures
```

截图由同一次 `desktopTest` 产出并断言尺寸；其余 13 张帧的 MD5 与本批重跑完全一致
（渲染可复现），只有两张新帧是新文件。
