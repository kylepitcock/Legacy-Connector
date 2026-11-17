package legacyconnector.v1_6_4.facade.net.minecraft.entity.monster;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code EntityEnderman} for 1.4.7 and 1.5.2 mods: the carried block as an id. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class EntityEnderman {
    private EntityEnderman() {}

    /** {@code getCarried()}: the id of the carried block, 0 when it carries none. */
    public static int getCarried(legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityEnderman self) {
        legacyconnector.v1_7_10.net.minecraft.block.Block block = self.getCarriedBlock();
        return block == null || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.air ? 0 : Ids.blockId(block);
    }
}
