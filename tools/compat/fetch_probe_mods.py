#!/usr/bin/env python3
"""Downloads the most downloaded Forge mods of one Minecraft version from Modrinth, as input for tools/compat/probe.py.

usage: fetch_probe_mods.py <mc-version> <dest-dir> [--limit 200] [--no-deps] [--content] [--client-extra N]

--content keeps content mods only: no performance/optimization or library mods, and nothing client-only (a realistic corpus for
the layer, whose job is content mods); --client-extra N then adds the N most downloaded client-side mods (UI/utility), minus optimization.

Uses Modrinth's public API (search by downloads, newest Forge file of each project, SHA-1 checked). By default every
required dependency a download declares is fetched too (recursively, up to 3 levels; the version the mod pins, else the newest
Forge file for the Minecraft version): a mod that needs a library or parent mod would otherwise be reported as missing it,
which is a failure of the corpus, not of the mod. The jars are only for the offline link probe: keep them in a gitignored or
temporary folder, never commit or redistribute them. Jars already in the folder are skipped, so a re-run only fetches new ones.
"""
import argparse, hashlib, json, pathlib, sys, time, urllib.parse, urllib.request

API = "https://api.modrinth.com/v2"
UA = {"User-Agent": "kylepitcock/legacy-connector-linkprobe"}
MAX_DEPTH = 3


def get(url):
    for attempt in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
                return r.read()
        except Exception:
            if attempt == 3:
                raise
            time.sleep(2 * (attempt + 1))


def newest_version(project_id, mc):
    q = urllib.parse.urlencode({"game_versions": json.dumps([mc]), "loaders": json.dumps(["forge"])})
    versions = json.loads(get(f"{API}/project/{project_id}/version?{q}"))
    return versions[0] if versions else None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("mc")
    ap.add_argument("dest")
    ap.add_argument("--limit", type=int, default=200)
    ap.add_argument("--no-deps", action="store_true", help="do not download required dependencies")
    ap.add_argument("--content", action="store_true", help="content mods only: exclude optimization and library categories and client-only mods")
    ap.add_argument("--client-extra", type=int, default=0, help="with --content: also fetch this many client-side mods (never optimization)")
    a = ap.parse_args()
    dest = pathlib.Path(a.dest)
    dest.mkdir(parents=True, exist_ok=True)
    stats = {"got": 0, "skipped": 0, "failed": 0, "deps": 0}
    seen_projects = set()

    def fetch(version, is_dep):
        files = version["files"]
        f = next((x for x in files if x.get("primary")), files[0])
        target = dest / f["filename"]
        if target.exists():
            stats["skipped"] += 1
            return
        data = get(f["url"])
        if hashlib.sha1(data).hexdigest() != f["hashes"]["sha1"]:
            print("hash mismatch, skipped:", f["filename"], file=sys.stderr)
            stats["failed"] += 1
            return
        target.write_bytes(data)
        stats["deps" if is_dep else "got"] += 1
        time.sleep(0.2)

    def fetch_deps(version, depth):
        if a.no_deps or depth >= MAX_DEPTH:
            return
        for dep in version.get("dependencies", []):
            if dep.get("dependency_type") != "required" or not dep.get("project_id") or dep["project_id"] in seen_projects:
                continue
            seen_projects.add(dep["project_id"])
            try:
                if dep.get("version_id"):
                    v = json.loads(get(f"{API}/version/{dep['version_id']}"))
                else:
                    v = newest_version(dep["project_id"], a.mc)
                if not v:
                    continue
                fetch(v, True)
                fetch_deps(v, depth + 1)
            except Exception as e:
                print(f"dependency {dep['project_id']}: {e}", file=sys.stderr)
                stats["failed"] += 1

    base = [[f"versions:{a.mc}"], ["categories:forge"], ["project_type:mod"]]

    def search(extra, limit):
        facets = json.dumps(base + extra)
        found, offset = [], 0
        while len(found) < limit:
            q = urllib.parse.urlencode({"facets": facets, "index": "downloads", "limit": min(100, limit - len(found)), "offset": offset})
            page = json.loads(get(f"{API}/search?{q}"))["hits"]
            if not page:
                break
            found += page
            offset += len(page)
        return found

    if a.content:
        # "!=" facets: not optimization/library, and the mod does something on a server (content, not client-only UI)
        hits = search([["categories!=optimization"], ["categories!=library"], ["server_side!=unsupported"]], a.limit)
        if a.client_extra:
            hits += search([["categories!=optimization"], ["categories!=library"], ["server_side=unsupported"], ["client_side=required"]], a.client_extra)
    else:
        hits = search([], a.limit)
    for h in hits:
        try:
            seen_projects.add(h["project_id"])
            v = newest_version(h["project_id"], a.mc)
            if not v:
                continue
            fetch(v, False)
            fetch_deps(v, 0)
        except Exception as e:
            print(f"{h['slug']}: {e}", file=sys.stderr)
            stats["failed"] += 1
    print(f"{a.mc}: {stats['got']} downloaded, {stats['deps']} dependencies downloaded, {stats['skipped']} already present, "
          f"{stats['failed']} failed ({len(hits)} projects)")


main()
