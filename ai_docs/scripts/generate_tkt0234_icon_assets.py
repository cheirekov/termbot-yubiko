#!/usr/bin/env python3
"""Generate TKT-0234 launcher/store icon assets.

Creates:
- Android launcher png assets (legacy + mipmap)
- Adaptive icon foreground/monochrome png layers
- Draft store asset package with 3 concepts + launcher mask previews
"""

from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

WORKSPACE = Path(__file__).resolve().parents[2]
RES = WORKSPACE / "repos/termbot-termbot/app/src/main/res"
STORE = WORKSPACE / "ai_docs/docs/NEW/tkt-0234-store-assets"

DENSITY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

BG_TOP = (0x0A, 0x66, 0x68, 0xFF)
BG_BOTTOM = (0x1F, 0x94, 0x8D, 0xFF)
KEY_COLOR = (0xF7, 0xFB, 0xFF, 0xFF)
HOLE_COLOR = (0x12, 0x72, 0x75, 0xFF)
ACCENT = (0xFB, 0xC0, 0x2D, 0xFF)


def ensure_parent(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def lerp(a: int, b: int, t: float) -> int:
    return int(a + (b - a) * t)


def vertical_gradient(size: int, top, bottom) -> Image.Image:
    img = Image.new("RGBA", (size, size), top)
    px = img.load()
    for y in range(size):
        t = y / max(1, size - 1)
        row = (
            lerp(top[0], bottom[0], t),
            lerp(top[1], bottom[1], t),
            lerp(top[2], bottom[2], t),
            lerp(top[3], bottom[3], t),
        )
        for x in range(size):
            px[x, y] = row
    return img


def key_geometry(size: int):
    cx = int(size * 0.36)
    cy = int(size * 0.45)
    r = int(size * 0.16)
    stem_h = int(size * 0.13)
    stem_y0 = cy - stem_h // 2
    stem_y1 = stem_y0 + stem_h
    stem_x0 = cx + int(r * 0.25)
    stem_x1 = int(size * 0.80)
    t1_x0 = int(size * 0.63)
    t1_w = int(size * 0.07)
    t1_h = int(size * 0.09)
    t2_x0 = int(size * 0.73)
    t2_w = int(size * 0.06)
    t2_h = int(size * 0.065)
    return {
        "cx": cx,
        "cy": cy,
        "r": r,
        "stem": (stem_x0, stem_y0, stem_x1, stem_y1),
        "t1": (t1_x0, stem_y1 - 1, t1_x0 + t1_w, stem_y1 + t1_h),
        "t2": (t2_x0, stem_y1 - 1, t2_x0 + t2_w, stem_y1 + t2_h),
    }


def draw_key(draw: ImageDraw.ImageDraw, size: int, fill, hole=None, add_accent=True):
    g = key_geometry(size)
    cx = g["cx"]
    cy = g["cy"]
    r = g["r"]

    draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=fill)
    draw.rounded_rectangle(g["stem"], radius=max(1, int(size * 0.03)), fill=fill)
    draw.rectangle(g["t1"], fill=fill)
    draw.rectangle(g["t2"], fill=fill)

    if hole is not None:
        hr = int(r * 0.43)
        draw.ellipse((cx - hr, cy - hr, cx + hr, cy + hr), fill=hole)

    if add_accent:
        a = int(size * 0.11)
        ax1 = int(size * 0.84)
        ay0 = int(size * 0.17)
        draw.ellipse((ax1 - a, ay0, ax1, ay0 + a), fill=ACCENT)


def rounded_bg(size: int) -> Image.Image:
    bg = vertical_gradient(size, BG_TOP, BG_BOTTOM)
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    radius = int(size * 0.24)
    d.rounded_rectangle((0, 0, size - 1, size - 1), radius=radius, fill=255)

    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(bg, (0, 0), mask)
    return out


