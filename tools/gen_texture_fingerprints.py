#!/usr/bin/env python3
"""Writes src/main/resources/legacyconnector/legacy/texture-fingerprints.txt, the table TextureModernizer uses to recognise
old vanilla art inside legacy mod textures. It holds fingerprints only, never pixels (Mojang's textures are not redistributed):

  E <hash> <modern> <WxH>  a mod texture whose pixels hash to <hash> is a copy of an old vanilla texture; the modern
                           vanilla texture textures/<modern>.png replaces it
  B <legacy> <modern> <fp> an old vanilla block texture that mods paint on (cobblestone, planks, smooth stone...): <fp> is
                           one hex byte per pixel (16x16, row by row) of a position-salted hash of the old pixel; pixels of a
                           mod texture whose fingerprint matches are the old base showing through and take the modern pixel

The hashes (shared with TextureModernizer.java): the exact hash is the first 16 hex digits of SHA-1 over width and height
(4-byte big endian) and every pixel as big-endian ARGB, fully transparent pixels as 0. The pixel fingerprint is
fmix32(argb ^ (index * 0x9E3779B9)) & 0xFF (murmur3's finaliser; index = y * 16 + x).

usage: tools/gen_texture_fingerprints.py <1.7.10 client jar> <1.12.2 client jar> <1.21.1 client jar>"""
import hashlib, io, os, re, sys, zipfile
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RENAMES = os.path.join(ROOT, "src", "main", "resources", "legacyconnector", "legacy", "1.7.10", "texture-renames.txt")
OUT = os.path.join(ROOT, "src", "main", "resources", "legacyconnector", "legacy", "texture-fingerprints.txt")
T = "assets/minecraft/textures/"


def pixels(img):
    """ARGB ints, fully transparent pixels as 0 (their colour is invisible and editors store anything there)."""
    img = img.convert("RGBA")
    b = img.tobytes()
    out = []
    for i in range(0, len(b), 4):
        r, g, bl, a = b[i], b[i + 1], b[i + 2], b[i + 3]
        out.append(0 if a == 0 else (a << 24) | (r << 16) | (g << 8) | bl)
    return out


def exact_hash(w, h, px):
    d = hashlib.sha1()
    d.update(w.to_bytes(4, "big") + h.to_bytes(4, "big"))
    d.update(b"".join(p.to_bytes(4, "big") for p in px))
    return d.hexdigest()[:16]


def fmix32(h):
    h &= 0xFFFFFFFF
    h ^= h >> 16
    h = (h * 0x85EBCA6B) & 0xFFFFFFFF
    h ^= h >> 13
    h = (h * 0xC2B2AE35) & 0xFFFFFFFF
    h ^= h >> 16
    return h


def fingerprint(argb, index):
    return fmix32(argb ^ ((index * 0x9E3779B9) & 0xFFFFFFFF)) & 0xFF


def textures(jar):
    out = {}
    with zipfile.ZipFile(jar) as z:
        names = set(z.namelist())
        for n in names:
            if n.startswith(T) and n.endswith(".png"):
                try:
                    img = Image.open(io.BytesIO(z.read(n)))
                    img.load()
                except Exception:
                    continue
                out[n[len(T):-4]] = (img.size, pixels(img), n + ".mcmeta" in names)
    return out


# 1.8-1.12 block textures 1.13 renamed (texture-renames.txt covers the 1.7.10 ones).
BLOCKS_1122 = {"red_sandstone_normal": "red_sandstone", "red_sandstone_carved": "chiseled_red_sandstone",
               "red_sandstone_smooth": "cut_red_sandstone", "red_nether_brick": "red_nether_bricks", "end_bricks": "end_stone_bricks",
               "slime": "slime_block", "prismarine_rough": "prismarine", "prismarine_dark": "dark_prismarine",
               "purpur_pillar": "purpur_pillar", "purpur_pillar_top": "purpur_pillar_top", "bone_block_side": "bone_block_side",
               "bone_block_top": "bone_block_top", "nether_wart_block": "nether_wart_block", "stone_slab_top": "smooth_stone",
               "stone_slab_side": "smooth_stone_slab_side", "sponge_wet": "wet_sponge", "ice_packed": "packed_ice",
               "observer_front": "observer_front", "grass_path_side": "dirt_path_side", "grass_path_top": "dirt_path_top"}

COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"]


def modern_name(legacy, renames):
    """textures/<legacy> (1.7.10 or 1.12.2 name) -> the modern texture's name, or None."""
    if legacy in renames:
        return renames[legacy]
    folder, _, name = legacy.partition("/")
    if folder == "blocks":
        m = re.fullmatch(r"(concrete|concrete_powder|glazed_terracotta)_(\w+)", name)
        if m and m[2] in COLORS:
            c = "light_gray" if m[2] == "silver" else m[2]
            return "block/" + {"concrete": f"{c}_concrete", "concrete_powder": f"{c}_concrete_powder",
                               "glazed_terracotta": f"{c}_glazed_terracotta"}[m[1]]
        m = re.fullmatch(r"door_(acacia|birch|dark_oak|jungle|spruce)_(lower|upper)", name)
        if m:
            return f"block/{m[1]}_door_{'bottom' if m[2] == 'lower' else 'top'}"
        return "block/" + BLOCKS_1122.get(name, name)
    if folder == "items":
        m = re.fullmatch(r"door_(acacia|birch|dark_oak|jungle|spruce)", name)
        if m:
            return f"item/{m[1]}_door"
        return "item/" + name
    if folder == "gui" and name.startswith("container/"):
        return legacy
    return None


def mean(px):
    o = [p for p in px if p >> 24 == 255]
    if not o:
        return None
    return tuple(sum((p >> s) & 255 for p in o) / len(o) for s in (16, 8, 0))


