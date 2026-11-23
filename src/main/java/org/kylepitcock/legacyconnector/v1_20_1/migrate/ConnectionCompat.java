package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/**
 * Client connection and world-opening calls of 1.20.1 that 1.21.1 reshaped (client only: only translated code that runs on a client calls these):
 * {@code Minecraft.clearLevel}, {@code WorldOpenFlows.loadLevel}, {@code ConnectScreen.startConnecting} (gained a transfer state), the {@code ServerData}
 * constructor (a realm flag became a type), {@code User.getUuid}.
 */
public final class ConnectionCompat {
    private ConnectionCompat() {}

    /** {@code Minecraft.clearLevel()}: the old no-argument overload showed the progress screen while the level is torn down. */
    public static void clearLevel(Minecraft mc) {
        mc.clearClientLevel(new ProgressScreen(true));
    }

    /** {@code Minecraft.is64Bit()}: the JVM's data model (the field of the same answer was dropped). */
    public static boolean is64Bit(Minecraft mc) {
        String model = System.getProperty("sun.arch.data.model");
        return model != null ? model.contains("64") : System.getProperty("os.arch", "").contains("64");
    }

    /** {@code Minecraft.setConnectedToRealms(boolean)}: Realms connection tracking is gone from 1.21.1; nothing reads it, so setting it is inert. */
    public static void setConnectedToRealms(Minecraft mc, boolean connected) {
    }

    /** {@code User.getUuid()}: the profile id as the undashed string the launcher passed (1.21.1 keeps it as a {@code UUID}). */
    public static String getUuid(User user) {
        return user.getProfileId().toString().replace("-", "");
    }

    /** {@code new ServerData(name, ip, isLan)}: the boolean became {@code ServerData.Type} (the 1.20.1 flag meant "LAN", not a realm). */
    public static ServerData newServerData(String name, String ip, boolean lan) {
        return new ServerData(name, ip, lan ? ServerData.Type.LAN : ServerData.Type.OTHER);
    }

    /** {@code ConnectScreen.startConnecting(screen, mc, address, data, quickPlay)}: 1.21.1 also takes the transfer state (none for a normal connection). */
    public static void startConnecting(Screen parent, Minecraft mc, ServerAddress address, ServerData data, boolean quickPlay) {
        ConnectScreen.startConnecting(parent, mc, address, data, quickPlay, null);
    }

    /** {@code WorldOpenFlows.loadLevel(lastScreen, levelId)}: 1.21.1 takes the action for a cancelled backup prompt instead of the screen to return to. */
    public static void loadLevel(WorldOpenFlows flows, Screen lastScreen, String levelId) {
        flows.openWorld(levelId, () -> Minecraft.getInstance().setScreen(lastScreen));
    }
}
