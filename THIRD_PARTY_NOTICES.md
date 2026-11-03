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
