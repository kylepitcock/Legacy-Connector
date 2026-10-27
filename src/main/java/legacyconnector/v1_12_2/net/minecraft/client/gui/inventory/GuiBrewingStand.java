package legacyconnector.v1_12_2.net.minecraft.client.gui.inventory;

import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.gui.ModernContainer1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's brewing stand screen over a client-side modern brewing menu (1.12's ContainerBrewingStand layout: three bottles, ingredient,
 * fuel, inventory). Fuel and brew time come from the brewing stand inventory's fields 1 and 0, as in 1.12; the game's own brewing screen
 * is announced to mods as one of these.
 */
public class GuiBrewingStand extends GuiContainer {
    private static final ResourceLocation BREWING_STAND_GUI_TEXTURES = new ResourceLocation("textures/gui/container/brewing_stand.png");
    private static final int[] BUBBLELENGTHS = new int[] {29, 24, 20, 16, 11, 6, 0};
    private final InventoryPlayer playerInventory;
    private final IInventory tileBrewingStand;

    public GuiBrewingStand(InventoryPlayer playerInv, IInventory inventoryIn) {
        super(mcc$container(playerInv));
        this.playerInventory = playerInv;
        this.tileBrewingStand = inventoryIn;
    }

    private static legacyconnector.v1_12_2.net.minecraft.inventory.Container mcc$container(InventoryPlayer playerInv) {
        net.minecraft.world.entity.player.Player player = playerInv != null && playerInv.player != null ? playerInv.player.mcc$player()
                : net.minecraft.client.Minecraft.getInstance().player;
        return player == null ? null : ModernContainer1122.of(new net.minecraft.world.inventory.BrewingStandMenu(0, player.getInventory()));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String s = tileBrewingStand == null ? I18n.format("container.brewing") : tileBrewingStand.getName();
        s = I18n.hasKey(s) ? I18n.format(s) : s;
        fontRenderer.drawString(s, xSize / 2 - fontRenderer.getStringWidth(s) / 2, 6, 4210752);
        String inv = playerInventory == null ? I18n.format("container.inventory") : playerInventory.getName();
        fontRenderer.drawString(I18n.hasKey(inv) ? I18n.format(inv) : inv, 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        RenderContext.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(BREWING_STAND_GUI_TEXTURES);
        int i = guiLeft;
        int j = guiTop;
        drawTexturedModalRect(i, j, 0, 0, xSize, ySize);
        var gui = RenderContext.gui();
        if (gui == null || tileBrewingStand == null) return;
        int fuel = tileBrewingStand.getField(1);
        int l = Math.max(0, Math.min(18, (18 * fuel + 20 - 1) / 20));
        if (l > 0) gui.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace("container/brewing_stand/fuel_length"), 18, 4, 0, 0, i + 60, j + 44, l, 4);
        int brewTime = tileBrewingStand.getField(0);
        if (brewTime > 0) {
            int j1 = (int) (28.0F * (1.0F - (float) brewTime / 400.0F));
            if (j1 > 0) gui.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace("container/brewing_stand/brew_progress"), 9, 28, 0, 0, i + 97, j + 16, 9, j1);
            j1 = BUBBLELENGTHS[brewTime / 2 % 7];
            if (j1 > 0) gui.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace("container/brewing_stand/bubbles"), 12, 29, 0, 29 - j1, i + 63, j + 14 + 29 - j1, 12, j1);
        }
    }
}
