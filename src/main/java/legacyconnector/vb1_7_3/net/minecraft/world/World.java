package legacyconnector.vb1_7_3.net.minecraft.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.kylepitcock.legacyconnector.vb1_7_3.BlocksB173;
import org.kylepitcock.legacyconnector.vb1_7_3.StacksB173;

/**
 * Beta 1.7.3 {@code World} as a view of a modern level: block ids and metadata ({@link BlocksB173}), block changes with or without
 * neighbour notification (flags 3 and 2), scheduled ticks, redstone power, light and time. One view per level ({@link #mcc$wrap}).
 * Sound requests use the native client engine (server requests are sent to nearby clients).
 */
public class World implements BlockView {
    private static final Map<Level, World> VIEWS = Collections.synchronizedMap(new WeakHashMap<>());

    public List<Object> entities = new ArrayList<>();
    public List<Object> players = new ArrayList<>();
    /** Beta's {@code loadedTileEntityList}. */
    public List<BlockEntity> blockEntities = new ArrayList<>();
    public boolean pauseTicking;
    public int lightningTicksLeft;
    private long mcc$flashWrittenAt;
    private static java.util.function.ToIntFunction<Level> mcc$flashReader=level->0;
    public static void mcc$installFlashReader(java.util.function.ToIntFunction<Level> reader){mcc$flashReader=java.util.Objects.requireNonNull(reader);}
    public int mcc$get$lightningTicksLeft(){return mcc$level!=null&&mcc$level.isClientSide?mcc$flashReader.applyAsInt(mcc$level):mcc$level==null?lightningTicksLeft:(int)Math.max(0L,(long)lightningTicksLeft-(mcc$level.getGameTime()-mcc$flashWrittenAt));}
    public void mcc$set$lightningTicksLeft(int ticks){lightningTicksLeft=ticks;if(mcc$level!=null){mcc$flashWrittenAt=mcc$level.getGameTime();mcc$level.setSkyFlashTime(ticks);}}

    public boolean instantBlockUpdateEnabled;
    public int difficulty = 2;
    public Random random = new Random();
    public boolean isRemote;
    public WorldProperties properties = new WorldProperties(this);
    public int saveInterval = 40;
    public legacyconnector.vb1_7_3.net.minecraft.world.storage.WorldStorage storage = new legacyconnector.vb1_7_3.net.minecraft.world.storage.AlphaWorldStorage(this);
    public legacyconnector.vb1_7_3.net.minecraft.world.dimension.Dimension dimension = new legacyconnector.vb1_7_3.net.minecraft.world.dimension.Dimension();

    private final Level mcc$level;
    private final List<legacyconnector.vb1_7_3.net.minecraft.entity.Entity> mcc$entityQuery = new ArrayList<>();

    protected World() {
        this.mcc$level = null;
    }

    private World(Level level) {
        this.mcc$level = level;
        this.isRemote = level.isClientSide;
        this.entities = liveEntities(false);
        this.players = liveEntities(true);
        this.blockEntities = new java.util.AbstractList<>() {
            @Override public int size(){return org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity.loaded(level).size();}
            @Override public BlockEntity get(int index){return org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity.loaded(level).get(index);}
            @Override public java.util.Iterator<BlockEntity> iterator(){return org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity.loaded(level).iterator();}
        };
        dimension.evaporatesWater = level.dimensionType().ultraWarm();
        dimension.hasNoSky = !level.dimensionType().hasSkyLight();
        dimension.id = level.dimension() == Level.NETHER ? -1 : level.dimension() == Level.END ? 1 : 0;
    }

    /** The legacy view of a modern level (one per level). */
    public static World mcc$wrap(Level level) {
        if (level == null) return null;
        synchronized (VIEWS) {
            World view=VIEWS.get(level);
            if(view==null){
                view=new World(level);VIEWS.put(level,view);
                try{view.dimension=org.kylepitcock.legacyconnector.vb1_7_3.DimensionsB173.providerFor(view);}
                catch(RuntimeException|LinkageError error){VIEWS.remove(level);throw error;}
            }
            view.difficulty=level.getDifficulty().getId();return view;
        }
    }

    private static final ThreadLocal<net.minecraft.server.level.WorldGenRegion> GENERATION = new ThreadLocal<>();

    /** Spawn checks run against unfinished chunks; never request those chunks through the full server level. */
    public static final class GenerationScope implements AutoCloseable {
        private final net.minecraft.server.level.WorldGenRegion previous;
        private final TerrainReader previousTerrain;
        private boolean closed;
        private GenerationScope(net.minecraft.world.level.LevelAccessor access,TerrainReader terrain) {
            previous=GENERATION.get();
            previousTerrain=TERRAIN.get();
            if(access instanceof net.minecraft.server.level.WorldGenRegion region)GENERATION.set(region);
            else GENERATION.remove();
            if(access instanceof net.minecraft.server.level.WorldGenRegion&&terrain!=null)TERRAIN.set(terrain);
            else TERRAIN.remove();
        }
        @Override public void close(){
            if(closed)return;closed=true;
            try{
                var current=GENERATION.get();
                if(current!=null&&(previous==null||previous.getLevel()!=current.getLevel()))
                    org.kylepitcock.legacyconnector.vb1_7_3.WorldGenWritesB173.snapshot(current.getLevel());
            }finally{
                if(previous==null)GENERATION.remove();else GENERATION.set(previous);
                if(previousTerrain==null)TERRAIN.remove();else TERRAIN.set(previousTerrain);
            }
        }
    }
    /** Detached original terrain only; unavailable reads must return null rather than requesting native chunks. */
    @FunctionalInterface public interface TerrainReader {BlockState read(int x,int y,int z);}
    private static final ThreadLocal<TerrainReader> TERRAIN=new ThreadLocal<>();
    public static GenerationScope mcc$generation(net.minecraft.world.level.LevelAccessor access){return new GenerationScope(access,null);}
    public static GenerationScope mcc$generation(net.minecraft.world.level.LevelAccessor access,TerrainReader terrain){return new GenerationScope(access,terrain);}
    public static boolean mcc$generationActive(){return GENERATION.get()!=null;}
    /** The region is thread scoped and applies only to views of its own backing level. */
    public net.minecraft.world.level.LevelAccessor mcc$access(){
        var region=GENERATION.get();return region!=null&&region.getLevel()==mcc$level?region:mcc$level;
    }

