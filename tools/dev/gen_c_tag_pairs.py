#!/usr/bin/env python3
"""Writes loader/src/main/resources/legacyconnector/forward/1.20.1/forge-c-tag-pairs.tsv and c-tags.txt.

NeoForge 21.1 defines its convention tags (data/c/tags/**) with an optional entry for the Forge tag each one replaces
({"id": "#forge:ingots/brick", "required": false}), so the jar itself says which Forge tag became which c: tag. This reads those entries:
Forge tag path TAB c tag path (no registry folder, no namespace); only pairs whose path differs, and only where the block, item and fluid
registries agree. Pairs whose c: tag is a colour tag (dyed/<colour>, which also holds wool, beds, banners) are left out: a recipe that asks for
the black stained glass tag must not accept black wool, an empty tag is the lesser error.

usage: gen_c_tag_pairs.py <neoforge-merged.jar> [out-dir]
"""
import json
import sys
import zipfile

RESOURCES = "loader/src/main/resources/legacyconnector/forward/1.20.1/"
SINGULAR = {"blocks": "block", "items": "item", "fluids": "fluid"}


def main():
    jar = zipfile.ZipFile(sys.argv[1])
    out_dir = sys.argv[2] if len(sys.argv) > 2 else RESOURCES
    c_tags = []
    pairs = {}  # forge path -> {c path: {registry}}
    for name in sorted(jar.namelist()):
        if not (name.startswith("data/c/tags/") and name.endswith(".json")):
            continue
        rel = name[len("data/c/tags/"):-5]
        c_tags.append(rel)
        registry, _, rest = rel.partition("/")
        if registry == "worldgen":
            registry, _, rest = rest.partition("/")  # worldgen/biome/is_cold: ids are relative to the registry folder
            registry = "worldgen/" + registry
        for value in json.loads(jar.read(name)).get("values", []):
            ident = value["id"] if isinstance(value, dict) else value
            if ident.startswith("#forge:"):
                pairs.setdefault(ident[len("#forge:"):], {}).setdefault(rest, set()).add(registry)
    rows = []
    for forge_path, targets in sorted(pairs.items()):
        if len(targets) != 1:
            print("ambiguous, skipped:", forge_path, sorted(targets))
            continue
        (c_path,) = targets
        if forge_path == c_path or c_path.startswith("dyed/"):
            continue
        rows.append((forge_path, c_path))
    with open(out_dir + "forge-c-tag-pairs.tsv", "w", newline="\n") as f:
        f.write("# Forge 1.20.1 tag path -> NeoForge 21.1 convention (c:) tag path; read from the '#forge:' entries of NeoForge's own data/c/tags by "
                "tools/dev/gen_c_tag_pairs.py (pairs that agree in the block, item and fluid registries, without the dyed/<colour> tags)\n")
        for forge_path, c_path in rows:
            f.write(forge_path + "\t" + c_path + "\n")
    with open(out_dir + "c-tags.txt", "w", newline="\n") as f:
        f.write("\n".join(c_tags) + "\n")
    print(len(rows), "pairs,", len(c_tags), "c: tags")


if __name__ == "__main__":
    main()
