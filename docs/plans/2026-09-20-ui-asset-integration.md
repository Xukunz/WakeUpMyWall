# UI 素材接入记录（2026-09-20）

**起因：** 用户交付了 UI 素材包（`imgs/ui/`，13 个 PNG）与 7 张壁纸母版（`imgs/wallpaper/`），
交付形态是**本身带颜色的 PNG**，并给出处理原则：

> 没有的尽量通过代码实现，如果代码实现过于复杂或者是无法实现则通知我制作需要的 UI。

本文记录这批素材怎么进包、进到哪些界面、哪些图形是代码画的、以及**还需要用户出图的清单**（第 5 节）。

## 1. 素材加工流水线

母版不能直接进包：它们是图像生成工具的原始输出（天气图标 1254×1254、四周大片空白透明边、
边缘有零散半透明像素，封面 1672×941 不是 16:9）。统一由脚本加工：

```bash
python3 tools/prepare_ui_assets.py            # 生成全部资源（幂等）
python3 tools/prepare_ui_assets.py --dry-run  # 只列出将要生成的文件
```

加工规则（脚本里逐条有注释）：

1. 按 alpha 裁剪到真实内容，alpha < 8 的杂点清零；
2. 透明图放进正方形画布，内容占比固定 —— 图标 76%（= 四周 12% 安全边距，即素材说明里未达成的目标留白）；
3. 缩放走 premultiplied alpha（`RGBa`），避免半透明边缘渗出黑边；
4. 壁纸按 `imgs/wallpaper/README.md` 的既有约定派生 2560×1440 JPEG（`minimal` 用 q95 防色带），
   另出一张 384×216 缩略图给 Appearance 的壁纸选择器。

| 母版 | 产物 | 尺寸 / 格式 |
| --- | --- | --- |
| `imgs/ui/weather_*.png`（11 张） | `weather_*.png` | 256×256 RGBA |
| `imgs/ui/power_button_base.png` | `power_button_base.png` | 512×512 RGBA |
| `imgs/ui/pc_default_cover.png` | `pc_default_cover.jpg` | 640×360 q88 |
| `imgs/wallpaper/wallpaper_*.png`（7 张） | `<id>_wallpaper.jpg` + `<id>_wallpaper_thumb.jpg` | 2560×1440 q90/95 + 384×216 q85 |

产出共 27 个文件、5.65 MiB（其中壁纸 5.05 MiB）。壁纸是大头：**如果后续要压包体，
把 `WALLPAPER_SIZE` 降到 1920×1080 能省掉约 45%**，代价是 2560px 宽的目标屏上略微软化。

## 2. 素材接进了哪些界面

| 素材 | 接入点 | 代码位置 |
| --- | --- | --- |
| `weather_*.png` | 首页天气卡主图标、逐时四列的小图标、StandBy 天气卡 | `ui/components/WeatherIcon.kt`、`ui/dashboard/widgets/WeatherWidget.kt`、`ui/standby/StandByMode.kt` |
| `power_button_base.png` | Power Rail 主按钮；StandBy 浮层卡的小环 | `ui/powerrail/PowerRingButton.kt` |
| `pc_default_cover.jpg` | 首页 PC 摘要卡缩略图、Monitor 身份卡缩略图 | `ui/components/PcCover.kt`、`ui/dashboard/widgets/PcSummaryWidget.kt`、`ui/monitor/MonitorMode.kt` |
| 7 张壁纸 | 背景 + Appearance 的壁纸选择器（带选中对勾） | `core/wallpaper/BuiltInWallpapers.kt`、`ui/components/WallpaperBackground.kt`、`ui/settings/AppearanceScreen.kt` |

