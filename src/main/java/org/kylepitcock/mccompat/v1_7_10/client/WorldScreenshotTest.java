package org.kylepitcock.mccompat.v1_7_10.client;

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
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.mccompat.v1_7_10.bridge.ItemBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graphical test ({@code -Dretrofit.screenshotTest=true}): creates a world, flies around, builds showcase platforms of
 * legacy blocks, fills the inventory with legacy items and writes a screenshot after each step, then quits. The
 * screenshots go to {@code retrofit.screenshotDir} (default {gameDir}/shots), so what the game really draws can be looked at.
 *
 * <p>Properties: {@code retrofit.shotWorldType} (preset id such as biomesoplenty:biomesop), {@code retrofit.shotMods}
 * (comma list; default every legacy mod), {@code retrofit.shotBlocksPerMod} (default 24), {@code retrofit.shotItemsPerMod}
 * (default 36), {@code retrofit.shotTour} (terrain shots, default 6), {@code retrofit.shotSeed}.</p>
 */
public final class WorldScreenshotTest {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/Screenshots");
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
        if (System.getProperty("retrofit.screenshotTest") == null) return;
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
        dir = new File(System.getProperty("retrofit.screenshotDir", new File(mc.gameDirectory, "shots").getPath()));
        dir.mkdirs();
        // The window may not be focused (the harness runs unattended): never pause, never show the tutorial toast.
        mc.options.pauseOnLostFocus = false;
        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
        mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
        mc.options.renderDistance().set(intProp("retrofit.shotRenderDistance", 16));
        String type = System.getProperty("retrofit.shotWorldType", "");
        long seed = Long.parseLong(System.getProperty("retrofit.shotSeed", "20241022"));
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
            state.setName("RetrofitShots" + System.currentTimeMillis());
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
            if (++t[0] < 12) return false;
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
        int tour = intProp("retrofit.shotTour", 6);
        int perModBlocks = intProp("retrofit.shotBlocksPerMod", 24);
        int perModItems = intProp("retrofit.shotItemsPerMod", 36);
        add(waitForWorld(), wait(40),
                command("gamemode spectator"), command("time set noon"), command("weather clear"),
                command("gamerule doDaylightCycle false"), command("gamerule doMobSpawning false"), command("gamerule doWeatherCycle false"),
                command("tp @s 0 180 0"), waitForTerrain(200));
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
        String modsProp = System.getProperty("retrofit.shotMods", "");
        List<String> mods = new ArrayList<>();
        Map<String, List<ResourceLocation>> blocks = new LinkedHashMap<>();
        for (var e : BuiltInRegistries.BLOCK.entrySet()) {
            if (!(e.getValue() instanceof LegacyBlockAdapter)) continue;
            ResourceLocation id = e.getKey().location();
            if (!modsProp.isEmpty() && !java.util.Arrays.asList(modsProp.split(",")).contains(id.getNamespace())) continue;
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

    /** A stone platform in front of the player with the blocks in rows of six, two blocks apart. */
    private static void buildShowcase(Minecraft mc, List<ResourceLocation> ids) {
        BlockPos base = mc.player.blockPosition();
        int x0 = base.getX() - 6;
        int y = 150;
        int z0 = base.getZ() + 8;
        var conn = mc.player.connection;
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
        if (++ticks > WATCHDOG_TICKS) {
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
