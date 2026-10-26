package legacyconnector.v1_7_10.net.minecraft.client.gui.inventory;

/**
 * 1.7.10's creative inventory. The modern game shows its own creative screen; this class carries the 1.7.10 members mods use
 * or find by reflection (Fisk's Heroes switches tabs and reads the scroll position).
 */
public class GuiContainerCreative extends legacyconnector.v1_7_10.net.minecraft.client.renderer.InventoryEffectRenderer {
    private static int selectedTabIndex = legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabBlock.getTabIndex();
    private float currentScroll;

    public GuiContainerCreative(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        super(player == null ? null : legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.mcc$get$inventoryContainer(player));
    }

    public int func_147056_g() {
        return selectedTabIndex;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    }

    private void setCurrentCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs tab) {
        if (tab != null) selectedTabIndex = tab.getTabIndex();
    }
}
