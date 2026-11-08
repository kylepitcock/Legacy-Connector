#!/usr/bin/env python3
"""Keeps mod-specific code out of the core (CLAUDE.md: "Fix generically first").

usage: check_mod_specific.py [--update] [--list]

Scans the Java sources (comments ignored) for the names of specific mods (tools/dev/mod-names.txt). Mod-specific code is allowed only in
the designated places, which are not counted:
  - any compat/ package (org/kylepitcock/legacyconnector/compat/, v1_7_10/compat/chromaticraft/, mixin/compat/): per-mod and per-library
                                                   compatibility code (labelled, isolated, easy to remove)
  - legacyconnector/v*/<third-party package>/      the shim of another mod's API (codechicken, cofh, baubles, mcp, thaumcraft, ...): bridging
                                                   an outdated library to its modern replacement is the design, not a mod hack
  - vb1_7_3/*SelfTestB173.java                     development-only actual-mod regression fixtures (explicitly authorized for Beta)
Anywhere else a mention is counted per file and compared with tools/dev/mod-specific-baseline.txt, which records the existing ones. The
check fails when a file is new in the baseline or has more mentions than recorded; it only reports when a file has fewer (run with
--update to lower the baseline, which you should: the numbers are meant to go down). To add mod-specific code, put it under compat/.
"""
import collections, os, pathlib, re, sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
NAMES = ROOT / "tools" / "dev" / "mod-names.txt"
BASELINE = ROOT / "tools" / "dev" / "mod-specific-baseline.txt"
SOURCES = ("src/main/java", "loader/src/main/java")
# first path component under legacyconnector/v*/ that is the old game itself, not another mod's API
GAME_PACKAGES = {"net", "cpw", "org", "com", "java", "javax", "paulscode", "io", "gnu"}


def allowed(rel):
    if re.fullmatch(r"src/main/java/org/kylepitcock/legacyconnector/vb1_7_3/[^/]*SelfTestB173\.java", rel):
        return True  # Actual-mod fixtures run only with the development self-test flag; production bridges remain checked.
    if "/compat/" in "/" + rel:   # org/kylepitcock/legacyconnector/compat/, v1_7_10/compat/chromaticraft/, mixin/compat/, ...
        return True
    m = re.match(r"src/main/java/legacyconnector/v[0-9_]+/([^/]+)/", rel)
    return bool(m and m.group(1) not in GAME_PACKAGES)


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", " ", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


def scan():
    names = [n.strip() for n in NAMES.read_text(encoding="utf-8").splitlines() if n.strip() and not n.startswith("#")]
    pat = re.compile(r"\b(" + "|".join(re.escape(n) for n in names) + r")\b", re.I)
    found = {}
    for root in SOURCES:
        for d, _, files in os.walk(ROOT / root):
            if "worktrees" in d:
                continue
            for f in files:
                if not f.endswith(".java"):
                    continue
                p = pathlib.Path(d) / f
                rel = p.relative_to(ROOT).as_posix()
                if allowed(rel):
                    continue
                n = len(pat.findall(strip_comments(p.read_text(encoding="utf-8", errors="replace")))) + (1 if pat.search(f) else 0)
                if n:
                    found[rel] = n
    return found


def read_baseline():
    out = {}
    if BASELINE.exists():
        for line in BASELINE.read_text(encoding="utf-8").splitlines():
            if line and not line.startswith("#"):
                path, n = line.rsplit("\t", 1)
                out[path] = int(n)
    return out


def main():
    found = scan()
    if "--update" in sys.argv:
        BASELINE.write_text("# Existing mentions of specific mods outside the designated places (see check_mod_specific.py). Only ever lower these.\n"
                            + "".join(f"{p}\t{n}\n" for p, n in sorted(found.items())), encoding="utf-8")
        print(f"baseline written: {len(found)} files, {sum(found.values())} mentions")
        return
    if "--list" in sys.argv:
        for p, n in sorted(found.items(), key=lambda kv: -kv[1]):
            print(n, p)
        return
    base = read_baseline()
    bad = [(p, n, base.get(p, 0)) for p, n in sorted(found.items()) if n > base.get(p, 0)]
    better = [(p, base[p], found.get(p, 0)) for p in sorted(base) if found.get(p, 0) < base[p]]
    for p, n, was in bad:
        print(f"MOD-SPECIFIC CODE: {p}: {n} mention(s) of specific mods" + (f" (baseline {was})" if was else " (new)"))
    if better:
        print(f"{len(better)} file(s) have fewer mentions than the baseline: run tools/dev/check_mod_specific.py --update to lower it")
    if bad:
        print("\nMod-specific code belongs under org/kylepitcock/legacyconnector/compat/ (labelled, isolated, keyed by mod id); "
              "everything else stays generic. See CLAUDE.md, 'Fix generically first'.")
        sys.exit(1)
    print(f"ok: {sum(found.values())} baseline mentions in {len(found)} files, none new")


if __name__ == "__main__":
    main()
