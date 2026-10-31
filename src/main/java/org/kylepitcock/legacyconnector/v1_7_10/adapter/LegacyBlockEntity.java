package org.kylepitcock.legacyconnector.v1_7_10.adapter;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.network.NetworkManager;
import legacyconnector.v1_7_10.net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.TileEntityBridge;
import org.slf4j.LoggerFactory;

/**
 * Modern block entity hosting a 1.7.10 {@link TileEntity}. Persists the legacy NBT verbatim under {@code "legacy"},
 * so the legacy tile entity sees exactly the data it wrote, and forwards ticking and description packets.
 */
public class LegacyBlockEntity extends BlockEntity {
    private TileEntity legacy;
    private CompoundTag pending;
    private boolean tickFailed;

    public LegacyBlockEntity(BlockPos pos, BlockState state) {
        super(TileEntityBridge.type(), pos, state);
    }

    /** The legacy tile entity, created on first use (legacy createTileEntity may need the world). */
    public TileEntity legacy() {
        return legacy(null);
    }

    /** As {@link #legacy()}; {@code fallback} is the world to create the tile entity in while this block entity has no level yet (world generation). */
    public TileEntity legacy(World fallback) {
        if (legacy == null && !createFailed) {
            try {
                if (pending != null) {
                    legacy = TileEntityBridge.createAndLoad(new NBTTagCompound(pending));
                    pending = null;
                }
                if (legacy == null && (level != null || fallback != null)) {
                    Block block = (Block) ((LegacyBlockAdapter) getBlockState().getBlock()).legacy();
                    if (!block.hasTileEntity(getBlockState().getValue(LegacyBlockAdapter.META))) return null;
                    legacy = block.createTileEntity(level != null ? World.mcc$wrap(level) : fallback, getBlockState().getValue(LegacyBlockAdapter.META));
                }
            } catch (RuntimeException | LinkageError e) {
                // A tile entity that cannot be created must not take the server down (this runs while chunks are sent
                // and saved): the block stays, without its tile entity.
                createFailed = true;
                legacy = null;
                Object block = getBlockState().getBlock() instanceof LegacyBlockAdapter a ? a.legacy() : getBlockState().getBlock();
                if (CREATE_FAILURES.add(block.getClass().getName())) {
                    org.slf4j.LoggerFactory.getLogger("LegacyConnector/TileEntities").error("Legacy tile entity of {} could not be created; such blocks have none",
                            block.getClass().getName(), e);
                }
            }
            if (legacy != null) attach(legacy);
        }
        return legacy;
    }

    private static final java.util.Set<String> CREATE_FAILURES = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private boolean createFailed;

    /** Whether creating the legacy tile entity threw (as opposed to the block having none for this metadata). */
    public boolean createFailed() {
        return createFailed;
    }

