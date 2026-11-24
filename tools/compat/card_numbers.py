#!/usr/bin/env python3
"""The numbers of the compatibility site's version cards, computed from docs/data/mods.json the way docs/assets/site.js does (without community reports).

usage: card_numbers.py [--mods docs/data/mods.json] [--json]

Per Minecraft version: mods counted (everything except native/engine), "% start and run" (loads, partial, works, playable, perfect over the counted mods),
confirmed working (works, playable, perfect), and the numbers left out of the card: native, engine.  Tier order as the site: community (not in mods.json), maintainer,
client, corpus, probe; the status of a mod is the most common one of its winning tier (client tier: the best), ties go to the newest, then to the better rating;
an `engine` entry wins over a probe/corpus result, but not over a better-sourced rating that says the mod runs.
"""
import argparse
import collections
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
TIER = ["community", "maintainer", "client", "corpus", "probe"]
ORDER = ["perfect", "playable", "works", "partial", "loads", "fails", "crashes"]
EXTRA = ["native", "engine"]
GOOD = ["perfect", "playable", "works"]
HOSTED_OK = ["perfect", "playable", "works", "partial"]


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


def status_of(entries):
    pool, used = [], ""
    for tier in TIER:
        pool = [e for e in entries if (e.get("source") or "maintainer") == tier]
        used = tier
        if pool:
            break
    if used == "client":
        best = min((ORDER.index(e["status"]) if e["status"] in ORDER else 99) for e in pool)
        pool = [e for e in pool if (ORDER.index(e["status"]) if e["status"] in ORDER else 99) == best]
    counts, newest = collections.Counter(), {}
    for e in pool:
        counts[e["status"]] += 1
        newest[e["status"]] = max(newest.get(e["status"], ""), e.get("date") or "")
    allst = ORDER + EXTRA
    st = sorted(allst, key=lambda s: (-counts[s], _neg(newest.get(s, "")), allst.index(s)))[0]
    if not counts[st]:
        st = "untested"
    if any(e["status"] == "engine" for e in entries) and not (st in HOSTED_OK and used not in ("probe", "corpus")):
        st = "engine"
    return st


def _neg(s):   # newest first
    return tuple(-ord(c) for c in s)


def cards(mods, hidden=()):
    by = collections.defaultdict(list)
    for e in mods:
        if norm(e["name"]) in hidden:
            continue
        by[(norm(e["name"]), e.get("mc", "1.7.10"))].append(e)
    out = collections.defaultdict(collections.Counter)
    for (n, mc), es in by.items():
        out[mc][status_of(es)] += 1
    res = {}
    for mc, c in out.items():
        counted = sum(v for k, v in c.items() if k not in EXTRA)
        runs = sum(c[k] for k in ("loads", "partial", "works", "playable", "perfect"))
        res[mc] = {"mods": counted, "start_and_run_pct": round(100 * runs / counted) if counted else None, "confirmed_working": sum(c[k] for k in GOOD),
                   "native": c["native"], "engine": c["engine"], "loads": c["loads"], "fails": c["fails"], "all_entries": sum(c.values())}
    return res


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--mods", default=str(ROOT / "docs" / "data" / "mods.json"))
    ap.add_argument("--json", action="store_true")
    a = ap.parse_args()
    d = json.loads(pathlib.Path(a.mods).read_text(encoding="utf-8"))
    hidden = {norm(h) for h in d.get("hidden", [])}
    res = cards(d["mods"], hidden)
    if a.json:
        print(json.dumps(res, indent=1))
        return
    print("%-8s %6s %8s %9s %7s %7s %6s %6s" % ("mc", "mods", "start%", "confirmed", "native", "engine", "loads", "fails"))
    for mc in sorted(res):
        r = res[mc]
        print("%-8s %6d %7s%% %9d %7d %7d %6d %6d" % (mc, r["mods"], r["start_and_run_pct"], r["confirmed_working"], r["native"], r["engine"], r["loads"], r["fails"]))


if __name__ == "__main__":
    main()
