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

    /** {@code World.setTileEntity}: this block entity now hosts the given tile entity. */
    public void mcc$setLegacy(TileEntity te) {
        if (te == null || te == legacy) return;
        if (legacy != null) {
            try {
                legacy.invalidate();
            } catch (RuntimeException | LinkageError e) {
                // replaced regardless
            }
        }
        legacy = te;
        failed = false;
        pending = null;
        bind(te);
        setChanged();
    }

    /** The data of a tile entity a generator filled before the block existed (a stand-in) becomes this one's. */
    public void mcc$loadFrom(TileEntity standIn) {
        CompoundTag tag = standIn.writeToNBT(new NBTTagCompound()).mcc$modern().copy();
        tag.putInt("x", worldPosition.getX());
        tag.putInt("y", worldPosition.getY());
        tag.putInt("z", worldPosition.getZ());
        legacy = null;
        failed = false;
        pending = tag;
        setChanged();
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
        if (org.kylepitcock.legacyconnector.core.VirtualLevels.skipTick(level)) return;
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

    /**
     * The tile entity's own update packet when it makes one (its data and type travel in the modern block entity packet and reach
     * its {@code onDataPacket} on the client), else the update tag.
     */
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        TileEntity te = legacy();
        if (te != null) {
            try {
                var packet = te.getUpdatePacket();
                if (packet != null) {
                    CompoundTag tag = new CompoundTag();
                    tag.putInt("mcc$type", packet.getTileEntityType());
                    if (packet.getNbtCompound() != null) tag.put("mcc$packet", packet.getNbtCompound().mcc$modern().copy());
                    return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this, (be, reg) -> tag);
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Legacy tile entity {} failed to build its update packet: {}", te.getClass().getName(), e.toString());
            }
        }
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    private static final java.util.concurrent.atomic.AtomicInteger DELIVERY_FAILURES = new java.util.concurrent.atomic.AtomicInteger();

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,
            HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (!tag.contains("mcc$type")) {
            super.onDataPacket(net, pkt, registries);
            return;
        }
        TileEntity te = legacy();
        if (te == null) return;
        var pos = new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        NBTTagCompound data = tag.contains("mcc$packet") ? new NBTTagCompound(tag.getCompound("mcc$packet")) : null;
        try {
            te.onDataPacket(legacyconnector.v1_12_2.net.minecraft.network.NetworkManager.mcc$of(net),
                    new legacyconnector.v1_12_2.net.minecraft.network.play.server.SPacketUpdateTileEntity(pos, tag.getInt("mcc$type"), data));
        } catch (RuntimeException | LinkageError e) {
            // a mod's packet handler throwing must not disconnect the player
            if (DELIVERY_FAILURES.incrementAndGet() <= 10) LOG.warn("{} failed to handle its update packet", te.getClass().getName(), e);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("legacy")) {
            pending = tag.getCompound("legacy");
            // Saved at another position (Create contraptions and Sable/Create Aeronautics sub-levels load a moved block's tag into a new
            // block entity, fixing only the modern x/y/z): readFromNBT must see where the tile entity is now.
            if (pending.contains("x") && (pending.getInt("x") != worldPosition.getX() || pending.getInt("y") != worldPosition.getY()
                    || pending.getInt("z") != worldPosition.getZ())) {
                pending = pending.copy();
                pending.putInt("x", worldPosition.getX());
                pending.putInt("y", worldPosition.getY());
                pending.putInt("z", worldPosition.getZ());
            }
            legacy = null;
            failed = false;
        }
    }
}
