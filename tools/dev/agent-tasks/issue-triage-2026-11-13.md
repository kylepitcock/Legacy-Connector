# Triage of open issues, 2026-11-13

86 open issues. Current release 0.4.14. Hashes come from `git log`; a fix is in 0.4.14 when the commit is after v0.4.13 (no v0.4.14 tag exists locally). Classes added to the starting list: `tracker`, `world-height`, `item-hooks`, `coremod-hooks`, `migration-1201`, `block-render`, `block-tick-update`. Modern-port column is from memory and unverified. Several issues have more than one row.

## 1. Classes by number of issues

| Class | Issues | Numbers | Versions | Generic fix and where |
|---|---|---|---|---|
| model-loader | 15 | #89, #125, #158, #162, #163, #164, #165, #166, #169, #170, #171, #172, #173, #174, #252 | 1.12.2 | One 1.12.2 bridge for ICustomModelLoader, ModelBakeEvent, state mappers and code-made sprites, probed offline by ResourceCheck (`src/main/java/org/kylepitcock/legacyconnector/v1_12_2/client/Models1122.java, bridge/StatesBridge1122.java, v1_12_2/Assets1122.java`) |
| item-render | 11 | #121, #132, #144, #145, #168, #184, #185, #193, #194, #197, #217 | 1.12.2, 1.4.7, 1.5.2, 1.5.2/1.6.4, 1.7.10 | Item renderers get damage/NBT; block items without icons use fallbacks; one per-stack model path (`src/main/java/org/kylepitcock/legacyconnector/v1_7_10/client/LegacyItemRenderer.java, v1_12_2/client/Models1122.java`) |
| load-failure | 6 | #92, #105, #111, #114, #202, #245 | 1.12.2, 1.7.10 | Shim the reflected vanilla internals/registries; extend checkLegacyMod probes (`loader/src/main/java/.../transform, shims v1_7_10 and v1_12_2`) |
| entity-ai-spawn | 5 | #75, #105, #113, #167, #178 | 1.12.2, 1.7.10 | Verify AI hooks, getCanSpawnHere and move controls through the entity bridge (`src/main/java/org/kylepitcock/legacyconnector/v1_7_10/bridge/EntityBridge.java, v1_12_2/bridge/EntityBridge1122.java, v1_12_2/entity/EntityHost1122.java`) |
| tracker | 4 | #64, #133, #135, #136 | 1.12.2, 1.7.10 | one-off, see rows (`-`) |
| entity-render | 4 | #107, #110, #154, #190 | 1.12.2, 1.6.4/1.7.10, 1.7.10 | one-off, see rows (`-`) |
| gui-sync | 4 | #125, #150, #181, #248 | 1.12.2, 1.4.7, 1.7.10 | Mirror runtime slot/container changes and progress-bar data into the modern menu (`src/main/java/org/kylepitcock/legacyconnector/v1_7_10/bridge/GuiBridge.java, v1_12_2/bridge/GuiBridge1122.java`) |
| migration-1201 | 4 | #198, #205, #218, #250 | 1.20.1 | Migration-table rules and soft-link policy for 1.20.1 signature changes (`loader/.../transform/SoftLink1201.java, Mc1201Tables.java, src/main/java/org/kylepitcock/legacyconnector/v1_20_1/migrate`) |
| world-height | 3 | #78, #82, #134 | 1.12.2, 1.7.10 | Handle legacy 0..255 heights against -64..320 once (clamps, deep pass) (`src/main/java/org/kylepitcock/legacyconnector/v1_12_2/bridge/WorldGenBridge1122.java, 1.7.10 worldgen shim`) |
| gui-draw | 3 | #82, #87, #143 | 1.12.2, 1.7.10 | Legacy screen GL state, fonts and draw helpers (`src/main/java/org/kylepitcock/legacyconnector/v1_7_10/client/ClientGui.java, LegacyContainerScreen.java`) |
| not-layer | 3 | #110, #177, #189 | 1.12.2, 1.12.2/1.7.10, 1.6.4 | one-off, see rows (`-`) |
| recipe-data | 3 | #113, #148, #183 | 1.12.2 | Lang, recipe and enchantment conversion at load (`loader transform; src/main/java/org/kylepitcock/legacyconnector/v1_12_2/bridge/RecipeBridge1122.java`) |
| needs-info | 3 | #128, #201, #219 | 1.20.1, 1.7.10 | one-off, see rows (`-`) |
| tesr-render-state | 3 | #152, #221, #234 | 1.7.10 | Compare each GL default with 1.7.10 once (cull face, depth, texgen) and emulate in one place (`src/main/java/org/kylepitcock/legacyconnector/v1_7_10/client/RenderContext.java`) |
| interop-modern-mod | 2 | #10, #156 | 1.21.1 pack, 1.7.10 | one-off, see rows (`-`) |
| item-hooks | 2 | #78, #112 | 1.7.10 | Call item/block hooks NeoForge dropped (onBlockStartBreak, ISpecialArmor, IShearable) (`src/main/java/org/kylepitcock/legacyconnector/v1_7_10/bridge/EventBridge1710.java`) |
| coremod-hooks | 2 | #103, #106 | 1.7.10 | Hook bridges (mixins on modern equivalents) per popular mod, under compat/ (`src/main/java/org/kylepitcock/legacyconnector/mixin/, loader/.../compat/`) |
| worldgen | 2 | #106, #124 | 1.7.10 | one-off, see rows (`-`) |
| player-ability | 2 | #118, #151 | 1.7.10 | Key/packet/capability plumbing for player abilities (`src/main/java/org/kylepitcock/legacyconnector/v1_7_10/bridge/EventBridge1710.java`) |
| module-conflict | 2 | #147, #203 | 1.12.2 | Strip/merge META-INF/services of jars whose packages move to the shared jar (`loader/.../SplitPackageResolver.java, BundledLibraries.java`) |
| block-render | 2 | #176, #192 | 1.6.4, 1.7.10 | Real light/biome data for ISBRH meshes; placeholder icon fallbacks (`src/main/java/org/kylepitcock/legacyconnector/v1_7_10/client/RenderContext.java`) |
| crash | 2 | #179, #215 | 1.12.2 | one-off, see rows (`-`) |
| performance | 2 | #188, #191 | 1.6.4, 1.7.10 | Profile per mod with spark; cache shim lookups (`docs/wiki/Performance.md`) |
| fluid | 2 | #247, #253 | 1.12.2 | Mod fluids as real fluids with buckets; water-material blocks hold water (`src/main/java/org/kylepitcock/legacyconnector/v1_12_2/bridge/FluidBridge1122.java`) |
| sound | 1 | #195 | 1.5.2 | one-off, see rows (`-`) |
| block-tick-update | 1 | #200 | 1.4.7 | one-off, see rows (`-`) |

