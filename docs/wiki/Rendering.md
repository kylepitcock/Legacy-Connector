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
- **Display lists** (`glGenLists`/`glNewList`/`glCallList`) are recorded and replayed (`DisplayLists`); `glIsList` answers whether an id was compiled,
  because model libraries (Traincraft's bundled TMT) call their cached list only while it is true and drop their geometry after compiling it.
- **Carts:** a mod cart's legacy `posY` is the modern bottom plus its current `yOffset` field (Traincraft's rolling stock sets 0.65 after the constructor).
- **Vanilla behaviour constants** (render types, opacity of the vanilla block classes) come from a table generated
  from the 1.7.10 jar (`tools/extract_block_constants.sh`), since the vanilla classes are shells.
- Handler quads of one colour get modern face shading and ambient occlusion (1.7.10's baked-in face shade is divided out).
- The Tessellator keeps 1.7.10's `color` (ABGR) and `brightness` fields current, and `RenderBlocks` records each untinted face's shade in `color`: mods read
  them by reflection to cache lighting (AE2's cable bus parts restore the cached colour per face; an unreadable field drew them black).
- Not yet: per-vertex colour of handler-drawn blocks, `IIcon`s of non-block atlases.
