package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.ITickable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.TileEntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Modern block entity hosting a 1.12.2 tile entity; the legacy NBT is stored verbatim under "legacy". */
public class Legacy1122BlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/TileEntities");
    private TileEntity legacy;
    private CompoundTag pending;
    private boolean failed;

    public Legacy1122BlockEntity(BlockPos pos, BlockState state) {
        super(TileEntityBridge1122.type(), pos, state);
    }

    /** The legacy tile entity, created on first use (its creation may need the world). */
    public TileEntity legacy() {
        if (legacy == null && !failed && level != null) {
            try {
                var block = StatesBridge1122.legacyBlock(getBlockState());
                var world = WorldBridge1122.wrap(level);
                var state = StatesBridge1122.toLegacy(getBlockState());
                if (pending != null) {
                    legacy = TileEntityBridge1122.createAndLoad(block, world, state, new NBTTagCompound(pending));
                    pending = null;
                } else if (block != null && block.hasTileEntity(state)) {
                    legacy = block.createTileEntity(world, state);
                }
                if (legacy != null) bind(legacy);
            } catch (RuntimeException | LinkageError e) {
                failed = true;
                LOG.error("Legacy tile entity at {} could not be created", getBlockPos(), e);
            }
        }
        return legacy;
    }

    private void bind(TileEntity te) {
        te.mcc$host = this;
        te.setWorld(WorldBridge1122.wrap(level));
        te.setPos(new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()));
        te.validate();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        TileEntity te = legacy();
        if (te != null) {
            try {
                te.onLoad();
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Legacy tile entity {} failed in onLoad: {}", te.getClass().getName(), e.toString());
            }
        }
    }

    @Override
    public void setRemoved() {
        if (legacy != null) {
            try {
                legacy.invalidate();
            } catch (RuntimeException | LinkageError e) {
                // the block entity goes away regardless
            }
        }
        super.setRemoved();
    }

    /** Block events (addBlockEvent: chest lids, pistons) reach the legacy tile entity as 1.12's BlockContainer passed them on. */
    @Override
    public boolean triggerEvent(int id, int type) {
        TileEntity te = legacy();
        if (te != null) {
            try {
                if (te.receiveClientEvent(id, type)) return true;
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Legacy tile entity {} failed in receiveClientEvent: {}", te.getClass().getName(), e.toString());
            }
        }
        return super.triggerEvent(id, type);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        TileEntity te = legacy;
        if (te != null) {
            try {
                te.onChunkUnload();
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Legacy tile entity {} failed in onChunkUnload: {}", te.getClass().getName(), e.toString());
            }
        }
    }

    public void tick() {
        TileEntity te = legacy();
        if (te instanceof ITickable t && !te.isInvalid()) {
            try {
                t.update();
            } catch (RuntimeException | LinkageError e) {
                if (!failed) LOG.error("Legacy tile entity {} failed while ticking; it stops ticking", te.getClass().getName(), e);
                failed = true;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        TileEntity te = legacy;
        if (te != null) {
            try {
                tag.put("legacy", te.writeToNBT(new NBTTagCompound()).mcc$modern());
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Legacy tile entity {} failed to save: {}", te.getClass().getName(), e.toString());
            }
        } else if (pending != null) {
            tag.put("legacy", pending);
        }
    }

    /** What the client is told about this tile entity: the legacy update tag (its own idea of what the client needs). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        TileEntity te = legacy();
        if (te != null) {
            try {
                tag.put("legacy", te.getUpdateTag().mcc$modern());
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Legacy tile entity {} failed to build its update tag: {}", te.getClass().getName(), e.toString());
            }
        }
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("legacy")) {
            pending = tag.getCompound("legacy");
            legacy = null;
            failed = false;
        }
    }
}
