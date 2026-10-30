package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.entity.ExperienceOrb;

/** An experience orb: a view of a modern experience orb (an exact EntityXPOrb a mod constructs is a new modern orb). */
public class EntityXPOrb extends Entity {
    @Deprecated
    public int xpValue;
    @Deprecated
    public int delayBeforeCanPickup;
    @Deprecated
    public int xpOrbAge;
    private int mcc$localValue;

    public EntityXPOrb(World world, double x, double y, double z, int expValue) {
        super(world);
        setSize(0.5F, 0.5F);
        setPosition(x, y, z);
        mcc$set$rotationYaw(this, (float) (Math.random() * 360.0D));
        mcc$local(expValue);
        if (mcc$entity != null) mcc$entity.setDeltaMovement((Math.random() * 0.2D - 0.1D) * 2.0D, Math.random() * 0.2D * 2.0D, (Math.random() * 0.2D - 0.1D) * 2.0D);
    }

    public EntityXPOrb(World world) {
        super(world);
        setSize(0.25F, 0.25F);
    }

    public EntityXPOrb(ExperienceOrb modern) {
        super(modern);
    }

    /** Phantom: the orb's color cycle counter (the modern orb's tick count, which its renderer cycles on). */
    @Deprecated
    public int xpColor;
    private int mcc$local_xpColor;

    public static int mcc$get$xpColor(EntityXPOrb e) {
        return e.mcc$entity != null ? e.mcc$entity.tickCount : e.mcc$local_xpColor;
    }

    public static void mcc$set$xpColor(EntityXPOrb e, int v) {
        if (e.mcc$entity == null) e.mcc$local_xpColor = v;
    }

    private ExperienceOrb mcc$orb() {
        return mcc$entity instanceof ExperienceOrb o ? o : null;
    }

    private void mcc$local(int value) {
        mcc$localValue = value;
        var o = mcc$orb();
        if (o != null) org.kylepitcock.legacyconnector.v1_12_2.entity.OrbAccess1122.setValue(o, value);
    }

    public static int mcc$get$xpValue(EntityXPOrb e) {
        var o = e.mcc$orb();
        return o != null ? o.getValue() : e.mcc$localValue;
    }

    public static void mcc$set$xpValue(EntityXPOrb e, int v) {
        e.mcc$local(v);
    }

    public static int mcc$get$delayBeforeCanPickup(EntityXPOrb e) {
        return 0;
    }

    public static void mcc$set$delayBeforeCanPickup(EntityXPOrb e, int v) {
    }

    public static int mcc$get$xpOrbAge(EntityXPOrb e) {
        return mcc$get$ticksExisted(e);
    }

    public static void mcc$set$xpOrbAge(EntityXPOrb e, int v) {
    }

    public int getXpValue() {
        return mcc$get$xpValue(this);
    }

    public int getTextureByXP() {
        return mcc$orb() != null ? mcc$orb().getIcon() : 0;
    }

    public static int getXPSplit(int expValue) {
        return ExperienceOrb.getExperienceValue(expValue);
    }

    @Override
    public boolean canBeAttackedWithItem() {
        return false;
    }
}