    /**
     * 1.7.10's Chunk.setBlock: when the metadata of the block changes, the tile entity is dropped if it asks to be refreshed, and the
     * block creates the one for the new metadata on next access (a table becoming a research table gets a TileResearchTable).
     */
    public void refreshForMeta(int oldMeta, int newMeta) {
        if (legacy == null || oldMeta == newMeta) return;
        boolean refresh;
        try {
            Block block = (Block) ((LegacyBlockAdapter) getBlockState().getBlock()).legacy();
            refresh = legacy.shouldRefresh(block, block, oldMeta, newMeta, legacy.worldObj, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        } catch (RuntimeException | LinkageError e) {
            refresh = false;
        }
        if (!refresh) return;
        legacy.invalidate();
        legacy = null;
        pending = null;
        createFailed = false;
    }

    /** Client: a metadata change from the server is 1.7.10's client-side setBlock, which refreshes the tile entity as on the server. */
    @Override
    public void setBlockState(BlockState state) {
        BlockState old = getBlockState();
        super.setBlockState(state);
        if (level != null && level.isClientSide && old.getBlock() == state.getBlock() && old.hasProperty(LegacyBlockAdapter.META)
                && state.hasProperty(LegacyBlockAdapter.META)) {
            refreshForMeta(old.getValue(LegacyBlockAdapter.META), state.getValue(LegacyBlockAdapter.META));
        }
    }

    public void replaceLegacy(TileEntity te) {
        if (legacy != null) legacy.invalidate();
        legacy = te;
        if (te != null) attach(te);
        setChanged();
    }

    private void attach(TileEntity te) {
        te.mcc$host = this;
        te.xCoord = worldPosition.getX();
        te.yCoord = worldPosition.getY();
        te.zCoord = worldPosition.getZ();
        if (level != null) te.setWorldObj(World.mcc$wrap(level));
        try {
            te.validate();
        } catch (RuntimeException | LinkageError e) {
            // e.g. multiblock registration using an API this world view lacks: the tile entity still exists and ticks.
            if (CREATE_FAILURES.add("validate:" + te.getClass().getName())) {
                LoggerFactory.getLogger("LegacyConnector/TileEntity").error("Legacy tile entity {} failed in validate()", te.getClass().getName(), e);
            }
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (legacy != null) legacy.setWorldObj(World.mcc$wrap(level));
    }

    private static final java.util.Map<Class<?>, Boolean> UPDATES = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * IndustrialCraft 2's tile entities return false from {@code canUpdate()} (final) and still do all their work in an
     * {@code updateEntity()} that 1.7.10's world called for them: not a single IC2 machine or generator ran. A tile that
     * overrides {@code updateEntity} outside the shim ticks whatever {@code canUpdate()} says; the default one is empty.
     */
    private static boolean overridesUpdate(Class<?> type) {
        return UPDATES.computeIfAbsent(type, c -> {
            for (Class<?> k = c; k != null && !k.getName().startsWith("legacyconnector."); k = k.getSuperclass()) {
                try {
                    k.getDeclaredMethod("updateEntity");
                    return true;
                } catch (NoSuchMethodException ignored) {
                    // keep looking up the hierarchy
                } catch (LinkageError e) {
                    return false;
                }
            }
            return false;
        });
    }

    void tick() {
        if (tickFailed || org.kylepitcock.legacyconnector.core.VirtualLevels.skipTick(level)) return;
        TileEntity te = legacy();
        if (te == null || te.isInvalid() || !(te.canUpdate() || overridesUpdate(te.getClass()))) return;
        try {
            te.updateEntity();
        } catch (RuntimeException | LinkageError e) {
            tickFailed = true;
            LoggerFactory.getLogger("LegacyConnector/TileEntity").error("Legacy tile entity {} at {} crashed while ticking; it will no longer tick",
                    te.getClass().getName(), worldPosition, e);
        }
    }

    @Override
    public void setRemoved() {
        if (legacy != null) legacy.invalidate();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (legacy != null) legacy.onChunkUnload();
        super.onChunkUnloaded();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("legacy")) {
            CompoundTag data = atThisPosition(tag.getCompound("legacy"));
            if (legacy != null) {
                try {
                    legacy.readFromNBT(new NBTTagCompound(data));
                } catch (RuntimeException | LinkageError e) {
                    logNbtFailure("reading", e);
                }
                // readFromNBT set the coordinates from the tag; the tile entity stays where its host is
                legacy.xCoord = worldPosition.getX();
                legacy.yCoord = worldPosition.getY();
                legacy.zCoord = worldPosition.getZ();
            } else {
                pending = data;
            }
        }
    }

    /**
     * The legacy NBT with TileEntity.writeToNBT's "x"/"y"/"z" set to this host's position. They differ when the data was saved at
     * another place: Create contraptions and Sable/Create Aeronautics sub-levels move a block by saving its block entity and loading
     * the tag into a new one at the destination (they fix the modern tag's x/y/z, not the legacy one inside it).
     */
    private CompoundTag atThisPosition(CompoundTag data) {
        if (data.contains("x") && (data.getInt("x") != worldPosition.getX() || data.getInt("y") != worldPosition.getY() || data.getInt("z") != worldPosition.getZ())) {
            data = data.copy();
            data.putInt("x", worldPosition.getX());
            data.putInt("y", worldPosition.getY());
            data.putInt("z", worldPosition.getZ());
        }
        return data;
    }

    private boolean nbtFailureLogged;

    /** A legacy tile entity throwing while its NBT is read or written must not crash the chunk load or save; log it once per block entity. */
    private void logNbtFailure(String what, Throwable e) {
        if (nbtFailureLogged) return;
        nbtFailureLogged = true;
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/TileEntities").warn("{} threw while {} its NBT at {}: {}", legacy == null ? "A tile entity" : legacy.getClass().getName(), what, getBlockPos(), e.toString());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        TileEntity te = legacy();
        if (te != null) {
            NBTTagCompound out = new NBTTagCompound();
            try {
                te.writeToNBT(out);
            } catch (RuntimeException | LinkageError e) {
                logNbtFailure("writing", e);
            }
            tag.put("legacy", out.mcc$modern());
        } else if (pending != null) {
            tag.put("legacy", pending);
        }
    }

    // ------------------------------------------------------------------ client sync (getDescriptionPacket/onDataPacket)

    private CompoundTag descriptionTag() {
        TileEntity te = legacy();
        if (te == null) return null;
        legacyconnector.v1_7_10.net.minecraft.network.Packet p = te.getDescriptionPacket();
        if (p instanceof S35PacketUpdateTileEntity s35 && s35.getNbtCompound() != null) {
            CompoundTag tag = s35.getNbtCompound().mcc$modern().copy();
            tag.putInt("mcc$type", s35.getTileEntityType());
            return tag;
        }
        if (p instanceof legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket fml) {
            // Mods like Iron Chest return a custom-channel packet here; carry its bytes inside the BE update.
            CompoundTag tag = new CompoundTag();
            tag.putString("mcc$channel", fml.channel());
            tag.putByteArray("mcc$fml", fml.mcc$bytes());
            return tag;
        }
        return null;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        CompoundTag tag = descriptionTag();
        return tag == null ? null : ClientboundBlockEntityDataPacket.create(this, (be, reg) -> tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = descriptionTag();
        return tag == null ? new CompoundTag() : tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        deliver(pkt.getTag());
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.isEmpty()) deliver(tag);
    }

    private static final java.util.concurrent.atomic.AtomicInteger DELIVERY_FAILURES = new java.util.concurrent.atomic.AtomicInteger();

    private void deliver(CompoundTag tag) {
        if (tag.contains("mcc$fml")) {
            // The mod's handler finds its tile entity in the client's world by coordinates: for a copy of this block in a moving
            // contraption's render world they are contraption-local and would hit an unrelated block of the real world.
            if (org.kylepitcock.legacyconnector.core.VirtualLevels.isVirtual(level)) return;
            org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge.deliver(tag.getString("mcc$channel"), tag.getByteArray("mcc$fml"),
                    legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side.CLIENT, legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient.INSTANCE);
            return;
        }
        TileEntity te = legacy();
        if (te == null) return;
        int type = tag.getInt("mcc$type");
        CompoundTag copy = tag.copy();
        copy.remove("mcc$type");
        try {
            te.onDataPacket(NetworkManager.MCC_DUMMY, new S35PacketUpdateTileEntity(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), type,
                    new NBTTagCompound(copy)));
        } catch (RuntimeException | LinkageError e) {
            // A mod's packet handler throwing must not disconnect the player ("Network Protocol Error").
            if (DELIVERY_FAILURES.incrementAndGet() <= 10) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/TileEntities").warn("{} failed to handle its update packet", te.getClass().getName(), e);
            }
        }
    }
}
