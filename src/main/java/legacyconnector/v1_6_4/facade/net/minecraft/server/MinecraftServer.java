package legacyconnector.v1_6_4.facade.net.minecraft.server;

/** Facade of the 1.7.10 shim's {@code MinecraftServer} for 1.6.4 and older mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class MinecraftServer {
    private MinecraftServer() {}

    /** {@code getServerPort()}: the port the server listens on. */
    public static int getServerPort(legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer self) {
        return self.mcc$modern().getPort();
    }

    /** 1.4.7 and 1.5.2 {@code MinecraftServer.getServerConfigurationManager(server)}: the player list of the server. */
    public static legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager getServerConfigurationManager(
            legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer server) {
        return server.getConfigurationManager();
    }

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("Minecraft-Server");

    /** 1.4.7 and 1.5.2 {@code MinecraftServer.logger}: a java.util.logging logger (the 1.7.10 field is log4j's). */
    public static java.util.logging.Logger mcc$get$logger() {
        return LOGGER;
    }

    /** {@code setDifficultyForAllWorlds(id)}: 0 peaceful to 3 hard. */
    public static void setDifficultyForAllWorlds(legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer self, int difficultyId) {
        self.mcc$modern().setDifficulty(net.minecraft.world.Difficulty.byId(difficultyId), true);
    }

    // ------------------------------------------------------------------ 1.6.4 gap round (2026-11-17)

    /** 1.6.4 {@code getPort()}: the port the server listens on. */
    public static int getPort(legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer self) {
        return self.mcc$modern().getPort();
    }

    /** 1.6.4 {@code getServerMOTD()} (1.7.10: getMOTD). */
    public static String getServerMOTD(legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer self) {
        return self.getMOTD();
    }

    /** 1.6.4 {@code getDifficulty()}: 0 peaceful to 3 hard. */
    public static int getDifficulty(legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer self) {
        return self.mcc$modern().getWorldData().getDifficulty().getId();
    }

    /** 1.6.4 {@code getGameType()}: the game mode new players start in. */
    public static legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType getGameType(legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer self) {
        return legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType.getByID(self.mcc$modern().getDefaultGameType().getId());
    }

    /** 1.6.4 {@code stopServer()} (1.7.10: initiateShutdown): saves and stops. */
    public static void stopServer(legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer self) {
        self.initiateShutdown();
    }

    /** 1.6.4 {@code executeCommand(command)}: runs the command as the console; the output goes to the log, so the returned text is empty. */
    public static String executeCommand(legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer self, String command) {
        self.getCommandManager().executeCommand(self, command);
        return "";
    }
}
