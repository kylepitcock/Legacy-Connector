package org.kylepitcock.legacyconnector.v1_12_2.client;

import net.minecraft.client.Minecraft;

/**
 * The parts of the 1.12.2 {@code Minecraft} shim that talk to the modern client. Kept out of the shim class: client-only mods call
 * {@code Minecraft.getMinecraft()} on dedicated servers too, and verifying code that hands client classes around would load them.
 */
public final class Minecraft1122 {
    private Minecraft1122() {}

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult objectMouseOver() {
        var hit = mc().hitResult;
        return hit == null ? null : legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult.mcc$of(hit);
    }

    public static void setObjectMouseOver(legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult result) {
        mc().hitResult = result == null ? null : result.mcc$modern();
    }

    public static boolean connected() {
        return mc().getConnection() != null;
    }

    public static boolean integratedServerRunning() {
        return mc().hasSingleplayerServer();
    }

    public static legacyconnector.v1_12_2.net.minecraft.util.Session session() {
        var user = mc().getUser();
        String id = user.getProfileId() == null ? "" : com.mojang.util.UndashedUuid.toString(user.getProfileId());
        return new legacyconnector.v1_12_2.net.minecraft.util.Session(user.getName(), id, user.getAccessToken(),
                user.getType() == null ? "legacy" : user.getType().getName());
    }

    private static net.minecraft.client.multiplayer.ServerData serverSource;
    private static legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData serverData;

    /** The server played on (null in single player), one legacy entry per game entry, refreshed on each call. */
    public static synchronized legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData currentServerData() {
        var modern = mc().getCurrentServer();
        if (modern == null) return null;
        if (modern != serverSource || serverData == null) {
            serverSource = modern;
            serverData = legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData.mcc$of(modern);
        } else {
            serverData.mcc$update(modern);
        }
        return serverData;
    }

    public static void refreshResources() {
        mc().reloadResourcePacks();
    }

    public static void resize() {
        mc().resizeDisplay();
    }

    /** Crashes the game with the report, as 1.12 did on its next frame. */
    public static void crash(legacyconnector.v1_12_2.net.minecraft.crash.CrashReport report) {
        Throwable cause = report.getCrashCause() == null ? new RuntimeException(report.getDescription()) : report.getCrashCause();
        mc().delayCrash(net.minecraft.CrashReport.forThrowable(cause, report.getDescription()));
    }

    /** Which music fits now: credits, the menu, the Nether, the End (with the dragon), creative flight or the game. */
    public static legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker.MusicType ambientMusicType() {
        var mc = mc();
        if (mc.screen instanceof net.minecraft.client.gui.screens.WinScreen) return legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker.MusicType.CREDITS;
        if (mc.player == null || mc.level == null) return legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker.MusicType.MENU;
        var dim = mc.level.dimension();
        if (dim == net.minecraft.world.level.Level.NETHER) return legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker.MusicType.NETHER;
        if (dim == net.minecraft.world.level.Level.END) {
            return mc.gui.getBossOverlay().shouldPlayMusic() ? legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker.MusicType.END_BOSS
                    : legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker.MusicType.END;
        }
        var abilities = mc.player.getAbilities();
        return abilities.instabuild && abilities.mayfly ? legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker.MusicType.CREATIVE
                : legacyconnector.v1_12_2.net.minecraft.client.audio.MusicTicker.MusicType.GAME;
    }

    /** Opens a saved world by folder name; a folder without a world opens the world creation screen. */
    public static void launchIntegratedServer(String folder) {
        var mc = mc();
        if (mc.getLevelSource().levelExists(folder)) {
            mc.createWorldOpenFlows().openWorld(folder, () -> mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen()));
        } else {
            net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.openFresh(mc, mc.screen);
        }
    }

    public static float tickLength() {
        return mc().getTimer().getGameTimeDeltaTicks();
    }

    public static float pausedPartialTicks() {
        return mc().getTimer().getGameTimeDeltaPartialTick(false);
    }

    public static boolean isDemo() {
        return mc().isDemo();
    }

    /** Leaving the world ({@code loadWorld(null)}): back to the title screen. A client world cannot be swapped in. */
    public static void leaveWorld() {
        var mc = mc();
        if (mc.level != null) mc.level.disconnect();
        mc.disconnect(new net.minecraft.client.gui.screens.TitleScreen());
    }

    public static boolean fullscreen() {
        return mc().getWindow().isFullscreen();
    }

    public static void toggleFullscreen() {
        var mc = mc();
        mc.getWindow().toggleFullScreen();
        mc.options.fullscreen().set(mc.getWindow().isFullscreen());
    }

    public static int maxTextureSize() {
        return com.mojang.blaze3d.systems.RenderSystem.maxSupportedTextureSize();
    }

    public static String versionType() {
        return mc().getVersionType();
    }

    /** Connects to a server with the game's connecting screen (GuiConnecting); a failed connection returns to the parent screen. */
    public static void connect(legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen parent, String host,
            legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData server) {
        var mc = mc();
        net.minecraft.client.gui.screens.Screen back = parent == null ? new net.minecraft.client.gui.screens.TitleScreen() : ClientGui1122.host(parent);
        var data = server != null ? server.mcc$modern() : new net.minecraft.client.multiplayer.ServerData(host, host,
                net.minecraft.client.multiplayer.ServerData.Type.OTHER);
        mc.execute(() -> net.minecraft.client.gui.screens.ConnectScreen.startConnecting(back, mc,
                net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(host), data, false, null));
    }

    public static java.io.File resourcePackDir() {
        return mc().getResourcePackDirectory().toFile();
    }
}
