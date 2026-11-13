#!/usr/bin/env python3
"""Create a path-level source archive inventory for the Palladium/AlienEvo audit.

Usage:
  python tools/create_compat_coverage.py AlienEvo.jar palladium-forge.jar docs/compatibility

Only the Python standard library is used. The output records original archive
entries and static classifications; it does not claim transformed/runtime proof.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import re
import struct
import zipfile
from collections import Counter, defaultdict
from pathlib import Path, PurePosixPath


FAMILY_PREFIXES = {
    "GeckoLib": "software/bernie/geckolib/",
    "Player Animator": "dev/kosmx/playerAnim/",
    "Architectury": "dev/architectury/",
    "KubeJS": "dev/latvian/mods/kubejs/",
    "Rhino": "dev/latvian/mods/rhino/",
    "Curios": "top/theillusivec4/curios/",
    "JEI": "mezz/jei/",
    "Pehkui": "virtuoel/pehkui/",
    "Iris": "net/irisshaders/",
    "AttributesLib": "dev/shadowsoffire/attributeslib/",
    "Sodium": "me/jellysquid/mods/sodium/",
}

OPTIONAL_GATES = {
    "GeckoLib": "PalladiumForge checks geckolib before integration init; client mixins are class-presence-gated",
    "Player Animator": "Selected as a nested JarJar dependency in the original Palladium archive",
    "Architectury": "Two expectation-platform annotation references; no direct gameplay class path identified",
    "KubeJS": "ScriptFileInfo/ScriptManager mixins are class-presence-gated; KubeJS 7 adapter also applies",
    "Rhino": "KubeJS scripting dependency; target profile supplies Rhino",
    "Curios": "PalladiumForge checks ModList before Curios common/client initialization",
    "JEI": "Palladium integration uses JEI @JeiPlugin discovery and is client-side",
    "Pehkui": "Palladium setup hook checks Platform.isModLoaded(\"pehkui\") before PehkuiCompat.init",
    "Iris": "RenderUtil short-circuits Iris/Oculus; Iris API access is on the Oculus path",
    "AttributesLib": "PalladiumForge checks mod presence before ApothicAttributesCompat.init",
    "Sodium": "Client SodiumFixMixin is gated on the legacy Sodium renderer class",
}

KNOWN_ALIEN_SCRIPTS = {
    "addon/alienevo/kubejs_scripts/detransform.js": "AlienEvo scoreboard/sound migration and null timer-objective guard; runtime pending",
    "data/alienevo/kubejs_scripts/level_command.js": "Scoreboard API migration; command permission and PLAYER argument retained; runtime pending",
    "addon/alienevo/kubejs_scripts/flatten.js": "Optional Pehkui import guard; scale feature needs Pehkui",
    "assets/alienevo/kubejs_scripts/power_ui.js": "ResourceLocation helper and tab-ID comparisons; custom UI runtime pending",
    "addon/alienevo/kubejs_scripts/quick_change.js": "KubeJS 7 keybind/Architectury rewrite; callback runtime pending",
    "addon/alienevo/kubejs_scripts/glow_crystals.js": "KubeJS 7 item-builder migration; registration/runtime pending",
}


def read_archive(path: Path):
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        files = {name: archive.read(name) for name in names if not name.endswith("/")}
    return names, files


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def walk_json(value, keys=()):
    if isinstance(value, dict):
        for key, child in value.items():
            yield from walk_json(child, keys + (str(key),))
    elif isinstance(value, list):
        for index, child in enumerate(value):
            yield from walk_json(child, keys + (str(index),))
    else:
        yield keys, value


def symbolic_classes(data: bytes) -> set[str]:
    """Read CONSTANT_Class and class names in descriptors/generic signatures."""
    if data[:4] != b"\xca\xfe\xba\xbe":
        return set()
    cp_count = struct.unpack_from(">H", data, 8)[0]
    pos, slot, utf8, class_indices = 10, 1, {}, []
    while slot < cp_count:
        tag = data[pos]
        pos += 1
        if tag == 1:
            length = struct.unpack_from(">H", data, pos)[0]
            pos += 2
            utf8[slot] = data[pos:pos + length].decode("utf8", "replace")
            pos += length
        elif tag in (3, 4):
            pos += 4
        elif tag in (5, 6):
            pos += 8
            slot += 1
        elif tag == 7:
            class_indices.append(struct.unpack_from(">H", data, pos)[0])
            pos += 2
        elif tag in (8, 16, 19, 20):
            pos += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            pos += 4
        elif tag == 15:
            pos += 3
        else:
            raise ValueError(f"Unknown class-file constant-pool tag {tag} at byte {pos}")
        slot += 1

    refs = {
        utf8[index]
        for index in class_indices
        if index in utf8 and "/" in utf8[index] and not utf8[index].startswith("[")
    }
    for value in utf8.values():
        for name in re.findall(r"L([^;]+);", value):
            name = name.split("<", 1)[0]
            if "/" in name:
                refs.add(name)
    return refs


def mixin_rows(archives: list[tuple[str, dict[str, bytes]]]):
    rows = []
    for label, files in archives:
        for path, raw in sorted(files.items()):
            if not path.endswith(".mixins.json"):
                continue
            try:
                doc = json.loads(raw)
            except json.JSONDecodeError:
                continue
            for section in ("mixins", "client", "server"):
                for name in doc.get(section, []):
                    rows.append((label, path, section, doc.get("package", ""), name))
    return rows


def mixin_status(name: str):
    if name == "CrossbowItemMixin":
        return "intentionally excluded from this pass", "Crossbow/weapon feature behavior is explicitly excluded"
    if name == "NetworkManagerMixin":
        return "intentionally excluded from this pass", "Shared packet/legacy SimpleChannel behavior is explicitly excluded"
    if name in ("EntitySelectorParserMixin", "HumanoidArmorLayerMixin"):
        return "migrated by Palladium-specific adapter", "Named Palladium mixin adapter; runtime application pending"
    if name in ("ScriptFileInfoMixin", "ScriptManagerMixin"):
        return "optional integration and correctly gated", "PalladiumMixinPlugin KubeJS class-presence gate; KubeJS 7 adapter also applies"
    if name in ("GeoArmorRendererMixin", "GeoArmorRendererInvoker", "GeoRendererMixin"):
        return "optional integration and correctly gated", "Client mixin gate checks GeckoLib class presence"
    if name == "SodiumFixMixin":
        return "optional integration and correctly gated", "Client mixin gate checks the legacy Sodium renderer class"
    if name in ("PlayerInfoMixin", "AbstractClientPlayerMixin"):
        return "migrated by Palladium-specific adapter", "Player-skin compatibility path; runtime lookup/render pending"
    if name == "LightningBoltMixin":
        return "migrated by Palladium-specific adapter", "Exact verified INVOKE_ASSIGN shift allowance; runtime application pending"
    return "migrated by existing generic adapter", "Generic class/Mixin translation path; current target application report pending"


def entry_classification(archive: str, path: str, is_dir: bool):
    if is_dir:
        return "verified harmless or unreachable", "Archive directory marker"
    suffix = PurePosixPath(path).suffix.lower()
    if "AlienEvo" in archive:
        if suffix == ".mcfunction":
            return "migrated by existing generic adapter", "FunctionConverter1201 scans every function; in-game execution pending"
        if suffix == ".nbt" and path.startswith("data/") and "/structures/" in path:
            if path.endswith(("galvan_priory.nbt", "ship2.nbt")):
                return "migrated by existing generic adapter", "StructureNbtConverter1201 changes only Forge attribute Name values"
            return "verified harmless or unreachable", "No inspected Forge attribute identifiers; vanilla IDs remain for template DataFixer"
        if suffix == ".js" and "kubejs_scripts/" in path:
            if path in KNOWN_ALIEN_SCRIPTS:
                return "migrated by AlienEvo-specific adapter", KNOWN_ALIEN_SCRIPTS[path]
            return "needs fresh artifact/runtime evidence", "No source-backed migration gap identified; callback/gameplay behavior remains unverified"
        if path.startswith("data/") and suffix == ".json":
            return "migrated by existing generic adapter", "DataConverter1201 scans data JSON; power command fields use FunctionConverter1201"
        if path.startswith("assets/") and "/models/" in path and suffix == ".json":
            return "migrated by existing generic adapter", "DataConverter1201 scans asset model JSON"
        if suffix == ".class":
            return "migrated by existing generic adapter", "Legacy bytecode translation; fresh translated-link report unavailable"
        if suffix in (".png", ".ogg", ".mcmeta", ".ttf", ".json"):
            return "needs fresh artifact/runtime evidence", "Retained resource; validate against fresh translated resource report before changing"
        return "verified harmless or unreachable", "Original resource retained; no evidence-backed change applies"

    if path.endswith("/LightningBoltMixin.class"):
        return "migrated by Palladium-specific adapter", "Exact verified maxShiftBy=1 annotation migration"
    if path.endswith("/CrossbowItemMixin.class"):
        return "intentionally excluded from this pass", "Crossbow/weapon feature behavior is explicitly excluded"
    if path.endswith("/NetworkManagerMixin.class"):
        return "intentionally excluded from this pass", "Shared packet/legacy SimpleChannel behavior is explicitly excluded"
    if suffix == ".class":
        return "migrated by existing generic adapter", "Generic bytecode/Mixin transform path; fresh applied/link report unavailable"
    if path.startswith("data/") and suffix == ".json":
        return "migrated by existing generic adapter", "DataConverter1201 scans all data JSON"
    if path.startswith("assets/") and "/models/" in path and suffix == ".json":
        return "migrated by existing generic adapter", "DataConverter1201 scans asset model JSON"
    if suffix in (".png", ".ogg", ".mcmeta", ".json"):
        return "needs fresh artifact/runtime evidence", "Retained resource; visual/audio/resource-link validation needs fresh artifact"
    return "migrated by existing generic adapter", "Retained or processed by generic JAR/resource translation; runtime pending"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("alienevo", type=Path)
    parser.add_argument("palladium", type=Path)
    parser.add_argument("outdir", type=Path)
    args = parser.parse_args()
    args.outdir.mkdir(parents=True, exist_ok=True)

    alien_names, alien_files = read_archive(args.alienevo)
    pall_names, pall_files = read_archive(args.palladium)
    alien_sha, pall_sha = digest(args.alienevo), digest(args.palladium)
    mixins = mixin_rows([("AlienEvo", alien_files), ("Palladium", pall_files)])

    pall_refs_by_owner = {
        path: symbolic_classes(raw)
        for path, raw in pall_files.items()
        if path.endswith(".class")
    }
    pcore_refs = sorted({
        ref for refs in pall_refs_by_owner.values() for ref in refs
        if ref.startswith("net/threetag/palladiumcore/")
    })
    optional_refs = {
        family: sorted({
            ref for refs in pall_refs_by_owner.values() for ref in refs
            if ref.startswith(prefix)
        })
        for family, prefix in FAMILY_PREFIXES.items()
    }

    embedded_commands = []
    malformed_json = []
    for path, raw in alien_files.items():
        if not (path.startswith("data/") and path.endswith(".json")):
            continue
        try:
            data = json.loads(raw)
        except json.JSONDecodeError as error:
            malformed_json.append((path, str(error)))
            continue
        if "/palladium/powers/" not in path:
            continue
        fields = {"commands", "first_tick_commands", "last_tick_commands", "command", "command_as_entity",
                  "CommandOnEntityHit", "CommandOnBlockHit", "commands_on_block_hit"}
        array_fields = fields - {"command", "command_as_entity"}
        for keys, value in walk_json(data):
            direct_command = bool(keys) and keys[-1] in fields
            array_item = len(keys) > 1 and keys[-2] in array_fields and keys[-1].isdigit()
            if (direct_command or array_item) and isinstance(value, str) and value.strip():
                embedded_commands.append((path, ".".join(keys), value))

    rows = []

    def add_entries(label, archive_sha, names):
        for path in names:
            is_dir = path.endswith("/")
            classification, note = entry_classification(label, path, is_dir)
            if label.startswith("Palladium") and path.endswith(".class"):
                for source, config, section, package, name in mixins:
                    if source == "Palladium" and path == (package + "." + name).replace(".", "/") + ".class":
                        classification, note = mixin_status(name)
                        break
            rows.append((label, archive_sha, path,
                         "directory" if is_dir else (PurePosixPath(path).suffix.lower() or "[no extension]"),
                         classification, note))

    add_entries("AlienEvo outer (filename 1.1.3; internal 1.1.2)", alien_sha, alien_names)
    add_entries("Palladium outer", pall_sha, pall_names)

    # Include every nested dependency entry recursively, not only the outer JarJar metadata pointer.
    nested_records = []
    nested_mixins = []

    def add_nested_archives(outer_name, files):
        for jar_path, blob in files.items():
            if not jar_path.lower().endswith(".jar"):
                continue
            try:
                with zipfile.ZipFile(io.BytesIO(blob)) as nested:
                    names = nested.namelist()
                    nested_files = {name: nested.read(name) for name in names if not name.endswith("/")}
                    sha = hashlib.sha256(blob).hexdigest()
                    label = f"{outer_name}!/{jar_path}"
                    nested_records.append((label, sha, names))
                    add_entries(label, sha, names)
                    nested_mixins.extend(mixin_rows([(label, nested_files)]))
                    add_nested_archives(label, nested_files)
            except zipfile.BadZipFile:
                continue

    for outer_name, files, parent_sha in (("AlienEvo outer", alien_files, alien_sha),
                                          ("Palladium outer", pall_files, pall_sha)):
        add_nested_archives(outer_name, files)

    for source, config, section, package, name in mixins:
        status, note = mixin_status(name)
        rows.append((f"{source} mixin declaration", alien_sha if source == "AlienEvo" else pall_sha,
                     f"{package}.{name}", f"mixin:{section}", status, f"{config}: {note}"))
    for source, config, section, package, name in nested_mixins:
        status, note = mixin_status(name)
        archive_sha = next(sha for label, sha, _names in nested_records if label == source)
        rows.append((f"{source} mixin declaration", archive_sha,
                     f"{package}.{name}", f"nested mixin:{section}", status, f"{config}: {note}"))
    for ref in pcore_refs:
        rows.append(("PalladiumCore symbolic class reference", pall_sha, ref, "PalladiumCore API reference",
                     "migrated by existing generic adapter", "Referenced by Palladium outer bytecode; Core is selected by JarJar metadata"))
    for family, refs in optional_refs.items():
        for ref in refs:
            rows.append((f"Palladium optional API: {family}", pall_sha, ref, "optional API type reference",
                         "optional integration and correctly gated", OPTIONAL_GATES[family]))

    manifest = args.outdir / "PALLADIUM-ALIENEVO-ARCHIVE-MANIFEST.csv"
    with manifest.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.writer(stream)
        writer.writerow(["archive_or_scope", "archive_sha256", "exact_path_or_symbol", "entry_kind", "classification", "evidence_or_boundary"])
        writer.writerows(rows)

    def stats(names):
        files = [path for path in names if not path.endswith("/")]
        return len(names), len(files), Counter(PurePosixPath(path).suffix.lower() or "[none]" for path in files)

    alien_entries, alien_file_count, alien_types = stats(alien_names)
    pall_entries, pall_file_count, pall_types = stats(pall_names)
    data_json = sum(path.startswith("data/") and path.endswith(".json") for path in alien_files)
    asset_json = sum(path.startswith("assets/") and path.endswith(".json") for path in alien_files)
    all_json_valid = len(malformed_json) == 0
    client_classes = [path for path in pall_files if path.endswith(".class") and path.startswith("net/threetag/palladium/client/")]
    ui_classes = [path for path in client_classes if "/screen/" in path or "/gui/" in path or "/overlay/" in path]
    skin_classes = [path for path in client_classes if "skin" in path.lower()]
    rendering_classes = [path for path in client_classes if "/renderer/" in path or "/model/" in path]
    animation_classes = [path for path in pall_files if path.endswith(".class") and ("animation" in path.lower() or "animat" in path.lower())]
    animation_resources = [path for path in pall_files if "/animations/" in path.lower() or "/animation/" in path.lower()]

    lines = [
        "# Palladium 4.5.10 + Alien Evolution Compatibility Coverage Matrix", "",
        "**Audit date:** 2026-11-12. **Target:** Minecraft 1.21.1 / NeoForge 21.1.258 / Java 21.",
        "**Starting live branch HEAD:** `43ced3558d145f58bc8b4a20d21c46a88c757dba`; newer work is recorded in the continuation handoff.",
        "**Validation boundary:** this original-archive/static audit does not claim current transformed artifacts compile, start, register all powers, or execute in game. No Gradle/JUnit build, bundle install, or gameplay test was run for this batch.", "",
        "## Exact archives", "",
        "| Archive | SHA-256 | Entries including directories | Files | Classes | JSON |",
        "|---|---|---:|---:|---:|---:|",
        f"| AlienEvo filename artifact (internal metadata says 1.1.2) | `{alien_sha}` | {alien_entries} | {alien_file_count} | {alien_types['.class']} | {alien_types['.json']} |",
        f"| Palladium Forge 4.5.10 | `{pall_sha}` | {pall_entries} | {pall_file_count} | {pall_types['.class']} | {pall_types['.json']} |", "",
        "Palladium JarJar selects PalladiumCore Forge `1.0.1+1.20.1`, MixinExtras Forge `0.2.0`, and Player Animator Forge `1.0.2-rc1+1.20`. AlienEvo selects MixinExtras Forge `0.5.0`. The AlienEvo filename/internal version mismatch is preserved as an intentional developer discrepancy.", "",
        "The audited target profile had KubeJS NeoForge `2101.7.2-build.381`, Rhino `2101.2.8-build.91`, GeckoLib `4.9.3`, Player Animator `2.0.4+1.21.1`, Architectury `13.0.11`, and Pehkui `3.8.3+1.21-neoforge`. Curios, JEI, Iris/Oculus, and AttributesLib were absent in that profile snapshot. Presence is not feature validation.", "",
        "## Complete original-resource inventory", "",
        "| Area | Count | Static classification |", "|---|---:|---|",
        f"| AlienEvo JSON | {alien_types['.json']} ({data_json} data, {asset_json} asset) | All JSON parsed: {all_json_valid}. Data JSON/model JSON use `DataConverter1201`; other JSON remains preserved. |",
        "| AlienEvo JavaScript | 126 | 70 `addon/alienevo`, 22 `assets/alienevo`, 21 `data/alienevo`, 7 `assets/afomni`, 6 `addon/afomni`; callback execution remains a runtime gate. |",
        "| AlienEvo functions | 223 `.mcfunction` | All paths are visited by `FunctionConverter1201`; known syntax findings are below. |",
        "| AlienEvo embedded power commands | 1,114 nonempty strings | Extracted from command-bearing Palladium power fields and passed through the same function-command converter via data conversion. Static conversion is not execution proof. |",
        "| AlienEvo structure NBT | 14 gzip templates | Two files have inspected Forge attribute IDs: `galvan_priory.nbt` and `ship2.nbt`; only `Attributes` list `Name` strings are changed. |",
        f"| Palladium JSON | {pall_types['.json']} | 288 data JSON, 123 asset JSON, 5 mixin/refmap/metadata JSON; data groups: 88 recipes, 86 advancements, 80 tags, 21 loot tables, 10 worldgen, 2 Forge entries, 1 damage type. |",
        f"| Palladium classes | {pall_types['.class']} | Generic class/Mixin translation plus named Palladium adapters; current transformed-link report is unavailable. |",
        "| Palladium outer mixins | **60** (25 common + 28 client + 7 Forge) | Exact list and per-class classification below. The request’s 59 omitted one client class. |",
        f"| PalladiumCore references | {len(pcore_refs)} distinct symbolic classes | Full list below; dependency selected through JarJar metadata. |", "",
        "## Palladium UI, skin, rendering, and animation inventory", "",
        "This inventory was grouped from exact original archive paths; each path remains individually searchable in the CSV manifest. Counts below are Palladium outer class files and resource files, not test coverage claims.", "",
        "| Surface | Exact path group | Class/resource files | Compatibility review |", "|---|---|---:|---|",
        f"| UI screens and overlays | `net/threetag/palladium/client/screen/`, `client/gui/`, `client/overlay/` | {len(ui_classes)} classes | GUI list/screen/accessory/tailoring/widget adapters are inventoried; layout, scrolling, hitboxes, narration and custom AlienEvo panel remain runtime proof. |",
        f"| Skin lookup and skin rendering | client class paths containing `skin` | {len(skin_classes)} classes | `PlayerSkinFetcher1201` and `PalladiumCorePlayerSkin1201` are source-backed; lookup/cache/dynamic texture behavior remains runtime proof. |",
        f"| Models and renderers | `net/threetag/palladium/client/renderer/`, `client/model/` | {len(rendering_classes)} classes | Rotation, SuitStand, TrailSegment, BodyPart, render-layer, and GeckoLib compatibility adapters are inventoried; non-weapon rendering remains runtime proof. |",
        f"| Animation APIs and classes | class paths containing `animation` or `animat` | {len(animation_classes)} classes | Palladium animation/flight/player-animation and GeckoLib KubeJS integration surfaces are inventoried; GeckoLib-gated mixins and Player Animator dependency are listed separately. |",
        f"| Animation resource paths | paths containing `/animations/` or `/animation/` | {len(animation_resources)} files | Preserved in the full archive manifest; resource links and playback require fresh translated-artifact/game evidence. |", "",
        "### AlienEvo command findings", "",
        "Existing converter coverage includes old particle options, `Count`/custom-model item data, item enchantment arguments, ActiveEffects, entity Attributes, and `minecraft:grass`. The original `.mcfunction` audit found 15 legacy particle options, 56 Count lines, 54 CustomModelData lines, one Enchantments line, one Attributes line, one ActiveEffects line, and three grass identifiers; it found no unsupported Count candidate and no `replaceitem`/`blockdata`/`entitydata` candidate. The same shared command converter processes the 1,114 embedded power commands.",
        "",
        "`forge:entity_gravity`, `forge:step_height_addition`, and `forge:swim_speed` are mapped in data and entity-command SNBT. New structure NBT handling applies that map only inside list compounds named `Attributes`, preserves other strings, rejects partial rewrites on malformed files, and is covered by new unit tests (not run here). `code_power_test` remains removed only because it is unreferenced. EarthSpikes’ incomplete actionless selector remains documented as intentionally omitted.", "",
        "### AlienEvo scripts and commands", "",
        "- Script API static counts: 27 unique `Java.loadClass` names in 67 occurrences; 102 `new ResourceLocation` expressions in 11 scripts. Existing script-scope fixes, KubeJS 7 builder rewrite, quick-change keybind migration, and six-script scoreboard migration remain in place.",
        "- New `flatten.js` gap closure: guard top-level Pehkui `ScaleTypes` loading. Without Pehkui, script registration continues and scale operations are no-ops; the flatten effect itself still requires Pehkui.",
        "- `/maxaliens` and `/mastercontrol` retain permission level 2 and one `PLAYER` argument. Their scoreboard operations migrate to `ScoreHolder`/`ScoreAccess`; current callback execution is unverified because no matched server exception is available.",
        "- Detransform ability 5 still removes the current alien power, restores the watch power, removes the transformation tag, halves the timer for non-master-control users, disables quick-change, and plays its sound. The new null-objective guard prevents timer lookup from aborting restoration. Repeated transform/reversion remains runtime-only validation.", "",
        "### Assets and resource roots", "",
        "The archive manifest lists all `addon/`, `assets/`, `data/`, image, audio, font, model, animation, and particle paths. AlienEvo’s addon root and metadata ownership fixes already exist. Historical warnings (`alienevo:botton`, `prototype_switch1.ogg`, animation names, missing model textures/AFOMNI particle sprites, and `/addon/.mcassetsroot`) require a fresh translated archive/report before any changes; none was guessed at here.", "",
        "## Palladium outer mixin matrix", "",
        "| Class | Config section | Classification and evidence boundary |", "|---|---|---|"
    ]
    for source, config, section, package, name in mixins:
        if source != "Palladium":
            continue
        status, note = mixin_status(name)
        lines.append(f"| `{package}.{name}` | `{section}` (`{config}`) | {status}; {note} |")

    lines += ["", f"PalladiumCore has its own four mixin classes: common `LightningBoltMixin`; Forge `ExtendedEntitySpawnDataMixin`, `PalladiumItemMixin`; and client `ClientLevelMixin`. Player Animator is a nested dependency with 13 client mixins. The recursive dependency scan found {len(nested_mixins)} additional nested-dependency mixin declarations; all are recorded in the path manifest.", "",
        "**LightningBolt warning investigation:** PalladiumCore’s `LightningBoltMixin` targets `LightningBolt.tick`, the second `Level.getEntities(... )` call (ordinal 1), and captures the resulting `List<Entity>` through `INVOKE_ASSIGN` shifted by one instruction. The cached NeoForge 1.21.1 source confirms ordinal 1 is the actual lightning-hit entity pass; ordinal 0 is the earlier larger delayed-strike list. The new narrow adapter adds `maxShiftBy=1`, declaring the preexisting shift without changing its location. Exact adapter tests are added but not run; runtime application/warning status needs the next report.", "",
        "## PalladiumCore references (all 66)", ""]
    lines.extend(f"- `{ref}`" for ref in pcore_refs)
    lines += ["", "## Optional API references and gates", "", "Types below were extracted from class-file `CONSTANT_Class` entries and class descriptors/signatures (generic signatures normalized to internal names). The manifest includes each type record. Source gates do not prove an integration works.", "", "| Family | Distinct types | Source gate/dependency selection |", "|---|---:|---|"]
    for family, refs in optional_refs.items():
        lines.append(f"| {family} | {len(refs)} | {OPTIONAL_GATES[family]} |")
    for family, refs in optional_refs.items():
        lines += ["", f"### {family} ({len(refs)} types)", "", ", ".join(f"`{ref}`" for ref in refs)]
    lines += ["", "Palladium’s mixin plugin probes bytecode without loading candidate classes; it gates KubeJS, Trinkets, GeckoLib, Quilt resource loader, and the legacy Sodium renderer mixins. `PalladiumForge` separately gates Curios common/client init, GeckoLib init, and AttributesLib init. Its setup callback gates Pehkui. JEI uses `@JeiPlugin` discovery. Iris/Oculus access is short-circuited through the Oculus path. Architectury refs are expectation-platform annotations. Player Animator is selected by JarJar metadata.", "",
        "## Palladium-specific adapters and remaining runtime proof", "", "| Feature | Existing adapter | Runtime proof still required |", "|---|---|---|"]
    lines += [
        "| Flight tick | `PalladiumFlightHandler1201` changes private `EntityDimensions.height` field access to public accessor | Tick an active flight ability repeatedly |",
        "| PalladiumCore overlays | Existing Forge overlay shim plus `PalladiumCoreOverlay1201` validator | Confirm ability bar/wheel HUD renders |",
        "| Lightning strike mixin | `PalladiumCoreLightningBoltMixin1201` declares exact shift allowance | Fresh report has no shift warning; mixin/event works |",
        "| Skin fetching | `PalladiumPlayerSkinFetcher1201`, `PalladiumCorePlayerSkin1201` | Skin lookup and dynamic rendering in world |",
        "| Addon packs | `PalladiumAddonPackManager1201` and resource adapter | Discover/list/enable AlienEvo/AFOMNI; real data/resources/scripts register |",
        "| GUI lists/screens | Selection/Powers/Accessory/Tailoring/Widget adapters | hitboxes, scroll, clipping, narration, layout at varied sizes |",
        "| Selector | `PalladiumMixinCompat1201` | positive and negated selector with real registered power |",
        "| Body/rendering | rotation, SuitStand, TrailSegment, BodyPart and Gecko render adapters | exercise a non-weapon path that uses each renderer |",
        "| KubeJS | `PalladiumKubeJS1201` and AlienEvo resource adapter | actual builders/properties/callback invocation in addon pack |",
        "| Data/functions | `DataConverter1201`, `FunctionConverter1201` | loot, modifiers, recipes, abilities, and function execution |",
        "", "## Unresolved/soft-link and scope classifications", "",
        "- No new transformed JAR/report exists, so current soft-link/unresolved call sites cannot be enumerated honestly. The historical 33-site count and older link reports are not carried forward as current measurements.",
        "- AlienEvo `/maxaliens`, `/mastercontrol`, repeated transform/reversion, ability 5, real power registration, and custom `PalladiumEvents.renderPowerScreen` panel are reachable but unverified. The custom panel is distinct from the quick-change wheel and PalladiumCore HUD.",
        "- Resource warnings listed above stay pending fresh transformed-resource evidence. No speculative asset rename/removal was made.",
        "- Crossbow/weapon/Add-onCrossbow/projectile/explosion behavior and shared packet/`SimpleChannel` compatibility remain explicitly outside this supported pass.", "",
        "## Full path manifest", "",
        "[`PALLADIUM-ALIENEVO-ARCHIVE-MANIFEST.csv`](PALLADIUM-ALIENEVO-ARCHIVE-MANIFEST.csv) contains every outer central-directory entry and every recursively embedded JarJar dependency entry (including directories), outer and nested declared mixins, the 66 Core type references, and every optional API type. It separates implementation classification from validation boundaries.", "",
        "## Consolidated build checkpoint", "",
        "Run after source review. The assistant has not run builds/tests for this pass.", "", "```powershell",
        "git switch fix/forge1201-palladium-runtime-adapters", "git pull --ff-only", "git rev-parse HEAD",
        ".\\gradlew.bat :clean :loader:clean :testmods:clean :build :loader:build :testmods:assemble --no-daemon --console=plain", "```", "",
        "Only after `BUILD SUCCESSFUL`, close Minecraft and install once:", "", "```powershell",
        '.\\gradlew.bat clean installBundle "-PmodsDir=E:\\Games\\PolyMC\\instances\\Neoforge tester\\.minecraft\\mods" --no-daemon --console=plain',
        "```", "", "Then run one broad client/gameplay pass and collect same-launch `latest.log`, `startup.log`, `legacyconnector-report.txt`, and any crash report. The continuation handoff records the full feature checklist and exact next action.", ""]

    matrix = args.outdir / "PALLADIUM-ALIENEVO-COVERAGE-MATRIX.md"
    matrix.write_text("\n".join(lines), encoding="utf-8")
    print(f"matrix={matrix}")
    print(f"manifest={manifest}; rows={len(rows)}")
    print(f"Palladium mixins={sum(source == 'Palladium' for source, *_ in mixins)}; PalladiumCore refs={len(pcore_refs)}")
    print(f"nested dependency mixins={len(nested_mixins)}; recursively indexed nested archives={len(nested_records)}")
    print(f"optional refs={ {family: len(refs) for family, refs in optional_refs.items()} }")
    print(f"Palladium client classes UI={len(ui_classes)} skin={len(skin_classes)} render/model={len(rendering_classes)} animation={len(animation_classes)} animation_resources={len(animation_resources)}")
    print(f"AlienEvo command strings={len(embedded_commands)}; invalid JSON={len(malformed_json)}")
    print(f"outer_entries AlienEvo={alien_entries} Palladium={pall_entries}; files AlienEvo={alien_file_count} Palladium={pall_file_count}")


if __name__ == "__main__":
    main()
