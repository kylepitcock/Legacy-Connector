package mccompat.v1_7_10.net.minecraft.entity;

import net.minecraft.world.entity.MobCategory;

/** Spawn categories; each maps to its modern counterpart. */
public enum EnumCreatureType {
    monster(70, false, false, MobCategory.MONSTER),
    creature(10, true, true, MobCategory.CREATURE),
    ambient(15, true, false, MobCategory.AMBIENT),
    waterCreature(5, true, false, MobCategory.WATER_CREATURE);

    private final int maxNumberOfCreature;
    private final boolean isPeacefulCreature;
    private final boolean isAnimal;
    private final MobCategory modern;

    EnumCreatureType(int max, boolean peaceful, boolean animal, MobCategory modern) {
        this.maxNumberOfCreature = max;
        this.isPeacefulCreature = peaceful;
        this.isAnimal = animal;
        this.modern = modern;
    }

    public int getMaxNumberOfCreature() {
        return maxNumberOfCreature;
    }

    public boolean getPeacefulCreature() {
        return isPeacefulCreature;
    }

    public boolean getAnimal() {
        return isAnimal;
    }

    public MobCategory mcc$modern() {
        return modern;
    }
}
