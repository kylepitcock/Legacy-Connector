#!/usr/bin/env python3
"""Writes the shim worklist from raw probe records: every legacy API class the shim lacks (missing) or only stubs, with the members to implement.

usage: worklist.py <probe-records.json>... --mc <ver> --out <dir> [--top 100] [--members 12] [--name-prefix shim-gaps]

Input: the raw records `probe.py --json out.json` keeps. Output: <dir>/<prefix>-<ver>-missing.md and <prefix>-<ver>-stubs.md, classes ranked by the number of
distinct mods that need them, each with its members (distinct mods per member) in readable form. Missing = the mod throws when it uses it; stubs = the
layer generated an empty placeholder (links, does nothing), so the real implementation is the work. Client-only JDK/LWJGL noise is left out.
"""
import argparse, collections, json, pathlib, re

NOISE = ("java.awt.", "javax.swing.", "sun.", "com.sun.", "jdk.", "org.lwjgl.", "paulscode.")


def split(sym):
    head = re.split(r"[(:]", sym, maxsplit=1)[0]
    cls = head.rsplit(".", 1)[0] if "." in head else head
    return cls, sym[len(cls) + 1:]


def short(text):
    text = text.replace("legacyconnector_synth.", "SYNTH:").replace("legacyconnector.", "")
    return re.sub(r"L([\w.$/]+);", lambda m: m.group(1).rsplit(".", 1)[-1], text)


def load(paths, mc):
    seen, mods = set(), []
    for p in paths:
        for r in json.loads(pathlib.Path(p).read_text(encoding="utf-8")):
            if r.get("result") not in ("links", "unresolved") or not r.get("mods") or r.get("mc") != mc:
                continue
            m = r["mods"][0]
            key = ((m.get("name") or m["id"]).strip().lower(), (m.get("version") or "").strip())
            if key not in seen:
                seen.add(key)
                mods.append((key[0], r))
    return mods


def rank(mods, field):
    classes = collections.defaultdict(lambda: {"mods": set(), "members": collections.defaultdict(set)})
    for name, r in mods:
        for sym in r.get(field, []):
            if sym.startswith(NOISE):
                continue
            cls, member = split(sym)
            classes[cls]["mods"].add(name)
            classes[cls]["members"][member].add(name)
    return sorted(classes.items(), key=lambda kv: (-len(kv[1]["mods"]), -len(kv[1]["members"]), kv[0]))


def write(path, title, ranked, top, members, total_mods, blurb):
    out = [f"---", f"name: {path.stem}", f"description: {title}", "metadata:", "  type: project", "---", "", blurb, "",
           f"{len(ranked)} classes in total ({total_mods} mods probed); the {min(top, len(ranked))} most needed are listed. Format: `class` (mods) then members (mods): "
           "a class `SYNTH:` is one the shim lacks entirely, otherwise the members are missing from an existing shim class.", ""]
    for cls, d in ranked[:top]:
        ms = sorted(d["members"].items(), key=lambda kv: (-len(kv[1]), kv[0]))
        shown = "; ".join(f"{short(m)} ({len(who)})" for m, who in ms[:members])
        more = f"; +{len(ms) - members} more" if len(ms) > members else ""
        ex = ", ".join(sorted(d["mods"])[:3])
        out.append(f"- `{short(cls)}` ({len(d['mods'])}): {shown}{more} | e.g. {ex}")
    path.write_text("\n".join(out) + "\n", encoding="utf-8")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("records", nargs="+")
    ap.add_argument("--mc", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--top", type=int, default=100)
    ap.add_argument("--members", type=int, default=12)
    ap.add_argument("--name-prefix", default="shim-gaps")
    a = ap.parse_args()
    mods = load(a.records, a.mc)
    out = pathlib.Path(a.out)
    v = a.mc.replace(".", "")
    today = __import__("datetime").date.today().isoformat()
    write(out / f"{a.name_prefix}-{v}-missing.md", f"{a.mc}: legacy API classes and members the shim lacks (mods throw), ranked by mods affected, from the probe ({today})",
          rank(mods, "missing"), a.top, a.members, len(mods),
          f"Worklist for making the {a.mc} shim complete (goal: [[shim-completeness-goal]]). Each entry is a class or member a real mod references that nothing "
          "provides: implement it generically in the shim (src/main/java/legacyconnector/v*/...) or, for NEI/Baubles/Waila/CodeChickenLib, in the bridge. "
          "Entries for another mod's API (thaumcraft.api, appeng, ic2, ...) are the layer's only when the layer is meant to provide that API. "
          "Regenerate: `python3 tools/compat/probe.py <dir> --mc " + a.mc + " --json out.json` then `python3 tools/compat/worklist.py out.json --mc " + a.mc + " --out <memory dir>`.")
    write(out / f"{a.name_prefix}-{v}-stubs.md", f"{a.mc}: legacy API members that are only empty stubs (link, do nothing), ranked by mods affected, from the probe ({today})",
          rank(mods, "stubbed"), a.top, a.members, len(mods),
          f"Worklist for replacing {a.mc} stubs with real implementations (goal: [[shim-completeness-goal]]). A stub is a member the shim lacks that the layer "
          "generated empty so the mod still links; the real behaviour is the work. Heaviest on core classes (Minecraft, Entity, Item, TileEntity, renderers). "
          "Perf/engine mods (angelica, embeddium, beddium, emi, ...) hit renderer stubs they do not need: weigh by gameplay mods first.")
    print("wrote", [p.name for p in sorted(out.glob(f"{a.name_prefix}-{v}-*.md"))])


main()
