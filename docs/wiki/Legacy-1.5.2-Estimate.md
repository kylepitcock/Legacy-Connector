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
