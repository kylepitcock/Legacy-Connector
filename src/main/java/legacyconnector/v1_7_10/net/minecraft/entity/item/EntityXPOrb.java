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
