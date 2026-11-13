package legacyconnector.v1_7_10.net.minecraft.entity;

import net.minecraft.world.entity.MobCategory;

/** Spawn categories; each maps to its modern counterpart. */
public enum EnumCreatureType {
    monster(70, false, false, MobCategory.MONSTER, legacyconnector.v1_7_10.net.minecraft.entity.monster.IMob.class, null),
    creature(10, true, true, MobCategory.CREATURE, legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityAnimal.class, null),
    ambient(15, true, false, MobCategory.AMBIENT, legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityAmbientCreature.class, null),
    waterCreature(5, true, false, MobCategory.WATER_CREATURE, legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityWaterMob.class, null);

    private final int maxNumberOfCreature;
    private final boolean isPeacefulCreature;
    private final boolean isAnimal;
    private final MobCategory modern;
    private final Class<?> creatureClass;
    private final legacyconnector.v1_7_10.net.minecraft.block.material.Material creatureMaterial;

    EnumCreatureType(int max, boolean peaceful, boolean animal, MobCategory modern, Class<?> creatureClass,
            legacyconnector.v1_7_10.net.minecraft.block.material.Material material) {
        this.creatureClass = creatureClass;
        this.creatureMaterial = material;
        this.maxNumberOfCreature = max;
        this.isPeacefulCreature = peaceful;
        this.isAnimal = animal;
        this.modern = modern;
    }

    /**
     * Vanilla's own constructor {@code (Class, int, Material, boolean, boolean)}: mods add spawn categories with
     * {@code EnumHelper.addEnum(paramTable, EnumCreatureType.class, name, creatureClass, max, material, peaceful, animal)} and the
     * table names exactly these parameter types (#245). Peaceful types spawn under the modern creature category, others as monsters.
     */
    EnumCreatureType(Class<?> creatureClass, int max, legacyconnector.v1_7_10.net.minecraft.block.material.Material material,
            boolean peaceful, boolean animal) {
        this(max, peaceful, animal, peaceful ? MobCategory.CREATURE : MobCategory.MONSTER, creatureClass, material);
    }

    public Class<?> getCreatureClass() {
        return creatureClass;
    }

    public legacyconnector.v1_7_10.net.minecraft.block.material.Material getCreatureMaterial() {
        return creatureMaterial != null ? creatureMaterial
                : this == waterCreature ? legacyconnector.v1_7_10.net.minecraft.block.material.Material.water
                : legacyconnector.v1_7_10.net.minecraft.block.material.Material.air;
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
