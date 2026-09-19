参考你这组概念图以后，原来的计划需要做一个明显调整：这个项目已经不应该再按“WOL App + 一些桌面小组件”来规划，而应该定义成一个完整的 **Desktop Companion / 桌面伴侣系统**。

核心产品结构应当变成：

> **手机负责展示、交互和控制；PC Agent 负责电脑状态采集和执行控制；WOL 只负责电脑处于关机状态时的启动。**

下面这版可以作为后续开发的正式产品方案和项目 Backlog 基础。

---

# 1. 产品定位

## 1.1 产品目标

把闲置 Android 手机或普通手机变成一块长期横放在桌面上的：

**PC 控制器 + 系统监视器 + StandBy 信息屏 + 桌搭装饰屏。**

应用长期处于横屏、常亮状态，用户不需要解锁手机、不需要进入多个页面，就可以完成主要操作。

核心目标有四个：

| 目标    | 说明                       |
| ----- | ------------------------ |
| PC 控制 | 开机、睡眠、关机、重启、锁屏           |
| PC 监控 | CPU、GPU、RAM、磁盘、温度、网络、风扇等 |
| 信息展示  | 时间、天气、日历、Todo            |
| 桌搭展示  | 壁纸、主题、玻璃卡片、布局、夜间模式       |

---

# 2. 根据概念图调整后的产品结构

原计划只有一个 Dashboard。

现在应该升级成 **三个主要工作空间**：

```text
Home Dashboard
       │
       ├── StandBy Dashboard
       │
       └── PC Monitor
       │
       ▼
Settings Workspace
       │
       ├── Device Setup
       ├── Integrations
       ├── Display & Behavior
       ├── Appearance
       ├── Notifications
       └── About
```

与此同时：

```text
右侧 PC Power Rail
```

应该成为整个 App 的核心固定组件。

也就是说多数页面采用：

```text
┌──────────────────────────────────┬──────────────┐
│                                  │              │
│                                  │    My PC     │
│       Main Content               │              │
│          ~72%                    │    Power     │
│                                  │    Panel     │
│                                  │    ~28%      │
│                                  │              │
└──────────────────────────────────┴──────────────┘
```

这点和你概念图的方向是对的，建议正式确定。

---

# 3. Home Dashboard

这是应用最重要的页面。

参考你现在的概念图，建议最终布局：

```text
┌──────────────────────────────────────┬──────────────┐
│ Good Evening                         │ My PC     ⚙  │
│                                      │              │
│ ┌──────────┐ ┌──────────┐ ┌────────┐│ ● Status    │
│ │ Weather  │ │ Calendar │ │ Tasks  ││              │
│ │          │ │          │ │        ││     ⏻        │
│ └──────────┘ └──────────┘ └────────┘│              │
│                                      │              │
│ ┌──────────────────┐ ┌─────────────┐ │ Sleep        │
│ │ My PC            │ │ Decorative  │ │ Shutdown     │
│ │ CPU GPU RAM NET  │ │ Widget      │ │ Restart      │
│ └──────────────────┘ └─────────────┘ │              │
│                                      │ LAN / WOL    │
└──────────────────────────────────────┴──────────────┘
```

### 左侧区域

默认展示：

* Greeting
* 当前时间
* Weather
* Calendar
* Todo
* PC Status Summary
* 可选装饰 Widget

### 右侧区域

永远负责 PC。

包括：

```text
PC Name

PC State

Main Power Control

Sleep

Shutdown

Restart

Network / Agent State

Settings
```

---

# 4. 一个需要修正的概念图逻辑

你现在部分概念图存在一个产品逻辑冲突：

```text
My PC Online

但是：

Power On
Ready to wake
Connected via Wake-on-LAN
```

这三个状态不能同时成立。

正式实现时一定要改成真正的状态机。

---

# 5. PC 状态机

建议完整定义：

```text
UNCONFIGURED

OFFLINE

WOL_READY

WAKING

ONLINE

AGENT_UNAVAILABLE

SLEEPING

SHUTTING_DOWN

RESTARTING

ERROR
```

具体表现：

