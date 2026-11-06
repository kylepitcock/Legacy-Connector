# Legacy Connector documentation

Legacy Connector runs unmodified legacy Forge mods (1.7.10, 1.12.2) on NeoForge 1.21.1 by translating them at load time. This is the
technical documentation; the [README](../../README.md) has installation and the status overview, and the
[compatibility site](https://kylepitcock.github.io/Legacy-Connector/) lists how each mod works.

## Pages

| Page | What |
|---|---|
| [Architecture](Architecture.md) | Loader, shim, synthesized API, runtime; the legacy loader behaviour that is reproduced |
| [Cache and Redistribution](Cache-and-Redistribution.md) | What the translation cache contains, the provenance watermark, why translated jars are not loaded from mods/ |
| [Bridges](Bridges.md) | Outdated dependencies replaced by modern mods, and every engine bridge with its state |
| [Library candidates](Library-Candidates.md) | Modern libraries that could replace hand-rolled code (Pehkui adopted for entity size) |
| [World generation](World-Generation.md) | Biomes, world types, structures, fluids and input |
| [Mod dimensions](Mod-Dimensions.md) | Dimensions registered by legacy mods, runtime dimensions, mixed 1.7.10/1.12.2 packs |
| [Entities](Entities.md) | Entity hosts, AI, attributes, renderers and models |
| [Rendering](Rendering.md) | Textures, block models, tile entity renderers, item renderers |
| [Create and Sable](Create-and-Sable.md) | Legacy blocks on Create contraptions and Aeronautics/Sable ships |
| [Recipes](Recipes.md) | Changing legacy recipes with data packs, KubeJS and MineTweaker |
| [Configuration](Configuration.md) | `config/legacyconnector.properties` |
| [Performance](Performance.md) | Measured memory, frame rate and startup cost |
| [Troubleshooting](Troubleshooting.md) | Join failures such as "length wider than 21-bit", measuring join payload sizes |
| [Compatibility and status](Compatibility-and-Status.md) | Status per legacy version, known issues, reading the report |
| [Development](Development.md) | Building, testing, project layout |
| [Addon API](Addon-API.md) | Writing addons for legacy mods |
| [Legacy 1.20.1 estimate](Legacy-1.20.1-Estimate.md) | Effort, approach options and risks for supporting 1.20.1 Forge (and Sinytra) mods |
| [Legacy 1.6.4 estimate](Legacy-1.6.4-Estimate.md) | Effort and approach for a 1.6.4 layer on top of the 1.7.10 shim |
