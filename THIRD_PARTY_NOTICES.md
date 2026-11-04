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

## PortKit

[PortKit](https://github.com/portkit) (Forge/NeoForge/Fabric porting tool for 1.20.1 and newer), released under a free license by its author, who agreed to this use.

| Our file | Taken from | Changes |
|---|---|---|
| `loader/.../tools/BytecodeCheck` | `linkcheck/scan/BytecodeVerifier` | Only the idea (verify translated classes with ASM's `Analyzer` and a `SimpleVerifier` whose type relations come from the project's own class index, skipping methods that touch unknown types; interface-typed values are assignable as in the JVM verifier). The code is our own, written against `Hierarchy` |
