#!/usr/bin/env python3
"""Adds corpus results to docs/data/mods.json: every legacy mod of a corpus run (tools/dev/run-corpus.sh) that is not
already listed becomes "loads" (registered at startup without errors) or "fails" (listed as failed to load, with the reason).

usage: from_corpus.py <legacyconnector-report.txt> <latest.log> [mc-version] [date] [--dry-run] [--force]
--dry-run prints what would change (added entries, refreshed entries with a changed status or version) and writes nothing.
Without --force the tool refuses to write when a refreshed corpus entry has another mod VERSION than this run (a run of a different mod set,
e.g. run-subtest, once overwrote about 150 entries that had to be reverted): look at the --dry-run output first.
Only jars the report marks with mc-version are taken (a pack can mix versions: run once per version).
Maintainer entries (hand written) are never overwritten; earlier corpus entries (source "corpus") are refreshed with this run.
Player reports (docs/data/reports.json) always outrank both on the site.
"""
import json, pathlib, re, sys

FLAGS = {a for a in sys.argv[1:] if a.startswith("--")}
ARGS = [a for a in sys.argv[1:] if not a.startswith("--")]
DRY, FORCE = "--dry-run" in FLAGS, "--force" in FLAGS
report, log = pathlib.Path(ARGS[0]), pathlib.Path(ARGS[1])
mc = ARGS[2] if len(ARGS) > 2 else "1.7.10"
date = ARGS[3] if len(ARGS) > 3 else "2026-10-23"
DATA = pathlib.Path(__file__).resolve().parents[2] / "docs" / "data" / "mods.json"


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


mods = {}   # modern id -> (name, version, jar)
jar = None
for line in report.read_text(encoding="utf-8", errors="replace").splitlines():
    m = re.match(r"^(\S.*\.jar)\s+\[(\d[\d.]*)?", line)
    if m:
        # jars of another legacy version are left to a run with that version
        jar = m.group(1) if m.group(2) in (None, mc) else None
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

LOADS_NOTE = {
    "1.7.10": "Loads at startup without errors (blocks, items and entities register). Gameplay not verified yet.",
    "1.2.5": "Loads at startup on a server without errors (1.2.5 is experimental and opt-in; many game members are still empty placeholders). Gameplay not verified yet.",
    "1.12.2": "Loads at startup without errors (blocks, items and recipes register). 1.12.2 support is in alpha; gameplay not verified yet.",
}
data = json.loads(DATA.read_text(encoding="utf-8"))
before = {(e["name"], e.get("mc", "1.7.10")): e for e in data["mods"]}
# entries are per Minecraft version: a 1.12.2 run never replaces or hides the 1.7.10 entry of the same mod
same_mc = [e for e in data["mods"] if e.get("mc", "1.7.10") == mc]
refreshed = {norm(e["name"]) for e in same_mc if e.get("source") == "corpus"} & {norm(n) for n, _, _ in mods.values()}
data["mods"] = [e for e in data["mods"] if not (e.get("mc", "1.7.10") == mc and e.get("source") == "corpus" and norm(e["name"]) in refreshed)]
listed = {norm(e["name"]) for e in data["mods"] if e.get("mc", "1.7.10") == mc}
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
        entry.update(status="loads", notes=LOADS_NOTE.get(mc, LOADS_NOTE["1.7.10"]))
    data["mods"].append(entry)
    listed.add(norm(name))
    added += 1

data["mods"].sort(key=lambda e: e["name"].lower())
# mods listed in "hidden" (nothing to do on 1.21.1, or client mods with their own 1.21.1 version) stay off the site
hidden = {norm(h) for h in data.get("hidden", [])}
data["mods"] = [e for e in data["mods"] if norm(e["name"]) not in hidden
                and norm(e["name"]) not in {norm(h) for h in data.get("hidden_by_mc", {}).get(e.get("mc", "1.7.10"), [])}]
after = {(e["name"], e.get("mc", "1.7.10")): e for e in data["mods"]}
changed = [(k, before[k], after[k]) for k in before if k in after and (before[k].get("status"), before[k].get("version")) != (after[k].get("status"), after[k].get("version"))]
removed = [k for k in before if k not in after]
version_changes = [c for c in changed if c[1].get("version") != c[2].get("version")]
print(f"{added} added, {len(changed)} refreshed with another status or version ({len(version_changes)} with another version), {len(removed)} removed (hidden or replaced), {len(data['mods'])} total")
for (name, m), old, new in changed[:40]:
    print(f"  CHANGE {name} [{m}]: {old.get('status')} {old.get('version')} -> {new.get('status')} {new.get('version')}")
if DRY:
    print("dry run: nothing written")
    sys.exit(0)
if version_changes and not FORCE:
    sys.exit(f"refusing to write: {len(version_changes)} refreshed entries have another mod version than this run (different mod set?). Check --dry-run, or pass --force.")
DATA.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
print(f"{added} mod(s) added, {len(data['mods'])} total")
