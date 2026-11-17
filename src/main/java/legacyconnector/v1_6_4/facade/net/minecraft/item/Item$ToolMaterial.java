package legacyconnector.v1_6_4.facade.net.minecraft.item;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code Item.ToolMaterial} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class Item$ToolMaterial {
    private Item$ToolMaterial() {}

    /** 1.6.4 {@code getToolCraftingMaterial()}: the id of the item that repairs tools of this tier (the base item of {@code getRepairItemStack}). */
    public static int getToolCraftingMaterial(legacyconnector.v1_7_10.net.minecraft.item.Item.ToolMaterial self) {
        legacyconnector.v1_7_10.net.minecraft.item.Item item = self.getBaseItemForRepair();
        return item == null ? 0 : Ids.itemId(item);
    }
}
