# Legacy Connector documentation

Legacy Connector runs unmodified legacy Forge mods (1.4.7, 1.5.2, 1.6.4, 1.7.10, 1.12.2, 1.20.1 and Beta 1.7.3) on NeoForge 1.21.1 by translating them at load time. This is the
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
| [Troubleshooting](Troubleshooting.md) | Start-up failures (Mixin modules, file names, missing dependencies), join failures such as "length wider than 21-bit", measuring join payload sizes |
| [Compatibility and status](Compatibility-and-Status.md) | Status per legacy version, known issues, reading the report |
| [Development](Development.md) | Building, testing, project layout |
| [Addon API](Addon-API.md) | Writing addons for legacy mods |
| [Legacy 1.20.1 estimate](Legacy-1.20.1-Estimate.md) | Effort, approach options and risks for supporting 1.20.1 Forge (and Sinytra) mods |
| [Legacy 1.20.1 fork porting notes](Legacy-1.20.1-Fork-Porting-Notes.md) | A contributor's (kentvillamor08) independent 1.20.1 pipeline notes: scoped adapters, verifiers, pilot results |
| [Legacy 1.20.1 corpus](Legacy-1.20.1-Corpus.md) | Measured gap report of 140 real Forge 1.20.1 jars (72 abandoned content mods): ranked unresolved symbols, mixins, cheap/medium/heavy, order of work |
| [Version roadmap](Version-Roadmap.md) | Which versions come when, and why |
| [Work plan](Work-Plan.md) | How the work is organised: defect classes, automated verification, generic features, who does what |
| [Plan for 1.19.2, 1.18.2, 1.16.5](Legacy-1.16-1.19-Plan.md) | Where each supported version stands, and the staged design for the next three Forge versions |
| [Legacy 1.6.4 estimate](Legacy-1.6.4-Estimate.md) | Effort and approach for a 1.6.4 layer on top of the 1.7.10 shim |
| [Legacy 1.6.4 corpus](Legacy-1.6.4-Corpus.md) | Measured API usage of 43 real 1.6.4 mods (numeric ids, old packets, renamed classes) |
| [Legacy b1.7.3](Legacy-b1.7.3.md) | Beta 1.7.3 ModLoader / Forge 1.0 mods: status, how jars are deobfuscated and linked, plan |
| [Legacy 1.4.7 estimate](Legacy-1.4.7-Estimate.md) | Effort and approach for a 1.4.7 layer (obfuscated names, ModLoader, sprite sheets) over the 1.5.2/1.6.4 layers |
| [Legacy 1.4.7 corpus](Legacy-1.4.7-Corpus.md) | Measured API usage of 65 real 1.4.7 mods (notch names, sprite sheets, ModLoader, numeric ids) |
| [Legacy 1.5.2 estimate](Legacy-1.5.2-Estimate.md) | Effort and measured state of a 1.5.2 layer (a delta over the 1.6.4 one) |
| [Legacy 1.5.2 corpus](Legacy-1.5.2-Corpus.md) | Measured API usage of 67 real 1.5.2 mod files (EntityLiving split, string textures, zips) |
