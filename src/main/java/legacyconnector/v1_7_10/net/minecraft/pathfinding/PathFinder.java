package legacyconnector.v1_7_10.net.minecraft.pathfinding;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
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
}
