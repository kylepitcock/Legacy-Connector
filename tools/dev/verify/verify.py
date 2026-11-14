#!/usr/bin/env python3
"""Log parsing, baseline comparison and report formatting for tools/dev/verify.sh (standard library only).

  verify.py parse <latest.log> [<crash-dir>]                  prints the metrics as JSON
  verify.py compare <baseline.json> <suite> <metrics.json>    prints {"status", "regressions"} as JSON
  verify.py report <baseline.json> <results.json> [--update]  prints the table, exit 1 when a suite failed
"""
import json
import os
import re
import sys

BAD = ["mods_failed", "mods_failed_1201", "blocks_problems", "entities_failed", "entities_not_spawned", "silent_failed", "crash_reports", "verify_errors"]
GOOD = ["mods_loaded_1201", "blocks_ok", "entities_alive", "placement_placed", "content_items", "content_blocks", "content_entities", "reached_selftest"]
TOLERANCE = 0.02
# alive/removed after the entity self test is not deterministic (random despawns; Traincraft varies 308 to 319 between identical runs)
TOLERANCES = {"entities_alive": 0.10}
COLUMNS = ["mods_failed", "mods_failed_1201", "mods_loaded_1201", "blocks_placed", "blocks_ok", "blocks_problems", "entities_alive", "entities_failed", "entities_not_spawned",
           "silent_failed", "placement_placed", "placement_total", "content_items", "content_blocks", "content_entities",
           "crash_reports", "verify_errors", "reached_selftest"]
SHORT = {"mods_failed": "modF", "mods_failed_1201": "modF21", "mods_loaded_1201": "mod21", "blocks_placed": "blkP", "blocks_ok": "blkOk", "blocks_problems": "blkBad", "entities_alive": "entOk",
         "entities_failed": "entF", "entities_not_spawned": "entNS", "silent_failed": "silF", "placement_placed": "plcP",
         "placement_total": "plcT", "content_items": "items", "content_blocks": "blocks", "content_entities": "ents",
         "crash_reports": "crash", "verify_errors": "verr", "reached_selftest": "test"}

# (regex, [(metric, group)])
PATTERNS = [
    (r"Legacy 1\.20\.1 mods: (\d+) loaded, (\d+) failed", [("mods_loaded_1201", 1), ("mods_failed_1201", 2)]),
    (r"Legacy mods loaded in \d+ ms \((\d+) failed\)", [("mods_failed", 1)]),
    (r"BLOCK SELF TEST: (\d+) placed, (\d+) still there after 60 ticks, (\d+) activated, (\d+) with problems",
     [("blocks_placed", 1), ("blocks_ok", 2), ("blocks_problems", 4)]),
    (r"ENTITY SELF TEST: (\d+) alive, (\d+) removed, (\d+) failed, (\d+) not spawned of (\d+) types",
     [("entities_alive", 1), ("entities_failed", 3), ("entities_not_spawned", 4)]),
    (r"SILENT FAILURE CHECKS: (\d+) passed, (\d+) failed", [("silent_failed", 2)]),
    (r"1\.12\.2 item placement: (\d+) of (\d+) placed", [("placement_placed", 1), ("placement_total", 2)]),
    (r"Legacy content: \d+ mods, (\d+) items, (\d+) blocks, (\d+) entity types", [("content_items", 1), ("content_blocks", 2), ("content_entities", 3)]),
]


def parse_log(text, crash_dir=None):
    m = {}
    compiled = [(re.compile(p), g) for p, g in PATTERNS]
    for line in text.splitlines():
        for rx, groups in compiled:
            hit = rx.search(line)
            if hit:
                for name, idx in groups:
                    m[name] = int(hit.group(idx))  # the last occurrence wins
    m["verify_errors"] = text.count("VerifyError")
    m["reached_selftest"] = 1 if ("BLOCK SELF TEST" in text or "SILENT FAILURE CHECKS" in text) else 0
    failed = failed_mods_1201(text)
    if failed:
        # FML aborts the start at the first phase with errors: the mods it names are the failed ones (id -> first line of the cause)
        m["mods_failed_1201"] = len(failed)
        m["failed_mods_1201"] = failed
    n = 0
    if crash_dir and os.path.isdir(crash_dir):
        n = len([f for f in os.listdir(crash_dir) if os.path.isfile(os.path.join(crash_dir, f))])
    m["crash_reports"] = n
    return m