| 状态                | 主按钮               | 辅助按钮                       |
| ----------------- | ----------------- | -------------------------- |
| 未配置               | Setup PC          | 禁用                         |
| Offline           | Wake PC           | 禁用                         |
| WOL Ready         | Wake PC           | 禁用                         |
| Waking            | Waking…           | 禁用                         |
| Online            | PC Online         | Sleep / Shutdown / Restart |
| Agent unavailable | Agent unavailable | Wake disabled              |
| Sleeping          | Sleeping…         | 禁用                         |
| Restarting        | Restarting…       | 禁用                         |
| Shutting down     | Shutting down…    | 禁用                         |

### Offline

```text
○ My PC

OFFLINE

      ⏻
   Wake PC

Wake-on-LAN Ready
```

### Waking

```text
◌ My PC

WAKING

     ◌
Waking PC...

Waiting for Agent
```

### Online

```text
● My PC

ONLINE

      ✓
  PC Online

Sleep
Shutdown
Restart

Agent connected over LAN
```

这里还应该修正一个术语：

> **“Connected via Wake-on-LAN” 不准确。**

WOL 并不是保持连接的协议。

正确显示应该是：

```text
Wake-on-LAN Ready
```

或者在线的时候：

```text
Agent connected over LAN
```

---

# 6. Home Dashboard 两种模式

你的概念图实际上已经隐含了两套主页。

建议正式把它们做成两个模式。

## Dashboard Mode

偏 StandBy：

```text
Weather
Calendar
Todo
PC Summary
Clock
Wallpaper
```

适合平时长期放桌面上。

---

## Monitor Mode

偏系统监控：

```text
PC Hardware
Quick Actions
CPU
GPU
RAM
Storage
Temperatures
Fans
Network
Uptime
Recent Activity
```

用户可以：

```text
点击 My PC Widget
```

进入 Monitor Mode。

或者：

```text
左右滑动
```

切换：

```text
Dashboard
      ↔
PC Monitor
```

右侧 Power Rail 始终保持不动。

这是我认为最适合你这些 UI 图的交互方式。

---

# 7. PC Monitor 页面

参考你的后几张图，可以正式定义为：

```text
┌─────────────────────────────────────┐
│ My PC             Quick Actions     │
│                                     │
│ Desktop-Alpha      Browser Discord  │
│ Ryzen 7 7700X      Steam Spotify    │
│ RTX 4070 Ti                        │
├────────┬────────┬────────┬─────────┤
│ CPU    │ GPU    │ RAM    │ Storage │
│ 28%    │ 62%    │ 38%    │ 54%     │
│ graph  │ graph  │ graph  │ graph   │
├──────────────┬──────────────────────┤
│ Network      │ Temps & Fans        │
│              │                     │
├──────────────┼──────────────────────┤
│ Uptime       │ Recent Activity     │
└──────────────┴──────────────────────┘
```

---

# 8. PC Monitor 第一版数据

V1 建议实现：

| 数据                      | V1 |
| ----------------------- | -- |
| CPU Usage               | ✅  |
| CPU Temperature         | ✅  |
| CPU Frequency           | ✅  |
| GPU Usage               | ✅  |
| GPU Temperature         | ✅  |
| GPU VRAM                | ✅  |
| RAM Usage               | ✅  |
| Storage Usage           | ✅  |
| Download Speed          | ✅  |
| Upload Speed            | ✅  |
| Uptime                  | ✅  |
| CPU Fan                 | ✅  |
| GPU Fan                 | ✅  |
| SSD Temperature         | P1 |
| Motherboard Temperature | P1 |
| Recent Applications     | P2 |
| Historical charts       | P2 |

不要第一版直接保存几个星期的性能历史。

第一版图表显示：

```text
最近 60 秒
```

就够了。

数据保存在内存 Ring Buffer。

---

# 9. Quick Actions

概念图里的：

```text
Open Browser

Discord

Steam

Spotify
```

值得保留。

但必须改变实现逻辑。

手机不能直接“打开 PC 程序”。

需要：

```text
Android
   ↓
POST /actions/{id}
   ↓
PC Agent
   ↓
启动应用
```

比如：

```http
POST /api/v1/actions/steam
```

Agent 预先配置：

```text
steam → Steam.exe

discord → Discord.exe

spotify → Spotify.exe
```

### 非常重要

不要提供：

```text
POST /execute?command=xxxxx
```

这种任意 Shell Command API。

否则安全风险很大。

Quick Actions 必须是：

```text
ID → Agent Allowlist
```

