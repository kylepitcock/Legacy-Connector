package legacyconnector.v1_12_2.net.minecraft.inventory;

/** The kinds of container clicks (same order as the modern {@code ClickType}). */
public enum ClickType {
    PICKUP,
    QUICK_MOVE,
    SWAP,
    CLONE,
    THROW,
    QUICK_CRAFT,
    PICKUP_ALL;

    public net.minecraft.world.inventory.ClickType mcc$modern() {
        return net.minecraft.world.inventory.ClickType.values()[ordinal()];
    }

    public static ClickType mcc$of(net.minecraft.world.inventory.ClickType type) {
        return values()[type.ordinal()];
    }
}
