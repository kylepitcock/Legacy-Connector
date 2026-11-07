# Open issue triage 2026-11-06 (#103-#152)

Status: a = fixed on master / awaiting release or reporter retest, b = waiting on reporter, c = actionable, d = duplicate / low value / unreproducible / tracker.
Shim paths: 1.7.10 `src/main/java/legacyconnector/v1_7_10`, 1.12.2 `src/main/java/legacyconnector/v1_12_2`, bridges `src/main/java/org/kylepitcock/legacyconnector/<ver>/bridge`.
Based on issue threads and `git log` (commit hashes cited).

| # | Mod | St | Reason | Where to start |
|---|-----|----|--------|----------------|
| 103 | Legends 8.6.2 (1.7.10) | c | Symbiote chamber mutate/eject and generator effect broken; relies on its coremod transformers (EntityLivingBase, FoodStats, RenderPlayer, EffectRenderer), which do not run | Coremod-hook bridging as done for Fisk (b2008563, dafde678); loader coremod handling |
| 105 | Traincraft CE (1.7.10) | c | Most fixed (display lists, GUIs, NEI hang 1f3cb2b6). Open: non-locomotive car GUIs (re-reported on 0.3.8, retest on 0.3.10 unclear) and trains derail on diagonal TC tracks | Traincraft track/rolling stock movement on diagonal rails; car GUI container registration |
| 106 | Starminer (1.7.10) | c | Gravity from coremod; overworld planets, space sky, G-flower orientation, Stardirt tint open | Same coremod-bridge area as #103; WorldProvider shim for planet gen |
| 107 | Witchery (1.7.10) | a | Nearly all points fixed for 0.3.14; owl/toad taming reply and altar icon were "could not reproduce" | Wait for retest after release |
| 110 | SRParasites 1.10.9 (1.12.2) | b | Fixes in 0.3.8; reporter to retest incomplete form with Sodium/EntityCulling, particles. Rupter AI is mod design | Wait for retest |
| 111 | Chocolate Quest (1.7.10) | a | EnchantmentProtection VerifyError fixed in 8d73863e | Needs reporter confirmation, then close |
| 112 | ProjectE (1.7.10) | c (lowpriority) | Boots fully. Open: shears do not drop leaves/grass/dead bush as blocks, Gem boots step assist, orb sprite, EMC values, bag name | `IShearable` drop path in 1.7.10 shim; step height from armour |
| 113 | Kindred Legacy (1.12.2) | a | Recipes, GUI, crops fixed (0.3.18 confirmed). Remaining nitpicks: glitchy models of renamed tameables (Feywood Absol), Mystic Tech x-ray, natural spawning | Renamed-tameable render state (transparency layering) in 1.12.2 render shim |
| 114 | NTM-CE / CTM (1.12.2) | d | lowpriority; loads since 0.3.12, radiation/models/bombs broken; huge scope | If ever: `UnsafeHolder.fieldOffset` for 1.12 chunk storage, null armorArray/IntHashMap |
| 118 | Fisk Superheroes (1.7.10) | c | Flight/blade/cannon work; open: suit not rendered equipped, first-person hand, very glitchy flight, HUD placement | Fisk key probe (7db89ac8), suit model/pose application, client movement hook; shared with #132 |
| 121 | Scape and Spartan Parasites (1.12.2) | c | Recipes fixed; weapons untextured. Reporter says it is the S&RP/Spartan Weaponry compat add-on | Inspect reporter's latest.log (2026-11-05) for missing model/texture names; `Assets1122` item model generation |
| 124 | Roguelike Dungeons (1.7.10) | a | Generation, enchants, spawners, potions fixed (ee9ef664, 61ec8408, 57df2391, 0f226c17); items confirmed correct | Ask reporter about remaining empty spawners/chests, then close |
| 125 | Thaumcraft 6 (1.12.2) | c (lowpriority) | Crystals/gauntlet fixed. Open: Arcane Workbench/Pedestal/Focal Manipulator GUIs, beds, crystal collision, Thaumonomicon textures and lang keys | 1.12.2 GUI handler (`openGui`/IGuiHandler) for TC6 containers; lang loading in `Assets1122` |
| 128 | Portal Gun + iChunUtil (1.7.10) | b | Asked for latest.log; authlib redirect 4c007a65 may already help | Wait for log, retest next release |
| 131 | Galacticraft 1.12.2 | c | Planet travel fails: mod does setDead then sets entity.world/setWorld by hand; modern player already removed. GalaxySpace mixins need #137 | Entity world change/teleport in 1.12.2 shim (`Entity.world` setter, `EntityPlayerMP.setWorld`, dimension transfer) |
| 132 | Fisk (1.7.10) | c | Iron Man blade pale; recipes katana/trick_arrow_2/quiver do not match own ingredients, desert_eagle assembles empty stack; NPE on null stack in hasTagCompound | Custom recipe matching in `RecipeBridge`; null ItemStack NBT handling |
| 133 | AbyssalCraft 1.12.2 | d | Untested-features tracker (rated Playable) | Keep as test plan |
| 134 | The Midnight 1.12.2 | d | Untested-features tracker | Keep as test plan |
| 135 | ChromatiCraft (1.7.10) | c | Lexicon item does nothing server side; projectile entities spawned without constructor state NPE on tick/save | `EntityList.createEntityFromNBT` / entity creation path |
| 136 | ElectriCraft/ReactorCraft (1.7.10) | c | Hazmat pig still damaged; jet-engine to wire line delivers nothing | Radiation damage vs `hasHazmatSuit` check; `ForeignEnergy1710` / power adapters |
| 137 | MixinBooter mods (1.12.2) | c | Mixins in translated 1.12.2 mods never applied; blocks GalaxySpace, TF2 Stuff (#145) | Loader: MixinBooter plugin construction, apply only to translated legacy targets (guard in 7b4a75c1) |
| 138 | RedstoneControl + CD4017BE_lib (1.12.2) | c | Fails to load together; crash report and latest.log attached 2026-11-04, not yet looked at | Read attached crash-2026-11-04_11.34.51-fml.txt |
| 139 | ReactorCraft thorium_7 (1.7.10) | c | Client kicked: server registers `reactorcraft_block_thorium_7` (hardcoded meta 7 in getSubBlocks), client does not | `ItemBridge.discoverMetas` + `BlockFlattening1710`; diff `.legacyconnector/registry-keys.txt` (7aa856d7) |
| 142 | Thaumcraft 4 silverwood (1.7.10) | d | Not reproducible; only reporter's old world, reporter ready to close | Close |
| 143 | Ars Magica 2 (1.7.10) | c | Compendium chapters empty ("Unable to update the compendium", MC Version null, ~20 s postInit), GuiAchievement stub, ender boots gravity | Compendium load/version check path; `GuiAchievement.displayAchievement` |
| 144 | Thaumcraft 4 (1.7.10) | b | Baubles crash fixed 0.3.11; thaumometer on non-physical things, eye ritual spawn, robe colours: owner asked twice for latest.log | Wait for log; overlaps #152 |
| 145 | TF2 Stuff Reloaded (1.12.2) | b | No game log; depends on mixins (#137) and stubbed APIs | Wait for latest.log; blocked by #137 |
| 147 | HuajiAge Infinite Galaxy (1.12.2) | a | Fixed on master (4823ef3d), unreleased, untested on real jar | Retest after release |
| 148 | Electroblob's Wizardry (1.12.2) | a | Fixed on master (4823ef3d), unreleased | Retest after release |
| 150 | NovaCraft (1.7.10) | a | Brewing stand ctor, enchantment table, beacon, End decorator shims added (fa439b4d, b2e6ea04); synthesized classes still lack behaviour | Retest; GUIs may need real container logic |
| 151 | Fisk Superheroes (1.7.10) | d | Duplicate of #118/#132; extras: HeroPack textures missing, quiver GUI | Fold into #118; hero pack loading, quiver container |
| 152 | Thaumcraft 4 (1.7.10) | c | Aura nodes: no hitbox, cannot be drained by wands, node-in-jar fails, shaders broken; ores not glowing, no sounds; wand right-click on multiblocks (Infernal Furnace) fails; zombies fixed (056e9a6b, bcac087c) | Node entity/TE selection box; wand `onItemUseFirst`/IWandable dispatch; sound event name mapping; ore glow render pass |
