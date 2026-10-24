package legacyconnector.v1_7_10.net.minecraft.util;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

/** Result of a ray trace: a block face, an entity, or a miss. */
public class MovingObjectPosition {
    public MovingObjectType typeOfHit;
    public int blockX;
    public int blockY;
    public int blockZ;
    /** Side of the block that was hit: 0 bottom, 1 top, 2 north, 3 south, 4 west, 5 east. */
    public int sideHit;
    public Vec3 hitVec;
    public Entity entityHit;
    /** Forge: which sub-part of a multi-part block was hit. */
    public int subHit = -1;

    public MovingObjectPosition(int x, int y, int z, int side, Vec3 hit) {
        this(x, y, z, side, hit, true);
    }

    public MovingObjectPosition(int x, int y, int z, int side, Vec3 hit, boolean block) {
        this.typeOfHit = block ? MovingObjectType.BLOCK : MovingObjectType.MISS;
        this.blockX = x;
        this.blockY = y;
        this.blockZ = z;
        this.sideHit = side;
        this.hitVec = Vec3.createVectorHelper(hit.xCoord, hit.yCoord, hit.zCoord);
    }

    public MovingObjectPosition(Entity entity) {
        this(entity, Vec3.createVectorHelper(Entity.mcc$get$posX(entity), Entity.mcc$get$posY(entity), Entity.mcc$get$posZ(entity)));
    }

    public MovingObjectPosition(Entity entity, Vec3 hit) {
        this.typeOfHit = MovingObjectType.ENTITY;
        this.entityHit = entity;
        this.hitVec = hit;
    }

    @Override
    public String toString() {
        return "HitResult{type=" + typeOfHit + ", x=" + blockX + ", y=" + blockY + ", z=" + blockZ + ", f=" + sideHit + ", pos=" + hitVec + ", entity=" + entityHit + '}';
    }

    public enum MovingObjectType {
        MISS, BLOCK, ENTITY
    }
}
