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
| `1.5.2/resource-engineering-composter-gui-shaders.png` | Resource Engineering | The composter screen opened by right-click (container works; the mod's own GUI background is missing) | Partly working |
| `1.5.2/applied-energistics-1-blocks-shaders.png` | Applied Energistics rv14 (AE1) | Its blocks (grinder, terminals, ore, crafting grid) with their textures | Partly working |
| `1.5.2/twilight-forest-mobs-shaders.png` | The Twilight Forest 1.5.2 | A boar and a bighorn sheep summoned; the mod's init failed on a render class so they use vanilla models | Partly working |

### 1.6.4

| File | Mod | What it shows | Rating |
|---|---|---|---|
| `1.6.4/sorcery-kitchens-blocks-shaders.png` | Sorcery, Kitchens | Rune and machine blocks, slime block, counters, cupboard, icebox, churn | Works |
| `1.6.4/tragicmc-countercraft-sorcery-blocks-shaders.png` | TragicMC, Counter Craft, Sorcery | Ruby and sapphire blocks, candle, torches, crates, barrel, bench, magic leaves, transport node | Works (Counter Craft: partly) |
| `1.6.4/sorcery-mobs-shaders.png` | Sorcery | Unicorn, phoenix, unicow and golden chicken with their own models | Works |
| `1.6.4/tragicmc-mobs-kitsune-boss-shaders.png` | TragicMC | Kitsune boss (with boss bar), troll, wraith, minotaur | Works |
| `1.6.4/tragicmc-troll-death-survival-shaders.png` | TragicMC | Survival: the player killed by the troll; death message uses the mob's name | Works |
| `1.6.4/kitchens-icebox-gui-shaders.png` | Kitchens | The icebox container screen | Works |
| `1.6.4/counter-craft-bench-screen-shaders.png` | Counter Craft | Right-click on the bench: a full-screen logo splash instead of a crafting screen | Partly working |
| `1.6.4/power-of-the-elements-hammer-and-soldier.png` | Power of the Elements | Survival hotbar (lightning hammer, earth chestplate, blocks) and a soldier mob in the distance; no shaders | Works |
| `1.6.4/power-of-the-elements-electric-and-lightning-blocks.png` | Power of the Elements | Electric and lightning blocks placed in survival; no shaders | Works |

Findings: The Lion King Mod makes the client disconnect right after joining (empty reason); Mad Science, Applied Energistics rv14.finale3 (1.6.4) and RedLogic (1.5.2) fail to load in a client session
(details in `docs/data/mods.json`).
