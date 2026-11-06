# Estimate: legacy 1.6.4 Forge mods on NeoForge 1.21.1

Research note, written 2026-11-06 from the repository state and general knowledge of Forge 9.11 / MC 1.6.4. A 43-jar corpus
(`test-mods-164/`, gitignored) has since been scanned; **measured numbers are in [Legacy-1.6.4-Corpus](Legacy-1.6.4-Corpus.md)** and the "Measured" section at the end of this page
lists where they change the estimate. Back to [Home](Home.md).
Not on the roadmap yet (the order in `CLAUDE.md` is 1.7.10, 1.12.2, 1.20.1, 1.16.5, 1.2.5); this answers "how big would it be".

## Short answer

**About 4 to 8 days of wall-clock time to reach the level 1.7.10 has today ("loads, server side works, a playable client"), working the way the
project has worked so far (parallel agents, generated gap lists, real-client checks), because 1.6.4 is built as a thin layer on top of the
existing 1.7.10 shim.** (Corrected 2026-11-06: the first version of this note gave 4 to 6 weeks, a conventional human estimate that ignored the measured pace
of this project. The per-area sizes below are relative; read the "days" as agent-days of work that run in parallel.)

Measured pace (this repo): the first commit is 2026-10-10; by 2026-11-06 it holds the 1.7.10 shim (1,339 files / 112k lines, plus 183 bridge
files / 33k lines), the 1.12.2 shim (169k lines, started 2026-10-25), the 1.20.1 groundwork, 22 releases and the fixes of about 160 issues. 1.12.2 reached its
playable tier about 12 days after it started, as a separate shim; 1.6.4 reuses the 1.7.10 shim, so it is a fraction of that.

## What differs between 1.6.4 and 1.7.10

Package names are the same (`cpw.mods.fml`, `net.minecraftforge`, `net.minecraft.*`), which is what makes a forward layer viable
(`LegacyVersion.fromMinecraftVersion` already maps "1.6*" strings to the 1.7.10 shim). The differences, by size:

| Area | 1.6.4 | Work |
|---|---|---|
| **Numeric ids** (the big one) | `Block.blocksList[id]`, `Item.itemsList[id]`, `block.blockID`, `item.itemID`, `new Block(int, Material)`, `new Item(int)`, `new ItemStack(int, n, dmg)`, `world.getBlockId/setBlock(x,y,z,id,...)`, ids read from the mod's config; biome, potion, enchantment and entity ids numeric too | An **id facade**: sparse id tables (4096 blocks, 32000 items, 256 biomes...) assigned at registration (config id first, else next free), `blockID`/`itemID` as virtual fields (the existing `mcc$get$` mechanism), int-taking constructors and world methods mapped onto the 1.7.10 shim's name-keyed objects, vanilla id table (about 500 constants). About 1,200 to 2,000 lines of shim plus loader field tables. Risk: mods that write ids into their own saves or arrays, id conflicts. **1 to 1.5 weeks.** |
| Vanilla statics | `Block.stone`, `Block.dirt`, `Item.ingotIron`... (1.7.10: `Blocks.stone`, `Items.iron_ingot`, several renamed) | Static provider on the shim's `Block`/`Item` classes (the repo already has `static-arrays.txt` plumbing). About 800 names, mostly data. a few hours |
| `Icon` / `IconRegister` | 1.7.10: `IIcon` / `IIconRegister` | Type rename in the loader remapper plus a shim interface alias. an hour |
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

## Milestones (wall-clock, parallel agents)

| Milestone | Content | Estimate |
|---|---|---|
| M0 | Corpus (30 to 50 jars), mapping tables, `LegacyVersion.V1_6_4`, inspector accepts the old `@Mod` forms, probe numbers | about 1 day |
| M1 | Id facade + vanilla statics + `Icon`: the first content mods register blocks/items/tiles and the server self test passes | 1 to 2 days |
| M2 | Network adapter, entities, world generation, recipes: server-side "playable" | 1 to 2 days |
| M3 | Client rendering and GUIs through the 1.7.10 bridges, probe gap lists worked down | 2 to 3 days |

Total: **4 to 8 days** for a first usable 1.6.4 tier; compatibility will stay lower than 1.7.10's for mods with their own packet protocols and tight id
handling, and that long tail is open-ended like for every version (it is the issue list, not a milestone).

## Risks and cost drivers

- **Value:** few popular mods exist *only* on 1.6.4; most have a 1.7.10 version already supported. The audience is people who run
  an old 1.6.4 pack for its mod mix (Tekkit-era, Feed The Beast Ultimate, Hexxit). Worth doing only after the 1.20.1 and 1.16.5 work, as the roadmap has it.
- **Numeric ids** leak everywhere (arrays indexed by id, ids compared as ints, ids written into mod configs, Forge `IDProxy`-style
  checks). The facade must be exact for vanilla ids and stable for modded ones.
- **Test material:** none in the repo today; CurseForge's old files are available but some authors removed them.
- **Coremods:** like 1.7.10, many big 1.6.4 mods (IC2, BuildCraft, ThermalExpansion via CoFH later) patch vanilla with ASM that cannot run here;
  the same per-mod hook bridges as for 1.7.10 mods apply.
- **Shared maintenance:** every API added to the 1.7.10 shim now has a second consumer; keep the id facade out of the shared classes.

