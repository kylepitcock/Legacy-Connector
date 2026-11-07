# Estimate: legacy 1.6.4 Forge mods on NeoForge 1.21.1

Research note, written 2026-11-06 from the repository state and general knowledge of Forge 9.11 / MC 1.6.4. A 43-jar corpus
(`test-mods-164/`, gitignored) has since been scanned; **measured numbers are in [Legacy-1.6.4-Corpus](Legacy-1.6.4-Corpus.md)** and the "Measured" section at the end of this page
lists where they change the estimate. Back to [Home](Home.md).
Not on the roadmap yet (the order in `CLAUDE.md` is 1.7.10, 1.12.2, 1.20.1, 1.16.5, 1.2.5); this answers "how big would it be".

## Short answer

**About 1 to 1.5 days of wall-clock time to a first usable 1.6.4 tier (a handful of real content mods playable in a client); the server side (M1 and M2) needs about 2 to 4 more hours.** Built as a thin layer on the 1.7.10 shim.

Wall-clock figures below were corrected on 2026-11-06: elapsed time, not summed agent time, at the project's measured pace (1.6.4 work started 15:27; by 16:45 the loader, member forwards, id layer and network adapters were merged and 4 corpus mods loaded on a server, with 3 to 4 agents in parallel; limits are merging and about three game clients at once). Plus or minus 50%.

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

## Milestones (wall-clock, from 2026-11-06 16:45)

| Milestone | Content | State and estimate |
|---|---|---|
| M0 | Corpus (42 jars), tables, `LegacyVersion.V1_6_4`, class renames, notch names | done |
| M1 | Id facade, statics, `Icon`, member forwards; bridge methods for overridden id-signature methods; biome/potion/enchantment ids | done: bridge methods (`BridgeMethods`, table `forward/1.6.4/bridge-methods.tsv`: a mod override with a 1.6.4 id signature gets the 1.7.10-shaped method that calls it, `super.` and shim calls are rewritten); biome, potion and enchantment ids are the shim's own numeric ids and needed no facade |
| M2 | `@NetworkMod`, packets, tick handlers, trackers seen working; entities, worldgen, recipes | adapters merged, unverified; with M1 |
| M3 | Client rendering, textures, GUIs, notch-named jars | started 2026-11-06 (client session `run-c164`): Twilight Forest, ExtrabiomesXL, Carpenter's Blocks, BiblioCraft (OBJ tile entity renderers, container GUIs), Minechem, Modular Turrets, Ender IO and Mystcraft blocks/items render with the existing 1.7.10 resource conversion (no 1.6.4-specific converter was needed: 1.6.4 jars use `assets/<id>/textures/blocks|items`); tile entity blocks need the override bridge for `createNewTileEntity(World)`; notch-named jars not started |

Then 1.5.2 (about 1 day, overlapping) and 1.4.7 (0.5 to 1 day after it); the 0.4.0 release is about 3 to 4 days out (range 2 to 5).

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

## Server loop result (2026-11-06, 62 legacy mods of the 48 corpus files)

`runServerSubTest` over the whole corpus: 53 mods construct and run their pre-init to post-init, 1 client-only mod (JourneyMap, `@SideOnly(CLIENT)` main class) is skipped on a dedicated server,
9 fail. The self test prints "1.6.4 tick handlers" (19 registered, each with its call count and first error), "1.6.4 network" (64 channels, 74 packet handlers) and a loopback check of the
packet receive path (`SelfTest164`). Remaining failures: Thaumic Tinkerer, Gendustry (Scala 2.9 library, Thaumcraft and bdew lib are not in the corpus), bspkrs Core and TreeCapitator
(client classes loaded on a server by verification), Binnie's three mods and Magic Bees (Forestry's recipe and genetics managers are null when they run), Mystcraft (duplicate
symbol ids). `.zip` mods are now picked up like FML did (a third of the corpus was ignored before).

## Measured again (2026-11-07, stubs and unresolved references)

Offline link check (`checkLegacyMod`) over `test-mods-164` (98 jars with `old-only/`), `test-mods-152` (45) and `test-mods-147` (64); synthesized members (empty stubs) / synthesized classes / unresolved references:

| Version | Before | After |
|---|---|---|
| 1.6.4 | 1424 / 182 / 57 | 1113 / 167 / 57 |
| 1.5.2 | 596 / 121 / 11 | 474 / 112 / 11 |
| 1.4.7 | 2210 / 390 / 72 | 1958 / 374 / 72 |

