#!/usr/bin/env python3
"""Downloads the most downloaded Forge mods of one Minecraft version from Modrinth, as input for tools/compat/probe.py.

usage: fetch_probe_mods.py <mc-version> <dest-dir> [--limit 200]

Uses Modrinth's public API (search by downloads, newest Forge file of each project, SHA-1 checked). The jars are only for the
offline link probe: keep them in a gitignored or temporary folder, never commit or redistribute them. Jars already in the
folder are skipped, so a re-run only fetches new ones.
"""
import argparse, hashlib, json, pathlib, sys, time, urllib.parse, urllib.request

API = "https://api.modrinth.com/v2"
UA = {"User-Agent": "kylepitcock/legacy-connector-linkprobe"}


def get(url):
    for attempt in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
                return r.read()
        except Exception as e:
            if attempt == 3:
                raise
            time.sleep(2 * (attempt + 1))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("mc")
    ap.add_argument("dest")
    ap.add_argument("--limit", type=int, default=200)
    a = ap.parse_args()
    dest = pathlib.Path(a.dest)
    dest.mkdir(parents=True, exist_ok=True)
    facets = json.dumps([[f"versions:{a.mc}"], ["categories:forge"], ["project_type:mod"]])
    hits, offset = [], 0
    while len(hits) < a.limit:
        q = urllib.parse.urlencode({"facets": facets, "index": "downloads", "limit": min(100, a.limit - len(hits)), "offset": offset})
        page = json.loads(get(f"{API}/search?{q}"))["hits"]
        if not page:
            break
        hits += page
        offset += len(page)
    got = skipped = failed = 0
    for h in hits:
        try:
            q = urllib.parse.urlencode({"game_versions": json.dumps([a.mc]), "loaders": json.dumps(["forge"])})
            versions = json.loads(get(f"{API}/project/{h['project_id']}/version?{q}"))
            if not versions:
                continue
            files = versions[0]["files"]
            f = next((x for x in files if x.get("primary")), files[0])
            target = dest / f["filename"]
            if target.exists():
                skipped += 1
                continue
            data = get(f["url"])
            if hashlib.sha1(data).hexdigest() != f["hashes"]["sha1"]:
                print("hash mismatch, skipped:", f["filename"], file=sys.stderr)
                failed += 1
                continue
            target.write_bytes(data)
            got += 1
            time.sleep(0.2)
        except Exception as e:
            print(f"{h['slug']}: {e}", file=sys.stderr)
            failed += 1
    print(f"{a.mc}: {got} downloaded, {skipped} already present, {failed} failed ({len(hits)} projects)")


main()
