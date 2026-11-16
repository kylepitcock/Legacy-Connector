#!/usr/bin/env python3
"""EXPERIMENTAL (1.19.2 scoping, docs/wiki/Legacy-1.19.2-Estimate.md): data/resource differences between Forge 1.19.2 and 1.20.1 jars that tools/compat/data1201.py does
not look for (it knows the 1.20.1 -> 1.21.1 changes). Counts files and jars per finding in a directory of jars.

usage: python -I data1192_extra.py <jar-dir> [--json out.json]
"""
import collections, json, pathlib, re, sys, zipfile

CHECKS = {
    # 1.19.4: smithing recipes became smithing_transform / smithing_trim (the old type needs a template slot)
    "smithing_old_type": (re.compile(r'"type"\s*:\s*"minecraft:smithing"'), "recipes"),
    "smithing_transform": (re.compile(r'"type"\s*:\s*"minecraft:smithing_(transform|trim)"'), "recipes"),
    # 1.19.4: damage types are a data-driven registry; tags/damage_type is new, tag files naming DamageSource ids do not exist in 1.19.2
    "damage_type_tags_or_json": (re.compile(r"damage_type"), "data"),
    # recipe categories (1.19.3): "category" on crafting recipes (optional field)
    "recipe_category": (re.compile(r'"category"\s*:'), "recipes"),
    # 1.19.3: feature flags (data/<ns>/... "required_features") and the 1.20 trims/armor_trim data
    "feature_flags": (re.compile(r'"required_features"|feature_flags'), "data"),
    # 1.20: item model json "display" is the same; 1.19.3 changed block light/biome tags: tag names for structures
    "biome_json": (re.compile(r'"has_precipitation"|"precipitation"'), "worldgen/biome"),
    "alternative_condition": (re.compile(r'"(minecraft:)?alternative"'), "loot_tables"),
    "item_predicate_nbt": (re.compile(r'"nbt"\s*:'), "any"),
    "text_component_legacy": (re.compile(r'"(translate|text)"\s*:'), "any"),
    "chat_type": (re.compile(r'chat_type'), "data"),
    "forge_condition": (re.compile(r'"forge:'), "any"),
}


def main():
    jar_dir = pathlib.Path(sys.argv[1])
    files = collections.Counter()
    jars = collections.defaultdict(set)
    pack_fmt = collections.Counter()
    pack_fmt_res = collections.Counter()
    n = 0
    for j in sorted(jar_dir.glob("*.jar")):
        try:
            z = zipfile.ZipFile(j)
        except Exception:
            continue
        n += 1
        for name in z.namelist():
            if name == "pack.mcmeta":
                try:
                    pf = json.loads(z.read(name).decode("utf-8", "replace"))["pack"]
                    pack_fmt[str(pf.get("pack_format"))] += 1
                except Exception:
                    pack_fmt["unreadable"] += 1
                continue
            if not name.endswith(".json") or not (name.startswith("data/") or name.startswith("assets/")):
                continue
            where = name.split("/")
            try:
                text = z.read(name).decode("utf-8", "replace")
            except Exception:
                continue
            for k, (rx, area) in CHECKS.items():
                if area == "recipes" and "/recipes/" not in name:
                    continue
                if area == "loot_tables" and "/loot_tables/" not in name:
                    continue
                if area == "worldgen/biome" and "/worldgen/biome/" not in name:
                    continue
                if area == "data" and not name.startswith("data/"):
                    continue
                if k in ("text_component_legacy",) and not (name.startswith("assets/") is False and "/advancements/" in name):
                    continue
                if k == "item_predicate_nbt" and "/advancements/" not in name and "/loot_tables/" not in name:
                    continue
                if rx.search(text) or (k == "damage_type_tags_or_json" and "damage_type" in name):
                    files[k] += 1
                    jars[k].add(j.name)
    print(f"jars: {n}; pack_format of pack.mcmeta: {dict(pack_fmt)}")
    out = {"jars": n, "pack_format": dict(pack_fmt)}
    for k in CHECKS:
        print(f"{k:28s} files {files[k]:7d} jars {len(jars[k]):4d}")
        out[k] = {"files": files[k], "jars": len(jars[k])}
    if "--json" in sys.argv:
        pathlib.Path(sys.argv[sys.argv.index("--json") + 1]).write_text(json.dumps(out, indent=1), encoding="utf-8")


if __name__ == "__main__":
    main()
