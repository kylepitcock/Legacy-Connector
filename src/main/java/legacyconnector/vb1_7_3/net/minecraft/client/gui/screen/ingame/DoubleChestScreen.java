package legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame;

import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.screen.GenericContainerScreenHandler;
import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;

/** Beta 1.7.3 {@code GuiChest}: a chest's rows over the player's inventory, drawn from {@code /gui/container.png}. */
public class DoubleChestScreen extends HandledScreen {
    public Inventory playerInventory;
    public Inventory inventory;
    public int rows;

    public DoubleChestScreen(Inventory playerInventory, Inventory inventory) {
        super(new GenericContainerScreenHandler(playerInventory, inventory));
        this.playerInventory = playerInventory;
        this.inventory = inventory;
        this.passEvents = false;
        this.rows = inventory.size() / 9;
        this.backgroundHeight = 114 + rows * 18;
    }

    @Override
    protected void drawForeground() {
        textRenderer.draw(inventory.getName(), 8, 6, 0x404040);
        textRenderer.draw(playerInventory.getName(), 8, backgroundHeight - 96 + 2, 0x404040);
    }

    @Override
    protected void drawBackground(float delta) {
        GuiDrawB173.bind(minecraft.textureManager.getTextureId("/gui/container.png"));
        GuiDrawB173.color(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        drawTexture(x, y, 0, 0, backgroundWidth, rows * 18 + 17);
        drawTexture(x, y + rows * 18 + 17, 0, 126, backgroundWidth, 96);
    }
}
