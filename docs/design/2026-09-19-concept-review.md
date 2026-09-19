# 概念图审阅报告（imgs/concept，10 张）

**日期：** 2026-09-19
**审阅对象：** `imgs/concept/` 下全部 10 张 PNG
**用途：** 作为 UI 还原的权威依据，并据此修订产品规范与 Phase 1 实施计划

---

## 1. 图片清单与去重

| 文件 | 屏幕 | 关系 |
| --- | --- | --- |
| `0499d5bc-6980-4309-846b-0b6ae3d54794.png` | Home Dashboard | 基准 |
| `image-gen-1(1).png` | Home Dashboard | 与上一张近乎相同（仅地点写作 `Riverside, CA`） |
| `cb576a75-5889-44cf-b38a-15d2565d69dd.png` | PC Monitor | 基准 |
| `image-gen-2(1).png` | PC Monitor | 变体：副标题 `YOUR SYSTEM AT A GLANCE`、`Last updated` |
| `image-gen-4.png` | PC Monitor | 变体：Open Browser 用显示器图标、Sleep 副标 `S4/S0 MODE` |
| `4f74989a-2b43-4edd-89b1-718e7982a103.png` | Settings（卡片式，无左导航） | 与下一张布局冲突 |
| `image-gen-3(1).png` | Device Setup（左导航式） | 与上一张布局冲突 |
| `402af674-18e3-4495-87a2-81a3722546a0.png` | Wallpaper & Personalization | 基准（选中 Mountains） |
| `image-gen-4(1).png` | Wallpaper & Personalization | 变体（选中 Dusk Lake） |
| `image-gen-5.png` | **StandBy / 时钟模式** | **简报中完全没有的第三块主页形态** |

去重后实际需要实现 **5 个屏幕**：Home Dashboard、PC Monitor、StandBy、Settings（含 Device Setup）、Wallpaper & Personalization。

---

## 2. 全局视觉语言

| 元素 | 规格 |
| --- | --- |
| 背景 | 全屏壁纸图（山湖夜景），上面叠一层深色 scrim 保证卡片可读 |
| 卡片 | 深色半透明玻璃卡：低透明度填充 + 1dp 细边框 + 大圆角（约 20dp），卡内留白约 16dp |
| 高亮色 | 琥珀/暖橙用于时间与问候语中的强调词（`Evening`、`41`） |
| 状态色 | 绿色 = 在线/就绪；蓝-青、绿、紫、蓝分别用于 CPU / GPU / RAM / Storage 环形进度 |
| 排版 | 大号细体数字（时钟约 120sp，问候语约 56sp）；全大写字母间距标签（`POWER CONTROL`、`SNAP MODE`、`A MORE FOCUSED TOMORROW`） |
| 常驻装饰文案 | 主区右上 `SAME ROOM / DIFFERENT / PERSPECTIVE`；主区底部 `A MORE FOCUSED TOMORROW`（带细分割线）；引用卡 `Better Tools A Calmer Mind — SAME PROGRESS A BRIGHTER TOMORROW` |
| 状态栏 | 图里的 `9:41` 与信号/电量是**手机系统状态栏**，不是 App 绘制内容（App 为沉浸式全屏，见规范 §9） |
| `v1.0` 角标 | 概念图版本水印，**不实现** |

> 精确色值需要一次取色校准。本报告只锁定颜色家族与用途，实现时以取色器结果写进 `core/theme/Tokens.kt`，不要在 Composable 里散落色值。

---

## 3. Home Dashboard

布局：主区约 72%，三行卡片；右侧 28% 为 Power Rail。

| 行 | 左 | 中 | 右 |
| --- | --- | --- | --- |
| 1 | 问候语 `Good Evening`（`Evening` 用强调色）+ 副标题 `A CALMER DESKTOP. A BRIGHTER YOU.` | — | 装饰文案 `SAME ROOM / DIFFERENT / PERSPECTIVE` |
| 2 | Weather 卡 | Calendar 卡 | My Tasks 卡 |
| 3 | My PC 摘要卡（可点进 Monitor） | — | 引用装饰卡 |

**Weather 卡：** 月亮+云图标、`18°`、`Partly Cloudy`、右上 `↑22° ↓14°`、`📍Riverside, CA`、分割线、四列逐时（`10PM 17°` / `1AM 16°` / `4AM 15°` / `7AM 16°`，每列带小图标）。

