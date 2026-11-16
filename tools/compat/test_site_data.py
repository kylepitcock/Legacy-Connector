#!/usr/bin/env python3
"""Consistency test of the compatibility site: every Minecraft version the data files carry is known to the site (docs/assets/site.js), every
supported version has data, and the places that enumerate versions (issue template, probe workflow) list the versions that have results.

usage: py -3 -I tools/compat/test_site_data.py   (exit 1 on a failure; no network, no game)
"""
import json, pathlib, re, sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
DOCS = ROOT / "docs"
errors = []


def check(ok, msg):
    if not ok:
        errors.append(msg)


mods = json.loads((DOCS / "data" / "mods.json").read_text(encoding="utf-8"))
probe = json.loads((DOCS / "data" / "probe.json").read_text(encoding="utf-8"))
js = (DOCS / "assets" / "site.js").read_text(encoding="utf-8")


def js_list(name):
    m = re.search(r"var %s = \[([^\]]*)\]" % name, js)
    assert m, f"site.js has no {name} list"
    return re.findall(r'"([^"]+)"', m.group(1))


supported, planned = js_list("SUPPORTED"), js_list("PLANNED")
known = set(supported) | set(planned)
check(len(set(supported) & set(planned)) == 0, "a version is both SUPPORTED and PLANNED in site.js")

mods_mc = {e.get("mc", "1.7.10") for e in mods["mods"]}
probe_mc = {r["mc"] for r in probe["runs"]}

for v in sorted(mods_mc | probe_mc):
    check(v in known, f"version {v} has data but is not in SUPPORTED/PLANNED of site.js (a report could not be filed under it)")
for v in supported:
    check(v in mods_mc, f"supported version {v} has no entry in mods.json")
    check(v in probe_mc, f"supported version {v} has no run in probe.json")
for v in sorted(probe_mc):
    check(any(e.get("mc", "1.7.10") == v for e in mods["mods"]), f"probe.json has runs of {v} but mods.json has no entry for it")

# shape the site reads
for e in mods["mods"]:
    check(all(k in e for k in ("name", "status")), f"mods.json entry without name/status: {e.get('name')}")
for r in probe["runs"]:
    check(all(k in r for k in ("mc", "date", "version", "counts", "mods")), f"probe.json run without the required fields: {r.get('mc')} {r.get('date')}")
    for row in r["mods"]:
        check(len(row) >= 4 and row[2] in ("links", "stubs", "gap_shim", "gap_mod", "gap_lib", "obsolete", "unresolved"), f"{r['mc']} {r['date']}: odd row {row[:4]}")
    if "stubCounts" in r:
        check(isinstance(r["stubCounts"], dict) and all(isinstance(n, int) for n in r["stubCounts"].values()), f"{r['mc']} {r['date']}: stubCounts is not {{name: int}}")

# the version note of the experimental versions must exist in site.js (1.2.5 says "experimental, opt-in" and warns about placeholders)
for v in ("1.2.5",):
    if v in mods_mc | probe_mc:
        check(f'mc === "{v}"' in js, f"site.js has no version note for {v}")
        check("opt-in" in js and "placeholder" in js, "site.js lost the experimental/placeholder wording")

# the issue template lists every version that has data
tpl = (ROOT / ".github" / "ISSUE_TEMPLATE" / "compatibility_report.yml").read_text(encoding="utf-8")
options = re.findall(r'^\s*- "([^"]+)"', tpl, re.M)
for v in sorted(mods_mc | probe_mc):
    bare = v[1:] if v.startswith("b") else v
    check(any(bare in o for o in options), f"issue template dropdown has no option for {v}")

# the weekly probe workflow probes every version of the probe history that is fetched from Modrinth or MCArchive
wf = (ROOT / ".github" / "workflows" / "probe.yml").read_text(encoding="utf-8")
for v in ("1.7.10", "1.12.2", "1.6.4", "1.5.2", "1.4.7", "b1.7.3", "1.20.1", "1.2.5"):
    check(v in wf, f"probe.yml does not mention {v}")

if errors:
    print("\n".join("FAIL " + e for e in errors))
    sys.exit(1)
print(f"site data ok: mods.json {sorted(mods_mc)}, probe.json {sorted(probe_mc)}, site knows {len(known)} versions")