---

# 10. Settings 页面需要重新设计

概念图中的 Settings 更适合升级成一个完整 Settings Workspace。

建议左侧 Navigation：

```text
Device Setup

Integrations

Display & Behavior

Notifications

Appearance

Backup & Sync

About

─────────────

Reset to Default
```

右侧仍保留 Power Rail。

---

# 11. Device Setup

这是第一次启动最重要的页面。

建议字段：

```text
PC Name

MAC Address

IP Address

Broadcast Address

WOL Port

Agent Port
```

其中：

```text
MAC
```

必须。

例如：

```text
AA:BB:CC:DD:EE:FF
```

Broadcast：

```text
192.168.1.255
```

默认端口：

```text
WOL
9

Agent
9876
```

---

# 12. 多 PC 支持

你的概念图已经明显有多设备管理。

所以数据结构一开始就不能只支持：

```kotlin
pcMacAddress
```

而应该：

```text
List<PCDevice>
```

例如：

```text
My PC
Living Room PC
Workstation
Media Server
```

其中一个：

```text
Default PC
```

首页始终控制 Default PC。

用户可以点击：

```text
My PC >
```

切换目标 PC。

---

# 13. PCDevice 数据模型

建议从一开始就按下面思路设计：

```text
PCDevice

id
name

macAddress

ipAddress
broadcastAddress

wolPort

agentHost
agentPort

isDefault

agentToken

lastSeen

hardwareInfo
```

避免以后多设备时重构全部代码。

---

# 14. Display & Behavior

参考你的 Settings 图，这个模块应该正式包括：

```text
Keep Screen On

Auto Dim

Idle Timeout

Active Brightness

Idle Brightness

Lock Orientation

Fullscreen Mode

Show PC Widgets

Pixel Shift
```

建议默认：

```text
Keep Screen On
ON

Auto Dim
ON

Idle Timeout
60 sec

Active Brightness
80%

Idle Brightness
15%

Pixel Shift
ON
```

---

# 15. Idle 模式

这个其实应该成为应用自己的状态：

```text
ACTIVE
       ↓ 60 sec
IDLE
```

进入 Idle：

```text
降低亮度

停止不必要动画

降低 PC Metrics 刷新率

Pixel Shift

降低天气刷新
```

例如：

```text
ACTIVE

PC metrics:
1 sec

IDLE

PC metrics:
5 sec
```

这样长期使用能显著降低：

```text
手机发热

OLED 消耗

网络请求

电池压力
```

---

# 16. OLED Burn-in 防护

因为你的应用极有可能：

```text
每天亮屏 8–24 小时
```

所以这个应该升级成正式功能，不只是额外优化。

实现：

```text
Pixel Shift
```

每隔：

```text
3–5 min
```

Dashboard 整体产生：

```text
±1~3 dp
```

的微小偏移。

同时：

```text
Clock
Greeting
Status
```

可以偶尔轻微改变位置。

---

# 17. Wallpaper & Personalization

概念图中的这一页非常值得保留。

建议成为一个完整模块。

左侧：

```text
Wallpaper

Themes & Colors

Widgets

Layout

Appearance
```

中间：

```text
Live Preview
```

底部：

```text
Wallpaper

Accent

Widget Style

Transparency

Font Scale

Layout Preset
```

---

# 18. Wallpaper 系统

支持：

### Built-in Wallpapers

例如：

```text
Mountain
Forest
City Night
Cozy Room
Minimal
Abstract
```

### Custom Wallpaper

```text
Choose Image
```

从 Android Photo Picker 选择。

保存 URI。

---

# 19. 主题系统

不要把颜色散落写死在 Composable 里面。

一开始定义：

```text
AppTheme
```

比如：

```text
Aurora Blue

Emerald

Purple

Amber

Red

Dynamic
```

主题控制：

```text
Accent

Online color

Glow

Progress color

Selected border
```

---

# 20. Widget Style

概念图里的设计可以直接保留：

```text
Glass

Solid

Minimal
```

### Glass

```text
透明背景
Blur
细边框
```

### Solid

```text
高不透明 Card
```

### Minimal

```text
几乎没有 Card
```

以后整个 Dashboard Widget 可以共享：

```text
WidgetSurface
```

组件。

---

# 21. Layout Preset

建议实现三套：

```text
Default

Compact

Minimal
```

