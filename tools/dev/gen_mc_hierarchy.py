#!/usr/bin/env python3
"""Writes loader/src/main/resources/legacyconnector/forward/1.20.1/mc-hierarchy.tsv.gz: name TAB super TAB interfaces (comma separated) of the
Minecraft 1.21.1 + NeoForge classes (build/moddev/artifacts/neoforge-*-merged.jar), so the 1.20.1 migration (Mc1201Migration) can tell whether a mod
class below e.g. BlockEntity or a call on a ServerPlayer reaches a method declared on Player/LivingEntity without having the game jars at hand.

usage: gen_mc_hierarchy.py <merged.jar> [out.tsv.gz]
"""
import gzip, struct, sys, zipfile

PREFIXES = ("net/minecraft/", "net/neoforged/neoforge/", "com/mojang/blaze3d/", "com/mojang/math/")


def parse(data):
    """Returns (name, super, [interfaces]) of a class file."""
    pos = 8
    n = struct.unpack_from(">H", data, pos)[0]
    pos += 2
    utf = {}
    cls = {}
    i = 1
    while i < n:
        tag = data[pos]
        if tag == 1:
            ln = struct.unpack_from(">H", data, pos + 1)[0]
            utf[i] = data[pos + 3:pos + 3 + ln].decode("utf-8", "replace")
            pos += 3 + ln
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            pos += 5
        elif tag in (5, 6):
            pos += 9
            i += 1
        elif tag in (7, 8, 16, 19, 20):
            if tag == 7:
                cls[i] = struct.unpack_from(">H", data, pos + 1)[0]
            pos += 3
        elif tag == 15:
            pos += 4
        else:
            raise ValueError(tag)
        i += 1
    pos += 2  # access
    this = utf[cls[struct.unpack_from(">H", data, pos)[0]]]
    sup = struct.unpack_from(">H", data, pos + 2)[0]
    sup = utf[cls[sup]] if sup else ""
    ni = struct.unpack_from(">H", data, pos + 4)[0]
    itfs = [utf[cls[struct.unpack_from(">H", data, pos + 6 + 2 * k)[0]]] for k in range(ni)]
    return this, sup, itfs


def main():
    jar = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else "loader/src/main/resources/legacyconnector/forward/1.20.1/mc-hierarchy.tsv.gz"
    lines = []
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if not name.endswith(".class") or not name.startswith(PREFIXES):
                continue
            try:
                this, sup, itfs = parse(z.read(name))
            except Exception:
                continue
            lines.append(f"{this}\t{sup}\t{','.join(itfs)}")
    lines.sort()
    with gzip.GzipFile(out, "wb", mtime=0) as f:
        f.write(("\n".join(lines) + "\n").encode())
    print(len(lines), "classes ->", out)


main()
