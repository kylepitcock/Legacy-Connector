package legacyconnector.v1_12_2.net.minecraft.block.material;

/** What a piston does to a block: push it, destroy it, stop at it, ignore it, or only push (never pull). */
public enum EnumPushReaction {
    NORMAL, DESTROY, BLOCK, IGNORE, PUSH_ONLY;

    public net.minecraft.world.level.material.PushReaction mcc$modern() {
        return switch (this) {
            case NORMAL -> net.minecraft.world.level.material.PushReaction.NORMAL;
            case DESTROY -> net.minecraft.world.level.material.PushReaction.DESTROY;
            case BLOCK -> net.minecraft.world.level.material.PushReaction.BLOCK;
            case IGNORE -> net.minecraft.world.level.material.PushReaction.IGNORE;
            case PUSH_ONLY -> net.minecraft.world.level.material.PushReaction.PUSH_ONLY;
        };
    }
}
