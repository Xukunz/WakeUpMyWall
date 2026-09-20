# Phase 1 视觉还原复核

**日期：** 2026-09-19（**2026-09-20 复核补记见 §7**，那一节推翻了本文两处结论）
**计划：** [Phase 1 实施计划](../superpowers/plans/2026-09-19-phase1-design-system-and-mock-ui.md) Task 15
**权威规格：** 同计划内的「概念图还原规格」A–F 表

## 0. 复核方式与证据边界

渲染证据 = 用 desktop 渲染管线抓的**真实帧**（不是设计稿、不是示意图）。产出命令：

```bash
./gradlew :composeApp:desktopTest --tests "*AppScreenshotTest*"
# 产物在 mobile/composeApp/build/screenshots/，已复制到 docs/plans/screenshots/
```

渲染条件是 1280×720（= 2560×1440 @320dpi 的墙面屏 dp 尺寸，概念图 1672×941 量出的 UI 区域换算一致），因此下列大幅偏差多是**该尺寸下的真实表现**，不是主观印象。

> **更正（2026-09-20）**：上面这句在写本文时**是错的**。09-19 那批截图的测试窗口是默认的 1024×768，`Modifier.size(1280.dp, 720.dp)` 被入参约束压回 1024 宽，实际帧是 **1024×720**，也就是布局按 737dp 主区（3 列区间）跑出来的，与概念图不是同一个断点区间。断言"渲染条件是 1280×720"没有证据支撑。§7 记录了修正与按真实 1280×720 重做的复核。

**证据边界（必须说明）：**

| 屏幕 | 本轮比对基准 |
| --- | --- |
| Home Dashboard | **直接比对概念图** `imgs/concept/0499d5bc-….png` |
| PC Monitor | **直接比对概念图** `imgs/concept/cb576a75-….png` |
| StandBy | 比对权威规格 D 表（该表由设计评审从 `image-gen-5.png` 逐项转写） |
| Device Setup | 比对权威规格 E 表 |
| Wallpaper & Personalization | 比对权威规格 F 表 |

后三屏**没有**逐像素比对原图，结论强度低于前两屏，需要人工过目（见 §6）。

## 1. Home Dashboard（`screenshots/dashboard.png`）

| 维度 | 概念图 | 当前实现 | 结论 |
| --- | --- | --- | --- |
| 布局顺序 | 问候+透视 → 天气/日历/任务 → PC 摘要+引用卡 → 品牌条 | 同 | 已还原 |
| 卡片数量 | 3 行共 7 块 | 同（时钟与几何装饰默认关闭） | 已还原 |
| 设备名 | PC 摘要卡写 `My PC` | 原为 `Desktop-Alpha` | 本轮已修：Mock 设备名改为 `My PC`，`DESKTOP-ALPHA` 只作 Monitor 身份卡主机名 |
| 任务清单 | Finish project notes / Order desk accessories / Reply to Alex / Plan weekend trip / Read 1 chapter | 原为我编的 5 条 | 本轮已修：改为概念图原文 |
| 任务计数 | `3 of 5` 数的是**剩余**待办（图里 2 项已完成） | 原为"已完成数"（数值凑巧相同） | 本轮已修：改为剩余数，语义与概念图一致 |
| 引用卡文案 | `Better Tools / A Calmer Mind` | 原为占位 `Small steps, steady light.` | 本轮已修 |
| 天气地点 | `📍Riverside` | `📍Riverside, CA` | 留到 Phase 6：接真实 Weather Provider 时由 provider 返回格式决定 |
| 逐时天气 | 4 列：时刻在上、图标在中、温度在下，列间留白明显 | 单行文本 `10PM 17°1AM 16°…`，无图标、列间无留白 | 留到视觉打磨：需要图标位（受"不引入图标库"约束）与列间距 |
| 温度区间位置 | `↑22° ↓14°` 在温度右侧 | 在随后的独立行 | 留到视觉打磨（卡片内部栅格重排） |
| PC 摘要指标排布 | CPU / Temp / RAM / Network 四列横向排列，各带彩色细条 | 四项纵向堆叠 | 留到视觉打磨：横向四列要把摘要卡加宽到主区 60% 以上，与引用卡抢位，需一并决定 |
| 完成项样式 | 实心蓝圆 + 白勾 | `●` 字形 + 删除线 | 留到视觉打磨（需矢量勾选图形） |
| 圆角与间距 | 卡片圆角明显大于按钮，卡片内边距约 16–20dp | `AppShapes.card = 20dp` / `Spacing.md = 16dp` | 已还原 |

