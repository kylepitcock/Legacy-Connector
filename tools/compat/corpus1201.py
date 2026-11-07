#!/usr/bin/env python3
"""Builds the 1.20.1 Forge corpus (test-mods-1201/) and its index, with the 'newer NeoForge 1.21.1 version exists' check.

usage: corpus1201.py <dest-dir> [--scan 700] [--abandoned 70] [--with-ported 20] [--libs] [--index-only]

Candidates are the most downloaded Forge 1.20.1 projects of Modrinth (no optimization/library categories unless --libs). For each one
the script asks Modrinth whether the project has a NeoForge file for 1.21.1 and cfwidget (CurseForge) whether the same slug has a
NeoForge 1.21.1 file; a project with neither is 'abandoned' (no port known). Downloads come from Modrinth only (SHA-1 checked), required
dependencies included (recursive, 3 levels). Writes <dest>/index.json and <dest>/index.md (mod id, version, size, port status, mods.toml
dependencies). The jars are for the offline gap report; never commit or redistribute them.
"""
import argparse, hashlib, io, json, pathlib, re, sys, time, tomllib, urllib.parse, urllib.request, zipfile

API = "https://api.modrinth.com/v2"
UA = {"User-Agent": "kylepitcock/legacy-connector-corpus1201"}
BUILTIN_DEPS = {"forge", "minecraft", "neoforge", "java"}


def get(url, tries=4, quiet404=False):
    for attempt in range(tries):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            if e.code == 404 and quiet404:
                return None
            if attempt == tries - 1:
                raise
            time.sleep(2 * (attempt + 1))
        except Exception:
            if attempt == tries - 1:
                raise
            time.sleep(2 * (attempt + 1))


def versions(pid, mc, loader):
    q = urllib.parse.urlencode({"game_versions": json.dumps([mc]), "loaders": json.dumps([loader])})
    return json.loads(get(f"{API}/project/{pid}/version?{q}"))


def cf_port(slug):
    """True/False/None: does CurseForge list a NeoForge file for 1.21.1 under this slug (None = slug unknown there)."""
    raw = get(f"https://api.cfwidget.com/minecraft/mc-mods/{slug}", tries=2, quiet404=True)
    if not raw:
        return None
    try:
        d = json.loads(raw)
    except Exception:
        return None
    if "versions" not in d:
        return None
    files = list(d.get("files", []))
    for lst in d["versions"].values():
        files += lst
    for f in files:
        v = f.get("versions", [])
        if "1.21.1" in v and "NeoForge" in v:
            return True
    return False


def mod_meta(path):
    """mods.toml facts plus mixin counts (config files and mixin classes, read from the mixin json files)."""
    try:
        with zipfile.ZipFile(path) as z:
            names = z.namelist()
            toml = tomllib.loads(z.read("META-INF/mods.toml").decode("utf-8", "replace")) if "META-INF/mods.toml" in names else {}
            ntoml = "META-INF/neoforge.mods.toml" in names
            fabric = "fabric.mod.json" in names and "META-INF/mods.toml" not in names
            cfgs = [m.get("config") for m in toml.get("mixins", []) if m.get("config")]
            cfgs += [n for n in names if re.search(r"mixins?[^/]*\.json$", n, re.I) and "/" not in n and n not in cfgs]
            if fabric:
                try:
                    cfgs += json.loads(z.read("fabric.mod.json").decode("utf-8", "replace")).get("mixins", [])
                except Exception:
                    pass
            n_mixin = 0
            seen = set()
            for c in cfgs:
                c = c if isinstance(c, str) else c.get("config", "")
                if c in seen or c not in names:
                    continue
                seen.add(c)
                try:
                    j = json.loads(z.read(c).decode("utf-8-sig", "replace"))
                    n_mixin += len(j.get("mixins", [])) + len(j.get("client", [])) + len(j.get("server", []))
                except Exception:
                    pass
            classes = sum(1 for n in names if n.endswith(".class"))
    except Exception as e:
        return {"error": str(e)}
    mods = toml.get("mods", [{}])
    mid = mods[0].get("modId", "?") if mods else "?"
    ver = mods[0].get("version", "?") if mods else "?"
    deps = []
    for ds in (toml.get("dependencies", {}) or {}).values():
        for d in ds:
            if d.get("modId") not in BUILTIN_DEPS and d.get("mandatory", d.get("type") == "required") not in (False, "optional"):
                deps.append(d["modId"])
    return {"modId": mid, "version": ver, "deps": sorted(set(deps)), "mixinConfigs": len(seen), "mixins": n_mixin, "classes": classes,
            "fabric": fabric or "connectormod" in deps, "neoforgeToml": ntoml}


