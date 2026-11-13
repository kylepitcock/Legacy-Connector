#!/usr/bin/env python3
"""List archive mods with no base class edits, from the read-only audit.

Runs tools/dev/audit_b173_archive.py (needs build/devtools/b1.7.3: run tools/fetch_devtools.sh once),
then prints the units whose status is "no-base-class-replacements", mods with an entry point
first. Usage: scan_b173_no_base_edits.py <archive-dir> [--out DIR] [--csv FILE] [--reuse]
"""
import argparse, csv, subprocess, sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]

def main():
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    p = argparse.ArgumentParser()
    p.add_argument('archive', type=Path)
    p.add_argument('--out', type=Path, default=REPO / 'build/b173-archive-audit')
    p.add_argument('--csv', type=Path, help='also write the list here')
    p.add_argument('--reuse', action='store_true', help='read an existing inventory.csv instead of rescanning')
    a = p.parse_args()
    inventory = a.out / 'inventory.csv'
    if not (a.reuse and inventory.exists()):
        subprocess.run([sys.executable, str(REPO / 'tools/dev/audit_b173_archive.py'), str(a.archive), '--out', str(a.out)],
                       check=True, stdout=subprocess.DEVNULL)
    with inventory.open(encoding='utf-8-sig', newline='') as f:
        rows = [r for r in csv.DictReader(f) if r['status'] == 'no-base-class-replacements']
    rows.sort(key=lambda r: (not r['entrypoints'], r['archive'].lower(), r['root']))
    for r in rows:
        print(f"{r['archive']}{' [' + r['root'] + ']' if r['root'] else ''}\t{r['entrypoints']}\t{r['apis']}")
    print(f'{len(rows)} units without base class edits', file=sys.stderr)
    if a.csv:
        with a.csv.open('w', encoding='utf-8', newline='') as f:
            w = csv.DictWriter(f, fieldnames=rows[0].keys() if rows else [])
            w.writeheader(); w.writerows(rows)

if __name__ == '__main__':
    main()