## 2. PC Monitor（`screenshots/monitor.png`）

| 维度 | 概念图 | 当前实现 | 结论 |
| --- | --- | --- | --- |
| 布局顺序 | 身份卡+快捷动作 → 四指标卡+温度风扇 → 网络/时长/活动/引用卡 → 品牌条 | 同 | 已还原 |
| 身份卡 | 绿点 + `My PC` + `Online` + `Last seen 1 min ago` + 缩略图 + `DESKTOP-ALPHA` + 系统/CPU/GPU + `>` | 只有缩略图 + 主机名 + 三行型号 + `>`，**缺状态行** | 立即修（§6 T1） |
| 快捷动作 | 4 个等宽图标块横向排布，标签在下，右上 `+` | 4 行纵向列表 + `Open X` 文案 | 留到视觉打磨：需要图标位与横向瓦片布局 |
| 指标卡结构 | 图标 + 名称 + 型号小字 → 环形进度（环心百分数）→ sparkline → 两行数值 | 同 | 已还原（本轮删掉重复的大号百分数，只留环心） |
| 指标卡配色 | 每个指标一种色（CPU 蓝 / GPU 绿 / RAM 紫 / Storage 蓝） | 全部用强调色 | 留到 Phase 5：等真实指标接入后按语义配色，避免现在拍脑袋定色板 |
| 温度/风扇 | 每行 = 彩色圆点 + 名称 + 进度条 + 右对齐数值；Fans 三段同构 | 温度有进度条但无圆点；风扇是纯文本三行、无进度条 | 留到视觉打磨（§6 T2） |
| 最近活动 | 彩色圆点 + 应用名 + 右对齐相对时间 | 应用名 + 相对时间（无圆点；窄卡下应用名省略号截断） | 留到视觉打磨（§6 T2） |
| 数值 | 28% / 62% / 38% / 54%、4.9 GHz、8 cores 16 threads、67 °C、8.1 / 12 GB VRAM、12.1 / 32 GB、1.1 / 2.0 TB、554 GB free、68/67/42/38 °C、1,240 / 1,560 / 820 RPM、124.3 / 31.7 Mbps、3d 6h 24m | 全部一致 | 已还原（由 `MockDataTest` 固定） |
| 圆角与间距 | 同 Dashboard | 同 | 已还原 |

## 3. StandBy（`screenshots/standby.png`，基准 = 规格 D 表）

| 维度 | 规格 D | 当前实现 | 结论 |
| --- | --- | --- | --- |
| 时钟 | 超大时钟 + 分钟用 accent + `PM` + 长日期 | 同（`21:04`，`04` 为强调色） | 已还原 |
| 时钟字号 | "超大"未给数值 | 104sp + 显式行高 | 本轮已修：120sp 且未指定行高时数字顶部被裁，降为 104sp 并给足 lineHeight |
| 天气卡 | 含一句话天气 `Clearer skies later tonight.` | 同 | 已还原（新增 `WeatherSnapshot.summary`） |
| Next Event | `In 1 hr 19 min` / `Team sync` / `11:00 PM – 12:00 AM` / `Microsoft Teams` | 同 | 已还原（新增 `NextEvent` + `MockData.nextEvent`） |
| PC 浮层卡 | 右侧浮层：标题 + 环 + `Power On` + `WAKE YOUR PC` + `Night Mode/ON` + `Auto-Dim/ACTIVE` | 同 | 已还原（本轮把铺满整行的卡片收窄到 34%） |
| 不出现 Power Rail | StandBy 用浮层卡，不渲染常驻 Rail | 同（`powerrail` 标签不存在，由测试守住） | 已还原 |
| 底部状态条 | 显示器图标 + 绿点 + `My PC` + `Online` + `Last seen 1 min ago` + `>` | 同 | 已还原 |

