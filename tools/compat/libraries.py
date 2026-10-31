#!/usr/bin/env python3
"""Rates libraries in docs/data/mods.json by the mods that need them: a mod that only loads (no content of its own) is
marked "works" (source "client") when at least one mod requires it and every mod that requires it works on the site.
Repeats until nothing changes, so a library needed only by other libraries counts too.

usage: libraries.py <translation cache dir>...  (run-*/.legacyconnector/cache: the translated jars' neoforge.mods.toml
       carry the legacy mods' required-after dependencies)
"""
import json, pathlib, re, sys, zipfile, collections

DATA = pathlib.Path(__file__).resolve().parents[2] / "docs" / "data" / "mods.json"
REPORTS = DATA.parent / "reports.json"
TIER = ["community", "maintainer", "client", "corpus"]
ORDER = ["perfect", "works", "partial", "loads", "fails", "crashes"]
IGNORED = {"legacyconnector", "neoforge", "minecraft", "forge", "fml"}


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


names, versions, requires = {}, {}, collections.defaultdict(set)
for d in sys.argv[1:]:
    for jar in pathlib.Path(d).glob("*.jar"):
        try:
            with zipfile.ZipFile(jar) as z:
                toml = z.read("META-INF/neoforge.mods.toml").decode("utf-8", "replace")
        except (KeyError, zipfile.BadZipFile, OSError):
            continue
        if "from legacy jar" not in toml:
            continue
        for block in toml.split("[[mods]]")[1:]:
            mod = re.search(r'modId="([^"]+)"', block)
            if mod:
                names[mod.group(1)] = (re.search(r'displayName="([^"]*)"', block) or mod).group(1)
                versions[mod.group(1)] = (re.search(r'version="([^"]*)"', block) or re.match("()", "")).group(1)
        for dep in re.finditer(r'\[\[dependencies\.([^\]]+)\]\]\s*modId="([^"]+)"\s*type="required"', toml):
            if dep.group(2) not in IGNORED and dep.group(2) != dep.group(1):
                requires[dep.group(1)].add(dep.group(2))

data = json.loads(DATA.read_text(encoding="utf-8"))
reports = json.loads(REPORTS.read_text(encoding="utf-8"))["reports"] if REPORTS.exists() else []
entries = collections.defaultdict(list)
for e in data["mods"]:
    if e.get("mc", "1.7.10") == "1.7.10":
        entries[norm(e["name"])].append((e.get("source", "maintainer"), e["status"]))
for r in reports:
    entries[norm(r["mod"])].append(("community", r["status"]))


def site_status(key):
    """The badge the site shows (docs/index.html merge), simplified: highest tier, its most common (client: best) status."""
    for tier in TIER:
        pool = [s for t, s in entries.get(key, []) if t == tier]
        if pool:
            if tier == "client":
                return min(pool, key=ORDER.index)
            return collections.Counter(pool).most_common(1)[0][0]
    return "untested"


dependents = collections.defaultdict(set)
for mod, deps in requires.items():
    for dep in deps:
        dependents[dep].add(mod)
status = {m: site_status(norm(n)) for m, n in names.items()}
promoted = {}
changed = True
while changed:
    changed = False
    for lib, users in dependents.items():
        if lib not in names or status.get(lib) != "loads" or lib in promoted:
            continue
        users = {u for u in users if u in names}
        if users and all(status.get(u) == "works" for u in users):
            status[lib] = "works"
            promoted[lib] = sorted(names[u] for u in users)
            changed = True

for lib, users in promoted.items():
    name = names[lib]
    data["mods"] = [e for e in data["mods"] if not (e.get("source") == "client" and e.get("mc") == "1.7.10"
                                                  and norm(e["name"]) == norm(name) and e.get("library"))]
    shown = ", ".join(users[:8]) + (f" and {len(users) - 8} more" if len(users) > 8 else "")
    data["mods"].append({"name": name, "version": versions.get(lib, ""), "mc": "1.7.10", "source": "client", "date": "2026-10-28",
                         "status": "works", "library": True,
                         "notes": f"Library: every mod tested that requires it works ({len(users)}: {shown})."})
data["mods"].sort(key=lambda e: e["name"].lower())
# mods listed in "hidden" (nothing to do on 1.21.1, or client mods with their own 1.21.1 version) stay off the site
hidden = {norm(h) for h in data.get("hidden", [])}
data["mods"] = [e for e in data["mods"] if norm(e["name"]) not in hidden]
DATA.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
print(f"{len(names)} mods with metadata, {len(dependents)} required by others, {len(promoted)} libraries marked works:")
for lib, users in sorted(promoted.items()):
    print(f"  {names[lib]} <- {len(users)} mod(s)")
