#!/usr/bin/env python3
"""Reads the static initializers of 1.7.10 Blocks/Items to recover field -> registry name pairs
(e.g. Items.potionitem -> "potion"). Writes build/devtools/legacy-1.7.10-registry-names.txt."""
import os, re, struct, zipfile

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "build", "devtools")
cmap, fmap = {}, {}
for line in zipfile.ZipFile(os.path.join(ROOT, "mcp", "srg1710.zip")).read("joined.srg").decode().splitlines():
    p = line.split()
    if p[0] == "CL:": cmap[p[1]] = p[2]
    elif p[0] == "FD:": fmap[p[1]] = p[2].rsplit("/", 1)[1]
sig_names = {}
for line in open(os.path.join(ROOT, "legacy-1.7.10.sig")):
    p = line.split()
    if p[0] == "F": sig_names[p[4]] = p[1]

def clinit_pairs(data):
    pos = 8
    count = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2
    cp = [None] * count; i = 1
    while i < count:
        tag = data[pos]; pos += 1
        if tag == 1:
            ln = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2; cp[i] = ("utf", data[pos:pos + ln].decode()); pos += ln
        elif tag in (3, 4): pos += 4
        elif tag in (5, 6): pos += 8; i += 1
        elif tag in (7, 8, 16, 19, 20): cp[i] = (tag, struct.unpack(">H", data[pos:pos + 2])[0]); pos += 2
        elif tag in (9, 10, 11, 12, 17, 18): cp[i] = (tag, struct.unpack(">HH", data[pos:pos + 4])); pos += 4
        elif tag == 15: pos += 3
        i += 1
    utf = lambda k: cp[k][1]
    pos += 6
    n = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2 + 2 * n
    def skip_members(pos):
        n = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2
        for _ in range(n):
            pos += 6; ac = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2
            for _ in range(ac): pos += 6 + struct.unpack(">I", data[pos + 2:pos + 6])[0]
        return pos
    pos = skip_members(pos)
    n = struct.unpack(">H", data[pos:pos + 2])[0]; pos += 2
    for _ in range(n):
        acc, ni, di, ac = struct.unpack(">HHHH", data[pos:pos + 8]); pos += 8
        for _ in range(ac):
            an = utf(struct.unpack(">H", data[pos:pos + 2])[0]); ln = struct.unpack(">I", data[pos + 2:pos + 6])[0]
            body = data[pos + 6:pos + 6 + ln]; pos += 6 + ln
            if utf(ni) == "<clinit>" and an == "Code":
                code = body[8:8 + struct.unpack(">I", body[4:8])[0]]
                yield from scan(code, cp, utf)

def scan(code, cp, utf):
    last = None; i = 0
    lens = {0x12: 2, 0x13: 3, 0xb2: 3, 0xb3: 3, 0xb6: 3, 0xb7: 3, 0xb8: 3, 0xb9: 5, 0xbb: 3, 0xc0: 3, 0x10: 2, 0x11: 3, 0xbd: 3}
    while i < len(code):
        op = code[i]
        if op in (0x12, 0x13):
            idx = code[i + 1] if op == 0x12 else struct.unpack(">H", code[i + 1:i + 3])[0]
            e = cp[idx]
            if e and e[0] == 8: last = utf(e[1])
        elif op == 0xb3:
            idx = struct.unpack(">H", code[i + 1:i + 3])[0]
            cls_i, nat_i = cp[idx][1]
            nat = cp[nat_i][1]
            yield utf(nat[0]), last
            last = None
        i += lens.get(op, 1)

out = []
with zipfile.ZipFile(os.path.join(ROOT, "mojang", "client-1.7.10.jar")) as jar:
    inv = {v: k for k, v in cmap.items()}
    for owner in ("net/minecraft/init/Blocks", "net/minecraft/init/Items"):
        obf = inv[owner]
        for field, name in clinit_pairs(jar.read(obf + ".class")):
            srg = fmap.get(obf + "/" + field, field)
            out.append("%s %s %s" % (owner, sig_names.get(srg, srg), name))
open(os.path.join(ROOT, "legacy-1.7.10-registry-names.txt"), "w").write("\n".join(out) + "\n")
print(len(out), "pairs")
