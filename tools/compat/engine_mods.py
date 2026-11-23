"""Engine and performance mods the layer never hosts, read from the loader's own table so the compatibility site and the game agree.

Source of truth: loader/src/main/resources/legacyconnector/engine-mods.tsv (LibraryBridges loads the same file to skip those jars).
Columns: id, Minecraft versions, display names ("|"-separated), modern replacement, reason.  Nothing here is a second hand-written list.
"""
import pathlib
import re

TABLE = pathlib.Path(__file__).resolve().parents[2] / "loader" / "src" / "main" / "resources" / "legacyconnector" / "engine-mods.tsv"


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", (s or "").lower())


def load(path=TABLE):
    """[{id, versions: set, names: [..], replacement, reason}] in file order."""
    rows = []
    for line in pathlib.Path(path).read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        c = line.split("\t")
        if len(c) < 5:
            continue
        rows.append({"id": c[0].strip().lower(), "versions": set(c[1].strip().split(",")),
                     "names": [n for n in c[2].split("|") if n.strip()], "replacement": c[3].strip(), "reason": c[4].strip()})
    return rows


_index = {}


def _by(mc, rows):
    key = (mc, id(rows))
    if key not in _index:
        ids, names = {}, {}
        for r in rows:
            if mc in r["versions"]:
                ids[norm(r["id"])] = r
                ids[r["id"]] = r
                for n in r["names"]:
                    names[norm(n)] = r
        _index[key] = (ids, names)
    return _index[key]


def find(mc, mod_ids=(), mod_names=(), rows=None):
    """The table row for a mod of Minecraft version `mc`, matched by mod id (the loader's rule) or, for site data that only has display names, by a listed name; else None.
    A jar counts only when ALL its mods match (as LibraryBridges.obsoleteReason): the first mod's row is returned."""
    rows = rows if rows is not None else load()
    ids, names = _by(mc, rows)
    hits = []
    for i in mod_ids:
        r = ids.get((i or "").lower()) or ids.get(norm(i))
        hits.append(r)
    if mod_ids and all(hits):
        return hits[0]
    if not mod_ids:
        for n in mod_names:
            r = names.get(norm(n))
            if r:
                return r
    return None


def describe(row):
    """One sentence for a site entry's notes."""
    text = "Engine mod: not needed. " + row["reason"][0].upper() + row["reason"][1:].rstrip(".") + "."
    if row["replacement"]:
        text += " Modern replacement: " + row["replacement"] + "."
    return text
