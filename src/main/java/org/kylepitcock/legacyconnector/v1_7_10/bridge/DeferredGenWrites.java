package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blocks a legacy populator places beyond the chunks its generation region covers. 1.7.10 generated or loaded any chunk a
 * populator wrote to; a modern region only holds the chunk being decorated and its neighbours, and the writes were dropped.
 * Witchery's Torment dimension builds its six 63-block mazes while populating chunk (0, 0), so the far half of every level was
 * missing and the Lord of Torment, spawned at z 36, fell into the void. The writes are kept per chunk and placed once that chunk
 * is loaded as a full chunk (at once, if it already is).
 */
public final class DeferredGenWrites {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/WorldGen");
    /** Writes kept at most, over all levels (a runaway generator must not exhaust the heap). */
    private static final int MAX = 1_000_000;

    /** A block to place, or (state null) a metadata change of the block found there. */
    private record Write(BlockPos pos, BlockState state, int meta, Consumer<BlockEntity> attach, boolean intercepted, long durable) {
        Write(BlockPos pos,BlockState state,int meta,Consumer<BlockEntity> attach,boolean intercepted){this(pos,state,meta,attach,intercepted,0);}
        Write(BlockPos pos, BlockState state, int meta, Consumer<BlockEntity> attach) {
            this(pos,state,meta,attach,false);
        }
        Write(BlockPos pos, BlockState state, int meta) {
            this(pos, state, meta, null);
        }
    }

    /** The block a deferred write will place, per position (what a populator that reads the block back sees). */
    private static final Map<ServerLevel, Map<BlockPos, BlockState>> STATES = new ConcurrentHashMap<>();
    /** Stand-ins for the tile entities of deferred blocks, per position. */
    private static final Map<ServerLevel, Map<BlockPos, Object>> TILES = new ConcurrentHashMap<>();

    private static final Map<ServerLevel, Map<Long, Queue<Write>>> PENDING = new ConcurrentHashMap<>();
    private static final AtomicInteger COUNT = new AtomicInteger();
    private static volatile boolean listening;
    private static volatile boolean warnedFull;

    private DeferredGenWrites() {
    }

    public static void block(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos key = pos.immutable();
        STATES.computeIfAbsent(level, l -> new ConcurrentHashMap<>()).put(key, state);
        add(level, new Write(key, state, -1));
    }

