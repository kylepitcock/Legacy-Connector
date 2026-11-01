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

## Client memory and frame time switches

Each has a `perf.*` key in `config/legacyconnector.properties` (see [Configuration](Configuration.md)). Those on by default draw the same pixels;
none replaces what Sodium, Embeddium, Iris, ImmediatelyFast, EntityCulling, ModernFix, FerriteCore, MoreCulling or Flywheel do (they work on
vanilla and modern content; these work on what the legacy layer generates).

| Key | Default | What it does | Expected effect |
| --- | --- | --- | --- |
| `perf.shareBlockModels` | on | A 1.7.10 block got one generated model per metadata value (`_m0` .. `_m15`), baked 16 times even when all were the same; identical ones are written once and the variants name the first. 1.12 variant models with the same parent and textures are shared the same way | About 2 KB of baked quads and JSON per duplicate; with 1500 legacy blocks, most drawing all values alike, 20-30 MB of heap and a shorter model bake |
| `perf.shareItemModels` | on | Legacy subtype items (`mod:name_N`) and block items whose model files are identical keep one baked model after baking. An `item/generated` model of a 16x16 icon is a few hundred quads | 20-50 KB per duplicate item model. Off by itself with ModernFix (dynamic resources bake models on demand) |
| `perf.packSpill` | on | The generated resource and data packs live as long as the resource manager (the whole session); each file's bytes move to one scratch file (unlinked while open, removed with the process) on its first read and are read back from it later | Every generated model, blockstate, recolored texture and translated recipe leaves the heap: 10-50 MB in large packs. Any I/O failure keeps the bytes in memory |
| `perf.cacheTextureLocations` | on | A legacy mod texture location remembers the modern location it binds | In ATM10 (spark `kMioCFEa5y`) `TextureLocations.modern` was 0.11% of the render thread, from Thaumcraft's eldritch renderers binding their textures per frame |
| `perf.cullLegacyParticles` | on | Legacy particles whose renderer is a built-in one (the default quad, the vanilla particle classes) are skipped when a box around them is outside the view frustum (1.7.10 drew all of them) | Proportional to particles behind the camera; Thaumcraft's and Reika's swarms |
| `perf.cullModParticles` | off | The same for particles drawn by mod code, with a wider box | Beams and rings drawn from a particle outside the view could vanish |
| `perf.particleSetting` | on | 1.7.10's `GameSettings.particleSetting` follows the vanilla option (All 0, Decreased 1, Minimal 2): mods that check it spawn fewer particles, as they did in 1.7.10 | Only with Decreased or Minimal |

Without a switch (always on, no behaviour change): the legacy particle pass returns at once when no legacy particle is alive (it set up its state
and flushed the buffer source every frame: 0.14% of the render thread in ATM10 with Sodium's fully buffered source), the 1.12 default particle
quad is written without new arrays per particle and frame, and the missing-texture check of each bind no longer locks a map bin.

Not done: identical textures under different names stay separate atlas sprites (sharing them would rewrite every model and icon that names them);
GUI renders of legacy item renderers are not cached (a renderer's output can depend on time or NBT, which cannot be detected reliably).
ModernizedTextures and LegacySprites keep their own PNG caches for reloads.

