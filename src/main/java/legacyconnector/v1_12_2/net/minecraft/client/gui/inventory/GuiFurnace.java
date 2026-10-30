package legacyconnector.v1_12_2.net.minecraft.client.gui.inventory;

import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.gui.ModernContainer1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's furnace screen: input, fuel and output over the player's inventory, with the flame and arrow drawn from the furnace's fields
 * (0 burn time, 1 item burn time, 2 cook time, 3 total cook time). The slots are a client-side modern furnace menu's.
 */
public class GuiFurnace extends GuiContainer {
    private static final ResourceLocation FURNACE_GUI_TEXTURES = new ResourceLocation("textures/gui/container/furnace.png");
    private final InventoryPlayer playerInventory;
    private final IInventory tileFurnace;

    public GuiFurnace(InventoryPlayer playerInv, IInventory furnaceInv) {
        super(mcc$container(playerInv));
        this.playerInventory = playerInv;
        this.tileFurnace = furnaceInv;
    }

    private static legacyconnector.v1_12_2.net.minecraft.inventory.Container mcc$container(InventoryPlayer playerInv) {
        net.minecraft.world.entity.player.Player player = playerInv != null && playerInv.player != null ? playerInv.player.mcc$player()
                : net.minecraft.client.Minecraft.getInstance().player;
        return player == null ? null : ModernContainer1122.of(new net.minecraft.world.inventory.FurnaceMenu(0, player.getInventory()));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String s = tileFurnace == null ? I18n.format("container.furnace") : tileFurnace.getDisplayName().getUnformattedText();
        fontRenderer.drawString(s, xSize / 2 - fontRenderer.getStringWidth(s) / 2, 6, 4210752);
        String inv = playerInventory == null ? I18n.format("container.inventory") : playerInventory.getDisplayName().getUnformattedText();
        fontRenderer.drawString(inv, 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        RenderContext.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(FURNACE_GUI_TEXTURES);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        if (field(0) > 0) {
            int k = getBurnLeftScaled(13);
            drawTexturedModalRect(guiLeft + 56, guiTop + 36 + 12 - k, 176, 12 - k, 14, k + 1);
        }
        int l = getCookProgressScaled(24);
        drawTexturedModalRect(guiLeft + 79, guiTop + 34, 176, 14, l + 1, 16);
    }

    private int field(int id) {
        return tileFurnace == null || tileFurnace.getFieldCount() <= id ? 0 : tileFurnace.getField(id);
    }

    private int getCookProgressScaled(int pixels) {
        int i = field(2), j = field(3);
        return j != 0 && i != 0 ? i * pixels / j : 0;
    }

    private int getBurnLeftScaled(int pixels) {
        int i = field(1);
        if (i == 0) i = 200;
        return field(0) * pixels / i;
    }
}
