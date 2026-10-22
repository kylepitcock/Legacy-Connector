#!/usr/bin/env python3
"""Builds loader/src/main/resources/legacyconnector/legacy/<ver>/methods.txt from build/devtools/legacy-<ver>.sig:
one "owner name desc flags" line per non-private, non-synthetic method of every vanilla class. The synthesizer
adds these to the classes it generates for vanilla types missing from the shim, so code that reflects over a
vanilla class (integrity checks, "does this method exist") finds what vanilla had.

usage: tools/extract_legacy_methods.py <1.7.10|1.12.2>   (run tools/extract_legacy_signatures.py first)
"""
import os, sys

ver = sys.argv[1]
root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
sig = os.path.join(root, "build", "devtools", "legacy-%s.sig" % ver)
out = os.path.join(root, "loader", "src", "main", "resources", "legacyconnector", "legacy", ver, "methods.txt")
owner = None
lines = []
for line in open(sig, encoding="utf-8"):
    parts = line.rstrip("\n").split(" ")
    if parts[0] == "C":
        owner = parts[1]
    elif parts[0] == "M" and owner:
        name, desc, flags = parts[1], parts[2], int(parts[3])
        if name in ("<init>", "<clinit>") or flags & 0x2 or flags & 0x1000 or flags & 0x40:
            continue
        lines.append("%s %s %s %d" % (owner, name, desc, flags))
with open(out, "w", encoding="utf-8") as f:
    f.write("\n".join(lines) + "\n")
print("wrote", out, len(lines), "methods")
