package legacyconnector.v1_6_4.facade.mcp.mobius.waila.api;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaDataProvider;
import org.kylepitcock.legacyconnector.compat.waila.WailaBridge;

/**
 * Facade of the bridged Waila registrar for 1.4.7 - 1.6.4 mods: Waila of that time registered providers by numeric block id.
 * See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class IWailaRegistrar {
    private IWailaRegistrar() {}

    public static void registerStackProvider(legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaRegistrar self, IWailaDataProvider provider, int blockId) {
        WailaBridge.INSTANCE.registerForBlock(0, provider, Ids.block(blockId));
    }

    public static void registerHeadProvider(legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaRegistrar self, IWailaDataProvider provider, int blockId) {
        WailaBridge.INSTANCE.registerForBlock(1, provider, Ids.block(blockId));
    }

    public static void registerBodyProvider(legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaRegistrar self, IWailaDataProvider provider, int blockId) {
        WailaBridge.INSTANCE.registerForBlock(2, provider, Ids.block(blockId));
    }

    public static void registerTailProvider(legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaRegistrar self, IWailaDataProvider provider, int blockId) {
        WailaBridge.INSTANCE.registerForBlock(3, provider, Ids.block(blockId));
    }

    public static void registerNBTProvider(legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaRegistrar self, IWailaDataProvider provider, int blockId) {
        WailaBridge.INSTANCE.registerForBlock(4, provider, Ids.block(blockId));
    }
}
