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
    public boolean pauseTicking;
    public int difficulty = 2;
    public Random random = new Random();
    public boolean isRemote;
    public legacyconnector.vb1_7_3.net.minecraft.world.dimension.Dimension dimension = new legacyconnector.vb1_7_3.net.minecraft.world.dimension.Dimension();

    private final Level mcc$level;

    protected World() {
        this.mcc$level = null;
    }

    private World(Level level) {
        this.mcc$level = level;
        this.isRemote = level.isClientSide;
        this.entities = liveEntities(false);
        this.players = liveEntities(true);
        dimension.evaporatesWater = level.dimensionType().ultraWarm();
        dimension.hasNoSky = !level.dimensionType().hasSkyLight();
        dimension.id = level.dimension() == Level.NETHER ? -1 : level.dimension() == Level.END ? 1 : 0;
    }

    /** The legacy view of a modern level (one per level). */
    public static World mcc$wrap(Level level) {
        if (level == null) return null;
        synchronized (VIEWS) {
            return VIEWS.computeIfAbsent(level, World::new);
        }
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
        if (mcc$level == null || from == null || to == null) return null;
        var hit = mcc$level.clip(new net.minecraft.world.level.ClipContext(
                new net.minecraft.world.phys.Vec3(from.x, from.y, from.z), new net.minecraft.world.phys.Vec3(to.x, to.y, to.z),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, fluids ? net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY
                        : net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return null;
        var p = hit.getBlockPos(); var v = hit.getLocation();
        return new legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult(p.getX(), p.getY(), p.getZ(), hit.getDirection().get3DDataValue(),
                legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d.create(v.x, v.y, v.z));
    }

    public boolean canInteract(legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player, int x, int y, int z) {
        return mcc$level != null && player != null && player.mcc$modern instanceof net.minecraft.world.entity.player.Player modern
                && modern.mayBuild() && mcc$level.mayInteract(modern, pos(x, y, z));
    }

    public legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult raycast(
            legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d from, legacyconnector.vb1_7_3.net.minecraft.util.math.Vec3d to,
            boolean fluids, boolean ignoreWithoutCollision) {
        return raycast(from,to,fluids);
    }

    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    private BlockState state(int x, int y, int z) {
        return mcc$level == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : mcc$level.getBlockState(pos(x, y, z));
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
        return mcc$level.getBlockEntity(pos(x, y, z)) instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity host ? host.legacy() : null;
    }

    /** MCP {@code setBlockTileEntity}: only a block with a tile entity can hold one. */
    public void setBlockEntity(int x, int y, int z, BlockEntity te) {
        if (mcc$level == null) return;
        if (mcc$level.getBlockEntity(pos(x, y, z)) instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity host) host.setLegacy(te);
    }

    /** MCP {@code removeBlockTileEntity}. */
    public void removeBlockEntity(int x, int y, int z) {
        if (mcc$level == null) return;
        if (mcc$level.getBlockEntity(pos(x, y, z)) instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockEntity host) host.setLegacy(null);
    }

    @Override
    public float method_1782(int x, int y, int z) {
        return mcc$level == null ? 1.0F : mcc$level.getMaxLocalRawBrightness(pos(x, y, z)) / 15.0F;
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
        return mcc$level != null && mcc$level.isLoaded(pos(x, y, z));
    }

    private boolean set(int x, int y, int z, int id, int meta, int flags) {
        if (mcc$level == null) return false;
        BlockPos p = pos(x, y, z);
        BlockState next = BlocksB173.modernState(id, meta);
        if (mcc$level.getBlockState(p) == next) return false;
        return mcc$level.setBlock(p, next, flags);
    }

    /** MCP {@code setBlock}: no neighbour updates. */
    public boolean setBlockWithoutNotifyingNeighbors(int x, int y, int z, int id) {
        return set(x, y, z, id, 0, 2);
    }

    /** MCP {@code setBlockAndMetadata}. */
    public boolean setBlockWithoutNotifyingNeighbors(int x, int y, int z, int id, int meta) {
        return set(x, y, z, id, meta, 2);
    }

    /** MCP {@code setBlockWithNotify}. */
    public boolean setBlock(int x, int y, int z, int id) {
        return set(x, y, z, id, 0, 3);
    }

    /** MCP {@code setBlockAndMetadataWithNotify}. */
    public boolean setBlock(int x, int y, int z, int id, int meta) {
        return set(x, y, z, id, meta, 3);
    }

    private boolean setMeta(int x, int y, int z, int meta, int flags) {
        if (mcc$level == null) return false;
        BlockPos p = pos(x, y, z);
        BlockState s = mcc$level.getBlockState(p);
        if (s.getBlock() instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block) {
            BlockState next = s.setValue(org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block.META, meta & 15);
            return next != s && mcc$level.setBlock(p, next, flags);
        }
        BlockState next = BlocksB173.modernState(BlocksB173.legacyId(s), meta);
        return next != s && next.getBlock() == s.getBlock() && mcc$level.setBlock(p, next, flags);
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

    /** MCP {@code notifyBlockChange}. */
    protected void blockUpdate(int x, int y, int z, int id) {
        blockUpdateEvent(x, y, z);
        notifyNeighbors(x, y, z, id);
    }

    /** MCP {@code notifyBlocksOfNeighborChange}. */
    public void notifyNeighbors(int x, int y, int z, int id) {
        if (mcc$level == null) return;
        mcc$level.updateNeighborsAt(pos(x, y, z), id > 0 ? BlocksB173.modernBlock(id) : state(x, y, z).getBlock());
    }

    /** MCP {@code scheduleBlockUpdate}. */
    public void scheduleBlockUpdate(int x, int y, int z, int id, int delay) {
        if (mcc$level == null || mcc$level.isClientSide) return;
        mcc$level.scheduleTick(pos(x, y, z), BlocksB173.modernBlock(id), Math.max(1, delay));
    }

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

    public boolean isRaining() {
        return mcc$level != null && mcc$level.isRaining();
    }

    public boolean isThundering() {
        return mcc$level != null && mcc$level.isThundering();
    }

    public int getLightLevel(int x, int y, int z) {
        return mcc$level == null ? 15 : mcc$level.getMaxLocalRawBrightness(pos(x, y, z));
    }

    public boolean hasSkyLight(int x, int y, int z) {
        return mcc$level != null && mcc$level.canSeeSky(pos(x, y, z));
    }

    /** MCP {@code getChunkFromBlockCoords}. */
    public legacyconnector.vb1_7_3.net.minecraft.world.chunk.Chunk getChunkFromPos(int x, int z) {
        return getChunk(x >> 4, z >> 4);
    }

    /** MCP {@code getChunkFromChunkCoords}: a view of that chunk. */
    public legacyconnector.vb1_7_3.net.minecraft.world.chunk.Chunk getChunk(int chunkX, int chunkZ) {
        return new legacyconnector.vb1_7_3.net.minecraft.world.chunk.Chunk(this, chunkX, chunkZ);
    }

    public int getTopY(int x, int z) {
        return mcc$level == null ? 64 : mcc$level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
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
    public boolean spawnEntity(legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity) {
        if(mcc$level==null||entity==null)return false;
        if(entity instanceof legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity item) {
            if(mcc$level.isClientSide||item.mcc$modern!=null||item.stack==null)return false;
            var stack=StacksB173.toModern(item.stack);
            if(stack.isEmpty())return false;
            var host=new net.minecraft.world.entity.item.ItemEntity(mcc$level,item.x,item.y,item.z,stack);
            host.setPickUpDelay(item.pickupDelay);host.setDeltaMovement(item.velocityX,item.velocityY,item.velocityZ);
            host.setYRot(item.yaw);host.setXRot(item.pitch);item.mcc$modern=host;
            boolean added=false;
            try {added=mcc$level.addFreshEntity(host);return added;}
            finally {if(!added){item.mcc$modern=null;host.discard();}}
        }
        if(entity instanceof legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity living) {
            if(org.kylepitcock.legacyconnector.vb1_7_3.AnimalsB173.supports(living.getClass()))
                return org.kylepitcock.legacyconnector.vb1_7_3.AnimalsB173.spawn(mcc$level,living);
            return org.kylepitcock.legacyconnector.vb1_7_3.MobsB173.spawn(mcc$level,living);
        }
        return org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Arrow.spawn(mcc$level, entity);
    }
    public List<legacyconnector.vb1_7_3.net.minecraft.entity.Entity> getEntities(legacyconnector.vb1_7_3.net.minecraft.entity.Entity except,
            legacyconnector.vb1_7_3.net.minecraft.util.math.Box box) {
        if (mcc$level == null) return List.of();
        return mcc$level.getEntities(except == null ? null : except.mcc$modern,box.mcc$box()).stream()
                .map(org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173::wrap).toList();
    }

    /** Each traversal reflects the level's current loaded entities and preserves hosted originals. */
    private List<Object> liveEntities(boolean onlyPlayers) {
        return new java.util.AbstractList<>() {
            private List<net.minecraft.world.entity.Entity> snapshot() {
                var result=new ArrayList<net.minecraft.world.entity.Entity>();
                if(onlyPlayers)result.addAll(mcc$level.players());
                else if(mcc$level instanceof net.minecraft.server.level.ServerLevel server)
                    server.getAllEntities().forEach(result::add);
                else if(mcc$level instanceof net.minecraft.client.multiplayer.ClientLevel client)
                    client.entitiesForRendering().forEach(result::add);
                return result;
            }
            @Override public int size(){return snapshot().size();}
            @Override public Object get(int index){return org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173.wrap(snapshot().get(index));}
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
        var bounds=box.mcc$box();
        for(var shape:mcc$level.getBlockCollisions(entity==null?null:entity.mcc$modern,bounds))
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

    public void addParticle(String name, double x, double y, double z, double vx, double vy, double vz) {
        if (mcc$level == null) return;
        var particle = org.kylepitcock.legacyconnector.vb1_7_3.ParticlesB173.of(name);
        if (particle != null) {
            if(mcc$level instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(particle,x,y,z,0,vx,vy,vz,1);
            else mcc$level.addParticle(particle, x, y, z, vx, vy, vz);
        }
    }

    public void worldEvent(int event, int x, int y, int z, int data) {}
}
