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
- 1.12.2: `v1_12_2/bridge/LegacyWorldPresets1122` (listing; `World.getWorldType()` returns the mod type from the preset id).
