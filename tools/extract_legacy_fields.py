#!/usr/bin/env python3
"""Writes loader/src/main/resources/mccompat/legacy/<ver>/fields.txt from the legacy signature dump:
one line per field: `srgName owner mcpName descriptor static(0/1)` (srgName is the plain name for fields Forge
added, which have no SRG id).
usage: tools/extract_legacy_fields.py [1.7.10]   (needs build/devtools/legacy-<ver>.sig)"""
import os, sys
ver = sys.argv[1] if len(sys.argv) > 1 else "1.7.10"
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sig = os.path.join(root, "build", "devtools", f"legacy-{ver}.sig")
out_dir = os.path.join(root, "loader", "src", "main", "resources", "mccompat", "legacy", ver)
owner = None
rows = []
with open(sig) as f:
    for line in f:
        p = line.split()
        if not p:
            continue
        if p[0] == "C":
            owner = p[1]
        elif p[0] == "F" and len(p) >= 4 and not p[1].startswith("$") and not p[1].startswith("this$"):
            static = "1" if int(p[3]) & 0x0008 else "0"
            srg = p[4] if len(p) >= 5 else p[1]
            rows.append(f"{srg} {owner} {p[1]} {p[2]} {static}")
with open(os.path.join(out_dir, "fields.txt"), "w") as f:
    f.write("\n".join(sorted(rows)) + "\n")
print(f"{len(rows)} fields")
