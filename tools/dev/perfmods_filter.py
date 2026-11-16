#!/usr/bin/env python3
"""Chooses which jars of run-perfmods/ may be copied into a game's mods folder: a jar whose mandatory dependency (neoforge.mods.toml,
type="required" / mandatory=true) names a mod id that no jar of the set (or the game's mods folder) provides is skipped, with a printed line.

usage: perfmods_filter.py <perfmods-dir> [<game-mods-dir>]     prints the jars to copy (one path per line); skipped ones go to stderr.

Some mods crash without a mod their toml only lists as optional because their mixin configs target it (Iris Veil Compat without Veil):
EXTRA_REQUIRES names those pairs.
"""
import json
import os
import re
import sys
import zipfile

try:
    import tomllib
except ImportError:  # Python < 3.11
    tomllib = None

# Always provided by the platform.
BUILTIN = {"minecraft", "neoforge", "forge", "javafml", "lowcodefml", "mixinextras", "mixin"}
# mod id -> ids that must be present although the toml marks them optional (mixin-only hard dependencies)
EXTRA_REQUIRES = {"irisveil": {"veil"}}


def _parse(text):
    """-> (provided ids, required ids) of one neoforge.mods.toml text."""
    if tomllib is None:
        return _parse_fallback(text)
    data = tomllib.loads(text)
    provided = {m["modId"] for m in data.get("mods", []) if "modId" in m}
    required = {}
    for owner, deps in data.get("dependencies", {}).items():
        for d in deps if isinstance(deps, list) else []:
            mandatory = str(d.get("type", "required" if d.get("mandatory", False) else "optional")).lower() == "required"
            if mandatory and "modId" in d:
                required.setdefault(owner, set()).add(d["modId"])
    return provided, required


def _parse_fallback(text):
    provided = set(re.findall(r'^\s*modId\s*=\s*"([^"]+)"', text.split("[[dependencies", 1)[0], re.M))
    required = {}
    for block in re.split(r"^\[\[dependencies\.", text, flags=re.M)[1:]:
        owner = block.split("]]", 1)[0]
        mod = re.search(r'modId\s*=\s*"([^"]+)"', block)
        typ = re.search(r'type\s*=\s*"([^"]+)"', block)
        mand = re.search(r"mandatory\s*=\s*true", block)
        if mod and ((typ and typ.group(1).lower() == "required") or (not typ and mand)):
            required.setdefault(owner, set()).add(mod.group(1))
    return provided, required


def read_jar(path):
    """-> (provided ids incl. jar-in-jar mods, {owner mod id: required ids}); unreadable jars provide and require nothing."""
    provided, required = set(), {}
    try:
        with zipfile.ZipFile(path) as z:
            names = z.namelist()
            if "META-INF/neoforge.mods.toml" in names:
                p, r = _parse(z.read("META-INF/neoforge.mods.toml").decode("utf-8", "replace"))
                provided |= p
                required.update(r)
            for n in names:  # jar-in-jar libraries that are mods themselves provide their ids
                if n.startswith("META-INF/jarjar/") and n.endswith(".jar"):
                    import io
                    try:
                        with zipfile.ZipFile(io.BytesIO(z.read(n))) as inner:
                            if "META-INF/neoforge.mods.toml" in inner.namelist():
                                provided |= _parse(inner.read("META-INF/neoforge.mods.toml").decode("utf-8", "replace"))[0]
                    except Exception:
                        pass
    except Exception:
        pass
    return provided, required


def select(jar_info, extra_provided=()):
    """jar_info: {jar: (provided, required)} -> (kept jars, {skipped jar: missing ids}); iterates until stable (a skipped jar stops providing)."""
    kept = dict(jar_info)
    skipped = {}
    while True:
        have = set(BUILTIN) | set(extra_provided)
        for p, _ in kept.values():
            have |= p
        changed = False
        for jar, (prov, req) in list(kept.items()):
            need = set()
            for owner in prov:
                need |= req.get(owner, set()) | EXTRA_REQUIRES.get(owner, set())
            if not prov:
                for r in req.values():
                    need |= r
            missing = sorted(need - have)
            if missing:
                skipped[jar] = missing
                del kept[jar]
                changed = True
        if not changed:
            return list(kept), skipped


def main(argv):
    if len(argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    jars = sorted(os.path.join(argv[1], f) for f in os.listdir(argv[1]) if f.endswith(".jar")) if os.path.isdir(argv[1]) else []
    extra = set()
    if len(argv) > 2 and os.path.isdir(argv[2]):
        for f in os.listdir(argv[2]):
            if f.endswith(".jar"):
                extra |= read_jar(os.path.join(argv[2], f))[0]
    kept, skipped = select({j: read_jar(j) for j in jars}, extra)
    for j, missing in skipped.items():
        print("perfmods: skipping %s (needs %s, not present)" % (os.path.basename(j), ", ".join(missing)), file=sys.stderr)
    for j in kept:
        print(j)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
