# World generation, structures, fluids and input

Back to [Home](Home.md); see also [Mod dimensions](Mod-Dimensions.md).

## World generation

Legacy biomes (for example Biomes O' Plenty's) become real 1.21 biomes, generated as data (they also join the vanilla biome tags of the vanilla biome they are modelled on, so vanilla structures, mob spawns and biome modifiers apply to them): climate, colours, and the
ores, caves and small plants of the closest vanilla biome. Trees and grass come from the legacy biome itself: its
decorator runs the legacy passes (the biome's own tree and plant generators) and its extra features, gated by Forge's
terrain-gen events as on 1.7.10. Where legacy biomes appear is decided by the 1.7.10 biome layer system (`GenLayer`,
`WorldType`, Forge's `BiomeManager`), re-implemented in the shim, so a mod's own layer chain runs unmodified.
1.21 terrain, oceans, rivers, beaches, mountains and caves are kept; legacy biomes replace ordinary land biomes where
the legacy layers place them. Mods' `IWorldGenerator`s (ores, structures) run for every chunk.

A world type a mod registers (BoP's "BIOMESOP") appears on the Create World screen's world type button; on a
dedicated server use `level-type=<modid>:<type>` (e.g. `biomesoplenty:biomesop`). With the default type, mod biomes
that mods added to Forge's `BiomeManager` still generate.

## Structures, fluids and input

`MapGenBase`, `MapGenStructure`, `StructureStart`, `StructureComponent` and `StructureBoundingBox` are ported, so mods'
dungeons, villages and ruins generate through their own legacy code (structure starts are recomputed from the seed rather
than persisted). Forge fluids (`BlockFluidClassic`/`Finite`, `ItemFluidContainer`) flow by scheduled ticks and are drawn
by a built-in renderer; every legacy `Fluid` also gets a modern fluid, and fluid tanks of legacy tiles and containers
are exposed as NeoForge `FluidHandler` capabilities. `Keyboard`, `Mouse` and `KeyBinding` read the modern window and
register with the controls screen. ForgeMultipart's runtime mixin compiler works (Scala signatures of translated classes are rewritten to the relocated names, and ASM 5's lenient descriptor parsing is kept). Shaders written for the fixed-function pipeline are virtual (they compile "successfully"
and have no effect); VBO and vertex-array calls are emulated on top of immediate mode.
