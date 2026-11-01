# Performance

Back to [Home](Home.md).

Measured on 2026-10-25 (0.2.7 plus fixes) in a real client: NeoForge 1.21.1, a 640x360 window, render distance 8, vsync off, the
same seed and spot. Each setup ran three times, each with a fresh world:

| | No legacy mods | Thaumcraft 4 (with Baubles, bridged to Curios) |
|---|---|---|
| Heap in use after a full GC | 595, 600, 596 MB | 640, 633, 632 MB (about +38 MB) |
| Process private memory | 2831, 3334, 2844 MB | 2999, 3044, 2865 MB (same range) |
| Frames per second (30 s average) | 1038, 1181, 1494 | 960, 1463, 955 |
| Server tick time | 1.46, 1.68, 1.95 ms | 1.58, 1.82, 1.69 ms |
| Extra startup | none | about 5.5 s to load Thaumcraft (45 blocks, 153 items, 148 recipes) on every launch; translating the jar adds about 0.5 s on the first launch only (the result is cached) |

The only consistent difference is memory: Thaumcraft adds about 38 MB of heap. Frame rate and tick time vary more between
identical runs than between the two setups, so no cost shows at this resolution. The harness that takes these
measurements is part of Legacy Connector, so "no legacy mods" means Legacy Connector loaded with nothing to translate,
not a NeoForge install without it. Translation costs about 0.4 to 0.7 s per large jar (AgriCraft, Applied Energistics 2,
Botania) on first launch.

## World generation writes and per-tick work

- Blocks that a legacy generator writes into a chunk that does not exist yet are kept and placed during that chunk's own
  generation, as plain world-generation writes (no neighbour updates, no block-change hooks). Only writes into chunks that are
  already loaded go through `Level.setBlock` (in ATM10 Sable's per-block hook made that path about 3% of the server tick).
- The legacy `previousEquipment` copy that follows every `LivingUpdateEvent` wraps only equipment slots whose modern stack
  changed.

## World load

Measured from the log of an All the Mods 10 world creation (about 450 mods, 7 legacy 1.7.10 mods: Thaumcraft, Witchery and the Reika
mods). Creating the world took about 75 s from the data pack load to the spawn area: 33 s of data pack reload (141895 recipes, KubeJS),
4 s of server start, 33 s creating the 42 levels and 0.8 s for the spawn area. Legacy Connector's share and what was done about it:

- **Feature order cycle (fixed, not a switch).** ChromatiCraft registers one world generator twice. FML keeps generators in a set, so a
  second registration only changes the weight; the layer listed it twice, which put its placed feature twice into every biome. That is a
  feature order cycle in every dimension: vanilla's feature indexer fails and Biolith falls back to its resilient indexer, which dropped
  858 cycle edges caused by it (of 1412; the rest came from a modern mod) while the levels were created (18 s of indexing in total). The
  generator also ran twice per chunk. Now a generator is registered once and the biome modifier lists every generator once.
- `perf.lazyPackMetadata` (default on): the game opens every pack once just to read its metadata when it scans the pack repository, and
  the generated packs built all their files each time (the legacy data pack was built three times per world creation, 0.2 to 0.7 s each).
  The metadata-only open builds nothing now.
- `perf.biomeJarCache` (default on): every build of the legacy data pack read the vanilla biome templates and resolved about 150 vanilla
  and NeoForge biome tags from the jars again (about 0.4 s of the first build in ATM10: "Generated 22 legacy biome(s)", "Legacy biomes
  joined 7 biome tag(s)"). They are read once per session now.

The spawn area size is vanilla's game rule `spawnChunkRadius` (default 2; `/gamerule spawnChunkRadius 0` keeps no chunks loaded around
the spawn and makes "Preparing spawn area" almost instant); Legacy Connector does not change it. Chunk generation in parallel and faster
world generation are left to C2ME and friends.

## Legacy rendering on the client

Measured in ATM10 with the Reika mods (spark, 60 s of the render thread, before 0.3.11): Legacy Connector took 8.3% of the render thread, the pack
allocated about 1.3 GB/s and the render thread parked in `RenderContext.beginGui` (HUD overlay events) behind chunk-meshing threads that held the
one lock of the legacy render emulation. Since then:

- The emulated GL state is per thread (see [Rendering](Rendering.md)); the render thread takes no lock, chunk threads only one among themselves.
- No garbage per vertex or GL call: Tessellator and glBegin vertex arrays are reused between draws, quads go to the buffer without a corner array, a
  draw looks its render buffer up once, the emulated GL calls build their display-list action only while a list is compiling (they built a lambda
  per call), `glRotate`/`glTranslate` use no temporary matrices, captured block quads are stored in flat arrays, and particles draw without a copy
  of their list per frame.
- Display lists stay compiled: found by id without boxing, their draws keep the assembled quads and normals.
- Wrappers are reused: `ItemStack.mcc$wrap` hands back the view it made for the same modern stack (a per-thread table of 64), and the legacy
  item of a vanilla or modern-mod item comes from a lock-free cache instead of a synchronized lookup.
- Event bridges build nothing nobody listens to: the legacy event bus answers "anyone listening?" and finds its listeners without allocating
  (it built a capturing lambda per post), HUD overlay Pre, Post and Text events are each built only when a listener receives that kind,
  `RenderLivingEvent` work is skipped unless listened to, and DragonAPI's reflective events no longer copy their constructor's parameter types per post.
- Tile entity renderers are culled by distance and bounding box as in 1.7.10.