**Calendar 卡：** 标题 `Tue, Apr 22` + 副标 `This Week` + 右上 `+`；周条 `S M T W T F S` 与日期 `20 21 [22] 23 24 25 26`，选中日为强调色实心圆；事件列表带彩色圆点：`10:00 Team sync`（青）、`1:00 Lunch break`（琥珀）、`4:00 Plan next week`（红）。

**My Tasks 卡：** 标题 `My Tasks` + 右侧 `3 of 5`；五行任务，已完成项为实心勾选 + 文字删除线，未完成项为空心圈；底部 `+ Add a task`。

**My PC 摘要卡：** 绿点 + `My PC` + `Online`（绿）+ 右上 `Last seen 1 min ago`；左侧缩略图；四个指标 `CPU 12%` / `Temp 42°C` / `RAM 38%` / `Network ↓12.4 Mbps ↑3.1 Mbps`，前三个下方各有一条彩色进度细条；右侧 `>` 进入 Monitor。

**引用装饰卡：** 衬线体两行 `Better Tools / A Calmer Mind` + 分割线 + `SAME PROGRESS A BRIGHTER TOMORROW`。

---

## 4. PC Monitor

| 行 | 内容 |
| --- | --- |
| 1 | 左：My PC 身份卡（绿点 / `My PC` / `Online` / `Last updated 1 min ago`、缩略图、`DESKTOP-ALPHA`、`Windows 11 Pro`、`AMD Ryzen 7 7700X`、`NVIDIA GeForce RTX 4070 Ti`、`>`）<br>右：Quick Actions 卡（右上 `+`，四个动作块：Open Browser / Open Discord / Launch Steam / Open Spotify） |
| 2 | CPU / GPU / RAM / Storage 四张指标卡 + System Temps 卡 |
| 3 | Network / Uptime / Recent Activity + 引用装饰卡 |

**指标卡规格（4 张一致）：** 顶部图标 + 名称 + 型号小字（`Ryzen 7 7700X` / `RTX 4070 Ti` / `32 GB DDR5` / `2 TB NVMe SSD`）；中部**环形进度**（中心显示百分数：28% / 62% / 38% / 54%）；下方 sparkline 曲线；底部两行数值：`4.9 GHz` + `8 cores 16 threads`、`67 °C` + `8.1 / 12 GB VRAM`、`12.1 / 32 GB`、`1.1 / 2.0 TB` + `554 GB free`。

**System Temps 卡（含两段）：** 温度段每行 = 彩色点 + 名称 + 进度条 + 数值：CPU 68°C、GPU 67°C、Motherboard 42°C、SSD 38°C；风扇段 = CPU Fan 1,240 RPM、GPU Fan 1,560 RPM、Case Fans 820 RPM。

**Network 卡：** `↓ 124.3 Mbps`、`↑ 31.7 Mbps` + 曲线。
**Uptime 卡：** `3d 6h 24m` + `Since Apr 18, 2025`。
**Recent Activity 卡：** 彩色点 + 应用名 + 相对时间：Microsoft Edge 5 min ago、Steam 12 min ago、Visual Studio Code 28 min ago、Spotify 1 hr ago。

> 概念图按宽屏渲染。真机（如 2400×1080、密度 2.6）下主区约 640dp，排 5 列会过窄，因此实现必须做成**断点自适应**：可用宽度 ≥ 1000dp 用概念图列数；600–1000dp 用 3 列；< 600dp 用 2 列并允许纵向滚动。

---

## 5. StandBy 模式（简报未覆盖）

无右侧 Power Rail，元素直接浮在壁纸上：

| 区域 | 内容 |
| --- | --- |
| 左上 | `A MORE FOCUSED TOMORROW` + 短横线 |
| 右上 | `SAME ROOM / DIFFERENT / PERSPECTIVE` |
| 主区 | 超大时钟 `9:41`（`41` 用强调色）+ `PM`，下一行 `Tuesday, April 22` |
| 中部左 | Weather 卡：`18°`、`Partly Cloudy`、`↑22° ↓14°`、`Riverside`、`Clearer skies later tonight.` |
| 中部右 | Next Event 卡：日历图标 + `Next Event` + `In 1 hr 19 min`、`Team sync`、`11:00 PM – 12:00 AM`、`Microsoft Teams` |
| 右侧浮层 | 玻璃卡 `My PC` + `POWER CONTROL` + `>`；电源环；`Power On` + `WAKE YOUR PC`；两个按钮：`Night Mode / ON`、`Auto-Dim / ACTIVE` |
| 底部 | 通栏条：显示器图标 + 绿点 + `My PC` + `Online` + `Last seen 1 min ago` + `>` |