# Renames that point at a stand-in rather than the same picture (1.7.10's bed sides became an entity texture; the quiver and
# ruby items never shipped; redstone dust's sheets were split differently).
SUBSTITUTES = re.compile(r"blocks/bed_.*|items/bed|items/quiver|items/ruby|blocks/redstone_dust_.*")


def similar(legacy, old, new, gui):
    (ow, oh), opx, _ = old
    (nw, nh), npx, _ = new
    if SUBSTITUTES.fullmatch(legacy):
        return False, "stand-in"
    if (ow, oh) != (nw, nh):
        return False, "size"
    oo = {i for i, p in enumerate(opx) if p >> 24 >= 128}
    no = {i for i, p in enumerate(npx) if p >> 24 >= 128}
    if gui:
        # A GUI sheet is only exchangeable when every part the old one draws is still drawn: 1.20.2 moved the furnace's
        # flame and arrow (and others) to sprites, and a mod drawing them from its copy would draw nothing.
        return (oo <= no, "gui parts moved")
    if not oo or not no:
        return False, "empty"
    if len(oo) == len(opx) and len(no) < len(npx):
        return False, "no longer opaque"  # fast-graphics leaves: a solid block would show the holes' colour
    a, b = mean(opx), mean(npx)
    if a and b and sum((x - y) ** 2 for x, y in zip(a, b)) ** 0.5 > 110:
        return False, "colour"
    return True, ""


# Old vanilla block textures mods paint their own art on, and the modern texture of the same block.
BASES = ["stone", "cobblestone", "cobblestone_mossy", "stonebrick", "stonebrick_mossy", "stonebrick_cracked", "stonebrick_carved",
         "stone_slab_top", "stone_slab_side", "furnace_top", "furnace_side", "dispenser_front_vertical", "dropper_front_vertical",
         "piston_inner", "piston_bottom", "piston_side", "netherrack", "end_stone", "sand", "red_sand", "gravel", "dirt", "clay",
         "brick", "nether_brick", "obsidian", "bedrock", "soul_sand", "glowstone", "bookshelf", "crafting_table_top",
         "crafting_table_side", "crafting_table_front", "iron_block", "gold_block", "diamond_block", "emerald_block",
         "lapis_block", "redstone_block", "coal_block", "quartz_block_side", "quartz_block_top", "quartz_block_bottom",
         "sandstone_normal", "sandstone_top", "sandstone_bottom", "sandstone_carved", "sandstone_smooth", "hardened_clay",
         "ice_packed", "snow", "tnt_side", "tnt_top", "tnt_bottom", "noteblock", "jukebox_side", "jukebox_top", "melon_side",
         "pumpkin_side", "pumpkin_top", "hay_block_side", "hay_block_top", "mycelium_top", "dirt_podzol_top",
         "stone_andesite", "stone_diorite", "stone_granite", "stone_andesite_smooth", "stone_diorite_smooth", "stone_granite_smooth",
         "prismarine_bricks", "prismarine_dark", "end_bricks", "purpur_block", "red_nether_brick", "dirt_podzol_side"]
BASES += [f"planks_{w}" for w in ("oak", "spruce", "birch", "jungle", "acacia", "big_oak")]
BASES += [f"log_{w}{t}" for w in ("oak", "spruce", "birch", "jungle", "acacia", "big_oak") for t in ("", "_top")]
BASES += [f"wool_colored_{c}" for c in COLORS] + [f"hardened_clay_stained_{c}" for c in COLORS]


def main():
    old17, old12, new = (textures(j) for j in sys.argv[1:4])
    renames = {}
    for line in open(RENAMES, encoding="utf-8"):
        p = line.split()
        if len(p) == 2:
            renames[p[0]] = p[1]
    exact, rejected = {}, []
    for olds in (old17, old12):
        for legacy, tex in sorted(olds.items()):
            folder = legacy.split("/")[0]
            if folder not in ("blocks", "items", "gui") or tex[2]:
                continue  # animated sheets (water, fire, clock) keep their frames
            modern = modern_name(legacy, renames)
            if modern is None or modern not in new or new[modern][2]:
                continue
            if new[modern][1] == tex[1]:
                continue  # unchanged since then
            ok, why = similar(legacy, tex, new[modern], folder == "gui")
            if not ok:
                rejected.append(f"{legacy} -> {modern}: {why}")
                continue
            (w, h), px, _ = tex
            exact.setdefault(exact_hash(w, h, px), f"{modern} {w}x{h}")
    bases = []
    for legacy in BASES:
        tex = old17.get("blocks/" + legacy)
        if tex is None:
            tex = old12.get("blocks/" + legacy)
        if tex is None or tex[0] != (16, 16) or any(p >> 24 != 255 for p in tex[1]):
            continue
        modern = modern_name("blocks/" + legacy, renames)
        if modern is None or modern not in new or new[modern][2] or new[modern][0] != (16, 16) or new[modern][1] == tex[1]:
            continue
        fp = "".join("%02x" % fingerprint(p, i) for i, p in enumerate(tex[1]))
        bases.append(f"B {legacy} {modern} {fp}")
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write("# Generated by tools/gen_texture_fingerprints.py: fingerprints of old (1.7.10, 1.12.2) vanilla textures, no pixels.\n")
        for h, m in sorted(exact.items()):
            f.write(f"E {h} {m}\n")
        for b in bases:
            f.write(b + "\n")
    print(f"{len(exact)} exact, {len(bases)} bases; rejected {len(rejected)}:", file=sys.stderr)
    for r in rejected:
        print("  " + r, file=sys.stderr)


if __name__ == "__main__":
    main()
