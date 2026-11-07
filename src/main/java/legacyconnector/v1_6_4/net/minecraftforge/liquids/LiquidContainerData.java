package legacyconnector.v1_6_4.net.minecraftforge.liquids;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A liquid container pair: the filled item holds {@code stillLiquid}, the empty one is what it leaves (or fills from). */
public class LiquidContainerData {
    public final LiquidStack stillLiquid;
    public final ItemStack filled;
    public final ItemStack container;

    public LiquidContainerData(LiquidStack stillLiquid, ItemStack filled, ItemStack container) {
        this.stillLiquid = stillLiquid;
        this.filled = filled;
        this.container = container;
        if (stillLiquid == null || filled == null || container == null) throw new RuntimeException("stillLiquid, filled, or container is null, this is an error");
    }
}
