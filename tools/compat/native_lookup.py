"""Does a native NeoForge 1.21.1 version of this mod exist?  (Modrinth lookup by file hash, then by project slug = mod id; loaders=neoforge, game_versions=1.21.1.)

Answers are cached in tools/compat/native-modrinth.tsv (committed, small), so the weekly probe does not hammer the API and works offline:
    key TAB project_id TAB slug TAB native(yes|no) TAB version TAB checked
key is the jar's SHA-1, or "id:<mod id>" for the slug fallback.  Only rows that were answered are stored (a failed request is retried next run).
One request at a time, polite delay, own User-Agent.  `refresh=True` (probe.py --refresh-native) ignores the cache.
Only the Minecraft versions in NATIVE_MC are asked; another legacy version opts in by being added there.
"""
import datetime
import hashlib
import json
import pathlib
import time
import urllib.error
import urllib.parse
import urllib.request

CACHE = pathlib.Path(__file__).resolve().parent / "native-modrinth.tsv"
NATIVE_MC = {"1.20.1"}
UA = "legacyconnector-compat-probe (https://github.com/kylepitcock/Legacy-Connector)"
DELAY = 0.4


def sha1(path):
    h = hashlib.sha1()
    with open(path, "rb") as f:
        for b in iter(lambda: f.read(1 << 20), b""):
            h.update(b)
    return h.hexdigest()


def load_cache(path=CACHE):
    rows = {}
    p = pathlib.Path(path)
    if p.exists():
        for line in p.read_text(encoding="utf-8").splitlines():
            if line.startswith("#") or not line.strip():
                continue
            c = line.split("\t")
            if len(c) >= 6:
                rows[c[0]] = {"project": c[1], "slug": c[2], "native": c[3] == "yes", "version": c[4], "checked": c[5]}
    return rows


def save_cache(rows, path=CACHE):
    lines = ["# Modrinth lookups: is there a NeoForge 1.21.1 build? key = jar sha1 or id:<mod id>; written by tools/compat/native_lookup.py (probe.py --refresh-native renews it)",
             "# key\tproject_id\tslug\tnative\tversion\tchecked"]
    for k in sorted(rows):
        r = rows[k]
        lines.append("\t".join([k, r["project"], r["slug"], "yes" if r["native"] else "no", r.get("version", ""), r["checked"]]))
    pathlib.Path(path).write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")


def _get(url):
    """JSON of a Modrinth answer, None for 404, raises for anything else (the caller does not cache a failure)."""
    for attempt in range(4):
        try:
            time.sleep(DELAY)
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": UA}), timeout=30) as r:
                return json.load(r)
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return None
            time.sleep(3 * (attempt + 1))
        except Exception:
            time.sleep(3 * (attempt + 1))
    raise RuntimeError("Modrinth unreachable: " + url)


def _native_of_project(pid):
    q = urllib.parse.urlencode({"loaders": '["neoforge"]', "game_versions": '["1.21.1"]'})
    vs = _get("https://api.modrinth.com/v2/project/%s/version?%s" % (pid, q))
    proj = _get("https://api.modrinth.com/v2/project/" + pid) or {}
    return {"project": pid, "slug": proj.get("slug", pid), "native": bool(vs), "version": (vs[0].get("version_number", "") if vs else ""),
            "checked": datetime.date.today().isoformat()}


class Lookup:
    """lookup(jar_path, mod_id) -> {"native": bool, "slug": str, "project": str} or None (unknown: offline and not cached)."""

    def __init__(self, online=True, refresh=False, path=CACHE):
        self.online, self.refresh, self.path = online, refresh, path
        self.rows = load_cache(path)
        self.dirty = False
        self.asked = 0

    def __call__(self, jar_path, mod_id=""):
        key = sha1(jar_path)
        row = None if self.refresh else self.rows.get(key)
        if row is None and not self.refresh:
            row = self.rows.get("id:" + mod_id.lower()) if mod_id and key not in self.rows else None
        if row is not None:
            return row
        if not self.online:
            return None
        try:
            v = _get("https://api.modrinth.com/v2/version_file/%s?algorithm=sha1" % key)
            self.asked += 1
            pid = (v or {}).get("project_id")
            if pid:
                row = _native_of_project(pid)
                self.rows[key] = row
            elif mod_id:
                p = _get("https://api.modrinth.com/v2/project/" + urllib.parse.quote(mod_id.lower().replace("_", "-")))
                if p and "mod" in (p.get("project_type") or "mod"):
                    row = _native_of_project(p["id"])
                    self.rows["id:" + mod_id.lower()] = row
                else:   # known to Modrinth as nothing: remember that, so the next run does not ask again
                    row = {"project": "", "slug": "", "native": False, "version": "", "checked": datetime.date.today().isoformat()}
                    self.rows["id:" + mod_id.lower()] = row
            else:
                return None
            self.dirty = True
            return row
        except RuntimeError:
            return None

    def save(self):
        if self.dirty:
            save_cache(self.rows, self.path)
            self.dirty = False


def url_of(row):
    return "https://modrinth.com/mod/" + row["slug"] if row and row.get("slug") else ""