### Default

天气 + Calendar + Todo + PC。

### Compact

更多 PC Status。

### Minimal

时间 + 天气 + Power。

---

# 22. Widget 系统

这里需要从原计划进一步升级。

不要直接把主页写死成：

```kotlin
WeatherCard()

CalendarCard()

TodoCard()
```

应该至少有 Widget abstraction：

```text
DashboardWidget

id

type

enabled

order

size
```

第一版还不用支持自由拖拽。

先允许：

```text
Show / Hide
```

就可以。

之后再增加：

```text
Drag

Resize

Reorder
```

---

# 23. 天气

概念图中的天气需求实际比原计划更完整。

V1：

```text
Current temperature

Condition

High / Low

Location

Next 4 hours
```

后台刷新：

```text
30 min
```

不需要每分钟刷新。

---

# 24. Calendar

Dashboard：

```text
Tue, Apr 22

10:00 Team Sync

13:00 Lunch

16:00 Plan next week
```

Android 第一版建议读取：

```text
Android Calendar Provider
```

而不是马上自己连接 Google Calendar API。

因为 Android 本身已经可能同步：

```text
Google

Outlook

Exchange
```

未来 iOS：

```text
EventKit
```

---

# 25. Todo

概念图显示 Todo 能：

```text
完成任务

添加任务
```

V1 建议：

```text
Local Todo
```

数据本地 Room 保存。

之后：

```text
V1.1

Todoist

Microsoft To Do

Google Tasks
```

不要 V1 同时搞三套 OAuth。

---

# 26. Notifications

根据 Settings 图，可以加入：

```text
PC Online

Wake Failed

PC Offline

High Temperature

Agent Disconnected
```

但是高温报警建议后做。

第一版：

```text
Wake Success

Wake Failed

Agent Disconnected
```

已经够。

---

# 27. PC Agent 定位

整个项目现在应该正式拆成：

```text
Mobile App

+

PC Agent
```

推荐：

```text
DesktopCompanion/
    mobile/

DesktopCompanion-Agent/
    windows/
```

或者 Monorepo：

```text
DesktopCompanion/

├─ mobile
│
├─ agent
│
└─ docs
```

我更推荐 Monorepo。

因为 API Schema 和版本管理更简单。

---

# 28. Mobile 技术架构

建议继续：

```text
Kotlin Multiplatform

Compose Multiplatform
```

平台：

```text
commonMain

androidMain

iosMain
```

目前只完成：

```text
Android
```

但架构禁止让业务逻辑依赖 Android。

---

# 29. 推荐 App 架构

```text
UI
│
▼
ViewModel / Store
│
▼
Domain
│
▼
Repository
│
├── PCRepository
├── WeatherRepository
├── CalendarRepository
├── TodoRepository
├── SettingsRepository
│
▼
Data Sources
```

---

# 30. 项目目录建议

```text
mobile
└── composeApp
    └── src
        ├── commonMain
        │   └── kotlin
        │       ├── app
        │       │
        │       ├── navigation
        │       │
        │       ├── ui
        │       │   ├── dashboard
        │       │   ├── monitor
        │       │   ├── settings
        │       │   ├── personalization
        │       │   └── components
        │       │
        │       ├── domain
        │       │   ├── model
        │       │   ├── repository
        │       │   └── usecase
        │       │
        │       ├── data
        │       │   ├── pc
        │       │   ├── weather
        │       │   ├── calendar
        │       │   └── todo
        │       │
        │       └── core
        │           ├── network
        │           ├── storage
        │           └── theme
        │
        ├── androidMain
        │
        └── iosMain
```

---

# 31. PC Agent 架构

建议：

```text
ASP.NET Core Minimal API
```

负责：

```text
Heartbeat

System Metrics

Power Control

Quick Actions

Pairing
```

硬件监控：

```text
LibreHardwareMonitor
```

类型的 Hardware Monitor 库。

---

# 32. 手机和电脑通信

分三种完全不同的数据通路：

```text
            Phone
              │
       ┌──────┼──────┐
       │      │      │
       ▼      ▼      ▼

      UDP    HTTP   WebSocket
      WOL    Ctrl     Metrics
       │      │      │
       ▼      ▼      ▼

             PC
```

---

# 33. WOL Flow

用户按：

```text
WAKE PC
```

