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

    private BlockAccessView(BlockGetter getter) {
        this.getter = getter;
    }

    public static IBlockAccess of(BlockGetter getter) {
        return getter instanceof LevelAccessor level ? World.mcc$wrap(level) : new BlockAccessView(getter);
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
            var key = reader.getBiome(new BlockPos(x, reader.getSeaLevel(), z)).unwrapKey().map(k -> k.location()).orElse(null);
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
        return tint(getter, x, y, z, kind);
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return BlockBridge.legacyBlock(getter.getBlockState(new BlockPos(x, y, z)));
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
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
        return BlockBridge.legacyMeta(getter.getBlockState(new BlockPos(x, y, z)));
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        BlockPos p = new BlockPos(x, y, z);
        return getter.getBlockState(p).getDirectSignal(getter, p, net.minecraft.core.Direction.from3DDataValue(side));
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return getter.getBlockState(new BlockPos(x, y, z)).isAir();
    }

    @Override
    public int getHeight() {
        return 256;
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean defaultValue) {
        BlockPos p = new BlockPos(x, y, z);
        var d = side.mcc$modern();
        return d == null ? defaultValue : getter.getBlockState(p).isFaceSturdy(getter, p, d);
    }
}