## 4. Device Setup 与 Personalization（`screenshots/settings.png`、`personalization.png`）

> 本节原先引的 `screenshots/device-setup.png` 已于 2026-09-20 删除（与 `settings.png` 同屏重复，
> 且没有对应的抓取用例），比对基准改为 `settings.png` —— 它的内容就是 Settings 工作空间停在 Device Setup 段。

基准 = 规格 E/F 表。

| 维度 | 规格 | 当前实现 | 结论 |
| --- | --- | --- | --- |
| Device Setup 三段 | Wake-on-LAN Configuration + Saved Computers + Integrated Services | 同 | 已还原 |
| 表单控件 | 表单字段 + `Test Connection` + `WOL Ready` | 7 个输入框 + 两个按钮 + `WOL Ready` | 部分偏差：规格里的 `Port 步进器` 做成了普通输入框；留到 Phase 2（设备管理真正落地时换成 stepper） |
| Saved Computers | 4 台设备 + MAC + Default 徽标 + `Add Device` | 同（数据来自 `MockData.devices`） | 已还原 |
| Integrated Services | Weather / Calendar / Task 三项 + `Show on home screen` 开关 | 同 | 已还原 |
| 左导航 | 规格 E 列 6 项 + 简报要求的 `Display & Behavior` | 7 项，另有列表之外的 `← Home` 返回入口 | 已还原（刻意增加，见路标决策表） |
| Personalization 控制条 | 四段：Wallpaper（7 缩略图）/ Theme & Accent（6 色点）/ Widget Style（3 种）/ Appearance（滑杆 + 布局） | 四段并排；壁纸**只有 2 张**（有素材的两张） | 需用户裁决：其余 5 张无素材（§6） |
| Live Preview | 中部 `Live Preview — Your Home Screen` | 同，且改动实时作用于全应用（强调色/壁纸/卡片风格） | 超出规格但方向一致：只改预览不改真实界面等于没有设置项 |

## 5. 取色校准（Step 3）

从概念图真实采样（PIL 区域均值），与当前令牌对照：

| 采样区域 | 概念图实测 | 当前令牌 | 差值 | 结论 |
| --- | --- | --- | --- | --- |
| 深色底（Rail 底部条 / PC 卡） | `#091017` / `#0A1320` | `DarkSurface.background = #0B0F17` | ≤ 4 / 1 / 9 | 无需调整 |
| 卡片填充（Tasks 卡） | `#1F242E` | `DarkSurface.card = #151B26`（叠加 Glass α=0.35 后更亮） | ≤ 10 | 无需调整 |
| 卡片描边 | 采样落在阴影上，未取得干净边线像素 | `DarkSurface.outline = #33FFFFFF` | 无法判定 | 保持 20% 白，留待人工复核 |

**结论：本轮取色未发现需要改动的令牌**，`ThemeAccentTest` 的对比度断言（全部 ≥ 4.5:1）保持全绿，因此无需记录豁免。该结论来自上表实测，不是"看起来差不多"。

## 6. 待办与需人工确认的项

**T1（立即修，属 Phase 1 范围）**：Monitor 身份卡缺状态行（绿点 + `My PC` + `Online` + `Last seen 1 min ago`）。

**T2（视觉打磨，可在 Phase 1 之后）**：温度 / 风扇 / 最近活动的彩色圆点与进度条。

**需用户裁决（阻塞"完整还原"的硬缺口）：**