天气图标是**按语义选型**的，不是按文件名硬编码：`weatherKind(condition, isNight)` 把 provider 的自由文本
映射到 11 类中的一类（`freezing rain` → Sleet、`thunderstorm` → Thunderstorm、说不清的一律回落 Cloudy），
昼夜由数据里的 `isNight` 决定，**不从 `10PM` 这种展示串反推**。为此给 `WeatherSnapshot` 加了 `isNight`，
给 `HourlyForecast` 加了 `condition` / `isNight`（Phase 6 接真实 provider 时由 provider 直接填）。
规则由 `WeatherKindTest` 覆盖。

壁纸编目从 2 张扩到 7 张（`aurora` 默认，另有 `dusk_lake` / `forest_mist` / `city_night` / `space` /
`cherry` / `minimal`）。`scrimAlpha` 按母版实测平均亮度定：越亮压得越狠（`cherry` 104 → 0.36，
`minimal` 206 → 0.62），`BuiltInWallpapersTest` 里"浅色必须比深色压得狠"的断言继续成立。

## 3. 代码画的图形（没有素材的部分）

Phase 1 的全局约束是"不引入图标库、不内置第三方商标"，用户又要求缺的用代码补，因此新增
`ui/icons/AppIcons.kt`：24×24 网格上的纯路径图标，尺寸取 `AppSizes` 令牌、颜色由主题或
`MetricColors` 给，文件里没有裸 dp、没有字面量颜色（`DesignTokenDisciplineTest` 守着）。

| 图标 | 取代掉的字形占位 | 用在哪 |
| --- | --- | --- |
| Gear / ChevronRight / Plus / Check / LocationPin | `⚙` `>` `+` `●○` `📍` | Rail 设置、PC 摘要与身份卡、日历、待办、天气地点 |
| Moon / StopSquare / Restart | （原来只有文字） | Rail 的 Sleep / Shut Down / Restart |
| Display / AppTile | `▣` `▦` | StandBy 底部状态条、Quick Actions 的四个瓦片 |
| Cpu / Gpu / Ram / Storage / Thermometer / Fan / Wifi / Clock / List | （原来只有文字） | Monitor 的指标卡标题行、温度风扇行、网络 / 运行时长 / 最近活动 |
| PowerGlyph | `⏻` | 主电源环中央（与环的发光一起画） |

Quick Actions 里的 Discord / Steam / Spotify **是第三方商标，不内置也不仿制**：用中性的
`AppTile` 图形 + 文字标签表示"某个应用"（Phase 1 既定约束 R9）。

指标语义色（CPU 青 / GPU 绿 / RAM 蓝紫 / Storage 天蓝、温度绿与琥珀、活动圆点四色）来自概念图像素实测，
写在 `core/theme/MetricColors.kt`，表里逐条记录了采样值与用途；`MetricColorsTest` 断言这些**图形元素**
在深色卡面与深色背景上都 ≥ 3:1（WCAG 对非文本内容的要求）。

## 4. 本轮的视觉修正（素材接进来以后暴露出来的问题）

| 位置 | 问题 | 处理 |
| --- | --- | --- |
| 首页 PC 摘要卡 | 指标是纵向四条，概念图是横向四列 | 改成四列；Network 列更宽（唯一两行数值） |
| 同上 | 指标条是"浮空色条"，概念图是"灰底细轨 + 彩色填充" | 补上细轨 |
| Monitor 温度 / 风扇行 | 只有温度有进度条、风扇纯文本、无圆点；条是浮空色条 | 统一成"圆点 + 名称 + 右对齐数值 + 细轨"，风扇转速按 2000 RPM 换算 |
| Monitor 底行 | 四张卡等宽，应用名被截成 `Micros…` | 按概念图给不等权重（Activity 1.35×），网络 / 时长列收窄 |
| 天气卡 | 逐时是"一行文本"，没有图标、没有列 | 四列（时刻 / 图标 / 温度）；最高最低温改成温度右侧的上下两行 |
| StandBy 浮层卡 | 140dp 的环把卡片撑高、旋钮压住标题 | 收到 112dp |

