#!/usr/bin/env python3
"""Rates the evidence the survey driver (survey.py) gathered and merges it into docs/data/mods.json (source "client").

  python tools/survey/apply.py [--all | --mc 1.12.2] [--dry] [--stats]

Idempotent: run it any time (the driver calls it every ~50 jars and at the end). Rules:
  works    content registered, nothing wrong in the evidence
  partial  content registered, but: a placement/spawn threw, missing texture/model or error log lines, missing-texture magenta in the
           shot, sampled blocks/entities left no visible pixels, or the mod's load had a failed phase after registering content
  fails    a load phase failed and nothing could be shown, or the client crashed before the world started with this jar alone
  crashes  the client crashed while rendering this mod's sample
  loads    no content of its own: no entry is written (nothing new is known); not reached / unknown: no entry either
Never touches entries whose source is not probe/corpus/client (maintainer and player results win) and never replaces a better client
result that came from a real session (notes not starting with "Automated client survey"). Same key order and indent as from_client.py.
"""
import argparse, json, pathlib, re, sys

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[1]
MAIN = next((p for p in [ROOT, *ROOT.parents] if (p / "run-perfmods").is_dir()), ROOT)
SURVEY = MAIN / "run-survey-main"
RANK = ["crashes", "fails", "loads", "partial", "works", "playable", "perfect"]
PREFIX = "Automated client survey: "


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


def missing_dependency(fail):
    """A NoClassDefFoundError for a class outside Minecraft/Forge/the layer means a dependency mod that is not in the survey set."""
    mm = re.search(r"NoClassDefFoundError: ([\w./$]+)", fail or "")
    if not mm:
        return None
    cls = mm.group(1).replace("/", ".")
    return None if cls.startswith(("net.minecraft", "cpw.", "legacyconnector", "org.kylepitcock", "javax.", "java.", "org.lwjgl", "io.netty")) else cls


def rate(m):
    """-> (status or None, evidence text)"""
    if missing_dependency(m.get("fail")):
        return None, ""   # needs another mod that is not in the set: nothing to say about the layer
    if m.get("unknown"):
        return None, ""
    if m.get("loadCrash"):
        return "fails", m.get("note", "the client crashed while loading it")
    if m.get("ns") is None:
        return None, ""
    c = m.get("counts", {})
    total = c.get("blocks", 0) + c.get("items", 0) + c.get("entities", 0)
    ev = [f"{c.get('items', 0)} items, {c.get('blocks', 0)} blocks, {c.get('entities', 0)} entity types"
          + (f", {c['biomes']} biomes" if c.get("biomes") else "") + (f", {c['dims']} dimensions" if c.get("dims") else "")]
    if m.get("crashedHere"):
        return "crashes", "the client crashed while showing its sample blocks/entities (" + ev[0] + ")"
    if not m.get("reached", True):
        return None, ""
    pl, sp, lg, vis = m.get("placed", [0, 0, 0, []]), m.get("spawned", [0, 0, 0, []]), m.get("log", {}), m.get("vision", {})
    if pl[1]:
        ev.append(f"placed {pl[0]}/{pl[1]} sampled blocks")
    if sp[1]:
        ev.append(f"spawned {sp[0]}/{sp[1]} entity types")
    problems = []
    if m.get("fail"):
        problems.append("load failure during " + m["fail"])
    if pl[2]:
        problems.append(f"{pl[2]} block placements threw" + (f" ({pl[3][0]})" if pl[3] else ""))
    if sp[2]:
        problems.append(f"{sp[2]} entity spawns threw" + (f" ({sp[3][0]})" if sp[3] else ""))
    if lg.get("missing"):
        problems.append(f"{lg['missing']} missing texture/model log lines")
    if lg.get("errors"):
        problems.append(f"{lg['errors']} error log lines")
    b, e = vis.get("blocks"), vis.get("entities")
    if b and b.get("magentaPx", 0) > 60 and b.get("magenta", 0) > 0.03:
        problems.append("missing-texture magenta visible in the block shot")
    if b and pl[0] >= 4 and b.get("changed", 1) < 0.0008 and not lg.get("missing"):
        problems.append("sampled blocks left no visible pixels")
    if e and e.get("magentaPx", 0) > 60 and e.get("magenta", 0) > 0.03:
        problems.append("missing-texture magenta visible in the entity shot")
    if e and sp[0] >= 2 and e.get("changed", 1) < 0.0008:
        problems.append("spawned entities left no visible pixels (invisible?)")
    text = "; ".join(ev + problems)
    if total == 0:
        return ("fails", text) if m.get("fail") else (None, "")
    if m.get("fail") and not (pl[0] or sp[0]):
        return "fails", text
    return ("partial" if problems else "works"), text


