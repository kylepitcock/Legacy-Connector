#!/usr/bin/env python3
"""Link rate of a gap report (detail.tsv: jar, kind, symbol) split like docs/wiki/Legacy-1.19.2-Estimate.md: a jar 'links' when it has no unresolved
Minecraft/Forge symbol (what is left references other mods or libraries not in the sample); 'fully' when it has no unresolved symbol at all.

usage: link_rate_1192.py <detail.tsv> [more detail.tsv ...]"""
import sys

MC = ("net/minecraft/", "com/mojang/", "net/minecraftforge/", "net/neoforged/", "org/spongepowered/", "cpw/mods/")


def rate(path):
    jars, bad, anyunres = set(), set(), set()
    for l in open(path, encoding="utf-8"):
        jar, kind, sym = l.rstrip("\n").split("\t", 2)
        jars.add(jar)
        if kind == "refs":
            continue
        anyunres.add(jar)
        if sym.lstrip("L[").startswith(MC):
            bad.add(jar)
    return len(jars), len(jars) - len(anyunres), len(jars) - len(bad)


for p in sys.argv[1:]:
    n, full, links = rate(p)
    print(f"{p}: {n} jars with code; link fully {full}; link except for other mods/libraries {links - full} (total {links}, {100 * links / n:.1f}%)")
