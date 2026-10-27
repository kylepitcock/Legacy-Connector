package legacyconnector.v1_12_2.net.minecraft.util;

/** Left or right hand side (main arm of a player, arm of a model). */
public enum EnumHandSide {
    LEFT("options.mainHand.left"),
    RIGHT("options.mainHand.right");

    private final String handName;

    EnumHandSide(String nameIn) {
        handName = nameIn;
    }

    public EnumHandSide opposite() {
        return this == LEFT ? RIGHT : LEFT;
    }

    @Override
    public String toString() {
        return this == LEFT ? "Left" : "Right";
    }

    public static EnumHandSide mcc$of(net.minecraft.world.entity.HumanoidArm arm) {
        return arm == net.minecraft.world.entity.HumanoidArm.LEFT ? LEFT : RIGHT;
    }
}
