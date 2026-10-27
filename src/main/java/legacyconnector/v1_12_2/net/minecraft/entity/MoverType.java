package legacyconnector.v1_12_2.net.minecraft.entity;

/** What moves an entity in {@code Entity.move}. */
public enum MoverType {
    SELF, PLAYER, PISTON, SHULKER_BOX, SHULKER;

    public net.minecraft.world.entity.MoverType mcc$modern() {
        return switch (this) {
            case SELF -> net.minecraft.world.entity.MoverType.SELF;
            case PLAYER -> net.minecraft.world.entity.MoverType.PLAYER;
            case PISTON -> net.minecraft.world.entity.MoverType.PISTON;
            case SHULKER_BOX -> net.minecraft.world.entity.MoverType.SHULKER_BOX;
            case SHULKER -> net.minecraft.world.entity.MoverType.SHULKER;
        };
    }
}
