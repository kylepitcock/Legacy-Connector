# Legacy 1.20.1: why the "fails" jars fail

Analysis of the 231 jars the offline link probe rates "fails" in the ATM 9 run of 2026-11-23 (506 jars; 242 link, 231 unresolved, 26 replaced by a native library build, 7 not translated). Back to [Home](Home.md); the run itself is in [Legacy-1.20.1-ATM9-Run](Legacy-1.20.1-ATM9-Run.md). Script: `tools/dev/classify_1201_failures.py` (reads `detail.tsv`/`jars.json` of `tools/compat/atm9_run.py`, the mods folder, and Modrinth; caches in its `--out` dir). Per-jar result: `tools/dev/agent-tasks/1.20.1-failure-classes.tsv` (jar, class, detail).

Method limits, stated up front: no server runs were done (less than 6 GB free RAM when this was written), so "soft" below is a structural judgement, not a measured load. The Modrinth match is by SHA-1 of the jar (151 of 170 positive answers) or by slug equal to the mod id (19); CurseForge-only mods are not seen, so the native counts are a lower bound.

## 1. Primary cause per failing jar

| class | meaning | jars |
|---|---|---|
| a | the mod has a native NeoForge 1.21.1 release on Modrinth (nobody should load the 1.20.1 jar) | **151** (132 by hash, 19 by slug) |
| b | rendering engine / shader / performance mod (Oculus, Mekalus) | 2 |
| c | unresolved references only into another mod's API | 22 |
| d1 | Minecraft/Forge symbols, 1 to 5 distinct | 33 |
| d2 | Minecraft/Forge symbols, more than 5 | 23 |
| e | other (Fabric-origin names, nothing else) | 0 |

