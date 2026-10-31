# World generation, structures, fluids and input

Back to [Home](Home.md); see also [Mod dimensions](Mod-Dimensions.md).

## World generation

Legacy biomes (for example Biomes O' Plenty's) become real 1.21 biomes, generated as data (they also join the vanilla biome tags of the vanilla biome they are modelled on, so vanilla structures, mob spawns and biome modifiers apply to them): climate, colours, and the
ores, caves and small plants of the closest vanilla biome. Trees and grass come from the legacy biome itself: its
decorator runs the legacy passes (the biome's own tree and plant generators) and its extra features, gated by Forge's
terrain-gen events as on 1.7.10. Where legacy biomes appear is decided by the 1.7.10 biome layer system (`GenLayer`,
`WorldType`, Forge's `BiomeManager`), re-implemented in the shim, so a mod's own layer chain runs unmodified.
1.21 terrain, oceans, rivers, beaches, mountains and caves are kept; legacy biomes replace ordinary land biomes where
the legacy layers place them.

A world type a mod registers (BoP's "BIOMESOP") appears on the Create World screen's world type button; on a
dedicated server use `level-type=<modid>:<type>` (e.g. `biomesoplenty:biomesop`). With the default type, mod biomes
that mods added to Forge's `BiomeManager` still generate.

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
  of the generation region; a failure is logged (throttled) and the chunk goes on. 1.7.10 generators also get their deep-layer
  pass (below y=0) right after their normal pass.
- To remove one generator, use a `neoforge:remove_features` biome modifier with its id (or the tag for all) in step
  `top_layer_modification`; to change it, override its configured or placed feature (e.g. add placement modifiers, or point
  it at `minecraft:no_op`). A generator whose placed feature exists in the world is never run by the catch-all, even when a
  data pack removed it from the biomes.
- Mod dimensions with a legacy chunk provider keep running all generators from their own populate (1.7.10 ran them in every
  dimension; generators check the dimension themselves).

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
