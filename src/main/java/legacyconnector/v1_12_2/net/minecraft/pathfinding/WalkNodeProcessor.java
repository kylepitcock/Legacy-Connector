package legacyconnector.v1_12_2.net.minecraft.pathfinding;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.BlockDoor;
import legacyconnector.v1_12_2.net.minecraft.block.BlockFence;
import legacyconnector.v1_12_2.net.minecraft.block.BlockFenceGate;
import legacyconnector.v1_12_2.net.minecraft.block.BlockWall;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * The 1.12 walking node processor. Raw block types come from the modern path classification when the block view is backed by a level
 * (so modded blocks' own path types count), else from the 1.12 rules over the legacy block view.
 */
public class WalkNodeProcessor extends NodeProcessor {
    protected float avoidsWater;

    @Override
    public void init(IBlockAccess sourceIn, EntityLiving mob) {
        super.init(sourceIn, mob);
        this.avoidsWater = mob.getPathPriority(PathNodeType.WATER);
    }

    @Override
    public void postProcess() {
        if (entity != null) entity.setPathPriority(PathNodeType.WATER, avoidsWater);
        super.postProcess();
    }

    private static boolean isWater(IBlockState state) {
        return state != null && state.getMaterial() == Material.WATER;
    }

    private static boolean isAirOrPassable(IBlockAccess access, BlockPos pos) {
        IBlockState state = access.getBlockState(pos);
        return state == null || state.getMaterial() == Material.AIR || state.getBlock().isPassable(access, pos);
    }

    @Override
    public PathPoint getStart() {
        int y;
        if (getCanSwim() && entity.isInWater()) {
            y = (int) entity.getEntityBoundingBox().minY;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(MathHelper.floor(entity.mcc$x()), y, MathHelper.floor(entity.mcc$z()));
            while (isWater(blockaccess.getBlockState(pos)) && y < 255) {
                ++y;
                pos.setPos(MathHelper.floor(entity.mcc$x()), y, MathHelper.floor(entity.mcc$z()));
            }
        } else if (Entity.mcc$get$onGround(entity)) {
            y = MathHelper.floor(entity.getEntityBoundingBox().minY + 0.5D);
        } else {
            BlockPos pos = new BlockPos(entity);
            while (isAirOrPassable(blockaccess, pos) && pos.getY() > 0) pos = pos.down();
            y = pos.up().getY();
        }
        BlockPos at = new BlockPos(entity);
        PathNodeType type = getPathNodeType(entity, at.getX(), y, at.getZ());
        if (entity.getPathPriority(type) < 0.0F) {
            Set<BlockPos> corners = new LinkedHashSet<>();
            AxisAlignedBB bb = entity.getEntityBoundingBox();
            corners.add(new BlockPos(bb.minX, y, bb.minZ));
            corners.add(new BlockPos(bb.minX, y, bb.maxZ));
            corners.add(new BlockPos(bb.maxX, y, bb.minZ));
            corners.add(new BlockPos(bb.maxX, y, bb.maxZ));
            for (BlockPos corner : corners) {
                PathNodeType t = getPathNodeType(entity, corner.getX(), corner.getY(), corner.getZ());
                if (entity.getPathPriority(t) >= 0.0F) return openPoint(corner.getX(), corner.getY(), corner.getZ());
            }
        }
        return openPoint(at.getX(), y, at.getZ());
    }

    @Override
    public PathPoint getPathPointToCoords(double x, double y, double z) {
        return openPoint(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
    }

    private double floorHeight(BlockPos below) {
        IBlockState state = blockaccess.getBlockState(below);
        AxisAlignedBB bb = state == null ? null : state.getBoundingBox(blockaccess, below);
        return bb == null ? 0.0D : bb.maxY;
    }

    @Override
    public int findPathOptions(PathPoint[] pathOptions, PathPoint currentPoint, PathPoint targetPoint, float maxDistance) {
        int i = 0;
        int step = 0;
        PathNodeType above = getPathNodeType(entity, currentPoint.x, currentPoint.y + 1, currentPoint.z);
        if (entity.getPathPriority(above) >= 0.0F) step = MathHelper.floor(Math.max(1.0F, Entity.mcc$get$stepHeight(entity)));
        BlockPos below = new BlockPos(currentPoint.x, currentPoint.y, currentPoint.z).down();
        double floorY = currentPoint.y - (1.0D - floorHeight(below));
        PathPoint south = getSafePoint(currentPoint.x, currentPoint.y, currentPoint.z + 1, step, floorY, EnumFacing.SOUTH);
        PathPoint west = getSafePoint(currentPoint.x - 1, currentPoint.y, currentPoint.z, step, floorY, EnumFacing.WEST);
        PathPoint east = getSafePoint(currentPoint.x + 1, currentPoint.y, currentPoint.z, step, floorY, EnumFacing.EAST);
        PathPoint north = getSafePoint(currentPoint.x, currentPoint.y, currentPoint.z - 1, step, floorY, EnumFacing.NORTH);
        if (south != null && !south.visited && south.distanceTo(targetPoint) < maxDistance) pathOptions[i++] = south;
        if (west != null && !west.visited && west.distanceTo(targetPoint) < maxDistance) pathOptions[i++] = west;
        if (east != null && !east.visited && east.distanceTo(targetPoint) < maxDistance) pathOptions[i++] = east;
        if (north != null && !north.visited && north.distanceTo(targetPoint) < maxDistance) pathOptions[i++] = north;
        boolean n = north == null || north.nodeType == PathNodeType.OPEN || north.costMalus != 0.0F;
        boolean s = south == null || south.nodeType == PathNodeType.OPEN || south.costMalus != 0.0F;
        boolean e = east == null || east.nodeType == PathNodeType.OPEN || east.costMalus != 0.0F;
        boolean w = west == null || west.nodeType == PathNodeType.OPEN || west.costMalus != 0.0F;
        if (n && w) {
            PathPoint p = getSafePoint(currentPoint.x - 1, currentPoint.y, currentPoint.z - 1, step, floorY, EnumFacing.NORTH);
            if (p != null && !p.visited && p.distanceTo(targetPoint) < maxDistance) pathOptions[i++] = p;
        }
        if (n && e) {
            PathPoint p = getSafePoint(currentPoint.x + 1, currentPoint.y, currentPoint.z - 1, step, floorY, EnumFacing.NORTH);
            if (p != null && !p.visited && p.distanceTo(targetPoint) < maxDistance) pathOptions[i++] = p;
        }
        if (s && w) {
            PathPoint p = getSafePoint(currentPoint.x - 1, currentPoint.y, currentPoint.z + 1, step, floorY, EnumFacing.SOUTH);
            if (p != null && !p.visited && p.distanceTo(targetPoint) < maxDistance) pathOptions[i++] = p;
        }
        if (s && e) {
            PathPoint p = getSafePoint(currentPoint.x + 1, currentPoint.y, currentPoint.z + 1, step, floorY, EnumFacing.SOUTH);
            if (p != null && !p.visited && p.distanceTo(targetPoint) < maxDistance) pathOptions[i++] = p;
        }
        return i;
    }

    private boolean collides(AxisAlignedBB bb) {
        World world = Entity.mcc$get$world(entity);
        return world != null && world.collidesWithAnyBlock(bb);
    }

    protected PathPoint getSafePoint(int x, int y, int z, int stepHeight, double maxY, EnumFacing facing) {
        PathPoint point = null;
        BlockPos pos = new BlockPos(x, y, z);
        double floorY = y - (1.0D - floorHeight(pos.down()));
        if (floorY - maxY > 1.125D) return null;
        PathNodeType type = getPathNodeType(entity, x, y, z);
        float malus = entity.getPathPriority(type);
        float width = Entity.mcc$get$width(entity);
        float height = Entity.mcc$get$height(entity);
        double halfWidth = width / 2.0D;
        if (malus >= 0.0F) {
            point = openPoint(x, y, z);
            point.nodeType = type;
            point.costMalus = Math.max(point.costMalus, malus);
        }
        if (type == PathNodeType.WALKABLE) return point;
        if (point == null && stepHeight > 0 && type != PathNodeType.FENCE && type != PathNodeType.TRAPDOOR) {
            point = getSafePoint(x, y + 1, z, stepHeight - 1, maxY, facing);
            if (point != null && (point.nodeType == PathNodeType.OPEN || point.nodeType == PathNodeType.WALKABLE) && width < 1.0F) {
                double cx = (x - facing.getXOffset()) + 0.5D;
                double cz = (z - facing.getZOffset()) + 0.5D;
                AxisAlignedBB box = new AxisAlignedBB(cx - halfWidth, y + 0.001D, cz - halfWidth, cx + halfWidth, y + height, cz + halfWidth);
                IBlockState state = blockaccess.getBlockState(pos);
                AxisAlignedBB blockBox = state == null ? null : state.getBoundingBox(blockaccess, pos);
                AxisAlignedBB raised = box.expand(0.0D, (blockBox == null ? 0.0D : blockBox.maxY) - 0.002D, 0.0D);
                if (collides(raised)) point = null;
            }
        }
        if (type == PathNodeType.OPEN) {
            AxisAlignedBB box = new AxisAlignedBB(x - halfWidth + 0.5D, y + 0.001D, z - halfWidth + 0.5D, x + halfWidth + 0.5D, y + height, z + halfWidth + 0.5D);
            if (collides(box)) return null;
            if (width >= 1.0F) {
                PathNodeType under = getPathNodeType(entity, x, y - 1, z);
                if (under == PathNodeType.BLOCKED) {
                    point = openPoint(x, y, z);
                    point.nodeType = PathNodeType.WALKABLE;
                    point.costMalus = Math.max(point.costMalus, malus);
                    return point;
                }
            }
            int fallen = 0;
            while (y > 0 && type == PathNodeType.OPEN) {
                --y;
                if (fallen++ >= entity.getMaxFallHeight()) return null;
                type = getPathNodeType(entity, x, y, z);
                malus = entity.getPathPriority(type);
                if (type != PathNodeType.OPEN && malus >= 0.0F) {
                    point = openPoint(x, y, z);
                    point.nodeType = type;
                    point.costMalus = Math.max(point.costMalus, malus);
                    break;
                }
                if (malus < 0.0F) return null;
            }
        }
        return point;
    }

    @Override
    public PathNodeType getPathNodeType(IBlockAccess blockaccessIn, int x, int y, int z, EntityLiving entitylivingIn, int xSize, int ySize, int zSize,
            boolean canBreakDoorsIn, boolean canEnterDoorsIn) {
        EnumSet<PathNodeType> types = EnumSet.noneOf(PathNodeType.class);
        PathNodeType type = getPathNodeType(blockaccessIn, x, y, z, xSize, ySize, zSize, canBreakDoorsIn, canEnterDoorsIn, types, PathNodeType.BLOCKED,
                new BlockPos(entitylivingIn));
        if (types.contains(PathNodeType.FENCE)) return PathNodeType.FENCE;
        PathNodeType worst = PathNodeType.BLOCKED;
        for (PathNodeType t : types) {
            if (entitylivingIn.getPathPriority(t) < 0.0F) return t;
            if (entitylivingIn.getPathPriority(t) >= entitylivingIn.getPathPriority(worst)) worst = t;
        }
        return type == PathNodeType.OPEN && entitylivingIn.getPathPriority(worst) == 0.0F ? PathNodeType.OPEN : worst;
    }

    public PathNodeType getPathNodeType(IBlockAccess access, int x, int y, int z, int xSize, int ySize, int zSize, boolean canOpenDoorsIn,
            boolean canEnterDoorsIn, EnumSet<PathNodeType> types, PathNodeType type, BlockPos entityPos) {
        for (int i = 0; i < xSize; ++i) {
            for (int j = 0; j < ySize; ++j) {
                for (int k = 0; k < zSize; ++k) {
                    PathNodeType t = getPathNodeType(access, i + x, j + y, k + z);
                    if (t == PathNodeType.DOOR_WOOD_CLOSED && canOpenDoorsIn && canEnterDoorsIn) t = PathNodeType.WALKABLE;
                    if (t == PathNodeType.DOOR_OPEN && !canEnterDoorsIn) t = PathNodeType.BLOCKED;
                    if (t == PathNodeType.RAIL && !mcc$isRail(access, entityPos) && !mcc$isRail(access, entityPos.down())) t = PathNodeType.FENCE;
                    if (i == 0 && j == 0 && k == 0) type = t;
                    types.add(t);
                }
            }
        }
        return type;
    }

    private static boolean mcc$isRail(IBlockAccess access, BlockPos pos) {
        IBlockState state = access.getBlockState(pos);
        if (state == null) return false;
        Object modern = state.getBlock().mcc$modern;
        return modern instanceof net.minecraft.world.level.block.BaseRailBlock;
    }

    private PathNodeType getPathNodeType(EntityLiving mob, int x, int y, int z) {
        return getPathNodeType(blockaccess, x, y, z, mob, entitySizeX, entitySizeY, entitySizeZ, getCanOpenDoors(), getCanEnterDoors());
    }

    @Override
    public PathNodeType getPathNodeType(IBlockAccess access, int x, int y, int z) {
        PathNodeType type = getPathNodeTypeRaw(access, x, y, z);
        if (type == PathNodeType.OPEN && y >= 1) {
            PathNodeType below = getPathNodeTypeRaw(access, x, y - 1, z);
            type = below != PathNodeType.WALKABLE && below != PathNodeType.OPEN && below != PathNodeType.WATER && below != PathNodeType.LAVA
                    ? PathNodeType.WALKABLE : PathNodeType.OPEN;
            if (below == PathNodeType.DAMAGE_FIRE) type = PathNodeType.DAMAGE_FIRE;
            if (below == PathNodeType.DAMAGE_CACTUS) type = PathNodeType.DAMAGE_CACTUS;
            if (below == PathNodeType.DAMAGE_OTHER) type = PathNodeType.DAMAGE_OTHER;
        }
        return checkNeighborBlocks(access, x, y, z, type);
    }

    public PathNodeType checkNeighborBlocks(IBlockAccess access, int x, int y, int z, PathNodeType type) {
        if (type != PathNodeType.WALKABLE) return type;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                if (i == 0 && j == 0) continue;
                PathNodeType t = getPathNodeTypeRaw(access, x + i, y, z + j);
                if (t == PathNodeType.DAMAGE_CACTUS) type = PathNodeType.DANGER_CACTUS;
                else if (t == PathNodeType.DAMAGE_FIRE || t == PathNodeType.LAVA) type = PathNodeType.DANGER_FIRE;
                else if (t == PathNodeType.DAMAGE_OTHER) type = PathNodeType.DANGER_OTHER;
            }
        }
        return type;
    }

    protected PathNodeType getPathNodeTypeRaw(IBlockAccess access, int x, int y, int z) {
        PathNodeType modern = mcc$modernRawType(access, entity, x, y, z);
        if (modern != null) return modern;
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = access == null ? null : access.getBlockState(pos);
        if (state == null) return PathNodeType.OPEN;
        Block block = state.getBlock();
        Material material = state.getMaterial();
        if (material == Material.AIR) return PathNodeType.OPEN;
        if (block instanceof BlockDoor) {
            boolean open = Boolean.TRUE.equals(state.getValue(BlockDoor.OPEN));
            if (open) return PathNodeType.DOOR_OPEN;
            return material == Material.IRON ? PathNodeType.DOOR_IRON_CLOSED : PathNodeType.DOOR_WOOD_CLOSED;
        }
        if (mcc$isRail(access, pos)) return PathNodeType.RAIL;
        if (block instanceof BlockFence || block instanceof BlockWall
                || block instanceof BlockFenceGate && !Boolean.TRUE.equals(state.getValue(BlockFenceGate.OPEN))) {
            return PathNodeType.FENCE;
        }
        if (material == Material.WATER) return PathNodeType.WATER;
        if (material == Material.LAVA) return PathNodeType.LAVA;
        return block.isPassable(access, pos) ? PathNodeType.OPEN : PathNodeType.BLOCKED;
    }
}
