package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import legacyconnector.vb1_7_3.net.minecraft.block.BlockWithEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The modern block entity of every b1.7.3 tile entity. It holds the legacy one: made by the block's {@code getBlockEntity()} the first time
 * it is asked for, or loaded from the saved NBT (b1.7.3 NBT under {@code legacy}, with the tile entity's registered id). The server ticks it
 * ({@code updateEntity}) when its class has a tick of its own; a tile entity that throws while ticking stops ticking. Clients get the same
 * NBT (block entity data packet), so GUIs and renderers find the state there.
 */
public class LegacyB173BlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    private static final Map<Class<?>, Boolean> TICKS = new ConcurrentHashMap<>();
    private static final Map<Level, java.util.Set<LegacyB173BlockEntity>> LOADED = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private BlockEntity legacy;
    private CompoundTag pending;
    private boolean tickFailed;

    public LegacyB173BlockEntity(BlockPos pos, BlockState state) {
        super(TileEntitiesB173.type(), pos, state);
    }

    /** The legacy tile entity, made or loaded on first use; null if the block makes none. */
    public BlockEntity legacy() {
        if (legacy == null) {
            try {
                if (pending != null) {
                    legacy = BlockEntity.createFromNbt(NbtB173.fromModern(pending));
                    pending = null;
                }
                if (legacy == null && getBlockState().getBlock() instanceof LegacyB173Block host && host.legacy() instanceof BlockWithEntity bwe) {
                    legacy = bwe.createBlockEntity();
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Tile entity at {} could not be made: {}", worldPosition, e.toString());
            }
            attach();
        }
        return legacy;
    }

    /** {@code World.setBlockTileEntity}: another legacy tile entity (or none) at this place. */
    public void setLegacy(BlockEntity te) {
        if (legacy != null && legacy != te) legacy.markRemoved();
        legacy = te;
        pending = null;
        tickFailed = false;
        attach();
        if(!WorldGenWritesB173.generation())setChanged();
    }

    private void attach() {
        if (legacy == null) return;
        legacy.mcc$host = this;
        legacy.x = worldPosition.getX();
        legacy.y = worldPosition.getY();
        legacy.z = worldPosition.getZ();
        if (level != null) legacy.world = World.mcc$wrap(level);
        legacy.cancelRemoval();
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null) LOADED.computeIfAbsent(level, k -> ConcurrentHashMap.newKeySet()).add(this);
        attach();
    }

    /** Beta's {@code World.loadedTileEntityList}: the legacy tile entities of the loaded chunks of a level. */
    public static java.util.List<BlockEntity> loaded(Level level) {
        var hosts = LOADED.get(level);
        var result = new java.util.ArrayList<BlockEntity>();
        if (hosts == null) return result;
        for (var host : hosts) {
            if (host.isRemoved()) continue;
            BlockEntity te = host.legacy();
            if (te != null) result.add(te);
        }
        return result;
    }

    public void tickServer() {
        BlockEntity te = legacy();
        if (te == null || tickFailed || !ticks(te.getClass())) return;
        World w = World.mcc$wrap(level);
        te.world = w;
        var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();
        World previousWorld=minecraft.world;
        try {
            tickAndCommit(()->{
                try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(w,null);
                        var views=EntityViewsB173.callbackScope()){
                    te.tick();
                }
            },()->level.blockEntityChanged(worldPosition));
            syncToClients(te);
        } catch (RuntimeException | LinkageError e) {
            tickFailed = true;
            LOG.error("b1.7.3 tile entity {} at {} failed while ticking; it stops ticking", te.getClass().getName(), worldPosition, e);
        } finally {
            minecraft.world=previousWorld;
        }
    }

    /** Commit only completed callbacks, including their view flush, without comparator/neighbor notifications.
     * Original ticking tiles can change inventory/timers without calling markDirty themselves. */
    private static final Map<Class<?>, Boolean> SYNCS = new ConcurrentHashMap<>();
    private net.minecraft.nbt.CompoundTag lastSent;

    /** A tile entity that builds its own description packet (BuildCraft's engines and pipes) is the one Beta sent to the players near it. */
    private static boolean syncs(Class<?> type) {
        return SYNCS.computeIfAbsent(type, t -> {
            try { return t.getMethod("getDescriptionPacket").getDeclaringClass() != BlockEntity.class; }
            catch (ReflectiveOperationException e) { return false; }
        });
    }

    /** In Beta single player the client drew the very tile entity the server ticked; here the client has its own copy, so a changed state is sent. */
    private void syncToClients(BlockEntity te) {
        if (level == null || level.isClientSide || !syncs(te.getClass()) || level.getGameTime() % 4 != 0) return;
        var tag = saveWithoutMetadata(level.registryAccess());
        if (tag.equals(lastSent)) return;
        lastSent = tag;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    static void tickAndCommit(Runnable callback,Runnable storageDirty){
        callback.run();
        storageDirty.run();
    }

    /** b1.7.3 ticked every tile entity; only those whose class has its own {@code updateEntity} need it. */
    private static boolean ticks(Class<?> type) {
        return TICKS.computeIfAbsent(type, t -> {
            try {
                return t.getMethod("tick").getDeclaringClass() != BlockEntity.class;
            } catch (NoSuchMethodException | RuntimeException | LinkageError e) {
                return false;
            }
        });
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        BlockEntity te = legacy;
        if (te != null) {
            try {
                NbtCompound nbt = new NbtCompound();
                te.writeNbt(nbt);
                tag.put("legacy", NbtB173.toModern(nbt));
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("b1.7.3 tile entity {} at {} could not be saved: {}", te.getClass().getName(), worldPosition, e.toString());
            }
        } else if (pending != null) {
            tag.put("legacy", pending);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!tag.contains("legacy")) return;
        CompoundTag nbt = tag.getCompound("legacy");
        BlockEntity te = legacy;
        if (te != null && nbt.getString("id").equals(BlockEntity.mcc$idOf(te.getClass()))) {
            // a client update for the tile entity it already has: read into it, so screens keep their object
            try {
                te.readNbt(NbtB173.fromModern(nbt));
                attach();
                return;
            } catch (RuntimeException | LinkageError e) {
                // made again below
            }
        }
        legacy = null;
        pending = nbt;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) { var hosts = LOADED.get(level); if (hosts != null) hosts.remove(this); }
        if (legacy != null) legacy.markRemoved();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
