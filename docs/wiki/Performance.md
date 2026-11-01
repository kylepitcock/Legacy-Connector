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

## Server tick switches

Profile data from ATM10 put the legacy layer at 8.6% of the server thread (event bridges, `LivingUpdateEvent` for every living
entity, tile entity tickers, view and block lookups). Each optimization below has its own `perf.<name>` key in
`config/legacyconnector.properties` (read once at startup, see [Configuration](Configuration.md)); all default to `true` because they
keep the legacy behaviour. They touch only Legacy Connector's own objects, so they do not overlap with Lithium/Canary/Radium,
ModernFix, C2ME, ServerCore, AI-Improvements or Clumps (those optimize vanilla code paths; none of them knows legacy hosts).

| Key | What it does | Why it is safe |
| --- | --- | --- |
| `perf.staticTileEntities` | A legacy tile entity host learns on its first tick whether its tile entity ticks and, if not, asks its chunk to drop its ticker (`StaticTickers`, `LevelChunkStaticTickerMixin` wraps `BlockState.getTicker` in `LevelChunk.updateBlockEntityTicker` for hosts only). Static legacy machines, cables, storage blocks and decorations then cost nothing per tick; vanilla's ticker wrapper did a ticking-range lookup, a profiler push and a block state check for each of them every tick. A tile entity whose class only has the shim's empty `updateEntity()` is never ticked either | 1.7.10 Forge added a tile entity to the ticking list only if `canUpdate()` returned true when it was added (decided once, as here); IndustrialCraft 2's rule (a tile that overrides `updateEntity()` ticks whatever `canUpdate()` says) is kept. 1.12.2 ticked only `ITickable` tile entities. A host whose tile entity changes (`setTileEntity`, a metadata change that refreshes it) gets its ticker back. A tile that crashed while ticking already stopped ticking; now its ticker goes too |
| `perf.blockLookupCache` | `BlockBridge.legacyBlock` kept the legacy block of each modern block in a map behind the bridge's lock, taken on every legacy `getBlock` on every thread (server and world generation threads queued on it). The answer now sits in a field on the modern block (`BlockSlotMixin`); only the first lookup per block takes the lock. The 1.7.10 metadata of a vanilla state sits on the state (`BlockStateSlotMixin`) | The answer for a modern block never changes once made (the map entries were never replaced); the field mirrors the map |
| `perf.worldViewCache` | `World.mcc$wrap(level)` (every legacy block, tile and entity callback) looked the view up in a synchronized weak map. Real levels keep their view in a field (`LevelSlotMixin`); generation regions and other mods' copies of a level keep their own paths | The same view object the map holds |
| `perf.entityViewCache` | `EntityBridge.wrap(entity)` (every legacy event and callback of every entity each tick) read a weak-keyed concurrent map; the view is now kept on the entity (`EntitySlotMixin`) | Set wherever the map is set; the map still records all views for saving extended properties and sweeping |
| `perf.reuseBlockPos` | 1.7.10 `World.getBlock` and `getBlockMetadata` read through one `MutableBlockPos` per thread instead of allocating a `BlockPos` per call | Used only for one `getBlockState` of a chunk that is known to be loaded (no nested legacy code can run while the position is in use); vanilla passes mutable positions to `getBlockState` everywhere |
| `perf.hookOverrideCache` | Entity hosts ask "does the legacy class override hook X" every tick (`isPushedByWater`, `updateRidden`, `decreaseAirSupply`, `collideWithEntity`, ...); the cache built a string key and a capturing lambda per call. Now a per-class table compared by parameter arrays (`OverrideCache`), and the hot callers pass constant parameter arrays | Same answers, computed by the same hierarchy walk |

Checked and already cheap: the legacy event buses cache "has a listener for this event class" per class (cleared on register,
unregister and handler disabling), and every per-tick bridge (`EntityTickEvent`, `PlayerTickEvent`, `LevelTickEvent`, the DragonAPI
events) checks it before building a legacy event; legacy blocks are random-ticked only if `getTickRandomly()` is true (modern
chunk sections skip random ticks for sections without such blocks).

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
