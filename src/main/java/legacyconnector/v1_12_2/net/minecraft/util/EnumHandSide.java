package legacyconnector.v1_12_2.net.minecraft.util;

import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation;

/** A player's main hand side, as 1.12.2's ({@code toString} is the translated option name). */
public enum EnumHandSide {
    LEFT(new TextComponentTranslation("options.mainHand.left")),
    RIGHT(new TextComponentTranslation("options.mainHand.right"));

    private final ITextComponent handName;

    EnumHandSide(ITextComponent handName) {
        this.handName = handName;
    }

    public EnumHandSide opposite() {
        return this == LEFT ? RIGHT : LEFT;
    }

    @Override
    public String toString() {
        return handName.getUnformattedText();
    }

    public net.minecraft.world.entity.HumanoidArm mcc$modern() {
        return this == LEFT ? net.minecraft.world.entity.HumanoidArm.LEFT : net.minecraft.world.entity.HumanoidArm.RIGHT;
    }

    public static EnumHandSide mcc$of(net.minecraft.world.entity.HumanoidArm arm) {
        return arm == net.minecraft.world.entity.HumanoidArm.LEFT ? LEFT : RIGHT;
    }
}
