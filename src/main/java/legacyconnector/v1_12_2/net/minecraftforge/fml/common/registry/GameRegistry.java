package legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.registries.ForgeRegistries;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistryEntry;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.TileEntityBridge1122;

public class GameRegistry {
    public static void registerTileEntity(Class<? extends TileEntity> tileEntityClass, String id) {
        TileEntityBridge1122.registerClass(tileEntityClass, id);
    }

    public static void registerTileEntity(Class<? extends TileEntity> tileEntityClass, ResourceLocation key) {
        TileEntityBridge1122.registerClass(tileEntityClass, key.toString());
        TileEntityBridge1122.registerClass(tileEntityClass, key.getPath());
    }

    @SuppressWarnings("unchecked")
    public static <T extends IForgeRegistryEntry<T>> IForgeRegistry<T> findRegistry(Class<T> registryType) {
        if (registryType == Block.class) return (IForgeRegistry<T>) ForgeRegistries.BLOCKS;
        if (registryType == Item.class) return (IForgeRegistry<T>) ForgeRegistries.ITEMS;
        return null;
    }

    /** Fills a static field with the registry object of that name once the registry events ran. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface ObjectHolder {
        String value();
    }
}