手机：

```text
读取 MAC
↓
生成 Magic Packet
↓
UDP Broadcast Port 9
↓
发送 3 次
↓
State = WAKING
```

然后：

```text
每 2 sec
```

尝试：

```text
GET /api/v1/status
```

直到：

```text
Agent Response
```

然后：

```text
ONLINE
```

---

# 34. PC Online Flow

正常在线状态：

```text
Agent
     ↓
WebSocket
     ↓
Mobile
```

实时状态：

```text
CPU

GPU

RAM

Temp

Fan

Network
```

Dashboard 更新。

---

# 35. Power Command Flow

例如 Shutdown：

```text
User
↓
Shutdown
↓
Confirm
↓
POST /api/v1/power/shutdown
↓
Agent
↓
Windows Shutdown
↓
Agent Disconnect
↓
Mobile State
SHUTTING_DOWN
↓
OFFLINE
```

---

# 36. 建议 Agent API

```text
GET

/api/v1/status

/api/v1/system

/api/v1/actions


POST

/api/v1/power/sleep

/api/v1/power/shutdown

/api/v1/power/restart

/api/v1/power/lock


POST

/api/v1/actions/{id}
```

WebSocket：

```text
/ws/v1/metrics
```

---

# 37. 安全要求

Agent 不应该裸奔。

最少：

```text
Pairing
↓
Token
↓
Authorization
```

第一次：

```text
Phone
↓
Pair
↓
PC confirmation
↓
Token
```

之后：

```http
Authorization: Bearer TOKEN
```

Token 手机端：

```text
Android Keystore
```

未来 iOS：

```text
Keychain
```

---

# 38. 完整需求清单

下面可以直接作为项目 Requirements。

## Functional Requirements

### Dashboard

* [ ] 横屏 Dashboard
* [ ] Greeting
* [ ] Clock
* [ ] Date
* [ ] Weather
* [ ] Calendar
* [ ] Todo
* [ ] PC Summary
* [ ] Wallpaper
* [ ] Widget show/hide
* [ ] Dashboard Mode
* [ ] Monitor Mode

### Power

* [ ] Wake-on-LAN
* [ ] Wake retry
* [ ] PC online detection
* [ ] Sleep
* [ ] Shutdown
* [ ] Restart
* [ ] Lock PC
* [ ] Command confirmation
* [ ] PC state machine

### PC Monitor

* [ ] CPU usage
* [ ] CPU temp
* [ ] CPU frequency
* [ ] GPU usage
* [ ] GPU temp
* [ ] VRAM
* [ ] RAM
* [ ] Storage
* [ ] Network
* [ ] Uptime
* [ ] Fan RPM
* [ ] 60-sec charts

### Devices

* [ ] Add PC
* [ ] Edit PC
* [ ] Delete PC
* [ ] Multiple PC
* [ ] Default PC
* [ ] MAC
* [ ] IP
* [ ] Broadcast
* [ ] Agent address
* [ ] Test connection

### Appearance

* [ ] Built-in wallpaper
* [ ] Custom wallpaper
* [ ] Accent color
* [ ] Glass theme
* [ ] Solid theme
* [ ] Minimal theme
* [ ] Transparency
* [ ] Font scale
* [ ] Layout preset
* [ ] Live preview

### Display

* [ ] Keep screen on
* [ ] Auto dim
* [ ] Brightness control
* [ ] Idle timeout
* [ ] Touch wake
* [ ] Fullscreen
* [ ] Landscape lock
* [ ] Pixel shift

### Integrations

* [ ] Weather
* [ ] Android Calendar
* [ ] Local Todo

后续：

* [ ] Todoist
* [ ] Google Tasks
* [ ] Microsoft To Do

### Quick Actions

* [ ] Agent action registry
* [ ] Browser
* [ ] Steam
* [ ] Discord
* [ ] Spotify
* [ ] User-defined actions

### Security

* [ ] Device pairing
* [ ] Auth token
* [ ] Secure token storage
* [ ] LAN only
* [ ] Command allowlist
* [ ] Destructive command confirmation

---

# 39. 非功能需求

这部分之前计划里比较少，现在必须补上。

