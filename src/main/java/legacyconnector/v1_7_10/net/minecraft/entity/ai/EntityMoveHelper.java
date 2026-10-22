package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.Mob;

/** Legacy move helper over the host's move control (ticked by the host unless a mod subclass replaces it). */
public class EntityMoveHelper {
    protected final EntityLiving entity;
    protected double posX;
    protected double posY;
    protected double posZ;
    protected double speed;
    protected boolean update;

    public EntityMoveHelper(EntityLiving entity) {
        this.entity = entity;
    }

    private Mob mob() {
        return entity.mcc$entity instanceof Mob m ? m : null;
    }

    public boolean isUpdating() {
        Mob m = mob();
        return getClass() == EntityMoveHelper.class && m != null ? m.getMoveControl().hasWanted() : update;
    }

    public double getSpeed() {
        return speed;
    }

    public void setMoveTo(double x, double y, double z, double speed) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.speed = speed;
        this.update = true;
        Mob m = mob();
        if (m != null && getClass() == EntityMoveHelper.class) m.getMoveControl().setWantedPosition(x, y, z, speed);
    }

    /** Vanilla movement is ticked by the host; mod subclasses override this and are ticked by EntityLiving. */
    public void onUpdateMoveHelper() {
    }
}
