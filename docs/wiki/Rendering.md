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
  scratch state with its tile or item renderer); the loader sets it by itself when a 1.4 to 1.7 jar holds a `CCRenderState` (CodeChickenLib, or a repackaged copy such as CoFH Core's), whose static pipeline raced between a chunk thread and the render thread (RedNet cables, Thermal Expansion cells flickered or threw NullPointerExceptions).
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

## Connected textures (Chisel, CTM)

The 1.12 Connected Textures Mod keeps its settings in `<texture>.png.mcmeta` (`"ctm": {"type": ..., "extra": {"size": N}}`) and its own model
loader picks the tile. The loader's `CtmBridge` converts the two sheet types that would otherwise be stitched as one squeezed sprite: a
`random` sheet (N x N variants) or `pattern` sheet (N x N tiles) becomes its first tile as the sprite, and every tile is written as a numbered
image beside an OptiFine-format `ctm.properties` (`method=random` / `method=repeat`) under `assets/minecraft/optifine/ctm/legacyconnector/`, which
the modern connected textures mod Continuity (NeoForge build) reads. Without Continuity the first tile shows. Connected-edge types (`ctm`,
`CTMH`, `pillar`, `sctm`) keep their plain sprite; mapping them to Continuity's `ctm`/`horizontal`/`vertical` methods is not done yet.

## Iris and Sodium

Checked against Iris 1.8.14 and Sodium 0.8.13 (NeoForge 1.21.1) by reading their bytecode. The emulation registers no shader program of its
own: every legacy draw goes through a render type whose shader is one of vanilla's `GameRenderer` getters, which Iris overrides with the
gbuffers program of the current phase, and through the batched buffer sources (nothing is drawn immediately with `BufferUploader` during the
level render). The adaptations below apply only while a shader pack is in use and can be switched off with `render.irisCompat`
(see [Configuration](Configuration.md)); Iris is optional and only reached by reflection (`compat/IrisState`).

| Legacy path | Without a shader pack | With a shader pack (Iris program) |
| --- | --- | --- |
| ISBRH blocks (`IsbrhBlockModel`) | baked quads through `ModelData`, cutout and translucent passes | the same: Sodium calls `getModelData` with its `LevelSlice`, `getRenderTypes` and `getQuads` with the data and each type (`NeoForgeModelAccess`) → gbuffers_terrain / water |
| Tile entity and entity renderers, opaque | `entityCutoutNoCull` | gbuffers_entities, gbuffers_block during the block entity phase |
| ... alpha blended | `entityTranslucent` | the same programs, translucent; a model drawn with plain alpha blending at full colour alpha, depth writes on, whose texture has only opaque and clear texels (`TextureAlpha`, read once per texture) draws as `entityCutoutNoCull`, which looks the same: Iris's translucent batch did not stay behind the opaque models drawn with it (Fisk's Heroes' claw lamp layer showed its whole eye cube through the claw) |
| ... additive (glows, halos, beams: `GL_SRC_ALPHA, GL_ONE` or `GL_ONE, GL_ONE`) | `additiveAlpha` / `eyes` | `glow`: the beacon beam shader → gbuffers_beaconbeam (falls back to gbuffers_textured; the same in every phase, while the eyes shader becomes the lit gbuffers_block while block entities draw) |
| ... without a depth test (overlays through walls) | `cutoutNoDepth`, `translucentNoDepth`, `untexturedNoDepth`, `additiveAlpha` no-depth | same programs; Iris's batched rendering draws them last (the class of vanilla's no-depth types) |
| ... untextured | `debugQuads` / `lightning` | gbuffers_basic / gbuffers_lightning (falls back to gbuffers_entities) |
| GL_LINES | `lines` | gbuffers_line, drawn with the other lines |
| Legacy particles (`AFTER_PARTICLES`) | entity types | `particle`: the particle shader → gbuffers_particles (falls back to gbuffers_textured_lit; gbuffers_weather in the rain phase); additive ones `glow` |
| Legacy sky renderers (`LegacyDimensionEffects.renderSky`, custom sky phase) | entity types | `skyTextured` / `skyUntextured`: position_tex_color / position_color → gbuffers_skytextured / skybasic, no depth writes |
| Legacy cloud and weather renderers, `RenderWorldLastEvent` (`AFTER_LEVEL`), DragonAPI's entity loop events | entity types | entity programs (drawn into the gbuffers before Iris's composite passes) |
| Screens and HUD (`guiUnlit`, `guiAdditive`, Stated wrappers) | position_tex_color, depth/blend state carried by the type | unchanged: Iris overrides nothing outside the level render |

