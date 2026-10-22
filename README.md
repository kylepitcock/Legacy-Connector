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
- Coremods: their plugins are constructed and set up as FML did (construct all, then `getASMTransformerClass` /
  `injectData` / setup hook by sorting index; mod containers count as loaded mods). Their class transformers run over
  the legacy mod classes before translation, in a stand-in LaunchWrapper class loader with the legacy FML API, so
  coremods that patch *mod* classes work: CoFH `@Strippable`, AE2 `@Integration`, OpenMods, Steve's Addons, patcher
  mods fixing other mods' API mismatches. Transformers aimed at Minecraft's own classes find nothing to patch.
- LaunchWrapper's `Launch.blackboard` / `Launch.classLoader`.
- Resource URLs: mod jars are served from NeoForge's `union:` file system; mods that list their own jar's files
  (checking for `jar:`/`file:` URLs) get the equivalent `jar:file:` URL.
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

### Bridges at a glance

Two kinds of bridge exist. *Library bridges* replace an outdated legacy library by its modern equivalent (the table
above); *engine bridges* map a legacy system onto the 1.21 one it corresponds to. Status for 1.7.10:

**Implemented**

| Bridge | Legacy side | Modern side |
|---|---|---|
| Baubles → Curios | `baubles.api` | Curios slots |
| NEI → JEI | `codechicken.nei` | JEI categories and hidden items |
| Waila → Jade | `mcp.mobius.waila.api` | Jade tooltips |
| CoFH RF → NeoForge Energy | `cofh.api.energy` | `IEnergyStorage` (1 RF = 1 FE) |
| Blocks and tile entities | `Block`, `TileEntity`, metadata | registered blocks and block entities, one per legacy block |
| Items and ItemStacks | `Item`, `ItemStack` damage/NBT | one item per metadata subtype, `CUSTOM_DATA` NBT |
| Recipes and smelting | `GameRegistry` / `CraftingManager` / `OreDictionary` | data-pack recipes, tags |
| Entities | `Entity`, spawn/save/load, mob AI | entity types, spawns, saving |
| Fluids | Forge `Fluid` / containers | fluid types |
| GUIs and containers | `GuiHandler`, `Container` | menus and screens |
| Networking | FML `SimpleNetworkWrapper` / channels | NeoForge payloads |
| Commands | `ICommand`, server command manager | Brigadier |
| Biomes, world types, mod dimensions | `BiomeGenBase`, `GenLayer`, `WorldProvider` | data-driven biomes, `retrofit:dim_<id>` |
| World generation | `IWorldGenerator`, decorators | chunk generation hooks |
| Events | Forge/FML event buses | NeoForge event bus |
| Sounds, potions, creative tabs | `SoundHandler`, `Potion`, `CreativeTabs` | sound events, effects, tabs |
| Stats and achievements | `StatList`, `AchievementList` | local bookkeeping (counts not tracked yet) |
| Coremods that patch mods | `IClassTransformer` plugins | applied to mod classes before translation |
| Java 8 → 21 | reflection hacks, removed JDK classes | `ReflectionCompat`, `ResourceCompat`, JDK stand-ins |
| API drift between mods | a library method whose return type changed | relinked to the installed signature |

**In progress / partial**

