package legacyconnector.v1_12_2.net.minecraft.client.gui.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.gui.ModernContainer1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's beacon screen over a client-side modern beacon menu (1.12's ContainerBeacon layout: the payment slot, then the inventory).
 * The game's own beacon screen is announced to mods as one of these; subclasses draw the 1.12 background (the effect buttons are the
 * modern screen's).
 */
public class GuiBeacon extends GuiContainer {
    private static final ResourceLocation BEACON_GUI_TEXTURES = new ResourceLocation("textures/gui/container/beacon.png");
    private final IInventory tileBeacon;

    public GuiBeacon(InventoryPlayer playerInventory, IInventory tileBeaconIn) {
        super(mcc$container(playerInventory));
        this.tileBeacon = tileBeaconIn;
        this.xSize = 230;
        this.ySize = 219;
    }

    private static legacyconnector.v1_12_2.net.minecraft.inventory.Container mcc$container(InventoryPlayer playerInv) {
        net.minecraft.world.entity.player.Player player = playerInv != null && playerInv.player != null ? playerInv.player.mcc$player()
                : net.minecraft.client.Minecraft.getInstance().player;
        return player == null ? null : ModernContainer1122.of(new net.minecraft.world.inventory.BeaconMenu(0, player.getInventory()));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        RenderContext.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BEACON_GUI_TEXTURES);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}
