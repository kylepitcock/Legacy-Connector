package legacyconnector.v1_12_2.net.minecraft.block.state;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.IProperty;

/** A block with one value for each of its properties. */
public interface IBlockState {
    Collection<IProperty<?>> getPropertyKeys();

    <T extends Comparable<T>> T getValue(IProperty<T> property);

    <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property, V value);

    <T extends Comparable<T>> IBlockState cycleProperty(IProperty<T> property);

    ImmutableMap<IProperty<?>, Comparable<?>> getProperties();

    Block getBlock();

    Material getMaterial();

    // The block's (deprecated) per-state methods, as vanilla's StateImplementation forwards them.

    default int getLightValue() {
        return getBlock().getLightValue(this);
    }

    default boolean isOpaqueCube() {
        return getBlock().isOpaqueCube(this);
    }

    default boolean isFullBlock() {
        return getBlock().isFullBlock(this);
    }

    default boolean isFullCube() {
        return getBlock().isFullCube(this);
    }

    default legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType getRenderType() {
        return getBlock().getRenderType(this);
    }

    default float getBlockHardness(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBlock().getBlockHardness(this, world, pos);
    }

    default float getPlayerRelativeBlockHardness(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player,
            legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBlock().getPlayerRelativeBlockHardness(this, player, world, pos);
    }

    default IBlockState withRotation(legacyconnector.v1_12_2.net.minecraft.util.Rotation rot) {
        return getBlock().withRotation(this, rot);
    }

    default IBlockState withMirror(legacyconnector.v1_12_2.net.minecraft.util.Mirror mirror) {
        return getBlock().withMirror(this, mirror);
    }

    default void neighborChanged(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            Block block, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos fromPos) {
        getBlock().neighborChanged(this, world, pos, block, fromPos);
    }

    default boolean hasCustomBreakingProgress() {
        return getBlock().hasCustomBreakingProgress(this);
    }

    /** A block event (piston moves, note plays, chest lid) delivered to the block. */
    default boolean onBlockEventReceived(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            int id, int param) {
        return getBlock().eventReceived(this, world, pos, id, param);
    }

    default boolean canProvidePower() {
        return getBlock().canProvidePower(this);
    }

    default boolean hasComparatorInputOverride() {
        return getBlock().hasComparatorInputOverride(this);
    }

    default int getComparatorInputOverride(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBlock().getComparatorInputOverride(this, world, pos);
    }

    default boolean isTopSolid() {
        return getBlock().isTopSolid(this);
    }

    default boolean isBlockNormalCube() {
        return getBlock().isBlockNormalCube(this);
    }

    default boolean isTranslucent() {
        return getBlock().isTranslucent(this);
    }

    default int getLightOpacity() {
        return getBlock().getLightOpacity(this);
    }

    default boolean useNeighborBrightness() {
        return getBlock().getUseNeighborBrightness(this);
    }

    default float getAmbientOcclusionLightValue() {
        return getBlock().getAmbientOcclusionLightValue(this);
    }

    default boolean causesSuffocation() {
        return getBlock().causesSuffocation(this);
    }

    default boolean canEntitySpawn(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        return getBlock().canEntitySpawn(this, entity);
    }

    default legacyconnector.v1_12_2.net.minecraft.block.material.EnumPushReaction getPushReaction() {
        return getBlock().getPushReaction(this);
    }

    default void addCollisionBoxToList(legacyconnector.v1_12_2.net.minecraft.world.World world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB entityBox, java.util.List<legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB> boxes,
            legacyconnector.v1_12_2.net.minecraft.entity.Entity entity, boolean isActualState) {
        getBlock().addCollisionBoxToList(this, world, pos, entityBox, boxes, entity, isActualState);
    }

    default boolean isNormalCube() {
        Material m = getMaterial();
        return m != null && m.isOpaque() && isFullCube();
    }

    // ------------------------------------------------------------------ per-state forms of the block's IBlockAccess hooks

    default boolean isSideSolid(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        return getBlock().isSideSolid(this, world, pos, side);
    }

    default BlockFaceShape getBlockFaceShape(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        return getBlock().getBlockFaceShape(world, this, pos, facing);
    }

    default IBlockState getActualState(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBlock().getActualState(this, world, pos);
    }

    default legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB getCollisionBoundingBox(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBlock().getCollisionBoundingBox(this, world, pos);
    }

    default legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB getBoundingBox(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBlock().getBoundingBox(this, world, pos);
    }

    default legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB getSelectedBoundingBox(legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBlock().getSelectedBoundingBox(this, world, pos);
    }

    default int getLightOpacity(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBlock().getLightOpacity(this, world, pos);
    }

    default int getLightValue(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBlock().getLightValue(this, world, pos);
    }

    default legacyconnector.v1_12_2.net.minecraft.block.material.MapColor getMapColor(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBlock().getMapColor(this, world, pos);
    }

    default boolean shouldSideBeRendered(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        return getBlock().shouldSideBeRendered(this, world, pos, facing);
    }

    default boolean doesSideBlockRendering(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        return getBlock().doesSideBlockRendering(this, world, pos, facing);
    }

    default int getWeakPower(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        return getBlock().getWeakPower(this, world, pos, side);
    }

    default int getStrongPower(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        return getBlock().getStrongPower(this, world, pos, side);
    }

    default boolean isNormalCube(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBlock().isNormalCube(this, world, pos);
    }

    default int getPackedLightmapCoords(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess source, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBlock().getPackedLightmapCoords(this, source, pos);
    }

    default legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d getOffset(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess access,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        return getBlock().getOffset(this, access, pos);
    }

    default boolean doesSideBlockChestOpening(legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess world, legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing side) {
        return getBlock().doesSideBlockChestOpening(this, world, pos, side);
    }

    default legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult collisionRayTrace(legacyconnector.v1_12_2.net.minecraft.world.World worldIn,
            legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d start,
            legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d end) {
        return getBlock().collisionRayTrace(this, worldIn, pos, start, end);
    }
}