## 2. Per-defect table

| # | Ver | Mod | Reported on | Symptom | Class | Probably fixed? | Next actor | Modern port? |
|---|---|---|---|---|---|---|---|---|
| 10 | 1.7.10 | OpenComputers + HBM | unknown | Modern OC cannot connect to legacy mod blocks (HBM cables) | interop-modern-mod | no | code / maintainer decision | OC has modern ports |
| 64 | 1.12.2 | tracker | 0.2.x | 1.12.2 status tracker | tracker | n/a (tracker) | maintainer | n/a |
| 75 | 1.7.10 | Thaumcraft 4 | 0.1.5 | Golem cores/tasks do nothing, black spots on golems; trunk follow | entity-ai-spawn | Trunk follow fixed 918e5d7be, ec87fe6e6 (0.3.13); golem tasks unknown | lead in-game check | no |
| 78 | 1.7.10 | Thaumcraft 4 | 0.1.5 | Thaumium/elemental tool abilities | item-hooks | yes: ec87fe6e6, 519cbd4f (0.3.11-0.3.13) | lead in-game check | no |
| 78 | 1.7.10 | Thaumcraft 4 | 0.1.5 | Lamps clamp height to y>=5 (bedrock floor); sanity checker untested | world-height | no | code work (generic hard-coded min height) | no |
| 82 | 1.7.10 | CustomOreGen | 0.2.1 | Hour-long world creation (fixed); veins crossing y=0 cut off | world-height | slow gen fixed (maintainer comment: 3367773, 4af8dd5, 824f903; confirmed on 0.3.10); y<0 deep pass partial | code work | no |
| 82 | 1.7.10 | CustomOreGen | 0.2.1 | In-world ore config GUI button differs | gui-draw | no | maintainer decision (low) | no |
| 87 | 1.12.2 | tracker leftover | 0.3.3 | Creative tab page arrow does nothing on page 2/4 | gui-draw | unknown | lead in-game check | n/a |
| 89 | 1.12.2 | Chisel | 0.3.3 | No connected textures (CTM bridge), plain models only | model-loader | partial: 22d66ecf7; real CTM open | code work | no |
| 92 | 1.12.2 | Vampirism | 0.3.3 | Init fails: no root skill, @ObjectHolder action null; no capabilities | load-failure | no | code work (needs debug log) | yes (Vampirism 1.21.1); lowpriority |
| 103 | 1.7.10 | Legends Mod | 0.3.3 | Symbiote chamber/legacies do nothing (coremod transformers do not run) | coremod-hooks | no | maintainer decision (hook bridge per mod) | no |
| 105 | 1.7.10 | Traincraft | 0.3.10 | World hangs with NEI recipes on | load-failure | yes: 1f3cb2b61 (0.3.12); VerifyError #244 fixed in 0.4.14 (30c7c4b89) | reporter retest | no |
| 105 | 1.7.10 | Traincraft | 0.3.10 | Trains stuck, derail on diagonal tracks, train car GUIs blank | entity-ai-spawn | no | code work | no |
| 106 | 1.7.10 | Starminer | 0.3.x | Gravity cores/walls, rotation, space dimension need coremod | coremod-hooks | no | maintainer decision | no |
| 106 | 1.7.10 | Starminer | 0.3.x | Overworld planets, Stardirt tint; squid invisible | worldgen | squid fixed a66483e7c; rest unknown | code work | no |
| 107 | 1.7.10 | Witchery | 0.3.x | Long list (oven facing, brews, skins, chest lid) | entity-render | mostly fixed in 0.3.14 (5435e4819 etc.); rest unknown | reporter retest | unknown |
| 110 | 1.12.2 | Scape and Run Parasites | 0.3.7 | Incomplete form drawn wrongly, cloud colour | entity-render | yes: c916aa9b2 (0.3.8) | reporter retest (asked 10-31) | no |
| 110 | 1.12.2 | Scape and Run Parasites | 0.3.7 | Rupters only attack player (mod design) | not-layer | n/a | close with explanation | no |
| 111 | 1.7.10 | Chocolate Quest | 0.3.6 | Mod disabled: VerifyError EnchantmentProtection | load-failure | yes: 8d73863ec (0.3.7) | CLOSED this pass | no |
| 112 | 1.7.10 | ProjectE | 0.3.6 | Postinit NPE (fixed); shears drop, gem boots step assist | item-hooks | load fixed; ISpecialArmor 429ab3feb; shears/step assist open | code work (low) | yes (ProjectE 1.21.1); lowpriority |
| 113 | 1.12.2 | Kindred Legacy | 0.4.12 | No natural spawning, riding animations, firework particles | entity-ai-spawn | no | code work | no |
| 113 | 1.12.2 | Kindred Legacy | 0.4.12 | Missing recipes, armor layer texture path wrong, x-ray blocks | recipe-data | no | code work (needs log) | no |
| 114 | 1.12.2 | HBM NTM-CE | 0.3.12 | Loads after invalid-package fix; radiation/bombs/models do not work | load-failure | load yes; runtime errors (UnsafeHolder, armorArray) open | code work | no; lowpriority |
| 118 | 1.7.10 | Fisk Superheroes | 0.3.x | Armor gray, partial suit, HUD position, flight/blade/cannon | player-ability | partial: a1a9da952 (0.3.23); HUD/abilities unknown | reporter retest (asked 11-06) | no |
| 121 | 1.12.2 | SRP + Spartan compat | 0.3.11 | Weapons untextured, recipes | item-render | yes: be1205904 (0.3.23); recipes confirmed | CLOSED this pass | no |
| 124 | 1.7.10 | Roguelike Dungeons | 0.3.12 | Dungeon gen error (fixed); chests/spawners empty outside generation region | worldgen | partial: 0f226c171; reporter says still empty after fix | code work | unknown |
| 125 | 1.12.2 | Thaumcraft 6 | maintainer (0.3.12) | Crystals invisible, gauntlet texture | model-loader | partial 629a742d (0.3.13) | lead in-game check | no; lowpriority |
| 125 | 1.12.2 | Thaumcraft 6 | maintainer | Workbench/pedestal/focal manipulator GUIs do not open | gui-sync | unknown | lead in-game check | no |
| 128 | 1.7.10 | Portal Gun + iChunUtil | 0.3.10 | Items/blocks do nothing; authlib members missing | needs-info | no log supplied (asked 11-02) | reporter | no |
| 132 | 1.7.10 | Fisk Superheroes | 0.3.14 | Iron Man blade pale, entity NPEs, recipes | item-render | partial: a1a9da952 (0.3.23) | lead in-game check | no |
| 133 | 1.12.2 | AbyssalCraft | 0.3.14 | Untested survival features, liquids | tracker | n/a | maintainer | n/a |
| 134 | 1.12.2 | The Midnight | 0.3.14 | Untested; unloaded chunk height risk | world-height | n/a (tracker) | maintainer | n/a |
| 135 | 1.12.2 | ChromatiCraft (Reika) | 0.3.14 | Lexicon GUI untested; NPEs come from layer self test | tracker | self-test artifact (maintainer 11-06) | maintainer | n/a |
| 136 | 1.7.10 | ElectriCraft/ReactorCraft | 0.3.14 | Untested features; wire overload is the mod rule | tracker | not-layer for jet engine | maintainer | n/a |
| 143 | 1.7.10 | Ars Magica 2 | 0.3.16 | Compendium empty/animation, ender boots gravity, secrets fail | gui-draw | postInit timeouts feaa87275; compendium open | lead in-game check | unknown |
| 144 | 1.7.10 | Thaumcraft 4 | 0.3.10 | Baubles crash (fixed 0.3.11), mirror icons, robe colour, thaumometer scan | item-render | crash yes; rest unknown | reporter (latest.log asked 11-03) | no |
| 145 | 1.12.2 | TF2 Stuff Reloaded | 0.3.10 | Weapons all same model | item-render | yes: be1205904 (0.3.23); pickup/config open | reporter retest | no |
| 147 | 1.12.2 | HuajiAge Infinite Galaxy | 0.3.18 | Own Nashorn; stands drawn as horses | module-conflict | fix 4823ef3d1 (0.3.19), not run on real jar | reporter retest | no |
| 148 | 1.12.2 | Electroblob's Wizardry | 0.3.18 | IDs instead of names, no spells, GUIs | recipe-data | yes: b2472aae7 (0.3.22) | reporter retest | no |
| 150 | 1.7.10 | NovaCraft | 0.3.18 | GUIs (enchant table/beacon container), structures | gui-sync | partial b2e6ea047 (0.3.19) | code work | no |
| 151 | 1.7.10 | Fisk Superheroes | 0.3.18 | Models/animations, HeroPack textures, quiver GUI, offhand shield | player-ability | partial: a9f6f2e09 (quiver, 0.3.23) | reporter retest | no |
| 152 | 1.7.10 | Thaumcraft 4 | 0.3.10 | Nodes, bore, Thaumonomicon, GL spam, infused ores | tesr-render-state | 4 fixed in 0.4.14 (2c06dd9ef, 8c0672f3e, 29c0967d7, 33968e916); node jar, shaders, zombie AI open | touched today by lead | no |
| 154 | 1.7.10 | Morph | 0.3.22 | Morphed model ahead/in ground, shaking, no kill animation | entity-render | partial: 1bded640a (0.3.22); rest open | code work | unknown |
| 156 | 1.21.1 pack | Iris + Complementary | 0.3.21 | Screen-fixed dot on sky under shaders | interop-modern-mod | no cause found | code work | n/a |
| 158 | 1.12.2 | Scape and Run Parasites | 0.3.21 | Relay scanner invisible blocks / paper; textured with only SRP | model-loader | forge:obj fixed 6293236c2; interference by other mods unknown | reporter bisect | no |
| 162 | 1.12.2 | Tinkers' Construct | 0.3.22 | Material models fail to load | model-loader | yes: ed8c2c159 (0.3.23) | lead in-game check | yes (1.21.1) |
| 163 | 1.12.2 | Immersive Engineering | 0.3.22 | Machines render as magenta | model-loader | yes: 0aeaa3959 (0.3.23) | lead in-game check | yes |
| 164 | 1.12.2 | Mekanism | 0.3.22 | Blocks missing textures | model-loader | yes: 0aeaa3959 (0.3.23) | lead in-game check | yes |
| 165 | 1.12.2 | Thermal Expansion | 0.3.22 | Machines invisible (CCL loader) | model-loader | yes: f647a7295 (0.3.23) | lead in-game check | yes |
| 166 | 1.12.2 | Astral Sorcery | 0.3.10 | Custom models, telescope | model-loader | mostly yes (0.3.23); telescope zoom unverified | reporter retest | no |
| 167 | 1.7.10 | Aether II | 0.3.22 | Empty spawners, portal, menu, hostile spawns | entity-ai-spawn | partial b6580a08f (0.3.23) | reporter retest | no |
| 168 | 1.7.10 | EnderIO | 0.3.22 | Painted wall/gate icons; EnderCore handler (fixed) | item-render | handler yes (0.3.23); painted items no | code work | yes |
| 169 | 1.12.2 | Applied Energistics 2 | 0.3.22 | Drive/cable bus UVL models | model-loader | yes: 3977f7b80 (0.3.23) | lead in-game check | yes |
| 170 | 1.12.2 | EnderIO | 0.3.22 | fused_quartz, painted blocks | model-loader | partial; open per comment 11-06 | code work | yes |
| 171 | 1.12.2 | Extra Utilities 2 | 0.3.22 | Black cubes, no item models | model-loader | yes: 13bbe40f3 (0.3.23) | lead in-game check | unknown |
| 172 | 1.12.2 | Industrial Foregoing | 0.3.22 | Glass-like boxes | model-loader | could not reproduce (maintainer 11-06) | maintainer closes | yes |
| 173 | 1.12.2 | Blood Magic | 0.3.22 | Routing node sprites, orbs | model-loader | yes: d658efecf, 878873248 (0.4.0) | maintainer closes | yes |
| 174 | 1.12.2 | Quark/BiblioCraft/BoP | 0.3.22 | Client handler failures | model-loader | yes: 28240cf6e, d658efecf (0.4.0); atlas item models missing from jar (not-layer) | maintainer closes | yes |
| 176 | 1.7.10 | Thaumcraft 4 | 0.3.23 | Tainted soil lighting gradient/green tint | block-render | partial: 9da7963e1 (0.4.0), 29c0967d7 (0.4.14) | lead in-game check | no |
| 177 | 1.12.2/1.7.10 | C2ME pack | 0.3.22 | Log spam, invalid item / attribute ids | not-layer | not layer per maintainer analysis | maintainer decision | n/a |
| 178 | 1.12.2 | TF2 Stuff Reloaded | 0.3.23 | Weapons, cosmetics, bosses, Australium, MvM | entity-ai-spawn | crash fixed 0.4.0; rest undiagnosed | reporter details | no |
| 179 | 1.12.2 | TF2 Stuff Reloaded | 0.3.23 | ClassCastException hitting mercenary | crash | yes: 51fed5ff8 (0.4.0) | CLOSED this pass | no |
| 181 | 1.12.2 | Electroblob's Wizardry | 0.3.23 | Spell slots not clickable | gui-sync | yes: 028dd4f3e (0.4.0) | CLOSED this pass | no |
| 183 | 1.12.2 | Electroblob's Wizardry | 0.4.0 | Structure textures, spell names, workbench | recipe-data | partial; HuajiAge preInit failure in same log | reporter retest | no |
| 184 | 1.12.2 | TF2 Stuff Reloaded | 0.4.0 | Held weapon drawn untextured, dark | item-render | no (0.4.2 asked) | code work | no |
| 185 | 1.7.10 | Thermal Expansion | 0.4.4 | Machine inventory icons all generic frame | item-render | not reproduced; jars missing | reporter jars / lead | yes |
| 188 | 1.6.4 | Crafting Dead | 0.4.4 | Severe lag (minimap chunk scan) | performance | partial: d78ed2e8b (0.4.6) | lead in-game check | no |
| 189 | 1.6.4 | Tinkers' Steelworks | 0.4.4 | NoSuchFieldError FluidType.Water: library mismatch | not-layer | n/a (maintainer: no layer change) | maintainer closes | n/a |
| 190 | 1.6.4/1.7.10 | Twilight Forest | 0.4.4 | Naga cube, green box near bosses, boss bars | entity-render | fixed 1bd2ac792, 432108e8d (0.4.8); green box unknown | lead in-game check | yes |
| 191 | 1.7.10 | Erebus/HEE/others | 0.4.5 | TPS lag, mob sounds, two mods fail | performance | Erebus 8630a492a (0.4.7); lag unknown | reporter retest | Erebus no |
| 192 | 1.6.4 | Binnie's Extra Bees/Trees | 0.4.4 | Machine particle icon missing | block-render | yes: 0abc19abb (0.4.8) | lead in-game check | no |
| 193 | 1.5.2/1.6.4 | Binnie/Witchery/Minechem | 0.4.4 | Magenta placeholder in inventory | item-render | yes: 0abc19abb (0.4.8) | lead in-game check | no |
| 194 | 1.5.2 | Factorization | 0.4.4 | All items draw Mixer | item-render | yes: 0abc19abb (0.4.8) | lead in-game check | no |
| 195 | 1.5.2 | Twilight Forest | 0.4.6 | Unknown sound events mob.tf.* | sound | possible: 8630a492a (0.4.7); unknown | lead in-game check | yes |
| 197 | 1.4.7 | RedPower | 0.4.6 | redpowerbase:multi item magenta | item-render | unknown | code work (low) | no |
| 198 | 1.20.1 | Management Wanted: More Decor | 0.4.7 | Crash RecordItem missing | migration-1201 | stand-in on master (ff7c4dab8, 22a1dca11); unverified | reporter retest | no |
| 200 | 1.4.7 | RedPower 2 | 0.4.8 | Lamp unlit with NOT gate (regression?) | block-tick-update | no | code work | no |
| 201 | 1.7.10 | BuildCraft | 0.4.8 | No detail | needs-info | n/a | reporter | unknown |
| 202 | 1.12.2 | BuildCraft Silicon | 0.4.8 | Facade scan: quartz_block state reads back as air | load-failure | no | code work | unknown |
| 203 | 1.12.2 | Forgelin + TimeCore | 0.4.8 | Module split package kotlin.reflect export | module-conflict | no (services files) | code work | no |
| 205 | 1.20.1 | Born in Chaos | 0.4.9 | MobEffect.addAttributeModifier soft link throws | migration-1201 | yes: 142ccce8a (0.4.10) | maintainer closes | unknown |
| 215 | 1.12.2 | Lycanites Mobs | 0.3.10 | Crash on kill, invisible items | crash | yes: 0a73f8b2d (0.4.10) | CLOSED this pass | no |
| 217 | 1.12.2 | Lycanites Mobs | 0.4.10 | Checkered equipment textures | item-render | no | code work | no |
| 218 | 1.20.1 | Fisk Superheroes Renewed | 0.4.10 | FMLJavaModLoadingContext ctor | migration-1201 | yes: 8dcd1f170 (0.4.11); rest unknown | reporter retest | unknown |
| 219 | 1.20.1 | Jujutsu Craft | 0.4.10 | Fails to load; no log | needs-info | n/a | reporter | no |
| 221 | 1.7.10 | Thaumcraft 4 | 0.4.13 | Eldritch Obelisk panel missing sparkle layer | tesr-render-state | partial: c914baa97 | lead in-game check | no |
| 234 | 1.7.10 | Thaumcraft 4 | 0.4.13 | Infusion pillar drawn inside-out (cull) | tesr-render-state | no | code work | no |
| 245 | 1.7.10 | LOTR Legacy | 0.3.10 | Preinit logger null, EntityRenderer | load-failure | yes: dfee91948 (0.4.14) | touched today | no |
| 247 | 1.12.2 | Prehistoric Nature | 0.4.x | Underwater plants, enchantment book | fluid | yes 0.4.14: f51aa9317, f31b0aca5 | touched today | no |
| 248 | 1.4.7 | Thermal Expansion | 0.4.13 | Sprite sheet textures; GUI power/fluid bars | gui-sync | textures yes 1761c33b4; bars open | touched today | yes |
| 250 | 1.20.1 | Epic Core API | 0.4.13 | Crash on startup (mixin target) | migration-1201 | crash report supplied 11-13 | touched today | unknown |
| 252 | 1.12.2 | Betweenlands | 0.4.0 | Missing item models, corrodible textures | model-loader | yes 0.4.14: c7fba2013, 12cc25dab | touched today | no |
| 253 | 1.12.2 | Candy World | 0.4.x | Crafting, bounce, sounds, spawns fixed; liquids, 40 variants, plant models open | fluid | partial 0.4.14: 12cc25dab, 85e857bfb | touched today | no |

