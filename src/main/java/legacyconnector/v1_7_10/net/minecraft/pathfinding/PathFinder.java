package legacyconnector.v1_7_10.net.minecraft.pathfinding;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Legacy A* path finder over a block access (mods build their own for custom navigators). Paths are found by the modern
 * ground navigation of the entity's host ({@link World#getEntityPathToXYZ}) with this finder's door and water abilities.
 */
public class PathFinder {
    public IBlockAccess worldMap;
    public boolean isWoddenDoorAllowed;
    public boolean isMovementBlockAllowed;
    public boolean isPathingInWater;
    public boolean canEntityDrown;

    public PathFinder(IBlockAccess world, boolean woodenDoors, boolean movementBlocks, boolean pathInWater, boolean canDrown) {
        this.worldMap = world;
        this.isWoddenDoorAllowed = woodenDoors;
        this.isMovementBlockAllowed = movementBlocks;
        this.isPathingInWater = pathInWater;
        this.canEntityDrown = canDrown;
    }

    public PathEntity createEntityPathTo(Entity entity, Entity target, float range) {
        if (target == null) return null;
        return createEntityPathTo(entity, MathHelper.floor_double(Entity.mcc$get$posX(target)),
                MathHelper.floor_double(Entity.mcc$get$boundingBox(target).minY), MathHelper.floor_double(Entity.mcc$get$posZ(target)), range);
    }

    public PathEntity createEntityPathTo(Entity entity, int x, int y, int z, float range) {
        if (entity == null) return null;
        World world = worldMap instanceof World w ? w : Entity.mcc$get$worldObj(entity);
        if (world == null) return null;
        return world.getEntityPathToXYZ(entity, x, y, z, range, isWoddenDoorAllowed, isMovementBlockAllowed, isPathingInWater, canEntityDrown);
    }

    /**
     * Whether the entity's box (the size of {@code size}) fits at the position: 1 clear, 0 blocked, -1 water (when avoided), -2 lava,
     * -3 fence or rails, -4 closed trapdoor, 2 open trapdoor, water (when not avoided) or an open door (1.7.10's func_82565_a).
     */
    public static int canEntityStandAt(Entity entity, int x, int y, int z, PathPoint size, boolean avoidWater, boolean canPassClosedWoodDoor, boolean scanOpenDoor) {
        World world = Entity.mcc$get$worldObj(entity);
        if (world == null) return 0;
        boolean special = false;
        for (int px = x; px < x + size.xCoord; px++) {
            for (int py = y; py < y + size.yCoord; py++) {
                for (int pz = z; pz < z + size.zCoord; pz++) {
                    Block block = world.getBlock(px, py, pz);
                    if (block.getMaterial() == Material.air) continue;
                    if (block == Blocks.trapdoor) {
                        special = true;
                    } else if (block == Blocks.flowing_water || block == Blocks.water) {
                        if (avoidWater) return -1;
                        special = true;
                    } else if (!scanOpenDoor && block == Blocks.wooden_door) {
                        return 0;
                    }
                    int renderType = block.getRenderType();
                    if (renderType == 9) {
                        int ex = MathHelper.floor_double(Entity.mcc$get$posX(entity));
                        int ey = MathHelper.floor_double(Entity.mcc$get$posY(entity));
                        int ez = MathHelper.floor_double(Entity.mcc$get$posZ(entity));
                        if (world.getBlock(ex, ey, ez).getRenderType() != 9 && world.getBlock(ex, ey - 1, ez).getRenderType() != 9) return -3;
                    } else if (!block.getBlocksMovement(world, px, py, pz) && (!canPassClosedWoodDoor || block != Blocks.wooden_door)) {
                        if (renderType == 11 || block == Blocks.fence_gate || renderType == 32) return -3;
                        if (block == Blocks.trapdoor) return -4;
                        if (block.getMaterial() != Material.lava) return 0;
                        if (!entity.handleLavaMovement()) return -2;
                    }
                }
            }
        }
        return special ? 2 : 1;
    }

    /** SRG name of {@link #canEntityStandAt}. */
    public static int func_82565_a(Entity entity, int x, int y, int z, PathPoint size, boolean avoidWater, boolean canPassClosedWoodDoor, boolean scanOpenDoor) {
        return canEntityStandAt(entity, x, y, z, size, avoidWater, canPassClosedWoodDoor, scanOpenDoor);
    }
}