def collect(mcs):
    rows = []
    for mc in mcs:
        d = SURVEY / "results" / mc
        for f in sorted(d.glob("*.json")) if d.exists() else []:
            try:
                rec = json.loads(f.read_text(encoding="utf-8"))
            except Exception:
                continue
            for m in rec["mods"]:
                status, ev = rate(m)
                if status:
                    rows.append({"name": m["name"], "version": m.get("version", ""), "mc": mc, "source": "client", "date": rec["date"],
                                 "status": status, "notes": (PREFIX + ev)[:420]})
    return rows


def merge(rows, data_path, dry):
    data = json.loads(data_path.read_text(encoding="utf-8"))
    mods = data["mods"]
    changed = 0
    for r in rows:
        existing = [e for e in mods if e.get("mc") == r["mc"] and norm(e["name"]) == norm(r["name"])]
        if any(e.get("source") not in ("probe", "corpus", "client") for e in existing):
            continue   # maintainer / player entries win
        real = [e for e in existing if e.get("source") == "client" and not e["notes"].startswith(PREFIX)]
        if real and RANK.index(real[0]["status"]) >= RANK.index(r["status"]):
            continue   # a real session rated it at least as well
        auto = [e for e in existing if e.get("source") == "client" and e["notes"].startswith(PREFIX)]
        if auto and auto[0]["status"] == r["status"] and auto[0]["notes"] == r["notes"] and auto[0]["version"] == r["version"]:
            continue   # unchanged
        if dry:
            print(r["status"], r["mc"], r["name"], "-", r["notes"][:140])
        else:
            for e in [e for e in existing if e.get("source") in ("client", "probe", "corpus")]:
                mods.remove(e)
            mods.append(r)
        changed += 1
    if not dry and changed:
        mods.sort(key=lambda e: e["name"].lower())
        hidden = {norm(h) for h in data.get("hidden", [])}
        data["mods"] = [e for e in data["mods"] if norm(e["name"]) not in hidden]
        data_path.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    return changed


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--all", action="store_true")
    ap.add_argument("--mc")
    ap.add_argument("--dry", action="store_true")
    ap.add_argument("--stats", action="store_true")
    ap.add_argument("--data", help="mods.json to merge into (default: the checkout this script runs in, else the main checkout)")
    a = ap.parse_args()
    mcs = ["1.12.2", "1.7.10"] if a.all or not a.mc else [a.mc]
    rows = collect(mcs)
    SURVEY.mkdir(exist_ok=True)
    (SURVEY / "client-rows.json").write_text(json.dumps(rows, indent=1, ensure_ascii=False), encoding="utf-8")
    counts = {}
    for r in rows:
        counts[(r["mc"], r["status"])] = counts.get((r["mc"], r["status"]), 0) + 1
    print("ratings:", {f"{k[0]} {k[1]}": v for k, v in sorted(counts.items())})
    if a.stats:
        return
    cands = [pathlib.Path(a.data)] if a.data else [pathlib.Path.cwd() / "docs" / "data" / "mods.json", ROOT / "docs" / "data" / "mods.json", MAIN / "docs" / "data" / "mods.json"]
    data_path = next((p for p in cands if p.exists()), None)
    if not data_path:
        print("no docs/data/mods.json found")
        return
    print(f"{merge(rows, data_path, a.dry)} entries {'would be ' if a.dry else ''}written to {data_path}")


if __name__ == "__main__":
    main()
