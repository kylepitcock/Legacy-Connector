package org.kylepitcock.mccompat.v1_7_10.bridge;

import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.tileentity.TileEntity;
import mccompat.v1_7_10.net.minecraft.world.IBlockAccess;
import mccompat.v1_7_10.net.minecraft.world.World;
import mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockEntity;

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

    @Override
    public Block getBlock(int x, int y, int z) {
        return BlockBridge.legacyBlock(getter.getBlockState(new BlockPos(x, y, z)));
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return getter.getBlockEntity(new BlockPos(x, y, z)) instanceof LegacyBlockEntity l ? l.legacy() : null;
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
