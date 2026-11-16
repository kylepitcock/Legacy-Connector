package legacyconnector.v1_6_4.net.minecraft.src.forge;

import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.WorldProvider;
import legacyconnector.v1_7_10.net.minecraft.world.WorldServer;

/** Forge 3.x's dimension registry: it took the provider's class as an instance; the shim's takes the class. */
public class DimensionManager {
    private DimensionManager() {}

    public static boolean registerDimension(int id, WorldProvider provider, boolean keepLoaded) {
        if (provider == null) return false;
        if (!legacyconnector.v1_7_10.net.minecraftforge.common.DimensionManager.registerProviderType(id, provider.getClass(), keepLoaded)) return false;
        legacyconnector.v1_7_10.net.minecraftforge.common.DimensionManager.registerDimension(id, id);
        return true;
    }

    public static void init() {
        legacyconnector.v1_7_10.net.minecraftforge.common.DimensionManager.init();
    }

    public static WorldProvider getProvider(int id) {
        return legacyconnector.v1_7_10.net.minecraftforge.common.DimensionManager.getProvider(id);
    }

    public static Integer[] getIDs() {
        return legacyconnector.v1_7_10.net.minecraftforge.common.DimensionManager.getIDs();
    }

    public static void setWorld(int id, World world) {}

    public static WorldServer getWorld(int id) {
        return legacyconnector.v1_7_10.net.minecraftforge.common.DimensionManager.getWorld(id);
    }

    public static WorldServer[] getWorlds() {
        return legacyconnector.v1_7_10.net.minecraftforge.common.DimensionManager.getWorlds();
    }

    public static boolean shouldLoadSpawn(int id) {
        return legacyconnector.v1_7_10.net.minecraftforge.common.DimensionManager.shouldLoadSpawn(id);
    }
}