**这是第三个主页形态**：Power Rail 在此收窄成浮层卡，并且用 `Night Mode` / `Auto-Dim` 两个开关替换 Sleep / Shut Down / Restart。

---

## 6. Settings 与 Device Setup

概念图给了**两套互不兼容的布局**：

**方案 A（`image-gen-3(1)`，Device Setup）：** 左导航 + 卡片主区 + 保留完整 Power Rail。

| 区域 | 内容 |
| --- | --- |
| 左导航 | 眉标 `MY PC COMPANION`；条目：Device Setup（选中）、Integrations、Notifications、Appearance、Backup & Sync、About；底部 `Reset to Default` |
| 主区上排 | `Wake-on-LAN Configuration` 卡（PC Name / MAC Address / Broadcast IP / Port 带步进器；`Test Connection` 按钮；绿色状态块 `WOL Ready` + `Last tested 2 min ago`）+ `Saved Computers` 卡（4 台设备，每行显示器图标 + 名称 + MAC + 选中圆点，默认设备带绿色 `Default` 徽标；底部 `+ Add Device`） |
| 主区下排 | `Integrated Services` 卡组：Weather Integration（Weather Provider 下拉、Location 搜索框、Show on home screen 开关）、Calendar Integration（Calendar Source 下拉、Show next events 下拉、开关）、To-Do Integration（Task Source 下拉、Show in widget 下拉、开关） |
| 右侧 | 完整 Power Rail + 底部引用装饰卡 |

**方案 B（`4f74989a`，Settings）：** 无左导航，主区两列卡片，右侧栏被设置分组占用（Display & Behavior / Appearance / Notifications），该屏不显示电源控制。

| 分组 | 条目 |
| --- | --- |
| Display & Behavior | Keep screen on（Prevent screen from sleeping）、Auto-dim when idle（Dim screen after 1 minute）、Lock orientation（Keep landscape mode）、Show PC status widgets（Show system info on home screen） |
| Appearance | Wallpaper 行（缩略图 + `>`）、Theme 下拉（`Dark (Default)`） |
| Notifications | Show notifications、Notify on PC online、Notify on failed wake |

**处理建议：** 以方案 A 为 V1 权威（与简报 §10 的左导航一致、且保留 Power Rail 常驻的全局原则）；方案 B 的卡片分组作为 Device Setup 之外的「Settings 概览」内容，可作为后续迭代。**此点需用户确认。**

**字段差异：** 概念图的 Wake-on-LAN 表单只有 4 个字段（PC Name / MAC / Broadcast IP / Port），而简报 §11 要求 6 个（含 IP Address、Agent Port）。处理：V1 先按概念图渲染 4 个字段，Agent 相关字段（Agent Host / Agent Port / Token）在 Phase 4 接入 Agent 时以「Advanced / Agent」区补上；数据模型从第一天就保留这些字段。

---

## 7. Wallpaper & Personalization

| 区域 | 内容 |
| --- | --- |
| 左导航 | 眉标 `MY PC COMPANION`；标题 `Wallpaper & Personalization`（`Personalization` 用强调色）；`Make it yours` + `Beautiful backgrounds. A calmer workspace.`；条目：Wallpaper（选中）、Themes & Colors、Widgets、Layout、Appearance；底部 `Reset to Default` |
| 中部 | `Live Preview — Your Home Screen` 面板，内嵌真实 Dashboard 渲染 |
| 右侧 | Power Rail |
| 底部控制条 | **Wallpaper**：缩略图 `Dusk Lake`（选中，带对勾角标）、Mountains、Forest、City Night、Cozy Room、Minimal、Abstract，末尾 `+` More；**Theme & Accent**：6 个色点，首个标注 `Aurora Blue`；**Widget Style**：Glass / Solid / Minimal 三种小卡预览，Glass 选中；**Appearance**：Card Transparency 滑杆 `70%`、Font Scale 滑杆 `100%`、Layout Preset 三选一 Default / Compact / Minimal（Default 选中） |

---

## 8. 与产品简报的冲突及处理

