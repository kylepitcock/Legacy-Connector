#!/usr/bin/env python3
"""Adds corpus results to docs/data/mods.json: every legacy mod of a corpus run (tools/dev/run-corpus.sh) that is not
already listed becomes "loads" (registered at startup without errors) or "fails" (listed as failed to load, with the reason).

usage: from_corpus.py <legacyconnector-report.txt> <latest.log> [mc-version] [date]
Maintainer entries (hand written) are never overwritten.
"""
import json, pathlib, re, sys

report, log = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
mc = sys.argv[3] if len(sys.argv) > 3 else "1.7.10"
date = sys.argv[4] if len(sys.argv) > 4 else "2026-10-23"
DATA = pathlib.Path(__file__).resolve().parents[2] / "docs" / "data" / "mods.json"


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


mods = {}   # modern id -> (name, version, jar)
jar = None
for line in report.read_text(encoding="utf-8", errors="replace").splitlines():
    m = re.match(r"^(\S.*\.jar)\s+\[", line)
    if m:
        jar = m.group(1)
        continue
    m = re.match(r"^  mod (.+?) -> (\S+)\s+\((.*)\)\s*$", line)
    if m and jar:
        label = m.group(3)
        v = re.match(r"^(.*?)\s+(\d[^ ]*(?: .*)?)$", label)
        name, version = (v.group(1), v.group(2)) if v else (label, "")
        mods[m.group(2)] = (name.strip() or m.group(1), version.strip(), jar)

failed = {}  # normalised legacy id -> reason
text = log.read_text(encoding="utf-8", errors="replace")
for m in re.finditer(r"\]:\s+(\S+): Legacy mod \S+ \([^)]*\) failed during (\w+): (.*)", text):
    legacy, phase, reason = m.group(1), m.group(2), m.group(3)
    reason = re.sub(r"^java\.lang\.RuntimeException: ", "", reason)
    reason = re.sub(r"\s+", " ", reason)[:160]
    failed.setdefault(norm(legacy), f"Fails during {phase}: {reason}")

data = json.loads(DATA.read_text(encoding="utf-8"))
listed = {norm(e["name"]) for e in data["mods"]}
added = 0
for modern, (name, version, jarname) in sorted(mods.items()):
    if norm(name) in listed or norm(modern) in listed:
        continue
    reason = failed.get(norm(modern))
    if reason is None:
        # legacy ids that differ from the modern id in case only
        reason = next((r for k, r in failed.items() if k == norm(name)), None)
    entry = {"name": name, "version": version, "mc": mc, "source": "corpus", "date": date}
    if reason:
        entry.update(status="fails", notes=reason)
    else:
        entry.update(status="loads", notes="Loads at startup without errors (blocks, items and entities register). Gameplay not verified yet.")
    data["mods"].append(entry)
    listed.add(norm(name))
    added += 1

data["mods"].sort(key=lambda e: e["name"].lower())
DATA.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
print(f"{added} mod(s) added, {len(data['mods'])} total")