**Shadow pass.** Iris draws entities and block entities a second time from the sun (`ShadowRenderer`: `renderEntity`,
`BlockEntityRenderDispatcher.render`; no render stage events), so legacy tile entity and entity renderers run twice a frame. Overlays
(no depth test), lines and additive glows are not drawn into the shadow map (they are no surfaces and shadowed everything), and particles legacy
renderers spawn while they draw (`EffectRenderer.addEffect`, `World.spawnParticle`, 1.12.2's `ParticleManager`) are dropped there, so they
are not doubled. Render contexts, matrix stacks and counters are per call and balanced, so the second run leaves nothing behind.

**GL state.** Legacy `glDisable(GL_DEPTH_TEST)`, `glDepthMask`, `glColorMask`, `glDisable(GL_CULL_FACE)`, blending, blend function and equation,
polygon offset, logic op, scissor and depth function go straight to `RenderSystem`. The value each had is saved from GlStateManager's cache
the first time legacy code changes it in a render context and put back when the outermost context ends (`LegacyGlState`): modern render types
set only what differs from the defaults, so a leftover depth or colour mask made later drawing (and Iris's passes) lose depth or colour.

**Sodium and model data.** Sodium meshes chunks with `BlockRenderer.renderModel`: `PlatformModelAccess.getModelData(slice, model, state, pos,
data)` (NeoForge: `BakedModel.getModelData` on the `LevelSlice`), `getModelRenderTypes` (`getRenderTypes(state, random, data)`) and per type
`getQuads(state, side, random, data, type)`, so legacy block renderers run once per block on the meshing thread exactly as with vanilla's
section compiler; quads without a cull face are never culled. The `LevelSlice` clones the blocks and block entities of the section and a
2-block border, the range legacy renderers read.

**Not covered.** Legacy code that binds a framebuffer of its own while the level is drawn (1.12.2 `Framebuffer.bindFramebuffer`) draws into
it only until the next draw: Iris binds its gbuffers when it applies a program. Iris's `block.properties` ids do not know legacy blocks, so a
pack treats them as plain blocks (no waving leaves, no emissive ores).

## Modernized vanilla art (`modernizeTextures`)

Legacy mods copy or paint on the vanilla art of their time. With `modernizeTextures = true` (default, see [Configuration](Configuration.md)) the
generated pack replaces exact copies of old vanilla block, item and GUI textures by the modern 1.21.1 texture of the same thing, and
the old vanilla base (stone, cobblestone, planks, furnace, wool...) under a mod's own painting by the modern base (the mod's pixels
stay). The matching uses a table of fingerprints, never Mojang's pixels: `src/main/resources/legacyconnector/legacy/texture-fingerprints.txt`,
written by `tools/gen_texture_fingerprints.py`. The log has a line per mod: `<mod>: modernized N textures (X exact copies, Y backgrounds)`.

Vanilla art covered (each version's textures that differ from the later ones are in the table, so a mod copying any of them is recognised):

| Version | Source of the old art | Where the mod's copies are looked for |
|---|---|---|
| Beta 1.7.3 | `terrain.png`, `gui/items.png`, `gui/*.png` of the b1.7.3 client (tile index -> 1.4.7 index table) | the sprites `AssetsB173` generates (sheet tiles, ModLoader overrides): `textures/block/b173/*` of the generated pack |
| 1.4.7 | `terrain.png`, `gui/items.png`, `gui/*.png` (index table `v1_6_4/sheet-terrain.tsv`, `sheet-items.tsv`) | the tiles `SheetCutter` cuts from the mod's sprite sheets (`assets/legacy147/textures/blocks/<sheet>/<n>.png`) and its single textures |
| 1.5.2 | `textures/blocks|items/*.png` (camelCase names matched to 1.7.10 names by name or pixels) | the mod's texture files |
| 1.6.4 | `assets/minecraft/textures/blocks|items|gui/container` | the mod's texture files |
| 1.7.10, 1.12.2 | the client jars | the mod's texture files |

Regenerate the table with the client jars from the Mojang version manifest (`https://launchermeta.mojang.com/mc/game/version_manifest_v2.json`;
download only from `launcher.mojang.com` / `piston-data.mojang.com` and check the sha1), extracted outside the repository:
`tools/gen_texture_fingerprints.py <1.7.10 jar> <1.12.2 jar> <1.21.1 jar> --b173 <jar> --147 <jar> --152 <jar> --164 <jar>`.
Not covered: entity (mob) textures (their layouts changed), animated textures (water, lava, fire, portal keep their frames) and
sheets of 1.5.2/1.6.4 mods that have no vanilla counterpart. Offline report: `./gradlew checkLegacyMod -PlegacyJar=... -PkeepDir=<dir>`
keeps the translated jars, then `./gradlew textureReport -PlegacyJar=<dir>/a.translated.jar`. Pictures: `docs/screenshots/modernize/`.
