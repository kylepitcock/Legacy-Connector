package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 3 API: the item list on the side of the screen. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IItemListOverlay {
    legacyconnector.v1_12_2.net.minecraft.item.ItemStack getStackUnderMouse();
    default void setFilterText(String filterText) {}
    default String getFilterText() {
        return "";
    }
    default com.google.common.collect.ImmutableList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getVisibleStacks() {
        return com.google.common.collect.ImmutableList.of();
    }
    default void highlightStacks(java.util.Collection<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> stacks) {}
}
