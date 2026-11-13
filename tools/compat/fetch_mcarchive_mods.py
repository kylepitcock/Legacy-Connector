#!/usr/bin/env python3
"""Downloads mods of one old Minecraft version from MCArchive (https://mcarchive.net), as input for tools/compat/probe.py.

usage: fetch_mcarchive_mods.py <mc-version> <dest-dir> [--limit N] [--also 1.6.2,1.6.1] [--list]

Modrinth has few Forge mods older than 1.7.10; MCArchive preserves the mods of the ModLoader / early Forge era (Beta to 1.6).
For every mod of the archive, the newest listed version that supports <mc-version> (or one of --also, for mods released for a
neighbouring patch version only) is downloaded from the archive's own mirror and checked against its SHA-256.
--list prints how many mods each game version has and exits.

The files are only for the offline link probe: keep them in a gitignored or temporary folder, never commit or redistribute them.
Files already in the folder are skipped, so a re-run only fetches new ones. API: https://mcarchive.net/api/v1/
"""
import argparse, hashlib, json, pathlib, re, sys, time, urllib.error, urllib.parse, urllib.request

API = "https://mcarchive.net/api/v1"
UA = {"User-Agent": "kylepitcock/legacy-connector-linkprobe"}


PAUSE = 1.5  # seconds between API requests: the archive answers 429 to bursts
_last = [0.0]


def get(url, raw=False):
    for attempt in range(8):
        wait = PAUSE - (time.time() - _last[0])
        if wait > 0 and "/api/" in url:
            time.sleep(wait)
        _last[0] = time.time()
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=90) as r:
                data = r.read()
                return data if raw else json.loads(data.decode("utf-8"))
        except urllib.error.HTTPError as e:
            if e.code == 429 and attempt < 7:
                time.sleep(float(e.headers.get("Retry-After") or 0) or 20 * (attempt + 1))
                continue
            if attempt == 7 or e.code in (403, 404):
                raise
            time.sleep(3 * (attempt + 1))
        except Exception:  # noqa: BLE001 (network errors of any kind are retried)
            if attempt == 7:
                raise
            time.sleep(3 * (attempt + 1))


def mod_info(slug, cache):
    """The archive's record of one mod, kept in <cache>/<slug>.json so several versions (and re-runs) cost one request per mod."""
    f = cache / (re.sub(r"[^A-Za-z0-9._-]", "_", slug) + ".json")
    if f.exists():
        return json.loads(f.read_text(encoding="utf-8"))
    info = get(f"{API}/mods/by_slug/{urllib.parse.quote(slug)}")
    f.write_text(json.dumps(info), encoding="utf-8")
    return info


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("mc")
    ap.add_argument("dest", nargs="?")
    ap.add_argument("--limit", type=int, default=0, help="stop after this many downloads (0 = all)")
    ap.add_argument("--also", default="", help="comma separated game versions accepted when a mod has no file for <mc>")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--cache", default="", help="folder for the archive's mod records (default: <dest>/../.mcarchive-cache)")
    a = ap.parse_args()
    cache = pathlib.Path(a.cache) if a.cache else (pathlib.Path(a.dest).parent if a.dest else pathlib.Path(".")) / ".mcarchive-cache"
    cache.mkdir(parents=True, exist_ok=True)
    mods = get(f"{API}/mods/")
    wanted = [a.mc] + [v.strip() for v in a.also.split(",") if v.strip()]
    counts, done, failed = {}, 0, 0
    dest = pathlib.Path(a.dest) if a.dest else None
    if not a.list:
        if dest is None:
            ap.error("destination directory required")
        dest.mkdir(parents=True, exist_ok=True)
    for m in mods:
        try:
            info = mod_info(m["slug"], cache)
        except Exception as e:  # noqa: BLE001
            print(f"  {m['slug']}: {e}", file=sys.stderr)
            continue
        versions = info.get("mod_versions") or []
        if a.list:
            for gv in {g["name"] for v in versions for g in v.get("game_versions") or []}:
                counts[gv] = counts.get(gv, 0) + 1
            continue
        pick = None
        for want in wanted:  # exact version first, then the neighbours in the order given
            pick = next((v for v in versions if any(g["name"] == want for g in v.get("game_versions") or []) and v.get("files")), None)
            if pick:
                break
        if not pick:
            continue
        for f in pick["files"]:
            url = f.get("archive_url") or f.get("direct_url")
            name = re.sub(r"[^A-Za-z0-9._+ -]", "_", f.get("name") or "")
            if not url or not name.lower().endswith((".jar", ".zip")):
                continue
            target = dest / name
            if target.exists():
                continue
            try:
                data = get(url, raw=True)
            except Exception as e:  # noqa: BLE001
                print(f"  {name}: {e}", file=sys.stderr)
                failed += 1
                continue
            if f.get("sha256") and hashlib.sha256(data).hexdigest() != f["sha256"].lower():
                print(f"  {name}: SHA-256 mismatch, skipped", file=sys.stderr)
                failed += 1
                continue
            if data[:2] != b"PK":  # a .zip name on a RAR/7z/HTML payload: the probe reads zip archives only
                print(f"  {name}: not a zip archive, skipped", file=sys.stderr)
                failed += 1
                continue
            target.write_bytes(data)
            done += 1
            print(f"{name}  ({info.get('name')}, {pick.get('name')})", flush=True)
        if a.limit and done >= a.limit:
            break
    if a.list:
        for gv, n in sorted(counts.items(), key=lambda x: -x[1]):
            print(f"{gv}\t{n}")
        return
    print(f"{done} file(s) downloaded to {dest}, {failed} failed, {len(list(dest.iterdir()))} in the folder")


if __name__ == "__main__":
    main()
