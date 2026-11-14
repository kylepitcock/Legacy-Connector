#!/usr/bin/env python3
"""Generates the 1.20.1 batch suites `atm9-1201-<n>.txt` for tools/dev/verify.sh from the newest ATM 9 probe run.

usage: gen_suites_1201.py [--atm9 DIR] [--atm10 DIR] [--per N] [--out DIR] [--list]

Takes the mods of docs/data/probe.json (newest run with mc 1.20.1 and source atm9) whose status is "links", finds their jars in the (read-only) ATM 9
folder through the displayName of META-INF/mods.toml, drops the ones without content (no recipes, loot tables, tags, blockstates or models: libraries,
client-only helpers, tooltips, maps) and groups the rest into batches. A batch line is `1.20.1 <jar name>`; a mandatory dependency that is in
loader/.../forward/1.20.1/libraries.tsv becomes `lib <jar name of the 1.21.1 build in the ATM 10 folder>`, any other dependency found in the ATM 9 folder
a `1.20.1 <jar name>` line. A mod whose dependency is nowhere is listed as skipped.
"""
import argparse, json, pathlib, re, sys, zipfile

try:
    import tomllib
except ImportError:  # python < 3.11
    tomllib = None

ROOT = pathlib.Path(__file__).resolve().parents[2]
ATM9 = "X:/Modded games/Prism Launcher/instances/ATM 9 20026/minecraft/mods"
ATM10 = "X:/Modded games/Prism Launcher/instances/ATM10 2026/minecraft/mods"
BUILTIN = {"minecraft", "forge", "neoforge", "javafml", "lowcodefml", "mixinextras", "mixin"}
CONTENT = re.compile(r"^(data/[^/]+/(recipes|loot_tables|tags|worldgen)/|assets/[^/]+/(blockstates|models/(item|block))/)")


def mods_toml(z):
    try:
        raw = z.read("META-INF/mods.toml").decode("utf-8", "replace")
    except KeyError:
        return None
    if tomllib:
        try:
            return tomllib.loads(raw)
        except Exception:
            pass
    mods = [{"modId": m} for m in re.findall(r'^\s*modId\s*=\s*"([^"]+)"', raw, re.M)]
    names = re.findall(r'^\s*displayName\s*=\s*"([^"]*)"', raw, re.M)
    for m, n in zip(mods, names):
        m["displayName"] = n
    deps = {}
    for block in re.split(r"^\s*\[\[dependencies[^\]]*\]\]", raw, flags=re.M)[1:]:
        block = re.split(r"^\s*\[\[", block, flags=re.M)[0]
        mid = re.search(r'^\s*modId\s*=\s*"([^"]+)"', block, re.M)
        if mid:
            mand = re.search(r"^\s*mandatory\s*=\s*(\w+)", block, re.M)
            side = re.search(r'^\s*side\s*=\s*"(\w+)"', block, re.M)
            deps.setdefault("x", []).append({"modId": mid.group(1), "mandatory": not mand or mand.group(1) != "false",
                                              "side": side.group(1) if side else "BOTH"})
    return {"mods": mods, "dependencies": deps}


def info(path):
    try:
        z = zipfile.ZipFile(path)
    except Exception:
        return None
    t = mods_toml(z)
    if not t or not t.get("mods"):
        return None
    ids = [m.get("modId") for m in t["mods"]]
    deps = set()
    dd = t.get("dependencies") or {}
    for lst in (dd.values() if isinstance(dd, dict) else [dd]):
        for d in lst:
            mand = d.get("mandatory", True)
            if mand is not False and d.get("side", "BOTH") != "CLIENT":
                deps.add(d.get("modId"))
    content = any(CONTENT.match(n) for n in z.namelist())
    return {"ids": ids, "names": [m.get("displayName") for m in t["mods"]], "deps": deps - BUILTIN - set(ids), "content": content, "file": path.name}


def library_ids():
    out = set()
    p = ROOT / "loader/src/main/resources/legacyconnector/forward/1.20.1/libraries.tsv"
    for line in p.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#"):
            out.add(line.split("\t")[0])
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--atm9", default=ATM9)
    ap.add_argument("--atm10", default=ATM10)
    ap.add_argument("--per", type=int, default=15)
    ap.add_argument("--out", default=str(ROOT / "tools/dev/verify/suites"))
    ap.add_argument("--list", action="store_true", help="print the batches, write nothing")
    a = ap.parse_args()
    probe = json.loads((ROOT / "docs/data/probe.json").read_text(encoding="utf-8"))
    runs = probe if isinstance(probe, list) else probe.get("runs", list(probe.values())[0] if probe else [])
    run = [r for r in runs if r.get("mc") == "1.20.1" and r.get("source") == "atm9"][-1]
    link_names = {m[0] for m in run["mods"] if m[2] == "links"}
    atm9 = {p.name: info(p) for p in sorted(pathlib.Path(a.atm9).glob("*.jar"))}
    atm9 = {k: v for k, v in atm9.items() if v}
    by_id = {i: v for v in atm9.values() for i in v["ids"]}
    atm10 = {}
    for p in pathlib.Path(a.atm10).glob("*.jar"):
        try:
            z = zipfile.ZipFile(p)
            for n in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml"):
                if n in z.namelist():
                    t = tomllib.loads(z.read(n).decode("utf-8", "replace"))
                    for m in t.get("mods", []):
                        atm10.setdefault(m.get("modId"), p.name)
        except Exception:
            pass
    libs = library_ids()
    chosen, skipped = [], []
    for name, v in atm9.items():
        if not any(n in link_names for n in v["names"]) or not v["content"]:
            continue
        lines, ok = [], True
        todo, done = [name], set()
        while todo:
            cur = todo.pop(0)
            if cur in done:
                continue
            done.add(cur)
            lines.append(("1.20.1", cur))
            for d in sorted(atm9[cur]["deps"]):
                if d in libs:
                    if d in atm10:
                        lines.append(("lib", atm10[d]))
                    else:
                        ok = False
                        skipped.append((name, "library %s has no 1.21.1 jar in ATM 10" % d))
                elif d in by_id:
                    todo.append(by_id[d]["file"])
                else:
                    ok = False
                    skipped.append((name, "dependency %s not found" % d))
        if ok:
            chosen.append((name, lines))
    batches = [chosen[i:i + a.per] for i in range(0, len(chosen), a.per)]
    out = pathlib.Path(a.out)
    for n, b in enumerate(batches, 1):
        seen, text = set(), ["# generated by tools/dev/gen_suites_1201.py from docs/data/probe.json (ATM 9, status links)"]
        for name, lines in b:
            for kind, f in lines:
                if (kind, f) not in seen:
                    seen.add((kind, f))
                    text.append("%s %s" % (kind, f))
        if a.list:
            print("atm9-1201-%d: %d mods" % (n, len(b)))
            print("\n".join(text[1:]))
        else:
            (out / ("atm9-1201-%d.txt" % n)).write_text("\n".join(text) + "\n", encoding="utf-8")
    for name, why in skipped:
        print("skipped %s: %s" % (name, why), file=sys.stderr)
    print("%d content mods in %d batches (%d skipped)" % (len(chosen), len(batches), len(skipped)))


if __name__ == "__main__":
    main()