1. **壁纸素材**：规格 F 要 7 张缩略图，只有 2 张有母版。补素材 / 只出 2 张 / 用纯色渐变占位，三条路的视觉结果差别很大。
2. **图标方案**：概念图大量使用图标（快捷动作、指标卡、温度行、勾选框）。Phase 1 全局约束"不引入图标库"，目前用 `⏻ ⚙ ☁ ▦ ● ○` 字形占位。要还原到 80–90% 需要裁决：引入图标库 / 自绘矢量路径 / 继续字形占位。

**人工复核建议**：§1 与 §2 是我对着概念图逐项比对得出的；§3–§4 是对着规格表比对。请用 `docs/plans/screenshots/` 与 `imgs/concept/` 并排过一遍，把不认可的项标出来。

---

## 7. 2026-09-20 复核补记

### 7.1 起因：证据本身不可信

两处硬伤，都会让上面的结论失去依据：

1. **帧尺寸不是文档写的那一个**：`docs/plans/screenshots/` 里五个屏幕都是 **1024×720**，不是 §0 写的 1280×720。原因是 `AppScreenshotTest` 用 `Modifier.size(1280.dp, 720.dp)` 套在默认的 1024×768 测试窗口里，`size()` 会遵守入参约束 → 宽度被压回 1024。后果是整个 §1/§2 是在 737dp 主区（3 列区间）下比对概念图的。
2. **`android-standby.png` 是无效证据**：它与 `android-monitor.png` **逐字节相同**（MD5 都是 `e76205ed…`），说明抓图时 App 还停在 Monitor 形态。已删除该文件。

修正：`AppScreenshotTest` 改用 `runDesktopComposeUiTest(width, height)` 显式指定窗口（1280×720，窄屏 800×720），并**断言产出的 PNG 尺寸等于声明的渲染条件**——文档和图再对不上会直接让测试失败，而不是靠人眼。五个屏幕全部按 1280×720 重出。

### 7.2 本轮修掉的偏差

| 编号 | 位置 | 09-19 状态 | 依据 | 处理 |
| --- | --- | --- | --- | --- |
| T1 | Monitor 身份卡缺状态行 | 已由 §6 T1 记录 | 规格 C1 | ✅ 本轮提交（绿点 + `My PC` + `Online` + `Last seen 1 min ago`，`HomeSurfaceTest` 守住） |
| T3 | 指标卡型号小字用带厂商前缀的全名 | 未发现 | 规格 C2 逐字给的是 `Ryzen 7 7700X` / `RTX 4070 Ti`；概念图卡片也是短名，全名只在身份卡 | ✅ 新增 `HardwareIdentity.cpuShortName` / `gpuShortName`，卡片读短名、身份卡读全名 |
| T4 | 主区 921dp 落到 3 列，还原不出概念图的指标行 | 未发现（§2 只写了"布局顺序 同"） | 概念图本身就是 1280×720dp 的墙面屏，它在这一宽度下排的是 **5 列**指标卡；旧阈值 1000dp 是 Task 14 的估算，把目标屏排除在概念栅格之外 | ✅ 阈值 1000→900dp；同时发现 Dashboard 不能共用该列数，拆出 `Breakpoints.dashboardColumns`（概念图 B 最宽一行是 3 张卡） |
| T5 | Network / Uptime 大号读数折行 | 未发现（3 列时卡片更宽，不折行） | 概念图两行各一行排完；按概念图字形高换算约 16–21sp，而 `metricValue` 是 32sp | ✅ 新增 `AppTypography.metricReadout = 18sp` |
| T6 | StandBy 日期用日历卡的短形态 `Tue, Apr 22` | 记成"已还原" | 规格 D 逐字写的是 `Tuesday, April 22` | ✅ 新增 `MockData.standbyDateLabel`，`HomeSurface` 只在 StandBy 形态换成它 |
| T7 | StandBy 的 `PM` 用 14sp，在 104sp 数字旁缩成小灰点 | 记成"已还原" | 概念图 `image-gen-5` 里 `PM` 与数字同基线、高约数字的 1/3 | ✅ 改用 `metricValue`（32sp） |
| T8 | Glass 卡面过透，StandBy 的 PC 浮层卡几乎看不出是张卡 | 未发现（§5 取色只采了深色背景上的卡） | 概念图卡面填充实测 `(11,19,27)` / std≈9，我们的 Glass(α=0.35) 在亮壁上 std 到 15 | ✅ Glass α 0.35→0.6（实测 std 回到 9，与概念图一致） |