## Measured (corpus of 43 jars, 2026-11-06)

Details and tables: [Legacy-1.6.4-Corpus](Legacy-1.6.4-Corpus.md); scanner `tools/compat/scan_164.py`. What changes in the estimate above:

- **Confirmed, bigger than assumed: numeric ids** are used by 31 of 43 jars (72%): `blockID`/`itemID` fields 26-30 jars, `Block.blocksList`/`Item.itemsList` 22-27, `World.getBlockId` 25,
  int `setBlock` 23, int constructors of Block/Item/ItemStack 18-26 jars, biome/potion/enchantment ids 7-17. Keep the 1 to 2 days, and add: the facade must also cover int-taking constructors of
  about 40 vanilla block/item subclasses (`BlockContainer`, `ItemArmor`, `ItemFood`, `BlockFlower`, `ItemSword`...), `PotionEffect(int, ...)` (17 jars) and `Potion.potionTypes`.
- **Networking is larger than the 4 to 6 days line:** 35 of 43 jars (81%) use `NetworkMod`/`IPacketHandler`/`Packet250CustomPayload`/`PacketDispatcher`, and 34 use `ITickHandler`/`TickRegistry`, 17 `IPlayerTracker`,
  12 `IConnectionHandler`, 12 `KeyBindingRegistry`. The 1.7.10 shim has none of these; budget **2 to 3 days** for the network + tick + tracker + key binding adapters together (all are adapters onto 1.7.10 events/`FmlChannelBridge`).
- **Smaller than assumed: mod entry.** 40 of 40 `@Mod` jars already use `@Mod.EventHandler`; only 1 also has `@PreInit`/`@Init`/`@PostInit`. Cut "1 to 2 days" to a few hours.
- **Smaller than assumed: the class long tail.** Of 1,020 distinct Minecraft/Forge classes referenced by the 40 non-obfuscated jars, 861 exist in the shim unchanged, 84 are renames/package moves (a table in the remapper),
  28 are removed APIs (the network/tick/player-tracker/key-binding families above, `AABBPool`, `Vec3Pool`, `SoundPool`) and only 47 are unaccounted for (61 class-file references, 2 used by more than one jar). The
  "1.5 to 2 weeks" long-tail line is now mostly *member-level* work (1.6.4 method/field names and signatures against the shim), which the class scan cannot measure; run `probe.py --mc 1.6.4` once the mapping tables exist before trusting it.
- **New item not in the estimate: notch-named jars.** 9 of 43 jars (21%: CodeChickenCore, NEI, ChickenChunks, Ender Storage, TreeCapitator, Backpacks, Decocraft, Modular Turrets, VoxelMap) reference Minecraft
  by obfuscated class names (`ye`, `atv`...). The loader needs the 1.6.4 notch to MCP class map for them (1 to 2 days with the FML `joined.srg`); NEI is replaced by JEI anyway, so the real count that needs it is lower.
- **Corpus gaps:** BuildCraft 4, Railcraft, Thermal Expansion 3, CoFHCore, Redstone Arsenal, Waila, Ars Magica 2, Natura, MineFactory Reloaded and ComputerCraft are blocked from third-party download
  (HTTP 403 on the CDN); Biomes O' Plenty, Thaumcraft, Applied Energistics, Mekanism, Galacticraft, GregTech and Pam's have no 1.6.4 file on CurseForge. M0's "30 to 50 jars" is met (43), but a Tekkit/FTB-style
  pack cannot be assembled from it.

Net effect: M0 and M1 stay at about 1 and 1 to 2 days; M2 (network, entities, world generation) grows to 2 to 3 days for the network, tick and tracker adapters; the class-level part of M3 shrinks. Total range: **about 5 to 9 days** wall-clock at the project's measured pace.

## Member forward table (2026-11-06)

1.6.4 call sites reach the 1.7.10 shim through three layers in `loader/.../forward/1.6.4/` (class renames in `class-renames.tsv`, see `ForwardRenames`):

- **Name renames** (`ForwardMembers`, applied in `LegacyRemapper.mapMethodName`/`mapFieldName` to declarations and calls alike, looked up through the 1.6.4 hierarchy so a mod's own subclass of
  `TileEntity` renames `onInventoryChanged()` to `markDirty()`): `member-renames.tsv` (same SRG id, other MCP name) and `member-heuristic.tsv` (a unique same-descriptor pair on one class that only one version has)
  are generated by `tools/gen_forward_164.py` (needs `./gradlew :loader:extractMappings`); `member-manual.tsv` is hand written and wins.
- **Call-site rewrites** (`ForwardCalls`, after relocation, before `ShimLinker`): the R, G and C lines of `member-manual.tsv` change more than the name: a static helper in `legacyconnector/v1_6_4/forward/Forward164`
  (`getTagList(String)`, `tagAt`), wrapping a `String` argument in a `ChatComponentText`, `getBlockMaterial(x, y, z)` through `getBlock(...).getMaterial()`, dropped trailing arguments, nameless NBT tag constructors, a field
  read that became a getter (`player.username`).
- Not covered: int-id members (the id facade), `ChatMessageComponent`, the `Packet*` classes, pools (`AABBPool`, `Vec3Pool`), a mod's own override of a method whose descriptor changed.

Effect on the 40 readable corpus jars (`checkLegacyMod`): members synthesized or stubbed fell from 6,070 to 5,248.
