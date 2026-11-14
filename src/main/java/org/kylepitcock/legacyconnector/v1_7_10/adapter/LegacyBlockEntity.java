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
public class LegacyBlockEntity extends BlockEntity implements org.kylepitcock.legacyconnector.core.perf.StaticTickers.Host {
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
                    if (!block.hasTileEntity(LegacyBlockAdapter.metaOf(getBlockState()))) return null;
                    legacy = block.createTileEntity(level != null ? World.mcc$wrap(level) : fallback, LegacyBlockAdapter.metaOf(getBlockState()));
                }
            } catch (RuntimeException | LinkageError | AssertionError e) {
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
        invalidateLegacy(legacy);
        legacy = null;
        pending = null;
        createFailed = false;
        tickerMayChange();
    }

    /** Client: a metadata change from the server is 1.7.10's client-side setBlock, which refreshes the tile entity as on the server. */
    @Override
    public void setBlockState(BlockState state) {
        BlockState old = getBlockState();
        super.setBlockState(state);
        // the same legacy block: one modern block, or two flattened subtype blocks of it (a metadata change across subtypes)
        boolean sameLegacyBlock = old.getBlock() instanceof LegacyBlockAdapter && LegacyBlockAdapter.sameFamily(old.getBlock(), state.getBlock());
        int oldMeta = LegacyBlockAdapter.metaOf(old), newMeta = LegacyBlockAdapter.metaOf(state);
        if (level != null && level.isClientSide && sameLegacyBlock) {
            refreshForMeta(oldMeta, newMeta);
        }
        // 1.7.10's Chunk.setBlockMetadata told a kept tile entity its new metadata (updateContainingBlockInfo, blockMetadata = meta);
        // a cached getBlockMetadata() otherwise stays at the value read first. Witchery's fume funnel caches 0 before its facing
        // is set on placement and never draws the pipes to the oven beside it.
        if (legacy != null && sameLegacyBlock && oldMeta != newMeta) {
            try {
                legacy.updateContainingBlockInfo();
            } catch (RuntimeException | LinkageError ignored) {
                // a mod's override that needs more world than it has here
            }
            legacy.blockMetadata = newMeta;

        }
    }

    public void replaceLegacy(TileEntity te) {
        if (legacy != null) invalidateLegacy(legacy);
        legacy = te;
        if (te != null) attach(te);
        setChanged();
        tickerMayChange();
    }

    private void attach(TileEntity te) {
        te.mcc$host = this;
        te.xCoord = worldPosition.getX();
        te.yCoord = worldPosition.getY();
        te.zCoord = worldPosition.getZ();
        try {
            if (level != null) te.setWorldObj(World.mcc$wrap(level));
        } catch (RuntimeException | LinkageError e) {
            // setWorldObj runs mod code (MineFactory Reloaded's grinder builds a fake WorldServer there; a missing constructor was a
            // NoSuchMethodError raised while saving a chunk and took the whole server down): the tile entity keeps running without it
            if (CREATE_FAILURES.add("setWorldObj:" + te.getClass().getName())) {
                LoggerFactory.getLogger("LegacyConnector/TileEntity").error("Legacy tile entity {} failed in setWorldObj()", te.getClass().getName(), e);
            }
        }
        try {
            te.validate();
        } catch (RuntimeException | LinkageError e) {
            // e.g. multiblock registration using an API this world view lacks: the tile entity still exists and ticks.
            if (CREATE_FAILURES.add("validate:" + te.getClass().getName())) {
                LoggerFactory.getLogger("LegacyConnector/TileEntity").error("Legacy tile entity {} failed in validate()", te.getClass().getName(), e);
            }
        }
        try {
            te.cofh_validate();
        } catch (RuntimeException | LinkageError e) {
            if (CREATE_FAILURES.add("cofh_validate:" + te.getClass().getName())) {
                LoggerFactory.getLogger("LegacyConnector/TileEntity").error("Legacy tile entity {} failed in cofh_validate()", te.getClass().getName(), e);
            }
        }
    }

    /** {@code invalidate()}, then the hook CoFH Core's patched world called when a tile entity left its list. */
    private static void invalidateLegacy(TileEntity te) {
        te.invalidate();
        try {
            te.cofh_invalidate();
        } catch (RuntimeException | LinkageError e) {
            if (CREATE_FAILURES.add("cofh_invalidate:" + te.getClass().getName())) {
                LoggerFactory.getLogger("LegacyConnector/TileEntity").error("Legacy tile entity {} failed in cofh_invalidate()", te.getClass().getName(), e);
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

    // ------------------------------------------------------------------ perf.staticTileEntities

    /** Whether {@link #tickedTile} ticks; decided once per tile entity, as 1.7.10 decided when it added the tile entity to the world. */
    private boolean tileTicks;
    /** The tile entity {@link #tileTicks} was decided for (null: not yet). */
    private TileEntity tickedTile;
    /** The chunk was told to drop this host's ticker. */
    private boolean tickerDropped;

    /** Classes whose {@code updateEntity()} is the shim's empty TileEntity one: ticking them did nothing in 1.7.10 either. */
    private static final ClassValue<Boolean> EMPTY_UPDATE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("updateEntity").getDeclaringClass() == TileEntity.class;
            } catch (NoSuchMethodException | LinkageError | SecurityException e) {
                return false;
            }
        }
    };

    /** Whether 1.7.10 would have ticked this tile entity at all (Forge's canUpdate(), or the IC2 rule of {@link #overridesUpdate}). */
    private static boolean ticks(TileEntity te) {
        if (EMPTY_UPDATE.get(te.getClass())) return false;
        try {
            return te.canUpdate() || overridesUpdate(te.getClass());
        } catch (RuntimeException | LinkageError e) {
            return true;
        }
    }

    @Override
    public boolean legacyconnector$isStatic() {
        if (tickFailed) return true; // crashed while ticking: never ticks again
        if (createFailed && legacy == null) return true; // no tile entity will ever be made (until the metadata changes)
        return legacy != null && legacy == tickedTile && !tileTicks;
    }

    /** The tile entity changed: a ticker dropped for the old one is restored (the chunk asks {@link #legacyconnector$isStatic} again). */
    private void tickerMayChange() {
        if (tickerDropped && !legacyconnector$isStatic()) {
            tickerDropped = false;
            org.kylepitcock.legacyconnector.core.perf.StaticTickers.refresh(this);
        }
    }

    private void dropTicker() {
        if (tickerDropped) return;
        tickerDropped = true;
        org.kylepitcock.legacyconnector.core.perf.StaticTickers.refresh(this);
    }

    void tick() {
        if (tickFailed || org.kylepitcock.legacyconnector.core.VirtualLevels.skipTick(level)) return;
        if (org.kylepitcock.legacyconnector.core.perf.Perf.STATIC_TILE_ENTITIES) {
            TileEntity te = legacy();
            if (te == null) {
                if (createFailed) dropTicker();
                return;
            }
            if (te != tickedTile) {
                tickedTile = te;
                tileTicks = ticks(te);
            }
            if (!tileTicks) {
                dropTicker();
                return;
            }
            if (te.isInvalid()) return;
            update(te);
            return;
        }
        TileEntity te = legacy();
        if (te == null || te.isInvalid() || !(te.canUpdate() || overridesUpdate(te.getClass()))) return;
        update(te);
    }

    private void update(TileEntity te) {
        try {
            te.updateEntity();
        } catch (RuntimeException | LinkageError e) {
            tickFailed = true;
            LoggerFactory.getLogger("LegacyConnector/TileEntity").error("Legacy tile entity {} at {} crashed while ticking; it will no longer tick",
                    te.getClass().getName(), worldPosition, e);
            if (org.kylepitcock.legacyconnector.core.perf.Perf.STATIC_TILE_ENTITIES) dropTicker();
        }
    }

    @Override
    public void setRemoved() {
        if (legacy != null) invalidateLegacy(legacy);
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
        legacyconnector.v1_7_10.net.minecraft.network.Packet p;
        try {
            p = te.getDescriptionPacket();
        } catch (RuntimeException | LinkageError e) {
            // a tile that was never given its state (placed by setBlock, not by its item) can throw here; that must not crash the chunk broadcast
            logNbtFailure("describing", e);
            return null;
        }
        if (p instanceof S35PacketUpdateTileEntity s35 && s35.getNbtCompound() != null) {
            CompoundTag tag = s35.getNbtCompound().mcc$modern().copy();
            tag.putInt("mcc$type", s35.getTileEntityType());
            // 1.6.4's Packet132TileEntityData goes to the tile's 1.6.4 onDataPacket (see Tiles164)
            if (s35 instanceof legacyconnector.v1_6_4.net.minecraft.network.packet.Packet132TileEntityData) tag.putBoolean("mcc$164", true);
            return tag;
        }
        if (p instanceof legacyconnector.v1_6_4.net.minecraft.network.packet.Packet250CustomPayload payload164) {
            // 1.6.4 tiles answer with a custom payload packet: carried like a 1.7.10 custom-channel one
            CompoundTag tag = new CompoundTag();
            tag.putString("mcc$channel", payload164.channel == null ? "" : payload164.channel);
            tag.putByteArray("mcc$fml", payload164.mcc$bytes());
            return tag;
        }
        if (p instanceof legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket fml) {
            // Mods like Iron Chest return a custom-channel packet here; carry its bytes inside the BE update.
            CompoundTag tag = new CompoundTag();
            tag.putString("mcc$channel", fml.channel());
            tag.putByteArray("mcc$fml", fml.mcc$bytes());
            return tag;
        }
        if (p != null) {
            // A mod's own Packet subclass (1.4.7 RedPower's Packet211TileDesc, registered with Packet.addIdClassMapping): its legacy wire format
            // (id byte, data) goes along and the client rebuilds it and runs its processPacket, which finds this tile by coordinates.
            try {
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                legacyconnector.v1_7_10.net.minecraft.network.Packet.writePacket(p, new java.io.DataOutputStream(bytes));
                CompoundTag tag = new CompoundTag();
                tag.putByteArray("mcc$wire", bytes.toByteArray());
                return tag;
            } catch (java.io.IOException | RuntimeException | LinkageError e) {
                logNbtFailure("describing", e);
            }
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
        if (tag.contains("mcc$wire")) {
            if (org.kylepitcock.legacyconnector.core.VirtualLevels.isVirtual(level)) return;
            legacy(); // the mod's packet finds the tile by coordinates: it must exist in the client's world
            try {
                var packet = legacyconnector.v1_6_4.forward.Packets164.readPacket(null, new java.io.DataInputStream(new java.io.ByteArrayInputStream(tag.getByteArray("mcc$wire"))),
                        false, null);
                if (packet != null) packet.processPacket(legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient.INSTANCE);
            } catch (java.io.IOException | RuntimeException | LinkageError e) {
                if (DELIVERY_FAILURES.incrementAndGet() <= 10) {
                    org.slf4j.LoggerFactory.getLogger("LegacyConnector/TileEntities").warn("{} failed to handle its description packet", getClass().getName(), e);
                }
            }
            return;
        }
        TileEntity te = legacy();
        if (te == null) return;
        int type = tag.getInt("mcc$type");
        CompoundTag copy = tag.copy();
        copy.remove("mcc$type");
        boolean legacy164 = copy.getBoolean("mcc$164");
        copy.remove("mcc$164");
        try {
            if (legacy164 && org.kylepitcock.legacyconnector.v1_6_4.bridge.Tiles164.deliver(te, worldPosition.getX(), worldPosition.getY(),
                    worldPosition.getZ(), type, new NBTTagCompound(copy))) {
                return;
            }
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
