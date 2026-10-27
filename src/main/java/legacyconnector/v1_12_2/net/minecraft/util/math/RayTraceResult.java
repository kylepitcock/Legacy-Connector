package legacyconnector.v1_12_2.net.minecraft.util.math;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** A ray's hit: a block face, an entity or nothing. */
public class RayTraceResult {
    private BlockPos blockPos;
    public Type typeOfHit;
    public EnumFacing sideHit;
    public Vec3d hitVec;
    public Entity entityHit;
    /** Forge: the sub-part of the block that was hit. */
    public int subHit = -1;
    /** Forge: data a block attaches to its hit. */
    public Object hitInfo = null;

    public RayTraceResult(Vec3d hitVecIn, EnumFacing sideHitIn, BlockPos blockPosIn) {
        this(Type.BLOCK, hitVecIn, sideHitIn, blockPosIn);
    }

    public RayTraceResult(Vec3d hitVecIn, EnumFacing sideHitIn) {
        this(Type.BLOCK, hitVecIn, sideHitIn, BlockPos.ORIGIN);
    }

    public RayTraceResult(Entity entityIn) {
        this(entityIn, entityIn.getPositionVector());
    }

    public RayTraceResult(Type typeIn, Vec3d hitVecIn, EnumFacing sideHitIn, BlockPos blockPosIn) {
        this.typeOfHit = typeIn;
        this.blockPos = blockPosIn;
        this.sideHit = sideHitIn;
        this.hitVec = new Vec3d(hitVecIn.x, hitVecIn.y, hitVecIn.z);
    }

    public RayTraceResult(Entity entityHitIn, Vec3d hitVecIn) {
        this.typeOfHit = Type.ENTITY;
        this.entityHit = entityHitIn;
        this.hitVec = hitVecIn;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public String toString() {
        return "HitResult{type=" + typeOfHit + ", blockpos=" + blockPos + ", f=" + sideHit + ", pos=" + hitVec + ", entity=" + entityHit + '}';
    }

    /** The modern hit this one describes (null for a miss without a position). */
    public net.minecraft.world.phys.HitResult mcc$modern() {
        Vec3d v = hitVec == null ? Vec3d.ZERO : hitVec;
        if (typeOfHit == Type.ENTITY && entityHit != null && entityHit.mcc$entity != null) {
            return new net.minecraft.world.phys.EntityHitResult(entityHit.mcc$entity, v.mcc$modern());
        }
        net.minecraft.core.Direction side = sideHit == null ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.from3DDataValue(sideHit.getIndex());
        BlockPos p = blockPos == null ? BlockPos.ORIGIN : blockPos;
        net.minecraft.core.BlockPos mp = new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ());
        return typeOfHit == Type.BLOCK ? new net.minecraft.world.phys.BlockHitResult(v.mcc$modern(), side, mp, false)
                : net.minecraft.world.phys.BlockHitResult.miss(v.mcc$modern(), side, mp);
    }

    /** The legacy view of a modern hit (entities through their legacy views). */
    public static RayTraceResult mcc$of(net.minecraft.world.phys.HitResult hit) {
        if (hit == null) return null;
        Vec3d v = Vec3d.mcc$of(hit.getLocation());
        if (hit instanceof net.minecraft.world.phys.EntityHitResult e) {
            return new RayTraceResult(org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e.getEntity()), v);
        }
        if (hit instanceof net.minecraft.world.phys.BlockHitResult b) {
            BlockPos p = new BlockPos(b.getBlockPos().getX(), b.getBlockPos().getY(), b.getBlockPos().getZ());
            EnumFacing side = EnumFacing.byIndex(b.getDirection().get3DDataValue());
            return new RayTraceResult(b.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? Type.MISS : Type.BLOCK, v, side, p);
        }
        return new RayTraceResult(Type.MISS, v, EnumFacing.UP, BlockPos.ORIGIN);
    }

    public enum Type {
        MISS,
        BLOCK,
        ENTITY
    }
}
