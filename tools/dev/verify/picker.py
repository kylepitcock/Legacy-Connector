#!/usr/bin/env python3
"""Jar picker of tools/dev/verify.sh: turns the lines of suite files into jar paths, aware of the Minecraft version of each line.

A suite line is `<ver> <fragment> [comment]` (`lib <fragment>` picks a native library from the library folder). The fragment is matched
case-insensitively against file names (`[` and `]` in a fragment match any single character, like the old glob). Each jar has an inferred
Minecraft version: from a version token in its file name (`1.12.2`, `1.7.10`, `b1.7.3`, ...), else from the folder it lies in (`test-mods-164`,
`1.12.2 test mods`, `run-subtest/mods/1.6.4`, ...), else none (an unlabelled folder such as test-mods-issues).

Pick for a line with version V: jars labelled V win (search-folder order), then unlabelled jars; jars labelled with ANOTHER version are never
picked (a 1.4.7 Thermal Expansion for a 1.12.2 line). Status per line:
  ok       a jar was picked (a note lists other candidates when the pick was not unique: look at it)
  missing  no jar of that version matches (note lists jars of other versions that matched)
  setup    the pick is ambiguous in a way the picker cannot resolve: a line without a version (lib) matches jars of more than one Minecraft
           version, or several differently named unlabelled jars compete for a versioned line

usage: picker.py resolve --search <dir> [<dir> ...] [--lib <dir>] [--big <dir>] -- <suite.txt> [<suite.txt> ...]
prints TSV lines `suite<TAB>status<TAB>ver<TAB>fragment<TAB>path<TAB>note`.
"""
import os, re, sys

VERSIONS = ["1.12.2", "1.7.10", "1.6.4", "1.5.2", "1.4.7", "1.2.5", "1.20.1", "1.19.2", "1.18.2", "1.16.5", "b1.7.3"]
NAME_TOKEN = re.compile(r"(?<![\d.])(1\.(?:12\.2|7\.10|6\.4|5\.2|4\.7|2\.5|20\.1|19\.2|18\.2|16\.5)|b1\.7\.3)(?![\d])")
# folder component (lower case) -> version
FOLDER = {
    "test-mods-1122": "1.12.2", "1.12.2 test mods": "1.12.2", "1.12.2": "1.12.2", "1.12.2-content": "1.12.2", "test-mods-1122-fetched": "1.12.2", "mods-1122": "1.12.2",
    "test-mods-164": "1.6.4", "1.6.4": "1.6.4", "test-mods-152": "1.5.2", "1.5.2": "1.5.2", "test-mods-147": "1.4.7", "1.4.7": "1.4.7",
    "1.2.5": "1.2.5", "test-mods-1201-a": "1.20.1", "test-mods-1201-terramity": "1.20.1", "1.20.1": "1.20.1",
    "test-mods-b173": "b1.7.3", "b1.7.3": "b1.7.3", "test-mods-1710-extra": "1.7.10", "1.7.10": "1.7.10", "mods-1710": "1.7.10",
}


ATM9 = None


def infer(path, atm9=None):
    atm9 = atm9 or ATM9
    m = NAME_TOKEN.search(os.path.basename(path))
    if m:
        return m.group(1)
    if atm9 and os.path.normpath(path).lower().startswith(os.path.normpath(atm9).lower()):
        return "1.20.1"
    parts = re.split(r"[\\/]", path)
    for comp in reversed(parts[:-1]):
        if comp.lower() in FOLDER:
            return FOLDER[comp.lower()]
    return None


def matcher(frag):
    rx = "".join("." if ch in "[]" else (".*" if ch == "*" else re.escape(ch)) for ch in frag)
    return re.compile(rx, re.I)


def listing(root):
    out = []
    for d, _, files in os.walk(root):
        for f in files:
            if f.lower().endswith((".jar", ".zip", ".litemod")):
                out.append(os.path.join(d, f).replace("\\", "/"))
    return sorted(out, key=str.lower)


def pick(ver, frag, roots, lib_root=None):
    """roots: ordered list of (path, files). Returns (status, path, note)."""
    rx = matcher(frag)
    cands = []
    for r, files in (roots if ver != "lib" else [(lib_root, listing(lib_root) if lib_root and os.path.isdir(lib_root) else [])]):
        for p in files:
            if rx.search(os.path.basename(p)):
                cands.append((p, infer(p)))
    if not cands:
        return "missing", "", ""
    real = ver in VERSIONS
    if not real:
        versions = {v for _, v in cands if v}
        names = {os.path.basename(p).lower() for p, _ in cands}
        if len(versions) > 1:
            return "setup", cands[0][0], "fragment matches jars of %d Minecraft versions (%s) and the line gives none: %s" % (
                len(versions), ", ".join(sorted(versions)), "; ".join(p for p, _ in cands[:4]))
        return "ok", cands[0][0], ("also matches " + "; ".join(p for p, _ in cands[1:4])) if len(names) > 1 else ""
    exact = [c for c in cands if c[1] == ver]
    other = [c for c in cands if c[1] and c[1] != ver]
    unknown = [c for c in cands if not c[1]]
    if exact:
        chosen = exact[0]
        names = {os.path.basename(p).lower() for p, _ in exact}
        note = ("also matches " + "; ".join(p for p, _ in exact[1:4])) if len(names) > 1 else ""
        return "ok", chosen[0], note
    if unknown:
        names = {os.path.basename(p).lower() for p, _ in unknown}
        note = ""
        if len(names) > 1:
            return "setup", unknown[0][0], "%d differently named jars of no known Minecraft version match (first wins, may be wrong): %s" % (
                len(names), "; ".join(p for p, _ in unknown[:4]))
        if other:
            note = "unlabelled pick; jars of other versions also match and were ignored: " + "; ".join(p for p, _ in other[:3])
        return "ok", unknown[0][0], note
    return "missing", "", "only jars of other Minecraft versions match: " + "; ".join("%s (%s)" % (p, v) for p, v in other[:4])


def parse_suite(path):
    for line in open(path, encoding="utf-8", errors="replace").read().splitlines():
        p = line.strip().split()
        if len(p) < 2 or p[0].startswith("#"):
            continue
        yield p[0], p[1]


def main(argv):
    if len(argv) < 2 or argv[1] != "resolve":
        sys.stderr.write(__doc__)
        return 2
    search, lib, big, atm9, suites = [], None, None, None, []
    mode = None
    for a in argv[2:]:
        if a in ("--search", "--lib", "--big", "--atm9", "--"):
            mode = a
            continue
        if mode == "--search":
            search.append(a)
        elif mode == "--lib":
            lib = a
        elif mode == "--big":
            big = a
        elif mode == "--atm9":
            atm9 = a
        else:
            suites.append(a)
    global ATM9
    ATM9 = atm9
    ordered = [d for d in search + ([big] if big else []) + ([atm9] if atm9 else []) if os.path.isdir(d)]
    roots = [(d, listing(d)) for d in ordered]
    for s in suites:
        name = os.path.splitext(os.path.basename(s))[0]
        for ver, frag in parse_suite(s):
            status, path, note = pick(ver, frag, roots, lib)
            print("\t".join([name, status, ver, frag, path, note.replace("\t", " ")]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
