package legacyconnector.v1_6_4.facade.net.minecraft.world;

/** Facade of the 1.7.10 shim's {@code WorldServer} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class WorldServer {
    private WorldServer() {}

    /** 1.6.4 {@code saveChunkData()}: writes the chunks that changed to the region files now (a mod saving its data next to the world's). */
    public static void saveChunkData(legacyconnector.v1_7_10.net.minecraft.world.WorldServer self) {
        net.minecraft.server.level.ServerLevel level = org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps.server(self);
        if (level != null) level.getChunkSource().save(false);
    }
}
