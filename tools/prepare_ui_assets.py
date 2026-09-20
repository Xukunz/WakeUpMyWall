#!/usr/bin/env python3
"""把 imgs/ 下的母版加工成 App 打包资源。

母版（`imgs/ui/`、`imgs/wallpaper/`）是图像生成工具的原始输出：尺寸不一、带大片
空白透明边、部分边缘有零散半透明像素。App 里直接使用会出现"每个图标视觉大小不同"
和"透明边把布局撑开"的问题，因此统一在这里加工：

  1. 按 alpha 裁剪到真实内容（去掉生成器留下的空白边）。
  2. 透明图统一放进正方形画布，内容缩放到画布的固定占比，四周留出安全边距。
  3. 缩放走 premultiplied alpha（`RGBa`），避免半透明边缘出现黑色描边。
  4. 输出到 `mobile/composeApp/src/commonMain/composeResources/drawable/`，
     文件名即 Compose 资源名（只允许小写字母、数字、下划线）。

用法：

    python3 tools/prepare_ui_assets.py            # 生成全部资源
    python3 tools/prepare_ui_assets.py --dry-run  # 只打印将要生成的内容

依赖 Pillow。脚本是幂等的：重复运行结果一致，可安全重跑。
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

REPO = Path(__file__).resolve().parent.parent
UI_MASTERS = REPO / "imgs" / "ui"
WALLPAPER_MASTERS = REPO / "imgs" / "wallpaper"
OUT = REPO / "mobile" / "composeApp" / "src" / "commonMain" / "composeResources" / "drawable"

# 内容占正方形画布的比例：0.76 = 四周各留 12% 安全边距（imgs/ui/README.txt 的目标值）。
ICON_CONTENT_RATIO = 0.76
# 电源按钮底图是"实体旋钮"，四周只需一点点透气空间。
KNOB_CONTENT_RATIO = 0.94
# alpha 低于该值的像素视为生成器留下的杂点，直接清零（不会伤到真实的柔和阴影）。
ALPHA_FLOOR = 8


def load_rgba(path: Path) -> Image.Image:
    return Image.open(path).convert("RGBA")


def clean_alpha(image: Image.Image) -> Image.Image:
    """把低于阈值的 alpha 清零：透明背景里的零散半透明像素会被缩放放大成灰雾。"""
    alpha = image.getchannel("A").point(lambda value: 0 if value < ALPHA_FLOOR else value)
    cleaned = image.copy()
    cleaned.putalpha(alpha)
    return cleaned


def content_bbox(image: Image.Image) -> tuple[int, int, int, int]:
    bbox = image.getchannel("A").point(lambda value: 255 if value > ALPHA_FLOOR else 0).getbbox()
    if bbox is None:
        raise ValueError("图像没有任何不透明像素，无法裁剪")
    return bbox


def resize_rgba(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    """premultiplied 缩放：直接对 RGBA 做 LANCZOS 会让半透明边缘渗出黑边。"""
    return image.convert("RGBa").resize(size, Image.Resampling.LANCZOS).convert("RGBA")


def normalize_icon(image: Image.Image, canvas: int, content_ratio: float) -> Image.Image:
    """裁剪 → 等比缩放到固定占比 → 居中放进正方形画布。"""
    cleaned = clean_alpha(image)
    trimmed = cleaned.crop(content_bbox(cleaned))
    target = max(1, round(canvas * content_ratio))
    scale = target / max(trimmed.width, trimmed.height)
    scaled = resize_rgba(
        trimmed,
        (max(1, round(trimmed.width * scale)), max(1, round(trimmed.height * scale))),
    )
    out = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    out.paste(
        scaled,
        ((canvas - scaled.width) // 2, (canvas - scaled.height) // 2),
        scaled,
    )
    return out


def crop_to_ratio(image: Image.Image, ratio: float) -> Image.Image:
    """居中裁剪到指定宽高比（壁纸母版已经是 16:9，封面不是）。"""
    width, height = image.size
    target_height = round(width / ratio)
    if target_height <= height:
        top = (height - target_height) // 2
        return image.crop((0, top, width, top + target_height))
    target_width = round(height * ratio)
    left = (width - target_width) // 2
    return image.crop((left, 0, left + target_width, height))


# 天气图标的编目：文件名（去 weather_ 前缀）→ 画布边长。图标在 48–64dp 的槽位里显示，
# 256px 在 2x 屏上是 128dp 等效，留足了余量；再大只会白占 APK。
WEATHER_ICONS = [
    "clear_day",
    "clear_night",
    "partly_cloudy_day",
    "partly_cloudy_night",
    "cloudy",
    "rain",
    "snow",
    "sleet",
    "fog",
    "wind",
    "thunderstorm",
    "unknown",
]
WEATHER_CANVAS = 256

# PC 封面：每台设备一张（概念图 C1 / B3 的缩略图）。取不到设备封面时界面回落到 default。
PC_COVERS = ["pc_default_cover", "pc_cover_1", "pc_cover_2"]

# 壁纸编目：id → (标签, JPEG 质量)。`minimal` 是大面积渐变，需要更高质量避免色带。
WALLPAPERS = [
    ("aurora", 90),
    ("cherry", 90),
    ("city_night", 90),
    ("dusk_lake", 90),
    ("forest_mist", 90),
    ("minimal", 95),
    ("space", 90),
]
WALLPAPER_SIZE = (2560, 1440)
WALLPAPER_THUMB_SIZE = (384, 216)
WALLPAPER_ASPECT = 16 / 9


def save(image: Image.Image, path: Path, dry_run: bool, **kwargs) -> tuple[Path, int]:
    if not dry_run:
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path, **kwargs)
        return path, path.stat().st_size
    return path, 0


def build(dry_run: bool) -> list[tuple[Path, int]]:
    written: list[tuple[Path, int]] = []

    for name in WEATHER_ICONS:
        source = UI_MASTERS / f"weather_{name}.png"
        icon = normalize_icon(load_rgba(source), WEATHER_CANVAS, ICON_CONTENT_RATIO)
        written.append(save(icon, OUT / f"weather_{name}.png", dry_run, optimize=True))

    # 封面是照片、不透明，走 JPEG；PNG 在这里只会把包撑大一倍。
    for cover_name in PC_COVERS:
        cover = crop_to_ratio(
            Image.open(UI_MASTERS / f"{cover_name}.png").convert("RGB"),
            WALLPAPER_ASPECT,
        ).resize((640, 360), Image.Resampling.LANCZOS)
        written.append(
            save(
                cover,
                OUT / f"{cover_name}.jpg",
                dry_run,
                quality=88,
                optimize=True,
                progressive=True,
            )
        )

    # `imgs/ui/power_button_base.png`（写实旋钮）暂时**不进包**：2026-09-20 用户确认主按钮按
    # 概念图 A3 做成"发光环 + 字形"，环与字形都由代码画（accent 变色）。母版留在 imgs/ui/ 里，
    # 将来要做"实体按钮"风格时，把下面三行取消注释即可（KNOB_CONTENT_RATIO 仍然有效）。
    #
    # knob = normalize_icon(load_rgba(UI_MASTERS / "power_button_base.png"), 512, KNOB_CONTENT_RATIO)
    # written.append(save(knob, OUT / "power_button_base.png", dry_run, optimize=True))

    for wallpaper_id, quality in WALLPAPERS:
        master = Image.open(WALLPAPER_MASTERS / f"wallpaper_{wallpaper_id}.png").convert("RGB")
        full = master.resize(WALLPAPER_SIZE, Image.Resampling.LANCZOS)
        written.append(
            save(
                full,
                # 资源名沿用 Phase 0 的既有约定 `<id>_wallpaper.jpg`（Res.drawable.aurora_wallpaper），
                # 而不是把文件名前缀写成 wallpaper_ —— 否则会和已有的两个文件重复打包。
                OUT / f"{wallpaper_id}_wallpaper.jpg",
                dry_run,
                quality=quality,
                optimize=True,
                progressive=True,
                subsampling=0 if quality >= 95 else 2,
            )
        )
        thumb = master.resize(WALLPAPER_THUMB_SIZE, Image.Resampling.LANCZOS)
        written.append(
            save(
                thumb,
                OUT / f"{wallpaper_id}_wallpaper_thumb.jpg",
                dry_run,
                quality=85,
                optimize=True,
                progressive=True,
            )
        )

    return written


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="只列出将生成的文件")
    args = parser.parse_args()

    for directory in (UI_MASTERS, WALLPAPER_MASTERS):
        if not directory.is_dir():
            print(f"缺少母版目录：{directory}", file=sys.stderr)
            return 2

    written = build(args.dry_run)
    total = sum(size for _, size in written)
    verbose = args.dry_run or total > 0
    for path, size in written:
        if verbose:
            relative = path.relative_to(REPO)
            print(f"{relative}  {size / 1024:8.1f} KiB" if size else str(relative))
    if total:
        print(f"\n合计 {len(written)} 个文件，{total / 1024 / 1024:.2f} MiB")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
