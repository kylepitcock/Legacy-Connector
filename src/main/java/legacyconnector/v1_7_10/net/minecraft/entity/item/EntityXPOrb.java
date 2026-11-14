package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.ExperienceOrb;

/** An experience orb, a view of the modern one. */
public class EntityXPOrb extends Entity {
    public EntityXPOrb(World world, double x, double y, double z, int value) {
        super(new ExperienceOrb(world.mcc$fullLevel(), x, y, z, value));
    }

    public EntityXPOrb(ExperienceOrb modern) {
        super(modern);
    }

    /** An orb worth nothing yet (mods' orb subclasses and spawn packets construct it this way); a view of a modern orb. */
    public EntityXPOrb(World world) {
        super(mcc$emptyOrb(world));
    }

    private static ExperienceOrb mcc$emptyOrb(World world) {
        var level = world == null ? null : world.mcc$fullLevel();
        return level == null ? null : new ExperienceOrb(net.minecraft.world.entity.EntityType.EXPERIENCE_ORB, level);
    }

    /** Phantom field: the experience the orb is worth (machines drain it into tanks by writing it down). */
    @Deprecated
    public int xpValue;

    private static final java.lang.reflect.Field VALUE = find();

    private static java.lang.reflect.Field find() {
        try {
            return net.neoforged.fml.util.ObfuscationReflectionHelper.findField(ExperienceOrb.class, "value");
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static int mcc$get$xpValue(EntityXPOrb e) {
        return e.mcc$entity instanceof ExperienceOrb o ? o.getValue() : 0;
    }

    public static void mcc$set$xpValue(EntityXPOrb e, int v) {
        if (!(e.mcc$entity instanceof ExperienceOrb o) || VALUE == null) return;
        try {
            VALUE.setInt(o, v);
        } catch (IllegalAccessException ex) {
            // value stays
        }
    }

    public int getXpValue() {
        return ((ExperienceOrb) mcc$entity).getValue();
    }

    public int getTextureByXP() {
        return ((ExperienceOrb) mcc$entity).getIcon();
    }

    public static int getXPSplit(int value) {
        return ExperienceOrb.getExperienceValue(value);
    }
}
