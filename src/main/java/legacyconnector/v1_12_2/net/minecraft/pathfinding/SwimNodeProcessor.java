package legacyconnector.v1_12_2.net.minecraft.pathfinding;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** The 1.12 swimming node processor: paths through water only, in all six directions. */
public class SwimNodeProcessor extends NodeProcessor {
    public SwimNodeProcessor() {
    }

    @Override
    public PathPoint getStart() {
        var bb = entity.getEntityBoundingBox();
        return openPoint(MathHelper.floor(bb.minX), MathHelper.floor(bb.minY + 0.5D), MathHelper.floor(bb.minZ));
    }

    @Override
    public PathPoint getPathPointToCoords(double x, double y, double z) {
        float half = Entity.mcc$get$width(entity) / 2.0F;
        return openPoint(MathHelper.floor(x - half), MathHelper.floor(y + 0.5D), MathHelper.floor(z - half));
    }

    @Override
    public int findPathOptions(PathPoint[] pathOptions, PathPoint currentPoint, PathPoint targetPoint, float maxDistance) {
        int i = 0;
        for (EnumFacing facing : EnumFacing.values()) {
            PathPoint p = getWaterNode(currentPoint.x + facing.getXOffset(), currentPoint.y + facing.getYOffset(), currentPoint.z + facing.getZOffset());
            if (p != null && !p.visited && p.distanceTo(targetPoint) < maxDistance) pathOptions[i++] = p;
        }
        return i;
    }

    @Override
    public PathNodeType getPathNodeType(IBlockAccess blockaccessIn, int x, int y, int z, EntityLiving entitylivingIn, int xSize, int ySize, int zSize,
            boolean canBreakDoorsIn, boolean canEnterDoorsIn) {
        return PathNodeType.WATER;
    }

    @Override
    public PathNodeType getPathNodeType(IBlockAccess blockaccessIn, int x, int y, int z) {
        return PathNodeType.WATER;
    }

    private PathPoint getWaterNode(int x, int y, int z) {
        return isFree(x, y, z) == PathNodeType.WATER ? openPoint(x, y, z) : null;
    }

    private PathNodeType isFree(int x, int y, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = x; i < x + entitySizeX; ++i) {
            for (int j = y; j < y + entitySizeY; ++j) {
                for (int k = z; k < z + entitySizeZ; ++k) {
                    IBlockState state = blockaccess.getBlockState(pos.setPos(i, j, k));
                    if (state == null || state.getMaterial() != Material.WATER) return PathNodeType.BLOCKED;
                }
            }
        }
        return PathNodeType.WATER;
    }
}
