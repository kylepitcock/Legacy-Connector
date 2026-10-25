package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graphical test ({@code -Dlegacyconnector.screenshotTest=true}): creates a world, flies around, builds showcase platforms of
 * legacy blocks, fills the inventory with legacy items and writes a screenshot after each step, then quits. The
 * screenshots go to {@code legacyconnector.screenshotDir} (default {gameDir}/shots), so what the game really draws can be looked at.
 *
 * <p>Properties: {@code legacyconnector.shotWorldType} (preset id such as biomesoplenty:biomesop), {@code legacyconnector.shotMods}
 * (comma list; default every legacy mod), {@code legacyconnector.shotBlocksPerMod} (default 24), {@code legacyconnector.shotItemsPerMod}
 * (default 36), {@code legacyconnector.shotTour} (terrain shots, default 6), {@code legacyconnector.shotSeed}.</p>
 */
public final class WorldScreenshotTest {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Screenshots");
    private static boolean started;
    private static final Deque<Action> QUEUE = new ArrayDeque<>();
    private static File dir;
    private static long ticks;
    private static final long WATCHDOG_TICKS = 20L * 60 * 25;

    private interface Action {
        /** @return true when finished (called once per client tick). */
        boolean tick(Minecraft mc);
    }

    private WorldScreenshotTest() {}