def circle_bg(size: int) -> Image.Image:
    bg = vertical_gradient(size, BG_TOP, BG_BOTTOM)
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse((0, 0, size - 1, size - 1), fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(bg, (0, 0), mask)
    return out


def render_launcher_icon(size: int, round_icon=False) -> Image.Image:
    img = circle_bg(size) if round_icon else rounded_bg(size)
    d = ImageDraw.Draw(img)
    draw_key(d, size, KEY_COLOR, hole=HOLE_COLOR, add_accent=True)
    return img


def render_adaptive_foreground(size: int, monochrome=False) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Keep glyph in adaptive safe zone (center 66%).
    pad = int(size * 0.17)
    glyph = Image.new("RGBA", (size - 2 * pad, size - 2 * pad), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glyph)
    fill = (0, 0, 0, 255) if monochrome else KEY_COLOR
    hole = None if monochrome else HOLE_COLOR
    draw_key(gd, glyph.size[0], fill, hole=hole, add_accent=(not monochrome))
    img.paste(glyph, (pad, pad), glyph)
    return img


def save_png(path: Path, image: Image.Image) -> None:
    ensure_parent(path)
    image.save(path, "PNG")


def concept_a(size: int) -> Image.Image:
    return render_launcher_icon(size, round_icon=False)


def concept_b(size: int) -> Image.Image:
    top = (0x0F, 0x25, 0x3F, 0xFF)
    bottom = (0x19, 0x4D, 0x78, 0xFF)
    bg = vertical_gradient(size, top, bottom)
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle((0, 0, size - 1, size - 1), radius=int(size * 0.24), fill=255)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    img.paste(bg, (0, 0), mask)
    d = ImageDraw.Draw(img)

    # Shield silhouette
    sx0 = int(size * 0.24)
    sx1 = int(size * 0.76)
    sy0 = int(size * 0.20)
    sy1 = int(size * 0.80)
    shield = [
        (sx0, sy0),
        (sx1, sy0),
        (int(size * 0.72), int(size * 0.60)),
        (int(size * 0.50), sy1),
        (int(size * 0.28), int(size * 0.60)),
    ]
    d.polygon(shield, fill=(0xF5, 0xFA, 0xFF, 0xFF))

    # Key cut on top of shield
    draw_key(d, size, (0x1A, 0x55, 0x84, 0xFF), hole=(0xF5, 0xFA, 0xFF, 0xFF), add_accent=False)
    return img


def concept_c(size: int) -> Image.Image:
    top = (0x1D, 0x6E, 0x3A, 0xFF)
    bottom = (0x2C, 0x90, 0x4A, 0xFF)
    bg = vertical_gradient(size, top, bottom)
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle((0, 0, size - 1, size - 1), radius=int(size * 0.24), fill=255)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    img.paste(bg, (0, 0), mask)
    d = ImageDraw.Draw(img)

    # Terminal prompt >_ + key
    arrow = [
        (int(size * 0.22), int(size * 0.36)),
        (int(size * 0.37), int(size * 0.50)),
        (int(size * 0.22), int(size * 0.64)),
    ]
    d.line(arrow, fill=(0xF6, 0xFB, 0xF8, 0xFF), width=max(2, int(size * 0.06)), joint="curve")
    d.line(
        (
            int(size * 0.43),
            int(size * 0.62),
            int(size * 0.72),
            int(size * 0.62),
        ),
        fill=(0xF6, 0xFB, 0xF8, 0xFF),
        width=max(2, int(size * 0.05)),
    )
    draw_key(d, size, (0xF6, 0xFB, 0xF8, 0xFF), hole=(0x2A, 0x84, 0x44, 0xFF), add_accent=True)
    return img


def render_mask_preview(icon: Image.Image) -> Image.Image:
    w, h = (1300, 520)
    out = Image.new("RGBA", (w, h), (0xF3, 0xF6, 0xF8, 0xFF))
    d = ImageDraw.Draw(out)

    labels = [
        ("Circle", "circle"),
        ("Rounded", "rounded"),
        ("Squircle", "squircle"),
    ]
    x = 80
    y = 110
    box = 300
    font = ImageFont.load_default()

    for label, mask_name in labels:
        mask = Image.new("L", (box, box), 0)
        md = ImageDraw.Draw(mask)
        if mask_name == "circle":
            md.ellipse((0, 0, box - 1, box - 1), fill=255)
        elif mask_name == "rounded":
            md.rounded_rectangle((0, 0, box - 1, box - 1), radius=64, fill=255)
        else:
            md.rounded_rectangle((0, 0, box - 1, box - 1), radius=92, fill=255)
        thumb = icon.resize((box, box), Image.LANCZOS)
        tile = Image.new("RGBA", (box, box), (0, 0, 0, 0))
        tile.paste(thumb, (0, 0), mask)
        out.paste(tile, (x, y), tile)
        d.text((x + 100, y + 330), label, fill=(0x1E, 0x29, 0x32, 0xFF), font=font)
        x += 390

    d.text((80, 40), "Launcher mask preview (selected concept)", fill=(0x1E, 0x29, 0x32, 0xFF), font=font)
    return out


def render_feature_graphic(icon: Image.Image) -> Image.Image:
    w, h = (1024, 500)
    img = Image.new("RGBA", (w, h), BG_BOTTOM)
    d = ImageDraw.Draw(img)

    # Soft bands for depth.
    d.rectangle((0, 0, w, h // 3), fill=(0x14, 0x7B, 0x79, 0xFF))
    d.rectangle((0, h // 3, w, h), fill=(0x0C, 0x57, 0x5B, 0xFF))

    icon_size = 280
    icon_img = icon.resize((icon_size, icon_size), Image.LANCZOS)
    img.paste(icon_img, (70, 110), icon_img)

    font = ImageFont.load_default()
    d.text((390, 190), "TermBot", fill=(0xF4, 0xFA, 0xFF, 0xFF), font=font)
    d.text((390, 220), "YubiKey SSH Client", fill=(0xD9, 0xEC, 0xF4, 0xFF), font=font)
    d.text((390, 260), "Draft feature graphic", fill=(0xD9, 0xEC, 0xF4, 0xFF), font=font)
    return img


def generate_android_assets() -> None:
    for density, size in DENSITY_SIZES.items():
        square = render_launcher_icon(size, round_icon=False)
        round_img = render_launcher_icon(size, round_icon=True)

        save_png(RES / f"mipmap-{density}/icon.png", square)
        save_png(RES / f"mipmap-{density}/icon_round.png", round_img)
        save_png(RES / f"drawable-{density}/ic_launcher.png", square)

    adaptive_size = 432  # xxxhdpi baseline for adaptive layers
    save_png(
        RES / "drawable-nodpi/ic_launcher_foreground_adaptive.png",
        render_adaptive_foreground(adaptive_size, monochrome=False),
    )
    save_png(
        RES / "drawable-nodpi/ic_launcher_monochrome_adaptive.png",
        render_adaptive_foreground(adaptive_size, monochrome=True),
    )


def generate_store_assets() -> None:
    STORE.mkdir(parents=True, exist_ok=True)

    a = concept_a(1024)
    b = concept_b(1024)
    c = concept_c(1024)

    a.save(STORE / "icon-concept-a-selected.png", "PNG")
    b.save(STORE / "icon-concept-b.png", "PNG")
    c.save(STORE / "icon-concept-c.png", "PNG")

    a.resize((512, 512), Image.LANCZOS).save(STORE / "play-icon-512.png", "PNG")
    render_mask_preview(a).save(STORE / "launcher-mask-preview.png", "PNG")
    render_feature_graphic(a).save(STORE / "play-feature-graphic-1024x500.png", "PNG")


def main() -> None:
    generate_android_assets()
    generate_store_assets()
    print("Generated Android icon assets + store draft package for TKT-0234")


if __name__ == "__main__":
    main()
