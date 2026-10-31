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

### Legacy world generators (ores and small features)

Every `IWorldGenerator` a mod registers (1.7.10 and 1.12.2 `GameRegistry.registerWorldGenerator`: ores, plants, small
structures) becomes a native placed feature of its own, so data packs and other mods can see, tune or remove it like any
modern feature:

- Id: `<modid>:legacy_worldgen/<generator>`, the namespace of the legacy mod whose jar holds the generator, the name the
  snake case of its class (`thaumcraft:legacy_worldgen/thaumcraft_world_generator`; nested classes `outer_inner`; further
  instances of the same class get `_2`, `_3`, ... in registration order). Ids are stable across launches with the same mods.
  Both a configured feature (type `legacyconnector:legacy_generator`, config `{"generator": "<id>"}`) and a placed feature
  (no placement modifiers: it runs once per chunk) with that id are generated, plus the tag
  `#legacyconnector:legacy_generators` listing all of them.
- One generated biome modifier, `legacyconnector:legacy_worldgen` (`neoforge:add_features`, all biomes, step
  `top_layer_modification`: after vanilla decoration, as FML ran the generators after the chunk provider's populate), lists
  first `legacyconnector:legacy_worldgen` (the catch-all: it posts the populate terrain events, and runs any generator
  registered after the pack was built) and then every generator in legacy order (GameRegistry weight, ties in registration
  order). Features of one step run in the order the biome lists them, so the legacy order holds.
- Each feature runs exactly its generator for the chunk with FML's per-chunk seed (the same formula as
  `GameRegistry.generateWorld`, so generation does not depend on the other generators), the generator's mod lock, and a view
  of the generation region; a failure is logged (throttled) and the chunk goes on. The deep-layer pass of 1.7.10 generators
  (below y=0) is a placed feature of its own, see the next section.
- To remove one generator, use a `neoforge:remove_features` biome modifier with its id (or the tag for all) in step
  `top_layer_modification`; to change it, override its configured or placed feature (e.g. add placement modifiers, or point
  it at `minecraft:no_op`). A generator whose placed feature exists in the world is never run by the catch-all, even when a
  data pack removed it from the biomes.
- Mod dimensions with a legacy chunk provider keep running all generators from their own populate (1.7.10 ran them in every
  dimension; generators check the dimension themselves).

### The deep layer

1.7.10 worlds ended at y=0 (bedrock); modern ones reach down to -64. Each legacy `IWorldGenerator` therefore runs a
second time per chunk with the world shifted so that legacy y=0 is the dimension's bottom: ores, bedrock ores and
underground structures land in the deep layer too (the generator sees deepslate as stone, everything from modern y=0 up as
solid rock, air above legacy y=256; only writes below y=0 are kept). This deep pass is a native placed feature per
generator, `<modid>:legacy_worldgen_deep/<generator class, lowercased>` (feature type `legacyconnector:legacy_worldgen_deep`,
config `{"generator": "<that id>"}`), added to every biome by a biome modifier with the same id (step `underground_ores`).
Its placement is a `minecraft:height_range` whose y is where legacy y=0 lands (default `{"above_bottom": 0}`); the
generator fills the window from there up to modern y=0, and a y at or above 0 (the Nether, the End) runs nothing. A data
pack removes one generator's deep pass by replacing its biome modifier with `{"type": "neoforge:none"}`, or makes the deep
layer thinner by moving the height (e.g. `{"absolute": -32}`: legacy y=0 at -32). `deepLayer.enabled` and
`deepLayer.excludeGenerators` ([Configuration](Configuration.md)) decide which deep features are generated. The random
seed is FML's chunk seed xor a constant, so the deep layer is as reproducible as the normal pass. Generators without a
deep feature in the loaded data (registered after it was built) and mod dimensions (whose chunk generators do not run
biome features) keep running their deep pass from the legacy decorator.

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
