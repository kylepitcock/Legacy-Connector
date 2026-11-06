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
 * Tile entities, entities, sounds and world generation are not bridged yet.
 */
public class World implements BlockView {
    private static final Map<Level, World> VIEWS = Collections.synchronizedMap(new WeakHashMap<>());

    public List<Object> entities = new ArrayList<>();
    public List<Object> players = new ArrayList<>();
    public boolean pauseTicking;
    public int difficulty = 2;
    public Random random = new Random();
    public boolean isRemote;

    private final Level mcc$level;

    protected World() {
        this.mcc$level = null;
    }

    private World(Level level) {
        this.mcc$level = level;
        this.isRemote = level.isClientSide;
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

    /** MCP {@code isBlockGettingPowered}: direct power into the block. */
    public boolean isEmittingRedstonePower(int x, int y, int z) {
        return mcc$level != null && mcc$level.getDirectSignalTo(pos(x, y, z)) > 0;
    }

    /** MCP {@code isBlockIndirectlyGettingPowered}. */
    public boolean canTransferPower(int x, int y, int z) {
        return mcc$level != null && mcc$level.hasNeighborSignal(pos(x, y, z));
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

    public int getTopY(int x, int z) {
        return mcc$level == null ? 64 : mcc$level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    /** Drops a stack at the block, as {@code Block.dropStack} spawned an item entity. */
    public void mcc$dropStack(int x, int y, int z, ItemStack stack) {
        if (mcc$level == null || mcc$level.isClientSide || stack == null) return;
        var modern = StacksB173.toModern(stack);
        if (!modern.isEmpty()) net.minecraft.world.level.block.Block.popResource(mcc$level, pos(x, y, z), modern);
    }

    public void playSound(double x, double y, double z, String sound, float volume, float pitch) {}

    public void addParticle(String name, double x, double y, double z, double vx, double vy, double vz) {
        if (mcc$level == null || !mcc$level.isClientSide) return;
        var particle = org.kylepitcock.legacyconnector.vb1_7_3.ParticlesB173.of(name);
        if (particle != null) mcc$level.addParticle(particle, x, y, z, vx, vy, vz);
    }

    public void worldEvent(int event, int x, int y, int z, int data) {}
}