| Bridge | State |
|---|---|
| Entity rendering, armour and held items | bridged, not verified in game for every mod |
| Block rendering (`ISimpleBlockRenderingHandler`) | 3D inventory renders work; per-vertex colour and ambient occlusion of handler-drawn blocks missing |
| Dimension sky/fog rendering and portal placement | not done |
| Statistics counting | stats exist for mods to register against; the modern stats are not driven by them |
| Coremods that patch Minecraft itself | cannot run on the shim; the common cases (DragonAPI's recipe list and smelting hook) are handled one by one |
| Mods built for a different version of another mod | only return-type drift is relinked (Binnie's mods against Forestry 4.2.16 still fail) |
| 1.8 - 1.12.2, 1.16.5, 1.20.1 (+ Sinytra) bridges | planned, in that order |

### World generation

Legacy biomes (for example Biomes O' Plenty's) become real 1.21 biomes, generated as data: climate, colours, and the
ores, caves and small plants of the closest vanilla biome. Trees and grass come from the legacy biome itself: its
decorator runs the legacy passes (the biome's own tree and plant generators) and its extra features, gated by Forge's
terrain-gen events as on 1.7.10. Where legacy biomes appear is decided by the 1.7.10 biome layer system (`GenLayer`,
`WorldType`, Forge's `BiomeManager`), re-implemented in the shim, so a mod's own layer chain runs unmodified.
1.21 terrain, oceans, rivers, beaches, mountains and caves are kept; legacy biomes replace ordinary land biomes where
the legacy layers place them. Mods' `IWorldGenerator`s (ores, structures) run for every chunk.

A world type a mod registers (BoP's "BIOMESOP") appears on the Create World screen's world type button; on a
dedicated server use `level-type=<modid>:<type>` (e.g. `biomesoplenty:biomesop`). With the default type, mod biomes
that mods added to Forge's `BiomeManager` still generate.

### Mod dimensions

A dimension a legacy mod registers with `DimensionManager.registerDimension` becomes the modern dimension
`retrofit:dim_<id>`. Its chunk generator (`LegacyChunkGenerator`) runs the mod's `WorldProvider.createChunkGenerator()`:
terrain comes from `provideChunk` (block/metadata arrays), trees, structures and ores from `populate`. Biomes come from the
provider's own `WorldChunkManager` layers (`LegacyBiomeSource`), and `World.provider` is an instance of the mod's provider class.
`ServerConfigurationManager.transferPlayerToDimension` and `Teleporter` work as in 1.7.10. Verified with Twilight Forest's
terrain; portal placement and dimension-specific rendering (sky, fog) are not done yet.

### Structures, fluids and input

`MapGenBase`, `MapGenStructure`, `StructureStart`, `StructureComponent` and `StructureBoundingBox` are ported, so mods'
dungeons, villages and ruins generate through their own legacy code (structure starts are recomputed from the seed rather
than persisted). Forge fluids (`BlockFluidClassic`/`Finite`, `ItemFluidContainer`) flow by scheduled ticks and are drawn
by a built-in renderer; every legacy `Fluid` also gets a modern fluid, and fluid tanks of legacy tiles and containers
are exposed as NeoForge `FluidHandler` capabilities. `Keyboard`, `Mouse` and `KeyBinding` read the modern window and
register with the controls screen. ForgeMultipart's runtime mixin compiler works (Scala signatures of translated classes are rewritten to the relocated names, and ASM 5's lenient descriptor parsing is kept). Shaders written for the fixed-function pipeline are virtual (they compile "successfully"
and have no effect); VBO and vertex-array calls are emulated on top of immediate mode.

### Entities

Each legacy entity class a mod registers (`EntityRegistry.registerModEntity` / `registerGlobalEntityID`) becomes a
modern entity type. Its instances are run by a host: a vanilla entity (`PathfinderMob` for living ones, an `Enemy`
monster for `IMob`s) that constructs the legacy object and forwards ticking, AI, damage, death and drops, sounds,
interaction, saving and spawn data to it. The legacy base classes (`Entity`, `EntityLivingBase`, `EntityLiving`,
`EntityCreature`, `EntityMob`, `EntityAgeable`, `EntityAnimal`, `EntityTameable`) are real implementations on top of
the host, so `super` calls compose as on 1.7.10: the legacy AI scheduler (`EntityAITasks`) runs the mod's tasks, the
vanilla AI tasks are backed by the equivalent modern goals, navigation and move/look/jump helpers drive the host's
own, `DataWatcher` values are synced to clients, legacy attributes map to modern ones, Forge's
`EntityConstructing` event and `IExtendedEntityProperties` work (also for players), and `addSpawn` entries become
biome modifiers.

On the client, renderers registered with `RenderingRegistry.registerEntityRenderingHandler` draw the entities: the
legacy `Render`/`RenderLiving` code runs inside a render context that maps its OpenGL matrix and state calls onto
the modern pose stack and buffers, legacy models (`ModelBase`, `ModelBiped`, `ModelQuadruped`, ...) emit real
geometry, and `Tessellator`/immediate-mode (`glBegin`) geometry is assembled into quads with the matching modern
render type (textured, translucent or additive). Biped renderers draw worn armour (vanilla layers or a mod's
`getArmorTexture`) as render passes and the held item through the modern item renderer.

### Block and item rendering

- **Textures:** `registerBlockIcons`/`registerIcons` run when the resource pack is generated; icon names become sprite
  ids (vanilla 1.7.10 names map through a rename table to the 1.21 names). `TextureMap`/`TextureAtlasSprite` are real:
  sprites a mod builds itself with a custom loader (Railcraft's unstitched sheets, HBM's recoloured textures) are run
  at pack generation and written out as ordinary textures.
- **Blocks:** the generated block models cover cubes, partial blocks, plants, crops and stairs. Blocks drawn by an
  `ISimpleBlockRenderingHandler` get a model that runs the handler while the chunk is meshed: the Tessellator
  captures its quads (`RenderBlocks` implements the face renderers) and they become the block's baked quads.
  Tile entity special renderers (`TileEntitySpecialRenderer`) run in the render context like entity renderers.
- **Items:** Forge `IItemRenderer`s and 3D `ISimpleBlockRenderingHandler` inventory renders draw through a
  `builtin/entity` model. `AdvancedModelLoader` loads Wavefront OBJ models for renderers.
- **Vanilla behaviour constants** (render types, opacity of the vanilla block classes) come from a table generated
  from the 1.7.10 jar (`tools/extract_block_constants.sh`), since the vanilla classes are shells.
- Not yet: per-vertex colour and ambient occlusion of handler-drawn blocks, `IIcon`s of non-block atlases.

## Compatibility status

| Legacy version | Status |
|---|---|
| 1.7.10 | **In progress.** Offline, 129 real mods from a 140-mod test pack link with 0-34 unresolved references each, and all 1,400 synthesized API classes pass JVM verification. Iron Chest works (blocks, GUIs, upgrades, rendering, recipes); Witchery, Thaumcraft and others now get through most of loading. Mod biomes and world types generate; mod entities spawn, tick, fight and save (520 of 560 entity types in the test packs pass a spawn/save/load self test; 6 of the ~140 test mods still fail to finish loading, mostly API-version mismatches between mods of the test pack (Binnie/Forestry, DragonAPI family) and mods that patch vanilla classes with coremods). Entity rendering is bridged (not yet verified in game for every mod); armour and held items on biped mobs are drawn (not yet verified in game); dimension sky/fog rendering and portal placement are not done. |
| 1.8 - 1.12.2 | Planned next. Version detection and SRG/MCP mappings exist; the shim does not. |
| 1.16.5 | Planned. |
| 1.20.1 (Forge + Fabric via Sinytra Connector) | Planned. |

Not supported (and likely never fully): coremod transformers and mixins that patch legacy Minecraft code itself,
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
tools/dev/run-subset.sh hbm thaumcraft     # server test over a few mods only, with the
                                # block/item/entity self tests; the whole corpus takes minutes
./gradlew runClientTest         # client smoke test (run-clienttest, mods linked from run/mods): reports per mod
                                # which models show the missing texture, then quits
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