| # | 冲突 | 处理 |
| --- | --- | --- |
| C1 | Rail 同时显示 `Ready to wake`、`Power On`、`Connected via Wake-on-LAN` | 以规范 §3 状态机为准：关机态显示 `Wake-on-LAN Ready`，在线态显示 `Agent connected over LAN`；该连接条是「传输通道说明」，随状态切换文案 |
| C2 | 底部 `Connected via Wake-on-LAN` 与简报 §5 的术语纠正直接矛盾 | 同上，采用简报的术语纠正 |
| C3 | Settings 两套布局 | 见 §6，建议方案 A 为 V1 权威 |
| C4 | 概念图包含 StandBy 时钟屏，简报只有两种主页模式 | 主页升级为三种模式：Dashboard / Monitor / StandBy；StandBy 既是可滑动进入的模式，也是 Idle 长时间无人操作时的展示形态 |
| C5 | Quick Actions 出现在 Monitor，但简报 §46/§48 把它放到 V1.1 | Phase 1 只做静态 UI（Mock），真实动作注册表与白名单留在 Phase 5.5 |
| C6 | Integrated Services 显示 Todoist / Google Calendar / OpenWeatherMap 三个外部源，简报 §24/§25 要求先用 Android Calendar Provider 与本地 Todo | UI 上保留三个来源选择器；V1 默认值改为 OpenWeatherMap / Android Calendar / Local，Todoist 标记为 V1.1 |
| C7 | 概念图温度含 Motherboard、SSD，活动列表含 Recent Activity（简报列为 P1/P2） | 指标卡 UI 一次做齐；数据字段按 P1/P2 分阶段接入，缺失时显示 `—` 而不是隐藏卡片 |
| C8 | 壁纸列表比简报多一张 `Dusk Lake`（且为默认选中） | 内置壁纸扩为 7 张：Dusk Lake（默认）、Mountains、Forest、City Night、Cozy Room、Minimal、Abstract |
| C9 | 概念图的 `9:41`、信号、电量是手机系统状态栏 | 不由 App 绘制；App 侧保持沉浸式全屏 |
| C10 | 概念图的 `v1.0` 角标 | 不实现 |
| C11 | 概念图 Clock 卡在 Calendar 头部显示 `Tue, Apr 22`，StandBy 显示 `Tuesday, April 22` | 两种日期格式都保留为格式化选项（短格式用于卡片，长格式用于 StandBy） |

---

## 9. 由此产生的计划改动

1. **规范**：新增 StandBy 模式定义、Power Rail 精确构成、三种主页模式的切换关系、外观项清单（7 张壁纸 / 6 个强调色 / 3 种 Widget 风格 / 3 种布局预设 / 透明度 / 字号）、装饰文案层、以及 §8 的冲突裁决表。
2. **Phase 1 计划**：把「Power Rail」任务升级为带环形电源按钮、状态徽标、双行次级按钮、连接条的完整还原；新增 StandBy 模式任务；Dashboard 任务补齐问候语双色、周条、任务计数、指标细条、引用卡与品牌条；Monitor 任务补齐 Quick Actions、环形进度、System Temps / Fans 合并卡、Recent Activity；Personalization 任务补齐底部控制条与 `Dusk Lake`；Settings 任务按方案 A 改为左导航结构。
3. **Phase 1 计划**新增「响应式断点」约束，避免在窄屏上强排 5 列。
4. **Roadmap**：Phase 1 交付物描述更新为五个屏幕；StandBy 的自动进入（夜间/Idle）挂到 Phase 8。
5. **Phase 0**：`gradlew` 在 git 中是可执行位缺失（mode 100644），已修正为 100755 —— 这属于仓库既有缺陷，需一并提交。

---

## 10. 待用户确认

| # | 事项 | 建议 |
| --- | --- | --- |
| V1 | Settings 采用方案 A（左导航 + 保留 Power Rail）还是方案 B（卡片分组 + 右侧栏改为设置分组） | 采用方案 A |
| V2 | StandBy 是否作为第三个主页模式（并且是 Idle 展示形态） | 是 |
| V3 | `Dusk Lake` 设为默认壁纸 | 是 |
| V4 | 概念图里的装饰文案（`SAME ROOM DIFFERENT PERSPECTIVE`、`A MORE FOCUSED TOMORROW`、引用卡）是否保留为可关闭的装饰层 | 保留，并放进 Appearance 的 Widget 显隐开关 |
