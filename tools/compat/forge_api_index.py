#!/usr/bin/env python3
"""EXPERIMENTAL (1.18.2 / 1.16.5 scoping, docs/wiki/Legacy-1.18.2-Estimate.md): index of the public API of Forge jars, to diff the Forge API of neighbouring versions.

usage: forge_api_index.py <out.tsv> <jar> [<jar> ...] [--rename <mc-classes.tsv>]

Reads the class files of net/minecraftforge/** (and cpw/**) of the jars (Forge universal, fmlcore, javafmllanguage; downloaded at development time, never committed)
and writes one row per class and per public/protected member:
    class TAB <name> TAB <super> TAB <interfaces,comma>
    m     TAB <owner> TAB <name> TAB <descriptor>
    f     TAB <owner> TAB <name> TAB <descriptor>
--rename applies class rows (old TAB new, e.g. 1.16.5 MCP -> Mojang names) to descriptors, supers and interfaces, so versions with different class names compare.
Pure Python class file reader (constant pool, fields, methods); nothing of the jars is executed.
"""
import struct, sys, zipfile


def read_class(data):
    p = 8
    n = struct.unpack(">H", data[p:p + 2])[0]; p += 2
    cp = [None] * n
    i = 1
    while i < n:
        tag = data[p]; p += 1
        if tag == 1:
            l = struct.unpack(">H", data[p:p + 2])[0]; p += 2
            cp[i] = data[p:p + l].decode("utf-8", "replace"); p += l
        elif tag in (3, 4):
            p += 4
        elif tag in (5, 6):
            p += 8; i += 1
        elif tag in (7, 8, 16, 19, 20):
            cp[i] = ("ref", struct.unpack(">H", data[p:p + 2])[0]); p += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            p += 4
        elif tag == 15:
            p += 3
        else:
            raise ValueError("tag %d" % tag)
        i += 1
    access, this, sup = struct.unpack(">HHH", data[p:p + 6]); p += 6
    ni = struct.unpack(">H", data[p:p + 2])[0]; p += 2
    ifs = []
    for _ in range(ni):
        ifs.append(cp[cp[struct.unpack(">H", data[p:p + 2])[0]][1]]); p += 2

    def members():
        nonlocal p
        out = []
        c = struct.unpack(">H", data[p:p + 2])[0]; p += 2
        for _ in range(c):
            acc, name, desc, na = struct.unpack(">HHHH", data[p:p + 8]); p += 8
            for _ in range(na):
                ln = struct.unpack(">I", data[p + 2:p + 6])[0]; p += 6 + ln
            out.append((acc, cp[name], cp[desc]))
        return out
    fields = members()
    methods = members()
    return access, cp[cp[this][1]], (cp[cp[sup][1]] if sup else ""), ifs, fields, methods


def main():
    args = sys.argv[1:]
    rename = {}
    if "--rename" in args:
        i = args.index("--rename")
        for line in open(args[i + 1], encoding="utf-8"):
            t = line.rstrip("\n").split("\t")
            if len(t) == 2 and not line.startswith("#"):
                rename[t[0]] = t[1]
        del args[i:i + 2]
    out, jars = args[0], args[1:]

    def r(s):
        if not rename:
            return s
        import re
        return re.sub(r"L([^;]+);", lambda m: "L" + rename.get(m.group(1), m.group(1)) + ";", s)
    with open(out, "w", encoding="utf-8") as f:
        for j in jars:
            z = zipfile.ZipFile(j)
            for name in z.namelist():
                if not name.endswith(".class") or not name.startswith(("net/minecraftforge/", "cpw/")) or name.startswith("META-INF"):
                    continue
                try:
                    acc, this, sup, ifs, fields, methods = read_class(z.read(name))
                except Exception:
                    continue
                f.write("class\t%s\t%s\t%s\n" % (rename.get(this, this), rename.get(sup, sup), ",".join(rename.get(i, i) for i in ifs)))
                for a, n, d in fields:
                    if a & 0x5:   # public or protected
                        f.write("f\t%s\t%s\t%s\n" % (rename.get(this, this), n, r(d)))
                for a, n, d in methods:
                    if a & 0x5:
                        f.write("m\t%s\t%s\t%s\n" % (rename.get(this, this), n, r(d)))


if __name__ == "__main__":
    main()
