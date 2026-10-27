package legacyconnector.v1_12_2.net.minecraft.util;

public enum EnumActionResult {
    SUCCESS, PASS, FAIL;

    public net.minecraft.world.InteractionResult mcc$modern() {
        return switch (this) {
            case SUCCESS -> net.minecraft.world.InteractionResult.SUCCESS;
            case PASS -> net.minecraft.world.InteractionResult.PASS;
            case FAIL -> net.minecraft.world.InteractionResult.FAIL;
        };
    }

    public static EnumActionResult mcc$of(net.minecraft.world.InteractionResult result) {
        if (result == null) return PASS;
        if (result.consumesAction()) return SUCCESS;
        return result == net.minecraft.world.InteractionResult.FAIL ? FAIL : PASS;
    }
}
