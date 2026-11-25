# Legacy mod world types found in the local jar sets (2026-11-24)

Scan: classes whose superclass chain reaches `WorldType` (class names for 1.7.10/1.12.2; for 1.4.7-1.6.4 the obfuscated `yn`/`acg`),
over `test-mods*`, `test-mods-probe/*-modrinth-set`, `1.12.2 test mods`, `test-mods-mcarchive`. 1.5.2 and 1.2.5 sets contain no world type.
"Own gen" = overrides `getChunkGenerator` (own terrain), "Own biomes" = `getChunkManager` / `getBiomeProvider` / `getBiomeLayer` /
`getBiomesForWorldType`, "Misc" = cloud height, horizon, spawn fuzz, void fade, customize GUI.

## 1.7.10
| Mod | Type class (name) | Overrides | Listed before / after |
|---|---|---|---|
| Biomes O' Plenty | WorldTypeBOP (BIOMESOP) | own biomes (layers + chunk manager) | yes / yes (layers preset) |
| zombieplague2 | WorldTypeZp, CrazyZp, HardcoreZp (world_zp, world_crazy_zp, world_hard_zp) | own gen + biome layer | listed / own generator preset |
| Planetoid | PlanetoidWorld (planetoid) | own gen + chunk manager, spawn fuzz, customize | listed / own generator preset |
| FarLands | FarWorldType (FarLands) | own gen | listed / own generator preset |
| HFR (Earth map) | XFEarthWorldType (earthmap) | own gen + chunk manager, spawn fuzz, customize | listed / own generator preset |
| TerraFirmaCraft (+TFRU) | TFCWorldType | own gen + biomes list + chunk manager, horizon, min spawn height | NOT created in the test run (TFRU needs yamcore coremod); not verified |
| Botania | WorldTypeSkyblock | own gen, cloud, spawn, void | only created when Garden of Glass is loaded |
| ForgeEssentials | WorldTypeMultiworld | own gen, chunk manager, layer | not created in the test run |
| Game of Thrones, LOTR | GOTWorldType, LOTRWorldTypeMiddleEarth | `func_151357_h` only; own provider class | custom dimension mods, not listed |
| HBM NTM / RNTM | WorldTypeTeleport | none (a teleporter, not a WorldType user) | n/a |

## 1.12.2
| Mod | Type class | Overrides |
|---|---|---|
| Biomes O' Plenty | WorldTypeBOP | own biome provider, own gen, customize GUI |
| Botania | WorldTypeSkyblock | own gen, cloud height, horizon, spawn fuzz, void fade |
| Quark | WorldTypeRealistic | own gen, cloud height |
| Compact Machines | SkyWorldType | own gen, spawn fuzz, customize |
| Extra Utils 2 | WorldWall | own gen, spawn fuzz (special dimension helper) |
| Advanced Rocketry | WorldTypePlanetGen, WorldTypeSpace | own gen + biome provider / layer |
| TerraFirmaCraft | WorldTypeTFC | own gen + biome provider, cloud, horizon, min spawn height, customize |
| Lost Cities | LostWorldType, LostWorldTypeBOP, LostWorldTypeAdapter | own gen + biome provider, horizon, customize |
| Conquest Reforged | ModWorldType | own gen + biome provider + layer |
| Cubic Chunks | VanillaCubicWorldType | cubic generator (needs the cubic world format; not hostable) |
| Pixelmon | UltraSpaceWorldType, DrownedWorldWorldType, DebugWorldType | own gen + biome provider, many misc |
| Aether of the Ages 3 (AoA3) | 23 `WorldType<dim>` classes | own gen + biome provider (dimension helpers) |

## 1.4.7 - 1.6.4 (obfuscated)
| Mod | Version | Type | Overrides |
|---|---|---|---|
| Biomes O' Plenty | 1.4.7 | WorldTypeBase | own biomes (chunk manager) |
| Crafting Dead | 1.6.4 | WorldTypeCD | own gen + chunk manager, spawn fuzz |
| Solar Apocalypse | 1.6.4 | WorldTypeSolarApoc, WorldTypeSADefault | own gen + chunk manager, spawn fuzz |
| Michael's World Mod | 1.6.4 | ModWorldType | id helper only |

