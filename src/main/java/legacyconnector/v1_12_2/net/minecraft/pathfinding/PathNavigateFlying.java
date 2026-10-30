package legacyconnector.v1_12_2.net.minecraft.pathfinding;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A mob navigation over the modern navigation of the mob (the host's navigation does the moving). */
public class PathNavigateFlying extends PathNavigate {
    public PathNavigateFlying(EntityLiving entity, World world) {
        super(entity, world);
    }

    private NodeProcessor mcc$processor() {
        if (nodeProcessor == null) getPathFinder();
        return nodeProcessor;
    }

    @Override
    protected boolean canNavigate() {
        return entity != null && (canFloat() && isInLiquid() || !entity.isRiding());
    }

    @Override
    protected Vec3d getEntityPosition() {
        return new Vec3d(entity.mcc$x(), entity.mcc$y(), entity.mcc$z());
    }

    /** A straight flight: every block on the line between the points is air. */
    @Override
    protected boolean isDirectPathBetweenPoints(Vec3d from, Vec3d to, int sizeX, int sizeY, int sizeZ) {
        World w = legacyconnector.v1_12_2.net.minecraft.entity.Entity.mcc$get$world(entity);
        if (w == null) return false;
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4D) return false;
        int steps = MathHelper.ceil(len * 2.0D);
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            if (!w.isAirBlock(new BlockPos(from.x + dx * t, from.y + dy * t, from.z + dz * t))) return false;
        }
        return true;
    }

    public void setCanOpenDoors(boolean canOpenDoors) {
        mcc$processor().setCanOpenDoors(canOpenDoors);
    }

    public void setCanEnterDoors(boolean canEnterDoors) {
        mcc$processor().setCanEnterDoors(canEnterDoors);
    }

    public void setCanFloat(boolean canFloat) {
        mcc$processor().setCanSwim(canFloat);
        super.setCanSwim(canFloat);
    }

    public boolean canFloat() {
        return mcc$processor().getCanSwim();
    }

    @Override
    public boolean canEntityStandOnPos(BlockPos pos) {
        World w = legacyconnector.v1_12_2.net.minecraft.entity.Entity.mcc$get$world(entity);
        return w != null && w.getBlockState(pos).isTopSolid();
    }
}
