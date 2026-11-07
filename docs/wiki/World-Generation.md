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
- **TerraBlender is the only placement path.** The legacy biomes form a TerraBlender overworld region
  (`compat/terrablender/LegacyRegion`). TerraBlender (LGPL-3.0, see `THIRD_PARTY_NOTICES.md`) is a required dependency, embedded as
  jar-in-jar with an open-ended version range, so nothing extra needs installing and a separately installed (newer) TerraBlender
  wins. The region holds the vanilla layout with each analog's points shared among the legacy biomes modelled on it. Its weight is
  the total legacy weight / 3, clamped to 1-12 (TerraBlender's vanilla region has weight 10), and TerraBlender mixes it with the
  vanilla region and other mods' regions (Terralith, Tectonic, Biomes O' Plenty...). Inside the region the analog keeps weight 20 of each point and the legacy biomes alternate with it along the humidity axis (analog, legacy, analog, legacy...); there is no rule keeping legacy biomes apart or on different analogs, they sit where their climate puts them. `legacyBiomeFrequency` (config, default 1.0) scales both the region weight and the shares. Biomes in no `BiomeManager` list (Thaumcraft's Eerie and Eldritch) never generated in the 1.7.10 overworld and are not placed. There is no fallback that edits the
  `minecraft:overworld` parameter list: worldgen mods such as Tectonic and Terralith reshape that climate space, and points added
  there landed in thin river-shaped bands (issue #175).

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
solid rock, air above legacy y=256; only writes below y=0 are kept). Legacy ores stay exactly as common as in the original mod: the deep pass keeps only ore writes (natural overworld stone
replaced by another block), and an ore that lands in legacy y 0-63, the band both passes cover, is kept by exactly one of the two
passes, chosen per position. A chunk gets the 1.7.10 number of ores, the bottom 64 blocks of them spread over -64..63; underground
structures are built once, by the normal pass. This deep pass is a native placed feature per
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

Outside world generation, code that hard-codes the old range is rewritten by the loader to ask the world for its real one:
`y < 0` / `y >= 256` checks on the y parameter of a method taking `(World, x, y, z)` (`BuildHeightRewriter`, RotaryCraft's
placement bounds), and a tile entity's clamp of a height it picked around itself to the bedrock floor, `if (y < K) y = K`
with K 0..8 on a local computed from `yCoord` (`WorldFloorClampRewriter`, Thaumcraft's Arcane Lamp), which then counts K from
the world's bottom. Generation views and 0..256 dimensions answer the 1.7.10 range, so nothing changes there.

## Structures, fluids and input

### Legacy structures as modern structures

A 1.7.10 `MapGenStructure` that a mod drives from its own world generator (an `IWorldGenerator` or a `PopulateChunkEvent`
handler calling `generate` and `generateStructuresInChunk`, like Anti-Gravity's stars) is a real modern structure
(`core/worldgen/LegacyStructure*`, 1.7.10 side `v1_7_10/bridge/MapGenStructures1710`). The generated data pack holds, per
generator, under the legacy mod's namespace and stable across sessions (`<mod>:legacy/<structure name>`):

- `data/<mod>/worldgen/structure/legacy/<name>.json`: type `legacyconnector:legacy_structure`, step `top_layer_modification` (FML ran mod
  generators after decoration), no terrain adaptation. Each legacy `StructureComponent` is one piece
  (`legacyconnector:legacy_piece`) with the component's box; placing it runs `addComponentParts` for the chunk. A start class that
  overrides `generateStructure` is one piece covering the start. Pieces save the component's legacy NBT when the mod registered it
  with `MapGenStructureIO`, otherwise the start is laid out again from the seed.
- `.../worldgen/structure_set/legacy/<name>.json`: `random_spread` with spacing 1, separation 0: every chunk is a candidate and the legacy
  `canSpawnStructureAtCoords` decides, with the 1.7.10 seeding (`MapGenStructure.mcc$startAt`), so starts are exactly as frequent
  and where they were in 1.7.10 for the same seed. No exclusion zone by default (it would also drop starts near village chunks
  without a village); `-Dlegacyconnector.structureVillageDistance=N` adds one around `minecraft:villages`.
- `.../tags/worldgen/biome/has_structure/legacy/<name>.json`: `#minecraft:is_overworld` plus the legacy spawn biome list found in a
  field of the generator, mapped to modern biomes. The legacy check stays authoritative; a pack can narrow the tag.

The pieces are placed in a chunk only when the mod's own call asks for that generator's structures there
(`generateStructuresInChunk` no longer places anything itself, `generate` no longer lays out starts), so the mod's dimension checks
and config switches still decide. The game and the mod's call meet in the same chunk's decoration, in either order (a piece handed
over first waits for the call). `/locate structure <mod>:legacy/<name>` works natively: the start check (`canSpawnStructureAtCoords`
with the 1.7.10 seeding, `MapGenStructure.mcc$startAt`) stores nothing and normally reads only the seed and biomes, so it answers
quickly for chunks that never generated; it can name a start
the mod's own call then does not place (a dimension or config check of the mod's world generator). What stays on the 1.7.10 path: generators
constructed after loading (mod dimensions' chunk providers), generators that override `generate`/`generateStructuresInChunk`
(CustomOreGen, HBM), the layer's vanilla stand-ins, mod dimensions, the deep pass (skipped for modern ones) and levels without the
set (superflat). `-Dlegacyconnector.modernStructures=false` turns it off. Not ported: Forge `InitMapGenEvent` replacements of
vanilla's village/temple generators (the modern overworld has its own), `hasStructureAt` for modern ones (always false).

### Legacy generators that build in place

Most legacy structures (ChromatiCraft's shrines, Thaumcraft's obelisks, Witchery's huts) are written block by block by an
`IWorldGenerator`; nothing says which generator is a structure generator. `InPlaceStructureGuard` keeps them out of modern
structures without that knowledge, and without changing their chance per chunk:

- Only in a chunk where a modern structure piece (not a legacy one) reaches into the decorated 3x3 area does it do anything.
- There, each legacy generator call (and the populate events) is journaled: every write keeps the block it replaced. A write into
  a modern piece's box is refused, unless it only turns natural ground into another non-air block (ores under a house).
- When the call built something (24 or more "build" writes: a block into air, water or a plant, a block removed, a block entity)
  and at least 4 of them were refused, it was a structure crossing a modern one: the whole call is undone (blocks, block
  entities, entities it added, writes it deferred to neighbouring chunks), as if the generator had skipped this chunk. A call
  with fewer refused writes stays minus those blocks (a flower or some leaves inside a house's box).
- Pieces of legacy structures made modern go through the same check against the other structures (undone per chunk).

Limits: a mixed generator (ores, trees and structures in one call) loses its ores in an undone chunk too; a structure spanning
several chunks is undone only in the chunks where it hits the modern structure; the mod's own records of what it built
(saved data, registries) are not undone. Thresholds: `-Dlegacyconnector.inPlaceGuard.buildMin`, `...refusedMin`; off:
`-Dlegacyconnector.inPlaceGuard=false`. 1.12.2 generators are not journaled yet.

### Locating what legacy generators build

What an `IWorldGenerator` builds in place is found by `/locate structure` (`core/worldgen/LegacyGeneratorStructures`). Generation
does not change: the same generator builds at the same places in the same decoration step; the build is only recorded.

- Every 1.7.10 generator with a placed feature of its own gets a modern structure `<mod>:legacy/<name>` (type
  `legacyconnector:legacy_generator_structure`), its structure set (`random_spread`, spacing 1, `locate_offset` 8, 0, 8) and the biome tag
  `<mod>:has_structure/legacy/<name>` (`#minecraft:is_overworld`, `#minecraft:is_nether`, `#minecraft:is_end`). The name is the
  generator's class in snake case without `_generator`/`_world_generator` and without the mod's name; a mod's one world generator is
  `structures`. In ATM10 for example: `chromaticraft:legacy/pylon`, `chromaticraft:legacy/dungeon` (all of ChromatiCraft's
  structures from its dungeon generator), `chromaticraft:legacy/data_tower`, `thaumcraft:legacy/structures`, `witchery:legacy/structures`.
  The list is logged at startup ("N legacy world generator(s) are locatable structures ...").
- The game never starts one by itself. Each run of a generator in a chunk is observed (the writes the legacy world makes, the same
  hook `InPlaceStructureGuard` uses, and writes deferred past the region). A run that built a structure, 24 or more blocks put into
  air, water or plants (or removed), of which 8 or more are not natural, gets a start: one `legacyconnector:legacy_marker` piece
  whose box is the built area, in the chunk at the middle of the build. Natural blocks are ground, stone, sand, ores, logs, leaves,
  plants, vines, snow, ice and fluids (legacy blocks by their class and material), so trees, flowers, lakes and ores are not
  structures. The piece places nothing. The start is saved with the chunk and the structure check is refreshed, so `/locate`, maps
  and structure-aware mods see it like any modern structure.
- A generator whose runs "build" in more than a quarter of 256 or more chunks is no structure generator; it is no longer recorded
  (logged once). The first recorded starts are logged ("Legacy structure ... recorded at ...").
- Starts are not predicted. `/locate` finds the structures in chunks that have generated, not in new terrain. Predicting would mean
  running the legacy generator for chunks that do not exist yet against a side-effect-free copy of the terrain, and the generators
  that build the structures keep state of their own that such a dry run changes: ChromatiCraft's dungeon generator records each
  chunk's outcome and skips the chunk afterwards, its pylon generator records every pylon (the pylon finder points at them), Witchery
  keeps a list of its structures and builds none near one, Thaumcraft keeps a map of what it placed. A dry run would make them skip
  or misplace the real structure.
- Recorded builds are also kept per dimension in saved data (`LegacyStructureIndex`, `legacyconnector_structures`), and `/locate structure
  <mod>:legacy/<name>` answers from it at any distance without loading chunks (the handler is merged into the game's own `locate structure
  <structure>` node; a structure with nothing recorded, vanilla structures and tags go to the game's command). Worlds generated before the index
  existed fall back to the chunk walk. Tab completion lists the structures (they are in the structure registry).
- Cost: one block read per legacy write while a generator runs (the replaced block), and per chunk one empty structure check per
  generator. `/locate` with spacing 1 reads every chunk's saved structure data out to the nearest find (vanilla's own path; slow only
  when nothing is found within the radius).
- Limits: one id per generator (a generator that builds several kinds of structure, like ChromatiCraft's dungeon generator, finds the
  nearest of any kind); runs undone by `InPlaceStructureGuard` are not recorded; generators without a placed feature (registered after
  the data was built) and mod dimensions are not observed; 1.12.2 generators are not observed yet (their world does not report writes).
  Thresholds: `-Dlegacyconnector.locatable.buildMin`, `...madeMin`. Off: `structures.locatable=false` ([Configuration](Configuration.md)),
  which keeps the structures defined without biomes (so recorded starts still load) and records nothing.

### Legacy structure code

`MapGenBase`, `MapGenStructure`, `StructureStart`, `StructureComponent` and `StructureBoundingBox` are ported, so mods'
dungeons, villages and ruins generate through their own legacy code (structure starts are recomputed from the seed rather
than persisted). Buildings that mods add to vanilla villages (Forge `IVillageCreationHandler`: Witchery's shops and walls,
Thaumcraft's wizard tower, AgriCraft's greenhouse) are added to a village after vanilla assembled it, which stays exactly as
vanilla makes it (its pools are not changed): the buildings become extra pieces of the same structure
(`legacyconnector:legacy_village_building`), beside its streets with the front to the road, on free and dry ground only (a plot
touching any other piece is skipped). How many: the share mod buildings had in a 1.7.10 village (their weights against vanilla's 91),
counted against the village's own houses, and each building at most as often per village as 1.7.10 allowed (`villagePiecesLimit`).
Each added piece also carries the village's streets and other pieces, which its legacy component sees in 1.7.10's component
list: streets as `Path`s, buildings as one-block `Path` strips along their street (code that takes the village's extent from its
paths then covers the modern buildings, which reach deeper than 1.7.10's). This is how Witchery's town walls come back: its
district handler's `Wall` is a 2x2 post whose block entity, about two seconds after the chunk is loaded, builds the wall around
the paths of the list it was given (each path widened by 20 blocks along it and 7 across, gates where streets end), so the
wall stands outside the village with gaps at its streets. Handlers are told apart by handler class and component class (Witchery's
district registers one handler class for its wall, keep, watchtowers and town clusters), small components are added too (they take
no house's place), and handlers that only add vanilla's own 1.7.10 houses (Witchery's town clusters) are left out, as the modern
village has its own. Witchery's `TownWallMode`/`TownKeepMode` and cluster settings apply as in 1.7.10. Limits: the wall's block
entity keeps the paths in memory only (as in 1.7.10), so a chunk saved before it ticked gets no wall, and the wall reads chunks
that are not loaded as air.
`villages.legacyBuildings=false` leaves them out. Doors of legacy structures: 1.7.10 kept a door's facing and open state in the lower half's metadata and the hinge in the upper's,
so a half written on its own would keep the defaults of the rest (an upper half facing east); whenever legacy code writes a door half
and the other half is there, the world puts both modern halves together (facing and open from the lower, hinge and powered from the
upper), and reading a door back gives the nearest 1.7.10 metadata. `placeDoorAtCurrentPosition` passes the already rotated facing on
unchanged and `ItemDoor.placeDoorBlock` picks the hinge from the neighbours, both as in 1.7.10; `getMetadataWithOffset` follows
1.7.10's table exactly (its modes 1 and 3 mirror the piece, so directional blocks, ladders and pistons there are no plain rotation).
Chests that legacy structures fill from a Forge `ChestGenHooks` category get the matching modern
loot table, so the pack's loot applies. `ChestGenHooks.addItem` additions become an extra pool on that table
(`ChestLootBridge1710`), and so do entries a mod puts straight into a piece class's chest array (`ChestCorridor.strongholdChestContents`,
`Library.strongholdLibraryChestContents`, `RoomCrossing.strongholdRoomCrossingChestContents`, `StructureMineshaftPieces.Corridor.mineshaftChestContents`,
`House2.villageBlacksmithChestContents`, `DesertPyramid.itemsToGenerateInTemple`, `JunglePyramid.junglePyramidsChestContents`,
`StructureNetherBridgePieces.Piece.field_111019_a` -> `chests/nether_bridge`): the arrays start empty and the live field is read when the table loads
(`StructureChestFields`). Forge fluids (`BlockFluidClassic`/`Finite`, `ItemFluidContainer`) flow by scheduled ticks and are drawn
by a built-in renderer; every legacy `Fluid` also gets a modern fluid, and fluid tanks of legacy tiles and containers
are exposed as NeoForge `FluidHandler` capabilities. `Keyboard`, `Mouse` and `KeyBinding` read the modern window and
register with the controls screen. ForgeMultipart's runtime mixin compiler works (Scala signatures of translated classes are rewritten to the relocated names, and ASM 5's lenient descriptor parsing is kept). Shaders written for the fixed-function pipeline are virtual (they compile "successfully"
and have no effect); VBO and vertex-array calls are emulated on top of immediate mode.
