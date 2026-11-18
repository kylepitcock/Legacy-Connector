"""Draws the vertical slice (z horizontal, y vertical) of an ore probe result (tools/dev/ore-probe.sh) as a coloured PNG.
usage: py -3 tools/dev/render_ore_slice.py <ore-probe.json> <out.png> [<second.json> <label A> <label B>]
With two result files the images are stacked (before above, after below). A line marks y=0 (the old world floor) and y=-64.
"""
import colorsys
import json
import sys

from PIL import Image, ImageDraw

SCALE = 6
YMIN, YMAX = -64, 130


def colour(i, n):
    h = (i * 0.61803398875) % 1.0
    r, g, b = colorsys.hsv_to_rgb(h, 0.85, 0.95)
    return int(r * 255), int(g * 255), int(b * 255)


def render(path, title):
    data = json.load(open(path))
    zs = [s[0] for s in data["slice"]] or [0]
    zmin, zmax = -80, 95
    w, h = (zmax - zmin + 1) * SCALE, (YMAX - YMIN + 1) * SCALE
    img = Image.new("RGB", (w, h + 16), (24, 24, 28))
    d = ImageDraw.Draw(img)
    n = len(data["names"])
    for z, y, t in data["slice"]:
        if y > YMAX or y < YMIN or z < zmin or z > zmax:
            continue
        px, py = (z - zmin) * SCALE, (YMAX - y) * SCALE + 16
        d.rectangle([px, py, px + SCALE - 1, py + SCALE - 1], fill=colour(t, n))
    for yy, col in ((0, (255, 255, 255)), (-64, (120, 120, 120))):
        py = (YMAX - yy) * SCALE + 16 + SCALE // 2
        d.line([0, py, w, py], fill=col, width=1)
    d.text((4, 2), "%s: ore blocks, slab x=0..7, z %d..%d, y %d..%d (white line y=0)" % (title, zmin, zmax, YMIN, YMAX), fill=(255, 255, 255))
    return img


def main():
    a = sys.argv
    imgs = [render(a[1], a[4] if len(a) > 4 else "result")]
    if len(a) > 3:
        imgs.append(render(a[3], a[5] if len(a) > 5 else "second"))
    out = Image.new("RGB", (imgs[0].width, sum(i.height for i in imgs)))
    y = 0
    for i in imgs:
        out.paste(i, (0, y))
        y += i.height
    out.save(a[2])
    print("wrote", a[2])


main()
