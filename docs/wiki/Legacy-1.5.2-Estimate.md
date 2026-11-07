# Estimate: legacy 1.5.2 Forge mods on NeoForge 1.21.1

Written 2026-11-06 after the first day of 1.5.2 work. A delta over the 1.6.4 layer ([estimate](Legacy-1.6.4-Estimate.md)): `LegacyVersion.V1_5_2` links against the 1.7.10 shim, shares the 1.6.4 id facade and
network/tick adapters, and chains its forward tables (`loader/.../forward/1.5.2`) over the 1.6.4 ones. Measured API usage: [Legacy-1.5.2-Corpus](Legacy-1.5.2-Corpus.md). Back to [Home](Home.md).

## Short answer

**About 1 to 1.5 more days of wall-clock time to a first usable 1.5.2 tier** (the server side of the corpus mods loading, a few content mods playable in a client), on top of the 1.6.4 work it reuses. The 1.5.2 specific parts
(`EntityLiving` split, string textures, zips, version detection, liquids) are done on the server side; what is left is mostly shared with 1.6.4 (bridge methods for overridden id-signature methods, the Immibis library,
the liquid API's link to fluids) and the client side.

## What 1.5.2 adds to the 1.6.4 work

| Area | Work | State |
|---|---|---|
| `EntityLiving` is the base of all living entities | type uses become `EntityLivingBase`, real `EntityLiving` kept for `extends`/`super`, casts for members only `EntityLiving` has | done (`Living152ClassRemapper`, `LivingCasts152`); cleared the verify errors of 6 mods |
| String-path textures | `RenderEngine` is the texture manager, `bindTexture(String)` resolved by `Textures152`; `Render.loadTexture`, `bindTextureByName`; `Icon` registration is the 1.6.4 path | server side done; **no client run yet** (`/mods/<id>/` files are not yet copied to `assets/<id>/`) |
| Mods distributed as zips | the locator accepts zips that contain classes | done (19 of 67 files) |
| Version detection | 1.5.2 vs 1.6.4 by 1.5.2-only API, declared version, `modlist` mcmod.info, file name | done |
| `@PreInit/@Init/@PostInit` only | already accepted | nothing to do |
| Forge classes in 1.5 places | `ISidedInventory` (start/size per side) as an interface with the 1.7.10 methods derived; `Configuration$UnicodeInputStreamReader` | done |
| Liquid API | `LiquidStack`, `LiquidDictionary`, `LiquidContainerRegistry`, tanks as plain classes in the 1.6.4 shim (same API in 1.6.4) | links and works in memory; not connected to NeoForge fluid capabilities yet |
| Smaller | pools, `RelaunchClassLoader` / `FMLRelauncher` stand-ins, `setBlockHarvestLevel`, `sendChatToPlayer(String)`, `EnumHelper.addToolMaterial` with an int damage, `Minecraft.getMinecraftDir` | done |
| Pack mods, other libraries | `mods.immibis.core` (11 corpus mods), `bspkrsCore`, ComputerCraft turtle API | Immibis Core's id allocator opens a fatal error screen and sleeps forever; open |

## Measured (corpus of 67 files, 2026-11-06)

- Offline link check (`checkLegacyMod`): synthesized members 3,251 to 1,801, synthesized classes 561 to 226, unresolved references 26 to 21 (CodeChickenCore/NEI library calls, Scala).
- Server load, newest build of each mod, Immibis family left out (see above): 41 legacy mods discovered, **29 load cleanly** (BuildCraft Core/Transport/Silicon, Twilight Forest, BiblioCraft, Carpenter's Blocks, ComputerCraft,
  CustomNPCs, Grimoire of Gaia, Hats, iChunUtil, Dragon Mounts, Enchanting Plus, More Swords, Dirt Tools, Resource Engineering, ...). 12 fail: Mystcraft (duplicate symbol registration), Forestry (liquid container registered
  before its liquid), Binnie (needs Forestry's templates), AE1 (needs BuildCraft proxy), TreeCapitator and Backpacks (a missing library jar), Translocator/Ender Storage (reflection on `minecraftDir`), ForgeMultipart
  (mixin compiler, tiny packet handler), Factorization (it is a 1.6.4 mod), Sprintable (a client-only mod on a server), Advanced Repulsion Systems (needs Immibis Core).
- With the first build, 27 of 51 failed; the `EntityLiving` split, zips, detection and the shim additions took it to 12 of 41 plus the Immibis cluster.
- Corpus artefact: two builds of one mod id make FML skip both; load a single build of each.
- CodeChickenCore/NEI refuse to start on "Minecraft 1.7.10" (they check the version string); a per-version `Loader.getMCVersionString` would fix it.

## Measured again (2026-11-07, one build per mod, 50 files, server load)

- 55 legacy mods discovered (Immibis Core and its family included: they load now), **51 load, 4 fail**: ForgeMultipart (its Scala mixin compiler needs class bytes the loader cannot hand it), Sprintable and Sprint on CTRL (client-only mods, they touch client classes on a server), Block Helper (a bundled Waila plugin names `BlockComparator`, a block 1.4.7 did not have).
- Fixed on the way: ModLoader default-package classes (bspkrsCore, TreeCapitator, Sprint on CTRL) were moved to one name and referenced by another; `BaseMod` was only resolved for 1.4.x jars; the 1.5.2 AI task constructors (`EntityLiving` owner, `float` speeds and target distance: every Twilight Forest, CustomNPCs and Grimoire of Gaia mob had inert targeting before, 21 entity tick errors per run, now 0); fields typed `EntityLiving` in the mod but narrower in 1.7.10 (`EntityAITarget.taskOwner`) read a synthesized field that was always null; the home area on `EntityLiving` (flying mobs); `cpw.mods.fml.relauncher.IClassTransformer` and `RelaunchClassLoader` for 1.5.2 coremods (CodeChickenCore, NEI, Immibis Microblocks and TreeCapitator transformers all start now).
- Open: ForgeMultipart; client-only mods on a dedicated server are reported as failed (they could be skipped quietly); Mystcraft age dimensions are created by the self test without age data.

## Next, in order

1. Immibis Core (unblocks 11 mods): find why `IDAllocator.allocate` ends in its error screen; make mod error screens log instead of sleeping forever.
2. The 1.6.4 bridge-method mechanism (`breakBlock`, `onNeighborBlockChange` with int ids) when it lands; the liquid API to fluid capabilities.
3. Copy `mods/<id>/` resources to `assets/<id>/`, then a real client run of the textures path.
4. Per-version Minecraft version string for CodeChicken's version checks.

Wall-clock at the measured pace: 1 to 1.5 days for 1 to 3, plus the client verification.

## Better Than Wolves (jar mod): why it does not load (checked 2026-11-07, BTW 4 B0000003 for 1.5.2)

BTW is not a Forge mod; it is a patched Minecraft. The zip holds `MINECRAFT-JAR/` (1,756 entries) and `MINECRAFT_SERVER-JAR/` (905), meant to be copied over `minecraft.jar` / the server jar, plus `BTWConfig.txt`. There is no `mcmod.info`, no `@Mod`, no `mod_*` `BaseMod` class; the loader's "not a legacy Forge mod" is correct.

- Client side: 1,018 classes. 749 are new `FC*` classes (blocks, items, entities, tile entities) whose superclasses are notch names (`apa` = Block 69 times, `wk` = Item 45, `xn`, `om`...). The other 269 (`aab`, `ah`, `ahc`...) are **replaced vanilla classes** with BTW's changes compiled in (hunger, block hardness, mob AI, the world generator, the crafting manager, `Block`/`Item` registration tables), plus ~670 replaced textures.
- To load it the layer would have to (1) map notch names with the 1.5.2 table (exists), and (2) let those 269 modified classes **replace** the shim's vanilla classes. The shim's vanilla classes are hand-written facades over the modern game, not the 1.5.2 bytecode, so a replaced `Block` or `World` has fields and internals (`blocksList[]`, chunk providers, the entity tracker) that nothing implements. That is a different architecture (running real 1.5.2 vanilla code), not an extension of the shim. It stays out of scope, as for every jar mod; the BTW add-ons (which patch the same classes) are out too.
- What would work instead: a BTW-inspired Forge mod rewrite is a different mod. Nothing of BTW renders here, so there is no screenshot.

RedPower 2 has no 1.5.2 or 1.6.4 build in the corpus (the latest public builds are 2.0pr5/pr6 for 1.4.7; the modules are in `test-mods-147` and have screenshots under `docs/screenshots/old-versions/1.4.7`).

## Client survival round, second pass (2026-11-07)

Fixes from the first real-client round (screenshots in `docs/screenshots/old-versions/1.5.2`):

- **Twilight Forest**: `new RenderLiving(model, shadow)` is concrete in 1.5.2; the shim's was abstract (InstantiationError during init). It is concrete now and `Render.bindEntityTexture` binds the entity's string texture (`EntityLiving.texture` / `getTexture()`, default compared by identity) before the renderer's default, so the boar, sheep, bunny and penguin draw with their own models and skins.
- **Applied Energistics 1**: `LanguageRegistry.loadLocalization("/mods/appeng/lang/en_US.properties", ...)` read the path from the shim's own class loader; it goes through `ResourceCompat` (the mod's jar) now, so `ChatMsg.NoController` and the GUI strings translate.
- **RedLogic**: needs Immibis Microblocks beside Immibis Core (a test-set omission). It reads `BlockRedstoneWire.class.getDeclaredFields()[0]` and asserts a boolean: the shim's wire now declares `wiresProvidePower` first; an `AssertionError` from a legacy tile entity constructor no longer escapes `LegacyBlockEntity.legacy()` (it crashed the server).
- **Resource Engineering**: no layer defect found. The composter does draw the mod's background (it reuses `sifterGUI.png`; its foreground method has a wrong signature, no title in the real game either). The Thor chest's mismatch is the mod's: `GuiHandler.getServerGuiElement` builds a `ContainerBackpack` (53 slots), the client a `ContainerThorChest` (117).

## Survival round 3 notes (2026-11-07)

- Registry names: Immibis Core registers every Immibis mod's blocks and items from its own code, when no mod is the active owner, so they landed in the `minecraft:` namespace (`minecraft:redlogic.gates`). They are now owned by the mod whose package holds the class (`redlogic:redlogic.gates`); `BlockBridge.ownerByPackage`. This changes the saved ids of existing test worlds (acceptable at this stage; no migration). Raw path names such as `tile.x` come from the mod's own registration names (the 1.6.4 facade strips `tile.`); Applied Energistics 1 registers its blocks as `block900_2`-style ids because it hands the layer a null name, left as is since renaming would change world data for no functional gain.
- Resource domains with a slash (`/mods/immibis/redlogic/textures/...`) are now one namespace (`immibis_redlogic`), as the runtime derives it from the icon name.
- Open: Immibis' own lang files (`item:wire.red-alloy<TAB>name` keys) are not read, so RedLogic items show raw keys; RedLogic wires do not conduct and draw flat; Advanced Machines and Advanced Repulsion Systems need IC2.

## Status for 0.4.0 (2026-11-07)

What "supported" means here: a 1.5.2 mod set of the 55-mod corpus loads in a real client and on a dedicated server, the mods that add content register their blocks, items and entities, and the basics of the content mods that were played (placing blocks, items, opening GUIs, machines) work. It does not mean every mod is complete; the per-mod ratings are on the compatibility site (`docs/data/mods.json`, source "client").

Fixed in the last round:

- Client-only mods (Sprintable, Sprint on CTRL) on a dedicated server are skipped quietly (info log, not counted as failed) when they touch client classes; `LegacyLifecycle.touchedClientOnServer`.
- ForgeMultipart loads: the FML class loader view handed out bytes of JDK classes, so its Scala mixin compiler walked up to `java.lang.Object` and asked for the super class of Object. FML's loader returns no bytes for them; `findClass` delegates to the real loader.
- Block Helper loads: 1.5-era ModLoader jars name `net.minecraft.src.BlockComparator` (flat names added for the classes 1.5 introduced, `forward/1.4.7/flat-names.tsv`), the 1.6.4 table maps `BlockComparator`/`BlockRedstoneLogic` to the 1.7.10 names, and `Block.redstoneComparatorIdle/Active` exist.
- Forestry's tamper check (`Runtime.halt` when its jar has no signature) no longer ends the game: `Runtime.halt/exit` from a legacy mod is ignored like `System.exit`.
- Mystcraft's linkbook entity (data watcher slot 7, which the 1.7.10 living entity also registers) no longer fails its construction.
- Forestry's blocks drew missing-texture pink: its texture manager reads `RenderEngine.textureMapBlocks`/`textureMapItems` (now the shim's atlases, `TextureManager.mcc$get$textureMapBlocks`), and its signature check (`Runtime.halt`) ended the game on start.
- Patched-game jar mods (Better Than Wolves and its add-ons: a zip of `MINECRAFT-JAR/` files meant to be copied over `minecraft.jar`) are listed in the report under "Files that are not Forge mods" as "not supported: patched-game jar mod" and logged at warn level; the site rates Better Than Wolves "fails" with the same plain words.

