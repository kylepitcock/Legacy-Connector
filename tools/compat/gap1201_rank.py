#!/usr/bin/env python3
"""Ranks the output of `./gradlew gap1201` (the <report>.detail.tsv next to the report) against the corpus index of corpus1201.py.

usage: gap1201_rank.py <gap1201.txt.detail.tsv> <test-mods-1201/index.json> [--top 40] > ranking.md

Prints markdown: top unresolved symbols by number of jars, grouped by area (Forge API / Minecraft common / rendering+client / library /
other), per-area totals, mixin classes per jar (0, 1-5, 6+) and a cheap/medium/heavy classification of every jar.
"""
import argparse, collections, json, re


def area(sym):
    owner = sym.split(".")[0] if "." in sym.split("(")[0].split(":")[0] else sym
    owner = owner.split("(")[0]
    if owner.startswith(("net/minecraftforge/", "net/neoforged/")):
        return "Forge API"
    if owner.startswith(("net/minecraft/client/", "com/mojang/blaze3d/", "com/mojang/math/", "com/mojang/realmsclient/")):
        return "Rendering / client"
    if owner.startswith(("net/minecraft/", "com/mojang/")):
        return "Minecraft 1.20.1 to 1.21.1 change"
    if owner.startswith(("java/", "javax/", "sun/", "jdk/")):
        return "JDK (not an API gap)"
    return "Library / other mod"


def owner_of(sym):
    return re.split(r"[.(:]", sym.replace("/", "\u0000"), 1)[0].replace("\u0000", "/") if "." in sym else sym


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("detail")
    ap.add_argument("index")
    ap.add_argument("--top", type=int, default=40)
    ap.add_argument("--abandoned", action="store_true", help="only jars without a known NeoForge 1.21.1 port (group content)")
    a = ap.parse_args()
    idx = json.load(open(a.index))["mods"]
    sym_jars = collections.defaultdict(set)   # (kind, symbol) -> jars
    own_jars = collections.defaultdict(set)   # owner class (missing class or class with a missing member) -> jars
    per_jar = collections.defaultdict(lambda: collections.Counter())
    refs = {}
    for line in open(a.detail, encoding="utf-8"):
        jar, kind, sym = line.rstrip("\n").split("\t", 2)
        if a.abandoned and not (idx.get(jar, {}).get("port") == "no" and idx[jar].get("group") == "content"):
            continue
        if kind == "refs":
            refs[jar] = int(sym)
            continue
        sym_jars[(kind, sym)].add(jar)
        o = sym if kind == "class" else sym.split(".")[0]
        own_jars[o].add(jar)
        per_jar[jar][area(sym)] += 1
    jars = sorted(refs)
    # symbols that belong to another corpus mod's own packages are not a Minecraft/Forge gap: they are the library problem
    print(f"## Ranking ({len(jars)} jars, {len(sym_jars)} distinct unresolved symbols)\n")
    totals = collections.Counter()
    for (kind, sym), js in sym_jars.items():
        totals[area(sym)] += 1
    print("### Distinct unresolved symbols by area\n\n| Area | Distinct symbols |\n|---|---|")
    for k, v in totals.most_common():
        print(f"| {k} | {v} |")
    print()
    ranked = sorted(sym_jars.items(), key=lambda kv: (-len(kv[1]), kv[0]))
    print(f"### Top {a.top} unresolved symbols (jars that need them)\n\n| Jars | Kind | Symbol | Area |\n|---|---|---|---|")
    for (kind, sym), js in ranked[:a.top]:
        print(f"| {len(js)} | {kind} | `{sym}` | {area(sym)} |")
    print()
    ar = collections.defaultdict(list)
    for (kind, sym), js in ranked:
        ar[area(sym)].append((len(js), kind, sym))
    for k in ["Forge API", "Minecraft 1.20.1 to 1.21.1 change", "Rendering / client", "Library / other mod"]:
        print(f"### Top 20 in area: {k}\n\n| Jars | Kind | Symbol |\n|---|---|---|")
        for n, kind, sym in ar[k][:20]:
            print(f"| {n} | {kind} | `{sym}` |")
        print()
    pk = collections.defaultdict(set)
    pk_syms = collections.Counter()
    for (kind, sym), js in sym_jars.items():
        if area(sym) == "Library / other mod":
            key = "/".join(owner_of(sym).split("/")[:3])
            pk[key] |= js
            pk_syms[key] += 1
    print("### Library / other-mod gaps by package (jars that reference something unresolved there)\n\n| Jars | Distinct symbols | Package |\n|---|---|---|")
    for key, js in sorted(pk.items(), key=lambda kv: (-len(kv[1]), kv[0]))[:25]:
        print(f"| {len(js)} | {pk_syms[key]} | `{key}` |")
    print()
    print("### Top 40 classes by number of jars (a missing class or any missing member of it)\n\n| Jars | Class | Area |\n|---|---|---|")
    for o, js in sorted(own_jars.items(), key=lambda kv: (-len(kv[1]), kv[0]))[:40]:
        print(f"| {len(js)} | `{o}` | {area(o)} |")
    print()
    # mixins and classification
    print("### Per jar: mixins, gap size and class\n")
    print("| Jar | Group | 1.21.1 port | Mixin classes | Refs | Missing symbols (forge / mc / client / lib) | Class |\n|---|---|---|---|---|---|---|")
    bucket = collections.defaultdict(list)
    mix_bucket = collections.Counter()
    for jar in jars:
        m = idx.get(jar, {})
        meta = m.get("meta", {})
        mix = meta.get("mixins") or 0
        c = per_jar[jar]
        n = sum(v for k, v in c.items() if not k.startswith("JDK"))
        nf, nm, nc, nl = c["Forge API"], c["Minecraft 1.20.1 to 1.21.1 change"], c["Rendering / client"], c["Library / other mod"]
        mb = "0" if mix == 0 else "1-5" if mix <= 5 else "6+"
        mix_bucket[mb] += 1
        own = nf + nm + nc  # library gaps vanish when the library itself is migrated
        if own <= 40 and mix <= 1:
            cls = "cheap"
        elif own <= 300 and mix <= 5:
            cls = "medium"
        else:
            cls = "heavy"
        bucket[cls].append((own, mix, jar, m.get("port", "?"), m.get("group", "?")))
        print(f"| {jar} | {m.get('group', '?')} | {m.get('port', '?')} | {mix} | {refs[jar]} | {n} ({nf} / {nm} / {nc} / {nl}) | {cls} |")
    print(f"\nMixin classes per jar: 0 = {mix_bucket['0']}, 1-5 = {mix_bucket['1-5']}, 6+ = {mix_bucket['6+']}\n")
    for cls in ["cheap", "medium", "heavy"]:
        print(f"**{cls}** ({len(bucket[cls])}): " + ", ".join(f"{j} ({o}, mx {mx})" for o, mx, j, _, _ in sorted(bucket[cls])))
        print()


main()
