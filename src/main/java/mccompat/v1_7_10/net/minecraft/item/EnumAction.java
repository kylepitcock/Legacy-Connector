package mccompat.v1_7_10.net.minecraft.item;

public enum EnumAction {
    none, eat, drink, block, bow;

    public net.minecraft.world.item.UseAnim mcc$modern() {
        return switch (this) {
            case none -> net.minecraft.world.item.UseAnim.NONE;
            case eat -> net.minecraft.world.item.UseAnim.EAT;
            case drink -> net.minecraft.world.item.UseAnim.DRINK;
            case block -> net.minecraft.world.item.UseAnim.BLOCK;
            case bow -> net.minecraft.world.item.UseAnim.BOW;
        };
    }
}
