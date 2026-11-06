# Estimate: legacy 1.6.4 Forge mods on NeoForge 1.21.1

Research note, written 2026-11-06 from the repository state and general knowledge of Forge 9.11 / MC 1.6.4 (not re-verified
against the 1.6.4 sources; there are **no 1.6.4 jars in `test-mods*`**, so "what mods use" below is an expectation). Back to [Home](Home.md).
Not on the roadmap yet (the order in `CLAUDE.md` is 1.7.10, 1.12.2, 1.20.1, 1.16.5, 1.2.5); this answers "how big would it be".

## Short answer

**About 3 to 5 weeks of focused work to reach the level 1.7.10 has today ("loads, server side works, a playable client"), if 1.6.4 is
built as a thin layer on top of the existing 1.7.10 shim. A separate 1.6.4 shim (the way 1.12.2 was done) would be 8 weeks or more** and is the wrong
choice: 1.6.4 is the closest of all old versions to 1.7.10.

Reference sizes (this repo): the 1.7.10 shim is 1,339 files / 112k lines plus 183 bridge files / 33k lines; the 1.12.2 shim is 169k lines.
1.7.10 took about two weeks from the first commit (2026-10-10) to the first playable packs, 1.12.2 about two weeks from 2026-10-25 to its
"playable tier", both with several parallel agents working from the generated gap lists in `tools/dev/agent-tasks`.

## What differs between 1.6.4 and 1.7.10

Package names are the same (`cpw.mods.fml`, `net.minecraftforge`, `net.minecraft.*`), which is what makes a forward layer viable
(`LegacyVersion.fromMinecraftVersion` already maps "1.6*" strings to the 1.7.10 shim). The differences, by size:

| Area | 1.6.4 | Work |
|---|---|---|
| **Numeric ids** (the big one) | `Block.blocksList[id]`, `Item.itemsList[id]`, `block.blockID`, `item.itemID`, `new Block(int, Material)`, `new Item(int)`, `new ItemStack(int, n, dmg)`, `world.getBlockId/setBlock(x,y,z,id,...)`, ids read from the mod's config; biome, potion, enchantment and entity ids numeric too | An **id facade**: sparse id tables (4096 blocks, 32000 items, 256 biomes...) assigned at registration (config id first, else next free), `blockID`/`itemID` as virtual fields (the existing `mcc$get$` mechanism), int-taking constructors and world methods mapped onto the 1.7.10 shim's name-keyed objects, vanilla id table (about 500 constants). About 1,200 to 2,000 lines of shim plus loader field tables. Risk: mods that write ids into their own saves or arrays, id conflicts. **1 to 1.5 weeks.** |
| Vanilla statics | `Block.stone`, `Block.dirt`, `Item.ingotIron`... (1.7.10: `Blocks.stone`, `Items.iron_ingot`, several renamed) | Static provider on the shim's `Block`/`Item` classes (the repo already has `static-arrays.txt` plumbing). About 800 names, mostly data. 2 to 3 days |
| `Icon` / `IconRegister` | 1.7.10: `IIcon` / `IIconRegister` | Type rename in the loader remapper plus a shim interface alias. 1 day |
| Mapping tables | MCP 8.x / SRG of 1.6.4 (different `func_` numbering from 1.7.10) | New `fields.txt` / `methods.txt` / `hierarchy.txt` for 1.6.4 (from the Forge 9.11.1.1345 MCP zip), same generator as 1.7.10 and 1.12.2. 3 to 4 days including tooling fixes |
| Networking | `Packet250CustomPayload`, `IPacketHandler`, `@NetworkMod`, `NetworkRegistry.registerChannel`, `IPlayerTracker`, `PacketDispatcher` | Adapter onto the existing FML channel bridge (`FmlChannelBridge`). 4 to 6 days; many mods hand-roll packets, so this is also where most per-mod bugs will come from |
| Mod entry | `@Mod` with `@PreInit/@Init/@PostInit` (older mods) next to `@EventHandler` | Accept both in `LegacyJarInspector` and the lifecycle. 1 to 2 days |
| Rendering | Same `Tessellator`, `ISimpleBlockRenderingHandler`, `IItemRenderer`, `RenderBlocks`; `Icon` stitching; textures under `assets/<id>/` (1.6+) but older mods use `/mods/<id>/` or `/textures` paths | Reuse the 1.7.10 client bridges; extra texture path resolver. 3 to 5 days |
| Small API deltas | `World.getBlockId` family, `Entity` data watcher ids, `EntityRegistry.registerGlobalEntityID(class, name, id)`, `GameRegistry.registerBlock(block, name)` (without ItemBlock class overload in places), `LanguageRegistry.addName`, `Item.setUnlocalizedName`, `Material`/`StepSound` fields, `TileEntity.validate` | The long tail. This is the part the generated gap lists (`probe.py` over a 1.6.4 corpus) measure; budget 1.5 to 2 weeks with parallel agents |
| World generation / dimensions | `IWorldGenerator`, `WorldProvider.getRandomizedSpawnPoint`, biome ids numeric | Reuses the 1.7.10 bridges (placed features, biome modifiers). The id facade covers biome ids. 2 to 3 days |
| Resources | `.lang` files same, `mcmod.info` same, `pack.mcmeta` | Existing converter. 1 day |

