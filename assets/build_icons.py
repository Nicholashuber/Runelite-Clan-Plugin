"""Shrinks the generated 1024x1024 art in assets/icons/src/ down to the sizes the plugin uses.

Usage (from the repo root):  python assets/build_icons.py

- logo.png        -> 128x128
- panel_icon.png  -> 16x16
- everything else -> 11x11 chat icon

Chat icons get cropped to their opaque bounding box first so the subject fills the tiny canvas,
then a light alpha threshold so the game does not draw a fuzzy halo around them.
"""
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "assets", "icons", "src")
OUT = os.path.join(ROOT, "src", "main", "resources", "com", "corclan")

SIZES = {"logo.png": 128, "panel_icon.png": 16}
CHAT_ICON = 11

# Brand images derived from one source when no dedicated file exists in assets/icons/src/.
BRAND_SOURCE = "rank_staff.png"
BRAND_OUTPUTS = {
    "logo.png": (128, OUT),       # panel header
    "panel_icon.png": (16, OUT),  # RuneLite sidebar button
    "icon.png": (48, ROOT),       # Plugin Hub listing icon (max 48x72, repo root)
}


def pixel_square(img, size):
    """Crisp small square icon: same treatment as chat icons (toned greys, hard alpha, 1px outline)."""
    img = tone_greys(strip_outline(img.convert("RGBA")))
    box = img.getchannel("A").getbbox()
    if box:
        img = img.crop(box)
    w, h = img.size
    side = max(w, h)
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.paste(img, ((side - w) // 2, (side - h) // 2))
    inner = size - 2
    img = square.resize((inner, inner), Image.LANCZOS)
    px = img.load()
    for y in range(inner):
        for x in range(inner):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255 if a >= ALPHA_THRESHOLD else 0)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.paste(img, (1, 1))
    return add_outline(canvas)


def brand(img, size):
    """Crops to the art, pads to square and scales. Keeps the source outline (it reads fine above 11px)."""
    img = img.convert("RGBA")
    box = img.getchannel("A").getbbox()
    if box:
        img = img.crop(box)
    w, h = img.size
    side = max(w, h)
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.paste(img, ((side - w) // 2, (side - h) // 2))
    return square.resize((size, size), Image.LANCZOS)


OUTLINE_THRESHOLD = 60  # pixels darker than this on every channel are treated as outline


def strip_outline(img):
    """Makes near-black outline pixels transparent. At 11x11 a thick outline eats a third of the
    pixels and muddies the shape; the colored fill alone reads far better."""
    img = img.copy()
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a > 0 and max(r, g, b) < OUTLINE_THRESHOLD:
                px[x, y] = (0, 0, 0, 0)
    return img


MAX_CHAT_ICON_WIDTH = 22  # chat icons must be 11 tall but may be wider (wordmarks like "GZ")
CHAT_ICON_CONTENT = 10    # art height inside the 11px canvas; the rest is the outline
OUTLINE_COLOR = (33, 33, 33, 255)  # same near-black RuneLite outlines its own rank icons with
ALPHA_THRESHOLD = 110


GREY_TONE = 0.8  # grey fill is darkened slightly so the rhino head reads on the grey opaque chatbox


def tone_greys(img, factor=GREY_TONE, floor=60):
    """Scales grey-ish fill (the rhino head) by `factor`. Coloured pixels are untouched."""
    img = img.copy()
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a and abs(r - g) < 20 and abs(g - b) < 20 and r > floor:
                px[x, y] = (int(r * factor), int(g * factor), int(b * factor), a)
    return img


def add_outline(img):
    """1px dark outline around every opaque pixel, inside the existing canvas."""
    w, h = img.size
    src = img.load()
    out = img.copy()
    px = out.load()
    for y in range(h):
        for x in range(w):
            if src[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3]:
                    px[x, y] = OUTLINE_COLOR
                    break
    return out


def shrink(img, size, crop):
    img = img.convert("RGBA")
    if not crop:
        return img.resize((size, size), Image.LANCZOS)

    # chat icon: drop the source outline, tone greys, crop, scale to CONTENT tall, re-outline at pixel scale
    img = tone_greys(strip_outline(img))
    box = img.getchannel("A").getbbox()
    if box:
        img = img.crop(box)
    w, h = img.size
    content_w = max(CHAT_ICON_CONTENT, min(MAX_CHAT_ICON_WIDTH - 2, round(w * CHAT_ICON_CONTENT / h)))
    if w < h:
        square = Image.new("RGBA", (h, h), (0, 0, 0, 0))
        square.paste(img, ((h - w) // 2, 0))
        img = square
        content_w = CHAT_ICON_CONTENT
    img = img.resize((content_w, CHAT_ICON_CONTENT), Image.LANCZOS)
    px = img.load()
    for y in range(CHAT_ICON_CONTENT):
        for x in range(content_w):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255 if a >= ALPHA_THRESHOLD else 0)
    canvas = Image.new("RGBA", (content_w + 2, size), (0, 0, 0, 0))
    canvas.paste(img, (1, 1))
    return add_outline(canvas)


def main():
    if not os.path.isdir(SRC):
        print(f"Put the generated PNGs in {SRC}")
        return 1
    done = 0
    for name in sorted(os.listdir(SRC)):
        if not name.lower().endswith(".png"):
            continue
        src = os.path.join(SRC, name)
        size = SIZES.get(name, CHAT_ICON)
        img = shrink(Image.open(src), size, crop=name not in SIZES)
        img.save(os.path.join(OUT, name))
        print(f"{name}: -> {img.size[0]}x{img.size[1]}")
        done += 1

    brand_src = os.path.join(SRC, BRAND_SOURCE)
    if os.path.exists(brand_src):
        for name, (size, out_dir) in BRAND_OUTPUTS.items():
            if os.path.exists(os.path.join(SRC, name)):
                continue  # a dedicated source was supplied and already processed above
            make = pixel_square if size <= 16 else brand
            make(Image.open(brand_src), size).save(os.path.join(out_dir, name))
            print(f"{name}: -> {size}x{size} (from {BRAND_SOURCE}) in {os.path.relpath(out_dir, ROOT)}")
            done += 1
    print(f"{done} image(s) written")
    return 0


if __name__ == "__main__":
    sys.exit(main())
