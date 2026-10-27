package legacyconnector.v1_12_2.net.minecraft.util;

public enum EnumHand {
    MAIN_HAND, OFF_HAND;

    public net.minecraft.world.InteractionHand mcc$modern() {
        return this == MAIN_HAND ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND;
    }

    public static EnumHand mcc$of(net.minecraft.world.InteractionHand hand) {
        return hand == net.minecraft.world.InteractionHand.OFF_HAND ? OFF_HAND : MAIN_HAND;
    }
}
