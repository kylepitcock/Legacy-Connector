package legacyconnector.v1_7_10.net.minecraft.client.gui.inventory;

import legacyconnector.v1_7_10.net.minecraft.client.resources.I18n;
import legacyconnector.v1_7_10.net.minecraft.inventory.ContainerChest;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.7.10's chest screen over a {@link ContainerChest} of two inventories (the player's below, the chest's rows above). Mods open it on the
 * client for a chest-shaped inventory of their own ({@code displayGuiScreen(new GuiChest(player.inventory, chest))}); the screen is hosted by
 * the modern container screen like any legacy container GUI, so slots, clicks and dragging are the modern ones bound to the two inventories.
 */
public class GuiChest extends GuiContainer {
    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    private final IInventory upperChestInventory;
    private final IInventory lowerChestInventory;
    private final int inventoryRows;

    public GuiChest(IInventory upper, IInventory lower) {
        super(new ContainerChest(upper, lower));
        this.upperChestInventory = upper;
        this.lowerChestInventory = lower;
        this.allowUserInput = false;
        this.inventoryRows = lower.getSizeInventory() / 9;
        this.ySize = 114 + this.inventoryRows * 18;
    }

    private static String title(IInventory inventory) {
        return inventory.hasCustomInventoryName() ? inventory.getInventoryName() : I18n.format(inventory.getInventoryName());
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(title(lowerChestInventory), 8, 6, 4210752);
        fontRendererObj.drawString(title(upperChestInventory), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        RenderContext.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;
        drawTexturedModalRect(left, top, 0, 0, xSize, inventoryRows * 18 + 17);
        drawTexturedModalRect(left, top + inventoryRows * 18 + 17, 0, 126, xSize, 96);
    }
}