    /** Only explicit missing generation dependencies reject a spawn; original mod exceptions still propagate. */
    public static final class SpawnRegionUnavailable extends RuntimeException {
        private SpawnRegionUnavailable(){super(null,null,false,false);}
    }
    private boolean regionHasTerrain(int chunkX,int chunkZ){
        if(!(mcc$access() instanceof net.minecraft.server.level.WorldGenRegion region))return true;
        if(!region.hasChunk(chunkX,chunkZ))return false;
        var chunk=region.getChunk(chunkX,chunkZ,net.minecraft.world.level.chunk.status.ChunkStatus.EMPTY,false);
        return chunk!=null&&!chunk.getPersistedStatus().isBefore(net.minecraft.world.level.chunk.status.ChunkStatus.NOISE);
    }
    private TerrainReader missingTerrain(int x,int z){
        var region=GENERATION.get();
        return region!=null&&region.getLevel()==mcc$level&&TERRAIN.get()!=null&&!regionHasTerrain(x>>4,z>>4)?TERRAIN.get():null;
    }
    private void requireRegionChunk(int chunkX,int chunkZ){
        if(!regionHasTerrain(chunkX,chunkZ))throw new SpawnRegionUnavailable();
    }
    /** Native BlockCollisions scans a one-block shell, including its epsilon-expanded boundary. */
    public void mcc$requireSpawnCollisionArea(net.minecraft.world.phys.AABB bounds){
        if(!(mcc$access() instanceof net.minecraft.server.level.WorldGenRegion))return;
        int minX=(net.minecraft.util.Mth.floor(bounds.minX-1.0E-7)-1)>>4,maxX=(net.minecraft.util.Mth.floor(bounds.maxX+1.0E-7)+1)>>4;
        int minZ=(net.minecraft.util.Mth.floor(bounds.minZ-1.0E-7)-1)>>4,maxZ=(net.minecraft.util.Mth.floor(bounds.maxZ+1.0E-7)+1)>>4;
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)requireRegionChunk(x,z);
    }

    public Level mcc$level() {
        return mcc$level;
    }

    public legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult raycast(
            legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d from, legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d to) {
        return raycast(from, to, false);
    }

    public legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult raycast(
            legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d from, legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d to, boolean fluids) {
        return raycast(from,to,fluids,false);
    }

    public boolean canInteract(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player, int x, int y, int z) {
        return mcc$level != null && player != null && player.mcc$modern instanceof net.minecraft.world.entity.player.Player modern
                && modern.mayBuild() && mcc$level.mayInteract(modern, pos(x, y, z));
    }

    public legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult raycast(
            legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d from, legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d to,
            boolean fluids, boolean ignoreWithoutCollision) {
        var access=mcc$access();
        if(access==null||from==null||to==null||!Double.isFinite(from.x)||!Double.isFinite(from.y)||!Double.isFinite(from.z)
                ||!Double.isFinite(to.x)||!Double.isFinite(to.y)||!Double.isFinite(to.z))return null;
        var start=new net.minecraft.world.phys.Vec3(from.x,from.y,from.z);var end=new net.minecraft.world.phys.Vec3(to.x,to.y,to.z);
        var context=new net.minecraft.world.level.ClipContext(start,end,net.minecraft.world.level.ClipContext.Block.OUTLINE,
                fluids?net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY:net.minecraft.world.level.ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty());
        return net.minecraft.world.level.BlockGetter.traverseBlocks(start,end,context,(clip,p)->{
            var state=state(p.getX(),p.getY(),p.getZ());
            if(state.getBlock() instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block hosted){
                var block=hosted.legacy();
                synchronized(block){
                    double[] bounds={block.minX,block.minY,block.minZ,block.maxX,block.maxY,block.maxZ};
                    try{
                        if(ignoreWithoutCollision&&block.getCollisionShape(this,p.getX(),p.getY(),p.getZ())==null)return null;
                        if(!block.hasCollision(BlocksB173.legacyMeta(state),fluids))return null;
                        return block.raycast(this,p.getX(),p.getY(),p.getZ(),from,to);
                    }finally{block.minX=bounds[0];block.minY=bounds[1];block.minZ=bounds[2];block.maxX=bounds[3];block.maxY=bounds[4];block.maxZ=bounds[5];}
                }
            }
            if(ignoreWithoutCollision&&state.getCollisionShape(access,p).isEmpty())return null;
            var blockHit=access.clipWithInteractionOverride(start,end,p,clip.getBlockShape(state,access,p),state);
            var fluidHit=clip.getFluidShape(state.getFluidState(),access,p).clip(start,end,p);
            var hit=blockHit==null?fluidHit:fluidHit==null||start.distanceToSqr(blockHit.getLocation())<=start.distanceToSqr(fluidHit.getLocation())?blockHit:fluidHit;
            if(hit==null)return null;
            var at=hit.getBlockPos();var v=hit.getLocation();
            return new legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult(at.getX(),at.getY(),at.getZ(),hit.getDirection().get3DDataValue(),
                    legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d.create(v.x,v.y,v.z));
        },clip->null);
    }

    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    private BlockState state(int x, int y, int z) {
        var access=mcc$access();
        if(access==null)return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        var pending=org.kylepitcock.legacyconnector.vb1_7_3.WorldGenWritesB173.pending(this,pos(x,y,z));if(pending!=null)return pending;
        var terrain=missingTerrain(x,z);if(terrain!=null){var value=terrain.read(x,y,z);if(value==null)throw new SpawnRegionUnavailable();return value;}
        requireRegionChunk(x>>4,z>>4);return access.getBlockState(pos(x,y,z));
    }

    // -------------------------------------------------------------------------------------------------------- BlockView

    @Override
    public int getBlockId(int x, int y, int z) {
        return BlocksB173.legacyId(state(x, y, z));
    }

    /** The legacy tile entity of a b1.7.3 block with one (made on first use, as b1.7.3's chunk did). */
    @Override
    public BlockEntity getBlockEntity(int x, int y, int z) {
        if (mcc$level == null) return null;
        if(org.kylepitcock.legacyconnector.vb1_7_3.WorldGenWritesB173.outside(this,pos(x,y,z)))return org.kylepitcock.legacyconnector.vb1_7_3.WorldGenWritesB173.tile(this,pos(x,y,z));
        var access=mcc$access();requireRegionChunk(x>>4,z>>4);
        var nativeBlockEntity=access.getBlockEntity(pos(x,y,z));
        if(nativeBlockEntity instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity host)return host.legacy();
        var view=org.kylepitcock.legacyconnector.vb1_7_3.BlockEntityViewsB173.wrap(nativeBlockEntity);if(view!=null)view.world=this;return view;
    }

    /** MCP {@code setBlockTileEntity}: only a block with a tile entity can hold one. */
    public void setBlockEntity(int x, int y, int z, BlockEntity te) {
        if (mcc$level == null || clientRendererWrite()) return;
        if(org.kylepitcock.legacyconnector.vb1_7_3.WorldGenWritesB173.outside(this,pos(x,y,z))){org.kylepitcock.legacyconnector.vb1_7_3.WorldGenWritesB173.replace(this,pos(x,y,z),te);return;}
        requireRegionChunk(x>>4,z>>4);var nativeEntity=mcc$access().getBlockEntity(pos(x,y,z));
        if (nativeEntity instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity host) host.setLegacy(te);
        else if(te instanceof legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity chest&&nativeEntity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity nativeChest){
            chest.world=this;org.kylepitcock.legacyconnector.vb1_7_3.BlockEntityViewsB173.attachChest(chest,nativeChest);org.kylepitcock.legacyconnector.vb1_7_3.BlockEntityViewsB173.flush(chest);
        }
        else if(te instanceof legacyconnector.vb1_7_3.net.minecraft.block.entity.MobSpawnerBlockEntity spawner&&nativeEntity instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity nativeSpawner){
            spawner.world=this;spawner.x=x;spawner.y=y;spawner.z=z;spawner.mcc$host=nativeSpawner;spawner.mcc$apply();
        }
    }

    /** MCP {@code removeBlockTileEntity}. */
    public void removeBlockEntity(int x, int y, int z) {
        if (mcc$level == null || clientRendererWrite()) return;
        if (mcc$level.getBlockEntity(pos(x, y, z)) instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity host) host.setLegacy(null);
    }

    @Override
    public float method_1782(int x, int y, int z) {
        if(missingTerrain(x,z)!=null)return getLightLevel(x,y,z)/15.0F;
        requireRegionChunk(x>>4,z>>4);return mcc$level == null ? 1.0F : mcc$access().getMaxLocalRawBrightness(pos(x, y, z)) / 15.0F;
    }

    @Override
    public int getBlockMeta(int x, int y, int z) {
        return BlocksB173.legacyMeta(state(x, y, z));
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        if (id == 0) return Material.AIR;
        Block b = Block.BLOCKS[id];
        if (b != null && b.material != null) return b.material;
        return org.kylepitcock.legacyconnector.vb1_7_3.VanillaB173.materialOf(state(x, y, z).getBlock());
    }

    @Override
    public boolean method_1783(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        return id > 0 && Block.BLOCKS_OPAQUE[id];
    }

    @Override
    public boolean shouldSuffocate(int x, int y, int z) {
        return mcc$level != null && state(x, y, z).isRedstoneConductor(mcc$level, pos(x, y, z));
    }

    // -------------------------------------------------------------------------------------------------------- blocks

    public boolean isAir(int x, int y, int z) {
        return state(x, y, z).isAir();
    }

    public boolean isPosLoaded(int x, int y, int z) {
        return mcc$level != null && (mcc$access() instanceof net.minecraft.server.level.WorldGenRegion region?region.hasChunk(x>>4,z>>4):mcc$level.isLoaded(pos(x,y,z)));
    }

    /** Inclusive Beta chunk-presence query; never requests or generates a chunk. */
    public boolean isRegionLoaded(int x,int y,int z,int radius){
        return isRegionLoaded(x-radius,y-radius,z-radius,x+radius,y+radius,z+radius);
    }
    public boolean isRegionLoaded(int minX,int minY,int minZ,int maxX,int maxY,int maxZ){
        if(maxY<0||minY>=128)return false;
        for(int x=minX>>4;x<=maxX>>4;x++)for(int z=minZ>>4;z<=maxZ>>4;z++)
            if(!isPosLoaded(x<<4,0,z<<4))return false;
        return true;
    }

    /** Original renderer-owned block gameplay is replayed authoritatively by the hosted server tick. */
    private boolean clientRendererWrite(){return mcc$level.isClientSide&&org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.active();}
    private boolean set(int x, int y, int z, int id, int meta, int flags) {
        return set(x, y, z, id, meta, flags, false);
    }
    /** Original batched writes still synchronize clients but defer neighbor and shape callbacks. */
    private int writeFlags(int flags){
        return pauseTicking?(flags&~net.minecraft.world.level.block.Block.UPDATE_NEIGHBORS)
                |net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE:flags;
    }
    private boolean set(int x, int y, int z, int id, int meta, int flags, boolean inferDoorHalf) {
        if (mcc$level == null || clientRendererWrite() || org.kylepitcock.legacyconnector.vb1_7_3.BlockRenderB173.capturing()) return false;
        if(mcc$access() instanceof net.minecraft.server.level.WorldGenRegion)
            id=org.kylepitcock.legacyconnector.vb1_7_3.BlockInterceptionB173.intercept(this,
                    new legacyconnector.vb1_7_3.net.minecraft.src.Loc(x,y,z),id);
        BlockPos p = pos(x, y, z);
        BlockState previous=state(x,y,z);
        if(inferDoorHalf&&BlocksB173.legacyId(previous)==id
                &&org.kylepitcock.legacyconnector.vb1_7_3.VanillaBlockStatesB173.isTorch(id,previous))return false;
        BlockState next = BlocksB173.modernState(id, meta);
        BlockState torch=BlocksB173.legacyId(previous)==id?null:
                org.kylepitcock.legacyconnector.vb1_7_3.VanillaTorchPlacementB173.resolve(id,meta,next,
                        direction->torchSupport(x,y,z,direction));
        boolean unsupportedTorch=torch!=null&&torch.isAir();
        if(torch!=null&&!unsupportedTorch)next=torch;
        // Beta builders commonly place each door id first and write its metadata afterwards.
        // A temporary native LOWER above LOWER would delete its partner during shape updates.
        if (inferDoorHalf && (id == 64 || id == 71) && next.getBlock() instanceof net.minecraft.world.level.block.DoorBlock) {
            BlockState below = state(x,y-1,z);
            if (below.is(next.getBlock()) && below.getValue(net.minecraft.world.level.block.DoorBlock.HALF)
                    == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)
                next = below.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
        }
        // Live writes must reach the native before-write interceptor even when the requested state is unchanged.
        if (previous == next && (mcc$access() instanceof net.minecraft.server.level.WorldGenRegion
                || !org.kylepitcock.legacyconnector.vb1_7_3.BlockInterceptionB173.hasInterceptors())) return false;
        boolean chestChange=previous.is(net.minecraft.world.level.block.Blocks.CHEST)||next.is(net.minecraft.world.level.block.Blocks.CHEST);
        if(org.kylepitcock.legacyconnector.vb1_7_3.WorldGenWritesB173.defer(this,p,next)){if(chestChange)chestChanged(p);return true;}
        var access=mcc$access();if(!org.kylepitcock.legacyconnector.core.worldgen.InPlaceStructureGuard.allowWrite(access,p,next))return false;
        boolean changed=access.setBlock(p,next,writeFlags(access instanceof net.minecraft.server.level.WorldGenRegion?2:flags));
        if(changed&&chestChange)chestChanged(p);
        // Original onBlockAdded drops an unsupported torch. Native destruction owns loot and neighbor updates.
        if(changed&&unsupportedTorch&&state(x,y,z)==next)access.destroyBlock(p,true);
        return changed;
    }

    private void chestChanged(BlockPos pos){
        java.util.function.Function<BlockPos,BlockState> read=p->state(p.getX(),p.getY(),p.getZ());
        java.util.function.BiConsumer<BlockPos,BlockState> write=this::writeChestState;
        org.kylepitcock.legacyconnector.vb1_7_3.VanillaChestPlacementB173.placed(pos,read,write);
        for(var side:net.minecraft.core.Direction.Plane.HORIZONTAL)
            org.kylepitcock.legacyconnector.vb1_7_3.VanillaChestPlacementB173.placed(pos.relative(side),read,write);
    }
    private void writeChestState(BlockPos pos,BlockState state){
        if(org.kylepitcock.legacyconnector.vb1_7_3.WorldGenWritesB173.defer(this,pos,state))return;
        var access=mcc$access();if(org.kylepitcock.legacyconnector.core.worldgen.InPlaceStructureGuard.allowWrite(access,pos,state))access.setBlock(pos,state,writeFlags(2));
    }

    private boolean torchSupport(int x,int y,int z,int meta){
        return switch(meta){
            case 1->shouldSuffocate(x-1,y,z);case 2->shouldSuffocate(x+1,y,z);
            case 3->shouldSuffocate(x,y,z-1);case 4->shouldSuffocate(x,y,z+1);
            case 5->shouldSuffocate(x,y-1,z)||getBlockId(x,y-1,z)==85;
            default->false;
        };
    }

    /** MCP {@code setBlock}: no neighbour updates. */
    public boolean setBlockWithoutNotifyingNeighbors(int x, int y, int z, int id) {
        return set(x, y, z, id, 0, 2, true);
    }

    /** MCP {@code setBlockAndMetadata}. */
    public boolean setBlockWithoutNotifyingNeighbors(int x, int y, int z, int id, int meta) {
        return set(x, y, z, id, meta, 2);
    }

    /** MCP {@code setBlockWithNotify}. */
    public boolean setBlock(int x, int y, int z, int id) {
        return set(x, y, z, id, 0, 3, true);
    }

    /** MCP {@code setBlockAndMetadataWithNotify}. */
    public boolean setBlock(int x, int y, int z, int id, int meta) {
        return set(x, y, z, id, meta, 3);
    }

    private boolean setMeta(int x, int y, int z, int meta, int flags) {
        if (mcc$level == null || clientRendererWrite()) return false;
        BlockPos p = pos(x, y, z);
        BlockState s = state(x,y,z);
        if (s.getBlock() instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block) {
            BlockState next = s.setValue(org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block.META, meta & 15);
            return next != s && writeMeta(p,next,flags);
        }
        BlockState next = org.kylepitcock.legacyconnector.vb1_7_3.VanillaBlockStatesB173.modernState(BlocksB173.legacyId(s), meta, s);
        if (next == null) next = BlocksB173.modernState(BlocksB173.legacyId(s), meta);
        // Beta metadata writes validated nothing: a builder that writes the same metadata to both door halves and fixes each half afterwards
        // passes through a state the native door would repair by removing its partner. Without neighbour notification the shape is left alone.
        if (s.getBlock() instanceof net.minecraft.world.level.block.DoorBlock && (flags & net.minecraft.world.level.block.Block.UPDATE_NEIGHBORS) == 0)
            flags |= net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
        boolean torchVariant=org.kylepitcock.legacyconnector.vb1_7_3.VanillaBlockStatesB173.isTorch(BlocksB173.legacyId(s),s)
                &&org.kylepitcock.legacyconnector.vb1_7_3.VanillaBlockStatesB173.isTorch(BlocksB173.legacyId(s),next);
        return next != s && (next.getBlock() == s.getBlock()||torchVariant||BlocksB173.legacyId(next)==BlocksB173.legacyId(s)) && writeMeta(p,next,flags);
    }
    private boolean writeMeta(BlockPos pos,BlockState next,int flags){
        if(org.kylepitcock.legacyconnector.vb1_7_3.BlockRenderB173.capturing())return false;
        boolean chest=next.is(net.minecraft.world.level.block.Blocks.CHEST);
        if(chest)next=org.kylepitcock.legacyconnector.vb1_7_3.VanillaChestPlacementB173.metadata(pos,state(pos.getX(),pos.getY(),pos.getZ()),next,
                p->state(p.getX(),p.getY(),p.getZ()),this::writeChestState);
        if(org.kylepitcock.legacyconnector.vb1_7_3.WorldGenWritesB173.defer(this,pos,next)){if(chest)chestChanged(pos);return true;}
        var access=mcc$access();boolean changed=org.kylepitcock.legacyconnector.core.worldgen.InPlaceStructureGuard.allowWrite(access,pos,next)&&access.setBlock(pos,next,writeFlags(access instanceof net.minecraft.server.level.WorldGenRegion?2:flags));
        if(changed&&chest)chestChanged(pos);return changed;
    }

    /** MCP {@code setBlockMetadataWithNotify}. */
    public void setBlockMeta(int x, int y, int z, int meta) {
        setMeta(x, y, z, meta, 3);
    }

    /** MCP {@code setBlockMetadata}. */
    public boolean setBlockMetaWithoutNotifyingNeighbors(int x, int y, int z, int meta) {
        return setMeta(x, y, z, meta, 2);
    }

    /** MCP {@code markBlockNeedsUpdate}: clients redraw it. */
    public void blockUpdateEvent(int x, int y, int z) {
        if (mcc$level == null) return;
        BlockState s = state(x, y, z);
        mcc$level.sendBlockUpdated(pos(x, y, z), s, s, 3);
    }

    /** Beta's dirty-region listener redraws the block without notifying its neighbours. */
    public void setBlockDirty(int x,int y,int z) {
        blockUpdateEvent(x,y,z);
    }

    /** MCP {@code notifyBlockChange}. */
    protected void blockUpdate(int x, int y, int z, int id) {
        blockUpdateEvent(x, y, z);
        notifyNeighbors(x, y, z, id);
    }

    /** MCP {@code notifyBlocksOfNeighborChange}. */
    public void notifyNeighbors(int x, int y, int z, int id) {
        if (mcc$level == null || pauseTicking) return;
        mcc$level.updateNeighborsAt(pos(x, y, z), id > 0 ? BlocksB173.modernBlock(id) : state(x, y, z).getBlock());
    }

    /** MCP {@code scheduleBlockUpdate}. */
    public void scheduleBlockUpdate(int x, int y, int z, int id, int delay) {
        if(instantBlockUpdateEnabled){
            if(isRegionLoaded(x-8,y-8,z-8,x+8,y+8,z+8)&&id>0&&id<Block.BLOCKS.length
                    &&getBlockId(x,y,z)==id&&Block.BLOCKS[id]!=null)Block.BLOCKS[id].onTick(this,x,y,z,random);
            return;
        }
        if (mcc$level == null || mcc$level.isClientSide) return;
        if(isRegionLoaded(x-8,y-8,z-8,x+8,y+8,z+8))mcc$access().scheduleTick(pos(x,y,z),BlocksB173.modernBlock(id),Math.max(1,delay));
    }

    /** Native fluids own their propagation; generation regions enqueue into their chunk's fluid tick container. */
    public void mcc$scheduleVanillaFluidTick(int x,int y,int z,int id){
        if(mcc$level==null||mcc$level.isClientSide||(id!=8&&id!=10))return;
        var p=pos(x,y,z);
        if(org.kylepitcock.legacyconnector.vb1_7_3.WorldGenWritesB173.outside(this,p))return;
        requireRegionChunk(x>>4,z>>4);var access=mcc$access();var fluid=access.getFluidState(p);
        if(!fluid.isEmpty()&&((id<=9&&fluid.is(net.minecraft.tags.FluidTags.WATER))
                ||(id>=10&&fluid.is(net.minecraft.tags.FluidTags.LAVA))))
            access.scheduleTick(p,fluid.getType(),fluid.getType().getTickDelay(access));
    }

    /** Beta spawn columns start at sea level and stop at the first air block above the column. */
    public int getSpawnBlockId(int x,int z){int y=63;while(y<127&&!isAir(x,y+1,z))y++;return getBlockId(x,y,z);}

    public boolean canPlace(int id, int x, int y, int z, boolean ignoreEntities, int side) {
        if (id <= 0 || id >= Block.BLOCKS.length || Block.BLOCKS[id] == null) return false;
        BlockState s = state(x, y, z);
        if (!s.isAir() && !s.canBeReplaced()) return false;
        return Block.BLOCKS[id].canPlaceAt(this, x, y, z, side);
    }

    // -------------------------------------------------------------------------------------------------------- redstone

    /** MCP {@code isBlockIndirectlyGettingPowered}: weak sources and strong power through adjacent solid blocks. */
    public boolean isEmittingRedstonePower(int x, int y, int z) {
        return mcc$level != null && mcc$level.hasNeighborSignal(pos(x,y,z));
    }

    /** MCP {@code isBlockGettingPowered}: strong power into this block. */
    public boolean canTransferPower(int x, int y, int z) {
        return mcc$level != null && mcc$level.getDirectSignalTo(pos(x,y,z))>0;
    }

    /** MCP {@code isBlockIndirectlyProvidingPowerTo}: the block at x,y,z powers the side {@code side} (b1.7.3 sides are modern 3D values). */
    public boolean isEmittingRedstonePowerInDirection(int x, int y, int z, int side) {
        return mcc$level != null && mcc$level.getSignal(pos(x, y, z), Direction.from3DDataValue(side)) > 0;
    }

    // -------------------------------------------------------------------------------------------------------- world

    public long getTime() {
        return mcc$level == null ? 0 : mcc$level.getGameTime();
    }

    public long getSeed() {
        return mcc$level instanceof net.minecraft.server.level.ServerLevel s ? s.getSeed() : 0;
    }

    /** Original providers retain their own biome manager and its mutable climate arrays. */
    public legacyconnector.vb1_7_3.net.minecraft.world.biome.source.BiomeSource getBiomeSource(){
        if(dimension.biomeSource==null)dimension.biomeSource=new legacyconnector.vb1_7_3.net.minecraft.world.biome.source.BiomeSource(this);
        return dimension.biomeSource;
    }
    public legacyconnector.vb1_7_3.net.minecraft.world.biome.source.BiomeSource method_1781(){return getBiomeSource();}

    public boolean isRaining() {
        return mcc$level != null && mcc$level.isRaining();
    }

    /** Rain exposure at the original entity/block position, including shelter and biome precipitation. */
    public boolean isRaining(int x,int y,int z){
        if(mcc$level==null)return false;
        requireRegionChunk(x>>4,z>>4);return mcc$level.isRainingAt(pos(x,y,z));
    }

    public boolean isThundering() {
        return mcc$level != null && mcc$level.isThundering();
    }

    public int getLightLevel(int x, int y, int z) {
        if(missingTerrain(x,z)!=null)return Math.max(getBrightness(LightType.BLOCK,x,y,z),Math.max(0,getBrightness(LightType.SKY,x,y,z)-ambientDarkness));
        requireRegionChunk(x>>4,z>>4);return mcc$level == null ? 15 : mcc$access().getMaxLocalRawBrightness(pos(x, y, z));
    }

    /** Raw combined Beta chunk light: unlike getLightLevel, sky darkness is not subtracted. */
    public int getBrightness(int x, int y, int z) {
        if(y<0)return 0;
        int height=Math.min(y,127);
        return Math.max(getBrightness(LightType.SKY,x,height,z),getBrightness(LightType.BLOCK,x,height,z));
    }

    /** Beta's sky darkness subtracted from sky light (0 by day). */
    public int ambientDarkness;

    /** MCP {@code getSavedLightValue(EnumSkyBlock, x, y, z)}: the raw light of one channel. */
    public int getBrightness(LightType type, int x, int y, int z) {
        if(missingTerrain(x,z)!=null){
            if(type==LightType.BLOCK){
                int emitted=Block.BLOCKS_LIGHT_LUMINANCE[BlocksB173.legacyId(state(x,y,z))];
                // The native light engine is a non-loading lookup; retain any already propagated light.
                return Math.max(emitted,mcc$level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos(x,y,z)));
            }
            if(dimension.hasNoSky)return 0;
            int light=15;
            for(int height=127;height>=Math.max(0,y)&&light>0;height--){
                int opacity=Block.BLOCKS_LIGHT_OPACITY[BlocksB173.legacyId(state(x,height,z))];
                if(opacity==0&&light<15)opacity=1;
                light=Math.max(0,light-opacity);
            }
            return y<0?0:light;
        }
        requireRegionChunk(x>>4,z>>4);
        if (mcc$level == null) return type.defaultValue;
        return mcc$access().getBrightness(type == LightType.SKY ? net.minecraft.world.level.LightLayer.SKY : net.minecraft.world.level.LightLayer.BLOCK, pos(x, y, z));
    }

    public boolean hasSkyLight(int x, int y, int z) {
        if(missingTerrain(x,z)!=null)return y>=getTopY(x,z);
        requireRegionChunk(x>>4,z>>4);return mcc$level != null && mcc$access().canSeeSky(pos(x, y, z));
    }

    /** MCP {@code getChunkFromBlockCoords}. */
    public legacyconnector.vb1_7_3.net.minecraft.world.chunk.Chunk getChunkFromPos(int x, int z) {
        return getChunk(x >> 4, z >> 4);
    }

    /** MCP {@code getChunkFromChunkCoords}: a view of that chunk. */
    public legacyconnector.vb1_7_3.net.minecraft.world.chunk.Chunk getChunk(int chunkX, int chunkZ) {
        return new legacyconnector.vb1_7_3.net.minecraft.world.chunk.Chunk(this, chunkX, chunkZ);
    }

    /** Beta's {@code getTopSolidOrLiquidBlock}: the y above the highest solid or liquid block of the column, -1 for none. */
    public int getTopSolidBlockY(int x, int z) {
        for (int y = Math.min(255, Math.max(0, getTopY(x, z))); y >= 0; y--) {
            var material = getMaterial(x, y, z);
            if (material != null && (material.isSolid() || material.isFluid())) return y + 1;
        }
        return -1;
    }

    public int getTopY(int x, int z) {
        if(missingTerrain(x,z)!=null){for(int y=127;y>=0;y--)if(Block.BLOCKS_LIGHT_OPACITY[BlocksB173.legacyId(state(x,y,z))]>0)return y+1;return 0;}
        requireRegionChunk(x>>4,z>>4);return mcc$level == null ? 64 : mcc$access().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    /** Drops a stack at the block, as {@code Block.dropStack} spawned an item entity. */
    public void mcc$dropStack(int x, int y, int z, ItemStack stack) {
        if (mcc$level == null || mcc$level.isClientSide || stack == null) return;
        var modern = StacksB173.toModern(stack);
        if (!modern.isEmpty()) net.minecraft.world.level.block.Block.popResource(mcc$level, pos(x, y, z), modern);
    }

    public void playSound(double x, double y, double z, String sound, float volume, float pitch) {
        org.kylepitcock.legacyconnector.vb1_7_3.AudioB173.play(mcc$level,new org.kylepitcock.legacyconnector.vb1_7_3.AudioB173.Request(org.kylepitcock.legacyconnector.vb1_7_3.AudioB173.Kind.SOUND,sound,x,y,z,volume,pitch,false));
    }

    public void playSound(legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity, String sound, float volume, float pitch) {
        if(entity!=null)playSound(entity.x,entity.y,entity.z,sound,volume,pitch);
    }
    public void playStreaming(String name,int x,int y,int z){
        org.kylepitcock.legacyconnector.vb1_7_3.AudioB173.play(mcc$level,new org.kylepitcock.legacyconnector.vb1_7_3.AudioB173.Request(org.kylepitcock.legacyconnector.vb1_7_3.AudioB173.Kind.STREAMING,name,x+.5,y+.5,z+.5,4,1,false));
    }
    /** Explicit legacy spawns during decoration enter proto chunks through their native generation region. */
    public boolean mcc$spawnNative(net.minecraft.world.entity.Entity entity) {
        if(mcc$level==null||mcc$level.isClientSide)return false;
        if(mcc$access() instanceof net.minecraft.server.level.WorldGenRegion region)
            return org.kylepitcock.legacyconnector.vb1_7_3.BetaDeferredGenEntities.stage(region.getLevel(),entity);
        return mcc$access().addFreshEntity(entity);
    }
    /** MCP {@code sendTrackedEntityStatusUpdatePacket}: an entity status (hurt, death, a mod's own) for the clients tracking the entity. */
    public void broadcastEntityEvent(legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity, byte status) {
        if (entity == null || entity.mcc$modern == null) return;
        if (entity.mcc$modern.level() instanceof net.minecraft.server.level.ServerLevel level) level.broadcastEntityEvent(entity.mcc$modern, status);
    }
    public boolean spawnEntity(legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity) {
        if(entity instanceof legacyconnector.vb1_7_3.net.minecraft.entity.LightningEntity lightning)return mcc$spawnLightning(lightning);
        if(mcc$level==null||entity==null)return false;
        if(entity instanceof legacyconnector.vb1_7_3.net.minecraft.entity.decoration.painting.PaintingEntity painting&&entity.getClass()==legacyconnector.vb1_7_3.net.minecraft.entity.decoration.painting.PaintingEntity.class)return org.kylepitcock.legacyconnector.vb1_7_3.PaintingsB173.spawn(painting);
        if(entity instanceof legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity item) {
            if(entity.getClass()!=legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity.class)
                return org.kylepitcock.legacyconnector.vb1_7_3.NeutralEntitiesB173.spawn(mcc$level,entity);
            if(mcc$level.isClientSide||item.mcc$modern!=null||item.stack==null)return false;
            var stack=StacksB173.toModern(item.stack);
            if(stack.isEmpty())return false;
            var host=new net.minecraft.world.entity.item.ItemEntity(mcc$level,item.x,item.y-item.standingEyeHeight,item.z,stack);
            host.setPickUpDelay(item.pickupDelay);host.setDeltaMovement(item.velocityX,item.velocityY,item.velocityZ);
            host.setYRot(item.yaw);host.setXRot(item.pitch);item.mcc$modern=host;
            boolean added=false;
            try {added=mcc$spawnNative(host);if(added)org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173.spawned(host,item);return added;}
            finally {if(!added){item.mcc$modern=null;host.discard();}}
        }
        if(entity.getClass()==legacyconnector.vb1_7_3.net.minecraft.entity.projectile.ArrowEntity.class){
            if(mcc$level.isClientSide||entity.mcc$modern!=null)return false;
            var arrow=(legacyconnector.vb1_7_3.net.minecraft.entity.projectile.ArrowEntity)entity;
            var host=new org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173VanillaArrow(mcc$level);
            host.setPos(entity.x,entity.y,entity.z);host.setYRot(entity.yaw);host.setXRot(entity.pitch);host.setDeltaMovement(entity.velocityX,entity.velocityY,entity.velocityZ);
            if(arrow.owner!=null)host.setOwner(arrow.owner.mcc$modern);
            entity.mcc$modern=host;boolean added=false;
            try{added=mcc$spawnNative(host);if(added)org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173.spawned(host,entity);return added;}
            finally{if(!added){entity.mcc$modern=null;host.discard();}}
        }
        if(entity instanceof legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity living) {
            if(org.kylepitcock.legacyconnector.vb1_7_3.AnimalsB173.supports(living.getClass()))
                return org.kylepitcock.legacyconnector.vb1_7_3.AnimalsB173.spawn(mcc$level,living);
            return org.kylepitcock.legacyconnector.vb1_7_3.MobsB173.spawn(mcc$level,living);
        }
        if(org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Arrow.supports(entity.getClass()))return org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Arrow.spawn(mcc$level,entity);
        return org.kylepitcock.legacyconnector.vb1_7_3.NeutralEntitiesB173.spawn(mcc$level,entity);
    }
    public boolean spawnGlobalEntity(legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity){
        if(entity instanceof legacyconnector.vb1_7_3.net.minecraft.entity.LightningEntity lightning)return mcc$spawnLightning(lightning);
        return spawnEntity(entity);
    }
    /** Both original spawn APIs preserve the lightning family before considering neutral entity hosts. */
    protected boolean mcc$spawnLightning(legacyconnector.vb1_7_3.net.minecraft.entity.LightningEntity entity){
        if(mcc$level==null||mcc$level.isClientSide||entity.mcc$modern!=null)return false;
        if(entity.getClass()!=legacyconnector.vb1_7_3.net.minecraft.entity.LightningEntity.class){
            if(!org.kylepitcock.legacyconnector.vb1_7_3.NeutralEntitiesB173.supports(entity.getClass()))return false;
            var host=new org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Lightning(org.kylepitcock.legacyconnector.LegacyConnector.BETA_LIGHTNING.get(),mcc$level);
            boolean added=false;try{host.bind(entity);added=mcc$spawnNative(host);if(added)org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173.spawned(host,entity);return added;}
            finally{if(!added){entity.mcc$modern=null;host.discard();}}
        }
        var bolt=new net.minecraft.world.entity.LightningBolt(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT,mcc$level);bolt.setPos(entity.x,entity.y,entity.z);
        entity.mcc$modern=bolt;boolean added=false;
        try{added=mcc$spawnNative(bolt);if(added)org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173.spawned(bolt,entity);return added;}
        finally{if(!added){entity.mcc$modern=null;bolt.discard();}}
    }
    public legacyconnector.vb1_7_3.net.minecraft.world.explosion.Explosion createExplosion(legacyconnector.vb1_7_3.net.minecraft.entity.Entity source,double x,double y,double z,float power){return createExplosion(source,x,y,z,power,false);}
    public legacyconnector.vb1_7_3.net.minecraft.world.explosion.Explosion createExplosion(legacyconnector.vb1_7_3.net.minecraft.entity.Entity source,double x,double y,double z,float power,boolean fire){
        if(mcc$level==null)return null;
        // Legacy explosions without a source use native block destruction rules.
        var interaction=source==null?Level.ExplosionInteraction.BLOCK:Level.ExplosionInteraction.MOB;
        var nativeExplosion=mcc$level.explode(source==null?null:source.mcc$modern,x,y,z,power,fire,interaction);
        return new legacyconnector.vb1_7_3.net.minecraft.world.explosion.Explosion(this,source,x,y,z,power,fire,nativeExplosion);
    }
    public List<legacyconnector.vb1_7_3.net.minecraft.entity.Entity> getEntities(legacyconnector.vb1_7_3.net.minecraft.entity.Entity except,
            legacyconnector.vb1_7_3.net.minecraft.util.math.Box box) {
        var result=mcc$queryEntities(except,box);
        // Beta clears and returns the same mutable per-world list on every untyped query.
        mcc$entityQuery.clear();mcc$entityQuery.addAll(result);return mcc$entityQuery;
    }
    private List<legacyconnector.vb1_7_3.net.minecraft.entity.Entity> mcc$queryEntities(legacyconnector.vb1_7_3.net.minecraft.entity.Entity except,
            legacyconnector.vb1_7_3.net.minecraft.util.math.Box box){
        var result=new ArrayList<legacyconnector.vb1_7_3.net.minecraft.entity.Entity>();
        if(mcc$level!=null)for(var entity:mcc$level.getEntities(except==null?null:except.mcc$modern,box.mcc$box()))
            if(legacyVisible(entity))result.add(org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173.wrap(entity));
        return result;
    }
    public List<Object> getEntities(){return new ArrayList<>(entities);}
    public int countEntities(Class<?> type){
        java.util.Objects.requireNonNull(type,"entity class");int count=0;
        for(Object entity:entities)if(type.isInstance(entity))count++;
        return count;
    }
    public List<legacyconnector.vb1_7_3.net.minecraft.entity.Entity> collectEntitiesByClass(Class<?> type,legacyconnector.vb1_7_3.net.minecraft.util.math.Box box){
        var result=mcc$queryEntities(null,box);result.removeIf(entity->!type.isInstance(entity));return result;
    }
    public legacyconnector.vb1_7_3.net.minecraft.entity.ai.pathing.Path findPath(legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity,legacyconnector.vb1_7_3.net.minecraft.entity.Entity target,float range){
        if(entity==null||target==null||!(entity.mcc$modern instanceof net.minecraft.world.entity.Mob mob)||target.mcc$modern==null)return null;
        return legacyconnector.vb1_7_3.net.minecraft.entity.ai.pathing.Path.mcc$wrap(mob.getNavigation().createPath(target.mcc$modern.blockPosition(),0,Math.max(1,(int)Math.ceil(range))));
    }
    public legacyconnector.vb1_7_3.net.minecraft.entity.ai.pathing.Path findPath(legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity,int x,int y,int z,float range){
        if(entity==null||!(entity.mcc$modern instanceof net.minecraft.world.entity.Mob mob))return null;
        return legacyconnector.vb1_7_3.net.minecraft.entity.ai.pathing.Path.mcc$wrap(mob.getNavigation().createPath(new BlockPos(x,y,z),0,Math.max(1,(int)Math.ceil(range))));
    }
    /** Biny's historical name refers to the daytime sky-darkening test, not a spawn gamerule. */
    public boolean canMonsterSpawn(){return mcc$level!=null&&mcc$level.getSkyDarken()<4;}
    public legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d getSkyColor(legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity,float partial){
        if(mcc$level==null)return legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d.create(0,0,0);
        int rgb=mcc$level.getBiome(BlockPos.containing(entity.x,entity.y,entity.z)).value().getSkyColor();
        double brightness=Math.max(0,Math.min(1,Math.cos(mcc$level.getSunAngle(partial))*2+.5));
        double r=((rgb>>16)&255)/255D*brightness,g=((rgb>>8)&255)/255D*brightness,b=(rgb&255)/255D*brightness;
        double rain=mcc$level.getRainLevel(partial),gray=(r*.3+g*.59+b*.11)*.6,blend=rain*.75;
        r=r*(1-blend)+gray*blend;g=g*(1-blend)+gray*blend;b=b*(1-blend)+gray*blend;
        double thunder=mcc$level.getThunderLevel(partial)*.75;gray=(r*.3+g*.59+b*.11)*.2;
        return legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d.create(r*(1-thunder)+gray*thunder,g*(1-thunder)+gray*thunder,b*(1-thunder)+gray*thunder);
    }
    public boolean updateMovementInFluid(legacyconnector.vb1_7_3.net.minecraft.util.math.Box box,Material material,legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity){
        if(mcc$level==null)return false;boolean wet=false;net.minecraft.world.phys.Vec3 flow=net.minecraft.world.phys.Vec3.ZERO;
        int minX=(int)Math.floor(box.minX),minY=(int)Math.floor(box.minY),minZ=(int)Math.floor(box.minZ);
        int maxX=(int)Math.floor(box.maxX+1),maxY=(int)Math.floor(box.maxY+1),maxZ=(int)Math.floor(box.maxZ+1);
        for(int x=minX;x<maxX;x++)for(int y=minY;y<maxY;y++)for(int z=minZ;z<maxZ;z++){
            var pos=new BlockPos(x,y,z);var state=mcc$level.getFluidState(pos);
            if(state.isEmpty()||getMaterial(x,y,z)!=material||maxY<y+state.getHeight(mcc$level,pos))continue;
            wet=true;flow=flow.add(state.getFlow(mcc$level,pos));
        }
        if(flow.lengthSqr()>0){flow=flow.normalize().scale(.014);entity.velocityX+=flow.x;entity.velocityY+=flow.y;entity.velocityZ+=flow.z;}
        return wet;
    }
    public float getVisibilityRatio(legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d source,legacyconnector.vb1_7_3.net.minecraft.util.math.Box box){
        if(mcc$level==null)return 0;int visible=0,total=0;
        double sx=1/((box.maxX-box.minX)*2+1),sy=1/((box.maxY-box.minY)*2+1),sz=1/((box.maxZ-box.minZ)*2+1);
        for(double x=0;x<=1;x+=sx)for(double y=0;y<=1;y+=sy)for(double z=0;z<=1;z+=sz){
            var start=new net.minecraft.world.phys.Vec3(box.minX+(box.maxX-box.minX)*x,box.minY+(box.maxY-box.minY)*y,box.minZ+(box.maxZ-box.minZ)*z);
            var end=new net.minecraft.world.phys.Vec3(source.x,source.y,source.z);
            if(mcc$level.clip(new net.minecraft.world.level.ClipContext(start,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty())).getType()==net.minecraft.world.phys.HitResult.Type.MISS)visible++;
            total++;
        }
        return total==0?0:visible/(float)total;
    }

    private static boolean legacyVisible(net.minecraft.world.entity.Entity entity){
        return !(entity instanceof net.minecraft.world.entity.item.ItemEntity item)||StacksB173.canRepresent(item.getItem());
    }
    /** Each traversal reflects the level's current representable entities and preserves hosted originals. */
    private List<Object> liveEntities(boolean onlyPlayers) {
        return new java.util.AbstractList<>() {
            private List<net.minecraft.world.entity.Entity> snapshot() {
                var result=new ArrayList<net.minecraft.world.entity.Entity>();
                if(onlyPlayers)result.addAll(mcc$level.players());
                else if(mcc$level instanceof net.minecraft.server.level.ServerLevel server)
                    server.getAllEntities().forEach(result::add);
                else if(mcc$level instanceof net.minecraft.client.multiplayer.ClientLevel client)
                    client.entitiesForRendering().forEach(result::add);
                result.removeIf(entity->!legacyVisible(entity));
                return result;
            }
            @Override public int size(){return snapshot().size();}
            @Override public Object get(int index){return org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173.wrap(snapshot().get(index));}
            /** Beta's {@code loadedEntityList.remove}: the entity leaves the world. A native player is never removed this way (a mod that replaces its player object does it). */
            @Override public Object remove(int index){
                var entity=snapshot().get(index);var view=org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173.wrap(entity);
                if(!(entity instanceof net.minecraft.world.entity.player.Player)&&!entity.isRemoved())entity.discard();
                return view;
            }
            @Override public boolean remove(Object object){
                var all=snapshot();
                for(int i=0;i<all.size();i++){
                    var view=org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173.wrap(all.get(i));
                    if(view==object||view!=null&&view.equals(object)){remove(i);return true;}
                }
                return false;
            }
            @Override public java.util.Iterator<Object> iterator(){
                return snapshot().stream().map(e -> (Object)org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173.wrap(e)).iterator();
            }
        };
    }

    public legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity getClosestPlayer(
            legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity,double range) {
        return getClosestPlayer(entity.x,entity.y,entity.z,range);
    }
    public legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity getClosestPlayer(double x,double y,double z,double range) {
        legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity closest=null;
        double best=range<0?Double.POSITIVE_INFINITY:range*range;
        for(Object object:players) {
            var player=(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity)object;
            double distance=player.getSquaredDistance(x,y,z);
            if(distance<best){best=distance;closest=player;}
        }
        return closest;
    }
    /** Beta includes solid block shapes and hard entity collision boxes. */
    public List<legacyconnector.vb1_7_3.net.minecraft.util.math.Box> getEntityCollisions(
            legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity,legacyconnector.vb1_7_3.net.minecraft.util.math.Box box) {
        var result=new ArrayList<legacyconnector.vb1_7_3.net.minecraft.util.math.Box>();
        if(mcc$level==null)return result;
        var bounds=box.mcc$box();mcc$requireSpawnCollisionArea(bounds);
        for(var shape:mcc$access().getBlockCollisions(entity==null?null:entity.mcc$modern,bounds))
            for(var b:shape.toAabbs())result.add(legacyconnector.vb1_7_3.net.minecraft.util.math.Box.create(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ));
        for(var other:mcc$level.getEntities(entity==null?null:entity.mcc$modern,bounds.inflate(.25))) {
            var b=other.getBoundingBox();
            if(!b.intersects(bounds))continue;
            if(other.canBeCollidedWith())result.add(legacyconnector.vb1_7_3.net.minecraft.util.math.Box.create(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ));
            if(entity!=null && entity.mcc$modern!=null && entity.mcc$modern.canCollideWith(other))
                result.add(legacyconnector.vb1_7_3.net.minecraft.util.math.Box.create(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ));
        }
        return result;
    }
    public boolean canSpawnEntity(legacyconnector.vb1_7_3.net.minecraft.util.math.Box box) {
        for(var entity:getEntities(null,box))if(!entity.dead&&entity.blocksSameBlockSpawning)return false;
        return true;
    }
    /** Beta's material query checks any liquid cell, including partially filled flowing cells. */
    public boolean isBoxSubmergedInFluid(legacyconnector.vb1_7_3.net.minecraft.util.math.Box box) {
        int minX=(int)Math.floor(box.minX),minY=(int)Math.floor(box.minY),minZ=(int)Math.floor(box.minZ);
        if(box.minX<0)--minX;if(box.minY<0)--minY;if(box.minZ<0)--minZ;
        int maxX=(int)Math.floor(box.maxX+1),maxY=(int)Math.floor(box.maxY+1),maxZ=(int)Math.floor(box.maxZ+1);
        for(int x=minX;x<maxX;x++)for(int y=minY;y<maxY;y++)for(int z=minZ;z<maxZ;z++)
            if(getMaterial(x,y,z).isFluid())return true;
        return false;
    }

    /** Beta's material surface query, used independently of fluid-current acceleration. */
    public boolean isFluidInBox(legacyconnector.vb1_7_3.net.minecraft.util.math.Box box,Material material) {
        int minX=(int)Math.floor(box.minX),minY=(int)Math.floor(box.minY),minZ=(int)Math.floor(box.minZ);
        int maxX=(int)Math.floor(box.maxX+1),maxY=(int)Math.floor(box.maxY+1),maxZ=(int)Math.floor(box.maxZ+1);
        for(int x=minX;x<maxX;x++)for(int y=minY;y<maxY;y++)for(int z=minZ;z<maxZ;z++){
            if(getBlockId(x,y,z)==0||getMaterial(x,y,z)!=material)continue;
            int metadata=getBlockMeta(x,y,z);
            double surface=y+1D-(metadata<8?metadata/8D:0);
            if(surface>=box.minY)return true;
        }
        return false;
    }

    public void addParticle(String name, double x, double y, double z, double vx, double vy, double vz) {
        if (mcc$level == null) return;
        var particle = org.kylepitcock.legacyconnector.vb1_7_3.ParticlesB173.of(name);
        if (particle != null) {
            if(mcc$level instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(particle,x,y,z,0,vx,vy,vz,1);
            else mcc$level.addParticle(particle, x, y, z, vx, vy, vz);
        }
    }

    public void updateSleepingPlayers(){if(mcc$level!=null)org.kylepitcock.legacyconnector.vb1_7_3.PlayerViewB173.updateSleepingPlayers(mcc$level);}
    public void worldEvent(int event, int x, int y, int z, int data) {worldEvent(null,event,x,y,z,data);}
    public void worldEvent(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player,int event,int x,int y,int z,int data) {
        org.kylepitcock.legacyconnector.vb1_7_3.RecordsB173.worldEvent(this,player,event,x,y,z,data);
    }
}
