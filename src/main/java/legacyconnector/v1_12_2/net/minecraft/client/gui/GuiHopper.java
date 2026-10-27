package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.gui.ModernContainer1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's hopper screen: five slots over the player's inventory. The slots are a client-side modern hopper menu's (the layout of 1.12's
 * ContainerHopper), filled by the server's container; the game's own hopper screen is announced to mods as one of these.
 */
public class GuiHopper extends GuiContainer {
    private static final ResourceLocation HOPPER_GUI_TEXTURE = new ResourceLocation("textures/gui/container/hopper.png");
    private final IInventory playerInventory;
    private final IInventory hopperInventory;

    public GuiHopper(legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer playerInv, IInventory hopperInv) {
        super(mcc$container(playerInv));
        this.playerInventory = playerInv;
        this.hopperInventory = hopperInv;
        this.allowUserInput = false;
        this.ySize = 133;
    }

    private static legacyconnector.v1_12_2.net.minecraft.inventory.Container mcc$container(legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer playerInv) {
        net.minecraft.world.entity.player.Player player = playerInv != null && playerInv.player != null ? playerInv.player.mcc$player()
                : net.minecraft.client.Minecraft.getInstance().player;
        return player == null ? null : ModernContainer1122.of(new net.minecraft.world.inventory.HopperMenu(0, player.getInventory()));
    }

    public IInventory mcc$hopperInventory() {
        return hopperInventory;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = hopperInventory == null ? I18n.format("container.hopper") : hopperInventory.getName();
        fontRenderer.drawString(I18n.hasKey(title) ? I18n.format(title) : title, 8, 6, 4210752);
        String inv = playerInventory == null ? I18n.format("container.inventory") : playerInventory.getName();
        fontRenderer.drawString(I18n.hasKey(inv) ? I18n.format(inv) : inv, 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        RenderContext.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(HOPPER_GUI_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}
