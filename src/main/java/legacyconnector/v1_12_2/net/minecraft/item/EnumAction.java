package legacyconnector.v1_12_2.net.minecraft.item;

/** How an item is used while the use key is held (the animation): nothing, eating, drinking, blocking, drawing a bow. */
public enum EnumAction {
    NONE, EAT, DRINK, BLOCK, BOW;

    public net.minecraft.world.item.UseAnim mcc$modern() {
        return switch (this) {
            case NONE -> net.minecraft.world.item.UseAnim.NONE;
            case EAT -> net.minecraft.world.item.UseAnim.EAT;
            case DRINK -> net.minecraft.world.item.UseAnim.DRINK;
            case BLOCK -> net.minecraft.world.item.UseAnim.BLOCK;
            case BOW -> net.minecraft.world.item.UseAnim.BOW;
        };
    }
}
