#!/usr/bin/env python3
"""Link probe: translates a directory of legacy mod jars offline (no game) and adds the result to docs/data/mods.json,
like the Sinytra Connector compatibility checker does for Fabric.

usage: probe.py <jar-dir> [--mc 1.7.10|1.12.2|1.20.1] [--date YYYY-MM-DD] [--version <layer version>] [--json out.json] [--dry-run] [--no-history] [--fail-on-regression]
       probe.py --from-json probe.json [...]     (convert an earlier run without translating again)

The jars are only read (test-mods/ stays gitignored, never commit or redistribute them); only results are written.
All jars are translated as ONE mod set so mods that extend each other link. Per jar, ./gradlew checkLegacyMod yields:
  links       every reference resolves, few placeholder stubs        -> status "loads" (site: "Ready to test")
  stubs       links, but STUB_LIMIT+ synthesized empty members       -> status "loads" (site: "Partly placeholder")
  gap_shim    missing old Minecraft/Forge functionality (our shim)   -> status "fails"
  gap_mod     missing another mod's API (NEI, Thaumcraft, AE2, ...)  -> status "fails"
  gap_lib     missing removed Java/library code                      -> status "fails"
  gap_dep     a required dependency mod is not in the set            -> "fails", or "loads" when the jar links apart from that
  obsolete    engine patch with nothing to do on 1.21.1 -> not listed
--mc 1.20.1 (Forge 1.20.1 jars, remapped in place onto NeoForge 1.21.1) has no shim to link against: it runs ./gradlew gap1201 (loader tools Gap1201: SRG to
Mojang and Forge to NeoForge renames, then every reference is resolved against Minecraft 1.21.1 + NeoForge + the other jars) and turns the unresolved symbols into the
same records, so a jar is "loads" when nothing is left unresolved (a link check of the renamed jar, like the others). It needs build/moddev/artifacts/neoforge-*-merged.jar
(any ./gradlew build) and the gradle cache libraries; GAP1201_TARGETS (comma separated jars) replaces that target list.
A probe proves "translates and links", never "works in game": entries get source "probe", which the site ranks below
maintainer, client and corpus entries (a real run of the same mod always wins) and which never overwrites them.
Every run is also appended to docs/data/probe.json (counts + per-mod list), which the pie chart page docs/probe.html shows.
"""
import argparse, datetime, glob, json, os, pathlib, re, subprocess, sys, tempfile, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
DATA = ROOT / "docs" / "data" / "mods.json"
RESOURCES = ROOT / "docs" / "data" / "resources.json"   # the missing-resources tab: every unresolved model/texture reference of the latest run per Minecraft version
HISTORY = ROOT / "docs" / "data" / "probe.json"   # one entry per run: the pie chart page (docs/probe.html) reads it
STUB_LIMIT = 10   # a mod that links but uses this many synthesized (empty) legacy API members is "stubs", not "links"
KEEP_MODS = 10   # runs per Minecraft version that keep their per-mod list; older ones keep only the counts


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


def old_zip_era(mc):
    """Forge 1.6.4 and older (and Beta) mods were mostly distributed as .zip files (ComputerCraft, BuildCraft, RedPower, ...): they are part of the set."""
    return mc.startswith("b") or mc.split(".")[:2] in (["1", "2"], ["1", "3"], ["1", "4"], ["1", "5"], ["1", "6"])


