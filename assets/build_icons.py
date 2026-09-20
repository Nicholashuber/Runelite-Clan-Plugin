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


def shrink(img, size, crop):
    img = img.convert("RGBA")
    if crop:
        box = img.getchannel("A").getbbox()
        if box:
            img = img.crop(box)
        # pad to square so the aspect ratio survives
        w, h = img.size
        side = max(w, h)
        square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
        square.paste(img, ((side - w) // 2, (side - h) // 2))
        img = square
    img = img.resize((size, size), Image.LANCZOS)
    if crop:
        px = img.load()
        for y in range(size):
            for x in range(size):
                r, g, b, a = px[x, y]
                px[x, y] = (r, g, b, 255 if a >= 96 else 0)
    return img


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
        print(f"{name}: -> {size}x{size}")
        done += 1
    print(f"{done} icon(s) written to {OUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
