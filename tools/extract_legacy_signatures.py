#!/usr/bin/env python3
"""Builds build/devtools/legacy-<ver>.sig: every vanilla class/field/method of a legacy Minecraft version with
deobfuscated (MCP) names and descriptors. Reference data for the shim audit; never shipped.

usage: tools/extract_legacy_signatures.py <1.7.10|1.12.2>
Requires tools/fetch_devtools.sh data plus build/devtools/mojang/client-<ver>.jar.
"""
import csv, io, os, re, struct, sys, zipfile

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "build", "devtools")
ver = sys.argv[1]


def parse_class(data):
    """Minimal JVM class file reader: returns (name, super, interfaces, access, fields, methods)."""
    pos = 8
    count = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2
    cp = [None] * count
    i = 1
    while i < count:
        tag = data[pos]; pos += 1
        if tag == 1:
            ln = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2
            cp[i] = data[pos:pos + ln].decode("utf-8", "replace"); pos += ln
        elif tag in (3, 4): pos += 4
        elif tag in (5, 6): pos += 8; i += 1
        elif tag == 7 or tag == 8 or tag == 16 or tag == 19 or tag == 20:
            cp[i] = ("ref", struct.unpack(">H", data[pos:pos + 2])[0]); pos += 2
        elif tag in (9, 10, 11, 12, 17, 18): pos += 4
        elif tag == 15: pos += 3
        else: raise ValueError("bad tag %d" % tag)
        i += 1
    cls = lambda idx: cp[cp[idx][1]] if idx else None
    access, this, sup = struct.unpack(">HHH", data[pos:pos + 6]); pos += 6
    n = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2
    ifaces = [cls(struct.unpack(">H", data[pos + 2 * k:pos + 2 * k + 2])[0]) for k in range(n)]; pos += 2 * n

    def members(pos):
        out = []
        n = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2
        for _ in range(n):
            acc, ni, di, ac = struct.unpack(">HHHH", data[pos:pos + 8]); pos += 8
            for _ in range(ac):
                ln = struct.unpack(">I", data[pos + 2:pos + 6])[0]; pos += 6 + ln
            out.append((cp[ni], cp[di], acc))
        return out, pos

    fields, pos = members(pos)
    methods, pos = members(pos)
    return cls(this), cls(sup), ifaces, access, fields, methods


names = {}


def load_csv(zp, member):
    with zipfile.ZipFile(os.path.join(ROOT, "mcp", zp)) as z:
        for row in csv.reader(io.TextIOWrapper(z.open(member), "utf-8")):
            if row and row[0].startswith(("func_", "field_")):
                names[row[0]] = row[1]


cmap, fmap, mmap = {}, {}, {}
if ver == "1.7.10":
    load_csv("mcp_stable-12-1.7.10.zip", "methods.csv"); load_csv("mcp_stable-12-1.7.10.zip", "fields.csv")
    load_csv("forge1710-userdev.jar", "conf/methods.csv"); load_csv("forge1710-userdev.jar", "conf/fields.csv")
    for line in zipfile.ZipFile(os.path.join(ROOT, "mcp", "srg1710.zip")).read("joined.srg").decode().splitlines():
        p = line.split()
        if p[0] == "CL:": cmap[p[1]] = p[2]
        elif p[0] == "FD:": fmap[p[1]] = p[2].rsplit("/", 1)[1]
        elif p[0] == "MD:": mmap[p[1] + p[2]] = p[3].rsplit("/", 1)[1]
else:
    load_csv("mcp_stable-39-1.12.zip", "methods.csv"); load_csv("mcp_stable-39-1.12.zip", "fields.csv")
    cur = None
    for line in zipfile.ZipFile(os.path.join(ROOT, "mcp", "srg1122.zip")).read("config/joined.tsrg").decode().splitlines():
        if not line.startswith("\t"):
            a = line.split(); cmap[a[0]] = a[1]; cur = a[0]
        else:
            a = line.strip().split()
            if len(a) == 2: fmap[cur + "/" + a[0]] = a[1]
            else: mmap[cur + "/" + a[0] + a[1]] = a[2]

remap = lambda d: re.sub(r"L([^;]+);", lambda m: "L" + cmap.get(m.group(1), m.group(1)) + ";", d)
out = []
with zipfile.ZipFile(os.path.join(ROOT, "mojang", "client-%s.jar" % ver)) as jar:
    for entry in jar.namelist():
        if not entry.endswith(".class"): continue
        name, sup, ifaces, acc, fields, methods = parse_class(jar.read(entry))
        if not (name in cmap or name.startswith("net/minecraft/")): continue
        out.append("C %s %s %s %d" % (cmap.get(name, name), cmap.get(sup, sup) if sup else "-",
                                       ",".join(cmap.get(i, i) for i in ifaces) or "-", acc))
        for fn, fd, fa in fields:
            srg = fmap.get(name + "/" + fn, fn)
            out.append("F %s %s %d %s" % (names.get(srg, srg), remap(fd), fa, srg))
        for mn, md, ma in methods:
            srg = mmap.get(name + "/" + mn + md, mn)
            out.append("M %s %s %d %s" % (names.get(srg, srg), remap(md), ma, srg))
dst = os.path.join(ROOT, "legacy-%s.sig" % ver)
open(dst, "w").write("\n".join(out) + "\n")
print("wrote", dst, sum(1 for l in out if l.startswith("C ")), "classes")
