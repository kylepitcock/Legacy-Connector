package legacyconnector.v1_6_4.facade.net.minecraft.entity.monster;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code EntityEnderman} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class EntityEnderman {
    private EntityEnderman() {}

    /** 1.6.4 {@code getCarried()}: the id of the block the enderman carries (0 for none). */
    public static int getCarried(legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityEnderman self) {
        return Ids.blockId(self.getCarriedBlock());
    }
}
