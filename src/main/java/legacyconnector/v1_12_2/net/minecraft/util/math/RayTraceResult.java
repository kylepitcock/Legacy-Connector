package legacyconnector.v1_12_2.net.minecraft.util.math;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** What a ray hit: a block (position and side), an entity, or nothing; as 1.12.2's, with Forge's {@code subHit}/{@code hitInfo}. */
public class RayTraceResult {
    private BlockPos blockPos;
    public Type typeOfHit;
    public EnumFacing sideHit;
    public Vec3d hitVec;
    public Entity entityHit;
    /** Forge: the sub-part of the block or entity that was hit. */
    public int subHit = -1;
    /** Forge: anything the ray tracer wants to attach. */
    public Object hitInfo = null;

    public RayTraceResult(Vec3d hitVecIn, EnumFacing sideHitIn, BlockPos blockPosIn) {
        this(Type.BLOCK, hitVecIn, sideHitIn, blockPosIn);
    }

    public RayTraceResult(Vec3d hitVecIn, EnumFacing sideHitIn) {
        this(Type.BLOCK, hitVecIn, sideHitIn, BlockPos.ORIGIN);
    }

    public RayTraceResult(Entity entityIn) {
        this(entityIn, new Vec3d(Entity.mcc$get$posX(entityIn), Entity.mcc$get$posY(entityIn), Entity.mcc$get$posZ(entityIn)));
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

    /** The legacy view of a modern hit result (null stays null). */
    public static RayTraceResult mcc$of(net.minecraft.world.phys.HitResult hit) {
        if (hit == null) return null;
        Vec3d at = Vec3d.mcc$of(hit.getLocation());
        if (hit instanceof net.minecraft.world.phys.EntityHitResult e) {
            Entity view = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e.getEntity());
            if (view != null) return new RayTraceResult(view, at);
            return new RayTraceResult(Type.MISS, at, EnumFacing.UP, BlockPos.ORIGIN);
        }
        if (hit instanceof net.minecraft.world.phys.BlockHitResult b) {
            return new RayTraceResult(b.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? Type.MISS : Type.BLOCK, at,
                    EnumFacing.mcc$of(b.getDirection()), BlockPos.mcc$of(b.getBlockPos()));
        }
        return new RayTraceResult(Type.MISS, at, EnumFacing.UP, BlockPos.ORIGIN);
    }

    /** The modern hit result this stands for. */
    public net.minecraft.world.phys.HitResult mcc$modern() {
        net.minecraft.world.phys.Vec3 at = hitVec == null ? net.minecraft.world.phys.Vec3.ZERO : hitVec.mcc$modern();
        if (typeOfHit == Type.ENTITY && entityHit != null && entityHit.mcc$entity != null) {
            return new net.minecraft.world.phys.EntityHitResult(entityHit.mcc$entity, at);
        }
        net.minecraft.core.Direction side = sideHit == null ? net.minecraft.core.Direction.UP : sideHit.mcc$modern();
        net.minecraft.core.BlockPos pos = blockPos == null ? net.minecraft.core.BlockPos.containing(at) : blockPos.mcc$modern();
        return typeOfHit == Type.BLOCK ? new net.minecraft.world.phys.BlockHitResult(at, side, pos, false)
                : net.minecraft.world.phys.BlockHitResult.miss(at, side, pos);
    }

    public enum Type {
        MISS,
        BLOCK,
        ENTITY
    }
}
