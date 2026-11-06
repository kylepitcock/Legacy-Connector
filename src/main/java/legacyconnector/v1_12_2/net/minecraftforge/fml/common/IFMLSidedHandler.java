package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;

/** FML's per-side handler (client or dedicated server), what {@code FMLCommonHandler.getSidedDelegate()} answers. */
public interface IFMLSidedHandler {
    MinecraftServer getServer();

    default void haltGame(String message, Throwable exception) {
        throw new RuntimeException(message, exception);
    }

    default void showGuiScreen(Object clientGuiElement) {
    }

    default void queryUser(Object query) {
    }
}
