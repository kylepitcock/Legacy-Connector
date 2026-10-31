# Mod dimensions

Back to [Home](Home.md); see also [World generation](World-Generation.md).

A dimension a legacy mod registers with `DimensionManager.registerDimension` becomes the modern dimension
`legacyconnector:dim_<id>`. Its chunk generator (`LegacyChunkGenerator`) runs the mod's `WorldProvider.createChunkGenerator()`:
terrain comes from `provideChunk` (block/metadata arrays), trees, structures and ores from `populate`. Biomes come from the
provider's own `WorldChunkManager` layers (`LegacyBiomeSource`), and `World.provider` is an instance of the mod's provider class.
`ServerConfigurationManager.transferPlayerToDimension` and `Teleporter` work as in 1.7.10. Verified with Twilight Forest's
terrain, portal and fog, and Galacticraft's planets (per-planet gravity, silent space). A dimension takes its fog colour, cloud height, sky/cloud/weather renderers and flat-world horizon from its legacy `WorldProvider`; legacy sky renderers that draw with unsupported GL fall back to the vanilla sky.
Dimensions registered while the server runs get the same kind of level on first use (`worldServerForDimension`,
`DimensionManager.initDimension`, teleports), with a generic dimension type with or without a sky, since dimension types
cannot be added to a running server. Verified with Mystcraft's instability profiling dimension and a dimension registered at
runtime with Twilight Forest's provider; Mystcraft ages and Galacticraft space stations take the same path but are not
verified yet.

A pack may mix 1.7.10 and 1.12.2 mods. A library shipped for both versions (CodeChickenLib) or an API shaded by mods of both
keeps one copy per version: the 1.12.2 jars' copy of every package that 1.7.10 jars also define is relocated, so each version's
mods link to their own, and `Loader.isModLoaded` does not report a mod of the other version.
