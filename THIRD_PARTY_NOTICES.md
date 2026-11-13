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
| `loader/.../resource/ItemComponents1201.java`, `Snbt1201.java` (read by `DataConverter1201`) | `resources/ItemStackComponents.java`, `ItemStackDataMigrator.java`, `LegacyItemNbt.java` | Item NBT to data components: the split of an item's keys by owner (Minecraft's or the mod's), the banner pattern table, the attribute modifier mapping and the HideFlags mapping, reduced to the 1.21.1 target and re-checked against the 1.21.1 jars (attribute ids, banner patterns, component names); entity target and loot function rules of `ItemStackDataMigrator`; the SNBT parser and writer are our own code |
| `loader/.../transform/Subscribers1201.java` list of abstract events (`neoforge-abstract-events.tsv`), `loader/.../tools/EventTables1201Tool.java` | `scripts/NeoForgeEventTable.java`, `neoforge-abstract-events.tsv` | Only the idea of writing the abstract event list from the NeoForge jars; the generator is our own code and its output is read from our NeoForge 21.1.258 jars, not copied |
| `loader/src/main/resources/legacyconnector/forward/1.20.1/forge-c-tag-pairs.tsv`, `tools/dev/gen_c_tag_pairs.py` | `forge-convention-tags.tsv` (cross-check only) | Derived from the `#forge:` entries of NeoForge's own `data/c/tags` in the 1.21.1 jars; no row copied |
| `loader/.../transform/mixin1201/ParamAlignment.java`, `HandlerResigner.java`, `InjectionRepair.java` (mixin target proof and handler re-signing of the Forge 1.20.1 translation) | `src/main/java/com/retromod/mixin/AutomaticMixinTranslator.java`, `MixinHandlerResignature.java`, `MixinLocalSlotRepair.java` | Algorithms ported, not pasted: unique ordered-subsequence parameter addition, same-name unique target, handler slot and parameter-annotation shifting, `@Local` survival rules. Re-written against our member index (`mc-members.index.gz`, class tree `mc-hierarchy.tsv.gz`) instead of theirs; none of their mapping or selector-rename tables is used; per-injection removal, report lines, refmap hints and static/instance checks are our own |

Portions of the files above are adapted from Retromod; the licence text of Retromod is:

```
MIT License

Copyright (c) 2026 Bownlux

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

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

Bundled unmodified as a jar-in-jar (`META-INF/jarjar/`) and used through its public API (`terrablender.api.Regions`) to place legacy mod biomes in the overworld. A separately installed TerraBlender takes precedence. Source and license text: the project page above.
