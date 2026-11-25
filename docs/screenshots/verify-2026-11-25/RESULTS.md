# Client verification 2026-11-25 (base: batch4 4d22c1a63, real client on Windows, performance mods on, no Iris Veil Compat)

Scenarios are in `tools/dev/verify/scenarios/` (run with `tools/dev/verify-client.sh <name>`). Screenshots are in this folder.

## Failures first

| Item | Result | Detail |
|---|---|---|
| #328 IC2 1.4.7, BatBox GUI full | FAIL | BatBox with 40000 EU on the server (`TileEntityElectricBatBox.energy = maxStorage`, logged unchanged for 200 ticks) shows "Power Level 0 /40000" in the GUI (`328-ic2-batbox-gui-2.jpg`). With an RE-Battery inserted the server held 30000 and the GUI showed 30000/40000 (`328-ic2-batbox-battery-2.jpg`). Values above 32767 look like they are lost in the 16-bit container data (short) sync. Repro: `ic2-147` scenario (needs only `industrialcraft-2_1.115.231-lf.jar`). |
| #328 rubber leaves name | FAIL | `/give @s ic2:rubberleaves` hover name is "Oak Leaves", not "Leaves" (log line `IC2LEAVES`, also with a fresh translation cache). The `VanillaNames.genericName` fix of dd1b65759 is in the base but is not reached on this path (item hover name via the ItemBlock subclass). |
| #327 Xycraft 1.4.7 | FAIL (new finding) | Client log: `Exception in legacy event handler soaryn.xycraft.machines.handler.MachineEventHandler.renderBlockHighlight ... Cannot invoke WorldClient.getTileEntity(int,int,int) because "event.context.theWorld" is null` (RenderGlobal.theWorld is null in the DrawBlockHighlightEvent shim); the layer disables the handler after 4 failures. Also `Name of LegacyItem{world.items.aluminumFoil} failed: ... "liquid" is null`. Repro: `xycraft147` scenario, look at a placed multibrick. The tank itself was NOT proven to form (see below). |

## Passed or partly passed

| # | Item | Result | Evidence |
|---|---|---|---|
| 1 | #113 reload-twice, Kindred Legacy (1.12.2) | PASS | `reload-twice`: enchant, save, quit to title (`level.disconnect()` + `mc.disconnect(screen)`), reopen same world in the same JVM, the enchanted hoe is in the inventory, enchant again, save; log has none of "not valid in current registry set", "Error saving", "StackHolders". `113-reload-twice-kindred.jpg` |
| 1 | same, 1.7.10 set (suite `mixed` 1.7.10 jars) | PASS | `reload-twice-1710`, `113-reload-twice-1710.jpg` |
| 1 | same, 1.12.2 set (Thermal family of suite `thermal1122`) | PASS | `reload-twice-1122`, `113-reload-twice-1122.jpg` |
| 2 | `/legacyconnector report`, single player, no cheats | PASS | `report-bundle`: run with permission level 0 as the world owner: allowed, chat shows "Bug report bundle written: <path> [copy path]" and the attach line, zip appears in `logs/` (`report-bundle-chat.jpg`). Zip holds summary.txt, latest.log, debug.log, legacyconnector-report.txt, legacyconnector-degraded.txt. summary.txt has versions, JVM args, settings, "Legacy mods (2)" and "Modern mods (47)". Searching every file for `Kyleb`, `C:\Users`, `C:/Users`, `accessToken`, `Bearer`: 0 hits (`<home>` placeholders in the JVM arguments). The click action is `COPY_TO_CLIPBOARD` with the absolute path (checked in `BugReport.java`; the click itself was not performed). Note: this worktree lives on Y:, so the redactor was exercised through the `<home>\.gradle` paths only. |
| 3 | #344 world types 1.12.2: Biomes O' Plenty | PASS | `worldtypes1122`: button list "Default, Superflat, Large Biomes, AMPLIFIED, Single Biome, Biomes O' Plenty, Lost Cities, Lost Cities (BoP)" (+ Debug Mode); BoP created through the Create World screen's button, spawn (-120.5, 69, -86.5) on ground, plains; quit and reload keeps the position (`344-1122-bop-*.jpg`). |
| 3 | #344 Lost Cities (1.12.2) | PASS | `worldtypes1122-lostcities`: spawn (-12.5, 102, -38.5) on ground in badlands, reload identical (`344-1122-lostcities-*.jpg`; the spawn area shows no city). Cycling every entry with screenshots was not done: the list was read from the UI state and the type selected through it; the World tab with "World Type: Lost Cities" is in `344-1122-lostcities-wtlc-bop-selected.jpg`. |
| 5 | #248 sheet-based item renderers 1.4.7 | PASS (inventory) | `inv147`: Thermal Expansion Machine Frame (`component_32`), Tesseract/Energy Cell frames, machines, BuildCraft pipes/gears/engine/chipset/wire, RedPower ingots/sickle/paint/lamp/frame/bag all draw their textures in the creative inventory's hotbar row (`248-147-te-inventory.jpg`, `-bc-`, `-rp-`). JEI draw of the Machine Frame was NOT tested. |
| 6 | 1.12.2 JEI bridge, Thermal Expansion | PASS | `jei1122-thermal` (JEI 1.21.1 19.70): 55 categories, TE ones listed with recipes (Pulverizer 193, Induction Smelter 390, Sawmill 52, Magma Crucible 37, Phytogenic Insolator 87 ...); Pulverizer tab shown with energy bar (`1122-jei-thermal-jei-thermal-pulverizer.jpg`). |
| 6 | Ender IO SAG Mill tab | PASS (tab), "+" not seen | `jei1122-eio` with EnderCore-1.12.2-0.5.76.jar (the fragment `EnderCore-1.12.2` alone picks a library inside test-mods3 and EnderIO then fails with NoClassDefFoundError NNList; that is a harness pick, not a layer defect): SAG Mill=66, Alloy Smelter=190, Painting Machine=590 and more; SAG Mill tab with recipes (`1122-jei-eio-jei-eio-sag.jpg`). The "+" transfer button needs ingredients in a survival inventory and did not show here. |
| 6 | Mekanism gas slots | PASS | Electrolytic Separator tab draws the gas tank slots with fluid/gas fill and the energy bar (`1122-jei-eio-jei-mek-gas.jpg`); 15 `GasStack` ingredients registered. |
| 9 | #335 Management Wanted (1.20.1 + GeckoLib 1.21.1) | PASS | `mw1201`: title screen is the mod's `CustomMainMenuScreen` with logo, buttons and Mods emerald, background crisp (`335-mw-title.jpg`); vanilla pause and Options screens in world are blurred as in 1.21.1 (`335-mw-vanilla-pause.jpg`, `-options.jpg`); Options opened from the mod's title screen shows its blurred background (`335-mw-title-options.jpg`). Not tested: the laptop display, and screens of two other 1.20.1 mods (no config screen was available without extra dependencies). Log has recipe parse errors for `management_wanted:*_facade_brick_*` ("Map entry 'a' : Failed to parse") and `ceiling_tile_stairs_recipe` (unknown registry key). |
| 10 | #336 Galacticraft 1.12.2 spawn eggs | PASS | Creative "Spawn Eggs" tab holds 13 Galacticraft eggs (log `GCTABS`). |

