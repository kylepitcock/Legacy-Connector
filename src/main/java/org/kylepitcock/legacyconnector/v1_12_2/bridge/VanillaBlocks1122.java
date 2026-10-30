package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.core.flatten.VanillaFlattening;

/**
 * 1.12.2 vanilla blocks and items as instances of their own legacy classes ({@code Blocks.SAND} is a {@code BlockSand}, so
 * {@code withProperty(BlockSand.VARIANT, RED_SAND)} and the metadata it saves are real): bound to the modern block of their
 * metadata 0 through the game's flattening table, with the modern block's hardness, resistance and light.
 * {@link StatesBridge1122} maps their states to modern ones by metadata.
 */
public final class VanillaBlocks1122 {
    private VanillaBlocks1122() {}

    /** 1.12 registry paths that are not the lower-case field name. */
    private static final Map<String, String> PATHS = Map.of("OAK_DOOR", "wooden_door");

    public static String legacyPath(String field) {
        return PATHS.getOrDefault(field, field.toLowerCase(java.util.Locale.ROOT));
    }

    /** Binds a legacy vanilla block made by {@code Blocks} to the modern block of the field and registers it under its 1.12 name. */
    public static <T extends Block> T block(String field, T b) {
        String path = legacyPath(field);
        // a block shared by two constants (EnumFlowerColor.getBlock() hands out RED_FLOWER) is named once
        if (b.getRegistryName() == null) b.setRegistryName("minecraft", path);
        net.minecraft.world.level.block.Block modern = null;
        int id = VanillaFlattening.blockId(path);
        if (id >= 0) modern = VanillaFlattening.state(id, 0).getBlock();
        if (modern == null || modern == net.minecraft.world.level.block.Blocks.AIR && !path.equals("air")) {
            ResourceLocation rl = ResourceLocation.withDefaultNamespace(path);
            if (BuiltInRegistries.BLOCK.containsKey(rl)) modern = BuiltInRegistries.BLOCK.get(rl);
        }
        if (modern != null) {
            b.mcc$modern = modern;
            try {
                b.setHardness(modern.defaultDestroyTime());
                b.setResistance(modern.getExplosionResistance() / 3.0F);
                b.setLightLevel(modern.defaultBlockState().getLightEmission() / 15.0F);
            } catch (RuntimeException e) {
                // keeps the class's own values
            }
            reverse(modern, b);
        }
        Registries1122.BLOCKS.registerVanilla(b);
        return b;
    }

    /** Binds a legacy vanilla item made by {@code Items} to the modern item of its name and registers it. */
    public static <T extends Item> T item(String field, String modernPath, T i) {
        String path = field.equals("POTIONITEM") ? "potion" : field.toLowerCase(java.util.Locale.ROOT);
        i.setRegistryName("minecraft", path);
        ResourceLocation rl = ResourceLocation.withDefaultNamespace(modernPath == null ? path : modernPath);
        if (BuiltInRegistries.ITEM.containsKey(rl)) {
            var modern = BuiltInRegistries.ITEM.get(rl);
            i.mcc$modern = modern;
            i.mcc$vanillaModern = modern;
            reverseItem(modern, i);
        }
        Registries1122.ITEMS.registerVanilla(i);
        return i;
    }

    /**
     * Makes this legacy block the one the modern block reads back as (Vanilla1122's reverse map is private: it is filled through
     * reflection until that class offers a registration method). A plain wrapper made before is replaced; a typed one stays.
     */
    @SuppressWarnings("unchecked")
    private static void reverse(net.minecraft.world.level.block.Block modern, Block b) {
        try {
            var f = Vanilla1122.class.getDeclaredField("BLOCK_REVERSE");
            f.setAccessible(true);
            synchronized (Vanilla1122.class) {
                var map = (Map<net.minecraft.world.level.block.Block, Block>) f.get(null);
                Block old = map.get(modern);
                if (old == null || old instanceof Vanilla1122.VanillaBlock) map.put(modern, b);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // the plain wrapper stays the reverse mapping
        }
    }

    @SuppressWarnings("unchecked")
    private static void reverseItem(net.minecraft.world.item.Item modern, Item i) {
        try {
            var f = Vanilla1122.class.getDeclaredField("ITEM_REVERSE");
            f.setAccessible(true);
            synchronized (Vanilla1122.class) {
                var map = (Map<net.minecraft.world.item.Item, Item>) f.get(null);
                Item old = map.get(modern);
                if (old == null || old instanceof Vanilla1122.VanillaItem || old instanceof Vanilla1122.VanillaItemBlock) map.put(modern, i);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // the plain wrapper stays the reverse mapping
        }
    }
}
