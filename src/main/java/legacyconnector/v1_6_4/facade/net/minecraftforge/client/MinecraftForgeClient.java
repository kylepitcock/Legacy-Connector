package legacyconnector.v1_6_4.facade.net.minecraftforge.client;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.item.Item;

/** Facade of the 1.7.10 shim's {@code MinecraftForgeClient} for 1.6.4 mods: registrations keyed by item id. See {@code Hierarchy.facadeOf}. */
public final class MinecraftForgeClient {
    private MinecraftForgeClient() {}

    public static void registerItemRenderer(int itemId, legacyconnector.v1_7_10.net.minecraftforge.client.IItemRenderer renderer) {
        Item item = Ids.item(itemId);
        if (item != null) legacyconnector.v1_7_10.net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(item, renderer);
    }
}