def run_probe(jar_dir, out, zips=False):
    jars = sorted(pathlib.Path(jar_dir).rglob("*.jar"))
    if zips:   # Beta and 1.2.5-1.6.4 mods came as .zip files (ModLoader and early Forge mods)
        jars = sorted(jars + list(pathlib.Path(jar_dir).rglob("*.zip")))
    if not jars:
        sys.exit(f"no jars in {jar_dir}")
    # -PlegacyJar is a comma-separated list: a jar whose name contains a comma is probed through a symlink with a safe name
    safe = pathlib.Path(tempfile.mkdtemp(prefix="probe-links-"))
    for i, j in enumerate(jars):
        if "," in str(j):
            link = safe / f"{i}-{j.name.replace(',', '_')}"
            link.symlink_to(j.resolve())
            jars[i] = link
    jar_arg = ','.join(map(str, jars))
    if len(jar_arg) > 6000:   # the Windows command line is limited: hand over a list file ("@file", one path per line)
        listing = safe / "jars.txt"
        listing.write_text(chr(10).join(map(str, jars)), encoding="utf-8")
        jar_arg = "@" + str(listing)
    cmd = [str(ROOT / ("gradlew.bat" if sys.platform == "win32" else "gradlew")), "-q", "checkLegacyMod", f"-PlegacyJar={jar_arg}", f"-PcheckJson={out}"]
    print(f"probing {len(jars)} jar(s) as one mod set ...", flush=True)
    r = subprocess.run(cmd, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    if r.returncode != 0 or not pathlib.Path(out).exists():
        sys.exit(r.stdout[-3000:] + "\nprobe failed")
    for line in r.stdout.splitlines():   # the resource check says here whether it ran (it is skipped without the vanilla client jar)
        if line.startswith("Asset check"):
            print(line, flush=True)


def gap1201_targets():
    """Minecraft 1.21.1 + NeoForge jars and the libraries of the game class path, the target of the 1.20.1 gap report."""
    env = os.environ.get("GAP1201_TARGETS")
    if env:
        return env
    arts = []
    for base in (ROOT / "build" / "moddev" / "artifacts", ROOT.parents[2] / "build" / "moddev" / "artifacts"):   # a worktree reuses the main checkout's build
        arts = sorted(base.glob("neoforge-*-merged.jar")) + [j for j in sorted(base.glob("neoforge-*.jar"))
                                                              if "-merged" not in j.name and "-sources" not in j.name and "client-extra" not in j.name]
        if arts:
            break
    if not arts:
        sys.exit("no build/moddev/artifacts/neoforge-*-merged.jar: run ./gradlew build once, or set GAP1201_TARGETS")
    cache = pathlib.Path.home() / ".gradle" / "caches" / "modules-2" / "files-2.1"
    groups = ["io.netty", "it.unimi.dsi", "org.lwjgl", "com.ibm.icu", "org.apache.commons", "commons-io", "commons-codec", "commons-logging", "com.google.guava",
              "com.google.code.gson", "com.google.code.findbugs", "org.jetbrains", "com.github.oshi", "net.java.dev.jna", "com.mojang", "org.slf4j",
              "org.apache.logging.log4j", "com.electronwill.night-config", "net.neoforged", "net.neoforged.fancymodloader", "cpw.mods", "io.github.llamalad7",
              "org.joml", "net.sf.jopt-simple", "org.apache.httpcomponents", "org.ow2.asm", "net.fabricmc", "org.jspecify", "org.lz4", "org.checkerframework",
              "com.google.errorprone"]
    skip = ("sources", "javadoc", "installertools", "jst", "moddev", "srgutils", "neoform", "AutoRenaming", "DevLaunch", "JarJar", "minecraft-dependencies",
            "binarypatcher", "cli-utils", "gradle", "fatjar", "at-parser", "natives", "javadoctor", "mergetool-2.0.0")

    def vkey(v):
        return [int(x) if x.isdigit() else 0 for x in re.split(r"[.\-+]", v)]
    best = {}
    for g in groups:
        for j in glob.glob(str(cache / g / "**" / "*.jar"), recursive=True):
            rel = pathlib.Path(j).relative_to(cache).as_posix()
            if any(x in rel for x in skip) and "mergetool-2.0.3-api" not in rel:
                continue
            parts = rel.split("/")
            art = (parts[0], parts[1])
            if art not in best or vkey(parts[2]) > vkey(best[art][0]):
                best[art] = (parts[2], j)
    jars = [a.as_posix() for a in arts] + [pathlib.Path(j).as_posix() for _, j in best.values()]
    libs = os.environ.get("GAP1201_LIBS")   # a folder of NeoForge 1.21.1 mods (libraries the pack would install natively): references into them are checked against the real classes
    if libs:
        for folder in libs.split(";"):   # several folders: ';'-separated (a ':' would clash with drive letters)
            jars += [j.as_posix() for j in sorted(pathlib.Path(folder).glob("*.jar"))]
    return ",".join(jars)


def mod_info(jar):
    """[{id, name, version}] from META-INF/mods.toml of a 1.20.1 Forge jar (or neoforge.mods.toml)."""
    import tomllib
    try:
        with zipfile.ZipFile(jar) as z:
            for n in ("META-INF/mods.toml", "META-INF/neoforge.mods.toml"):
                if n in z.namelist():
                    t = tomllib.loads(z.read(n).decode("utf-8", "replace"))
                    out = []
                    for m in t.get("mods", []):
                        v = str(m.get("version", ""))
                        if v.startswith("${") or not v:
                            try:
                                mm = re.search(r"Implementation-Version:\s*(\S+)", z.read("META-INF/MANIFEST.MF").decode("utf-8", "replace"))
                                v = mm.group(1) if mm else ""
                            except KeyError:
                                v = ""
                        out.append({"id": m.get("modId", ""), "name": m.get("displayName") or m.get("modId", ""), "version": v})
                    return out
    except Exception:
        pass
    return []


def native_libraries():
    """Mod ids of forward/1.20.1/libraries.tsv: 1.20.1 libraries replaced by their native NeoForge 1.21.1 build when the pack has it."""
    f = ROOT / "loader" / "src" / "main" / "resources" / "legacyconnector" / "forward" / "1.20.1" / "libraries.tsv"
    return {l.split("	")[0] for l in f.read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")}


def dotted(kind, sym):
    """A gap report symbol (internal names, descriptors) as the dotted name probe.py classifies by prefix."""
    if kind != "class":
        sym = re.split(r"[(:]", sym, 1)[0]
    return sym.replace("/", ".")


def run_gap1201(jar_dir, out, keep_detail=None):
    """The 1.20.1 equivalent of run_probe: ./gradlew gap1201 over the directory, converted to the records checkLegacyMod writes."""
    jars = sorted(pathlib.Path(jar_dir).glob("*.jar"))   # as the gradle task does: top level only (a pack's .connector/ holds Sinytra's remapped Fabric jars)
    if not jars:
        sys.exit(f"no jars in {jar_dir}")
    tmp = pathlib.Path(tempfile.mkdtemp(prefix="gap1201-"))
    report = tmp / "gap1201.txt"
    listing = tmp / "targets.txt"   # "@file": the target list is longer than the Windows command line
    listing.write_text(gap1201_targets(), encoding="utf-8")
    cmd = [str(ROOT / ("gradlew.bat" if sys.platform == "win32" else "gradlew")), "-q", "gap1201", f"-PtargetJars=@{listing.as_posix()}",
           f"-PgapJars={pathlib.Path(jar_dir).resolve()}", f"-PgapReport={report}", "-Pgap.translate=true"]
    if os.environ.get("GAP1201_ASSUME_NATIVE"):   # e.g. mcjty/theoneprobe/api/,dev/emi/emi/api/,me/shedaniel/rei/ : API packages of libraries the pack gets as native 1.21.1 builds
        cmd.append("-Pgap.assumeNative=" + os.environ["GAP1201_ASSUME_NATIVE"])   # the report is of the jars AFTER the layer (facades, library bridges, renames)
    if os.environ.get("GAP1201_TRANSLATED_DIR"):   # keep the translated jars (what the layer leaves) for javap
        cmd.append("-Pgap.translated.dir=" + os.environ["GAP1201_TRANSLATED_DIR"])
    print(f"gap report for {len(jars)} Forge 1.20.1 jar(s) ...", flush=True)
    r = subprocess.run(cmd, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    detail = pathlib.Path(str(report) + ".detail.tsv")
    if r.returncode != 0 or not detail.exists():
        sys.exit(r.stdout[-3000:] + "\nprobe failed")
    if keep_detail:   # the full per-jar symbol list (records keep only 20): tools/compat/atm9_run.py ranks the blockers from it
        import shutil
        shutil.copyfile(detail, keep_detail)
    missing, refs = {}, {}
    for line in detail.read_text(encoding="utf-8").splitlines():
        jar, k, sym = line.split("\t", 2)
        if k == "refs":
            refs[jar] = int(sym)
        else:
            missing.setdefault(jar, []).append(dotted(k, sym))
    records = []
    for j in jars:
        with zipfile.ZipFile(j) as z:
            classes = sum(1 for n in z.namelist() if n.endswith(".class"))
        miss = sorted(set(missing.get(j.name, [])))
        # the telling ones first: Forge and Minecraft symbols before the other mods' (cause() looks at all of them)
        miss.sort(key=lambda m: (not m.startswith(("net.minecraftforge.", "net.neoforged.", "net.minecraft.", "com.mojang.")), m))
        info = mod_info(j)
        rec = {"jar": j.name, "mc": "1.20.1", "mods": info, "classes": classes, "synthesizedMembers": 0, "references": refs.get(j.name, 0)}
        if info and all(m["id"].lower() in native_libraries() for m in info):
            rec["result"] = "obsolete"   # a library with a native 1.21.1 build of the same mod id: the layer skips this jar (NativeLibraries1201), the native build is what runs
        elif miss:
            rec.update({"result": "unresolved", "unresolved": len(miss), "missing": miss[:20]})
        else:
            rec["result"] = "links"
        records.append(rec)
    pathlib.Path(out).write_text(json.dumps(records), encoding="utf-8")


# what a missing symbol belongs to: our shim (old Minecraft/Forge not built yet), old Java/library code, or another mod's API
LIB_PREFIXES = ("java.", "javax.", "sun.", "jdk.", "com.sun.", "com.google.", "org.apache.", "org.objectweb.", "org.lwjgl.", "io.netty.",
                "joptsimple.", "org.spongepowered.", "com.mojang.", "org.slf4j.", "gnu.", "paulscode.", "org.w3c.", "org.xml.")
# APIs the layer provides itself (LibraryBridges: NEI -> JEI, Baubles -> Curios, Waila -> Jade, CodeChickenLib), so a gap there is ours to close
SHIM_PREFIXES = ("legacyconnector.v1_", "net.minecraft.", "net.minecraftforge.", "cpw.mods.",
                 "codechicken.nei.", "codechicken.lib.", "baubles.", "mcp.mobius.waila.")


def cause(missing):
    """gap_shim (needs old Minecraft/Forge functionality), gap_mod (another mod's API) or gap_lib (removed Java/library code)."""
    if any(m.startswith(SHIM_PREFIXES) for m in missing):
        return "gap_shim"
    if any(not m.startswith(LIB_PREFIXES) for m in missing):
        return "gap_mod"
    return "gap_lib"


def kind(rec):
    """The category a record counts as: links, stubs (links but largely placeholders), gap_shim / gap_mod / gap_lib, obsolete."""
    if rec["result"] == "links":
        return "stubs" if rec.get("synthesizedMembers", 0) >= STUB_LIMIT else "links"
    if rec["result"] == "missing-dependency":
        return "gap_dep"
    if rec["result"] == "unresolved":
        return cause(rec.get("missing") or [])
    return rec["result"]


GAP_TEXT = {
    "gap_shim": "needs Minecraft or Forge features the layer does not have yet",
    "gap_mod": "needs code from another mod or API that is not available or does not match",
    "gap_lib": "uses old Java or library code that no longer exists",
    "gap_dep": "needs another mod that was not installed",
}


def asset_note(rec):
    """One sentence about references that point at nothing after translation (rendering smoke test), or ''."""
    a = rec.get("assets") or {}
    if not a.get("layer"):
        return ""
    ex = [e[len("[layer] "):] for e in a.get("examples", []) if e.startswith("[layer] ")][:2]
    return f" Resource check: {a['layer']} model/texture reference(s) are missing after translation (rendering may show missing textures or models), e.g. " + "; ".join(ex) + "."


def note(rec, layer):
    return note_links(rec, layer) + asset_note(rec)


def note_links(rec, layer):
    base = f"Automated check {layer}: " if layer else "Automated check: "
    k = kind(rec)
    if k in ("links", "stubs"):
        stubs = rec.get("synthesizedMembers", 0)
        text = f"{base}translates offline and everything it uses exists ({rec.get('classes', 0)} classes)."
        if k == "stubs":
            text += f" But {stubs} features it uses are empty placeholders, so large parts probably do nothing."
        elif stubs:
            text += f" {stubs} minor feature(s) it uses are empty placeholders."
        return text + " The game was not started: this only shows the jar translates, not that it works."
    if k == "gap_dep":
        if rec.get("linksWithoutDependency"):
            ids = ", ".join(sorted({m.split(" requires ", 1)[-1].split(",", 1)[0].split(" (", 1)[0] for m in rec.get("missingDependencies", [])}))
            return f"{base}declares a mod that was not installed: {ids}. Everything else it uses links; the mod may still work without it."
        return f"{base}{'; '.join(rec.get('missingDependencies', [])[:3])}."
    miss = ", ".join(rec.get("missing", [])[:5])
    more = f" (+{rec['unresolved'] - 5} more)" if rec.get("unresolved", 0) > 5 else ""
    return f"{base}{GAP_TEXT.get(k, 'has missing pieces')}: {miss}{more}."


def merge(records, mc, date, layer):
    data = json.loads(DATA.read_text(encoding="utf-8"))
    listed = {}
    for e in data["mods"]:
        if e.get("mc", "1.7.10") == mc:
            listed.setdefault(norm(e["name"]), []).append(e)
    hidden = {norm(h) for h in data.get("hidden", [])} | {norm(h) for h in data.get("hidden_by_mc", {}).get(mc, [])}
    added = refreshed = skipped = 0
    for rec in records:
        if rec["result"] not in ("links", "unresolved", "missing-dependency") or rec.get("mc") != mc:
            continue
        status = "loads" if rec["result"] == "links" or rec.get("linksWithoutDependency") else "fails"
        for m in rec["mods"]:
            name = (m.get("name") or m["id"]).strip()
            key = norm(name)
            if key in hidden:
                continue
            existing = listed.get(key, [])
            if any(e.get("source") != "probe" for e in existing):
                skipped += 1   # a maintainer, client or corpus result is better than a link probe
                continue
            entry = {"name": name, "version": (m.get("version") or "").strip(), "mc": mc, "source": "probe", "date": date,
                     "status": status, "notes": note(rec, layer)}
            if existing:
                existing[0].update(entry)
                refreshed += 1
            else:
                data["mods"].append(entry)
                listed[key] = [entry]
                added += 1
    if added:
        data["mods"].sort(key=lambda e: e["name"].lower())
    return data, added, refreshed, skipped


RANK = {"links": 0, "stubs": 1, "gap_shim": 2, "gap_mod": 2, "gap_lib": 2, "gap_dep": 2, "unresolved": 2}   # worse is higher; "obsolete" is not compared
MAX_CHANGES = 100


def environment(layer, jars):
    """What the result was produced with, so a change of status can be told apart from a change of tooling."""
    import hashlib
    env = {"layer": layer, "jars": jars}
    props = ROOT / "gradle.properties"
    if props.exists():
        for line in props.read_text(encoding="utf-8").splitlines():
            for key, name in (("neo_version=", "neoforge"), ("minecraft_version=", "minecraft")):
                if line.startswith(key):
                    env[name] = line[len(key):].strip()
    index = ROOT / "build" / "generated" / "shimIndex" / "legacyconnector" / "shim-index.txt"
    if index.exists():
        env["shim"] = hashlib.sha1(index.read_bytes()).hexdigest()[:12]   # changes whenever the hand-written shim does
    return env


def compare(previous, rows):
    """Per-mod status changes against the previous run of the same Minecraft version: {"regressed": [[name, from, to]], "fixed": [...]}.

    A mod can appear twice with different jar versions (a corpus plus Modrinth), so each name is compared by its BEST result in each run:
    only when the best one got worse (or better) does it count, which keeps a second, older jar from looking like a regression."""
    def best(entries):
        out = {}
        for name, k in entries:
            if k in RANK and (name.lower() not in out or RANK[k] < RANK[out[name.lower()][1]]):
                out[name.lower()] = (name, k)
        return out
    old = best((r[0], r[2]) for r in previous["mods"])
    new = best((r[0], r[2]) for r in rows)
    out = {"date": previous["date"], "version": previous.get("version", ""), "regressed": [], "fixed": []}
    for key, (name, k) in new.items():
        was = old.get(key)
        if was is None or was[1] == k:
            continue
        if RANK[k] > RANK[was[1]]:
            out["regressed"].append([name, was[1], k])
        else:
            out["fixed"].append([name, was[1], k])
    out["regressedCount"], out["fixedCount"] = len(out["regressed"]), len(out["fixed"])
    out["regressed"], out["fixed"] = out["regressed"][:MAX_CHANGES], out["fixed"][:MAX_CHANGES]
    return out


def resources(records, mc, date, layer):
    """The latest resource-check findings of one Minecraft version: per mod, the references that point at nothing (layer = the translation lost
    them, mod = the mod's own jar lacks them too), ready for docs/index.html's "Missing resources" tab. None when no record has a check."""
    best = {}
    for r in records:
        a = r.get("assets")
        if not a or r.get("mc") != mc or not r.get("mods") or not (a.get("layer") or a.get("mod")):
            continue
        m = r["mods"][0]
        name = (m.get("name") or m["id"]).strip()
        entry = {"name": name, "version": (m.get("version") or "").strip(), "layer": a.get("layer", 0), "mod": a.get("mod", 0),
                 "layerItems": a.get("layerItems", []), "modItems": a.get("modItems", [])}
        old = best.get(name.lower())
        if old is None or (entry["layer"], entry["mod"]) > (old["layer"], old["mod"]):   # the same mod from two jars: the worse one
            best[name.lower()] = entry
    checked = sum(1 for r in records if r.get("assets") and r.get("mc") == mc and r.get("mods"))
    if not checked:
        return None
    mods = sorted(best.values(), key=lambda e: (-e["layer"], -e["mod"], e["name"].lower()))
    return {"version": layer, "date": date, "checked": checked, "mods": mods}


def newest_builds(records, mc):
    """Keeps only the newest build of each mod of this Minecraft version (a corpus often holds two builds of one mod: the run list must show it once)."""
    def vkey(v):
        return [int(x) if x.isdigit() else 0 for x in re.split(r"[^0-9]+", str(v)) if x != ""]
    best = {}
    for r in records:
        if r.get("mc") != mc or not r.get("mods"):
            continue
        m = r["mods"][0]
        k = re.sub(r"[^a-z0-9]+", "", (m.get("name") or m["id"]).lower())
        if k not in best or vkey(m.get("version") or "") > vkey(best[k]["mods"][0].get("version") or ""):
            best[k] = r
    keep = {id(r) for r in best.values()}
    return [r for r in records if r.get("mc") != mc or not r.get("mods") or id(r) in keep]


def history(records, mc, date, layer, source=""):
    """Appends this run to docs/data/probe.json: counts per result and the per-jar list (mod, result, first unresolved reference)."""
    records = newest_builds(records, mc)
    rows, counts, seen = [], {"links": 0, "stubs": 0, "gap_shim": 0, "gap_mod": 0, "gap_lib": 0, "obsolete": 0}, set()
    assets = {"checked": 0, "modsWithLayerProblems": 0, "layerRefs": 0, "modsWithOwnProblems": 0, "ownRefs": 0}
    for r in records:
        k = kind(r) if r.get("result") else ""
        if r.get("mc") != mc or k not in counts or not r.get("mods"):
            continue   # other versions, non-mod jars and libraries without a mod are not part of the statistics
        m = r["mods"][0]
        key = ((m.get("name") or m["id"]).strip().lower(), (m.get("version") or "").strip())
        if key in seen:
            continue   # the same mod from two sources (a corpus and Modrinth) counts once
        seen.add(key)
        counts[k] += 1
        stubs = r.get("synthesizedMembers", 0)
        detail = r.get("reason") or (r.get("missing") or [""])[0] if r["result"] != "links" else (f"{stubs} synthesized stub(s)" if stubs else "")
        row = [(m.get("name") or m["id"]).strip(), (m.get("version") or "").strip(), k, detail]
        a = r.get("assets")
        if a:
            assets["checked"] += 1
            if a.get("layer"):
                assets["modsWithLayerProblems"] += 1
                assets["layerRefs"] += a["layer"]
            if a.get("mod"):
                assets["modsWithOwnProblems"] += 1
                assets["ownRefs"] += a["mod"]
            if a.get("layer") or a.get("mod"):
                row += [a.get("layer", 0), a.get("mod", 0), [e for e in a.get("examples", []) if e.startswith("[layer]")][:2]]
        rows.append(row)
    rows.sort(key=lambda x: x[0].lower())
    h = json.loads(HISTORY.read_text(encoding="utf-8")) if HISTORY.exists() else {"runs": []}
    # a run is identified by version, date and source: results of different populations (the local corpus plus Modrinth, or Modrinth only in CI)
    # never replace or get compared with each other
    h["runs"] = [x for x in h["runs"] if not (x["mc"] == mc and x["date"] == date and x["version"] == layer and x.get("source", "") == source)]
    prior = [x for x in h["runs"] if x["mc"] == mc and x.get("mods") and x.get("source", "") == source]
    prior.sort(key=lambda x: (x["date"], x["version"]))
    vs = compare(prior[-1], rows) if prior else None
    run = {"mc": mc, "date": date, "version": layer, "counts": counts, "env": environment(layer, len(records)), "mods": rows}
    if assets["checked"]:
        run["assets"] = assets
    if source:
        run["source"] = source
    if vs:
        run["vs"] = vs
    h["runs"].append(run)
    h["runs"].sort(key=lambda x: (x["mc"], x["date"], x["version"], x.get("source", "")))
    seen = {}
    for x in reversed(h["runs"]):
        seen[x["mc"]] = seen.get(x["mc"], 0) + 1
        if seen[x["mc"]] > KEEP_MODS:
            x["mods"] = []
    return h, counts, vs


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("jar_dir", nargs="?")
    ap.add_argument("--mc", default="1.7.10")
    ap.add_argument("--date", default=datetime.date.today().isoformat())
    ap.add_argument("--version", default="", help="layer version shown in the notes")
    ap.add_argument("--json", help="keep the raw probe records here")
    ap.add_argument("--from-json", help="skip translation, convert this probe file")
    ap.add_argument("--source", default="", help="label of the jar population (e.g. modrinth for CI); runs of different sources are kept and compared separately")
    ap.add_argument("--detail", help="1.20.1: keep the full <jar, kind, symbol> report (gap1201 detail.tsv) here")
    ap.add_argument("--no-history", action="store_true", help="do not add the run to docs/data/probe.json")
    ap.add_argument("--fail-on-regression", action="store_true", help="exit 2 when a mod got worse than in the previous run (after writing the results)")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()
    if a.from_json:
        path = a.from_json
    elif a.jar_dir:
        path = a.json or tempfile.mktemp(suffix=".json")
        run_gap1201(a.jar_dir, path, a.detail) if a.mc == "1.20.1" else run_probe(a.jar_dir, path, zips=old_zip_era(a.mc))
    else:
        ap.error("jar directory or --from-json required")
    records = json.loads(pathlib.Path(path).read_text(encoding="utf-8"))
    counts = {}
    for r in records:
        counts[r["result"]] = counts.get(r["result"], 0) + 1
    print("jars:", ", ".join(f"{v} {k}" for k, v in sorted(counts.items())))
    data, added, refreshed, skipped = merge(records, a.mc, a.date, a.version)
    print(f"{added} added, {refreshed} refreshed, {skipped} kept (better source), {len(data['mods'])} total")
    if not a.dry_run and (added or refreshed):
        DATA.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    if not a.no_history:
        h, c, vs = history(records, a.mc, a.date, a.version, a.source)
        print(f"run {a.mc} {a.version or '-'} {a.date}: " + ", ".join(f"{v} {k}" for k, v in c.items()))
        if not a.dry_run:
            HISTORY.write_text(json.dumps(h, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
        res = resources(records, a.mc, a.date, a.version)
        if res and not a.dry_run and not a.source:   # the site's Missing resources tab shows the main corpus; labelled sources (CI) keep to the history
            data = json.loads(RESOURCES.read_text(encoding="utf-8")) if RESOURCES.exists() else {"runs": {}}
            data["runs"][a.mc] = res
            RESOURCES.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
            print(f"missing resources: {len(res['mods'])} mods, {sum(m['layer'] for m in res['mods'])} lost in translation, {sum(m['mod'] for m in res['mods'])} the mods' own")
        if vs:
            print(f"since {vs['date']} {vs['version'] or ''}: {vs['fixedCount']} fixed, {vs['regressedCount']} regressed")
            for name, was, now in vs["regressed"]:
                print(f"  REGRESSED {name}: {was} -> {now}")
            if a.fail_on_regression and vs["regressedCount"]:
                sys.exit(2)


main()
