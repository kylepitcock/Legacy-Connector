package legacyconnector.v1_12_2.net.minecraft.entity;

/** Why an entity moves (its own motion, a player, a piston, a shulker). */
public enum MoverType {
    SELF, PLAYER, PISTON, SHULKER_BOX, SHULKER;

    public net.minecraft.world.entity.MoverType mcc$modern() {
        return net.minecraft.world.entity.MoverType.values()[ordinal()];
    }
}
