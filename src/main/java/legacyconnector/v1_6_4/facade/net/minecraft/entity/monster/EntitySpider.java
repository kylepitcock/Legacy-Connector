package legacyconnector.v1_6_4.facade.net.minecraft.entity.monster;

/** Facade of the 1.7.10 shim's {@code EntitySpider} for 1.4.7 and 1.5.2 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class EntitySpider {
    private EntitySpider() {}

    /** {@code spiderScaleAmount()}: the model scale of the spider (1.0; the cave spider's 0.7). */
    public static float spiderScaleAmount(legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySpider self) {
        return self instanceof legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityCaveSpider ? 0.7F : 1.0F;
    }
}
