# World generation, structures, fluids and input

Back to [Home](Home.md); see also [Mod dimensions](Mod-Dimensions.md).

## World generation

Legacy biomes (for example Biomes O' Plenty's) become real 1.21 biomes, generated as data (they also join the vanilla biome tags of the vanilla biome they are modelled on, so vanilla structures, mob spawns and biome modifiers apply to them): climate, colours, and the
ores, caves and small plants of the closest vanilla biome. Trees and grass come from the legacy biome itself: its
decorator runs the legacy passes (the biome's own tree and plant generators) and its extra features, gated by Forge's
terrain-gen events as on 1.7.10. Mods' `IWorldGenerator`s (ores, structures) run for every chunk.

### Where legacy biomes generate

In a default world, legacy biomes enter the overworld the way a modern biome mod adds its biomes: as climate parameter points
of the multi-noise biome source (`core/worldgen/BiomePlacement`, data from `v1_7_10/bridge/LegacyBiomeClimate`).

- **Climate points.** Each legacy biome is modelled on one vanilla land biome, its *analog*. The analog is picked from the
  biome's Forge `BiomeDictionary` tags first (MESA: badlands, SNOWY/FROZEN: snowy taiga or snowy plains, JUNGLE, SWAMP,
  SAVANNA, SANDY with HOT or DRY: desert, MOUNTAIN: windswept hills or forest, CONIFEROUS: taiga, SPOOKY or DENSE forest:
  dark forest, FOREST, PLAINS). If no tag decides it, its temperature, rainfall, height (`rootHeight`), tree count and the
  `BiomeManager` climate lists it is in (DESERT, WARM, COOL, ICY) decide. The legacy biome takes a share of every vanilla
  parameter point of its analog: the point's humidity interval is cut into consecutive pieces, one per biome, sized by weight.
  Ocean, river, beach, Nether and End biomes are not placed, because the legacy layers also left water and shores to the terrain.
- **Weights.** A biome's weight is the sum of its entry weights in Forge's `BiomeManager` lists, which is what
  `GenLayerBiome` drew from on 1.7.10. Mod biomes in no list never generated in a 1.7.10 default overworld (they belong to
  the mod's own dimension or world type), so they are not placed here. The log lists both groups once
  ("Legacy biome climate placement ...").
- **With TerraBlender** (for example in ATM10), the legacy biomes form a TerraBlender overworld region
  (`compat/terrablender/LegacyRegion`, the only class that touches TerraBlender, loaded only when it is installed). The region
  holds the vanilla layout with each analog's points shared among the legacy biomes modelled on it. Its weight is the total
  legacy weight / 10, clamped to 1-15 (TerraBlender's vanilla region has weight 10), and TerraBlender mixes it with the
  vanilla region and other mods' regions.
- **Without TerraBlender**, the legacy biomes are added at server start to the server's `minecraft:overworld` multi-noise
  parameter list (the preset the overworld biome source refers to), and the vanilla biomes are kept. The analog keeps weight
  20, so a legacy biome of weight 10 takes a third of its analog's points. The overworld is still saved as
  `preset: minecraft:overworld`. If a data pack gives the overworld its own inline biome list, legacy biomes do not appear there
  (logged).

Biome registry ids are unchanged (`thaumcraft:magical_forest`, ...), so existing worlds keep their already generated biomes.
New chunks of worlds made with older versions use the new placement, so biome borders can show where old and new chunks meet.
"Biome sample around spawn (mode): N of M points are legacy biomes {...}" in the server log samples the overworld source.

### Legacy world types

A world type a mod registers (BoP's "BIOMESOP") appears on the Create World screen's world type button; on a
dedicated server use `level-type=<modid>:<type>` (e.g. `biomesoplenty:biomesop`). Its world preset brings its own biome
source, `legacyconnector:legacy_layers` (`LegacyLayeredBiomeSource`, a multi-noise source of the vanilla parameter list, so
TerraBlender regions and terrain-tied biomes still apply). On top of that source, the world type's own 1.7.10 biome layer chain
(`GenLayer`, `WorldType.getChunkManager`, re-implemented in the shim and run unmodified) chooses the land biomes. Modern
terrain, oceans, rivers, beaches, peaks and caves are kept, and where the legacy layout has a vanilla biome the multi-noise
biome stays. Worlds created with a legacy world type before this source existed load as plain multi-noise worlds and get the
default placement above.

## Structures, fluids and input

`MapGenBase`, `MapGenStructure`, `StructureStart`, `StructureComponent` and `StructureBoundingBox` are ported, so mods'
dungeons, villages and ruins generate through their own legacy code (structure starts are recomputed from the seed rather
than persisted). Buildings that mods add to vanilla villages (Forge `IVillageCreationHandler`: Witchery's shops and walls,
AgriCraft's greenhouse) become elements of the vanilla village house pools (`legacyconnector:legacy_village_building`), so the
jigsaw generator places them at a street and clear of the other buildings like a modern mod's houses; their 1.7.10 weight is
scaled to the pools' weights. Chests that legacy structures fill from a Forge `ChestGenHooks` category get the matching modern
loot table, so the pack's loot applies. Forge fluids (`BlockFluidClassic`/`Finite`, `ItemFluidContainer`) flow by scheduled ticks and are drawn
by a built-in renderer; every legacy `Fluid` also gets a modern fluid, and fluid tanks of legacy tiles and containers
are exposed as NeoForge `FluidHandler` capabilities. `Keyboard`, `Mouse` and `KeyBinding` read the modern window and
register with the controls screen. ForgeMultipart's runtime mixin compiler works (Scala signatures of translated classes are rewritten to the relocated names, and ASM 5's lenient descriptor parsing is kept). Shaders written for the fixed-function pipeline are virtual (they compile "successfully"
and have no effect); VBO and vertex-array calls are emulated on top of immediate mode.
