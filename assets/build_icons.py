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


def lighten_greys(img, floor=110, target=225):
    """Pushes mid-grey fill up to light grey so grey art (the rhino face) does not vanish against
    the grey opaque chatbox. Coloured pixels are untouched."""
    img = img.copy()
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a and abs(r - g) < 20 and abs(g - b) < 20 and r > floor:
                px[x, y] = (target, target, target, a)
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

    # chat icon: drop the source outline, lift greys, crop, scale to CONTENT tall, re-outline at pixel scale
    img = lighten_greys(strip_outline(img))
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
    print(f"{done} icon(s) written to {OUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