def failed_mods_1201(text):
    """The "Loading errors encountered:" block FML logs when mod loading fails: {mod id or name: first line of the cause}."""
    out = {}
    lines = text.splitlines()
    for i, line in enumerate(lines):
        if "Loading errors encountered:" not in line:
            continue
        for j in range(i + 1, len(lines)):
            hit = re.match(r"^	- (.*)$", lines[j])
            if not hit:
                if lines[j].strip() == "" or lines[j].startswith("	at "):
                    break
                continue
            msg = hit.group(1)
            ident = re.search(r"\(([\w.\-]+)\) has failed to load correctly", msg)
            ident = ident or re.search(r"\(([\w.\-]+)\) encountered an error", msg)
            mod = re.match(r"Mod (\S+) requires", msg)
            key = ident.group(1) if ident else (mod.group(1) if mod else msg[:60])
            cause = msg
            if ident and j + 1 < len(lines) and lines[j + 1].startswith("	  "):
                cause = lines[j + 1].strip()
            out.setdefault(key, cause[:300])
        break
    return out


def compare(baseline, metrics):
    """baseline: {metric: value} or None. Returns (status, [regression strings])."""
    regs = []
    if not metrics.get("reached_selftest"):
        regs.append("reached_selftest: baseline %s, measured 0" % (baseline or {}).get("reached_selftest", 1))
    if baseline is None:
        return ("FAIL" if regs else "NEW"), regs
    for k in BAD:
        if k in baseline and metrics.get(k, 0) > baseline[k]:
            regs.append("%s: baseline %s, measured %s" % (k, baseline[k], metrics.get(k, 0)))
    for k in GOOD:
        if k == "reached_selftest" or k not in baseline:
            continue
        b, v = baseline[k], metrics.get(k, 0)
        if v < b * (1 - TOLERANCES.get(k, TOLERANCE)):
            regs.append("%s: baseline %s, measured %s" % (k, b, v))
    return ("FAIL" if regs else "PASS"), regs


def format_table(rows):
    """rows: list of dicts {suite, status, metrics, regressions, note}."""
    head = ["suite", "status"] + [SHORT[c] for c in COLUMNS]
    table = [head]
    for r in rows:
        mm = r.get("metrics") or {}
        table.append([r["suite"], r["status"]] + [str(mm[c]) if c in mm else "-" for c in COLUMNS])
    widths = [max(len(row[i]) for row in table) for i in range(len(head))]
    lines = ["  ".join(c.ljust(widths[i]) for i, c in enumerate(row)).rstrip() for row in table]
    for r in rows:
        for reg in r.get("regressions") or []:
            lines.append("  %s REGRESSION %s" % (r["suite"], reg))
        if r.get("note"):
            lines.append("  %s %s: %s" % (r["suite"], r["status"], r["note"]))
    return "\n".join(lines)


def load(path, default):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return default


def main(argv):
    if len(argv) >= 3 and argv[1] == "parse":
        with open(argv[2], encoding="utf-8", errors="replace") as f:
            text = f.read()
        print(json.dumps(parse_log(text, argv[3] if len(argv) > 3 else None), indent=1, sort_keys=True))
        return 0
    if len(argv) >= 5 and argv[1] == "compare":
        base = load(argv[2], {}).get(argv[3])
        status, regs = compare(base, load(argv[4], {}))
        print(json.dumps({"status": status, "regressions": regs}))
        return 0
    if len(argv) >= 4 and argv[1] == "report":
        base = load(argv[2], {})
        results = load(argv[3], [])
        update = "--update" in argv
        rows = []
        for r in results:
            if r.get("skipped"):
                rows.append({"suite": r["suite"], "status": "SKIP", "note": "missing jar: " + r["skipped"]})
                continue
            if update and r["metrics"].get("reached_selftest"):
                base[r["suite"]] = dict(r["metrics"])
                rows.append({"suite": r["suite"], "status": "PASS", "metrics": r["metrics"], "regressions": [], "note": "baseline updated"})
                continue
            status, regs = compare(base.get(r["suite"]), r["metrics"])
            if r.get("note"):
                regs = regs + [r["note"]]
                status = "FAIL"
            rows.append({"suite": r["suite"], "status": status, "metrics": r["metrics"], "regressions": regs})
        if update:
            with open(argv[2], "w", encoding="utf-8") as f:
                json.dump(base, f, indent=1, sort_keys=True)
                f.write("\n")
        print(format_table(rows))
        return 1 if any(r["status"] == "FAIL" for r in rows) else 0
    sys.stderr.write(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
