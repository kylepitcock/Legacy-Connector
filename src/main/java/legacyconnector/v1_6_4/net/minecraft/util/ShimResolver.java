package legacyconnector.v1_6_4.net.minecraft.util;

import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge;

/**
 * Resolves the vanilla ids of {@link IdRegistry#vanilla} to the 1.7.10 shim objects (blocks and items by registry name through
 * the bridges, biomes/potions/enchantments by their static fields). Needs the game; not used by the offline tests.
 */
public final class ShimResolver implements IdRegistry.Resolver {
    @Override
    public Object block(String name) {
        return BlockBridge.byName(name);
    }

    @Override
    public Object item(String name) {
        try {
            return ItemBridge.byName(name);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public Object biome(String field) {
        return staticField("legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase", field);
    }

    @Override
    public Object potion(String field) {
        return staticField("legacyconnector.v1_7_10.net.minecraft.potion.Potion", field);
    }

    @Override
    public Object enchantment(String field) {
        return staticField("legacyconnector.v1_7_10.net.minecraft.enchantment.Enchantment", field);
    }

    private static Object staticField(String cls, String field) {
        try {
            return Class.forName(cls).getField(field).get(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }
}
