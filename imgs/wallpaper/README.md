# 内置壁纸母版

| 文件 | 用途 | 编目 id |
| --- | --- | --- |
| `wallpaper_aurora.png` | 默认壁纸（暗色，极光） | `aurora` |
| `wallpaper_minimal.png` | 备选壁纸（浅色，抽象曲面） | `minimal` |

母版为 3840×2160 无损 PNG，只做留档，**不参与打包**。真正进包的是从母版派生的资源：

```
mobile/composeApp/src/commonMain/composeResources/drawable/
  aurora_wallpaper.jpg    2560×1440, JPEG q90
  minimal_wallpaper.jpg   2560×1440, JPEG q95
```

派生规则：Lanczos 缩放到 2560×1440（墙面屏 2x 足够），JPEG 分别取 q90 / q95（`minimal` 是大面积渐变，需要更高质量避免色带）。
这样两张壁纸合计约 1.1 MB，而不是母版的 13 MB。

## 重新生成

需要 Pillow：

```bash
python3 - <<'PY'
from PIL import Image
base = "imgs/wallpaper/"
out = "mobile/composeApp/src/commonMain/composeResources/drawable/"
for src, dst, quality in [
    ("wallpaper_aurora.png", "aurora_wallpaper.jpg", 90),
    ("wallpaper_minimal.png", "minimal_wallpaper.jpg", 95),
]:
    image = Image.open(base + src).convert("RGB").resize((2560, 1440), Image.LANCZOS)
    image.save(out + dst, quality=quality, optimize=True, progressive=True,
               subsampling=0 if quality >= 95 else 2)
PY
```

## 约束

- 每个母版必须在 `core/wallpaper/BuiltInWallpapers.kt` 有编目项，并在 `ui/components/WallpaperBackground.kt` 有资源映射；
  少了任何一边，`AppUiTest.every built in wallpaper renders` 会在渲染阶段失败。
- 暗色主题下必须保持正文对比度：浅色壁纸的 `scrimAlpha` 必须强于暗色壁纸（由 `BuiltInWallpapersTest` 断言）。
- 替换母版后请重跑 `./gradlew :composeApp:desktopTest`，并人工看一眼 `DesktopTest` 报告里的渲染截图。
