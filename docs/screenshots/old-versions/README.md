# Screenshots of old Minecraft versions running on 1.21.1

Taken in a real client (NeoForge 1.21.1, superflat world) through the layer. Mod jars are never committed.

## 1.4.7

Client set: Sodium, Sodium Extra, Iris, ImmediatelyFast, FerriteCore, ModernFix, EntityCulling, MoreCulling, BadOptimizations, C2ME, Clumps and the shader pack
Complementary Reimagined r5.9.3 + Euphoria Patches (Iris reported "Using shaderpack"). The shots are of old-only mods (BuildCraft excepted) (no file for Minecraft 1.7.2 or newer).

| File | Mod | Version | What it shows | Rating |
|---|---|---|---|---|
| `1.4.7/applied-energistics-rv9-blocks-and-quartz-pick-shaders.png` | Applied Energistics (the original, not AE2) | rv9 | Its blocks placed in the world and the quartz pick axe in hand, in creative mode. The quartz ore and the grinder part draw with their sprites cut from the mod's sheet; one multiblock still shows the missing texture | Partly working |
| `1.4.7/applied-energistics-rv9-quartz-tools-creative-shaders.png` | Applied Energistics, RedPower 2 | rv9, 2.0pr6 | The creative inventory with the quartz pick, sword, wrench and the RedPower diamond saw drawn from sprite sheet indices (1.4.7 has no icons) | Partly working (AE), Partly working (RedPower) |
| `1.4.7/redpower-world-ores-and-marble-shaders.png` | RedPower 2 (World module) | 2.0pr6 | Ruby, emerald, sapphire and silver ore, marble, basalt, ruby and emerald blocks, a rubber log and leaves, placed in creative mode: block textures come from the mod's own sprite sheet | Works |
| `1.4.7/redpower-core-items-and-block-icons-shaders.png` | RedPower 2 (Core, Base, Mechanical, Digital, World) | 2.0pr6 | The survival inventory with RedPower items (screwdriver, gems, tools, paint, bag, saw blade) and the 3D icons of block items drawn by the mod's own renderer (gate, deployer, frame, alloy furnace, wire, lamp) | Works |
| `1.4.7/redpower-multipart-wire-gate-deployer-lamp-shaders.png` | RedPower 2 (Digital, Mechanical) | 2.0pr6 | Logic gates with redstone torches, deployers and lamps placed with the item in creative mode: the multipart tessellator renderer draws them from several sprite sheets | Works |
| `1.4.7/redpower-base-alloy-furnace-gui-shaders.png` | RedPower 2 (Base) | 2.0pr6 | The alloy furnace GUI with items moved into its slots by clicks (the GUI image is read by its sheet path) | Works |
| `1.4.7/redpower-machine-deployer-gui-filled-shaders.png` | RedPower 2 (Mechanical) | 2.0pr6 | The deployer GUI after moving two stacks from the hotbar into it: container slots and clicks sync with the server | Works |
| `1.4.7/applied-energistics-blocks-ore-and-multiblocks-shaders.png` | Applied Energistics | rv9 | Quartz ore and tile-less multiblock parts placed by command; the parts draw as frame cubes, not as cables | Partly working |
| `1.4.7/applied-energistics-quartz-tools-and-parts-shaders.png` | Applied Energistics | rv9 | Quartz wrench, axe, pick, sword, shovel, hoe, knife, materials and machine items in the inventory | Works |
| `1.4.7/computercraft-detectors-and-peripherals-shaders.png` | ComputerCraft, Entity Detector, CCTV, MiscPeripherals | 1.5, 1.1, 1.0, 3.1c | Computer, disk drive, turtle, the entity detector blocks, the CCTV camera and two MiscPeripherals blocks | Works (blocks), untried (use) |
| `1.4.7/buildcraft-engines-tank-pump-workbench-shaders.png` | BuildCraft | 3.4.6 | The three engines, tank, pump, auto workbench, mining well, laser, refinery and hopper placed in creative mode, drawn from the mod's block sheet | Works |
| `1.4.7/redpower-alloy-furnace-red-alloy-survival-shaders.png` | RedPower 2 (Base) | 2.0pr6 | Survival: copper and redstone moved into the alloy furnace, coal as fuel (flames lit), red alloy ingots in the output slot | Works |
| `1.4.7/redpower-lever-lamp-survival-off-shaders.png`, `1.4.7/redpower-lever-lamp-survival-on-shaders.png` | RedPower 2 (Lighting) | 2.0pr6 | Survival: a lever on a cobblestone block powers the adjacent RedPower lamp (off and on); alloy furnace and machine block beside it | Works |
| `1.4.7/buildcraft-autocrafting-table-gui-survival-shaders.png` | BuildCraft | 3.4.6 | Survival: the autocrafting table screen with its translated title (the mod picks its language file by `StringTranslate.getCurrentLanguage`); the recipe preview does not fill | Partly working |
| `1.4.7/buildcraft-tank-pump-and-redpower-lamps-survival-shaders.png` | BuildCraft, RedPower 2 | 3.4.6, 2.0pr6 | Tank, pump and a lit lamp beside a redstone block in daylight | Works |
| `1.4.7/applied-energistics-ore-and-machine-survival-shaders.png` | Applied Energistics | rv9 | Quartz ore and a machine block placed in survival mode | Partly working |

