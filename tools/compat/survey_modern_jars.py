#!/usr/bin/env python3
"""EXPERIMENTAL (1.18.2 / 1.16.5 scoping): structural survey of a directory of Forge jars of the 1.16 to 1.20 era. Offline, reads archive entries only (nothing is executed).

usage: survey_modern_jars.py <jar-dir> [<out.json>]

Per jar and in total: class file major versions (52 = Java 8, 60 = Java 16, 61 = Java 17), mods.toml loader/loaderVersion/minecraft range, Mixin (manifest
MixinConfigs, *.mixins.json, refmaps and the naming style inside them: SRG func_/m_ ids), coremods (META-INF/coremods.json and the JavaScript files),
access transformers (META-INF/accesstransformer.cfg and their SRG ids), jar-in-jar, Fabric/Quilt markers, resource folder layout (plural 1.16 to 1.20.1 names vs
1.21 singular), world generation JSON (worldgen/*), and the data features the 1.20.1 data converter handles (forge: ids, loot modifiers, conditions).
"""
import collections, json, pathlib, re, sys, zipfile

SRG = re.compile(rb"\b(func_\d+_[A-Za-z]+_?|field_\d+_[A-Za-z]+_?|m_\d+_|f_\d+_)\b")


def survey(path):
    r = {"jar": path.name}
    try:
        z = zipfile.ZipFile(path)
    except Exception:
        r["error"] = "unreadable"
        return r
    names = z.namelist()
    majors = collections.Counter()
    for n in names:
        if n.endswith(".class") and not n.startswith("META-INF/versions/"):
            try:
                majors[int.from_bytes(z.read(n)[6:8], "big")] += 1
            except Exception:
                pass
    r["classes"] = sum(majors.values())
    r["major_max"] = max(majors) if majors else 0
    r["major_min"] = min(majors) if majors else 0
    r["fabric"] = "fabric.mod.json" in names or "quilt.mod.json" in names
    r["neoforge"] = "META-INF/neoforge.mods.toml" in names
    toml = ""
    if "META-INF/mods.toml" in names:
        toml = z.read("META-INF/mods.toml").decode("utf-8", "replace")
    r["mods_toml"] = bool(toml)
    m = re.search(r'modLoader\s*=\s*"([^"]+)"', toml)
    r["modLoader"] = m.group(1) if m else ""
    m = re.search(r'loaderVersion\s*=\s*"([^"]+)"', toml)
    r["loaderVersion"] = m.group(1) if m else ""
    mc = re.search(r'modId\s*=\s*"minecraft"[^\[]*?versionRange\s*=\s*"([^"]+)"', toml, re.S)
    r["mcRange"] = mc.group(1) if mc else ""
    fg = re.search(r'modId\s*=\s*"forge"[^\[]*?versionRange\s*=\s*"([^"]+)"', toml, re.S)
    r["forgeRange"] = fg.group(1) if fg else ""
    mf = z.read("META-INF/MANIFEST.MF").decode("utf-8", "replace") if "META-INF/MANIFEST.MF" in names else ""
    mm = re.search(r"MixinConfigs:\s*([^\r\n]+)", mf)
    mixin_cfgs = [x.strip() for x in mm.group(1).split(",")] if mm else []
    mixin_files = [n for n in names if re.search(r"(^|/)[^/]*mixins?[^/]*\.json$", n) and not n.startswith("data/") and "refmap" not in n]
    r["mixin"] = bool(mixin_cfgs or mixin_files)
    r["mixin_configs"] = len(set(mixin_cfgs) | set(mixin_files))
    classes = 0
    compat = set()
    refmaps = []
    for n in mixin_files:
        try:
            j = json.loads(z.read(n).decode("utf-8", "replace"))
        except Exception:
            continue
        classes += sum(len(j.get(k, [])) for k in ("mixins", "client", "server"))
        if j.get("compatibilityLevel"):
            compat.add(j["compatibilityLevel"])
        if j.get("refmap"):
            refmaps.append(j["refmap"])
    r["mixin_classes"] = classes
    r["mixin_compat"] = sorted(compat)
    srg_refmap = False
    for rm in set(refmaps):
        if rm in names:
            body = z.read(rm)
            if SRG.search(body):
                srg_refmap = True
    r["refmap"] = bool(refmaps)
    r["refmap_srg"] = srg_refmap
    r["coremods_json"] = "META-INF/coremods.json" in names
    r["coremod_js"] = sum(1 for n in names if n.endswith(".js") and not n.startswith(("assets/", "data/"))) if r["coremods_json"] else 0
    at = [n for n in names if n.endswith("accesstransformer.cfg")]
    r["at"] = bool(at)
    r["at_srg"] = any(SRG.search(z.read(n)) for n in at)
    r["jarjar"] = any(n.startswith("META-INF/jarjar/") and n.endswith(".jar") for n in names)
    r["nested_jars"] = sum(1 for n in names if n.endswith(".jar"))
    singular = plural = 0
    for n in names:
        if n.startswith("data/"):
            seg = n.split("/")
            if len(seg) > 3:
                if seg[2] in ("recipes", "loot_tables", "advancements", "structures", "functions") or (seg[2] == "tags" and len(seg) > 4 and seg[3] in ("blocks", "items", "entity_types", "fluids")):
                    plural += 1
                elif seg[2] in ("recipe", "loot_table", "advancement", "structure", "function") or (seg[2] == "tags" and len(seg) > 4 and seg[3] in ("block", "item", "entity_type", "fluid")):
                    singular += 1
    r["data_plural"] = plural
    r["data_singular"] = singular
    wg = collections.Counter()
    for n in names:
        mm = re.match(r"data/[^/]+/(worldgen/[^/]+)/", n)
        if mm:
            wg[mm.group(1)] += 1
        mm = re.match(r"data/[^/]+/(dimension|dimension_type)/", n)
        if mm:
            wg[mm.group(1)] += 1
    r["worldgen"] = dict(wg)
    r["loot_modifiers"] = any(n.startswith("data/forge/loot_modifiers/") for n in names)
    r["forge_recipe_conditions"] = 0
    r["pack_format"] = None
    if "pack.mcmeta" in names:
        try:
            r["pack_format"] = json.loads(z.read("pack.mcmeta").decode("utf-8-sig", "replace")).get("pack", {}).get("pack_format")
        except Exception:
            pass
    return r


