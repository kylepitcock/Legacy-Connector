package legacyconnector.v1_12_2.net.minecraft.inventory;

/** 1.12.2 click kinds; same constants in the same order as the modern {@code ClickType}. */
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

    public static ClickType mcc$of(net.minecraft.world.inventory.ClickType modern) {
        return modern == null ? PICKUP : values()[modern.ordinal()];
    }
}