Order of precedence is a, b, then c/d. Engine mods that also have a native build (Embeddium family, Entity Culling) are counted in a; the site already hides client-only mods (`hidden_by_mc` 1.20.1: 13 of the failing jars are on it, 5 of them outside a: Oculus, Mekalus, Just Enough Mekanism Multiblocks, PackMenu, BlockUI). "own" symbols (a member of the mod's own class that overrides or reads a vanilla member whose signature changed, e.g. `renderToBuffer`, `handleInsidePortal`) are counted as Minecraft symbols. Examples of a: Apotheosis, Draconic Evolution, Ender IO, Cyclic, Botany Pots, Aquaculture, Waystones, Create Additions, Pipez, Easy Villagers, Aether, EvilCraft, Zeta. Of the 242 jars that link, 149 also have a native build (so 300 of 506 jars have one, 326 with the 26 library replacements).

Of the 80 jars that are not in a, named: c (22): IntegratedTerminals, REI/EMI-only (JEMM), The One Probe (RFTools Builder/Control/Power/Storage/Utility, Deep Resonance), Balm (Crafting Tweaks, Farming/Cooking for Blockheads), CoFH Core (Thermal Foundation/Locomotion), FTB Essentials (LuckPerms API), Jellyfishing (Blueprint). d1 (33): bdlib, CoFH Core, Reliquary, Eidolon Repraised, MCDoom, Mythic Botany, PackMenu, Sophisticated Backpacks/Storage, Structurize, Crash Utilities. d2 (23): Twilight Forest, Tinkers' Construct, Botania, Alex's Caves/Mobs, Ice and Fire, Rats, Blue Skies, Tombstone, ExtendedAE, Ars Elemental.

## 2. Soft or hard

`SoftLink1201` (default on) replaces every reference 1.21.1 cannot link by a stand-in that throws only when executed, so a jar with unresolved references can load. The probe still counts them all (`Gap1201` adds the soft-linked symbols back into the report). The script finds, per unresolved symbol, the classes of the jar that hold it (owner class plus member descriptor in the constant pool), walks class references from the `@Mod` and mixin classes, and calls a symbol hard when a holder is reachable and its class name is not an integration class (`integration|compat|jei|rei|emi|plugin|waila|jade|probe|kubejs|crafttweaker|curios|patchouli`), and, for another mod's API, only when `mods.toml` lists that mod as mandatory. Symbols inside mixin classes are always hard (conservative: they are soft-linked since issue 302 but fail on use of the handler). Reachability by constant-pool references over-approximates; the integration-name heuristic is crude. The result is an estimate in both directions.

| class | jars | soft (no reachable non-integration holder) | hard |
|---|---|---|---|
| a (native exists) | 151 | 41 | 110 |
| b | 2 | 0 | 2 |
| c | 22 | 13 | 9 |
| d1 | 33 | 7 | 26 |
| d2 | 23 | 2 | 21 |
| c+d, without a/b | 78 | **22** | **56** |

The soft ones are almost all optional-integration cases: The One Probe in RFTools and Deep Resonance, REI/EMI in Integrated Terminals, CraftTweaker/Game Stages in Structure Compass, LuckPerms/Agricraft in FTB Essentials and Ultimine, Curios in Blood Magic. The hard remainder: 28 jars are blocked only by Minecraft/Forge symbols and each has its own few (18 of them have at most three: bdlib `ResourceLocation.<init>(String)`, Reliquary `ThrowableProjectile.getGravity`, Eidolon `BrewedPotionTrigger.trigger`, Redstone Arsenal `handleInsidePortal`, CoFH Core `Inventory.hurtArmor`, MCDoom `VillagerTrades$ItemsAndEmeraldsToItems.<init>`, Mythic Botany `WanderingTrader.lootTable`, Corail Woodcutter `SingleItemMaker`, Little Logistics `level` field of a path node processor, Sophisticated Storage `getOpenCount`, XyCraft Machines `getAllEnchantments`, Integrated Dynamics `getListeners`); the other 28 also have another mod's API or mixin symbols (Citadel, Balm, Farmer's Delight, Ars Nouveau...), and a few are big ports (Tinkers' Construct 406 Minecraft symbols, Alex's Caves 72, Alex's Mobs 51, Ice and Fire 37, Rats 32).

A real check on the sample was not possible here; to do it run the 22 soft jars in `run-subtest/mods/1.20.1/` one at a time with their mandatory dependencies, VERIFY_PORT_BASE=34490, and compare `mods_loaded_1201`.

## 3. Dependency APIs, ranked by non-native jars they hard-block

| dependency API | jars hard-blocked | sole blocker of |
|---|---|---|
| Citadel | 4 (Ice and Fire, Alex's Mobs, Alex's Caves, Domestication Innovation) | 0 |
| Balm | 3 (Crafting Tweaks, Farming/Cooking for Blockheads) | 3 |
| Farmer's Delight (`vectorwing/farmersdelight/common`) | 3 (Delightful, Nether's Delight, Twilight Delight) | 0 |
| JDK `java.xml` (`org/w3c/dom`: a probe class path gap, not a mod) | 3 (CyclopsCore, BlockUI, Mahou Tsukai) | 1 |
| ANTLR | 3 (Alex's Mobs, Domestication Innovation, Mahou Tsukai) | 0 |
| Ars Nouveau | 2 (Ars Elemental, All the Arcanist Gear) | 1 |
| Neapolitan (Blueprint) | 2 | 0 |
| Blueprint | 1 (+ Jellyfishing alone) | 1 |
| CoFH Core | 1 (Thermal Locomotion) | 1 |
| Create / Mantle | 1 each (Steam 'n' Rails / Tinkers) | 0 |

Over all 231 jars the most referenced other-mod APIs are The One Probe (38 jars), REI (28), EMI (23), Create (17), CraftTweaker (11), Game Stages, Farmer's Delight, Jade, Ars Nouveau (8 each) and Resourceful libs, Curios, JourneyMap (7 each); nearly all are optional integrations (soft), so bridging them improves features, not loading. The dependency lever is small: no single dependency API unblocks more than 4 jars.

## 4. Levers (non-native hard jars, 56)

1. **Do not load the 1.20.1 jar when the native build exists: 151 failing jars (65%), and 149 linking ones.** `NativeLibraries1201` already does this for the 42 rows of `libraries.tsv`; make it general (same mod id installed natively, or the Modrinth project) and generate the list from this script's lookup.
2. **Rate soft links as loading: 22 more jars (28% of the 78 left)**, 41 more among the native ones. See section 5.
3. **Minecraft signature families**, which hit several jars at once: `renderToBuffer` model override with the 1.21 colour argument (5 hard jars), portal fields and methods (`isInsidePortal`, `portalTime`, `handleInsidePortal`: 4 hard, 10 over all 231), advancement triggers/predicates (`critereon`: 6 hard, 20 over all), recipe classes (8 hard, 36 over all), `PathNavigationRegion level` (4), removed enchantment classes (3 hard, 14 over all), packets (4 hard, 20), authlib (3). Fixing them as override/accessor bridges (not one symbol at a time) is worth 20 to 25 of the 56 hard jars; the top single symbol blocks only 2 jars.

## 5. Recommended rating change (propose only, nothing implemented)

Today `tools/compat/probe.py` (`run_gap1201`) writes `result: unresolved` for any jar with one missing symbol, and `status = fails` unless `links`; `Gap1201` re-adds the soft-linked symbols to the report, so the verdict ignores the layer's own soft link. A fairer rule, per jar:

1. `native`: a NeoForge 1.21.1 build exists (Modrinth hash/slug; plus `libraries.tsv`): own status, shown as "use the native version", excluded from the percentage.
2. engine/client-only mods the layer skips (Oculus, Rubidium...; `hidden_by_mc`): excluded.
3. `loads` when every unresolved symbol is soft: no holder class reachable from `@Mod` or mixin classes, or all holders are optional-integration classes of non-mandatory dependencies. Needs `Gap1201` to write the holder class and the soft kind into `detail.tsv` (the script here reconstructs it from the class files).
4. `fails` otherwise.

Effect on this probe (506 jars): today 242 loads (47.8%), 231 fails (45.6%). Published site data (older run, 507 entries): 137 loads (27%), 355 fails (70%). Under the rule: native 326 (300 jars plus the 26 library replacements) and 7 not translated and 2 engine mods are left out; of the remaining 171, loads = 93 (linking, no native) + 22 (soft) = **115 (67%)**, fails = **56 (33%)**. Even with a crude heuristic, the 1.20.1 card reads "326 native, 115 load, 56 fail" instead of "242 load, 231 fail".

## 6. The 15 most valuable next tasks

1. Generalise `NativeLibraries1201` to every mod installed natively at 1.21.1 and generate its table from the Modrinth lookup (151+149+26 jars).
2. Probe/site: add the `native` status and exclude it from the card percentage (`probe.py`, `docs/assets/site.js`).
3. `Gap1201`: write holder class, soft kind and mandatory-dependency flag to `detail.tsv`; rate soft-only jars `loads` (22 now).
4. Real server runs of the 22 soft jars (and 10 of the 41 soft native ones) to confirm the structural verdict; log which stand-ins fire.
5. Override bridge for `renderToBuffer(PoseStack, VertexConsumer, int, int, float, float, float, float)` to the 1.21 signature with a colour (Rats, Ice and Fire, Alex's Caves, Domestication Innovation, Alex's Mobs).
6. Portal API: `Entity.isInsidePortal`/`portalTime` accessors and `handleInsidePortal` override bridge (Redstone Arsenal, Alex's Mobs, Structure Gel, Mahou Tsukai).
7. `critereon` trigger/predicate constructors and `fromJson`/`serializeToJson` (Eidolon, Ars Elemental, Twilight Delight, Alex's Mobs, Botania; 20 jars overall).
8. Recipe family: `SingleItemRecipe$Serializer$SingleItemMaker`, `Recipe`/`Ingredient` serialiser shapes (Corail Woodcutter, Refurbished Furniture, Sophisticated Backpacks; 36 jars overall).
9. Removed vanilla enchantment classes (`FireAspectEnchantment`, `KnockbackEnchantment`, `TridentLoyaltyEnchantment`) as stand-in classes (Blue Skies, Trials; 14 jars overall).
10. `PathNavigationRegion level` and `NodeEvaluator` field access (Rats, Ice and Fire, Little Logistics, Alex's Mobs).
11. Packet constructors (`ClientboundAddEntityPacket`, `ClientboundRespawnPacket`, `ClientboundCustomPayloadPacket`, `FriendlyByteBuf.writeRegistryIdUnsafe`): FluxNetworks, Tombstone, Minecolonies, Botania.
12. Balm API facade for Crafting Tweaks and the two Blockheads mods (3 jars, sole blocker).
13. Citadel: decide between a bridge and the side-by-side library for Ice and Fire / Alex's mods (4 jars; they also need item 5).
14. Probe class path: add `java.xml` (`org/w3c/dom`) and the layer's own `org/kylepitcock/legacyconnector` classes to the resolution targets; removes false gaps in CyclopsCore, BlockUI, Mahou Tsukai.
15. Farmer's Delight and Neapolitan package moves (Delightful, Nether's Delight, Twilight Delight): remap table in `forward/1.20.1`.