## 3. Close candidates (not closed in this pass)

Maintainer-opened (only the maintainer closes): #172 (could not reproduce, 11-06), #173 (fixed 0.4.0), #174 (fixed 0.4.0; leftover is the jar's own missing models), #189 (not layer), #205 (fixed 142ccce8a, 0.4.10), #192/#193/#194 (fixed 0abc19abb in 0.4.8, pending in-game check), #162-#165, #169, #171 (fixed in 0.3.23, pending in-game check).
Others, doubtful so left open: #166 (fixed 0.3.23, telescope zoom unverified), #148 (fixed 0.3.22; the reporter filed #183 since), #198 (RecordItem stand-in on master, no release named), #218 (fixed 8dcd1f170, 0.4.11, "rest unknown").
Suggested closing sentence: "Fixed in release X (commit Y). Please comment with a new latest.log if it still happens and this will be reopened."

## 4. Duplicates and merges

- Fisk Superheroes 1.7.10: #118, #151, #132 (keep #132, the maintainer umbrella; fold #118/#151 after a retest). #218 is the 1.20.1 Renewed mod, keep separate.
- Scape and Run Parasites: #110, #158 (keep #158 as the only SRP issue; #110 point 1 is the mod's design).
- Electroblob's Wizardry: #148, #183 (keep #148).
- TF2 Stuff Reloaded: #145, #178, #184 (keep #178; #184 is the same reporter and the same log as #183; #145 is fixed).
- Thaumcraft 4: keep #152 as umbrella; #221 and #234 are both tesr-render-state and could merge; #75, #78, #144, #176 stay separate.
- 1.12.2 model-loader umbrella: #89, #125, #162-#165, #169-#173.
- Render sweep 1.5.2/1.6.4: #192 into #193.
- #87 (tracker leftover: page arrow) should be closed or split.
- Thermal Expansion #165 (1.12.2), #185 (1.7.10), #248 (1.4.7): different versions, keep.