### 7.3 按 1280×720 重核后，本文旧结论的变动

| 旧结论 | 现在是否成立 |
| --- | --- |
| §1 Dashboard 三行 7 块、天/历/任务 3 列 | ✅ 成立（Dashboard 的 3 列是概念图本身的形态，已用 `dashboardColumns` 固定，不随 5 列断点漂移） |
| §1 逐时天气是单行文本、无图标 | ✅ 仍成立（1280dp 下列间有了留白，但仍是 `10PM 17° 1AM 16°…` 一行，没有列/图标） |
| §1 PC 摘要四项纵向堆叠 | ✅ 仍成立（概念图是横向四列） |
| §2 Monitor 布局顺序"已还原" | ⚠️ **09-19 的说法过宽**：当时是三列堆叠，行数并不等于概念图。改成 5 列后（T4）才是概念图的 5 列指标行 + 4 列底行 |
| §2 最近活动应用名被省略号截断 | ✅ 仍成立：底行四张卡等宽，概念图的 Recent Activity 卡明显比 Network/Uptime 宽（实测概念图 295px vs 205px），我们要么给不等权重、要么接受截断 |
| §3 StandBy 时钟"已还原" | ⚠️ 日期与 `PM` 两处不成立（T6/T7，已修） |
| §3 StandBy PC 浮层卡"已还原" | ⚠️ 仍是偏差：概念图卡面写 `POWER CONTROL` + 右上 `>` + `Power On` / `WAKE YOUR PC`；我们 ONLINE 状态下渲染的是 `PC Online` / `AGENT CONNECTED`（动作 vs 状态的语义差异），且没有 `POWER CONTROL` 与 `>` |
| §4 Personalization "四段并排" | ✅ 成立 |
| §5 取色"无需调整" | ⚠️ 部分不成立：卡面不透明度需要上调（T8）。深色/强调色令牌本身仍未发现需要改的 |

### 7.4 仍未解决 / 需要裁决

1. **壁纸素材**（原 §6-1，未变）：规格 F 要 7 张，只有 2 张有母版。
2. **图标方案**（原 §6-2，未变）：概念图大量用图标；Phase 1 约束"不引入图标库"，现在是 `⏻ ⚙ ▫ ●` 一类字形占位。
3. **断点阈值 900dp 与计划 Global Constraints 的 1000dp 冲突**：我按概念图实测改成了 900dp（否则目标屏永远落在 3 列）。计划正文那句是估算，改的是令牌不是架构（72/28 未动）。**若你不认可，改回 1000dp 是一行的事**，代价是墙面屏上还原不出概念图的 5 列指标行。
4. **两个"概念图没给数值、我按实测定的"值**：Glass α=0.6、StandBy `PM`=32sp。概念图只有像素，没有设计稿数值，这两处是换算结果，属于可推翻的判断。
5. ~~**Android StandBy 截图仍缺**~~ → **已结**（2026-09-20 二轮）。两条记录都要更正：
   - "`/dev/kvm` 不存在、恢复需 `sudo`" 是**错的**——它量自 Codex 沙箱（bwrap 私有 `/dev` 看不到宿主机设备）。
     宿主 `/dev/kvm` 一直在、`kvm` 模块已加载；真正卡住的是"启动者会话没有 kvm 组"与"09-19 09:40 起的旧无头
     实例锁住 AVD"。证据见 [version-matrix.md](version-matrix.md) §6。
   - 更关键的是**根因不是环境，而是功能缺陷**：App 的滑动手势只映射 Dashboard↔Monitor，
     `HomeModeController` 的三态逻辑只被测试调用、从没接进应用，**StandBy 在真机上根本不可达**——这才是
     09-19 那张 `android-standby.png` 与 `android-monitor.png` 逐字节相同的真正原因（抓图时 App 还停在 Monitor，
     根本没有第三个形态可切）。
   > 修复：`homeMode` 改由 `HomeModeController` 持有，滑动按 `HomeMode.forward()/backward()` 走，提交 `1f5ab94`；
   > 守卫是 `AppUiTest`「两次左滑走到 StandBy、两次右滑退回」与 `HomeSurfaceTest` 的单步断言。
   > 三张 Android 帧（2560×1600，含首次真正拍到的 StandBy）已按当前代码重抓，见
   > [screenshots/README.md](screenshots/README.md) Android 侧。

