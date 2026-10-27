package legacyconnector.v1_12_2.net.minecraft.util.math;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** 1.12.2 ray trace hit: a block (position and side), an entity, or a miss. */
public class RayTraceResult {
    private BlockPos blockPos;
    public Type typeOfHit;
    public EnumFacing sideHit;
    public Vec3d hitVec;
    public Entity entityHit;
    /** Forge: which part of a block was hit, and extra data a block's ray trace attaches. */
    public int subHit = -1;
    public Object hitInfo;

    public RayTraceResult(Vec3d hitVec, EnumFacing side, BlockPos pos) {
        this(Type.BLOCK, hitVec, side, pos);
    }

    public RayTraceResult(Vec3d hitVec, EnumFacing side) {
        this(Type.BLOCK, hitVec, side, BlockPos.ORIGIN);
    }

    public RayTraceResult(Entity entity) {
        this(entity, new Vec3d(Entity.mcc$get$posX(entity), Entity.mcc$get$posY(entity), Entity.mcc$get$posZ(entity)));
    }

    public RayTraceResult(Type type, Vec3d hitVec, EnumFacing side, BlockPos pos) {
        this.typeOfHit = type;
        this.blockPos = pos;
        this.sideHit = side;
        this.hitVec = new Vec3d(hitVec.x, hitVec.y, hitVec.z);
    }

    public RayTraceResult(Entity entity, Vec3d hitVec) {
        this.typeOfHit = Type.ENTITY;
        this.entityHit = entity;
        this.hitVec = hitVec;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public String toString() {
        return "HitResult{type=" + typeOfHit + ", blockpos=" + blockPos + ", f=" + sideHit + ", pos=" + hitVec + ", entity=" + entityHit + '}';
    }

    /** The legacy result of a modern hit (null for none). */
    public static RayTraceResult mcc$of(net.minecraft.world.phys.HitResult hit) {
        if (hit == null) return null;
        Vec3d vec = new Vec3d(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        if (hit instanceof net.minecraft.world.phys.BlockHitResult b) {
            BlockPos pos = new BlockPos(b.getBlockPos().getX(), b.getBlockPos().getY(), b.getBlockPos().getZ());
            EnumFacing side = EnumFacing.byName(b.getDirection().getName());
            return new RayTraceResult(b.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? Type.MISS : Type.BLOCK, vec, side, pos);
        }
        if (hit instanceof net.minecraft.world.phys.EntityHitResult e) {
            Entity view = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e.getEntity());
            if (view != null) return new RayTraceResult(view, vec);
        }
        return new RayTraceResult(Type.MISS, vec, EnumFacing.UP, BlockPos.ORIGIN);
    }

    public enum Type {
        MISS,
        BLOCK,
        ENTITY
    }
}
