package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 lead knot on a fence: {@link #createKnot} and {@link #getKnotForPosition} are the modern knot at that fence. */
public class EntityLeashKnot extends EntityHanging {
    public EntityLeashKnot(World world) {
        super(world);
    }

    public EntityLeashKnot(World world, BlockPos pos) {
        super(world, pos);
        setPosition(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        setSize(0.375F, 0.5F);
    }

    protected EntityLeashKnot(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    public static EntityLeashKnot createKnot(World world, BlockPos pos) {
        var level = world == null ? null : world.mcc$level();
        if (level == null || pos == null) return null;
        var knot = net.minecraft.world.entity.decoration.LeashFenceKnotEntity.getOrCreateKnot(level, pos.mcc$modern());
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(knot) instanceof EntityLeashKnot k ? k : null;
    }

    public static EntityLeashKnot getKnotForPosition(World world, BlockPos pos) {
        var level = world == null ? null : world.mcc$level();
        if (level == null || pos == null) return null;
        var p = pos.mcc$modern();
        for (var knot : level.getEntitiesOfClass(net.minecraft.world.entity.decoration.LeashFenceKnotEntity.class,
                new net.minecraft.world.phys.AABB(p).inflate(1.0D))) {
            if (knot.getPos().equals(p)) return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(knot) instanceof EntityLeashKnot k ? k : null;
        }
        return null;
    }

    @Override
    public int getWidthPixels() {
        return 9;
    }

    @Override
    public int getHeightPixels() {
        return 9;
    }

    @Override
    public float getEyeHeight() {
        return -0.0625F;
    }

    @Override
    public void onBroken(Entity brokenEntity) {
        playSound(org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.LEASH_KNOT_BREAK), 1.0F, 1.0F);
    }

    @Override
    public void playPlaceSound() {
        playSound(org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.LEASH_KNOT_PLACE), 1.0F, 1.0F);
    }

    @Override
    public boolean onValidSurface() {
        var m = mcc$entity;
        if (m instanceof net.minecraft.world.entity.decoration.LeashFenceKnotEntity k) return k.survives();
        BlockPos p = getHangingPosition();
        return m == null || p == null || m.level().getBlockState(p.mcc$modern()).is(net.minecraft.tags.BlockTags.FENCES);
    }
}
