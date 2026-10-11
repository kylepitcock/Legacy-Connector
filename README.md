# Retrofit

Run **unmodified legacy Forge mods on NeoForge 1.21.1**. Drop a 1.7.10 mod jar into your `mods` folder next to
this mod; it is translated at load time, the same way [Sinytra Connector](https://github.com/Sinytra/Connector)
runs Fabric mods on NeoForge. The original jar is never modified.

> **Status: early development.** 1.7.10 is the active target. Expect simple content mods to partly work and large
> mods to load with many features missing. See [Compatibility status](#compatibility-status).

## How it works

```
mods/SomeMod-1.7.10.jar
        │  loader (FML locator service, runs before mod discovery)
        ▼
  1. Inspect      detect the legacy version (mcmod.info, @Mod, FML/Forge references)
  2. Translate    relocate Minecraft/Forge/FML classes into the shim namespace, SRG -> MCP names,
                  Java 8 -> 21 fixes (removed JDK APIs, reflection on final fields, Guava, LWJGL 2 -> 3)
  3. Link         every legacy API reference is resolved against, in order:
                    a. the hand-written shim   - legacy API re-implemented on top of NeoForge (real behaviour)
                    b. the synthesized API     - generated for whatever the shim lacks (links, default behaviour)
  4. Resources    lang files, textures and models converted to 1.21 formats
        ▼
.retrofit/cache/SomeMod-<hash>.jar  +  retrofit-synth-1.7.10-<hash>.jar  ->  loaded by NeoForge
        │  runtime (normal NeoForge mod)
        ▼
  Bridges         legacy registrations -> modern registries (blocks, items with metadata, tile entities,
                  recipes -> data pack, ore dictionary -> tags, GUIs -> menus/screens, networking, world gen, ...)
```

### No per-mod code

Nothing in the layer is written for a specific mod. Mods are made to work by implementing the **legacy API** they
were written against:

- **Hand-written shim** (`src/main/java/mccompat/v1_7_10/...`): `Block`, `Item`, `ItemStack`, `World`, `Entity`,
  `GameRegistry`, `Container`, `GuiContainer`, `TileEntitySpecialRenderer`, `SimpleNetworkWrapper`, ... as views
  over the modern objects.
- **Synthesized API** (`loader/.../synth/`): any legacy class or member the shim does not implement yet is generated
  at load time from the legacy class hierarchy (shipped as `loader/src/main/resources/mccompat/legacy/<ver>/hierarchy.txt`)
  and the descriptors the mods use. Generated classes keep their real supertypes (a synthesized `BlockColored` *is*
  a `Block`), constructors chain to the closest real one, methods return safe defaults, and calls on hand-written
  classes for missing methods are dispatched to the mod's own override when it has one. This is what lets an
  arbitrary mod *load* instead of crashing with `NoClassDefFoundError`; the first call to each stub is logged so the
  most-used gaps can be implemented properly.

Mod-specific code is reserved for rare edge cases (none exist today).

### Legacy loader behaviour that is reproduced

- FML `@Optional.Interface` / `@Optional.Method` stripping for absent mods, and removal of interfaces from APIs that
  exist nowhere (what some mods' own coremods did).
- Integrations with mods that are not installed: methods that need an absent API are replaced by a lazy
  `NoClassDefFoundError`, so the rest of the class still loads (the JVM verifies whole classes at once).
- Access transformers: the shim is public and non-final throughout, as mods compiled against AT-widened Minecraft.
- FML's Scala language adapter (`object` mods) and the libraries 1.7.10 shipped: Scala 2.11, Trove, vecmath.
- LaunchWrapper's `Launch.blackboard` / `Launch.classLoader` (transformer registration is accepted and ignored).
- Mods linking against each other's classes (e.g. RotaryCraft extends DragonAPI) are linked as one modset; APIs
  shaded into several jars are merged, preferring the copy from the mod that owns the API.

### Outdated dependencies

Libraries that were never updated are replaced by their modern equivalent instead of being loaded, when that mod is
installed (the same idea as Connector Extras bridging Trinkets to Curios):

| Legacy library | Modern replacement | Status |
|---|---|---|
| Baubles 1.0 (`baubles.api`) | [Curios](https://www.curseforge.com/minecraft/mc-mods/curios) | Baubles is skipped; `IBauble` items become curios in the ring/necklace/belt slots; `BaublesApi.getBaubles` reads the Curios slots. |
| NotEnoughItems (`codechicken.nei`) | [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | NEI is skipped; mods' `NEI*Config` plugins run; each legacy recipe handler becomes a JEI category over its own GUI texture; NEI-hidden items are hidden in JEI. |
| Waila (`mcp.mobius.waila.api`) | [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) | Waila is skipped; legacy tooltip providers registered through Waila's IMC callback show their lines in Jade. |
| CoFH RF API (`cofh.api.energy`, shaded by most tech mods) | NeoForge Energy capability (built in) | Always on: RF tiles and items expose `IEnergyStorage` (1 RF = 1 FE), so modern cables and machines connect. |

Without the modern mod, the legacy library is translated and loaded as usual (except the RF API, which needs none).

Mods that only make sense on 1.7.10's engine or launcher (performance/bug-fix patches such as FastCraft or Hodgepodge,
ID extenders, LWJGL 3 ports) and client mods with modern versions (JourneyMap, Inventory Tweaks, ...) are not loaded;
`retrofit-report.txt` says why.

New bridges are declared in `loader/.../LibraryBridges.java` and implemented under the shim namespace.

### World generation

Legacy biomes (for example Biomes O' Plenty's) become real 1.21 biomes, generated as data: climate, colours, and the
generation features of the closest vanilla biome, plus the legacy biome's own surface blocks and decorator (its trees,
plants and features). Where they appear is decided by the 1.7.10 biome layer system (`GenLayer`, `WorldType`,
Forge's `BiomeManager`), re-implemented in the shim, so a mod's own layer chain (BoP's world type) runs unmodified.
1.21 terrain, oceans, rivers, beaches, mountains and caves are kept; legacy biomes replace ordinary land biomes where
the legacy layers place them. Mods' `IWorldGenerator`s (ores, structures) run for every chunk.

## Compatibility status

| Legacy version | Status |
|---|---|
| 1.7.10 | **In progress.** Offline, 129 real mods from a 140-mod test pack link with 0-34 unresolved references each, and all 1,400 synthesized API classes pass JVM verification. Iron Chest works (blocks, GUIs, upgrades, rendering, recipes); Witchery, Thaumcraft and others now get through most of loading. Mod biomes, dimensions, custom entities and fluids in the world are not bridged yet. |
| 1.8 - 1.12.2 | Planned next. Version detection and SRG/MCP mappings exist; the shim does not. |
| 1.16.5 | Planned. |
| 1.20.1 (Forge + Fabric via Sinytra Connector) | Planned. |

Not supported (and likely never fully): coremods / ASM transformers and mixins that patch legacy Minecraft code,
LaunchWrapper tweakers. They are reported, and the rest of the mod still loads.

### What happens to a mod

After launching, read **`logs/retrofit-report.txt`**. For every legacy jar it lists the detected version and mod
ids, references that could not be linked, and which legacy APIs were synthesized (they link but do nothing yet).
At runtime, `Retrofit/Stubs` log lines show which synthesized APIs are actually called.

/
## Development

Requires JDK 21.

```sh
./gradlew build                 # runtime mod + loader
./gradlew bundleJar             # single distributable jar
./gradlew runClient             # dev client, mods from run/mods
./gradlew runServerTest         # headless smoke test with run-servertest/mods; the fixture mod self-tests and stops
./gradlew checkLegacyMod -PlegacyJar=/path/a.jar,/path/b.jar   # e.g. every jar in test-mods/
                                # translate offline as one modset, list unlinkable references,
                                # generate + JVM-verify the synthesized API (no game launch)
```

Project layout:

| Path | What |
|---|---|
| `loader/` | FML locator service: jar inspection, translation, linking, API synthesis, caching, report |
| `src/main/java/mccompat/v1_7_10/` | 1.7.10 shim (legacy package names, relocated) |
| `src/main/java/org/kylepitcock/mccompat/` | runtime: bootstrap, bridges to NeoForge, JDK/library compat, synth runtime |
| `testmods/` | fixture mod compiled against legacy API stubs, built like a real 2014 jar |
| `tools/` | developer scripts: reference data download, signature/hierarchy extraction, API lookup, shim hierarchy audit |
| `test-mods/` | (not committed) local corpus of real legacy mods for `checkLegacyMod` / `runServerTest` |

Reference data (MCP mappings, Forge userdev sources, Mojang client jars) is downloaded into `build/devtools/` by
`tools/fetch_devtools.sh` and the extraction scripts; none of it is committed. Only class names and supertypes
(`hierarchy.txt`) and MCP name tables are shipped.

To regenerate the 1.7.10 hierarchy data:

```sh
tools/fetch_devtools.sh
python3 tools/extract_legacy_signatures.py 1.7.10   # needs build/devtools/mojang/client-1.7.10.jar
python3 tools/extract_legacy_hierarchy.py 1.7.10
```

## Legal

This project contains no Minecraft, Forge or third-party mod code. Do not commit or redistribute legacy mod jars;
users supply their own.
