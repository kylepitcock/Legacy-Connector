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

    /** {@code setDifficultyForAllWorlds(id)}: 0 peaceful to 3 hard. */
    public static void setDifficultyForAllWorlds(legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer self, int difficultyId) {
        self.mcc$modern().setDifficulty(net.minecraft.world.Difficulty.byId(difficultyId), true);
    }
}