| Requirement        | 目标                      |
| ------------------ | ----------------------- |
| 启动时间               | < 2 秒进入 Dashboard       |
| Idle CPU           | 尽量 < 5% 手机 CPU          |
| Idle Network       | 极低                      |
| PC Metrics         | Active 1 秒一次            |
| Idle Metrics       | 5 秒一次                   |
| Weather            | 30 min                  |
| UI FPS             | 60fps                   |
| Orientation        | Landscape               |
| Offline capability | Clock/Todo/Wallpaper 正常 |
| Burn-in protection | Pixel Shift             |
| App crash recovery | 自动恢复 Dashboard          |
| Agent reconnect    | 自动                      |
| API timeout        | 2–3 秒                   |
| WOL retry          | 3 次                     |
| Android min        | 推荐 Android 8+           |
| iOS ready          | commonMain 不依赖 Android  |

---

# 40. 调整后的任务列表

我建议把项目正式拆成 **12 个 Epic**。

---

## EPIC 1 — Project Foundation

| ID       | Task                     | Priority |
| -------- | ------------------------ | -------: |
| CORE-001 | 创建 KMP 工程                |       P0 |
| CORE-002 | Compose Multiplatform 配置 |       P0 |
| CORE-003 | Android target           |       P0 |
| CORE-004 | iOS target skeleton      |       P0 |
| CORE-005 | commonMain 架构            |       P0 |
| CORE-006 | Navigation               |       P0 |
| CORE-007 | Coroutines               |       P0 |
| CORE-008 | Ktor                     |       P0 |
| CORE-009 | Serialization            |       P0 |
| CORE-010 | DataStore abstraction    |       P0 |
| CORE-011 | Room / DB abstraction    |       P1 |
| CORE-012 | Git + CI                 |       P1 |

---

# EPIC 2 — Design System

| ID     | Task              | Priority |
| ------ | ----------------- | -------: |
| UI-001 | Dark Theme        |       P0 |
| UI-002 | Design Tokens     |       P0 |
| UI-003 | Typography        |       P0 |
| UI-004 | Glass Card        |       P0 |
| UI-005 | Solid Card        |       P1 |
| UI-006 | Minimal Card      |       P1 |
| UI-007 | Accent system     |       P1 |
| UI-008 | Icon system       |       P1 |
| UI-009 | Responsive layout |       P0 |
| UI-010 | Animations        |       P2 |

---

# EPIC 3 — Dashboard

| ID       | Task                        | Priority |
| -------- | --------------------------- | -------: |
| DASH-001 | Dashboard shell             |       P0 |
| DASH-002 | 72/28 Layout                |       P0 |
| DASH-003 | Greeting                    |       P1 |
| DASH-004 | Clock                       |       P0 |
| DASH-005 | Weather card                |       P1 |
| DASH-006 | Calendar card               |       P1 |
| DASH-007 | Todo card                   |       P1 |
| DASH-008 | PC summary card             |       P0 |
| DASH-009 | Decorative widget           |       P2 |
| DASH-010 | Dashboard/Monitor switching |       P1 |

---

# EPIC 4 — Power Rail

| ID        | Task                | Priority |
| --------- | ------------------- | -------: |
| POWER-001 | Power Rail UI       |       P0 |
| POWER-002 | PC State UI         |       P0 |
| POWER-003 | Wake button         |       P0 |
| POWER-004 | Sleep               |       P0 |
| POWER-005 | Shutdown            |       P0 |
| POWER-006 | Restart             |       P0 |
| POWER-007 | Lock                |       P1 |
| POWER-008 | Confirmation dialog |       P0 |
| POWER-009 | Error state         |       P0 |
| POWER-010 | Loading state       |       P0 |

---

# EPIC 5 — WOL

| ID      | Task                   | Priority |
| ------- | ---------------------- | -------: |
| WOL-001 | WakeOnLan interface    |       P0 |
| WOL-002 | Android implementation |       P0 |
| WOL-003 | Magic packet           |       P0 |
| WOL-004 | Broadcast              |       P0 |
| WOL-005 | Retry                  |       P0 |
| WOL-006 | Validation             |       P0 |
| WOL-007 | Wake state machine     |       P0 |
| WOL-008 | iOS implementation     |       V2 |

---

# EPIC 6 — Device Management

