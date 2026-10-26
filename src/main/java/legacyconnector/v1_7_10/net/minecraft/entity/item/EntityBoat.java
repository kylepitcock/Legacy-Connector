package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.vehicle.Boat;

/**
 * 1.7.10 boat. A mod's boat keeps 1.7.10's watcher slots (17 time since hit, 18 forward direction, 19 damage taken); a view of a
 * modern boat reads and writes the boat's own hurt time, hurt direction and damage.
 */
public class EntityBoat extends Entity {
    private boolean isBoatEmpty = true;

    public EntityBoat(World world) {
        super(world);
        preventEntitySpawning = true;
        setSize(1.5F, 0.6F);
        yOffset = 0.3F;
    }

    public EntityBoat(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y + yOffset, z);
    }

    /** View of a modern boat (mods test and cast vanilla boats against this class). */
    public EntityBoat(Boat modern) {
        super(modern);
    }

    private Boat mcc$boat() {
        return mcc$entity instanceof Boat b && !mcc$hosted() ? b : null;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(17, Integer.valueOf(0));
        dataWatcher.addObject(18, Integer.valueOf(1));
        dataWatcher.addObject(19, Float.valueOf(0.0F));
    }

    public void setDamageTaken(float damage) {
        var boat = mcc$boat();
        if (boat != null) boat.setDamage(damage);
        else dataWatcher.updateObject(19, Float.valueOf(damage));
    }

    public float getDamageTaken() {
        var boat = mcc$boat();
        return boat != null ? boat.getDamage() : dataWatcher.getWatchableObjectFloat(19);
    }

    /** Ticks the hurt wobble still lasts (the modern hurt time). */
    public void setTimeSinceHit(int ticks) {
        var boat = mcc$boat();
        if (boat != null) boat.setHurtTime(ticks);
        else dataWatcher.updateObject(17, Integer.valueOf(ticks));
    }

    public int getTimeSinceHit() {
        var boat = mcc$boat();
        return boat != null ? boat.getHurtTime() : dataWatcher.getWatchableObjectInt(17);
    }

    /** Side the wobble swings to, 1 or -1 (the modern hurt direction). */
    public void setForwardDirection(int direction) {
        var boat = mcc$boat();
        if (boat != null) boat.setHurtDir(direction);
        else dataWatcher.updateObject(18, Integer.valueOf(direction));
    }

    public int getForwardDirection() {
        var boat = mcc$boat();
        return boat != null ? boat.getHurtDir() : dataWatcher.getWatchableObjectInt(18);
    }

    /** Client: whether nobody rides the boat (it then drifts instead of following the server closely). */
    public void setIsBoatEmpty(boolean empty) {
        this.isBoatEmpty = empty;
    }
}