def main():
    d = pathlib.Path(sys.argv[1])
    rows = [survey(p) for p in sorted(d.glob("*.jar"))]
    code = [r for r in rows if r.get("classes", 0) > 0]
    n = len(code)

    def cnt(f):
        return sum(1 for r in code if f(r))
    print(f"jars {len(rows)}, with code {n}; Forge (mods.toml) {cnt(lambda r: r['mods_toml'])}; Fabric/Quilt marker {cnt(lambda r: r['fabric'])}; NeoForge toml {cnt(lambda r: r['neoforge'])}")
    maj = collections.Counter(r["major_max"] for r in code)
    print("class file major version (highest in the jar):", dict(sorted(maj.items())))
    print("modLoader:", dict(collections.Counter(r["modLoader"] for r in code if r["mods_toml"])))
    print("loaderVersion:", collections.Counter(r["loaderVersion"] for r in code if r["mods_toml"]).most_common(8))
    print("minecraft range:", collections.Counter(r["mcRange"] for r in code if r["mods_toml"]).most_common(8))
    print("forge range:", collections.Counter(r["forgeRange"] for r in code if r["mods_toml"]).most_common(8))
    print(f"mixin: {cnt(lambda r: r['mixin'])} jars, {sum(r['mixin_classes'] for r in code)} mixin classes in {sum(r['mixin_configs'] for r in code)} configs; compatibilityLevel:",
          dict(collections.Counter(x for r in code for x in r["mixin_compat"])))
    print(f"refmap {cnt(lambda r: r['refmap'])} jars, with SRG ids inside {cnt(lambda r: r['refmap_srg'])}")
    print(f"coremods.json {cnt(lambda r: r['coremods_json'])} jars, JavaScript coremod files {sum(r['coremod_js'] for r in code)}")
    print(f"access transformer {cnt(lambda r: r['at'])} jars, with SRG ids {cnt(lambda r: r['at_srg'])}")
    print(f"jar-in-jar (META-INF/jarjar) {cnt(lambda r: r['jarjar'])} jars; any nested jar {cnt(lambda r: r['nested_jars'] > 0)}")
    print(f"data folders: plural names in {cnt(lambda r: r['data_plural'] > 0)} jars ({sum(r['data_plural'] for r in code)} files), singular (1.21 style) in {cnt(lambda r: r['data_singular'] > 0)} jars")
    wg = collections.Counter()
    wgj = collections.Counter()
    for r in code:
        for k, v in r["worldgen"].items():
            wg[k] += v
            wgj[k] += 1
    print("world generation data (files / jars):", {k: f"{wg[k]}/{wgj[k]}" for k in sorted(wg)})
    print("pack_format:", dict(collections.Counter(r["pack_format"] for r in code).most_common(8)))
    print(f"forge loot modifiers {cnt(lambda r: r['loot_modifiers'])} jars")
    if len(sys.argv) > 2:
        json.dump(rows, open(sys.argv[2], "w", encoding="utf-8"), indent=1)


if __name__ == "__main__":
    main()