def other_ports(title, slug):
    """Modrinth projects with a NeoForge 1.21.1 file whose title contains this mod's title (unofficial or renamed ports)."""
    norm = re.sub(r"[^a-z0-9]", "", title.lower())
    if len(norm) < 4:
        return []
    q = urllib.parse.urlencode({"query": title, "facets": json.dumps([["versions:1.21.1"], ["categories:neoforge"]]), "limit": 8})
    try:
        hits = json.loads(get(f"{API}/search?{q}"))["hits"]
    except Exception:
        return []
    return [h["slug"] for h in hits if h["slug"] != slug and norm in re.sub(r"[^a-z0-9]", "", h["title"].lower())]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dest")
    ap.add_argument("--scan", type=int, default=700, help="how many top Modrinth projects to examine")
    ap.add_argument("--abandoned", type=int, default=70)
    ap.add_argument("--with-ported", type=int, default=20, help="extra mods that DO have a port (libraries/big ones for reference)")
    ap.add_argument("--libs", action="store_true", help="also allow library category projects (kept in the 'lib' group)")
    ap.add_argument("--index-only", action="store_true")
    ap.add_argument("--extra", default="", help="comma separated Modrinth slugs to add regardless of status")
    a = ap.parse_args()
    dest = pathlib.Path(a.dest)
    dest.mkdir(parents=True, exist_ok=True)
    idx_file = dest / "index.json"
    index = json.loads(idx_file.read_text()) if idx_file.exists() else {"mods": {}}
    mods = index["mods"]

    if not a.index_only:
        facets = json.dumps([["versions:1.20.1"], ["categories:forge"], ["project_type:mod"]])
        hits, off = [], 0
        while len(hits) < a.scan:
            q = urllib.parse.urlencode({"facets": facets, "index": "downloads", "limit": 100, "offset": off})
            page = json.loads(get(f"{API}/search?{q}"))["hits"]
            if not page:
                break
            hits += page
            off += len(page)
        for slug in filter(None, a.extra.split(",")):
            p = json.loads(get(f"{API}/project/{slug}", quiet404=True) or b"null")
            if p:
                hits.append({"project_id": p["id"], "slug": p["slug"], "title": p["title"], "downloads": p["downloads"], "categories": p["categories"], "forced": True})
        n_ab = sum(1 for m in mods.values() if m["port"] == "no" and m.get("group") != "lib")
        n_pt = sum(1 for m in mods.values() if m["port"] == "yes")
        seen = {m["project"] for m in mods.values()}
        for h in hits:
            if h["project_id"] in seen:
                continue
            is_lib = "library" in h["categories"]
            if is_lib and not (a.libs or h.get("forced")):
                continue
            if "optimization" in h["categories"] and not h.get("forced"):
                continue
            forced = h.get("forced")
            if not forced and n_ab >= a.abandoned and n_pt >= a.with_ported:
                break
            try:
                v120 = versions(h["project_id"], "1.20.1", "forge")
                if not v120:
                    continue
                v121 = versions(h["project_id"], "1.21.1", "neoforge")
                cf = cf_port(h["slug"])
                port = "yes" if (v121 or cf) else "no"
                if port == "no" and n_ab >= a.abandoned and not forced:
                    continue
                if port == "yes" and n_pt >= a.with_ported and not forced:
                    continue
                v = v120[0]
                f = next((x for x in v["files"] if x.get("primary")), v["files"][0])
                target = dest / f["filename"]
                data = get(f["url"])
                if hashlib.sha1(data).hexdigest() != f["hashes"]["sha1"]:
                    print("hash mismatch", f["filename"], file=sys.stderr)
                    continue
                target.write_bytes(data)
                seen.add(h["project_id"])
                if mod_meta(target).get("fabric"):
                    fdir = dest.parent / (dest.name + "-fabric")
                    fdir.mkdir(exist_ok=True)
                    target.replace(fdir / target.name)  # Fabric jars (Sinytra Connector territory) are not part of the Forge corpus
                    continue
                mods[f["filename"]] = {"project": h["project_id"], "slug": h["slug"], "title": h["title"], "downloads": h["downloads"], "port": port,
                                       "port_modrinth": bool(v121), "port_curseforge": cf, "group": "lib" if is_lib else "content", "required_deps": []}
                if port == "no" and not is_lib:
                    n_ab += 1
                elif port == "yes":
                    n_pt += 1
                print(f"{port:3} {h['slug']:40} {f['filename']}  (abandoned {n_ab}, ported {n_pt})", flush=True)
                # required dependencies
                for dep in v.get("dependencies", []):
                    if dep.get("dependency_type") != "required" or not dep.get("project_id") or dep["project_id"] in seen:
                        continue
                    seen.add(dep["project_id"])
                    try:
                        dv = json.loads(get(f"{API}/version/{dep['version_id']}")) if dep.get("version_id") else (versions(dep["project_id"], "1.20.1", "forge") or [None])[0]
                        if not dv:
                            continue
                        df = next((x for x in dv["files"] if x.get("primary")), dv["files"][0])
                        dt = dest / df["filename"]
                        if not dt.exists():
                            dd = get(df["url"])
                            if hashlib.sha1(dd).hexdigest() == df["hashes"]["sha1"]:
                                dt.write_bytes(dd)
                        p = json.loads(get(f"{API}/project/{dep['project_id']}"))
                        mods.setdefault(df["filename"], {"project": dep["project_id"], "slug": p["slug"], "title": p["title"], "downloads": p["downloads"],
                                                         "port": "yes" if versions(dep["project_id"], "1.21.1", "neoforge") else "no",
                                                         "port_modrinth": None, "port_curseforge": None, "group": "dep", "required_deps": []})
                        mods[f["filename"]]["required_deps"].append(p["slug"])
                    except Exception as e:
                        print("  dep failed", dep.get("project_id"), e, file=sys.stderr)
                time.sleep(0.15)
            except Exception as e:
                print(f"{h['slug']}: {e}", file=sys.stderr)
        idx_file.write_text(json.dumps(index, indent=1))

    # index.md from the jars
    rows = []
    for fn, m in sorted(list(mods.items()), key=lambda kv: (kv[1]["group"] != "content", kv[1]["port"] != "no", -kv[1]["downloads"])):
        p = dest / fn
        if not p.exists():
            continue
        meta = mod_meta(p)
        if meta.get("fabric") or fn.startswith(("Connector", "fabric-api")):
            fdir = dest.parent / (dest.name + "-fabric")
            fdir.mkdir(exist_ok=True)
            p.replace(fdir / fn)
            del mods[fn]
            continue
        m["meta"] = meta
        if m["port"] == "no" and "port_other" not in m:
            m["port_other"] = other_ports(m["title"], m["slug"])
        rows.append((fn, m, meta))
    idx_file.write_text(json.dumps(index, indent=1))
    out = ["# Corpus test-mods-1201 (generated by tools/compat/corpus1201.py)", "",
           "| Jar | Mod id | Version | Size KB | Classes | Mixin cfgs | Mixin classes | Group | Newer 1.21.1 NeoForge exists | Modrinth / CurseForge | Declared mandatory deps |", "|---|---|---|---|---|---|---|---|---|---|---|"]
    for fn, m, meta in rows:
        cf = {True: "yes", False: "no", None: "?"}[m.get("port_curseforge")]
        out.append(f"| {fn} | {meta.get('modId')} | {meta.get('version')} | {(dest / fn).stat().st_size // 1024} | {meta.get('classes')} | {meta.get('mixinConfigs')} | {meta.get('mixins')} | {m['group']} | "
                   f"{m['port']} | {'yes' if m.get('port_modrinth') else 'no'} / {cf}{(' (unofficial: ' + ', '.join(m['port_other']) + ')') if m.get('port_other') else ''} | {', '.join(meta.get('deps', []))} |")
    (dest / "index.md").write_text("\n".join(out) + "\n", encoding="utf-8")
    print(len(rows), "jars indexed;", sum(1 for _, m, _ in rows if m["port"] == "no" and m["group"] == "content"), "abandoned content mods")


main()