## Not tested

| # | Item | Why |
|---|---|---|
| 3 | 1.7.10 world types (BoP 1.7.10, Planetoid, FarLands) | No jar of any of them exists in the test folders (only 1.12.2, 1.20.1 and NeoForge BoP). The y -64..320 vs 0..255 and constant spawn height concern is therefore unchecked. Quark and Botania (Garden of Glass) 1.12.2 types were also not tried. |
| 4 | #328 three Geothermal Generators with lava show active | The generators ran with `fuel` set (19999 left, ticking) but `active=false`: they have no energy sink, which stops the burn in IC2; no lava feeding or sink was built, so this is inconclusive. Block metadata for the Geothermal Generator was guessed (`ic2:blockgenerator_1`). |
| 4 | RE-Battery drains visually | Inserting a battery worked (GUI 30000/40000, stable over 200 ticks) but the BatBox display bug above hides the real check. |
| 5 | Machine Frame in JEI | Not run (JEI with the 1.4.7 NEI bridge). |
| 6 | Vanilla Anvil tab with Blood Magic / Roots | Not run (Blood Magic needs Guide-API and Baubles; Anvil category has recipes: 662 to 2002 entries, but not examined for these mods). |
| 7 | #327 multiblock tank forms | The formation rules of the Xycraft tank are unknown to the worker; a 5x5x5 hollow of engineering bricks with a valve was built, a "right click" with a material item did nothing visible, and breaking a wall block and the valve gave no crash or tick error in the log. Formation not proven. |
| 8 | #107 Witchery vampire ritual fills the goblet | The ritual needs a ritual circle, altar and the rite's own sequence; not built. |
| 10 | #336 Space Race Manager button, Mars sky after teleport, reload on Mars | No way to teleport to Mars from the harness: `/dimensiontp` is Galacticraft's own command (opens the planet GUI), the Mars dimension is created lazily and not in the level list. The 2026-11-24 worker's screenshots (`docs/screenshots/verify-2026-11-24/galacticraft`) remain the only evidence. |

## Harness changes made

- `tools/dev/verify/client.py`: `SEARCH` also has `test-mods-issues/113, 328, 329, 310, 335, 327, 248` and `test-mods-mcarchive/1.4.7`.
- `reload-twice.txt`: fortune does not apply to a hoe (a hoe in 1.21.1 refuses it) so unbreaking is used; the inventory is cleared first (Kindred Legacy hands out a kit in slot 0); the save runs on the server thread (`server.executeBlocking`); the world is left with `level.disconnect()` followed by `mc.disconnect(GenericMessageScreen)` and `setScreen(TitleScreen)` (a bare `mc.disconnect()` called from the control script never finishes: the integrated server is not stopped); a count of enchanted tools is checked after reopening.
- New scenarios: `reload-twice-1710`, `reload-twice-1122`, `report-bundle`, `worldtypes1122`, `worldtypes1122-lostcities`, `ic2-147`, `inv147`, `xycraft147`, `mw1201`, `gc1122` (exploration only), `jei1122-thermal`, `jei1122-eio`.
- Two stray JVMs of mine (a leftover client from the first failed run) were killed; the regression lane's processes were not touched.
