#!/usr/bin/env python3
"""Lists hand-written shim classes whose legacy supertypes are missing from their shim hierarchy.

A missing *class* supertype (e.g. RegistryNamespaced without RegistrySimple) breaks bytecode verification wherever a
mod treats the shim class as that supertype; missing interfaces only break instanceof checks.

usage: tools/audit_shim_hierarchy.py [1.7.10]   (after ./gradlew generateShimIndex)
"""
import os, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ver = sys.argv[1] if len(sys.argv) > 1 else "1.7.10"
prefix = "mccompat/v" + ver.replace(".", "_") + "/"
legacy = {}
for line in open(os.path.join(ROOT, "loader/src/main/resources/mccompat/legacy", ver, "hierarchy.txt")):
    n, s, i, a = line.split()
    legacy[n] = ([] if s == "-" else [s]) + ([] if i == "-" else i.split(","))
shim = {}
for line in open(os.path.join(ROOT, "build/generated/shimIndex/mccompat/shim-index.txt")):
    p = line.split()
    if p[0] == "C" and p[1].startswith(prefix):
        shim[p[1][len(prefix):]] = [x[len(prefix):] if x.startswith(prefix) else x for x in ([p[2]] + ([] if p[3] == "-" else p[3].split(",")))]


def closure(n, graph, seen):
    for x in graph.get(n, []):
        if x not in seen:
            seen.add(x)
            closure(x, graph, seen)
    return seen


for name in sorted(shim):
    if name not in legacy:
        continue
    missing = sorted(a for a in closure(name, legacy, set()) - closure(name, shim, set()) if not a.startswith("java/"))
    if missing:
        kinds = ["class " + m if legacy.get(m) and not m.split("/")[-1].startswith("I") else m for m in missing]
        print(name, "->", ", ".join(kinds))