## 5. Placeholder titles

- #110: "Scape and Run Parasites 1.10.9 (1.12.2)"
- #144: "Thaumcraft 4.2.3.5 (1.7.10)"
- #145: "TF2 Stuff Reloaded 1.7.3a (1.12.2)"
- #198: "Management Wanted: More Decor (1.20.1)"
- #250: "Epic Core API 1.1.9 (1.20.1)"
- #178/#184: "TF2 Stuff Reloaded 1.7.3a: weapons, cosmetics, MvM"
- #151: "Fisk Superheroes 2.4.0 (1.7.10): models, HeroPack, quiver, offhand"

## 6. Closed in this pass

| Issue | Evidence | Comment posted |
|---|---|---|
| #111 | VerifyError fixed 8d73863ec (0.3.7); reporter silent since 10-31 | Fixed in 0.3.7; reopen with new log if it recurs |
| #121 | be1205904 (0.3.23); reporter said recipes work | Fixed in 0.3.23 |
| #179 | 51fed5ff8, released 0.4.0, no reply since | Fixed in 0.4.0 |
| #181 | 028dd4f3e, released 0.4.0, no reply since | Fixed in 0.4.0 |
| #215 | reporter confirmed crash gone on 0.4.10 (in #217); 0a73f8b2d (0.4.10) | Fixed in 0.4.10; remainder tracked in #217 |