| ID         | Task            | Priority |
| ---------- | --------------- | -------: |
| DEVICE-001 | PCDevice model  |       P0 |
| DEVICE-002 | Add PC          |       P0 |
| DEVICE-003 | Edit PC         |       P0 |
| DEVICE-004 | Delete PC       |       P1 |
| DEVICE-005 | Multiple PC     |       P1 |
| DEVICE-006 | Default PC      |       P1 |
| DEVICE-007 | Device selector |       P1 |
| DEVICE-008 | Connection test |       P0 |

---

# EPIC 7 — PC Agent

| ID        | Task             | Priority |
| --------- | ---------------- | -------: |
| AGENT-001 | Agent project    |       P0 |
| AGENT-002 | HTTP Server      |       P0 |
| AGENT-003 | Status API       |       P0 |
| AGENT-004 | Heartbeat        |       P0 |
| AGENT-005 | Shutdown         |       P0 |
| AGENT-006 | Restart          |       P0 |
| AGENT-007 | Sleep            |       P0 |
| AGENT-008 | Lock             |       P1 |
| AGENT-009 | Startup Service  |       P0 |
| AGENT-010 | Hardware monitor |       P1 |
| AGENT-011 | WebSocket        |       P1 |
| AGENT-012 | Pairing          |       P1 |
| AGENT-013 | Token auth       |       P1 |

---

# EPIC 8 — PC Monitor

| ID      | Task            | Priority |
| ------- | --------------- | -------: |
| MON-001 | Monitor screen  |       P1 |
| MON-002 | CPU card        |       P1 |
| MON-003 | GPU card        |       P1 |
| MON-004 | RAM card        |       P1 |
| MON-005 | Storage card    |       P1 |
| MON-006 | Temperature     |       P1 |
| MON-007 | Fans            |       P1 |
| MON-008 | Network         |       P1 |
| MON-009 | Uptime          |       P1 |
| MON-010 | Mini charts     |       P1 |
| MON-011 | Recent activity |       P2 |

---

# EPIC 9 — Quick Actions

| ID         | Task              | Priority |
| ---------- | ----------------- | -------: |
| ACTION-001 | Action data model |       P2 |
| ACTION-002 | Agent allowlist   |       P2 |
| ACTION-003 | Launch action API |       P2 |
| ACTION-004 | Quick Action UI   |       P2 |
| ACTION-005 | Custom actions    |       P3 |

---

# EPIC 10 — Integrations

| ID      | Task                 | Priority |
| ------- | -------------------- | -------: |
| INT-001 | Weather repository   |       P1 |
| INT-002 | Weather API          |       P1 |
| INT-003 | Calendar abstraction |       P1 |
| INT-004 | Android Calendar     |       P1 |
| INT-005 | Todo model           |       P1 |
| INT-006 | Local Todo           |       P1 |
| INT-007 | Todoist              |       P2 |
| INT-008 | Google Tasks         |       P3 |

---

# EPIC 11 — Personalization

| ID        | Task                  | Priority |
| --------- | --------------------- | -------: |
| STYLE-001 | Wallpaper picker      |       P1 |
| STYLE-002 | Built-in wallpapers   |       P1 |
| STYLE-003 | Wallpaper persistence |       P1 |
| STYLE-004 | Accent color          |       P1 |
| STYLE-005 | Widget style          |       P2 |
| STYLE-006 | Transparency          |       P2 |
| STYLE-007 | Font scale            |       P2 |
| STYLE-008 | Layout preset         |       P2 |
| STYLE-009 | Live preview          |       P2 |
| STYLE-010 | Widget visibility     |       P1 |

---

# EPIC 12 — Display & Reliability

| ID      | Task             | Priority |
| ------- | ---------------- | -------: |
| SYS-001 | Keep Screen On   |       P0 |
| SYS-002 | Landscape        |       P0 |
| SYS-003 | Immersive mode   |       P0 |
| SYS-004 | Idle detection   |       P0 |
| SYS-005 | Auto dim         |       P0 |
| SYS-006 | Touch restore    |       P0 |
| SYS-007 | Pixel Shift      |       P1 |
| SYS-008 | Agent reconnect  |       P0 |
| SYS-009 | Network recovery |       P0 |
| SYS-010 | Error handling   |       P0 |

---

# 41. 实际开发流程

我建议不要按照 UI 页面顺序开发。

应该按照依赖关系做。

## Phase 0

项目基础：

