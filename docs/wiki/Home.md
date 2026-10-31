# Legacy Connector documentation

Legacy Connector runs unmodified legacy Forge mods (1.7.10, 1.12.2) on NeoForge 1.21.1 by translating them at load time. This is the
technical documentation; the [README](../../README.md) has installation and the status overview, and the
[compatibility site](https://kylepitcock.github.io/Legacy-Connector/) lists how each mod works.

## Pages

| Page | What |
|---|---|
| [Architecture](Architecture.md) | Loader, shim, synthesized API, runtime; the legacy loader behaviour that is reproduced |
| [Bridges](Bridges.md) | Outdated dependencies replaced by modern mods, and every engine bridge with its state |
| [World generation](World-Generation.md) | Biomes, world types, structures, fluids and input |
| [Mod dimensions](Mod-Dimensions.md) | Dimensions registered by legacy mods, runtime dimensions, mixed 1.7.10/1.12.2 packs |
| [Entities](Entities.md) | Entity hosts, AI, attributes, renderers and models |
| [Rendering](Rendering.md) | Textures, block models, tile entity renderers, item renderers |
| [Create and Sable](Create-and-Sable.md) | Legacy blocks on Create contraptions and Aeronautics/Sable ships |
| [Recipes](Recipes.md) | Changing legacy recipes with data packs, KubeJS and MineTweaker |
| [Configuration](Configuration.md) | `config/legacyconnector.properties` |
| [Performance](Performance.md) | Measured memory, frame rate and startup cost |
| [Compatibility and status](Compatibility-and-Status.md) | Status per legacy version, known issues, reading the report |
| [Development](Development.md) | Building, testing, project layout |
| [Addon API](Addon-API.md) | Writing addons for legacy mods |