Each comment ended with the required AI-agent line.

## 7. Still open, not touched today

Maintainer's own (never closed by me): #10 not fixed; #64 tracker; #75 trunk fixed, golems unverified; #78 lamps below y=5 open; #87 tracker leftover; #89 CTM open; #92 not fixed; #125 unverified; #132-#136 trackers; #154 partial; #156 no cause; #162-#165, #169, #171 looks fixed (0.3.23 hashes above); #168 painted items open; #170 fused quartz/painted open; #172 could not reproduce; #173, #174 looks fixed (0.4.0); #176 partial; #177 not layer; #185 needs jars; #188 partial (d78ed2e8b); #189 not layer; #190 looks fixed except green box; #192-#194 looks fixed (0abc19abb); #195 maybe fixed (8630a492a); #197 unknown; #200 not fixed; #202 not fixed; #203 not fixed; #205 looks fixed (142ccce8a).
Others: #82 not fixed (veins below y=0); #103 not fixed (coremod); #105 waiting for reporter since 11-02, derailing open; #106 not fixed; #107 waiting for reporter since 11-02; #110 waiting for reporter since 10-31; #112 fixed but shears and step assist still open (10-31); #113 not fixed (0.4.12 retest); #114 not fixed; #118 waiting for reporter since 11-06; #124 fixed but reporter says chests empty outside region (11-02); #128 needs info; #143 not fixed: compendium text; #144 needs info (log asked 11-03); #145 waiting since 11-06; #147 waiting; #148 waiting since 11-06; #150 not fixed; #151 waiting since 11-06; #158 reporter says textured with only SRP, bisect asked 11-08; #166 waiting since 11-06; #167 waiting since 11-06; #178 needs details; #183 not fixed; #184 not fixed; #191 partial, waiting since 11-07; #198 waiting since 11-08; #201 needs info; #217 not fixed; #218 waiting since 11-08; #219 needs info; #221 not fixed (sparkle layer); #234 not fixed.