```text
KMP
↓
Compose
↓
Architecture
↓
Navigation
↓
Theme
```

目标：

**App 能启动。**

---

## Phase 1

做静态 UI：

```text
Dashboard

Power Rail

Settings

Monitor

Appearance
```

全部用 Mock Data。

例如：

```text
CPU 28%

GPU 62%

Weather 18°

PC Online
```

目标：

> UI 基本达到概念图 80–90%。

这一阶段不要碰 WOL。

---

## Phase 2

设备系统：

```text
PCDevice

Add Device

Edit Device

Default PC

Persistence
```

---

## Phase 3

WOL：

```text
Magic Packet
↓
Broadcast
↓
Wake
↓
Waking State
```

目标：

**真机可以稳定开电脑。**

---

## Phase 4

Agent：

```text
Status
↓
Power
↓
Heartbeat
↓
Authentication
```

目标：

手机可以：

```text
Wake

Sleep

Shutdown

Restart
```

---

## Phase 5

System Monitor：

```text
CPU
GPU
RAM
Temp
Fan
Network
```

HTTP 先跑通。

然后再切：

```text
WebSocket
```

---

# 42. 为什么 WebSocket 不应该一开始做

最开始：

```text
GET /system
```

每两秒请求一次。

非常容易调试。

等所有 Metrics 正确以后：

```text
WebSocket
```

再改成实时流。

这样排错难度会低很多。

---

# 43. Phase 6

StandBy 功能：

```text
Weather

Calendar

Todo
```

让项目正式成为桌搭设备。

---

# 44. Phase 7

Personalization：

```text
Wallpaper

Theme

Glass

Transparency

Layout

Widgets
```

---

# 45. Phase 8

长期使用优化：

```text
Keep screen

Idle dim

Pixel shift

Network reconnect

Agent reconnect

Caching
```

最终开始：

```text
24h Stability Test
```

---

# 46. 第一版明确不要做的东西

这点非常重要。

V1 暂时不要：

```text
Cloud account system

Internet remote control

Port Forwarding

Remote Desktop

File transfer

Full Google OAuth

PC screen mirroring

Android Widget

Apple Watch

WearOS

User plugins

Arbitrary command execution

Metrics history database
```

否则 Scope 会直接失控。

---

# 47. 最终 V1.0 范围

我建议你的 **正式 Android V1.0** 就锁定：

### Dashboard

```text
Clock

Weather

Calendar

Todo

PC status
```

### PC

```text
Wake

Sleep

Shutdown

Restart

Online detection
```

### Monitor

```text
CPU

GPU

RAM

Storage

Temperature

Fans

Network

Uptime
```

### Personalization

```text
Wallpaper

Accent

Widget Visibility
```

### System

```text
Landscape

Fullscreen

Keep screen on

Auto dim

Pixel shift
```

### Device

```text
Multiple PCs

Default PC
```

---

# 48. V1.1

再增加：

```text
Quick Actions

Widget Style

Layout Presets

Live Preview

Notifications

Todoist

More Metrics
```

---

# 49. V2

再进入：

```text
iOS

EventKit

iOS WOL

Keychain

iPhone StandBy optimization

Cross-device sync
```

这时候 KMP 的价值才真正体现出来。

---

# 50. 最终系统形态

调整之后，这个项目实际上已经是三个软件组件：

```text
                  Desktop Companion

                         │

       ┌─────────────────┴─────────────────┐
       │                                   │
       ▼                                   ▼

   Mobile App                          PC Agent

Android / iOS                         Windows

Dashboard                             Metrics

Weather                               Hardware

Calendar                              Power

Todo                                  Quick Actions

WOL ───────────────────────────────► Motherboard
```

产品层面则形成：

```text
Desk Decoration
      +
StandBy Display
      +
PC Monitor
      +
PC Remote Control
```

这比原来的“做一个带天气的 WOL App”完整得多，而且你这组概念图已经足够支撑这个方向。

**当前最合理的开发起点也随之变化：先不要写 WOL。第一步应该先把概念图抽象成统一 Design System，然后使用 Mock Data 把 `Dashboard + Power Rail + PC Monitor + Settings + Personalization` 五个核心页面完整还原出来。等 UI 架构稳定后，再逐层接入 Device、WOL 和 PC Agent。**这样后面不会因为功能实现而反复推翻 UI 和数据模型。
