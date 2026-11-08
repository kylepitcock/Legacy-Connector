package legacyconnector.vb1_7_3.net.minecraft.entity;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import net.minecraft.world.entity.MobCategory;

/** Beta's three natural-spawn categories. */
public enum SpawnGroup {
    MONSTER(70,false,Material.AIR,MobCategory.MONSTER),
    CREATURE(15,true,Material.AIR,MobCategory.CREATURE),
    WATER_CREATURE(5,true,Material.WATER,MobCategory.WATER_CREATURE);
    // Beta mods legitimately mutate this mapped field through ModLoader reflection.
    private volatile int capacity;
    private final boolean peaceful;
    private final Material material;
    private final MobCategory modern;
    SpawnGroup(int capacity,boolean peaceful,Material material,MobCategory modern){this.capacity=capacity;this.peaceful=peaceful;this.material=material;this.modern=modern;}
    public Class<?> getCreatureClass(){return switch(this){case MONSTER->Monster.class;case CREATURE->legacyconnector.vb1_7_3.net.minecraft.entity.passive.AnimalEntity.class;case WATER_CREATURE->WaterCreatureEntity.class;};}
    public int getCapacity(){return capacity;}
    public Material getSpawnMaterial(){return material;}
    public boolean isPeaceful(){return peaceful;}
    public MobCategory mcc$modern(){return modern;}
}