### 7.5 本轮验证命令与结果

```bash
JAVA_HOME=<jdk-25> ./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
# BUILD SUCCESSFUL —— desktopTest 163 / testDebugUnitTest 94，0 failures
```

截图由同一次 `desktopTest` 重新产出（`AppScreenshotTest` 会断言帧尺寸），并复制到 `docs/plans/screenshots/`（见该目录 README）。

---

## 8. 2026-09-20 补记（二）：UI 素材接入

**这一节解掉了 §6 / §7.4 里"待用户提供"的两条阻塞项。** 用户交付了：

- `imgs/ui/`：11 个天气图标、1 张 PC 默认封面、1 张电源按钮底图（均为带真实 alpha 的 PNG）；
- `imgs/wallpaper/`：补齐到 7 张母版（规格 F 要几张就有几张）。

处理原则由用户给定：**没有的尽量用代码实现，做不到的再回报要图**。执行结果与逐项清单见
[2026-09-20-ui-asset-integration.md](2026-09-20-ui-asset-integration.md)，要点：

| §6 / §7.4 的待办 | 现在 |
| --- | --- |
| 壁纸素材（规格 F 要 7 张，只有 2 张） | ✅ 7 张母版全部入库进包，Appearance 的缩略图从母版派生 |
| 图标方案（引入图标库 / 自绘矢量 / 继续字形占位） | ✅ 拐点是**第三条路 + 素材**：天气 / PC 封面 / 电源用交付的 PNG，其余界面图标由 `ui/icons/AppIcons.kt` 代码绘制（不引库、不内置第三方商标） |
| T2 温度 / 风扇 / 最近活动的圆点与进度条 | ✅ 已按概念图补齐，并加了不等权重让 Recent Activity 不再截断 |
| 逐时天气无图标、温度区间错位、PC 摘要四项纵向堆叠 | ✅ 三项都改了（见上表链接的 §4） |

**第二批复核（2026-09-20）** 按用户指示把上面三点全部收口，并补了手机 / 折叠屏的响应式适配：

1. `default wallpaper` 改为 `Dusk Lake`（`aurora` 降为备选）；
2. StandBy 浮层卡按概念图 D 逐项重建（`POWER CONTROL` + `>`、`Power On`/`WAKE YOUR PC`、
   `Night Mode`/`Auto-Dim` 图标瓦片、底部状态条）；
3. 电源按钮改为概念图的发光环（弃用旋钮底图，纯代码绘制）。

细节与证据见 [2026-09-20-responsive-and-concept-alignment.md](2026-09-20-responsive-and-concept-alignment.md)。

验证：`testDebugUnitTest 101 / desktopTest 184`，0 failures；`docs/plans/screenshots/` 下的
帧已按本轮代码重出（含 20:9 / 21.1:9 / 4:3.55 三种手机与折叠屏比例）。
