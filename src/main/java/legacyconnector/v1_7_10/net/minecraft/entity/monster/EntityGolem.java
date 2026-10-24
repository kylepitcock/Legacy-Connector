package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.IAnimals;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Base of golems (iron golem, snow golem, mods' constructs). */
public abstract class EntityGolem extends EntityCreature implements IAnimals {
    public EntityGolem(World world) {
        super(world);
    }

    @Override
    public int getTalkInterval() {
        return 120;
    }
}
