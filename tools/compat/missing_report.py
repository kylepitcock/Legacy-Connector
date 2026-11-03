#!/usr/bin/env python3
"""Ranks what the link probe found missing, so the layer's gaps can be closed in order of payoff.

usage: missing_report.py <probe-records.json>... [--mc 1.12.2] [--top 60] [--out report.md] [--exclude PREFIX ...]

Input: the raw records `probe.py --json out.json` keeps (one per jar, with the full "missing" and "stubbed" lists).
  missing  references the shim, the synthesizer and the libraries cannot satisfy (the jar would throw when it uses them)
  stubbed  legacy API the layer generated as an empty stub: it links but does nothing (a gap in behaviour)
Symbols are counted per distinct MOD (a jar that bundles two mods counts for the first; the same mod from two sources counts once),
so one missing method that blocks 40 mods outranks 40 methods that each block one. Also groups by class, because adding one class
with all its members usually closes many symbols at once. Client-only noise (java.awt, sun., ...) is flagged with --exclude.
"""
import argparse, collections, json, pathlib, re, sys

NOISE = ("java.awt.", "javax.swing.", "sun.", "com.sun.", "jdk.", "org.lwjgl.", "paulscode.")


def owner(sym):
    # "pkg.Class.member(desc)ret" or "pkg.Class.field:Ltype;" -> "pkg.Class"
    head = re.split(r"[(:]", sym, maxsplit=1)[0]
    return head.rsplit(".", 1)[0] if "." in head else head


def load(paths, mc):
    seen, mods = set(), []
    for p in paths:
        for r in json.loads(pathlib.Path(p).read_text(encoding="utf-8")):
            if r.get("result") not in ("links", "unresolved") or not r.get("mods"):
                continue
            if mc and r.get("mc") != mc:
                continue
            m = r["mods"][0]
            key = ((m.get("name") or m["id"]).strip().lower(), (m.get("version") or "").strip())
            if key in seen:
                continue
            seen.add(key)
            mods.append((key[0], r))
    return mods


def rank(mods, field, exclude):
    by_sym = collections.defaultdict(set)
    for name, r in mods:
        for sym in r.get(field, []):
            if not any(sym.startswith(x) for x in exclude):
                by_sym[sym].add(name)
    by_cls = collections.defaultdict(lambda: [set(), set()])
    for sym, names in by_sym.items():
        c = owner(sym)
        by_cls[c][0] |= names
        by_cls[c][1].add(sym)
    return by_sym, by_cls


def table(title, by_sym, by_cls, top, total):
    out = [f"## {title}", ""]
    out.append(f"{len(by_sym)} distinct symbols in {len(by_cls)} classes.\n")
    out.append("### By class (one class closes all its symbols)\n")
    out.append("| Class | Mods | Symbols | Example mods |")
    out.append("|---|---:|---:|---|")
    for c, (names, syms) in sorted(by_cls.items(), key=lambda kv: (-len(kv[1][0]), -len(kv[1][1]), kv[0]))[:top]:
        out.append(f"| `{c}` | {len(names)} | {len(syms)} | {', '.join(sorted(names)[:3])} |")
    out.append("\n### By symbol\n")
    out.append("| Symbol | Mods |")
    out.append("|---|---:|")
    for sym, names in sorted(by_sym.items(), key=lambda kv: (-len(kv[1]), kv[0]))[:top]:
        out.append(f"| `{sym}` | {len(names)} |")
    return "\n".join(out) + "\n"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("records", nargs="+")
    ap.add_argument("--mc")
    ap.add_argument("--top", type=int, default=60)
    ap.add_argument("--out")
    ap.add_argument("--exclude", nargs="*", default=list(NOISE), help="symbol prefixes to ignore (default: client-only JDK/LWJGL noise)")
    a = ap.parse_args()
    mods = load(a.records, a.mc)
    if not mods:
        sys.exit("no records")
    failing = [r for _, r in mods if r["result"] == "unresolved"]
    parts = [f"# Link probe gaps{' for ' + a.mc if a.mc else ''}\n",
             f"{len(mods)} mods probed, {len(failing)} with unresolved references. Excluded prefixes: {', '.join(a.exclude) or 'none'}.\n"]
    for field, title in (("missing", "Unresolved references (the mod would throw)"), ("stubbed", "Synthesized stubs (link, but do nothing)")):
        by_sym, by_cls = rank(mods, field, a.exclude)
        parts.append(table(title, by_sym, by_cls, a.top, len(mods)))
    # how many failing mods one more class would flip: mods whose only missing classes are among the top N
    by_sym, by_cls = rank(mods, "missing", a.exclude)
    top_classes = {c for c, _ in sorted(by_cls.items(), key=lambda kv: -len(kv[1][0]))[:10]}
    flips = sum(1 for _, r in mods if r["result"] == "unresolved" and r.get("missing") and
                all(owner(s) in top_classes or any(s.startswith(x) for x in a.exclude) for s in r["missing"]))
    parts.append(f"Closing just the top 10 classes above would flip {flips} of {len(failing)} failing mods to \"links\".\n")
    text = "\n".join(parts)
    if a.out:
        pathlib.Path(a.out).write_text(text, encoding="utf-8")
        print(f"wrote {a.out}")
    else:
        print(text)


main()
