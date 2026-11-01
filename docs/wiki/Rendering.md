# Block and item rendering

Back to [Home](Home.md); see also [Entities](Entities.md).

- **Textures:** `registerBlockIcons`/`registerIcons` run when the resource pack is generated; icon names become sprite
  ids (vanilla 1.7.10 names map through a rename table to the 1.21 names). `TextureMap`/`TextureAtlasSprite` are real:
  sprites a mod builds itself with a custom loader (Railcraft's unstitched sheets, HBM's recoloured textures) are run
  at pack generation and written out as ordinary textures.
- **Blocks:** the generated block models cover cubes, partial blocks, plants, crops and stairs. Blocks drawn by an
  `ISimpleBlockRenderingHandler` get a model that runs the handler while the chunk is meshed: the Tessellator
  captures its quads (`RenderBlocks` implements the face renderers) and they become the block's baked quads.
  Tile entity special renderers (`TileEntitySpecialRenderer`) run in the render context like entity renderers.
- **Items:** Forge `IItemRenderer`s and 3D `ISimpleBlockRenderingHandler` inventory renders draw through a
  `builtin/entity` model. `AdvancedModelLoader` loads Wavefront OBJ models for renderers.
- **Display lists** (`glGenLists`/`glNewList`/`glCallList`) are recorded and replayed (`DisplayLists`) against the matrix, colour and texture current at the call; a finished list keeps its actions in an array (found by id without boxing) and its Tessellator draws as assembled quads with their face normals (from the second call on), so a call per frame only writes vertices. Lists with state changes replay those changes as they were recorded. `glIsList` answers whether an id was compiled,
  because model libraries (Traincraft's bundled TMT) call their cached list only while it is true and drop their geometry after compiling it.
- **Carts:** a mod cart's legacy `posY` is the modern bottom plus its current `yOffset` field (Traincraft's rolling stock sets 0.65 after the constructor).
- **Vanilla behaviour constants** (render types, opacity of the vanilla block classes) come from a table generated
  from the 1.7.10 jar (`tools/extract_block_constants.sh`), since the vanilla classes are shells.
- Handler quads of one colour get modern face shading and ambient occlusion (1.7.10's baked-in face shade is divided out).
- The Tessellator keeps 1.7.10's `color` (ABGR) and `brightness` fields current, and `RenderBlocks` records each untinted face's shade in `color`: mods read
  them by reflection to cache lighting (AE2's cable bus parts restore the cached colour per face; an unreadable field drew them black).
- **Threads:** the emulated GL state (`RenderContext.State`: frames, colour, bound texture, enables, blend and depth state, matrix modes, glBegin vertices,
  polygon mode) is per thread, and `Tessellator.instance` is a static virtual field (`Tessellator.mcc$get$instance`): a chunk-meshing thread that
  captures a block renderer gets a Tessellator of its own, the render thread keeps the shared one. `ForgeHooksClient.worldRenderPass` is per thread too.
  The render thread never waits for chunk meshing; captures still run one at a time among themselves, as block renderers were written for 1.7.10's
  single chunk thread. `-Dlegacyconnector.renderLock=true` makes the render thread take that lock as well (a mod whose block renderer shares static
  scratch state with its tile or item renderer).
- **Tile entity culling:** a legacy tile entity renderer draws while the camera is closer than its `getMaxRenderDistanceSquared()` (64 blocks by
  default; renderers of big structures return more) and while its `getRenderBoundingBox()` is in the view (infinite boxes always), as 1.7.10 culled
  (1.12.2: the box joined with the block and its neighbours).
- **Zero-size quads** are dropped when a capture is baked: DragonAPI's renderers (all of ChromatiCraft's) end `renderWorldBlock` with four
  `addVertex(0, 0, 0)` so 1.7.10's chunk pass was never empty, which baked into a quad at the world origin in both layers of every such block.
- **Full opaque cubes have no holes:** for a block that is a full opaque cube in 1.7.10's terms (`isOpaqueCube`, `renderAsNormalBlock`, full bounds),
  modern neighbours cull their faces against it. After the handler ran, every face the modern pipeline shows (`Block.shouldRenderFace`) but no
  captured quad lies on is drawn as `RenderBlocks`' standard face with the block's world icon (logged once per block class). A face the handler
  skipped (its view of a neighbour, `isOpaqueCube`, disagreed with the modern occlusion shapes, or its pass logic left it out) showed the sky through
  the terrain (ChromatiCraft's crystalline stone under pylons and rune shrines).
- **Full-bright parts:** corners a block renderer drew with its own full block light (`setBrightness(240)`, the 1.7.10 idiom for glowing overlays) keep
  that light in the baked quad; the modern pipeline uses the larger of baked and world light, so they glow in the dark.
- Not yet: per-vertex colour of handler-drawn blocks, `IIcon`s of non-block atlases.
