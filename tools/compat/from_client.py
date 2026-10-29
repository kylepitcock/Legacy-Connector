#!/usr/bin/env python3
"""Adds in-game results to docs/data/mods.json (source "client"): reads a run's legacyconnector-report.txt and latest.log
(a real client session, or a server self-test run: ./gradlew runServerSubTest) and rates every legacy mod of it:

  works    content registered (the "Legacy content" table), no failure while loading, and the self tests (BLOCK/ITEM/ENTITY
           SELF TEST in the same log) found errors in less than 10% of the mod's blocks, items and entity types (a block
           or item that threw, an entity whose tick failed; blocks that pop off and mobs that despawn are not errors)
  partial  content registered, but the run had no self test, or the self tests found more problems, or a phase failed
  loads    loaded without content (libraries, client tweaks) and without failing
  fails    failed while loading and registered nothing

usage: from_client.py <legacyconnector-report.txt> <latest.log> <pack label> [mc-version] [date]
Earlier "client" entries of the same mod and version are replaced; a mod rated in several runs keeps its best result
(works > partial > loads > fails). Player reports and maintainer entries outrank these on the site, startup-test
(corpus) entries rank below them.
"""
import json, pathlib, re, sys

report, log, label = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2]), sys.argv[3]
mc = sys.argv[4] if len(sys.argv) > 4 else "1.7.10"
date = sys.argv[5] if len(sys.argv) > 5 else "2026-10-28"
DATA = pathlib.Path(__file__).resolve().parents[2] / "docs" / "data" / "mods.json"
RANK = ["fails", "loads", "partial", "works"]


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


# modern id -> (legacy id, name, version), only jars of the requested legacy version
mods, jar_ok = {}, False
for line in report.read_text(encoding="utf-8", errors="replace").splitlines():
    m = re.match(r"^(\S.*\.jar)\s+\[(\d[\d.]*)?", line)
    if m:
        jar_ok = m.group(2) in (None, mc)
        continue
    m = re.match(r"^  mod (.+?) -> (\S+)\s+\((.*)\)\s*$", line)
    if m and jar_ok:
        v = re.match(r"^(.*?)\s+(\d[^ ]*(?: .*)?)$", m.group(3))
        name, version = (v.group(1), v.group(2)) if v else (m.group(3), "")
        mods[m.group(2)] = (m.group(1), name.strip() or m.group(1), version.strip())

text = log.read_text(encoding="utf-8", errors="replace")
failed = {}
for m in re.finditer(r"Legacy mod (\S+) \([^)]*\) failed during (\w+): (.*)", text):
    failed.setdefault(norm(m.group(1)), f"{m.group(2)}: " + re.sub(r"\s+", " ", m.group(3))[:140])

content = {}
for m in re.finditer(r"(?m)^  (\S+)\s+(\d+) items\s+(\d+) blocks\s+(\d+) entities", text):
    content[m.group(1)] = tuple(int(x) for x in m.group(2, 3, 4))


def problems(header):
    """Problem lines (`  ns:name: reason`) after the last log line starting with header, keyed by namespace."""
    i = text.rfind(header)
    if i < 0:
        return None
    out = {}
    for line in text[i:].splitlines()[1:]:
        if not line.startswith("  ") or line.startswith("   "):
            break
        key = line.strip().split(": ", 1)[0]
        if ":" in key:
            ns = key.split(":", 1)[0]
            out[ns] = out.get(ns, 0) + 1
    return out


block_p, item_p, entity_p = problems("BLOCK SELF TEST:"), problems("ITEM SELF TEST:"), None
i = text.rfind("ENTITY SELF TEST:")
if i >= 0:
    entity_p = {}
    for line in text[i:].splitlines()[1:]:
        if not line.startswith("  "):
            break
        key, _, result = line.strip().partition(": ")
        if ":" in key and result.startswith("FAILED"):
            ns = key.split(":", 1)[0]
            entity_p[ns] = entity_p.get(ns, 0) + 1
vanished = {}
m = re.search(r"Blocks gone after 60 ticks, per mod: \{(.*)\}", text)
if m:
    for part in filter(None, m.group(1).split(", ")):
        k, _, v = part.partition("=")
        vanished[k] = int(v or 0)
selftest = block_p is not None and item_p is not None

results = {}
for modern, (legacy, name, version) in mods.items():
    items, blocks, entities = content.get(modern, (0, 0, 0))
    fail = failed.get(norm(legacy)) or failed.get(norm(modern))
    total = items + blocks + entities
    what = f"{items} items, {blocks} blocks, {entities} entity types"
    if total == 0:
        status = "fails" if fail else "loads"
        notes = f"Fails during {fail}" if fail else f"Loads in {label} without errors (no blocks, items or entities of its own)."
    elif fail:
        status, notes = "partial", f"In {label} {what} register, but loading failed during {fail}"
    elif not selftest:
        status, notes = "partial", f"Loads in {label}: {what} register. Not self-tested yet."
    else:
        bad = block_p.get(modern, 0) + item_p.get(modern, 0) + (entity_p or {}).get(modern, 0)
        share = bad / total
        status = "works" if share < 0.10 else "partial"
        notes = (f"Self-tested in {label}: {what}; every block placed and used, every item used, every entity spawned"
                 + (" without errors." if bad == 0 else f"; {bad} threw errors ({share:.0%}).")
                 + (f" {vanished[modern]} blocks did not stay placed." if vanished.get(modern) else ""))
    results[modern] = {"name": name, "version": version, "mc": mc, "source": "client", "date": date, "status": status, "notes": notes}

data = json.loads(DATA.read_text(encoding="utf-8"))
mods_list = data["mods"]
changed = 0
for r in results.values():
    same = [e for e in mods_list if e.get("source") == "client" and e.get("mc") == mc and norm(e["name"]) == norm(r["name"])]
    if same and RANK.index(same[0]["status"]) > RANK.index(r["status"]) and same[0]["version"] == r["version"]:
        continue   # an earlier run of the same version rated it better (e.g. a self test vs a plain session)
    for e in same:
        mods_list.remove(e)
    mods_list.append(r)
    changed += 1
mods_list.sort(key=lambda e: e["name"].lower())
# mods listed in "hidden" (nothing to do on 1.21.1, or client mods with their own 1.21.1 version) stay off the site
hidden = {norm(h) for h in data.get("hidden", [])}
data["mods"] = [e for e in data["mods"] if norm(e["name"]) not in hidden]
DATA.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
counts = {s: sum(1 for r in results.values() if r["status"] == s) for s in RANK}
print(f"{len(results)} mods rated ({counts}), {changed} entries written{'' if selftest else ' (no self test in this log)'}")