Replaced by real behaviour: `Entity.dropItem(id, n)`, fire tables (`Block.blockFlammability`, metadata shaped `isFlammable`/`getFireSpreadSpeed`), `MapGenBase` over block ids (`generate(byte[])`, `recursiveGenerate(byte[])` overrides), `EnchantmentThorns` statics, ops/whitelist/login members by name, grass plants, chat events as strings, the old packet classes (open window, window click, respawn, place, entity moves, map chunk wire format), int health / `moveSpeed` / `getMaxHealth()` / `initCreature` for 1.5.2 and 1.4.7 mobs, absolute AI speeds converted to multipliers, and numeric widening in the constructor adaptation (a float argument no longer becomes a defaulted double).
Left: bundled copies of NEI/Waila API classes (unresolved), Scala library members, client rendering members (`MapItemRenderer`, `ThreadDownloadImageData`), texture classes of 1.4.7 (another agent), notch-named jars (BTW), `EntityAIDoorInteract`, Tinkers' `stats.player` (needs a runtime trace).

## Client survival round, second pass (2026-11-07)

Fixes from the first real-client round (screenshots in `docs/screenshots/old-versions/1.6.4`):

- **The Lion King Mod**: the client was kicked ~100 ms after joining with an empty reason. Its `IConnectionHandler.connectionReceived` returns `""` for accepted; `Players164` treated any non-null result as a refusal. Null or empty accepts now (as FML's `Strings.isNullOrEmpty`).
- **Mad Science**: 1.6.4's `IModelCustomLoader` is `loadInstance(String, URL)`; the 1.7.10 shim declares `loadInstance(ResourceLocation)`. The interface has both as defaults (the ResourceLocation form builds a URL that reads through the mods' resource lookup and calls the 1.6.4 form).
- **Applied Energistics rv14.finale3**: `ElectricTool.getManager` returns `this` as IC2's absent `IElectricItemManager`; the verifier loads the type for that check, so the class failed. `AbsentDependencyStubber` now stubs an `areturn` in a method whose return type is absent (unless it returns null or a parameter of that type).
- **Counter Craft**: calls `System.exit(0)` when more than five mods are listed (Forge, FML, mcp, Minecraft and bridged libraries count). The layer turned that into an exception that aborted the client setup half way (the "logo splash" bench was the mod's intro of a menu that never finished loading). `System.exit` from a legacy mod is now ignored (logged once per caller) and the mod carries on; the bench opens the mod's own menu (an online lobby: "Cloud offline").
- **Power of the Elements**: the ice, fire and air titans draw with their skins; the first round's "did not show" could not be reproduced.

## Status for 0.4.0 (2026-11-07)

What "supported" means for 1.6.4: the 1.6.4 jars of the corpus translate and the mod set loads in one game, a list of old-only content mods was played in a real client (superflat, performance mods and the Complementary shader pack), and the failures that remain are understood and named below.

- **Load rate** (102 rated 1.6.4 mods, site data): 38 basics work, 1 mostly playable (Tinkers' Construct), 10 partly working, 40 start untested (probe or library jars: Lycanites, ICBM, Binnie, NEI add-ons), 13 do not start.
- **Played in a client** (survival or creative with actions): Tinkers' Construct (Tool Station crafts a pickaxe, heart canister, tool mines), Ancient Warfare (research table), Goblins, Herobrine, QuazarCraft, InfusionCraft, Counter Craft (rifle fires), MythicalCraft, Galaxy Warfare, Power of the Elements, TragicMC, Sorcery, Kitchens, Mad Science, The Lion King, Dimensional Warps (all three dimensions).
- **Fixed in this pass:** the server object is available while mods load (Crafting Dead read the MOTD in postInit); the comparator statics of `Block`; a jar with obfuscated names and no declared version is taken for 1.6.4 by its obfuscation fingerprint (Advanced Survival); zips with the whole mod in one root folder or with the real mod in a "Mods Folder" inner archive are unpacked (Advanced Survival, Goblins textures); a GuiScreen that keeps its own container and draws its own slots (Tinkers' Construct tool station and part builder) is hosted with its own drawing and clicks.
- **Known not to work:** Forestry, Witchery, Ender IO, Mystcraft and Thaumcraft Extras (see their site notes); Crafting Dead (its bundled Netty kqueue classes are dropped for the game's Netty); OpenBlocks network handler; mods that need a library or sibling mod that is not in the download (Utility Chest, Minor Alchemy, Ex Aliquo, TuxWeapons); Tinkers' Steelworks (built for another Tinkers' Construct build); two mods that claim the same dimension id (Mega RPG with Dimensional Warps) fail as in Forge. Online features (Counter Craft lobby) have no server.
- **Open:** the Goblins village spawner freeze seen in one server self test did not repeat in a client; the Tinkers' smeltery, casting and the other stations, the Ancient Warfare gates and civic blocks, and shader-pack behavior in the Dimensional Warps dimensions (pitch dark) were not checked.
