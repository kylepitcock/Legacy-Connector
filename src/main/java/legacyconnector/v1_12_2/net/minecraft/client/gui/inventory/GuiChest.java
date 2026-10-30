package legacyconnector.v1_12_2.net.minecraft.client.gui.inventory;

import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.gui.ModernContainer1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's chest screen: one to six rows of nine slots over the player's inventory. The slots are a client-side modern chest menu's of
 * that size (the layout of 1.12's ContainerChest), filled by the server's container.
 */
public class GuiChest extends GuiContainer {
    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    private final IInventory upperChestInventory;
    private final IInventory lowerChestInventory;
    private final int inventoryRows;

    public GuiChest(IInventory upperInv, IInventory lowerInv) {
        super(mcc$container(upperInv, rows(lowerInv)));
        this.upperChestInventory = upperInv;
        this.lowerChestInventory = lowerInv;
        this.allowUserInput = false;
        this.inventoryRows = rows(lowerInv);
        this.ySize = 114 + inventoryRows * 18;
    }

    private static int rows(IInventory inv) {
        int size = inv == null ? 27 : inv.getSizeInventory();
        return Math.max(1, Math.min(6, size / 9));
    }

    private static legacyconnector.v1_12_2.net.minecraft.inventory.Container mcc$container(IInventory upperInv, int rows) {
        net.minecraft.world.entity.player.Player player = upperInv instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer inv && inv.player != null
                ? inv.player.mcc$player() : net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return null;
        net.minecraft.world.inventory.MenuType<?> type = switch (rows) {
            case 1 -> net.minecraft.world.inventory.MenuType.GENERIC_9x1;
            case 2 -> net.minecraft.world.inventory.MenuType.GENERIC_9x2;
            case 4 -> net.minecraft.world.inventory.MenuType.GENERIC_9x4;
            case 5 -> net.minecraft.world.inventory.MenuType.GENERIC_9x5;
            case 6 -> net.minecraft.world.inventory.MenuType.GENERIC_9x6;
            default -> net.minecraft.world.inventory.MenuType.GENERIC_9x3;
        };
        return ModernContainer1122.of(new net.minecraft.world.inventory.ChestMenu(type, 0, player.getInventory(), new net.minecraft.world.SimpleContainer(rows * 9), rows));
    }

    public IInventory getLowerChestInventory() {
        return lowerChestInventory;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = lowerChestInventory == null ? I18n.format("container.chest") : lowerChestInventory.getDisplayName().getUnformattedText();
        fontRenderer.drawString(title, 8, 6, 4210752);
        String inv = upperChestInventory == null ? I18n.format("container.inventory") : upperChestInventory.getDisplayName().getUnformattedText();
        fontRenderer.drawString(inv, 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        RenderContext.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, inventoryRows * 18 + 17);
        drawTexturedModalRect(guiLeft, guiTop + inventoryRows * 18 + 17, 0, 126, xSize, 96);
    }
}