渲染证据（真实帧，非设计稿）：`docs/plans/screenshots/` 下的
`dashboard.png`、`monitor.png`、`standby.png`、`personalization.png`、`settings.png`、
`dashboard-compact.png`、`dashboard-minimal.png` 已按本轮代码重出（命令见该目录 README）。

## 5. 还需要用户出图的清单

**结论：不阻塞。** 概念图里的每个图形槽位都已经有图（交付素材 13 张 + 代码绘制 + 代码矢量图标）。
下面两条是**可选升级**，不是缺口：

1. **每台设备各自的封面图（可选，Phase 2 再说）**：现在 `pc_default_cover` 是唯一一张，
   Device Setup 里另外 3 台设备（Living Room PC / Workstation / Media Server）都用它。
   要做"每台机器一张",需要额外 3 张同风格封面，命名建议 `pc_cover_<deviceId>.png`，
   16:9、≥1280×720，风格与现有那张一致（深色环境、机身打光）。
2. **另一类天气图标的兜底图（可选）**：provider 返回无法归类的 condition 时现在回落到 `weather_cloudy`。
   如果希望这种情况有独立视觉，请出一张 `weather_unknown.png`（同样 1254×1254 RGBA 即可，脚本会统一到 256×256）。

**反过来，以下这些不用出图**，代码已经覆盖：所有界面图标（设置齿轮、箭头、加号、对勾、定位、
月牙、停止、重启、显示器、CPU/GPU/内存/硬盘/温度/风扇/网络/时钟/列表）、指标环形进度与细条、
发光与状态色、第三方应用的商标替身、壁纸缩略图（从母版自动派生）。

## 6. 验证

```bash
JAVA_HOME=<jdk-25> ./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest --offline
# BUILD SUCCESSFUL —— testDebugUnitTest 101 / desktopTest 175，0 failures（数字取自 build/test-results 的 XML 汇总）
```

新增的守卫：

- `WeatherKindTest`（commonTest）：11 类映射规则、大小写与同义词、具体词优先、兜底；
- `MetricColorsTest`（commonTest）：指标色对比度 ≥ 3:1、四个环色互不相同；
- `UiAssetsRenderTest`（desktopTest）：把"素材确实被渲染出来"钉住 —— 天气图标、PC 封面（摘要卡 + 身份卡）、
  电源底图 + 代码字形；
- 更新了因**有意的结构变化**而失效的旧断言（`dashboard:weather-range` 拆成 high/low、
  `metric:temps-cpu` 名称与数值分开、壁纸目录从 2 张变 7 张、壁纸选择器改为横向滚动后需先滚进视口）。

## 7. 仍未解决 / 需要裁决

1. **默认壁纸**：概念图 F 行标的是 `Dusk Lake` 默认带对勾，我们沿用 Phase 0 的 `aurora` 默认
   （`BuiltInWallpapersTest` 与 `AppUiTest` 都钉着 aurora）。要改成 `Dusk Lake` 的话，改一处常量 + 两个断言。
2. **StandBy 浮层卡语义**（§7.3 老问题，本轮未动）：概念图卡面写 `POWER CONTROL` + 右上 `>` +
   `Power On`/`WAKE YOUR PC`，我们在 ONLINE 状态渲染的是 `PC Online`/`AGENT CONNECTED`。
   这是"显示动作"还是"显示状态"的产品取舍，需要你定。
3. **壁纸母版与 `imgs/ui` 尚未入库**：`imgs/ui/`（13 张）与 5 张新壁纸母版仍是 untracked，
   `git status` 可见。要我提交请说一声（仓库里 8 MB 左右的母版按既有约定是只留档、不打包）。
4. **电源按钮的形态**：交付的 `power_button_base` 是写实旋钮，概念图 A3 是发光圆环 + 字形。
   现在的做法是"旋钮底图 + 代码画的环绕发光 + 代码画的字形"，强调色切换时环与字形跟着变色。
   如果最终想要的是概念图那种纯发光环，说一声即可——那一步是纯代码，不需要新素材。