    public static void register() {
        if (System.getProperty("legacyconnector.screenshotTest") == null) return;
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            if (started || !(e.getNewScreen() instanceof TitleScreen)) return;
            started = true;
            Minecraft.getInstance().tell(WorldScreenshotTest::begin);
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> onTick());
    }

    private static int intProp(String key, int def) {
        try {
            return Integer.parseInt(System.getProperty(key, String.valueOf(def)));
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private static void begin() {
        Minecraft mc = Minecraft.getInstance();
        dir = new File(System.getProperty("legacyconnector.screenshotDir", new File(mc.gameDirectory, "shots").getPath()));
        dir.mkdirs();
        // The window may not be focused (the harness runs unattended): never pause, never show the tutorial toast.
        mc.options.pauseOnLostFocus = false;
        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
        mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
        mc.options.renderDistance().set(intProp("legacyconnector.shotRenderDistance", 16));
        String type = System.getProperty("legacyconnector.shotWorldType", "");
        long seed = Long.parseLong(System.getProperty("legacyconnector.shotSeed", "20241022"));
        LOG.info("Creating the screenshot world through the create-world screen (type '{}', seed {}), shots go to {}", type, seed, dir);
        net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.openFresh(mc, new TitleScreen());
        // The screen builds itself asynchronously; configure it like a player would, then press "Create New World".
        QUEUE.add(createWorld(type, String.valueOf(seed)));
        queueScript();
    }

    // ------------------------------------------------------------------ script

    private static Action createWorld(String type, String seed) {
        int[] waited = {0};
        return mc -> {
            if (!(mc.screen instanceof net.minecraft.client.gui.screens.worldselection.CreateWorldScreen screen)) return ++waited[0] > 600;
            if (++waited[0] < 20) return false;
            var state = screen.getUiState();
            state.setName("LegacyConnectorShots" + System.currentTimeMillis());
            state.setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE);
            state.setDifficulty(Difficulty.PEACEFUL);
            state.setAllowCommands(true);
            state.setSeed(seed);
            if (!type.isEmpty()) {
                var wanted = ResourceLocation.parse(type);
                boolean found = false;
                var all = new ArrayList<>(state.getNormalPresetList());
                all.addAll(state.getAltPresetList());
                for (var entry : all) {
                    var key = entry.preset().unwrapKey();
                    if (key.isPresent() && key.get().location().equals(wanted)) {
                        state.setWorldType(entry);
                        found = true;
                    }
                }
                if (!found) {
                    var names = new ArrayList<String>();
                    for (var entry : all) entry.preset().unwrapKey().ifPresent(k -> names.add(k.location().toString()));
                    LOG.warn("World type {} is not offered on the create-world screen (offered: {})", type, names);
                }
            }
            for (var child : screen.children()) {
                if (child instanceof net.minecraft.client.gui.components.Button button
                        && button.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                        && tc.getKey().equals("selectWorld.create")) {
                    LOG.info("Creating the world: type {}", state.getWorldType().describePreset().getString());
                    button.onPress();
                    return true;
                }
            }
            LOG.error("No create button on the create-world screen");
            return true;
        };
    }

    private static Action wait(int n) {
        int[] left = {n};
        return mc -> --left[0] <= 0;
    }

    private static Action waitForWorld() {
        return mc -> mc.player != null && mc.level != null && mc.screen == null && mc.player.tickCount > 40;
    }

    private static Action waitForTerrain(int minTicks) {
        int[] t = {0};
        return mc -> ++t[0] >= minTicks && mc.levelRenderer.hasRenderedAllSections();
    }

    private static Action command(String c) {
        return mc -> {
            mc.player.connection.sendCommand(c);
            return true;
        };
    }

    private static Action look(float yaw, float pitch) {
        return mc -> {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
            mc.player.yRotO = yaw;
            mc.player.xRotO = pitch;
            return true;
        };
    }

    private static Action hideGui(boolean hide) {
        return mc -> {
            mc.options.hideGui = hide;
            return true;
        };
    }

    private static Action debug(boolean on) {
        return mc -> {
            mc.getDebugOverlay().toggleOverlay();
            return true;
        };
    }

    private static Action shot(String name) {
        int[] t = {0};
        return mc -> {
            if (++t[0] == 8) mc.gui.getChat().clearMessages(false);
            if (t[0] < 12) return false;
            try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                File file = new File(dir, name + ".png");
                image.writeToFile(file);
                LOG.info("Screenshot {}", file.getName());
            } catch (Exception e) {
                LOG.error("Screenshot {} failed", name, e);
            }
            return true;
        };
    }

    private static Action run(java.util.function.Consumer<Minecraft> r) {
        return mc -> {
            r.accept(mc);
            return true;
        };
    }

    private static void add(Action... actions) {
        for (Action a : actions) QUEUE.add(a);
    }

    private static void queueScript() {
        int tour = intProp("legacyconnector.shotTour", 6);
        int perModBlocks = intProp("legacyconnector.shotBlocksPerMod", 24);
        int perModItems = intProp("legacyconnector.shotItemsPerMod", 36);
        add(waitForWorld(), wait(40),
                command("gamemode spectator"), command("time set noon"), command("weather clear"),
                command("gamerule doDaylightCycle false"), command("gamerule doMobSpawning false"), command("gamerule doWeatherCycle false"),
                command("tp @s 0 180 0"), waitForTerrain(200));
        if ("portal".equals(System.getProperty("legacyconnector.shotScenario"))) {
            queuePortalScenario();
            return;
        }
        if ("control".equals(System.getProperty("legacyconnector.shotScenario"))) {
            // an interactive session: RemoteControl executes command files until it is told to quit
            var control = new RemoteControl(new File(dir.getParentFile(), "control"));
            add(command("gamemode creative"), command("time set noon"), command("weather clear"), command("gamerule doDaylightCycle false"),
                    command("gamerule doMobSpawning false"), command("tp @s 0 150 0"), waitForTerrain(100));
            add(mc -> {
                control.tick(mc);
                return false;
            });
            return;
        }
        if ("commands".equals(System.getProperty("legacyconnector.shotScenario"))) {
            // runs the commands of -Dlegacyconnector.shotCommands (separated by ';') and logs the chat
            add(waitForWorld(), wait(40), command("gamemode creative"), command("tp @s 0 150 0"), waitForTerrain(60));
            for (String c0 : System.getProperty("legacyconnector.shotCommands", "legacyconnector mods").split(";")) {
                final String c = c0.trim();
                final String camName = c.startsWith("cam:") ? c.substring(4) : "";
                // "shot:<name>" takes a screenshot (after the world around has had time to render), "wait:<ticks>" waits
                if (c.startsWith("shot:")) add(wait(100), shot(c.substring(5)));
                else if (c.startsWith("wait:")) add(wait(Integer.parseInt(c.substring(5))));
                else if (c.startsWith("cam:")) add(run(mc -> mc.options.setCameraType(camName.equals("front") ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT
                        : camName.equals("back") ? net.minecraft.client.CameraType.THIRD_PERSON_BACK : net.minecraft.client.CameraType.FIRST_PERSON)));
                else add(command(c), wait(30));
            }
            add(run(mc -> {
                LOG.info("Screenshot test finished");
                mc.stop();
            }));
            return;
        }
        if ("tabs".equals(System.getProperty("legacyconnector.shotScenario"))) {
            add(waitForWorld(), wait(60), run(mc -> {
                net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(), true, mc.level.registryAccess());
                for (var tab : net.minecraft.world.item.CreativeModeTabs.allTabs()) {
                    Map<String, Integer> perNamespace = new java.util.TreeMap<>();
                    for (var stack : tab.getDisplayItems()) perNamespace.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace(), 1, Integer::sum);
                    LOG.info("TAB {}: {} item(s) {}", tab.getDisplayName().getString(), tab.getDisplayItems().size(), perNamespace);
                }
                // items of the mods that no tab lists
                Map<String, Integer> missing = new java.util.TreeMap<>();
                var listed = new java.util.HashSet<net.minecraft.world.item.Item>();
                for (var tab : net.minecraft.world.item.CreativeModeTabs.allTabs()) for (var stack : tab.getDisplayItems()) listed.add(stack.getItem());
                for (var e : BuiltInRegistries.ITEM.entrySet()) if (!e.getKey().location().getNamespace().equals("minecraft") && !listed.contains(e.getValue())) missing.merge(e.getKey().location().getNamespace(), 1, Integer::sum);
                LOG.info("TAB TEST: items in no creative tab: {}", missing);
            }), command("gamemode creative"), wait(10));
            String wanted = System.getProperty("legacyconnector.shotMods", "appliedenergistics2");
            int page = 0;
            for (String modWanted : wanted.split(",")) {
                add(run(mc -> {
                    var screen = new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
                    mc.setScreen(screen);
                    for (var tab : net.minecraft.world.item.CreativeModeTabs.allTabs()) {
                        boolean mine = false;
                        for (var stack : tab.getDisplayItems()) if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(modWanted)) { mine = true; break; }
                        if (!mine || tab.getType() == net.minecraft.world.item.CreativeModeTab.Type.SEARCH) continue;
                        try {
                            var m = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
                            m.setAccessible(true);
                            m.invoke(screen, tab);
                            LOG.info("Creative tab shown: {}", tab.getDisplayName().getString());
                        } catch (ReflectiveOperationException e) {
                            LOG.warn("Cannot select the tab: {}", e.toString());
                        }
                        break;
                    }
                }), wait(30), shot("creative_" + modWanted), run(mc -> mc.setScreen(null)));
            }
            add(run(mc -> {
                LOG.info("Screenshot test finished");
                mc.stop();
            }));
            return;
        }
        if ("gallery".equals(System.getProperty("legacyconnector.shotScenario"))) {
            queueGalleryScenario();
            return;
        }
        if ("entities".equals(System.getProperty("legacyconnector.shotScenario"))) {
            queueEntityScenario();
            return;
        }
        if ("village".equals(System.getProperty("legacyconnector.shotScenario"))) {
            queueVillageScenario();
            return;
        }
        if ("gui".equals(System.getProperty("legacyconnector.shotScenario"))) {
            queueGuiScenario();
            return;
        }
        // terrain tour: spectator camera above the surface at spread-out places, four headings and a straight-down shot
        for (int i = 0; i < tour; i++) {
            int x = (int) (Math.cos(i * 1.1) * 220 * (i + 1) / 2);
            int z = (int) (Math.sin(i * 1.1) * 220 * (i + 1) / 2);
            add(run(mc -> {}), command("tp @s " + x + " 200 " + z), wait(100), waitForTerrain(120));
            int idx = i;
            add(run(mc -> {
                int y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                mc.player.connection.sendCommand("tp @s " + x + " " + (y + 6) + " " + z);
            }), wait(60), waitForTerrain(20));
            for (int h = 0; h < 4; h++) add(look(h * 90f, 15f), wait(8), shot("terrain_" + idx + "_h" + h));
            add(look(0f, 90f), wait(8), shot("terrain_" + idx + "_down"));
            add(look(30f, 25f), debug(true), wait(8), shot("terrain_" + idx + "_debug"), debug(false));
            // from far above, straight down: shows how big the biomes are
            add(command("tp @s " + x + " 300 " + z), wait(60), waitForTerrain(40), look(0f, 90f), wait(8), shot("terrain_" + idx + "_overview"));
        }
        // block showcases
        String modsProp = System.getProperty("legacyconnector.shotMods", "");
        List<String> onlyBlocks = java.util.Arrays.asList(System.getProperty("legacyconnector.shotBlocks", "").split(","));
        if (onlyBlocks.size() == 1 && onlyBlocks.get(0).isEmpty()) onlyBlocks = List.of();
        List<String> mods = new ArrayList<>();
        Map<String, List<ResourceLocation>> blocks = new LinkedHashMap<>();
        for (var e : BuiltInRegistries.BLOCK.entrySet()) {
            if (!(e.getValue() instanceof LegacyBlockAdapter)) continue;
            ResourceLocation id = e.getKey().location();
            if (!modsProp.isEmpty() && !java.util.Arrays.asList(modsProp.split(",")).contains(id.getNamespace())) continue;
            if (!onlyBlocks.isEmpty() && !onlyBlocks.contains(id.toString())) continue;
            blocks.computeIfAbsent(id.getNamespace(), k -> new ArrayList<>()).add(id);
        }
        add(command("gamemode creative"), command("tp @s 0 200 0"), wait(60), look(0f, 20f), wait(10), shot("hud_creative"));
        for (var entry : blocks.entrySet()) {
            List<ResourceLocation> ids = entry.getValue();
            for (int start = 0, part = 0; start < Math.min(ids.size(), perModBlocks); start += 12, part++) {
                int from = start;
                int to = Math.min(Math.min(ids.size(), perModBlocks), start + 12);
                String name = "blocks_" + entry.getKey() + "_" + part;
                add(run(mc -> buildShowcase(mc, ids.subList(from, to))), wait(50), look(0f, 20f), wait(15), shot(name));
            }
        }
        // items in the inventory
        Map<String, List<ResourceLocation>> items = new LinkedHashMap<>();
        for (var item : ItemBridge.modItems()) {
            for (var modern : item.mcc$variants.values()) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(modern);
                if (!modsProp.isEmpty() && !java.util.Arrays.asList(modsProp.split(",")).contains(id.getNamespace())) continue;
                items.computeIfAbsent(id.getNamespace(), k -> new ArrayList<>()).add(id);
            }
        }
        for (var entry : items.entrySet()) {
            List<ResourceLocation> ids = entry.getValue();
            int cap = Math.min(ids.size(), perModItems);
            for (int start = 0, part = 0; start < cap; start += 36, part++) {
                int from = start;
                int to = Math.min(cap, start + 36);
                add(command("gamemode survival"), command("clear @s"));
                for (int i = from; i < to; i++) add(command("give @s " + ids.get(i) + " 1"));
                add(wait(10), run(mc -> mc.setScreen(new InventoryScreen(mc.player))), wait(10), shot("items_" + entry.getKey() + "_" + part), run(mc -> mc.setScreen(null)));
            }
        }
        add(run(mc -> {
            LOG.info("Screenshot test finished");
            mc.stop();
        }));
    }

    /** Reflective probe of Twilight Forest's portal conditions on the server thread (development diagnostics). */
    private static void probePortal(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            try {
                var level = server.overworld();
                var world = legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level);
                int x = 40, y = 148, z = 40;
                Class<?> mod = Class.forName("twilightforest.TwilightForestMod");
                Object handler = mod.getField("tickHandler").get(null);
                Object portalItem = handler.getClass().getField("portalItem").get(handler);
                LOG.info("PORTAL PROBE: portalItem={}, allowOther={}", portalItem, mod.getField("allowPortalsInOtherDimensions").get(null));
                Object portal = Class.forName("twilightforest.block.TFBlocks").getField("portal").get(null);
                LOG.info("PORTAL PROBE: portal block {}", portal);
                var list = world.getEntitiesWithinAABB(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem.class,
                        legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB.getBoundingBox(x - 5, y - 5, z - 5, x + 5, y + 5, z + 5));
                LOG.info("PORTAL PROBE: {} item entities near the pool", list.size());
                for (Object o : list) {
                    var item = (legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem) o;
                    var stack = item.getEntityItem();
                    LOG.info("PORTAL PROBE: item {} == portalItem: {}, in water: {}", stack.getItem(), stack.getItem() == portalItem,
                            world.isMaterialInBB(legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$boundingBox(item), legacyconnector.v1_7_10.net.minecraft.block.material.Material.water));
                }
                var m = portal.getClass().getMethod("isGoodPortalPool", legacyconnector.v1_7_10.net.minecraft.world.World.class, int.class, int.class, int.class);
                LOG.info("PORTAL PROBE: isGoodPortalPool={}", m.invoke(portal, world, x, y, z));
                var strict = portal.getClass().getMethod("isGoodPortalPoolStrict", legacyconnector.v1_7_10.net.minecraft.world.World.class, int.class, int.class, int.class);
                LOG.info("PORTAL PROBE: strict={}", strict.invoke(portal, world, x, y, z));
                var sp = server.getPlayerList().getPlayers().get(0);
                var legacyPlayer = org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(sp);
                var check = handler.getClass().getDeclaredMethod("checkForPortalCreation", legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.class,
                        legacyconnector.v1_7_10.net.minecraft.world.World.class, float.class);
                check.setAccessible(true);
                check.invoke(handler, legacyPlayer, world, 32.0F);
                LOG.info("PORTAL PROBE: after checkForPortalCreation the pool block is {} (player at {} {} {})", world.getBlock(x, y, z).getUnlocalizedName(), sp.getX(), sp.getY(), sp.getZ());
                var gb = world.getBlock(x - 1, y, z - 1);
                LOG.info("PORTAL PROBE: ring block {} class {} material {} grass? {} ground? {} sameAsBlocksGrass {}", gb.getUnlocalizedName(), gb.getClass().getName(), gb.getMaterial(),
                        gb.getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.grass,
                        gb.getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.ground, gb == legacyconnector.v1_7_10.net.minecraft.init.Blocks.grass);
                var grassOrDirt = portal.getClass().getDeclaredMethod("isGrassOrDirt", legacyconnector.v1_7_10.net.minecraft.world.World.class, int.class, int.class, int.class);
                var nature = portal.getClass().getDeclaredMethod("isNatureBlock", legacyconnector.v1_7_10.net.minecraft.world.World.class, int.class, int.class, int.class);
                grassOrDirt.setAccessible(true);
                nature.setAccessible(true);
                for (int dx = -1; dx <= 2; dx++) for (int dz = -1; dz <= 2; dz++) {
                    LOG.info("PORTAL PROBE: ({},{}) grassOrDirt={} nature(y+1)={} solid(y-1)={} mat(y+1)={}", dx, dz, grassOrDirt.invoke(portal, world, x + dx, y, z + dz),
                            nature.invoke(portal, world, x + dx, y + 1, z + dz), world.getBlock(x + dx, y - 1, z + dz).getMaterial().isSolid(),
                            world.getBlock(x + dx, y + 1, z + dz).getUnlocalizedName());
                    var below = world.getBlock(x + dx, y - 1, z + dz);
                    var at = world.getBlock(x + dx, y, z + dz);
                    LOG.info("PORTAL PROBE: ({},{}) below={} at={} water={}", dx, dz, below.getUnlocalizedName(), at.getUnlocalizedName(), at.getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.water);
                }
            } catch (Throwable t) {
                LOG.error("PORTAL PROBE failed", t);
            }
        });
    }

    /** Ids from a property; "namespace:all" stands for up to {@code perMod} of that mod's entries spread over its registry. */
    private static <T> List<ResourceLocation> expand(String property, net.minecraft.core.Registry<T> registry, int perMod, boolean legacyBlocksOnly) {
        List<ResourceLocation> out = new ArrayList<>();
        for (String part : System.getProperty(property, "").split(",")) {
            if (part.isEmpty()) continue;
            if (part.endsWith(":all")) {
                String ns = part.substring(0, part.length() - 4);
                List<ResourceLocation> all = new ArrayList<>();
                for (var e : registry.entrySet()) {
                    if (!e.getKey().location().getNamespace().equals(ns)) continue;
                    if (legacyBlocksOnly && !(e.getValue() instanceof LegacyBlockAdapter)) continue;
                    all.add(e.getKey().location());
                }
                int step = Math.max(1, all.size() / perMod);
                for (int i = 0, n = 0; i < all.size() && n < perMod; i += step, n++) out.add(all.get(i));
                LOG.info("Gallery entries of {}: {}", ns, all);
            } else {
                ResourceLocation id = ResourceLocation.tryParse(part);
                if (id != null && registry.containsKey(id)) out.add(id);
            }
        }
        return out;
    }

    private static List<ResourceLocation> idList(String property) {
        List<ResourceLocation> out = new ArrayList<>();
        for (String part : System.getProperty(property, "").split(",")) {
            if (part.isEmpty()) continue;
            ResourceLocation id = ResourceLocation.tryParse(part);
            if (id != null) out.add(id);
        }
        return out;
    }

    /**
     * Pictures for the README: legacy blocks next to modern ones in a small scene, the same blocks seen through Jade's tooltip,
     * legacy items in the inventory beside JEI, and legacy entities beside vanilla ones. SHOW / SHOWITEMS / SHOWENT give the ids.
     */
    private static void queueGalleryScenario() {
        List<ResourceLocation> blocks = expand("legacyconnector.shotBlocks", BuiltInRegistries.BLOCK, 4, true);
        List<ResourceLocation> items = expand("legacyconnector.shotItems", BuiltInRegistries.ITEM, 6, false);
        List<ResourceLocation> entities = expand("legacyconnector.shotEntities", BuiltInRegistries.ENTITY_TYPE, 8, false);
        String[] modern = {"minecraft:diamond_block", "minecraft:crafting_table", "minecraft:furnace", "minecraft:enchanting_table", "minecraft:bookshelf",
                "minecraft:beacon", "minecraft:lantern", "minecraft:oak_log", "minecraft:copper_block", "minecraft:amethyst_block", "minecraft:target", "minecraft:honey_block"};
        int y = 150;
        add(waitForWorld(), wait(40), command("gamemode creative"), command("time set 3000"), command("weather clear"), command("gamerule doDaylightCycle false"),
                command("gamerule doMobSpawning false"), command("tp @s 0 170 0"), waitForTerrain(150));
        add(run(mc -> {
            var c = mc.player.connection;
            c.sendCommand("fill -40 " + (y - 3) + " -14 40 " + (y + 8) + " 24 minecraft:air");
            c.sendCommand("fill -40 " + (y - 3) + " -14 40 " + (y - 2) + " 24 minecraft:dirt");
            c.sendCommand("fill -40 " + (y - 1) + " -14 40 " + (y - 1) + " 24 minecraft:grass_block");
            c.sendCommand("fill -16 " + (y - 1) + " 0 16 " + (y - 1) + " 2 minecraft:dirt_path");
            c.sendCommand("fill -16 " + (y - 1) + " 5 16 " + (y - 1) + " 7 minecraft:dirt_path");
            for (int i = 0; i < blocks.size() && i < 24; i++) {
                int x = -23 + (i % 12) * 4;
                c.sendCommand("setblock " + x + " " + y + " " + (i < 12 ? 1 : -1) + " " + blocks.get(i));
            }
            for (int i = 0; i < modern.length; i++) c.sendCommand("setblock " + (-22 + i * 4) + " " + y + " 6 " + modern[i]);
            c.sendCommand("setblock -12 " + y + " 10 minecraft:oak_leaves");
            c.sendCommand("fill -40 " + y + " 12 40 " + (y + 2) + " 12 minecraft:stone_bricks");
        }), wait(60));
        add(run(mc -> mc.player.connection.sendCommand("tp @s 0 " + (y + 7) + " -12")), wait(30), look(0f, 28f), hideGui(true), wait(40), shot("gallery_blocks"));
        // a close view of the first legacy blocks with Jade's tooltip: the crosshair on one of them
        if (!blocks.isEmpty()) {
            for (int k = 0; k < Math.min(3, blocks.size()); k++) {
                int idx = k * Math.max(1, blocks.size() / 3);
                int bx = -23 + (idx % 12) * 4;
                String name = "gallery_jade_" + k;
                add(hideGui(false), command("gamemode spectator"), run(mc -> mc.player.connection.sendCommand("tp @s " + bx + ".5 " + (y + 0.0) + " -2.5")), wait(40), look(0f, 15.6f), wait(60),
                        run(mc -> LOG.info("Gallery target of {}: {} (player at {})", name, mc.hitResult, mc.player.position())), wait(100), shot(name));
            }
        }
        if (!items.isEmpty()) {
            add(command("gamemode survival"), command("clear @s"));
            for (ResourceLocation id : items) add(command("give @s " + id + " 1"));
            add(hideGui(false), wait(20), run(mc -> mc.setScreen(new InventoryScreen(mc.player))), wait(30), shot("gallery_inventory"), run(mc -> mc.setScreen(null)));
        }
        if (!entities.isEmpty()) {
            add(command("gamemode spectator"), command("difficulty normal"), run(mc -> {
                var c = mc.player.connection;
                c.sendCommand("kill @e[type=!player,distance=..40]");
                c.sendCommand("fill -40 " + y + " 2 40 " + (y + 4) + " 4 minecraft:air");
                LOG.info("Gallery entities: {}", entities);
                for (int i = 0; i < entities.size() && i < 10; i++) c.sendCommand("summon " + entities.get(i) + " " + (-14 + i * 3) + " " + y + " 3 {NoAI:1b,PersistenceRequired:1b}");
                c.sendCommand("summon minecraft:cow 16 " + y + " 3 {NoAI:1b}");
                c.sendCommand("summon minecraft:villager 19 " + y + " 3 {NoAI:1b}");
                c.sendCommand("tp @s 2 " + (y + 4) + " -10");
            }), wait(60), look(0f, 14f), hideGui(true), wait(40), shot("gallery_entities"));
        }
        add(run(mc -> {
            LOG.info("Screenshot test finished");
            mc.stop();
        }));
    }

    /** Summons the legacy entity types of the selected mods in rows on a platform, twelve per picture. */
    private static void queueEntityScenario() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent e) -> {
            if (e.getLevel().isClientSide() || e.getEntity() instanceof net.minecraft.world.entity.player.Player) return;
            var key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getEntity().getType());
            if (!key.getNamespace().equals("minecraft")) LOG.info("Entity left the level: {} reason {} health {}", key, e.getEntity().getRemovalReason(),
                    e.getEntity() instanceof net.minecraft.world.entity.LivingEntity l ? l.getHealth() : -1);
        });
        String modsProp = System.getProperty("legacyconnector.shotMods", "");
        int cap = intProp("legacyconnector.shotBlocksPerMod", 24);
        Map<String, List<ResourceLocation>> byMod = new LinkedHashMap<>();
        for (var e : BuiltInRegistries.ENTITY_TYPE.entrySet()) {
            ResourceLocation id = e.getKey().location();
            if (id.getNamespace().equals("minecraft")) continue;
            if (!modsProp.isEmpty() && !java.util.Arrays.asList(modsProp.split(",")).contains(id.getNamespace())) continue;
            var list = byMod.computeIfAbsent(id.getNamespace(), k -> new ArrayList<>());
            if (list.size() < cap) list.add(id);
        }
        add(waitForWorld(), wait(40), command("gamemode creative"), command("difficulty normal"), command("time set noon"), command("gamerule doMobSpawning false"), command("tp @s 0 200 0"), waitForTerrain(100));
        for (var entry : byMod.entrySet()) {
            List<ResourceLocation> ids = entry.getValue();
            for (int start = 0, part = 0; start < ids.size(); start += 12, part++) {
                List<ResourceLocation> batch = ids.subList(start, Math.min(ids.size(), start + 12));
                String name = "entities_" + entry.getKey() + "_" + part;
                add(command("kill @e[type=!player]"), wait(10), run(mc -> {
                    var c = mc.player.connection;
                    BlockPos base = mc.player.blockPosition();
                    int x0 = base.getX() - 10, y = 150, z0 = base.getZ() + 10;
                    LOG.info("Entity showcase (rows of six, front row first): {}", batch);
                    c.sendCommand("fill " + (x0 - 3) + " " + (y - 1) + " " + (z0 - 3) + " " + (x0 + 26) + " " + (y + 8) + " " + (z0 + 12) + " minecraft:air");
                    c.sendCommand("fill " + (x0 - 3) + " " + (y - 1) + " " + (z0 - 3) + " " + (x0 + 26) + " " + (y - 1) + " " + (z0 + 12) + " minecraft:smooth_stone");
                    var server = mc.getSingleplayerServer();
                    if (server != null) {
                        final int fx0 = x0, fz0 = z0;
                        server.execute(() -> {
                            var level = server.overworld();
                            for (int i = 0; i < batch.size(); i++) {
                                double x = fx0 + (i % 6) * 4 + 0.5, z = fz0 + (i / 6) * 5 + 0.5;
                                try {
                                    var type = BuiltInRegistries.ENTITY_TYPE.get(batch.get(i));
                                    var e = type.create(level);
                                    if (e == null) {
                                        LOG.warn("Entity {} could not be created (null)", batch.get(i));
                                        continue;
                                    }
                                    e.moveTo(x, y, z, 0f, 0f);
                                    if (e instanceof net.minecraft.world.entity.Mob mob) {
                                        mob.setNoAi(true);
                                        mob.setPersistenceRequired();
                                    }
                                    if (!level.addFreshEntity(e)) LOG.warn("Entity {} was not added to the world", batch.get(i));
                                } catch (Throwable t) {
                                    LOG.warn("Entity {} failed to spawn: {}", batch.get(i), t.toString(), t);
                                }
                            }
                        });
                    }
                    c.sendCommand("tp @s " + (x0 + 10) + " " + (y + 4) + " " + (z0 - 9));
                }), wait(40), look(0f, 15f), run(mc -> {
                    var server = mc.getSingleplayerServer();
                    if (server == null) return;
                    server.execute(() -> {
                        var found = new java.util.TreeMap<String, String>();
                        for (var e : server.overworld().getAllEntities()) {
                            if (e instanceof net.minecraft.world.entity.player.Player) continue;
                            var key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
                            if (key.getNamespace().equals("minecraft")) continue;
                            found.put(key.toString(), String.format("%.0f,%.0f,%.0f%s", e.getX(), e.getY(), e.getZ(), e.isInvisible() ? " invisible" : ""));
                        }
                        LOG.info("Entity showcase alive: {}", found);
                    });
                }), wait(10), shot(name));
            }
        }
        add(run(mc -> {
            LOG.info("Screenshot test finished");
            mc.stop();
        }));
    }

    /** Flies over the nearest village and takes pictures of it (legacy village components). */
    private static void queueVillageScenario() {
        add(waitForWorld(), wait(40), command("gamemode spectator"), command("time set noon"));
        int[] at = new int[2];
        add(run(mc -> {
            var server = mc.getSingleplayerServer();
            if (server == null) return;
            var level = server.overworld();
            var found = level.findNearestMapStructure(net.minecraft.tags.StructureTags.VILLAGE, level.getSharedSpawnPos(), 100, false);
            if (found != null) {
                at[0] = found.getX();
                at[1] = found.getZ();
                LOG.info("VILLAGE SHOT: village near {} {}", at[0], at[1]);
            }
        }), wait(20));
        add(run(mc -> mc.player.connection.sendCommand("tp @s " + at[0] + " 130 " + at[1])), wait(200), waitForTerrain(200),
                look(0f, 90f), wait(8), shot("village_down"),
                run(mc -> mc.player.connection.sendCommand("tp @s " + (at[0] - 40) + " 95 " + (at[1] - 40))), wait(120), waitForTerrain(100),
                look(-45f, 25f), wait(8), shot("village_oblique"),
                run(mc -> {
                    LOG.info("Screenshot test finished");
                    mc.stop();
                }));
    }

    /** Right-clicks each legacy block of the selected mods and records which screen opens (custom GUIs). */
    private static void queueGuiScenario() {
        String modsProp = System.getProperty("legacyconnector.shotMods", "");
        String metasProp = System.getProperty("legacyconnector.shotMetas", "0");
        int cap = intProp("legacyconnector.shotBlocksPerMod", 24);
        Map<String, Integer> perMod = new LinkedHashMap<>();
        // (block, metadata) pairs: metadata from the property, or "variants" = the block item's registered subtypes
        List<Object[]> targets = new ArrayList<>();
        for (var e : BuiltInRegistries.BLOCK.entrySet()) {
            if (!(e.getValue() instanceof LegacyBlockAdapter adapter)) continue;
            ResourceLocation id = e.getKey().location();
            if (!modsProp.isEmpty() && !java.util.Arrays.asList(modsProp.split(",")).contains(id.getNamespace())) continue;
            var only = idList("legacyconnector.shotBlocks");
            if (!only.isEmpty() && !only.contains(id)) continue;
            if (perMod.merge(id.getNamespace(), 1, Integer::sum) > cap) continue;
            java.util.TreeSet<Integer> metas = new java.util.TreeSet<>();
            if (metasProp.equals("variants")) {
                metas.add(0);
                try {
                    var item = org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge.fromBlock((legacyconnector.v1_7_10.net.minecraft.block.Block) adapter.legacy());
                    if (item != null) for (int m : item.mcc$variants.keySet()) if (m >= 0 && m < 16) metas.add(m);
                } catch (RuntimeException | LinkageError ex) {
                    // metadata 0 only
                }
            } else {
                for (String m : metasProp.split(",")) metas.add(Integer.parseInt(m.trim()));
            }
            for (int m : metas) targets.add(new Object[] {id, m});
        }
        add(waitForWorld(), wait(40), command("gamemode creative"), command("time set noon"), command("tp @s 0 150 0"), waitForTerrain(100));
        for (Object[] target : targets) {
            ResourceLocation id = (ResourceLocation) target[0];
            int meta = (Integer) target[1];
            String state = id + "[meta=" + meta + "]";
            add(run(mc -> {
                var c = mc.player.connection;
                BlockPos at = mc.player.blockPosition();
                int x = at.getX() + 2, y = at.getY(), z = at.getZ();
                c.sendCommand("setblock " + x + " " + y + " " + z + " minecraft:air");
                c.sendCommand("setblock " + x + " " + y + " " + z + " " + state);
            }), wait(20), run(mc -> {
                BlockPos at = mc.player.blockPosition().offset(2, 0, 0);
                if (mc.level.getBlockEntity(at) instanceof org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockEntity lbe) {
                    var te = lbe.legacy();
                    LOG.info("GUI TEST: {} client state {} tile entity {}", state, mc.level.getBlockState(at), te == null ? null : te.getClass().getName());
                }
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), net.minecraft.core.Direction.UP, at, false);
                var result = mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                LOG.info("GUI TEST: {} use result {}", state, result);
            }), wait(12), run(mc -> {
                var screen = mc.screen;
                if (screen != null) {
                    LOG.info("GUI TEST: {} opened {}", state, screen.getClass().getName());
                    if (screen instanceof LegacyContainerScreen lcs && lcs.legacy() != null) {
                        var g = lcs.legacy();
                        LOG.info("GUI TEST: {} legacy gui {} size {}x{} slots {} screen {}x{}", state, g.getClass().getName(), g.mcc$xSize(), g.mcc$ySize(),
                                g.inventorySlots.inventorySlots.size(), lcs.width, lcs.height);
                    }
                    mc.gui.getChat().clearMessages(false);
                } else {
                    LOG.info("GUI TEST: {} opened no screen", state);
                }
            }), wait(4), run(mc -> {
                if (mc.screen != null) {
                    mc.gui.getChat().clearMessages(false);
                    shotNow(mc, "gui_" + id.getNamespace() + "_" + id.getPath().replace('/', '_') + "_m" + meta);
                    mc.setScreen(null);
                }
            }), wait(2));
        }
        add(run(mc -> {
            LOG.info("Screenshot test finished");
            mc.stop();
        }));
    }

    /**
     * Twilight Forest style portal: a 2x2 pool in a ring of grass, a diamond thrown in, a player standing next to it.
     * Logs the dimension the player ends up in and the blocks of the pool.
     */
    private static void queuePortalScenario() {
        add(run(mc -> {
            var c = mc.player.connection;
            int x = 40, y = 150, z = 40;
            c.sendCommand("gamemode survival");
            c.sendCommand("forceload add " + (x - 8) + " " + (z - 8) + " " + (x + 8) + " " + (z + 8));
            // ground: grass over dirt; the pool is a 2x2 of water sunk into the grass (dirt below, grass all around)
            c.sendCommand("fill " + (x - 6) + " " + (y - 4) + " " + (z - 6) + " " + (x + 8) + " " + (y + 6) + " " + (z + 8) + " minecraft:air");
            c.sendCommand("fill " + (x - 6) + " " + (y - 4) + " " + (z - 6) + " " + (x + 8) + " " + (y - 3) + " " + (z + 8) + " minecraft:dirt");
            c.sendCommand("fill " + (x - 6) + " " + (y - 2) + " " + (z - 6) + " " + (x + 8) + " " + (y - 2) + " " + (z + 8) + " minecraft:grass_block");
            c.sendCommand("fill " + x + " " + (y - 2) + " " + z + " " + (x + 1) + " " + (y - 2) + " " + (z + 1) + " minecraft:water");
            // the shore plants on top of the ring
            c.sendCommand("fill " + (x - 1) + " " + (y - 1) + " " + (z - 1) + " " + (x + 2) + " " + (y - 1) + " " + (z + 2) + " minecraft:poppy");
            c.sendCommand("fill " + x + " " + (y - 1) + " " + z + " " + (x + 1) + " " + (y - 1) + " " + (z + 1) + " minecraft:air");
            c.sendCommand("tp @s " + (x - 4) + " " + y + " " + (z + 1));
        }), wait(60), run(mc -> {
            int x = 40, y = 150, z = 40;
            mc.player.connection.sendCommand("summon minecraft:item " + (x + 1) + " " + (y + 1) + " " + (z + 1) + " {PickupDelay:32767,Item:{id:\"minecraft:diamond\",count:1}}");
        }), wait(120), shot("portal_created"), run(WorldScreenshotTest::probePortal), wait(30), run(mc -> {
            int x = 40, y = 150, z = 40;
            for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++)
                LOG.info("PORTAL TEST: pool block {} is {}", dx + "," + dz, mc.level.getBlockState(new BlockPos(x + dx, y - 2, z + dz)).getBlock());
            mc.player.connection.sendCommand("tp @s " + x + " " + (y - 2) + " " + z);
        }), wait(100), shot("portal_enter"), run(mc -> {
            LOG.info("PORTAL TEST: player dimension {}", mc.level.dimension().location());
            LOG.info("Screenshot test finished");
            mc.stop();
        }));
    }

    /** A stone platform in front of the player with the blocks in rows of six, two blocks apart. */
    private static void buildShowcase(Minecraft mc, List<ResourceLocation> ids) {
        BlockPos base = mc.player.blockPosition();
        int x0 = base.getX() - 6;
        int y = 150;
        int z0 = base.getZ() + 8;
        var conn = mc.player.connection;
        LOG.info("Showcase blocks (rows of six, front row first): {}", ids);
        for (ResourceLocation id : ids) {
            try {
                var b = BuiltInRegistries.BLOCK.get(id);
                if (b instanceof LegacyBlockAdapter adapter && adapter.legacy() instanceof legacyconnector.v1_7_10.net.minecraft.block.Block lb) {
                    int rt = lb.getRenderType();
                    var access = legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(mc.level);
                    LOG.info("Showcase {}: tinted={} colorMultiplier={} renderColor={}", id, org.kylepitcock.legacyconnector.v1_7_10.Assets1710.tinted(lb),
                            Integer.toHexString(lb.colorMultiplier(access, 0, 150, 0)), Integer.toHexString(lb.getRenderColor(0)));
                    for (int side = 0; side < 6; side += 5) {
                        var icon = lb.getIcon(side, 0);
                        LOG.info("Showcase {}: icon(side {}, meta 0) = {}", id, side, icon == null ? "null" : icon.getIconName());
                    }
                    LOG.info("Showcase {}: render type {}, handler {}, shape {}", id, rt,
                            legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.mcc$blockHandler(rt), b.defaultBlockState().getRenderShape());
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.info("Showcase {}: {}", id, e.toString());
            }
        }
        conn.sendCommand("fill " + (x0 - 2) + " " + (y - 1) + " " + (z0 - 2) + " " + (x0 + 14) + " " + (y + 5) + " " + (z0 + 6) + " minecraft:air");
        conn.sendCommand("fill " + (x0 - 2) + " " + (y - 1) + " " + (z0 - 2) + " " + (x0 + 14) + " " + (y - 1) + " " + (z0 + 6) + " minecraft:smooth_stone");
        for (int i = 0; i < ids.size(); i++) {
            int x = x0 + (i % 6) * 2;
            int z = z0 + (i / 6) * 3;
            conn.sendCommand("setblock " + x + " " + y + " " + z + " " + ids.get(i));
        }
        conn.sendCommand("tp @s " + (x0 + 5) + " " + (y + 3) + " " + (z0 - 6));
    }

    /**
     * Creating a world with data packs the game cannot vouch for (the generated legacy world types) shows the
     * "Experimental Settings" warning, a backup/optimise prompt can follow: confirm them like a player pressing Proceed.
     */
    private static void confirmWarnings(Minecraft mc) {
        var screen = mc.screen;
        if (screen == null) return;
        if (!(screen instanceof net.minecraft.client.gui.screens.ConfirmScreen) && !screen.getClass().getSimpleName().contains("Experimental")
                && !screen.getClass().getSimpleName().contains("Backup")) return;
        net.minecraft.client.gui.components.Button chosen = null;
        for (var child : screen.children()) {
            if (!(child instanceof net.minecraft.client.gui.components.Button button)) continue;
            String key = button.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc ? tc.getKey() : "";
            if (key.equals("gui.proceed") || key.equals("gui.yes") || key.contains("experimental") && key.contains("yes")) {
                chosen = button;
                break;
            }
            if (chosen == null) chosen = button;
        }
        if (chosen != null && warned++ % 10 == 0) {
            LOG.info("Confirming the '{}' prompt with '{}'", screen.getClass().getSimpleName(), chosen.getMessage().getString());
        }
        if (chosen != null && chosen.active) chosen.onPress();
    }

    private static int warned;

    private static void shotNow(Minecraft mc, String name) {
        mc.gui.getChat().clearMessages(false);
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(new File(dir, name + ".png"));
        } catch (Exception e) {
            LOG.error("Screenshot {} failed", name, e);
        }
    }

    private static void onTick() {
        if (!started) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        confirmWarnings(mc);
        if (mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen) {
            LOG.error("The game disconnected during the screenshot test");
            shotNow(mc, "disconnected");
            mc.stop();
            return;
        }
        if (++ticks > WATCHDOG_TICKS && !"control".equals(System.getProperty("legacyconnector.shotScenario"))) {
            LOG.error("Screenshot test timed out");
            mc.stop();
            return;
        }
        Action a = QUEUE.peek();
        if (a == null) return;
        try {
            if (a.tick(mc)) QUEUE.poll();
        } catch (RuntimeException e) {
            LOG.error("Screenshot step failed", e);
            QUEUE.poll();
        }
    }
}
