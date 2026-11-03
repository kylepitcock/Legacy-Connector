#!/usr/bin/env python3
"""Link probe: translates a directory of legacy mod jars offline (no game) and adds the result to docs/data/mods.json,
like the Sinytra Connector compatibility checker does for Fabric.

usage: probe.py <jar-dir> [--mc 1.7.10] [--date YYYY-MM-DD] [--version <layer version>] [--json out.json] [--dry-run] [--no-history]
       probe.py --from-json probe.json [...]     (convert an earlier run without translating again)

The jars are only read (test-mods/ stays gitignored, never commit or redistribute them); only results are written.
All jars are translated as ONE mod set so mods that extend each other link. Per jar, ./gradlew checkLegacyMod yields:
  links       every reference resolves (missing legacy API is synthesized)  -> status "loads"
  unresolved  references the shim, the synthesizer and the libraries cannot satisfy -> status "fails"
  obsolete    engine patch with nothing to do on 1.21.1 -> not listed
A probe proves "translates and links", never "works in game": entries get source "probe", which the site ranks below
maintainer, client and corpus entries (a real run of the same mod always wins) and which never overwrites them.
Every run is also appended to docs/data/probe.json (counts + per-mod list), which the pie chart page docs/probe.html shows.
"""
import argparse, datetime, json, pathlib, re, subprocess, sys, tempfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
DATA = ROOT / "docs" / "data" / "mods.json"
HISTORY = ROOT / "docs" / "data" / "probe.json"   # one entry per run: the pie chart page (docs/probe.html) reads it
KEEP_MODS = 10   # runs per Minecraft version that keep their per-mod list; older ones keep only the counts


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


def run_probe(jar_dir, out):
    jars = sorted(pathlib.Path(jar_dir).rglob("*.jar"))
    if not jars:
        sys.exit(f"no jars in {jar_dir}")
    cmd = [str(ROOT / "gradlew"), "-q", "checkLegacyMod", f"-PlegacyJar={','.join(map(str, jars))}", f"-PcheckJson={out}"]
    print(f"probing {len(jars)} jar(s) as one mod set ...", flush=True)
    r = subprocess.run(cmd, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    if r.returncode != 0 or not pathlib.Path(out).exists():
        sys.exit(r.stdout[-3000:] + "\nprobe failed")


def note(rec, layer):
    base = f"Probe {layer}: " if layer else "Probe: "
    if rec["result"] == "links":
        extra = ""
        if rec.get("synthesizedMembers") or rec.get("synthesizedClasses"):
            extra = (f" {rec.get('synthesizedClasses', 0)} legacy API class(es) and {rec.get('synthesizedMembers', 0)} "
                     "member(s) are synthesized (they link, behaviour is default).")
        return f"{base}translates offline and all {rec.get('classes', 0)} classes link.{extra} Not started in game: this only proves the jar translates."
    miss = ", ".join(rec.get("missing", [])[:5])
    more = f" (+{rec['unresolved'] - 5} more)" if rec["unresolved"] > 5 else ""
    return f"{base}{rec['unresolved']} reference(s) cannot be linked: {miss}{more}."


def merge(records, mc, date, layer):
    data = json.loads(DATA.read_text(encoding="utf-8"))
    listed = {}
    for e in data["mods"]:
        if e.get("mc", "1.7.10") == mc:
            listed.setdefault(norm(e["name"]), []).append(e)
    hidden = {norm(h) for h in data.get("hidden", [])}
    added = refreshed = skipped = 0
    for rec in records:
        if rec["result"] not in ("links", "unresolved") or rec.get("mc") != mc:
            continue
        status = "loads" if rec["result"] == "links" else "fails"
        for m in rec["mods"]:
            name = (m.get("name") or m["id"]).strip()
            key = norm(name)
            if key in hidden:
                continue
            existing = listed.get(key, [])
            if any(e.get("source") != "probe" for e in existing):
                skipped += 1   # a maintainer, client or corpus result is better than a link probe
                continue
            entry = {"name": name, "version": (m.get("version") or "").strip(), "mc": mc, "source": "probe", "date": date,
                     "status": status, "notes": note(rec, layer)}
            if existing:
                existing[0].update(entry)
                refreshed += 1
            else:
                data["mods"].append(entry)
                listed[key] = [entry]
                added += 1
    if added:
        data["mods"].sort(key=lambda e: e["name"].lower())
    return data, added, refreshed, skipped


def history(records, mc, date, layer):
    """Appends this run to docs/data/probe.json: counts per result and the per-jar list (mod, result, first unresolved reference)."""
    rows, counts, seen = [], {"links": 0, "unresolved": 0, "obsolete": 0}, set()
    for r in records:
        if r.get("mc") != mc or r["result"] not in counts or not r.get("mods"):
            continue   # other versions, non-mod jars and libraries without a mod are not part of the statistics
        m = r["mods"][0]
        key = ((m.get("name") or m["id"]).strip().lower(), (m.get("version") or "").strip())
        if key in seen:
            continue   # the same mod from two sources (a corpus and Modrinth) counts once
        seen.add(key)
        counts[r["result"]] += 1
        rows.append([(m.get("name") or m["id"]).strip(), (m.get("version") or "").strip(), r["result"],
                     r.get("reason") or (r.get("missing") or [""])[0]])
    rows.sort(key=lambda x: x[0].lower())
    h = json.loads(HISTORY.read_text(encoding="utf-8")) if HISTORY.exists() else {"runs": []}
    h["runs"] = [x for x in h["runs"] if not (x["mc"] == mc and x["date"] == date and x["version"] == layer)]
    h["runs"].append({"mc": mc, "date": date, "version": layer, "counts": counts, "mods": rows})
    h["runs"].sort(key=lambda x: (x["mc"], x["date"], x["version"]))
    seen = {}
    for x in reversed(h["runs"]):
        seen[x["mc"]] = seen.get(x["mc"], 0) + 1
        if seen[x["mc"]] > KEEP_MODS:
            x["mods"] = []
    return h, counts


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("jar_dir", nargs="?")
    ap.add_argument("--mc", default="1.7.10")
    ap.add_argument("--date", default=datetime.date.today().isoformat())
    ap.add_argument("--version", default="", help="layer version shown in the notes")
    ap.add_argument("--json", help="keep the raw probe records here")
    ap.add_argument("--from-json", help="skip translation, convert this probe file")
    ap.add_argument("--no-history", action="store_true", help="do not add the run to docs/data/probe.json")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()
    if a.from_json:
        path = a.from_json
    elif a.jar_dir:
        path = a.json or tempfile.mktemp(suffix=".json")
        run_probe(a.jar_dir, path)
    else:
        ap.error("jar directory or --from-json required")
    records = json.loads(pathlib.Path(path).read_text(encoding="utf-8"))
    counts = {}
    for r in records:
        counts[r["result"]] = counts.get(r["result"], 0) + 1
    print("jars:", ", ".join(f"{v} {k}" for k, v in sorted(counts.items())))
    data, added, refreshed, skipped = merge(records, a.mc, a.date, a.version)
    print(f"{added} added, {refreshed} refreshed, {skipped} kept (better source), {len(data['mods'])} total")
    if not a.dry_run and (added or refreshed):
        DATA.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    if not a.no_history:
        h, c = history(records, a.mc, a.date, a.version)
        print(f"run {a.mc} {a.version or '-'} {a.date}: {c['links']} link, {c['unresolved']} unresolved, {c['obsolete']} obsolete")
        if not a.dry_run:
            HISTORY.write_text(json.dumps(h, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")


main()