## Mechanism
- Version-neutral: `core/worldgen/LegacyWorldPresetPack` (preset + noise settings copy per type, one `minecraft:normal` tag, `generator.<mod>.<name>` names).
- 1.7.10 (also 1.4.7-1.6.4): `v1_7_10/bridge/LegacyWorldPresets`; biome-only types use `legacyconnector:legacy_layers`, types overriding
  `getChunkGenerator` use `legacyconnector:legacy` with `dimension: 0` (the mod-dimension generator hosting the overworld provider's legacy generator).
- 1.12.2: `v1_12_2/bridge/LegacyWorldPresets1122`. A type overriding `getChunkGenerator` (Quark, RTG, BoP, Lost Cities, ...) is hosted: the preset's generator is
  `legacyconnector:legacy_1122` with `dimension: 0` and `world_type`, its biome source `legacy_1122` too (the type's `getBiomeProvider`, else 1.12's vanilla layer
  `BiomeProvider(WorldInfo)`); `LegacyChunkGenerator1122.attach` builds the mod's generator before `WorldEvent.Load` (RTG reads `world.getChunkProvider().chunkGenerator`
  there), `World.getWorldType()` comes from the generator's `world_type`. Other types are listing only.

## Server test results (2026-11-25, tools/dev/worldtype-test.sh, seed 1710, vanilla baseline = same seed)
Columns: listed (preset in `world_preset` registry and the `minecraft:normal` tag), generates (own terrain differs from vanilla, no exception), spawn ok (solid ground, 3 blocks of the
surface), reload ok (restart on the same world: same preset and generator, the area sampled before has the same surface; chunks never visited generate again).
Jars: gitignored `test-mods-issues/344/<mc>/`; suites `tools/dev/verify/suites/worldtypes1710-*.txt`, `worldtypes1122-*.txt` (`# level-type:` line, see verify.sh).

| Mc | Mod / type (preset id) | listed | generates | spawn ok | reload ok | notes |
|---|---|---|---|---|---|---|
| 1.7.10 | ATG (`atg:atg`) | yes | yes | yes | yes | was void before this run: the provider had no `worldChunkMgr` and ATG's generator casts it (fixed). Heights 55-92, ocean/forest/swamp |
| 1.7.10 | Biomes O' Plenty (`biomesoplenty:biomesop`) | yes | yes (biome layers; vanilla terrain, BoP biomes) | yes | yes | |
| 1.7.10 | RWG alpha (`legacyconnector:rwg`; the mod's owner resolves to legacyconnector) | yes | partly | yes | yes | own terrain and biomes, but 60% of the columns (ocean_oasis, wet_ocean) come back with no blocks at all: cause not found (no exception) |
| 1.7.10 | Botania + Garden of Glass (`botania:botania-skyblock`) | yes | yes (void) | no | yes | void world as intended; the start island is placed by the mod when a player joins: needs a client check |
| 1.7.10 | Highlands 2.2.3 | n/a | n/a | n/a | n/a | no WorldType; the mod fails at init (`AdvancedBiomeEntry` of a null biome in `MultiWorldSupport`) |
| 1.7.10 | ExtrabiomesXL 3.16.4 | n/a | biomes join the default world | yes | n/a | no WorldType |
| 1.12.2 | Quark realistic (`quark:quark_realistic`) | yes | yes (hosted, was vanilla) | yes | yes | tall custom-scale terrain from the shim's ChunkGeneratorOverworld, vanilla layer biomes |
| 1.12.2 | RTG (`rtg:rtg`) | yes | yes (hosted, was vanilla) | yes | yes | needed the SidedProxy nested-class fix, the in-construction provider, and no neighbour updates inside `ChunkEvent.Load` (hung the server thread) |
| 1.12.2 | Biomes O' Plenty (`biomesoplenty:biomesop`) | yes | yes (hosted: `ChunkGeneratorOverworldBOP`) | yes | yes | |
| 1.12.2 | Open Terrain Generator 9.9 (`openterraingenerator:otg`) | yes | no | no | n/a | the mod fails at init (NPE in `WorldLoader.<init>`, the underlying exception is hidden by its own logger being null) |
| 1.12.2 | Lost Cities, TFC, Advanced Rocketry, Conquest Reforged, Botania skyblock | not tested | | | | no jars on this machine; the hosting path is the same as RTG's |
| 1.7.10 / 1.4.7-1.6.4 | zombieplague2, Planetoid, FarLands, HFR, TFC, Crafting Dead, Solar Apocalypse | not tested | | | | no jars on this machine (the own-generator path is the ATG one) |

## Known limits of the hosted overworld
- Blocks exist only from y 0 to 255 (the legacy world); y -64..-1 is void under the bedrock floor of the mod's terrain. Players cannot dig out of it in survival; creative flight below y 0 shows void.
- Hosted generators replace carvers, surface rules and decoration entirely (the legacy `populate` and the mods' `IWorldGenerator`s run): modern placed features and biome modifiers of other mods do not run in these worlds. `getBaseHeight` is a constant 64, so modern structures (villages...) placed through the vanilla structure sets would use that height.
- 1.12.2 hosting is also applied to Biomes O' Plenty 1.12.2 (before: vanilla terrain with the BoP preset id); revert by dropping `hasOwnGenerator` from the preset if a client check dislikes it.
