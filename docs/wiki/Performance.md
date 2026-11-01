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

## Memory

Legacy views of modern objects (the `World` of a level, an entity's legacy `Entity`, a connection's `NetworkManager`, ...) are
cached per object so legacy code sees one object per modern one. The caches have weak keys, but each view holds its modern
object, so an entry never goes away on its own. Each switch below is a `perf.*` key in [Configuration](Configuration.md);
none duplicates FerriteCore or ModernFix (they work on modern block states, models, shapes and resources, not on these maps).

- `perf.releaseUnloadedLevels` (on): every client level left behind by a dimension change stayed in memory through its `World`
  view, entity views and tile views (all its chunks, entities and light data: tens of MB at render distance 8, over 100 MB at 16),
  and every integrated server of a closed singleplayer world through `MinecraftServer`, `Scoreboard` and `GameRules` views. The
  entries of a level are removed when `LevelEvent.Unload` has reached every other listener (legacy `WorldEvent.Unload`
  handlers have seen the view), those of a server after `ServerStoppedEvent`. Entity and tile views of an unloaded level are
  swept too (they used to be kept until the entity was removed, which never happens to a dropped client level). A released
  view stays the same object for anyone who holds it: asked for again (chunk unload events after the level's at shutdown), the
  released view comes back while it is held (weak reference), otherwise a new one, which compares equal (views compare by level).
- `perf.sweepRemovedPlayers` (on): player views were never swept (players change dimension in place, removed and revived in one
  call), so the old player of every death and respawn stayed. A player removed for another reason than a dimension change at two
  sweeps in a row (20 to 40 s) is swept.
- `perf.sweepDeadViews` (on): the 1.7.10 `NetworkManager` of a connection (holding its packet listener, player and on the client the
  level) is swept two sweeps after its channel closed; the modern container view of a legacy inventory once its block entity is
  removed or unloaded; the 1.12.2 view of a game block entity likewise.
- `perf.weakStackViews1122` (off): 1.12.2 keeps one live view per modern item stack, and every stack it ever viewed stayed in
  memory. With the switch on a view is held weakly unless it made capabilities (their providers may hold state that is not in the
  tag); tag changes still pending are held by the flush list. Off by default: a mod that keeps a tag compound without its stack
  and changes it later would see the change lost.
- `perf.releaseLoaderData` (on): the loader is an FML service whose classes live as long as the game. On a translation run it
  read the legacy hierarchy, method and field tables (about 3 MB of text per version, several times that as maps), MCP name tables
  and the class infos of every library class it linked against; they stayed for the whole session. They are dropped when
  discovery ends.

Profiles of an ATM10 client and server with about seven legacy mods (CPU samples, 2026-10-31) show the legacy layer in under
3% of the allocating frames of the render thread and under 0.5% on the server thread: the heap of such a pack is the modern
mods'. Heap histograms (`/spark heapsummary`) are what attributes the rest.

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
