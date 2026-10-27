package legacyconnector.v1_12_2.net.minecraft.util;

import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation;

public enum EnumHandSide {
    LEFT(new TextComponentTranslation("options.mainHand.left")),
    RIGHT(new TextComponentTranslation("options.mainHand.right"));

    private final ITextComponent handName;

    EnumHandSide(ITextComponent nameIn) {
        this.handName = nameIn;
    }

    public EnumHandSide opposite() {
        return this == LEFT ? RIGHT : LEFT;
    }

    @Override
    public String toString() {
        return handName.getUnformattedText();
    }

    public static EnumHandSide mcc$of(net.minecraft.world.entity.HumanoidArm arm) {
        return arm == net.minecraft.world.entity.HumanoidArm.LEFT ? LEFT : RIGHT;
    }

    public net.minecraft.world.entity.HumanoidArm mcc$modern() {
        return this == LEFT ? net.minecraft.world.entity.HumanoidArm.LEFT : net.minecraft.world.entity.HumanoidArm.RIGHT;
    }
}
