#!/usr/bin/env python3
"""Writes loader/src/main/resources/legacyconnector/forward/1.20.1/mc-closed.txt.gz: the Minecraft 1.21.1 + NeoForge classes a mod cannot extend as they are
(final classes and records; enums and anonymous/synthetic classes left out). Forge 1.20.1 opened many of them with its own transformers, a translated mod
that extends one gets a `public-f` line in its jar's access transformer (ClassShape1201).

usage: gen_mc_closed.py <merged.jar> [out.txt.gz]
"""
import gzip, struct, sys, zipfile, pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
PREFIXES = ("net/minecraft/", "net/neoforged/neoforge/", "com/mojang/blaze3d/", "com/mojang/math/")


def header(data):
    pos = 8
    n = struct.unpack_from(">H", data, pos)[0]; pos += 2
    utf = {}; cls = {}
    i = 1
    while i < n:
        t = data[pos]
        if t == 1:
            ln = struct.unpack_from(">H", data, pos + 1)[0]
            utf[i] = data[pos + 3:pos + 3 + ln].decode("utf-8", "replace"); pos += 3 + ln
        elif t == 7: cls[i] = struct.unpack_from(">H", data, pos + 1)[0]; pos += 3
        elif t in (3, 4, 9, 10, 11, 12, 17, 18): pos += 5
        elif t in (5, 6): pos += 9; i += 1
        elif t in (8, 16, 19, 20): pos += 3
        elif t == 15: pos += 4
        else: raise ValueError(t)
        i += 1
    acc, this, sup = struct.unpack_from(">HHH", data, pos)
    return utf[cls[this]], (utf[cls[sup]] if sup else ""), acc


def main():
    src = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else "loader/src/main/resources/legacyconnector/forward/1.20.1/mc-closed.txt.gz"
    names = []
    with zipfile.ZipFile(src) as z:
        for nm in z.namelist():
            if not nm.endswith(".class") or not nm.startswith(PREFIXES): continue
            try: name, sup, acc = header(z.read(nm))
            except Exception: continue
            if acc & 0x200 or not acc & 0x10 or acc & 0x4000: continue  # interface, not final, enum
            if sup == "java/lang/Enum" or acc & 0x1000: continue
            tail = name.rsplit("$", 1)[-1]
            if tail.isdigit(): continue
            if not acc & 0x1: continue
            names.append(name)
    names.sort()
    with gzip.open(out, "wt", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(names) + "\n")
    print(len(names), "closed classes ->", out)


main()