Real-client survival pass of this round (details and screenshots in `docs/screenshots/old-versions/README.md`): BuildCraft auto workbench screen, ComputerCraft terminal, BiblioCraft desk screen, Mystcraft writing desk screen, Forestry blocks and alveary, Carpenter's Blocks, a Twilight Forest fight (the mobs attacked and killed the player) work. RedLogic wires do conduct (a lever beside the wire powers it and lights a lamp: RedLogic only connects to power sources, a redstone block next to a wire does nothing, as in 1.5.2); the flat grey pad around the wire stripe is not solved (the quads and the tile entity are right; the white region is not from the wire texture, which is transparent there). The "drew nothing for WireBlock" log line comes from the first meshing before the wire's side mask has arrived; later meshes draw 5 quads.

Second real-client pass (crafting-grid recipes, machines placed and opened, one mechanic each; ratings in `docs/data/mods.json`, screenshots listed in `docs/screenshots/old-versions/README.md`). Totals now: 19 work, 14 partly work, 18 start but are untested, 3 fail (of 54 rated; the earlier 81 counted the offline probe rows of the same mods twice).

- Crafted from the grid and placed or opened: Backpacks (Quantum Chest, backpack screen, stacks stored), Applied Energistics 1 (ME cables, Grind Stone screen), Ender Storage (Ender Chest, items stored), Chicken Chunks (loader screen, radius), Dimensional Anchors, InfiniTubes (Teleposer screen), Liquid XP (collector), Forestry (apiary screen with a bee in the queen slot), Mystcraft (writing desk), RedLogic (wire), BiblioCraft (desk; typesetting table placed), Binnie's Extra Bees (Genetic Machine placed), Grimoire of Gaia (sword), Translocator (recipe only), Dragon Mounts and CustomNPCs (entities).
- Fixed generically: the crafting event hands legacy `ICraftingHandler`s the real grid (Grimoire of Gaia threw on every craft); `TileEntity.onInventoryChanged` exists again (Applied Energistics' `IInventoryNotifyable` is satisfied by the superclass in 1.5.2: shift-clicking ore into the Grind Stone ended the server with an `AbstractMethodError`); a `/mods/<package>/...` texture path whose first segment is not the mod id falls back to the class path file (the Dimensional Anchor screen background); transformers a mod registers itself in a static initialiser (`Launch.classLoader.registerTransformer` in `ImmibisCore`: the trait injection that gives Immibis tiles their inventories) are replayed at translation time (the constant registrations only: the initialiser itself touches the game) and the stand-in coremod loader serves empty interfaces for unknown `net.minecraft` classes; a failing lifecycle handler (`LinkageError`, a mod needing ComputerCraft's turtle API) no longer ends the server.
- Findings that were test artefacts: the Forestry apiary screen opens (the earlier note was a click that did not aim); the Ender Chest opens when aimed at (its ray tracer needs the player's look; clicking a block position gets no screen); NEI is replaced by its bridge here, so its server tick failure does not occur in this set.

Known not to work or not verified:

- Screens that do not open or draw: Binnie's Extra Bees Genetic Machine (no screen, no log line), CustomNPCs' wand (no editing screen), the BiblioCraft typesetting table opens none on a plain right-click (by the mod's design), the Mystcraft desk lays its panels out wrongly. Translocators cannot be placed (Forge Multipart's Scala references do not all link). Dragon Mounts' spawn egg spawns nothing (`/summon` works). The RedLogic wire still draws a flat grey pad. The Dimensional Anchor block icon (`immibis/chunkloader:chunkloader`) and Extra Bees' capital-letter texture files (`GeneticMachine.png`) are missing textures.
- The Ender Chest item renderer binds `textures/misc/tunnel.png` and `particlefield.png`, which 1.21.1 lacks.

- Better Than Wolves and other patched-`minecraft.jar` mods (see above). Advanced Machines / Advanced Repulsion Systems features that need IC2 (IC2 has no 1.5.2 build to test with).
- Immibis' mods share one block id file (`immibis.cfg`); when another mod uses the same numeric id (Resource Engineering's `Loaded Sand` = 410, ComputerCraft's disk item 4001) Immibis Core's allocator aborts, as it does in the real game with an error screen, and every Immibis block after the conflict is lost. This is a configuration conflict of the mod set, not a layer defect; change the id in `immibis.cfg`.
- NEI's server handler (`NEIServerConfig.loadSavedConfig`) failed every tick on a null world save location (`CommonUtils.getWorldBaseSaveLocation` reads the chunk loader's folder); in the 2026-11-07 set NEI is bridged (not loaded), so this was not re-checked.
- Mystcraft age dimensions generate with errors without age data (self test).
- The server self test reports "Listener already listening" for Mystcraft, Ender Storage pouch and similar GUIs opened by a fake player; real-client behaviour is not yet verified.
