package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122EntityBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tile entity registration ({@code GameRegistry.registerTileEntity}), the host block entity type and tile entity lookup. */
public final class TileEntityBridge1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/TileEntities");
    private static final Map<String, Class<? extends TileEntity>> BY_NAME = new HashMap<>();
    private static final List<Legacy1122EntityBlock> BLOCKS = new ArrayList<>();
    private static BlockEntityType<Legacy1122BlockEntity> type;

    private TileEntityBridge1122() {}

    public static synchronized void registerClass(Class<? extends TileEntity> cls, String name) {
        BY_NAME.put(name, cls);
    }

    public static synchronized void addBlock(Legacy1122EntityBlock block) {
        BLOCKS.add(block);
    }

    public static BlockEntityType<Legacy1122BlockEntity> type() {
        return type;
    }

    /** Registers the host type once all legacy blocks exist (end of legacy loading). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized void registerType() {
        if (type != null) return;
        var blocks = BLOCKS.toArray(new net.minecraft.world.level.block.Block[0]);
        BlockEntityType<Legacy1122BlockEntity> built = BlockEntityType.Builder.of(Legacy1122BlockEntity::new, blocks).build(null);
        LegacyRegistrar.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_tile_1_12_2"),
                (BlockEntityType<?>) built);
        type = built;
    }

    /** A tile entity loaded from saved data: its class is found by the "id" the legacy writeToNBT stored, else the block creates it. */
    public static TileEntity createAndLoad(Block block, World world, IBlockState state, NBTTagCompound tag) {
        TileEntity te = null;
        if (block != null && block.hasTileEntity(state)) te = block.createTileEntity(world, state);
        if (te == null && tag.hasKey("id")) {
            Class<? extends TileEntity> cls = BY_NAME.get(tag.getString("id"));
            if (cls != null) {
                try {
                    var ctor = cls.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    te = ctor.newInstance();
                } catch (ReflectiveOperationException e) {
                    LOG.error("Cannot create the legacy tile entity {}", cls.getName(), e);
                }
            }
        }
        if (te != null) {
            te.setWorld(world);
            te.readFromNBT(tag);
        }
        return te;
    }

    /** The legacy tile entity at a position of a modern level, or null. */
    public static TileEntity legacyAt(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        var be = level.getBlockEntity(pos);
        return be instanceof Legacy1122BlockEntity l ? l.legacy() : null;
    }
}
