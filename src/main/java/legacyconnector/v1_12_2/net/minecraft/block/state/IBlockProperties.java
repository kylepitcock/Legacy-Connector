package legacyconnector.v1_12_2.net.minecraft.block.state;

import legacyconnector.v1_12_2.net.minecraft.block.material.EnumPushReaction;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * The per-state block queries 1.12 split off {@link IBlockState} (the interface vanilla's states and mods' states implement). Mods call these
 * through this type (CraftTweaker), so it is an interface that {@link IBlockState} extends; the defaults live in IBlockState.
 */
public interface IBlockProperties {
    Material getMaterial();

    boolean isFullBlock();

    boolean canEntitySpawn(Entity entity);

    int getLightOpacity();

    int getLightOpacity(IBlockAccess world, BlockPos pos);

    int getLightValue();

    int getLightValue(IBlockAccess world, BlockPos pos);

    boolean useNeighborBrightness();

    boolean isFullCube();

    boolean isOpaqueCube();

    boolean isNormalCube();

    boolean isBlockNormalCube();

    boolean hasCustomBreakingProgress();

    boolean canProvidePower();

    boolean causesSuffocation();

    EnumPushReaction getPushReaction();

    float getBlockHardness(World world, BlockPos pos);

    float getPlayerRelativeBlockHardness(EntityPlayer player, World world, BlockPos pos);

    int getComparatorInputOverride(World world, BlockPos pos);

    int getWeakPower(IBlockAccess world, BlockPos pos, EnumFacing side);

    int getStrongPower(IBlockAccess world, BlockPos pos, EnumFacing side);

    boolean isSideSolid(IBlockAccess world, BlockPos pos, EnumFacing side);

    boolean doesSideBlockRendering(IBlockAccess world, BlockPos pos, EnumFacing facing);

    IBlockState getActualState(IBlockAccess world, BlockPos pos);
}
