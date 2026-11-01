# Block and item rendering

Back to [Home](Home.md); see also [Entities](Entities.md).

- **Textures:** `registerBlockIcons`/`registerIcons` run when the resource pack is generated; icon names become sprite
  ids (vanilla 1.7.10 names map through a rename table to the 1.21 names). `TextureMap`/`TextureAtlasSprite` are real:
  sprites a mod builds itself with a custom loader (Railcraft's unstitched sheets, HBM's recoloured textures) are run
  at pack generation and written out as ordinary textures.
- **Modern leaves** (`textures.modernLeaves`, `ModernLeaves` + `LeafModernizer`): most mod trees recolour the old vanilla
  leaves, which 1.14 redrew. The icons legacy leaf blocks register (`BlockLeaves` subclasses, blocks of leaves material) are
  checked against the cut-out masks of the old oak (= birch, acacia, dark oak), spruce and jungle leaves, known only by
  their hashes (`leaf-fingerprints.txt`, `tools/gen_leaf_fingerprints.py`; no Mojang pixels are shipped). A texture whose
  mask matches, also after taking off its decoration pixels (berries, flowers, fruit: colours that do not follow the
  leaf's colour ramp), is a recolour and becomes the reference of that kind (other leaf-named block textures of the translated jars, such as plain copies of the old leaves, serve as references too); textures with edited holes are recolours when
  their brightness pattern correlates with a reference (rank correlation 0.7 or more, at most 64 mask pixels different).
  A recolour is rebuilt from the modern vanilla leaves of its kind: the mod's leaf colours sorted by brightness are laid on
  the modern texture's brightness ranks (so hue, saturation and brightness range are the mod's, and grey biome-tinted
  leaves stay grey), the cut-out is the modern one, and decoration pixels stay where the mod drew them. A fast (opaque)
  texture that is a filled copy of a converted fancy one (paired by pixels, and by name: `_fast`/`_fancy`, `_opaque`,
  `low`) gets the same pattern with its holes filled from its own hole colours. Hand-drawn leaves (other shapes or
  patterns), multi-coloured art, textures with fewer than 120 drawn pixels, animated textures and sizes other than 16x16
  or clean upscales of it are kept, and exact copies of old vanilla leaves are left to `modernizeTextures`. The log
  (`LegacyConnector/Leaves`) has a line per converted and per kept texture with the reason, and a summary; results go into
  the generated pack and are reused on reloads. `java -cp build/classes/java/main;src/main/resources
  org.kylepitcock.legacyconnector.core.pack.LeafModernizer <1.21.1 client jar> <out dir> <legacy jars...>` converts the
  leaf textures of jars offline and writes before/after pictures.
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
- **Sky renderers of vanilla dimensions:** a legacy `WorldProvider.setSkyRenderer` on a vanilla dimension (ChromatiCraft's
  `EndOverhaulManager` sets its End tendril sky, `Chroma_EndTendrils`) is only stored: legacy sky renderers are drawn only for legacy
  dimensions (`LegacyDimensionEffects.renderSky`, in the sky pass, logged as "Sky of dimension ..."), so the End keeps its modern sky.
- **Not ours, End in ATM10:** the green/purple band with vertical streaks drawn across the terrain is the Auroras mod (`config/auroras/the_end.json`):
  with Iris's `enableShaders` on it draws at the end of `LevelRenderer.renderLevel` with the depth test off, which covered the terrain when no
  shader pack was actually in use (fixed by the optional pack compat `compat.auroras.irisPackCheck`, see [Configuration](Configuration.md)).
  The small black squares floating everywhere in Nullscape's shadowlands are that biome's ambient `minecraft:ash` particles, as in basalt deltas.
  To find out whether a legacy handler draws something, skip it with `events.skip` (see [Configuration](Configuration.md)).
- Not yet: per-vertex colour of handler-drawn blocks, `IIcon`s of non-block atlases.
