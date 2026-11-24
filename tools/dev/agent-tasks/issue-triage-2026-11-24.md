# Triage part 1 (31 issues, state as of 2026-11-24)

Class codes: CC = CLOSE-CONFIRMED, CAND = CLOSE-CANDIDATE, AWAIT = AWAITING-REPORTER, FNA = FIXED-NOT-ANNOUNCED, PB = PENDING-BATCH / IN-PROGRESS, UNANS = UNANSWERED, FIX = FIXABLE-NOW, BLK = BLOCKED, LOW = LOWPRIORITY.
"Last speaker" is the last comment (us = maintainer's AI account kylepitcock).

| # | ver | mod | class | last speaker | state | action |
|---|---|---|---|---|---|---|
| 10 | 1.7.10 | OpenComputers vs HBM cables | BLK | none (body only, 10-24) | needs bridge legacy OC network API -> modern OC; no comments, no label; unknown which modern OC target | maintainer decision; L |
| 75 | 1.7.10 | Thaumcraft 4 golems | FIX | us 11-01 | Trunk follows (918e5d7be, ec87fe6e6); golem cores/tasks + black spots unverified | server test of golem tasks; S-M |
| 78 | 1.7.10 | Thaumcraft 4 tools/lamps | FIX | us 11-01 | axe/shovel/hoe fixed; lamp y<5 clamp + sanity checker open | generic min-height fix; S |
| 89 | 1.12.2 | Chisel (CTM) | FIX | us 11-01 | plain models only; connected textures need CTM bridge | M-L |
| 92 | 1.12.2 | Vampirism | LOW | us 11-01 | needs debug log lines; label lowpriority; jar local | last |
| 103 | 1.7.10 | Legends | UNANS | koonfam 11-24 | 5 new visual defects listed today | reply + triage |
| 105 | 1.7.10 | Traincraft CE | AWAIT | us 11-22 | fluid fix on master, retest asked 2 days ago | none |
| 106 | 1.7.10 | Starminer | FNA (partly) | us 10-31 | squid model a66483e7c + coremod hook replay 6c13b3225 (names #106) on master, not announced; rest open | comment; jar missing |
| 107 | 1.7.10 | Witchery | UNANS | nmills3 11-22 | mirror fixed; vampire ritual still does not trigger (nobody replied) | FIX vampire ritual |
| 110 | 1.12.2 | SRParasites | CAND | reporter 10-31 | fixes in 0.3.8 (c916aa9b2, 0c7f21bc5), reporter said he would test next morning, silent 24 d | ask before closing |
| 112 | 1.7.10 | ProjectE | AWAIT | us 11-22 | EMC + bag name fixed (2a6681c7c); gem boots step assist status unknown | none |
| 113 | 1.12.2 | Kindred Legacy | PB | sm644me 11-24 | spawns/textures fixed; enchantment crash root cause unknown (worker); new log 11-24 | reply with log analysis |
| 114 | 1.12.2 | HBM NTM-CE + CTM | UNANS | JayTheCoderX 11-01 | loads since 0.3.12; "radiation, models, bombs don't work", never answered; label lowpriority | ask for latest.log |
| 118 | 1.7.10 | Fisk Superheroes | CAND | us 11-14 | all reporter points answered, retest asked 10 d ago | ask before closing |
| 124 | 1.7.10 | Roguelike Dungeons | AWAIT (borderline CAND) | us 11-17 | spawners filled; empty-chest fix on nightly after e0ee19b; asked retest exactly 7 d ago | wait or ask |
| 125 | 1.12.2 | Thaumcraft 6 | LOW | us 11-01 | crystals etc. fixed; GUIs/beds/Thaumonomicon open; lowpriority | last |
| 128 | 1.7.10 | Portal Gun 4.0.0 + iChunUtil | BLK | us 11-02 | waiting 22 d for reporter's latest.log; jar not local (local PortalGun is 1.12.2 7.1.0) | none |
| 132 | 1.7.10 | Fisk tracker | FIX | us 11-06 | v0.3.23 retest asked; remaining: off-hand shield, blade pale, Spellwhip NBT NPE, HeroPack textures, first-person hand | re-verify (jar local) |
| 133 | 1.12.2 | AbyssalCraft | FIX (test pass) | none | maintainer backlog list of untested features | jar local |
| 134 | 1.12.2 | The Midnight | FIX (test pass) | none | backlog; includes risk in Chunk.getHeightValue | themidnight-0.3.5 local |
| 135 | 1.7.10 | ChromatiCraft (Reika) | FIX (test pass) | us 11-06 | entity NPEs were self-test artifacts; Lexicon GUI client untested | jars local |
| 136 | 1.7.10 | ElectriCraft/ReactorCraft | FIX (test pass) | us 11-06 | server cases pass; client GUIs, turbine placement untested | jars local |
| 143 | 1.7.10 | Ars Magica 2 | FIX | us 11-06 | postInit/timeouts fixed; empty compendium chapters, ender boots, sprite 16390 not multiple of frame size open; reporter silent 18 d | real client; jar not local |
| 144 | 1.7.10 | Thaumcraft 4 (template title) | FIX | us 11-03 | Baubles NPE fixed; reporter's later list (sinister nodes not spawning naturally, eye ritual puts player outside maze, robe colour) got only "I need your log" | test with local TC jar |
| 147 | 1.12.2 | HuajiAge Infinite Galaxy | UNANS | nikele321 11-18 | models work; thrown items show white square, dungeon loot unsure | reply + fix |
| 148 | 1.12.2 | Electroblob's Wizardry | CAND | us 11-14 | fixes in v0.3.22, retest asked 10 d ago, silent | ask before closing |
| 150 | 1.7.10 | NovaCraft | CAND | us 11-14 | missing classes fixed (b2e6ea047, fa439b4db), never run with the mod (no jar), retest asked 10 d ago | ask before closing; weak evidence |
| 151 | 1.7.10 | Fisk Superheroes | PB (queued #151 arm sync) | us 11-24 | flight squash fixed (e4434f2aa); arm sync queued | none |
| 152 | 1.7.10 | Thaumcraft 4 | FNA (likely) | us 11-16 | remaining item aspect icons dark: fix 99a995bbd (unlit world overlays) is on origin/master, comment said not merged; also a5b42a24e (aura node targeting) only on a worker branch | announce after checking |
| 154 | 1.7.10 | Morph | UNANS | t3rav0lt 11-23 | transition draws only shadow + camera tilt still open per our 11-16; reporter now reports shaking models with video + log | reply; analyze log |
| 156 | 1.21.1 | Iris/Complementary dot | FIX | none | no repro; screen-fixed dot under shaders; needs real client | M |

## CLOSE-CONFIRMED
None.

## CLOSE-CANDIDATE (lead asks before closing)
- #110: our ask 2026-10-31 ("please retest ... 0.3.8"), reporter's last words "I'll test in the morning" (10-31). No answer for 24 days. Fixes c916aa9b2 (culling opt-out), 0c7f21bc5 (cloud colours), 1d8ade888 (sounds). The issue title is the unfilled template.
- #118: ask 2026-11-14 (nightly retest). Reporter's 11-02 points (fly, blade, canon work; gray armor, HUD) answered by a1a9da952 and our 11-14 comment says they are fixed. Residual Fisk work lives in #132/#151.
- #148: ask 2026-11-14. Fixes b2472aae7, 4823ef3d1, v0.3.22 released. Silent 10 d.
- #150: ask 2026-11-14. Fixes b2e6ea047 (shim BlockEnchantmentTable, ContainerBeacon, BiomeEndDecorator...), fa439b4db. Never run with the mod; reporter complaint "GUI and structures not working" is vague. Weakest candidate.
- Borderline #124: ask 2026-11-17 exactly 7 days ago; the empty chests ("still open" in 11-17 comment 1, follow-up says the change is on master; commits 7aa0d0603 spawner names, ee9ef664d populator tile entity). Wait one more day or ask.

## AWAITING-REPORTER
#105 (11-22), #112 (11-22; open question: gem boots step assist from reporter 10-31 not mentioned in later comments, unknown), #124 (see above), #128 (needs latest.log since 11-02, also BLK).

## FIXED-NOT-ANNOUNCED
- #106 Starminer. Commits on master: a66483e7c (ModelSquid, comment 10-31 said "next build"), 6c13b3225 merge "generic coremod hook replay ... (#103, #106)". Draft: "The Manbazooka squid and the generic coremod hook replay (the mechanism that runs the hooks coremods insert into the player/entity classes) are on master, in the nightly. I haven't run Starminer's gravity itself in a client yet, so please retest gravity cores/walls, planets and the space dimension on the nightly (delete `.legacyconnector/cache`) and say which still fail. *(Written by an AI agent working on this project.)*" The Starminer jar is not local (download needed to verify first).
- #152 (likely). 99a995bbd "world overlays at the block outline and RenderWorldLastEvent draw unlit" is on origin/master; this matches the aspect-icon fix described on 11-16 as unmerged. Verify the commit is the aspect-icon fix, then: "The dark/washed-out aspect icons above nodes are fixed on master (world overlays now draw unlit as in 1.7.10), in the nightly. Please retest nodes and the node in a jar. *(Written by an AI agent working on this project.)*"

## PENDING-BATCH / IN-PROGRESS
- #113: worker on enchantment-reference crash (root cause unknown); sm644me posted a log 11-24 (crash needs two launches, error logged repeatedly after switching to survival, using an enchanted hoe). Natural spawns + Fennekin textures confirmed fixed by reporter 11-24.
- #151 (arm sync queued; squash fix in nightly). #132 shares the Fisk arm/off-hand defects.
- #152: a5b42a24e (aura node targeting) exists only on a worker branch.

## UNANSWERED
- #103 Legends (koonfam 11-24): 1 Symbiote/venom model spins constantly; 2 generator flickering dark red pillar; 3 Iron Man suits: suit and player both drawn, worse when crouching; 4 suit flight animations flash for a fraction of a second; 5 Iron Man armor on armor stands renders light grey leather. Jar local (1Legends-1.7.10-8.6.2.jar, FiskHeroes 2.4.0 local too). Items 3-5 overlap the Fisk arm-sync / armor model defects.
- #107 Witchery (nmills3 11-22): mirror ok; "vampire ritual still does not trigger". Jar local (witchery-1.7.10-0.24.1).
- #114: reporter 11-01 on 0.3.12 "radiation doesn't work, most models fail to load, bombs don't work". Needs latest.log; lowpriority label. HBM jars local (HBM-NTM-.1.0.27 is a 1.7.10 one, the 1.12.2 NTM-CE jar not seen).
- #147 (nikele321 11-18): thrown items (knives, car) show a white square for a few seconds in flight; unsure dungeon loot spawns. HuajiAge jar local.
- #154 Morph (t3rav0lt 11-23): models shake in-morph; video + latest.log attached. Morph 1.7.10 jar not found locally (only 1.12.2 Morph), though a client test was done 11-16.

## BLOCKED
#10 (needs a decision on the target modern OC API; no comments), #128 (reporter log; jar not local).

## LOWPRIORITY
#92 Vampirism (jar local, needs debug log), #125 Thaumcraft 6 (many commits since 11-01, e.g. f33a17bf5, 77baa8afb; GUIs/beds/Thaumonomicon state unknown), #114 (HBM). Cheap check: #92 one run with debug logging looking for 'Object holder ...HunterActions/VampireActions'.

## FIXABLE-NOW groups, ranked by value
1. Fisk/Legends armor + player model (popular, no native 1.21.1 version): #132 (remaining list), #103 items 3-5, #151 arm sync. Real client needed, M. Jars local (FiskHeroes 2.4.0, Legends 8.6.2).
2. Thaumcraft 4 (popular, 1.7.10): #75 golem tasks, #78 lamp y<5 / sanity checker, #144 sinister node natural spawn + eldritch ritual placement + robe tint, #152 follow-up. Server tests mostly (S-M), jar local (Thaumcraft-1.7.10-4.2.3.5).
3. Reika mods: #135, #136 untested client GUIs/turbine, S each, jars local (ChromatiCraft, DragonAPI, ElectriCraft, ReactorCraft). Test pass only.
4. 1.12.2 survival test passes: #133 AbyssalCraft, #134 The Midnight (watch Chunk.getHeightValue sync-generation risk); jars local; no known defect.
5. Rendering under shaders: #156 (Iris + Complementary, screen-fixed dot; legacy particles through LegacyParticles.onRender / glow render type are suspects), real client with perf mods, M.
6. Mod-specific content: #143 Ars Magica 2 (empty compendium chapters, animated sprite 16390 not a multiple of 16, real client, jar must be downloaded); #89 Chisel CTM bridge (1.12.2, M-L, Chisel jar local, CTM jar unknown; check if native 1.21.1 Chisel exists before investing); #106 Starminer remaining (planet generation, space sky, G-flower orientation; jar to download).
# Triage part 2 (31 issues), as of 2026-11-24

Evidence: `gh issue view`, `git log origin/master`, `origin/master..batch2`, `--all`. Fix commits are on origin/master unless marked batch2.

| # | ver | mod | class | last speaker + date | state | action |
|---|---|---|---|---|---|---|
| 158 | 1.12.2 | Scape and Run Parasites | AWAITING-REPORTER | us 2026-11-08 | red cubes, scanner paper, forge:obj fixed (cd8818c7, 6293236c2); black relay only in reporter's pack, bisect asked 16 d ago | ask once more / close as not reproducible |
| 166 | 1.12.2 | Astral Sorcery | CLOSE-CANDIDATE | us 2026-11-06 | v0.3.23 retest asked, silent 18 d | ask to close |
| 167 | 1.7.10 | Aether II 1.6 | BLOCKED | us 2026-11-15 | reporter confirmed all fixed except overworld mobs spawning in the Aether | needs comparison with original; jar not local |
| 168 | 1.7.10 / 1.12.2 | EnderIO | FIXABLE-NOW | us 2026-11-16 | 1.12.2 painted blocks fixed; 1.7.10 unpainted wall/fence gate item textures untouched | see group C |
| 170 | 1.12.2 | EnderIO | CLOSE-CANDIDATE | us 2026-11-15 | fused quartz fixed (71bc1acda); maintainer-opened | verify once and close |
| 172 | 1.12.2 | Industrial Foregoing | CLOSE-CANDIDATE | us 2026-11-06 | not reproducible; SidedProxy fix 07b0ee288; maintainer-opened | close |
| 176 | 1.7.10 | Thaumcraft 4 tainted soil | CLOSE-CANDIDATE | us 2026-11-14 | biome fix 9da7963e1; even purple in test client; retest asked 10 d ago; maintainer-opened | close |
| 177 | pack | c2me log spam | BLOCKED | us 2026-11-07 | layer cleared by log reading; asked for world without legacy mods; silent 17 d | close as not ours if no answer |
| 178 | 1.12.2 | TF2 Stuff Reloaded | AWAITING-REPORTER | us 2026-11-07 | bosses/names, cosmetics, Australium, MvM undiagnosed; screenshots requested; overlaps #184 | consolidate into #184 |
| 183 | 1.12.2 | Electroblob's Wizardry | UNANSWERED | nikele321 2026-11-21 | 11-18 and 11-21 comments never answered | see below |
| 184 | 1.12.2 | TF2 Stuff Reloaded | UNANSWERED | nikele321 2026-11-21 | ammo fix 42a7dc4f2 on master; he still says weapons do not fire and mob AI mishandles weapons | see below |
| 188 | 1.6.4 | Crafting Dead | CLOSE-CANDIDATE | us 2026-11-14 | perf fix 9315ad2fa merged; retest asked 10 d ago; maintainer-opened | verify and close |
| 191 | 1.7.10/1.6.4 | Erebus, HEE, Witches and More | AWAITING-REPORTER | us 2026-11-21 | status list posted 3 d ago; lag still uninvestigated; Witches structures = #325 | wait |
| 201 | 1.7.10 | BuildCraft | CLOSE-CANDIDATE | us 2026-11-16 | Data Anchor NPE fixed (5ddd64e20); retest without Data Anchor asked 8 d ago | ask to close |
| 217 | 1.12.2 | Lycanites Mobs | CLOSE-CANDIDATE | us 2026-11-15 | equipment built-in models (bb01b3f57 etc.); retest asked 9 d ago | ask to close |
| 218 | 1.20.1 | Fisk Superheroes Renewed | CLOSE-CANDIDATE | us 2026-11-14 | FMLJavaModLoadingContext ctor fixed (8dcd1f170); jar IS local (test-mods-issues/218), so a run can confirm | run once, then close |
| 219 | 1.20.1 | JujutsuCraft | CLOSE-CANDIDATE | us 2026-11-14 | abstract-event handler fixed (no "#219" commit found, unknown which); jar local (test-mods-issues/309, see #309) | run once, then close |
| 221 | 1.7.10 | Thaumcraft 4 obelisk | CLOSE-CANDIDATE | us 2026-11-15 | glTexGen/eye-linear fixes (c914baa97, 5fb61ccf7); asked for shader/culling info 9 d ago | ask to close |
| 234 | 1.7.10 | Thaumcraft 4 pillars | CLOSE-CANDIDATE | us 2026-11-13 | not reproducible; asked Entity Culling test 11 d ago; no fix needed found | close if no answer |
| 245 | 1.7.10 | LOTR | AWAITING-REPORTER | us 2026-11-16 | 3 load steps fixed from logs; jar not local; later steps unknown | wait for retest |
| 247 | 1.12.2 | Prehistoric Nature | FIXABLE-NOW | us 2026-11-15 | plants, enchant, creatures fixed; blue plane under water open | see group B |
| 248 | 1.4.7 | Thermal Expansion | PENDING-BATCH | Staegrin 2026-11-24, us same day | ore gen confirmed; machine frame texture = bind fix 8e89f1c30 (batch2) | wait for nightly |
| 250 | 1.20.1 | Epic Core API | CLOSE-CANDIDATE | us 2026-11-14 | EntityDataAccessor fix a0ee2c69e; jar local (test-mods-issues/250), mod never run | run once, then close |
| 252 | 1.12.2 | The Betweenlands | PENDING-BATCH | us 2026-11-15 | more fixes on batch2 under #316 (ca46d0516, 07b8a72fa, bfdb815d5); leftovers weedwood_lever, caving_rope_light, shield poses | wait; post after #316 ships |
| 259 | 1.12.2 | BuildCraft 8 | CLOSE-CANDIDATE | us 2026-11-16 | all reporter points fixed per commits (94c833fd8, eb3160378, 8cf55b5cd, 409f2a4e9); Creative Engine texture not reproducible, close-up asked 8 d ago | ask to close |
| 268 | 1.7.10 | MineFactory Reloaded | PENDING-BATCH | us 2026-11-15 | RedNet fix e8fa99624 on master; Portaspawner and Liquicrafter fixes on batch2 (f02e5c0a6, 10046d0ed); question on energy cable outline unanswered 9 d | wait; then post |
| 278 | 1.7.10 | FNAF | AWAITING-REPORTER | us 2026-11-24 | normals fix 1ca989e7d in nightly | wait |
| 282 | 1.12.2 | Wolf Armor and Storage | AWAITING-REPORTER | us 2026-11-22 | c3b3b563c, checked in client | wait |
| 284 | 1.12.2 | Colored Redstone | AWAITING-REPORTER | us 2026-11-24 | lamp/repeater/dust fix 48c95ee69 in nightly | wait |
| 289 | 1.12.2 | Candy World | PENDING-BATCH | us 2026-11-22 | biome/spawn/fluid/egg tab on batch2 (20dfa2874 etc.) | in-flight |
| 293 | 1.7.10 | Thermal Expansion | PENDING-BATCH | us 2026-11-24 | smelting done; item texture bind 8e89f1c30 on batch2 | in-flight |

Counts: CLOSE-CANDIDATE 13, AWAITING-REPORTER 7, UNANSWERED 2, PENDING-BATCH 5, FIXABLE-NOW 2, BLOCKED 2, others 0. FIXED-NOT-ANNOUNCED 0 (every fix on master has a comment). LOWPRIORITY, OUT-OF-SCOPE, CLOSE-CONFIRMED 0.

## CLOSE-CANDIDATE (lead will ask before closing)
- 166 (last ask 2026-11-06, v0.3.23 retest): caveat, telescope zoom, night sky constellations and placed telescope were never verified. Jar local (`1.12.2 test mods/astralsorcery-1.12.2-1.10.27.jar`).
- 170 (2026-11-15), 172 (2026-11-06), 176 (2026-11-14), 188 (2026-11-14): all opened by the maintainer; no external reporter will answer. Close after one own verification or close as fixed per commits above.
- 201 (2026-11-16): reporter's log pointed at Data Anchor; commit 5ddd64e20. Our server test reload of BuildCraft 7.1.23 was fine.
- 217 (2026-11-15), 221 (2026-11-15), 259 (2026-11-16): fixes listed above.
- 218, 219, 250: 1.20.1 fixes were "build checks only". The comment "we do not have the Renewed jar" is outdated: jars exist at `test-mods-issues/218`, `/309` (Jujutsu), `/250`. A single headless/client run of each would turn "unverified" into evidence before closing.
- 234: not reproduced; asked Entity Culling test 11-13; reporter silent. Close as cannot reproduce if the lead agrees.

## UNANSWERED (draft comments)
- 183 (nikele321, 2026-11-18 and 2026-11-21: spell name HUD below the wand does not show, wizard AI problematic, textures missing). Last answer from us 2026-11-14 (acacia bookshelf, missing in the mod itself). Draft: "Thanks for the update. Noted as still open: the spell name that should appear at the bottom of the screen when a wand is held, and the wizard mob AI. The missing bookshelf side texture is not in the mod's own jar, so it is missing in 1.12.2 too; please send a screenshot of any other block that shows the missing-texture pattern. We are looking at the spell name display and the wizard AI next. *(Written by an AI agent working on this project.)*"
- 184 (nikele321, 2026-11-21: "Weapons don't fire, and the AI for mobs also doesn't handle weapons correctly"). Ammo listener fix 42a7dc4f2 is on master since 2026-11-18 (nightly), so either he tested an older nightly or fire still fails for another reason. Draft: "Thanks. Which nightly build number are you on, and which weapon? The ammo count fix was in nightly builds from 2026-11-18; with an older build the weapon still shows 0 ammo. If it is the current nightly, please attach latest.log from a run where you picked up ammo and tried to fire, and say what the mob does with its weapon (holds it, never attacks, attacks without damage). *(Written by an AI agent working on this project.)*"

## PENDING-BATCH / in-flight notes
- 248, 293: same texture bind fix (batch2 8e89f1c30). Reporter's latest 2026-11-24 (Machine Frame textured as sandstone, inventory icons wrong in Thermal GUIs) already answered by us the same day. 293: inventory icons wrong only while a CoFH GUI is open; worker is on it.
- 252: leftovers not covered by batch2 as far as I can tell: `weedwood_lever`, `caving_rope_light` plain models, shield blocking poses (unknown whether #316/#336 cover them).
- 268: still unanswered by reporter: which mod the energy cables with the lost golden outline belong to (asked 2026-11-15).
- 289: known in-flight (#289 branch merged into batch2).

## BLOCKED
- 167: only open point is overworld hostile mobs spawning in the Aether. Maintainer said the original may do the same. Needs the Aether II 1.6 jar (not found under test-mods*, run-subtest/mods*, run-screenshots/mods; only Aether I and 1.12.2 Aether jars exist) or its source to compare spawn lists. Commit b6580a08f (biome spawn lists from legacy biomes) may be related.
- 177: log reading shows the errors start before any legacy generation and come from other mods' templates/villagers; reporter has not replied to the questions since 2026-11-07. Close as not caused by the layer if no answer.

## FIXABLE-NOW groups, ranked
A. 1.12.2 content mods: Electroblob's Wizardry + TF2 Stuff Reloaded (#183, #184; also #178 undiagnosed items). Class: HUD text from item (selected-spell name) not drawn; mob AI with held weapons (entity AI/equipment); TF2 weapons still not firing for the reporter. Area: 1.12.2 shim entity AI/EntityLiving, HUD overlay events (`bridge/*1122`, `Platform1122`), see agent-tasks BRIEF "where is X" for entities/GUI. Needs real client for HUD, server enough for AI. Size M. Jars local: TF2 `test-mods-issues/184/TF2-Stuff-Reloaded-1.12.2-1.7.3a.jar`, Wizardry `1.12.2 test mods/ElectroblobsWizardry-4.3.14.jar` (reporter uses 4.3.19). Reporter pings repeatedly; popular mod; no native 1.21.1 port.
B. #247 Prehistoric Nature (Lepidodendron) 1.12.2: "blue plane under water" (water-material block/fluid rendering through a translucent layer). Area: 1.12.2 fluid/render layer of water-material blocks (commit f51aa9317 is the related earlier change). Real client needed. Size S to M. Jar local: `test-mods-issues/247/lepidodendron-1.12.2-63.02.jar`. Not yet reproduced by us.
C. #168 EnderIO 1.7.10: unpainted `blockpaintedwall` / `blockpaintedfencegate` items with missing icon (`missing_icon_block_502_...`). Class: 1.7.10 item icons of ItemBlocks whose block icon registration depends on meta or on a paint tile. Area: 1.7.10 shim icon registry / item atlas. Real client needed. Size S. Jar local: `run-screenshots/mods-big-1793109628/EnderIO-1.7.10-2.3.0.429_beta.jar`. EnderIO has a native 1.21.1 version (LOWPRIORITY under project rule); cheap.

Not FIXABLE-NOW because in-flight: #248, #293, #289, #252/#316 leftovers, #268 leftovers.
# Triage part 3 (31 issues, state as of 2026-11-24; origin/master e02415b3f, batch2 = 72 commits ahead)

| issue | ver | mod | class | last speaker + date | state | action |
|---|---|---|---|---|---|---|
| 295 | 1.12.2 | CustomOreGen | PENDING-BATCH | us 11-24 | offset fix on master; baked display lists (10->70 fps) only in next batch | wait for batch2 nightly, post |
| 298 | 1.20.1 | Thermal Foundation | CLOSE-CANDIDATE | us 11-17 | creative-tab crash fixed on master (b82c7cef9); reporter silent (7 days today) | ask before closing |
| 302 | 1.20.1 | Thermal Expansion | AWAITING-REPORTER | us 11-18 | motion packet crash fixed (1d7612209, eb20431); no retest (6 days) | wait one more day, then close-candidate |
| 303 | 1.20.1 | Thermal Dynamics | AWAITING-REPORTER | us 11-21 | RF between machines/ghost duct blocks: not reproduced, asked placement/reload question | wait; nothing since |
| 306 | 1.7.10 | Mystcraft | CLOSE-CANDIDATE | us 11-17 | reporter: "it seems to be working now"; 3 GUI leftovers + slow save fixed on master (45a474cbf, 106cd12fe); no retest (7 days today, borderline) | ask before closing |
| 307 | any | End void structures | AWAITING-REPORTER | us 11-22 | not reproducible; real fault fixed (79136bb6e); need mod list/log | wait |
| 308 | 1.7.10 | Mystcraft | AWAITING-REPORTER | us 11-18 | age persistence + save hang fixed on master; new-age-per-entry and sky not reproduced, retest asked (6 days). Sky is now tracked in #332 | wait, then close-candidate |
| 309 | 1.20.1 | GeckoLib/JujutsuCraft | AWAITING-REPORTER | us 11-21 | original ClassCircularityError fixed (a391667dd..e64ecef3b); ability cooldowns never recover: not reproduced, asked which ability | wait (jar in test-mods-issues/309) |
| 310 | 1.4.7 | RedPower 2 | FIXABLE-NOW | us 11-21 | tubes/wires/GUIs fixed; microblock textures, pick block, dark icons, recipes still open, no worker, no commit since 11-18 | group A |
| 311 | 1.12.2 | Thermal 1.12.2 | IN-PROGRESS | us 11-24 | tier regression (urgent worker) | none |
| 312 | 1.2.5 | RedPower 2 | FIXABLE-NOW | us 11-21 | same open list as #310 | group A |
| 315 | 1.12.2 | Fossils | AWAITING-REPORTER | us 11-22 | 4 defects fixed (640d926fd, 40925c3b4); "garbled title logo" leftover | wait |
| 316 | 1.12.2 | Betweenlands | PENDING-BATCH | us 11-21 | hang fixed on master (6e6bf567a); model fixes (07b8a72fa, ca46d0516) on batch2; sky bubble/transparent vegetation/load time unanswered | post after batch2 |
| 318 | 1.7.10 | Galacticraft | AWAITING-REPORTER | us 11-21 | tab fixed (55503fced, on origin); leftovers: faint mask icon, misplaced player preview | wait; leftovers FIXABLE-NOW (S) |
| 320 | 1.20.1 | Cracker's Wither Storm | AWAITING-REPORTER | us 11-21 | 3 crashes fixed (7168a423c); no client test | wait |
| 322 | 1.20.1 | Magic Evolved 2 / Photon | IN-PROGRESS | us 11-24 | billboard + model particles missing; asked for example spells (queued) | none |
| 323 | 1.4.7 | Railcraft | PENDING-BATCH | us 11-22 | carts/GUI crashes fixed; coke/plates lost on reload fixed only on batch2 (9b7a96fb6, no issue number in subject); cobble/fence/wall textures open | post after batch2 |
| 324 | 1.12.2 + 1.7.10 | IC2 Classic | AWAITING-REPORTER | Staegrin 11-22, us 11-22 | 1.12 preInit + cable drops fixed; explosions/helmet not reproducible, asked item/log | wait |
| 325 | 1.6.4 | Witches and More | IN-PROGRESS | us 11-24 | worker on biome-dependent structures (branch lead-wam-items) | none |
| 327 | 1.4.7 | Xycraft | AWAITING-REPORTER | us 11-24 | ore bands, torches in nightly; asked retest | wait |
| 328 | 1.4.7 | IC2 | AWAITING-REPORTER | us 11-24 | loads; ores/leaves name in nightly; rubber leaves texture e361ef91a only on batch2; cable/geothermal items not done | wait |
| 329 | 1.4.7 | BuildCraft | IN-PROGRESS | us 11-24 | worker: arm sync, double chest pipes; quarry below y=0 on batch2 | none |
| 331 | 1.7.10 | Erebus | AWAITING-REPORTER | us 11-24 | fixes in nightly; leaf decay fix 4a7750959 on batch2 | wait |
| 332 | 1.7.10 | Mystcraft sky | IN-PROGRESS | us 11-24 | clouds fixed (93ef4b583); sky box bubble: loading reporter's save (dim 5) | none |
| 335 | 1.20.1 | FNAF Management Wanted | PENDING-BATCH | us 11-23 | widget fix 184eec37f on master; tags/biome modifier/GeckoLib on batch2 | post after batch2 |
| 336 | 1.12.2 | Galacticraft Legacy | PENDING-BATCH | us 11-23 | tab/tooltip/model fixes on master (7945c3aca); dark item icons partial on batch2 (2888e4b91) | post after batch2 |
| 337 | 1.12.2 | AVP + MDXLib | PENDING-BATCH | us 11-23 | 3b324551c on batch2 | post |
| 338 | 1.12.2 | Rival Rebels | PENDING-BATCH | us 11-23 | loader fix 49a2117e0 on batch2; jar not downloadable | post |
| 339 | 1.12.2 | ApocalypseCraft | PENDING-BATCH | us 11-23 | 3b324551c on batch2; jar not downloadable | post |
| 340 | 1.12.2 | TFC + Florae | PENDING-BATCH | us 11-23 | 7d3784965 on batch2 | post |
| 342 | 1.12.2 | GTCE TJ | PENDING-BATCH | us 11-24 | 172dc2d04 on batch2; also needs AE2 1.12.2 (told reporter); runtime resource pack blockstates open | post |

No issue is CLOSE-CONFIRMED, FIXED-NOT-ANNOUNCED or UNANSWERED: every issue's last comment is ours, except #324 (Staegrin's note about #328 is answered by #328 itself).

## CLOSE-CANDIDATE
- #298 (Thermal Foundation 1.20.1): last comment ours 2026-11-17 (fix list, retest asked). Reporter's second report (machines tab crash) was fixed in b82c7cef9; no word since. Counts as 7 days on 11-24.
- #306 (Mystcraft 1.7.10): reporter's last message 11-17 "it seems to be working now", then listed three GUI points (e key, black box, desk at auto scale), fixed 11-17; slow saves improved (106cd12fe). Our last comment 11-17 asked to retest. Draft: "Everything listed here is fixed on master and in the current nightly (0.5.2-nightly.150 or later). Closing as fixed; reopen with a log if the name field, black box, desk layout or save time still misbehave. *(Written by an AI agent working on this project.)*"
- Not yet (6 days): #302, #308.

## AWAITING-REPORTER (details)
- #303: asked whether ducts were placed in the same session as the machines or in a reopened world. Ghost blocks come from `Grid.findEdge` "grid is null".
- #309: asked for one ability that stays on cooldown. Jar in `test-mods-issues/309`.
- #315, #320, #318, #327, #328, #331: fix posted, retest asked, silence since.
- #324: asked which item set off IC2 explosions and whether the quantum helmet was charged. Server scenarios (98bac6a02, 9cef3224d) pass.
- #307: this issue was opened by the maintainer from a second-hand report; no log from the original reporter.

## PENDING-BATCH (draft comments once the batch is in a nightly)
- #323: "Coke Oven and Rolling Machine contents now survive a world reload (stacks of mods in the 1.4.7 id space are saved by their own id). In the next nightly after the regression run; delete `.legacyconnector/cache`. Cobblestone, fence and wall textures are still open. *(Written by an AI agent working on this project.)*"
- #316, #335, #336, #337, #338, #339, #340, #342, #295: fixes listed in the table, all on batch2.

## FIXABLE-NOW groups (ranked)
A. **RedPower 2 microblock/multipart rendering and recipes, 1.4.7 and 1.2.5 (#310, #312).** No native 1.21.1 version. Defect class: multipart (covers/panels) textures wrong (Basalt Brick cover as oak log, ruby cover as torch, iron panel strips purple, diamond panel see-through, atlas-sized texture on pick block), pick block returns the dropped subtype, insulated wire/logic icons dark and logic components repeated and rotated texture, Wooden Sails/Athame recipes, Alloy Furnace recipes missing in JEI, covers not in JEI on 1.2.5. Area: 1.4.7/1.2.5 forward layer render types (see 0a2bd6da0, 280ab6f33), `Assets` for item blocks, JEI recipe bridge (`e0d22e3f5` shows the overlay-id pattern). Needs a real client; size L. Jars: `test-mods-issues/310`, `test-mods-issues/312`. Reporter Staegrin is active and tests every nightly.
B. **Railcraft 1.4.7 textures (#323):** cobblestone/fence/wall textures wrong (screenshots in the 11-21 comment). Real client, size S-M. Jar: `test-mods-issues/323b`, `rails1122`. Item loss on reload already on batch2.
C. **Galacticraft 1.7.10 inventory tab leftovers (#318):** mask icon very faint, player preview in extended inventory misplaced. Real client, size S. Jar available (`Galacticraft-1.7-3.0.12.504` in test mods).
D. **JujutsuCraft abilities/cooldowns (#309):** MCreator mod, suspect player-variable capability or tick handler on 1.20.1; not reproduced, needs a world with a jar (`test-mods-issues/309`), size M. Lower value (niche mod); wait for reporter's answer first.
E. **Fossils title logo garbled (#315 leftover),** client, size S, jar in `1.12.2 test mods/fossilsarcheology-8.0.6.jar`.

## BLOCKED
- #338, #339: jars could not be downloaded (known); fixes exist for loading but behaviour (structures, guns, Rival Rebels) cannot be verified. #342 needs AE2 1.12.2 added by reporter (asked).
- #307: needs the original reporter's mod list/log.

## LOWPRIORITY / OUT-OF-SCOPE
None of the 31 identified (Management Wanted, Wither Storm, Magic Evolved, Jujutsu Craft are 1.20.1 mods with no 1.21.1 port known to me; unknown whether any has one).
