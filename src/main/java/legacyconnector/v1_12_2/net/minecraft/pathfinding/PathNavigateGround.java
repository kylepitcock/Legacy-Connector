package legacyconnector.v1_12_2.net.minecraft.pathfinding;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Walking navigation (doors, sun avoidance, swimming) over the modern ground navigation. */
public class PathNavigateGround extends PathNavigate {
    private final WalkNodeProcessor mcc$processor = new WalkNodeProcessor();

    public PathNavigateGround(EntityLiving entity, World world) {
        super(entity, world);
    }

    private net.minecraft.world.entity.ai.navigation.GroundPathNavigation mcc$ground() {
        return mcc$nav() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation g ? g : null;
    }

    @Override
    public NodeProcessor getNodeProcessor() {
        return mcc$processor;
    }

    public void setBreakDoors(boolean canBreakDoors) {
        mcc$processor.setCanOpenDoors(canBreakDoors);
        var g = mcc$ground();
        if (g != null) g.setCanOpenDoors(canBreakDoors);
    }

    public void setEnterDoors(boolean canEnterDoors) {
        mcc$processor.setCanEnterDoors(canEnterDoors);
        var g = mcc$ground();
        if (g != null) g.setCanPassDoors(canEnterDoors);
    }

    public boolean getEnterDoors() {
        return mcc$processor.getCanEnterDoors();
    }

    @Override
    public void setCanSwim(boolean canSwim) {
        mcc$processor.setCanSwim(canSwim);
        super.setCanSwim(canSwim);
    }

    public void setAvoidSun(boolean avoidSun) {
        var g = mcc$ground();
        if (g != null) g.setAvoidSun(avoidSun);
    }
}
