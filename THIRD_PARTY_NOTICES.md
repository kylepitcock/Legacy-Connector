# Third-party notices

Legacy Connector is released under the MIT License (`LICENSE`). Some data tables are taken from other MIT-licensed projects. Mod jars and
Minecraft assets are never included.

## Retromod

[Retromod](https://github.com/Bownlux/Retromod), MIT License, Copyright (c) 2026 Bownlux.

| Our file | Taken from | Changes |
|---|---|---|
| `loader/src/main/resources/legacyconnector/resource/vanilla-texture-renames.tsv` | `src/main/resources/retromod/flattening-texture-renames.tsv` | Every row re-checked against the 1.21.1 client jar; `quartz_block_lines` points at `quartz_pillar` (the source's later rename does not exist in 1.21.1); extra rows of our own appended |
| `LegacyResourceConverter.MODEL_RENAMES` (glazed terracotta, redstone dust, fire and wall torch parents) | `model-parent-renames.tsv` | Targets re-checked against the 1.21.1 client jar |
| `OreDictBridge.SPECIAL` (vanilla ore dictionary names such as `chest`, `fenceWood`, `dye`, `gravel`) | `forge-ore-dictionary.tsv` | Only names that map to a tag; every `c:` tag re-checked in NeoForge 21.1 and every `minecraft:` tag in vanilla 1.21.1 |
| `loader/src/main/resources/legacyconnector/resource/vanilla-texture-paths.tsv` (read by `TextureLocations`) | `LegacyTexturePathMappings.java` and `texture-migrations.tsv` | Only rows that apply up to 1.21.1; every row re-checked against the 1.21.1 client jar |

## Sinytra Connector

[Sinytra Connector](https://github.com/Sinytra/Connector), MIT License, Copyright (c) Sinytra.

| Our file | Taken from | Changes |
|---|---|---|
| `SideOnlyStripper` (removal of lambda bodies orphaned by side stripping) | `patch/EnvironmentStripperTransformer.java#getMethodLambdas` | Only the idea (collect `lambda$` handles of removed methods); the code is our own, with a fixed point over nested lambdas and a reachability check against the remaining methods |


## RetroMod

[RetroMod](https://github.com/Bownlux/RetroMod) 1.3.1, MIT License, Copyright (c) 2026 Bownlux 

| Our file | Taken from | Changes |
|---|---|---|
| `loader/src/main/resources/legacyconnector/forward/1.20.1/forge-event-renames.tsv`, `forge-fml-renames.tsv` | `retromod/forge-event-renames.json`, `forge-fml-renames.json` | Converted to TSV; every target re-checked against NeoForge 21.1.235 and FML 4.0.45; `RegisterClientReloadListenersEvent` corrected (21.1 has not renamed it yet) |

## TerraBlender

[TerraBlender](https://github.com/Glitchfiend/TerraBlender) 1.21.1-4.1.0.8, GNU Lesser General Public License v3.0, Copyright (c) Glitchfiend.

Required external runtime dependency, not redistributed by Legacy Connector. Used through its public API (`terrablender.api.Regions`) to place legacy mod biomes in the overworld. Source and license text: the project page above.

## Pehkui

[Pehkui](https://modrinth.com/mod/pehkui) `3.8.3+1.21-neoforge`, MIT License. Optional external runtime integration, not redistributed by Legacy Connector. When installed, its scale API backs legacy `Entity.setSize` and player eye-height requests; without it, those requests are skipped with a warning.