| `1.4.7/redpower-logic-not-gate-lever-lamp-lit-survival-shaders.png`, `1.4.7/redpower-logic-not-gate-lever-lamp-dark-survival-shaders.png` | RedPower 2 (Wiring, Logic, Lighting) | 2.0pr6 | Survival, shaders: a vanilla lever beside a red alloy wire, a NOT gate and a red lamp, placed through the item path. Lever off: the gate outputs and the lamp is lit; lever on: the wire carries the signal and the lamp goes dark | Works |
| `1.4.7/redpower-transposer-tubes-chest-survival-shaders.png` | RedPower 2 (Machine) | 2.0pr6 | Survival: a transposer moves items from a chest through two pneumatic tubes into a second chest when a redstone block pulses it | Works |
| `1.4.7/redpower-deployer-places-cobblestone-survival-shaders.png` | RedPower 2 (Machine) | 2.0pr6 | Survival: a deployer with cobblestone in its inventory places the block in front of it on a redstone pulse (next to the chest and tubes) | Works |
| `1.4.7/redpower-project-table-crafting-survival-shaders.png` | RedPower 2 (Base) | 2.0pr6 | Survival: the project table screen after crafting sticks from planks (the recipe lookup of the client container had no world and found nothing before) | Works |
| `1.4.7/redpower-world-generation-ore-and-marble-in-cave-shaders.png` | RedPower 2 (World) | 2.0pr6 | A normal world (cave pocket cut by command): generated RedPower ore next to marble | Works |
| `1.4.7/applied-energistics-terminal-gui-survival-shaders.png`, `1.4.7/applied-energistics-drive-gui-survival-shaders.png` | Applied Energistics | rv9 | Survival: the ME Terminal (title, search field, sort buttons) and the ME Drive screens opened on blocks placed through the item path | Works (screens), partial (network) |
| `1.4.7/buildcraft-wooden-pipe-engine-cobblestone-pipes-chest-survival-shaders.png` | BuildCraft | 3.4.6 | Survival: a redstone-powered wooden engine drives a wooden pipe that pulls cobblestone out of a chest; cobblestone pipes carry it to the second chest | Works |
| `1.4.7/buildcraft-oil-pool-pump-tank-survival-shaders.png` | BuildCraft | 3.4.6 | An oil pool (black, drawn by the mod's fluid renderer; it was a hole), a pump and a tank in a forest | Works (liquid transport untried) |
| `1.4.7/entity-detector-and-cctv-blocks-survival-shaders.png` | Entity Detector, CCTV | 1.1, 1.0 | The four entity detector blocks (face textures from the mod's sheet) and the CCTV camera block (the mod's note-block-like tile with a lens) | Works |
| `1.4.7/computercraft-computer-disk-drive-turtle-survival-shaders.png` | ComputerCraft | 1.5 | Computer, disk drive and turtle placed in survival | Works (blocks) |
| `1.4.7/computercraft-computer-terminal-help-shaders.png` | ComputerCraft | 1.5 | The computer screen after typing `help` and `ls` (keys forwarded to the program): the terminal draws its text. Needed the font sheet the mod uploads itself and the ROM mount | Works |
| `1.4.7/computercraft-turtle-lua-refuel-forward-shaders.png` | ComputerCraft | 1.5 | A turtle's screen (terminal and inventory): `turtle.refuel()` after coal was put in its slot, then `turtle.forward()` returned true and the turtle moved | Works |
| `1.4.7/computercraft-computer-detects-miscperipherals-gate-reader-shaders.png` | ComputerCraft, MiscPeripherals | 1.5, 3.1c | A MiscPeripherals block beside a computer is listed as a peripheral (`left gateReader`) | Works |
| `1.4.7/applied-energistics-me-network-cables-controller-drive-terminal-shaders.png` | Applied Energistics, BuildCraft | rv9, 3.4.6 | An ME network: controller powered by a wooden engine (redstone block), thin cables, a drive and a crafting terminal. Known defect: a light blue square on the ground under each cable | Works |
| `1.4.7/applied-energistics-drive-1k-cell-gui-shaders.png` | Applied Energistics | rv9 | The ME Drive screen with a 1k storage cell inserted (tooltip: 0 of 1024 bytes used) | Works |
| `1.4.7/applied-energistics-c-terminal-shows-stored-cobblestone-shaders.png` | Applied Energistics | rv9 | The ME C-Terminal listing 64 cobblestone that was put into the powered network (stored in the drive's cell) | Works |
| `1.4.7/miscperipherals-blocks-survival-shaders.png` | MiscPeripherals | 3.1c | Charge station, iron note block, resupply station, interactive sorter, rail reader, teleporter, player detector and crafter, all drawn with their textures | Works (blocks) |
| `1.4.7/forestry-apiary-block-and-bees-survival-shaders.png` | Forestry, Extra Bees | 2.0.0.11, 1.5.0.6b | The apiary placed (the screen does not open: this Forestry build targets another obfuscation); bee items show the missing texture | Partly working |


## 1.5.2 and 1.6.4: survival tests (2026-11-07)

Real client (NeoForge 1.21.1, superflat), survival mode for the actions (crafting items given, machines placed and opened, mobs fought), with the performance set
(Sodium, Iris, ImmediatelyFast, FerriteCore, ModernFix, EntityCulling, MoreCulling, BadOptimizations, C2ME, Clumps) and the shader pack Complementary Reimagined r5.9.3 + Euphoria Patches
(Iris logged "Using shaderpack"). Files marked without `-shaders` were taken without the shader pack. No defect appeared only under shaders. Not testable: Better Than Wolves
(a patched Minecraft jar, see `docs/wiki/Legacy-1.5.2-Estimate.md`) and RedPower 2 (no 1.5.2 or 1.6.4 build; the 1.4.7 build is above).

### 1.5.2

| File | Mod | What it shows | Rating |
|---|---|---|---|
| `1.5.2/resource-engineering-blocks-shaders.png` | Resource Engineering 0.5.3 | Gem blocks (ruby, emerald, citrine...) and its machines (composter, pressure vessel, fracker, Thor chest) | Partly working |
| `1.5.2/resource-engineering-tools-survival-shaders.png` | Resource Engineering, Applied Energistics | Survival hotbar with ruby pickaxe, boots, citrine sword, scythe, hammer and chain mesh in front of AE1 blocks | Partly working |
| `1.5.2/resource-engineering-composter-gui-shaders.png` | Resource Engineering | The composter screen opened by right-click with seeds and flesh in the hotbar: the mod's own background (it reuses `sifterGUI.png`) and items on their slots. (The earlier note of a missing background was wrong; the Thor chest's item mismatch is the mod's own server/client container mismatch) | Works |
| `1.5.2/applied-energistics-1-blocks-shaders.png` | Applied Energistics rv14 (AE1) | Its blocks (grinder, terminals, ore, crafting grid) with their textures | Partly working |
| `1.5.2/twilight-forest-mobs-shaders.png` | The Twilight Forest 1.5.2 | Penguin, bighorn sheep and wild boar with the mod's own models and skins (init no longer fails on `new RenderLiving(model, shadow)`; the entity's string texture is bound) | Works |
| `1.5.2/applied-energistics-1-machines-shaders.png` | Applied Energistics rv14 (AE1) | Its terminal, chest, drive, crafting and interface blocks in a row; the mod's lang file now loads (the no-controller message reads "Unable to communicate with controller.") | Partly working |

### 1.6.4

| File | Mod | What it shows | Rating |
|---|---|---|---|
| `1.6.4/sorcery-kitchens-blocks-shaders.png` | Sorcery, Kitchens | Rune and machine blocks, slime block, counters, cupboard, icebox, churn | Works |
| `1.6.4/tragicmc-countercraft-sorcery-blocks-shaders.png` | TragicMC, Counter Craft, Sorcery | Ruby and sapphire blocks, candle, torches, crates, barrel, bench, magic leaves, transport node | Works (Counter Craft: partly) |
| `1.6.4/sorcery-mobs-shaders.png` | Sorcery | Unicorn, phoenix, unicow and golden chicken with their own models | Works |
| `1.6.4/tragicmc-mobs-kitsune-boss-shaders.png` | TragicMC | Kitsune boss (with boss bar), troll, wraith, minotaur | Works |
| `1.6.4/tragicmc-troll-death-survival-shaders.png` | TragicMC | Survival: the player killed by the troll; death message uses the mob's name | Works |
| `1.6.4/kitchens-icebox-gui-shaders.png` | Kitchens | The icebox container screen | Works |
| `1.6.4/counter-craft-bench-menu-shaders.png` | Counter Craft | Right-click on the bench: the mod's own menu (Play, Inventory, Community; "Cloud offline" since the mod is an online lobby). The earlier logo splash was the intro of a menu whose client setup had been aborted by the mod's System.exit check (now ignored) | Works (offline) |
| `1.6.4/the-lion-king-mobs-shaders.png` | The Lion King Mod | A lion, a giraffe and a rhino: the client now stays connected (the mod's `connectionReceived` returned an empty string, which the layer took as a kick) | Works |
| `1.6.4/mad-science-machines-and-gmo-cows-shaders.png` | Mad Science | Cryofreezer, genome sequencer, mainframe, voxbox, CNC machine drawn with Techne models, and a GMO wooly cow (the mod's `loadInstance(String, URL)` loader now links) | Works |
| `1.6.4/applied-energistics-rv14-and-lion-king-blocks-shaders.png` | Applied Energistics rv14.finale3, The Lion King Mod | AE blocks (loads without IC2) and Lion King blocks (altar, hyena head, peacock block, pridestone brick, termite mound) | Partly working (AE), Works (Lion King) |
| `1.6.4/power-of-the-elements-titans-shaders.png` | Power of the Elements | Ice, fire and air titans and a soldier summoned: all draw with the mod's skins | Works |
| `1.6.4/power-of-the-elements-hammer-and-soldier.png` | Power of the Elements | Survival hotbar (lightning hammer, earth chestplate, blocks) and a soldier mob in the distance; no shaders | Works |
| `1.6.4/power-of-the-elements-electric-and-lightning-blocks.png` | Power of the Elements | Electric and lightning blocks placed in survival; no shaders | Works |

Third round (survival, shaders; all new old-only mods):

| File | Mod | What it shows | Rating |
|---|---|---|---|
| `1.6.4/mythicalcraft-thundersword-crafting-shaders.png` | MythicalCraft V3 | The 3x3 crafting grid (a crafting table made from four mythical planks in the 2x2 grid) with the thunder sword recipe | Works |
| `1.6.4/mythicalcraft-thundersword-fight-shaders.png` | MythicalCraft V3 | The crafted sword in hand against an Enchanted Iron Armor mob | Works |
| `1.6.4/galaxy-warfare-titanium-smelting-shaders.png` | Galaxy Warfare | Survival furnace: titanium lumps smelted into ingots (coal fuel) | Works |
| `1.6.4/galaxy-warfare-titanium-pickaxe-crafting-shaders.png` | Galaxy Warfare | Titanium pickaxe recipe in the 3x3 grid | Works |
| `1.6.4/galaxy-warfare-soldiers-shaders.png` | Galaxy Warfare | Soldier and special-ops soldier mobs with the MK-405 gun and a soldier helmet in the inventory | Works (gun firing not verified) |
| `1.6.4/raycraft-infuser-block-shaders.png` | Raycraft | The infuser (crafted from obsidian, iron bars and glowstone) placed | Works |
| `1.6.4/dimensional-warps-mobs-shaders.png` | Emerald's Dimensional Warps | Star cow, bull, seagull and other mobs with their own models | Works |
| `1.6.4/dimensional-warps-sky-dimension-shaders.png` | Emerald's Dimensional Warps | The sky dimension, which now generates (its chunk provider constructor failed on a mismatched `TerrainGen.getModdedNoiseGenerators` signature) | Works |

Fourth round (shaders; 2026-11-07, the Tinkers' Construct, Goblins, Counter Craft and old-only checks):

| File | Mod | What it shows | Rating |
|---|---|---|---|
| `1.6.4/tinkers-construct-tool-station-pickaxe-survival-shaders.png` | Tinkers' Construct EX.30 | Survival: the Tool Station screen (the mod's own, with its own slot drawing and clicks) after a wooden pickaxe head, rod and binding were placed by clicks: the output slot shows the pickaxe and its stats | Mostly playable |
| `1.6.4/goblins-mod-mobs-and-tinkers-slime-shaders.png` | Goblins! Mod, Tinkers' Construct, Advanced Survival | Goblin soldier, shaman, chief, ranger and dire wolf with their skins (the textures sit in an inner archive of the download), a blue slime, boss bars; the guardian mob of Advanced Survival at the right still shows a missing texture in this shot | Works |
| `1.6.4/counter-craft-ak47-first-person-shaders.png` | Counter Craft | The AK-47 in hand after firing (recoil has pushed the view up), grenades in the hotbar | Partly working |
| `1.6.4/herobrine-mod-mobs-and-boss-shaders.png` | Herobrine Mod | Herobrine with smoke, the zombie and pig variants, Notch and the boss bar | Works |
| `1.6.4/infusioncraft-blocks-and-tools-shaders.png` | Infusion Craft | Infused blocks with the mod's rainbow textures, the infused sword in hand, bow, pickaxe and armor in the hotbar | Works |
| `1.6.4/quazarcraft-blocks-sword-and-armor-shaders.png` | QuazarCraft | Quazar blocks, glass, ore, brick, the crafter blocks, a loot zombie and the shiny sword in hand | Works |
| `1.6.4/walking-dead-nuclear-power-mithril-blocks-shaders.png` | The Walking Dead, Nuclear Power, Mithril | A row of blocks (gun rack with a missing texture on the left, sandbag, barbed wire, mithril and ore, gas furnace, fusion reactor parts) and the M4A1 in hand | Works (gun rack texture missing) |
| `1.6.4/ancient-warfare-crafting-table-gui-survival-shaders.png` | Ancient Warfare | The research table screen with its Craft, Research and Queue tabs | Works |
| `1.6.4/booze-blocks-shaders.png`, `1.6.4/rhodium-tools-blocks-shaders.png`, `1.6.4/solar-apocalypse-blocks-shaders.png`, `1.6.4/mrcrayfish-construction-blocks-shaders.png`, `1.6.4/dark-adaptations-blocks-shaders.png`, `1.6.4/ocarina-mod-blocks-shaders.png` | Booze, Rhodium Tools, Solar Apocalypse, MrCrayfish's Construction Mod, Dark Adaptations, The Ocarina Mod | The mods' blocks placed on a superflat world (automatic block shots) | Works |

1.5.2 (same round):

| File | Mod | What it shows | Rating |
|---|---|---|---|
| `1.5.2/grimoire-of-gaia-mobs-shaders.png` | Grimoire of Gaia | Valkyrie, harpy, dryad and other mobs with their models | Works |
| `1.5.2/grimoire-of-gaia-fight-survival-shaders.png` | Grimoire of Gaia | A witch and a creeper girl fought with a diamond sword in survival; drops in the hotbar | Works |
| `1.5.2/applied-energistics-1-controller-terminal-drive-shaders.png` | Applied Energistics rv14.dev2 | Controller, terminal, drive and energy cell placed in survival | Partly working |
| `1.5.2/applied-energistics-1-terminal-gui-shaders.png` | Applied Energistics rv14.dev2 | The ME Terminal screen opened next to the controller | Partly working |
| `1.5.2/redlogic-lamp-lit-wire-shaders.png` | RedLogic (with Immibis Core and Microblocks) | A lamp lit by a redstone block, a RedLogic wire (flat white pad, does not conduct) and the items in the hotbar; ids are `redlogic:redlogic.*` now (they were `minecraft:redlogic.*`) and the lamp textures resolve | Partly working |

| `1.5.2/miscperipherals-resupply-station-gui-shaders.png` | MiscPeripherals, Liquid XP | MiscPeripherals' Resupply Station screen over a row of placed Liquid XP and MiscPeripherals machines (Liquid XP now loads: it read `Minecraft.session` while loading). One block in the hotbar shows the missing texture: Advanced Machines, which needs IC2 | Works (MiscPeripherals), Works (Liquid XP) |

Findings of the first round and their causes (fixed in the second round, details in `docs/data/mods.json`): The Lion King Mod's disconnect was `connectionReceived` returning an empty string; Mad Science
implements the 1.6.4 `IModelCustomLoader.loadInstance(String, URL)`; Applied Energistics rv14.finale3 needed absent IC2 types stubbed; RedLogic needs Immibis Microblocks next to Immibis Core
(and read `BlockRedstoneWire.getDeclaredFields()[0]`); Counter Craft calls `System.exit` when more than five mods are loaded; Twilight Forest 1.5.2 instantiates the concrete `RenderLiving`;
Applied Energistics 1 loads its lang file by class path. Resource Engineering's Thor chest mismatch is the mod's own (its server handler builds the backpack container).
| `1.4.7/buildcraft-pump-fills-tank-with-water-survival-shaders.png` | BuildCraft | 3.4.6 | Survival, shaders: a redstone-powered wooden engine drives a pump over a water source; the tank beside it holds 1000 mB of water (checked in the tile). Needs a world floor above y=0 (see the wiki) | Works |
| `1.4.7/applied-energistics-cables-between-machines-no-ground-square-shaders.png` | Applied Energistics | rv9 | Cables joining two machines. The pale square seen under cables in an earlier shot is not a model quad (see the wiki status) | Works |
| `1.4.7/mystcraft-writing-desk-gui-survival-shaders.png` | Mystcraft | 0.10.1 | The writing desk screen opened in survival (symbol list, ink and page slots); the layout is wider than the window | Partly working |
| `1.4.7/entity-detector-lights-lamp-beside-cow-shaders.png` | Entity Detector | 1.1 | A detector powers a redstone lamp while a cow stands next to it | Works |
| `1.4.7/cctv-monitor-on-wall-and-camera-block-shaders.png` | CCTV | 1.0 | The monitor item hangs on a wall (it did not place before) and draws its screen; the camera block beside it still uses a placeholder tile. The camera feed was not checked | Partly working |
