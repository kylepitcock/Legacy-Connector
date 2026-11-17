package legacyconnector.v1_6_4.facade.net.minecraft.entity.item;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code EntityFallingBlock} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class EntityFallingBlock {
    private EntityFallingBlock() {}

    /** 1.6.4 {@code blockID}: the id of the falling block. */
    public static int mcc$get$blockID(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityFallingBlock self) {
        return Ids.blockId(self.func_145805_f());
    }
}