## Approach

1. Add `V1_6_4` to `LegacyVersion` as a **forward version**: its remapper maps 1.6.4 names to the 1.7.10 shim names (the loader has this
   mechanism for the 1.20.1 renames under `loader/.../forward/`), plus the id facade classes under `legacyconnector/v1_6_4/` that only exist
   to translate numeric ids, `Icon` and the old packet API. Everything else (world, entities, rendering, world generation, capabilities, recipes) is shared with 1.7.10, so
   every 1.7.10 fix keeps paying off.
2. Collect a corpus (CurseForge/Modrinth have the 1.6.4 files of the era: IC2 1.115, BuildCraft 4.x, Forestry 2.x, Thermal Expansion 3, Railcraft 7, Mekanism 6, Twilight Forest 1.x, Biomes O' Plenty 1.6.4, Thaumcraft 4.0/4.1, Applied Energistics rv1, Industrial... ) and run the offline probe (`tools/compat/probe.py --mc 1.6.4`) to produce the gap lists, exactly as was done for 1.12.2.
3. Work the lists with parallel agents in order of use; use the server self tests and the headless/real client for rendering.

## Milestones

| Milestone | Content | Estimate |
|---|---|---|
| M0 | Corpus (30 to 50 jars), mapping tables, `LegacyVersion.V1_6_4`, inspector accepts the old `@Mod` forms, probe numbers | 1 week |
| M1 | Id facade + vanilla statics + `Icon`: the first content mods register blocks/items/tiles and the server self test passes | 1 to 1.5 weeks |
| M2 | Network adapter, entities, world generation, recipes: server-side "playable" | 1 to 1.5 weeks |
| M3 | Client rendering and GUIs through the 1.7.10 bridges, probe gap lists worked down | 1 to 2 weeks |

Total: **4 to 6 weeks** elapsed for a first usable 1.6.4 tier, 3 to 5 if the corpus turns out clean. Compatibility will stay lower than 1.7.10's for
mods with their own packet protocols and tight id handling.

## Risks and cost drivers

- **Value:** few popular mods exist *only* on 1.6.4; most have a 1.7.10 version already supported. The audience is people who run
  an old 1.6.4 pack for its mod mix (Tekkit-era, Feed The Beast Ultimate, Hexxit). Worth doing only after the 1.20.1 and 1.16.5 work, as the roadmap has it.
- **Numeric ids** leak everywhere (arrays indexed by id, ids compared as ints, ids written into mod configs, Forge `IDProxy`-style
  checks). The facade must be exact for vanilla ids and stable for modded ones.
- **Test material:** none in the repo today; CurseForge's old files are available but some authors removed them.
- **Coremods:** like 1.7.10, many big 1.6.4 mods (IC2, BuildCraft, ThermalExpansion via CoFH later) patch vanilla with ASM that cannot run here;
  the same per-mod hook bridges as for 1.7.10 mods apply.
- **Shared maintenance:** every API added to the 1.7.10 shim now has a second consumer; keep the id facade out of the shared classes.