    /** A Beta generation callback already handled this write before it entered the deferred queue. */
    public static void interceptedBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if(!reserveIntercepted())return;
        BlockPos key=pos.immutable();
        long durable;
        try{durable=org.kylepitcock.legacyconnector.vb1_7_3.BetaDeferredGenData.of(level).record(key,state,false);}
        catch(RuntimeException|Error error){COUNT.decrementAndGet();throw error;}
        STATES.computeIfAbsent(level,l->new ConcurrentHashMap<>()).put(key,state);
        add(level,new Write(key,state,-1,null,true,durable));
    }

    public static void restoreIntercepted(ServerLevel level,long durable,BlockPos pos,BlockState state,boolean attachment,net.minecraft.nbt.CompoundTag tile){
        if(!reserveIntercepted())throw new IllegalStateException("Saved Beta generation exceeds the deferred write admission limit; saved records were retained");
        try{
        if(attachment){
            STATES.computeIfAbsent(level,l->new ConcurrentHashMap<>()).putIfAbsent(pos,state);
            if(tile!=null){
                Object view=org.kylepitcock.legacyconnector.vb1_7_3.WorldGenWritesB173.restoreTile(level,pos,tile);
                if(view!=null)TILES.computeIfAbsent(level,l->new ConcurrentHashMap<>()).put(pos,view);
            }
            add(level,new Write(pos,null,-1,nativeEntity->org.kylepitcock.legacyconnector.vb1_7_3.WorldGenWritesB173.attachRestored(level,pos,nativeEntity),true,durable));
        }else{
            STATES.computeIfAbsent(level,l->new ConcurrentHashMap<>()).put(pos,state);
            add(level,new Write(pos,state,-1,null,true,durable));
        }
        }catch(RuntimeException|Error error){COUNT.decrementAndGet();throw error;}
    }

    /** Called after the original populator has finished filling its retained stand-ins. */
    public static void snapshotInterceptedTiles(ServerLevel level){
        var data=org.kylepitcock.legacyconnector.vb1_7_3.BetaDeferredGenData.cached(level);var tiles=TILES.get(level);
        if(data==null)return;
        var current=data.populationPositions();
        var snapshots=new java.util.LinkedHashMap<BlockPos,net.minecraft.nbt.CompoundTag>();
        if(tiles!=null)for(var position:current){Object tile=tiles.get(position);
            if(data.contains(position)&&tile instanceof legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity original){
                var tag=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();original.writeNbt(tag);
                snapshots.put(position,org.kylepitcock.legacyconnector.vb1_7_3.NbtB173.toModern(tag));
            }
        }
        var positions=data.completedPopulation(snapshots);
        if(!positions.isEmpty())later(level,()->{
            var byChunk=PENDING.get(level);if(byChunk==null)return;
            for(long key:new java.util.ArrayList<>(byChunk.keySet()))
                if(level.getChunkSource().getChunkNow(ChunkPos.getX(key),ChunkPos.getZ(key))!=null)apply(level,key);
        });
    }

    /** The stand-in tile entity of a deferred block, or null. */
    public static Object standIn(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Object> m = TILES.get(level);
        return m == null ? null : m.get(pos);
    }

    /** The populator set a tile entity itself: it replaces the stand-in (when a block is waiting there). */
    public static boolean replaceStandIn(ServerLevel level, BlockPos pos, Object tile) {
        Map<BlockPos, Object> m = TILES.get(level);
        if (m == null || !m.containsKey(pos)) return false;
        m.put(pos.immutable(), tile);
        return true;
    }

    /** The block still waiting to be placed at the position, or null. */
    public static BlockState pendingState(ServerLevel level, BlockPos pos) {
        Map<BlockPos, BlockState> m = STATES.get(level);
        return m == null ? null : m.get(pos);
    }

    /**
     * The stand-in tile entity of the deferred block at the position: made by {@code factory} (given the pending state) on first
     * request, and handed to {@code attach} when the block is placed, so what the populator stored in it (chest loot) is not lost.
     * Null when no block is waiting there or the factory made none.
     */
    public static Object tile(ServerLevel level, BlockPos pos, Function<BlockState, Object> factory, Consumer<BlockEntity> attach) {
        return tile(level,pos,factory,attach,false);
    }
    public static Object interceptedTile(ServerLevel level,BlockPos pos,Function<BlockState,Object> factory,Consumer<BlockEntity> attach){
        return tile(level,pos,factory,attach,true);
    }
    private static Object tile(ServerLevel level, BlockPos pos, Function<BlockState, Object> factory, Consumer<BlockEntity> attach,boolean intercepted) {
        BlockState state = pendingState(level, pos);
        if (state == null) return null;
        BlockPos key = pos.immutable();
        Map<BlockPos, Object> tiles = TILES.computeIfAbsent(level, l -> new ConcurrentHashMap<>());
        Object existing = tiles.get(key);
        if (existing != null) return existing;
        if(intercepted&&!reserveIntercepted())return null;
        boolean reserved=intercepted;
        try{
        Object made = factory.apply(state);
        if (made == null){if(reserved)COUNT.decrementAndGet();return null;}
        LOG.info("Stand-in tile entity {} made for the deferred block {} at {}", made.getClass().getName(), state.getBlock(), key);
        long durable=intercepted?org.kylepitcock.legacyconnector.vb1_7_3.BetaDeferredGenData.of(level).record(key,state,true):0;
        Object raced = tiles.putIfAbsent(key, made);
        if (raced != null){if(reserved){org.kylepitcock.legacyconnector.vb1_7_3.BetaDeferredGenData.of(level).accepted(durable);COUNT.decrementAndGet();}return raced;}
        add(level, new Write(key, null, -1, attach,intercepted,durable));
        return made;
        }catch(RuntimeException|Error error){if(reserved)COUNT.decrementAndGet();throw error;}
    }

    public static void meta(ServerLevel level, BlockPos pos, int meta) {
        add(level, new Write(pos.immutable(), null, meta));
    }

    /** Runs on a later server tick: from the thread itself execute() would run it at once, inside the chunk's own load. */
    private static void later(ServerLevel level, Runnable task) {
        var server = level.getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount(), task));
    }

    /** The key of an unloaded chunk next to a block near a chunk border (diagonals included), else Long.MIN_VALUE. */
    private static long missingNeighbour(ServerLevel level, BlockPos pos) {
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        int lx = pos.getX() & 15, lz = pos.getZ() & 15;
        // onPlace and neighbour checks reach two blocks out (a hopper reads the signal of its neighbours' neighbours)
        int dx0 = lx <= 1 ? -1 : 0, dx1 = lx >= 14 ? 1 : 0, dz0 = lz <= 1 ? -1 : 0, dz1 = lz >= 14 ? 1 : 0;
        for (int dx = dx0; dx <= dx1; dx++) {
            for (int dz = dz0; dz <= dz1; dz++) {
                if ((dx | dz) != 0 && level.getChunkSource().getChunkNow(cx + dx, cz + dz) == null) return ChunkPos.asLong(cx + dx, cz + dz);
            }
        }
        return Long.MIN_VALUE;
    }

    private static void add(ServerLevel level, Write write) {
        add(level, write, ChunkPos.asLong(write.pos.getX() >> 4, write.pos.getZ() >> 4));
    }

    /** Queues a write under the chunk whose load (or generation) releases it. */
    private static void add(ServerLevel level, Write write, long key) {
        if (write.durable==0&&COUNT.get() >= MAX) {
            if (!warnedFull) {
                warnedFull = true;
                LOG.warn("Too many blocks placed by legacy generators outside their chunks; further ones are dropped");
            }
            return;
        }
        listen();
        boolean[] created = {false};
        PENDING.computeIfAbsent(level, l -> new ConcurrentHashMap<>()).compute(key, (k,queue) -> {
            if(queue==null){created[0]=true;queue=new ConcurrentLinkedQueue<>();}
            queue.add(write);return queue;
        });
        if(write.durable==0)COUNT.incrementAndGet();
        // the chunk may be loaded already (generated earlier): place the blocks on the server thread, once the populator is done
        if (created[0]) {
            later(level, () -> {
                if (level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)) != null) apply(level, key);
            });
        }
    }

    /** A durable permit stays reserved while a write is queued, processing, or waiting for another chunk. */
    private static boolean reserveIntercepted(){
        for(;;){int count=COUNT.get();if(count>=MAX){
            if(!warnedFull){warnedFull=true;LOG.warn("Too many deferred legacy generation writes; further Beta writes were not admitted");}
            return false;
        }if(COUNT.compareAndSet(count,count+1))return true;}
    }

    private static synchronized void listen() {
        if (listening) return;
        listening = true;
        // writes for chunks that are still to be generated are placed during their own generation
        org.kylepitcock.legacyconnector.core.worldgen.LegacyWorldGenFeature.addDecorator(
                (region, chunkX, chunkZ, seed) -> applyDuringGeneration(region, chunkX, chunkZ));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Load e) -> {
            if (!(e.getLevel() instanceof ServerLevel level) || !(e.getChunk() instanceof LevelChunk chunk)) return;
            Map<Long, Queue<Write>> byChunk = PENDING.get(level);
            if (byChunk == null) return;
            long key = chunk.getPos().toLong();
            if (byChunk.containsKey(key)) later(level, () -> apply(level, key));
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload e) -> {
            if (e.getLevel() instanceof ServerLevel level) {
                STATES.remove(level);
                TILES.remove(level);
                Map<Long, Queue<Write>> dropped = PENDING.remove(level);
                if (dropped != null) dropped.values().forEach(q -> COUNT.addAndGet(-q.size()));
            }
        });
    }

    /**
     * Send to clients, but no shape updates: those read the neighbouring blocks, and a write on a chunk border waits forever on
     * the neighbouring chunk when it is still being generated (the server thread hung entering a Thaumcraft dimension).
     */
    private static final int WRITE_FLAGS = 2 | 16 | 32;

    /**
     * Places the writes waiting for a chunk that is being generated, straight into it through the generation region (called from
     * the legacy decoration feature of that chunk). Most writes target chunks that do not exist yet; placed here they are plain
     * world-generation writes: no neighbour or shape updates, no block-change hooks (Sable's per-block hook made the main-thread
     * path below 3% of the server tick in ATM10), nothing sent to clients. Writes to chunks that are already loaded keep using it.
     */
    public static void applyDuringGeneration(net.minecraft.world.level.WorldGenLevel region, int chunkX, int chunkZ) {
        ServerLevel level = region.getLevel();
        Map<Long, Queue<Write>> byChunk = PENDING.get(level);
        if (byChunk == null || byChunk.isEmpty()) return;
        Queue<Write> writes = byChunk.remove(net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ));
        if (writes == null) return;
        COUNT.addAndGet(-(int)writes.stream().filter(write->write.durable==0).count());
        for (Write w : writes) {
            if(w.durable!=0){applyIntercepted(level,region,w,net.minecraft.world.level.ChunkPos.asLong(chunkX,chunkZ));continue;}
            try {
                if (w.attach != null) {
                    BlockEntity be = region.getBlockEntity(w.pos);
                    if (be != null) w.attach.accept(be);
                    Map<BlockPos, Object> tiles = TILES.get(level);
                    if (tiles != null) tiles.remove(w.pos);
                } else if (w.state != null) {
                    Map<BlockPos, BlockState> states = STATES.get(level);
                    if (states != null) states.remove(w.pos, w.state);
                    region.setBlock(w.pos, w.state, 2);
                } else {
                    BlockState current = region.getBlockState(w.pos);
                    BlockState next = BlockBridge.withMeta(current, w.meta);
                    if (next != current) region.setBlock(w.pos, next, 2);
                }
            } catch (RuntimeException e) {
                LOG.debug("Deferred legacy generation write at {} failed", w.pos, e);
            }
        }
    }

    private static void apply(ServerLevel level, long key) {
        Map<Long, Queue<Write>> byChunk = PENDING.get(level);
        if (byChunk == null) return;
        Queue<Write> writes = byChunk.remove(key);
        if (writes == null) return;
        COUNT.addAndGet(-(int)writes.stream().filter(write->write.durable==0).count());
        for (Write w : writes) {
            try {
                // a block on a chunk border whose neighbouring chunk is not loaded (onPlace of hoppers, rails, redstone reads the
                // neighbours, and waits on the main thread for that chunk): wait for the neighbour's load instead
                long missing = missingNeighbour(level, w.pos);
                if (missing != Long.MIN_VALUE) {
                    add(level, w, missing);
                    continue;
                }
                if(w.durable!=0){applyIntercepted(level,level,w,key);continue;}
                if (w.attach != null) {
                    BlockEntity be = level.getBlockEntity(w.pos);
                    if (be != null) w.attach.accept(be);
                    Map<BlockPos, Object> tiles = TILES.get(level);
                    if (tiles != null) tiles.remove(w.pos);
                } else if (w.state != null) {
                    Map<BlockPos, BlockState> states = STATES.get(level);
                    if (states != null) states.remove(w.pos, w.state);
                    if(w.intercepted)org.kylepitcock.legacyconnector.vb1_7_3.BlockInterceptionB173.writeReplacement(level,w.pos,w.state,WRITE_FLAGS,512);
                    else level.setBlock(w.pos, w.state, WRITE_FLAGS);
                } else {
                    BlockState current = level.getBlockState(w.pos);
                    BlockState next = BlockBridge.withMeta(current, w.meta);
                    if (next != current) level.setBlock(w.pos, next, WRITE_FLAGS);
                }
            } catch (RuntimeException e) {
                if(w.durable!=0)retain(level,w,key);
                LOG.debug("Deferred legacy generation write at {} failed", w.pos, e);
            }
        }
    }

    private static void retain(ServerLevel level,Write write,long key){
        PENDING.computeIfAbsent(level,l->new ConcurrentHashMap<>()).compute(key,(k,queue)->{
            if(queue==null)queue=new ConcurrentLinkedQueue<>();queue.add(write);return queue;
        });
    }
    private static void applyIntercepted(ServerLevel level,net.minecraft.world.level.LevelAccessor access,Write write,long key){
        try{
            var data=org.kylepitcock.legacyconnector.vb1_7_3.BetaDeferredGenData.of(level);
            if(!data.canApply(write.durable)){retain(level,write,key);return;}
            if(write.attach!=null){
                var entity=access.getBlockEntity(write.pos);
                if(entity==null){retain(level,write,key);return;}
                write.attach.accept(entity);
                var tiles=TILES.get(level);if(tiles!=null)tiles.remove(write.pos);
            }else{
                boolean accepted=access instanceof ServerLevel
                        ?org.kylepitcock.legacyconnector.vb1_7_3.BlockInterceptionB173.writeReplacement(level,write.pos,write.state,WRITE_FLAGS,512)
                        :access.setBlock(write.pos,write.state,2);
                if(!accepted&&access.getBlockState(write.pos)!=write.state){retain(level,write,key);return;}
                var states=STATES.get(level);if(states!=null)states.remove(write.pos,write.state);
            }
            data.accepted(write.durable);
            COUNT.decrementAndGet();
            if(!data.contains(write.pos)){var states=STATES.get(level);if(states!=null)states.remove(write.pos);}
        }catch(RuntimeException error){retain(level,write,key);LOG.warn("Pending Beta generation write retained at {} after failed replay",write.pos,error);}
    }
}
