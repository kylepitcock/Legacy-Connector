package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockEntity;

/**
 * Read-only legacy view over a modern {@link BlockGetter} (render regions, collision lookups). Falls back to the
 * full {@link World} view when the getter is a level.
 */
public final class BlockAccessView implements IBlockAccess {
    private final BlockGetter getter;
    /** Added to every x / z the legacy code passes in (see {@link #shifted}); 0 normally. */
    private final int dx, dz;

    private BlockAccessView(BlockGetter getter) {
        this(getter, 0, 0);
    }

    private BlockAccessView(BlockGetter getter, int dx, int dz) {
        this.getter = getter;
        this.dx = dx;
        this.dz = dz;
    }

    public static IBlockAccess of(BlockGetter getter) {
        return getter instanceof LevelAccessor level ? World.mcc$wrap(level) : new BlockAccessView(getter);
    }

    /**
     * A view over a render region whose legacy coordinates are the real ones minus (dx, dz): block renderers run near the
     * origin and read the blocks around the real position. Legacy renderers do their position math in float (vanilla's
     * {@code (float) x + 0.375F}, ChromatiCraft's {@code Tessellator.addTranslation((float) x, ...)}), which at a Sable /
     * Create Aeronautics plot (~20 million blocks out, where a float steps by 2) moves and flattens their geometry.
     * Null for a level (the full World view, which legacy code may cast, is not shifted).
     */
    public static BlockAccessView shifted(BlockGetter getter, int dx, int dz) {
        return getter instanceof LevelAccessor ? null : new BlockAccessView(getter, dx, dz);
    }

    private BlockPos at(int x, int y, int z) {
        return new BlockPos(x + dx, y, z + dz);
    }

    public static int tint(BlockGetter getter, int x, int y, int z, int kind) {
        if (!(getter instanceof net.minecraft.world.level.BlockAndTintGetter tinted)) return 0xFFFFFF;
        net.minecraft.world.level.ColorResolver resolver = kind == 0 ? (biome, bx, bz) -> biome.getGrassColor(bx, bz) : (biome, bx, bz) -> biome.getFoliageColor();
        return tinted.getBlockTint(new BlockPos(x, y, z), resolver) & 0xFFFFFF;
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase getBiomeGenForCoords(int x, int z) {
        net.minecraft.world.level.LevelReader reader = getter instanceof net.minecraft.world.level.LevelReader r ? r : ClientAccess.level();
        if (reader == null) return legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.plains;
        try {
            var key = reader.getBiome(new BlockPos(x + dx, reader.getSeaLevel(), z + dz)).unwrapKey().map(k -> k.location()).orElse(null);
            var legacy = LegacyEntitySpawns.legacyBiome(key);
            return legacy != null ? legacy : legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.plains;
        } catch (RuntimeException e) {
            return legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase.plains;
        }
    }

    /** Isolated so the dedicated server never loads client classes. */
    private static final class ClientAccess {
        static net.minecraft.world.level.LevelReader level() {
            return net.neoforged.fml.loading.FMLEnvironment.dist.isClient() ? net.minecraft.client.Minecraft.getInstance().level : null;
        }
    }

    @Override
    public int mcc$blockTint(int x, int y, int z, int kind) {
        return tint(getter, x + dx, y, z + dz, kind);
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return BlockBridge.legacyBlock(getter.getBlockState(at(x, y, z)));
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        BlockPos pos = at(x, y, z);
        // shape queries during generation: a region warns for each block whose block entity is not created yet
        var be = getter instanceof net.minecraft.world.level.LevelAccessor level ? TileEntityBridge.blockEntityAt(level, pos) : getter.getBlockEntity(pos);
        return be instanceof LegacyBlockEntity l ? l.legacy() : null;
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int min) {
        return 15 << 20 | Math.max(min, 0) << 4;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return BlockBridge.legacyMeta(getter.getBlockState(at(x, y, z)));
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        BlockPos p = at(x, y, z);
        return getter.getBlockState(p).getDirectSignal(getter, p, net.minecraft.core.Direction.from3DDataValue(side));
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return getter.getBlockState(at(x, y, z)).isAir();
    }

    @Override
    public int getHeight() {
        return 256;
    }

    /** The getter's build range (render regions span the level's, -64..320 in the overworld), for {@link BuildHeight}. */
    int buildBottom() {
        return getter.getMinBuildHeight();
    }

    int buildTop() {
        return getter.getMaxBuildHeight();
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean defaultValue) {
        BlockPos p = at(x, y, z);
        var d = side.mcc$modern();
        return d == null ? defaultValue : getter.getBlockState(p).isFaceSturdy(getter, p, d);
    }
}
