package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.IdentityHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;

/** Turns 1.12.2 blocks registered through {@code RegistryEvent.Register<Block>} into modern blocks. */
public final class BlockBridge1122 {
    private static final Map<Block, net.minecraft.world.level.block.Block> MODERN = new IdentityHashMap<>();

    private BlockBridge1122() {}

    static ResourceLocation modernId(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation name) {
        return ResourceLocation.fromNamespaceAndPath(Ids.modernModId(name.getNamespace()), Ids.normalizePath(name.getPath()).replace('/', '_'));
    }

    static void register(Block block) {
        var id = modernId(block.getRegistryName());
        var legacySound = block.getSoundType();
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().sound(legacySound != null && legacySound.mcc$modern != null
                ? legacySound.mcc$modern : sound(block.getMaterialLegacy()));
        float hardness = block.mcc$hardness();
        if (hardness < 0) p.strength(-1.0F, 3600000.0F);
        // 1.12 kept three times the setResistance value and divided by five when an explosion asked (stone: 30 -> 6, modern stone 6)
        else p.strength(hardness, block.mcc$resistance() / 5.0F);
        int light = block.mcc$light();
        if (light > 0) p.lightLevel(s -> light);
        if (block.slipperiness != 0.6F) p.friction(block.slipperiness);
        Material m = block.getMaterialLegacy();
        if (m != null && !m.isOpaque()) p.noOcclusion();
        if (m != null && !m.blocksMovement()) p.noCollission();
        if (m != null) {
            p.pushReaction(m.mcc$pushReaction());
            if (m.getCanBurn()) p.ignitedByLava();
            if (m.isReplaceable()) p.replaceable();
        }
        boolean entity = false;
        try {
            entity = block.hasTileEntity(block.getDefaultState());
        } catch (RuntimeException | LinkageError e) {
            // a block that cannot answer before it has a world has no tile entity
        }
        Legacy1122Block modern;
        if (entity) {
            var eb = new org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122EntityBlock(block, p);
            TileEntityBridge1122.addBlock(eb);
            modern = eb;
        } else {
            modern = new Legacy1122Block(block, p);
        }
        LegacyRegistrar.register(BuiltInRegistries.BLOCK, id, modern);
        block.mcc$modern = modern;
        synchronized (MODERN) {
            MODERN.put(block, modern);
        }
    }

    public static net.minecraft.world.level.block.Block modern(Block block) {
        synchronized (MODERN) {
            return MODERN.get(block);
        }
    }

    private static SoundType sound(Material m) {
        if (m == null) return SoundType.STONE;
        if (m == Material.WOOD) return SoundType.WOOD;
        if (m == Material.GROUND || m == Material.CLAY) return SoundType.GRAVEL;
        if (m == Material.GRASS || m == Material.PLANTS || m == Material.LEAVES || m == Material.VINE) return SoundType.GRASS;
        if (m == Material.SAND) return SoundType.SAND;
        if (m == Material.GLASS || m == Material.ICE || m == Material.PACKED_ICE) return SoundType.GLASS;
        if (m == Material.IRON || m == Material.ANVIL) return SoundType.METAL;
        if (m == Material.CLOTH || m == Material.CARPET) return SoundType.WOOL;
        if (m == Material.SNOW || m == Material.CRAFTED_SNOW) return SoundType.SNOW;
        return SoundType.STONE;
    }
}
