package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
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
public class Legacy1122BlockEntity extends net.minecraft.world.level.block.entity.BlockEntity
        implements org.kylepitcock.legacyconnector.core.perf.StaticTickers.Host {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/TileEntities");
    private TileEntity legacy;
    private CompoundTag pending;
    private boolean failed;

    public Legacy1122BlockEntity(BlockPos pos, BlockState state) {
        super(TileEntityBridge1122.type(), pos, state);
    }

    /**
     * The legacy state of the {@code World.setBlockState} that is placing a block right now. A modern state keeps only the block's meta, but 1.12 asked
     * the tile entity of the block for the full state (a colour property that is not part of the meta: Colored Redstone's dust is told its colour that way).
     */
    private static final ThreadLocal<IBlockState> PLACING = new ThreadLocal<>();
    private IBlockState placedState;

    public static void mcc$placing(IBlockState state) {
        if (state == null) PLACING.remove();
        else PLACING.set(state);
    }

    /** The state being placed if it is one of {@code block}, else null. */
    public static IBlockState mcc$placingOf(legacyconnector.v1_12_2.net.minecraft.block.Block block) {
        IBlockState s = PLACING.get();
        return s != null && s.getBlock() == block ? s : null;
    }

    /** Called when the block creates this entity: takes over the state being placed, if it is a state of this entity's block. */
    public void mcc$adoptPlacing() {
        IBlockState s = PLACING.get();
        if (s != null && s.getBlock() == StatesBridge1122.legacyBlock(getBlockState())) placedState = s;
    }

    private static final java.util.Set<String> MISSING_REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** The legacy tile entity, created on first use (its creation may need the world). */
    public TileEntity legacy() {
        if (legacy == null && !failed && level != null) create(WorldBridge1122.wrap(level));
        return legacy;
    }

    /**
     * The tile entity of a block a legacy generator has just placed in a chunk still being generated (no level yet), made with the
     * generator's world: generators fill it right away (Techguns' structure spawners get their mobs and limits, dungeon chests their loot),
     * as 1.12 made it on the spot. It moves to the real level when the chunk is finished ({@link #setLevel}).
     */
    public TileEntity mcc$legacyForGeneration(legacyconnector.v1_12_2.net.minecraft.world.World world) {
        if (level != null) return legacy();
        if (legacy == null && !failed && world != null) create(world);
        return legacy;
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        if (legacy != null) legacy.setWorld(WorldBridge1122.wrap(level));
    }

    private void create(legacyconnector.v1_12_2.net.minecraft.world.World world) {
        {
            try {
                var block = StatesBridge1122.legacyBlock(getBlockState());
                var state = StatesBridge1122.toLegacy(getBlockState());
                if (pending != null) {
                    legacy = TileEntityBridge1122.createAndLoad(block, world, state, new NBTTagCompound(pending));
                    pending = null;
                } else if (block != null && block.hasTileEntity(placedState != null ? placedState : state)) {
                    legacy = block.createTileEntity(world, placedState != null ? placedState : state);
                }
                placedState = null;
                if (legacy != null) bind(legacy, world);
            } catch (RuntimeException | LinkageError e) {
                failed = true;
                if (e instanceof NoClassDefFoundError missing) {
                    // a tile entity needing an absent optional mod (NTM-CE's OpenComputers cable without OpenComputers): the block stays
                    // inert, named once per missing class instead of a stack trace per placement
                    if (MISSING_REPORTED.add(String.valueOf(missing.getMessage()))) {
                        LOG.warn("Legacy tile entity at {} needs {}, which is not installed; the block stays inert", getBlockPos(), missing.getMessage());
                    }
                } else {
                    LOG.error("Legacy tile entity at {} could not be created", getBlockPos(), e);
                }
            }
        }
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
        bind(te, level == null ? null : WorldBridge1122.wrap(level));
        setChanged();
        tickerMayChange();
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
        tickerMayChange();
    }

    private void bind(TileEntity te, legacyconnector.v1_12_2.net.minecraft.world.World world) {
        te.mcc$host = this;
        if (world != null) te.setWorld(world);
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

    // ------------------------------------------------------------------ perf.staticTileEntities

    /** The tile entity whose update() threw (it stops ticking, as the log says). */
    private TileEntity tickFailed;
    /** The chunk was told to drop this host's ticker. */
    private boolean tickerDropped;

    /** 1.12.2's World ticked only ITickable tile entities: any other one (or one that crashed while ticking) needs no ticker. */
    @Override
    public boolean legacyconnector$isStatic() {
        TileEntity te = legacy;
        if (te == null) return failed && pending == null; // creation failed: none will be made
        return !(te instanceof ITickable) || te == tickFailed;
    }

    /** The tile entity changed: a ticker dropped for the old one is restored (the chunk asks {@link #legacyconnector$isStatic} again). */
    private void tickerMayChange() {
        if (tickerDropped && !legacyconnector$isStatic()) {
            tickerDropped = false;
            org.kylepitcock.legacyconnector.core.perf.StaticTickers.refresh(this);
        }
    }

    public void tick() {
        if (org.kylepitcock.legacyconnector.core.VirtualLevels.skipTick(level)) return;
        TileEntity te = legacy();
        boolean perf = org.kylepitcock.legacyconnector.core.perf.Perf.STATIC_TILE_ENTITIES;
        if (perf && (te == null ? failed : !(te instanceof ITickable) || te == tickFailed)) {
            if (!tickerDropped) {
                tickerDropped = true;
                org.kylepitcock.legacyconnector.core.perf.StaticTickers.refresh(this);
            }
            return;
        }
        if (te instanceof ITickable t && !te.isInvalid() && !(perf && te == tickFailed)) {
            try {
                t.update();
            } catch (RuntimeException | LinkageError e) {
                if (!failed) LOG.error("Legacy tile entity {} failed while ticking; it stops ticking", te.getClass().getName(), e);
                failed = true;
                tickFailed = te;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        TileEntity te = legacy;
        if (te != null) {
            // a throwing writeNBT (or a tag the chunk writer rejects) must neither fail the chunk save nor wipe the tile's data: save the last good copy
            CompoundTag fresh = org.kylepitcock.legacyconnector.core.NbtSaveGuard.attempt(() -> te.writeToNBT(new NBTTagCompound()).mcc$modern(),
                    te, net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(getType()), getBlockPos().toShortString());
            if (fresh != null) lastGood = fresh;
            if (lastGood != null) tag.put("legacy", lastGood);
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
            lastGood = pending;
            legacy = null;
            failed = false;
        }
    }

    /** The legacy NBT of the last successful save or load (see NbtSaveGuard). */
    private CompoundTag lastGood;
}
