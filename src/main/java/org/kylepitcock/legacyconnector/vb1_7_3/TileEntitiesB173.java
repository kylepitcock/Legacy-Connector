package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;

/** The one modern block entity type of the b1.7.3 tile entities, valid for every b1.7.3 block with a tile entity. */
public final class TileEntitiesB173 {
    private static final List<LegacyB173EntityBlock> BLOCKS = new ArrayList<>();
    private static BlockEntityType<LegacyB173BlockEntity> type;

    private TileEntitiesB173() {}

    static synchronized void addBlock(LegacyB173EntityBlock block) {
        BLOCKS.add(block);
    }

    public static BlockEntityType<LegacyB173BlockEntity> type() {
        return type;
    }

    /** After the blocks are registered (the type names its blocks). */
    static synchronized void registerType() {
        if (type != null || BLOCKS.isEmpty()) return;
        BlockEntityType<LegacyB173BlockEntity> built = BlockEntityType.Builder.of(LegacyB173BlockEntity::new,
                BLOCKS.toArray(new net.minecraft.world.level.block.Block[0])).build(null);
        LegacyRegistrar.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_tile_b1_7_3"),
                (BlockEntityType<?>) built);
        type = built;
    }
}
