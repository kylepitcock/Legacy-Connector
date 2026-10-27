package legacyconnector.v1_12_2.net.minecraft.pathfinding;

/** How a mob's paths are found; the modern pathfinder of the mob is used instead (mods' processors only keep their settings). */
public abstract class NodeProcessor {
    protected boolean canEnterDoors;
    protected boolean canOpenDoors;
    protected boolean canSwim;

    public void setCanEnterDoors(boolean value) {
        canEnterDoors = value;
    }

    public void setCanOpenDoors(boolean value) {
        canOpenDoors = value;
    }

    public void setCanSwim(boolean value) {
        canSwim = value;
    }

    public boolean getCanEnterDoors() {
        return canEnterDoors;
    }

    public boolean getCanOpenDoors() {
        return canOpenDoors;
    }

    public boolean getCanSwim() {
        return canSwim;
    }

    public void postProcess() {
    }
}
