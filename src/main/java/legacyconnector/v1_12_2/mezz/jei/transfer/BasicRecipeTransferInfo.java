package legacyconnector.v1_12_2.mezz.jei.transfer;

/** JEI 4 (Minecraft 1.12.2) internals some plugins reach into; here a small working copy. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public class BasicRecipeTransferInfo<C extends legacyconnector.v1_12_2.net.minecraft.inventory.Container> implements legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferInfo<C> {
    private final Class<C> containerClass;
    private final String recipeCategoryUid;
    private final int recipeSlotStart, recipeSlotCount, inventorySlotStart, inventorySlotCount;

    public BasicRecipeTransferInfo(Class<C> containerClass, String recipeCategoryUid, int recipeSlotStart, int recipeSlotCount, int inventorySlotStart, int inventorySlotCount) {
        this.containerClass = containerClass;
        this.recipeCategoryUid = recipeCategoryUid;
        this.recipeSlotStart = recipeSlotStart;
        this.recipeSlotCount = recipeSlotCount;
        this.inventorySlotStart = inventorySlotStart;
        this.inventorySlotCount = inventorySlotCount;
    }

    @Override
    public Class<C> getContainerClass() {
        return containerClass;
    }

    @Override
    public String getRecipeCategoryUid() {
        return recipeCategoryUid;
    }

    @Override
    public boolean canHandle(C container) {
        return true;
    }

    @Override
    public java.util.List<legacyconnector.v1_12_2.net.minecraft.inventory.Slot> getRecipeSlots(C container) {
        return slots(container, recipeSlotStart, recipeSlotCount);
    }

    @Override
    public java.util.List<legacyconnector.v1_12_2.net.minecraft.inventory.Slot> getInventorySlots(C container) {
        return slots(container, inventorySlotStart, inventorySlotCount);
    }

    private static java.util.List<legacyconnector.v1_12_2.net.minecraft.inventory.Slot> slots(legacyconnector.v1_12_2.net.minecraft.inventory.Container container, int start, int count) {
        java.util.List<legacyconnector.v1_12_2.net.minecraft.inventory.Slot> out = new java.util.ArrayList<>();
        for (int i = Math.max(0, start); i < start + count && i < container.inventorySlots.size(); i++) out.add(container.inventorySlots.get(i));
        return out;
    }
}
