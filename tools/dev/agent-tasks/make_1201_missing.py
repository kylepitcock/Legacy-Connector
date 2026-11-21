#!/usr/bin/env python3
"""Writes tools/dev/agent-tasks/1.20.1-missing-references.md from an atm9_run.py output folder.

usage: make_1201_missing.py <atm9-out-dir> [--top 120] > tools/dev/agent-tasks/1.20.1-missing-references.md

<atm9-out-dir> holds detail.tsv and summary.json of `tools/compat/atm9_run.py <ATM 9 mods> --no-native --out <dir>` (with GAP1201_LIBS = an
ATM 10 mods folder, the setup of the gap rounds). Fabric jars (intermediary `class_N`/`method_N` names) and unmapped SRG names are left out of the
Minecraft/Forge tables: they are not translated at all. Kept by hand at the end: the "Known stubs" section of the previous file.
"""
import collections, datetime, json, pathlib, re, sys

FABRIC = re.compile(r"(class|method|field)_\d+|\b[mf]_\d+_")


def area(sym):
    o = re.split(r"[.(:]", sym, 1)[0].replace("/", ".")
    if o.startswith(("net.minecraftforge.", "net.neoforged.")):
        return "Forge/NeoForge API"
    if o.startswith(("net.minecraft.client.", "com.mojang.blaze3d.", "com.mojang.math.")):
        return "Minecraft client"
    if o.startswith(("net.minecraft.", "com.mojang.")):
        return "Minecraft common"
    if o.startswith(("java.", "javax.", "sun.", "jdk.")):
        return "JDK"
    if o.startswith(("org.spongepowered.", "org.objectweb.")):
        return "Mixin/ASM"
    return "Library or other mod"


def main():
    a = sys.argv[1:]
    top = 120
    if "--top" in a:
        i = a.index("--top"); top = int(a[i + 1]); del a[i:i + 2]
    out = pathlib.Path(a[0])
    summary = json.loads((out / "summary.json").read_text(encoding="utf-8"))
    miss = collections.defaultdict(set)
    for line in (out / "detail.tsv").read_text(encoding="utf-8").splitlines():
        jar, k, sym = line.split("\t", 2)
        if k != "refs":
            miss[jar].add((k, sym))
    pairs = sum(len(v) for v in miss.values())
    distinct = len({s for v in miss.values() for s in v})
    per_area = collections.defaultdict(lambda: [set(), 0])
    counts = collections.Counter()
    pkg_jars = collections.defaultdict(set)
    pkg_syms = collections.defaultdict(set)
    for jar, syms in miss.items():
        for k, s in syms:
            ar = area(s)
            per_area[ar][0].add(s); per_area[ar][1] += 1
            if FABRIC.search(s):
                continue
            if ar == "Library or other mod":
                pkg = ".".join(re.split(r"[.(:$]", s.replace("/", "."), 1)[0].split(".")[:4])
                pkg_jars[pkg].add(jar); pkg_syms[pkg].add(s)
            else:
                counts[(ar, k, s)] += 1
    p = print
    p(f"# 1.20.1: missing references (ATM 9 run, {summary.get('jars')} jars, {datetime.date.today()})")
    p()
    p(f"Source: `detail.tsv` of `tools/compat/atm9_run.py` with `--no-native` and `GAP1201_LIBS` = the ATM 10 mods folder ({distinct} distinct unresolved "
      f"symbols, {pairs} jar/symbol pairs; {summary.get('links')} jars link). \"jars\" = how many jars reference it. Regenerate: run `atm9_run.py "
      f"\"<ATM 9 mods>\" --no-native --out <dir>`, then `python tools/dev/agent-tasks/make_1201_missing.py <dir> > tools/dev/agent-tasks/1.20.1-missing-references.md`. "
      "Fabric jars (intermediary names) are left out of the tables below; several symbols of one jar are often one missing class.")
    p()
    p("## Totals per area")
    p()
    p("| Area | distinct symbols | jar/symbol pairs |")
    p("|---|---|---|")
    for ar, (syms, n) in sorted(per_area.items(), key=lambda e: -e[1][1]):
        p(f"| {ar} | {len(syms)} | {n} |")
    for title in ("Minecraft common", "Minecraft client", "Forge/NeoForge API"):
        rows = [(n, k, s) for (ar, k, s), n in counts.items() if ar == title]
        rows.sort(key=lambda r: (-r[0], r[2]))
        p()
        p(f"## {title}: top {top} by jars affected")
        p()
        p("| jars | kind | symbol |")
        p("|---|---|---|")
        for n, k, s in rows[:top]:
            p(f"| {n} | {k} | `{s}` |")
    p()
    p("## Library or other mod: by package (jars affected, distinct symbols)")
    p()
    p("| jars | symbols | package |")
    p("|---|---|---|")
    for pkg in sorted(pkg_jars, key=lambda x: (-len(pkg_jars[x]), x))[:60]:
        p(f"| {len(pkg_jars[pkg])} | {len(pkg_syms[pkg])} | `{pkg}` |")
    p()
    p("## Jars blocked only by Minecraft/Forge symbols (fewest first)")
    p()
    rows = []
    for jar, syms in miss.items():
        names = sorted(s for k, s in syms)
        if names and all(area(s) in ("Minecraft common", "Minecraft client", "Forge/NeoForge API") for s in names) and not any(FABRIC.search(s) for s in names):
            rows.append((len(names), jar, names))
    for n, jar, names in sorted(rows)[:60]:
        more = f" (+{n - 4} more)" if n > 4 else ""
        p(f"- {n} `{jar}`: " + ", ".join(f"`{s}`" for s in names[:4]) + more)


if __name__ == "__main__":
    main()
