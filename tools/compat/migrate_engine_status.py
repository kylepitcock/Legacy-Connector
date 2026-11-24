#!/usr/bin/env python3
"""Applies the `engine` status ("Engine mod: not needed") to the existing compatibility data of every legacy version (idempotent).

usage: migrate_engine_status.py [--dry-run]

Source of truth: loader/src/main/resources/legacyconnector/engine-mods.tsv (tools/compat/engine_mods.py).  For every docs/data/mods.json mod that is on the
table for its Minecraft version a probe-tier entry with status "engine" is written (an existing probe entry of the mod is turned into one); the site then
shows "Engine mod: not needed" unless a better-sourced entry (maintainer, client, community) says the mod really runs when hosted (works, playable, perfect,
partial), in which case nothing is added.  In docs/data/probe.json every run's per-mod list is reclassified the same way and its counts moved to "engine".
Future probe runs do this themselves (probe.py annotate_engine); this script is for data that already exists.
"""
import argparse
import collections
import datetime
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import engine_mods  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parents[2]
MODS = ROOT / "docs" / "data" / "mods.json"
PROBE = ROOT / "docs" / "data" / "probe.json"
HOSTED_OK = {"perfect", "playable", "works", "partial"}


def migrate_mods(data, today):
    rows = engine_mods.load()
    groups = collections.defaultdict(list)
    for e in data["mods"]:
        groups[(engine_mods.norm(e["name"]), e.get("mc", "1.7.10"))].append(e)
    changed = 0
    per_mc = collections.Counter()
    for (n, mc), entries in groups.items():
        row = engine_mods.find(mc, mod_names=[entries[0]["name"]], rows=rows)
        if not row:
            continue
        if any(e.get("source") not in ("probe", "corpus") and e["status"] in HOSTED_OK for e in entries):
            continue   # a real rating says it runs when hosted: that stays
        note = engine_mods.describe(row)
        probe = [e for e in entries if e.get("source") == "probe"]
        if probe:
            for e in probe:
                if e["status"] != "engine" or e.get("notes") != note:
                    e["status"], e["notes"], e["date"] = "engine", note, e.get("date") or today
                    changed += 1
        else:
            data["mods"].append({"name": entries[0]["name"], "version": entries[0].get("version", ""), "mc": mc, "source": "probe", "date": today, "status": "engine", "notes": note})
            changed += 1
        per_mc[mc] += 1
    data["mods"].sort(key=lambda e: e["name"].lower())
    return changed, per_mc


def unhide(data, today):
    """Engine mods used to be hidden from the site altogether (docs/data/mods.json "hidden"/"hidden_by_mc"); they are listed now, with the engine status.
    Removes them from both lists and gives each one an entry for the Minecraft version it belongs to."""
    rows = engine_mods.load()
    have = {(engine_mods.norm(e["name"]), e.get("mc", "1.7.10")) for e in data["mods"]}
    added = 0
    names = list(data.get("hidden", [])) + [n for v in data.get("hidden_by_mc", {}).values() for n in v]
    for name in names:
        row = None
        for mc in ("1.7.10", "1.12.2", "1.20.1"):
            row = engine_mods.find(mc, mod_names=[name], rows=rows)
            if row:
                break
        if not row:
            continue
        mc = "1.12.2" if "1.12.2" in row["reason"] else "1.7.10" if "1.7.10" in row["reason"] else ("1.20.1" if "1.20.1" in row["versions"] and "1.7.10" not in row["versions"] else "1.7.10")
        if "1.7.10" in row["versions"] and mc == "1.20.1":
            mc = "1.7.10"
        data["hidden"] = [h for h in data.get("hidden", []) if h != name]
        for v in data.get("hidden_by_mc", {}):
            data["hidden_by_mc"][v] = [h for h in data["hidden_by_mc"][v] if h != name]
            if name in names and v == "1.20.1" and "1.20.1" in row["versions"]:
                mc = "1.20.1"
        if (engine_mods.norm(name), mc) not in have:
            data["mods"].append({"name": name, "version": "", "mc": mc, "source": "probe", "date": today, "status": "engine", "notes": engine_mods.describe(row)})
            have.add((engine_mods.norm(name), mc))
            added += 1
    return added


def migrate_probe(h):
    rows = engine_mods.load()
    moved = 0
    for run in h["runs"]:
        for r in run.get("mods", []):
            if r[2] == "engine":
                continue
            row = engine_mods.find(run["mc"], mod_names=[r[0]], rows=rows)
            if row:
                run["counts"][r[2]] = max(0, run["counts"].get(r[2], 0) - 1)
                run["counts"]["engine"] = run["counts"].get("engine", 0) + 1
                r[2] = "engine"
                r[3] = row["replacement"] or row["reason"]
                moved += 1
    return moved


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()
    today = datetime.date.today().isoformat()
    data = json.loads(MODS.read_text(encoding="utf-8"))
    added = unhide(data, today)
    changed, per_mc = migrate_mods(data, today)
    h = json.loads(PROBE.read_text(encoding="utf-8"))
    moved = migrate_probe(h)
    print(f"mods.json: {added} engine mods un-hidden and listed; {changed} entries written or updated; engine mods per version: " + ", ".join(f"{k} {v}" for k, v in sorted(per_mc.items())))
    print(f"probe.json: {moved} per-mod rows reclassified as engine")
    if not a.dry_run:
        MODS.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
        PROBE.write_text(json.dumps(h, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
