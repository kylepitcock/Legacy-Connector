package legacyconnector.v1_7_10.net.minecraft.client.gui.inventory;

import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.inventory.Container;
import legacyconnector.v1_7_10.net.minecraft.inventory.Slot;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * 1.7.10 container screen. Slot rendering, item dragging and clicking are performed by the hosting modern
 * {@code AbstractContainerScreen}; the legacy class contributes its background, foreground and buttons.
 */
public abstract class GuiContainer extends GuiScreen {
    protected static final ResourceLocation inventoryBackground = new ResourceLocation("textures/gui/container/inventory.png");
    protected int xSize = 176;
    protected int ySize = 166;
    public Container inventorySlots;
    protected int guiLeft;
    protected int guiTop;

    public GuiContainer(Container container) {
        this.inventorySlots = container;
        this.allowUserInput = true;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    }

    protected abstract void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY);

    protected boolean isPointInRegion(int x, int y, int w, int h, int mouseX, int mouseY) {
        mouseX -= guiLeft;
        mouseY -= guiTop;
        return mouseX >= x - 1 && mouseX < x + w + 1 && mouseY >= y - 1 && mouseY < y + h + 1;
    }

    protected Slot getSlotAtPosition(int x, int y) {
        for (Object o : inventorySlots.inventorySlots) {
            Slot s = (Slot) o;
            if (isPointInRegion(s.xDisplayPosition, s.yDisplayPosition, 16, 16, x, y)) return s;
        }
        return null;
    }

    protected boolean checkHotbarKeys(int key) {
        return false;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // Bridge accessors for the hosting modern screen.
    public int mcc$xSize() {
        return xSize;
    }

    public int mcc$ySize() {
        return ySize;
    }

    public void mcc$setOrigin(int left, int top) {
        this.guiLeft = left;
        this.guiTop = top;
    }

    public void mcc$drawBackground(float partialTicks, int mouseX, int mouseY) {
        drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
    }

    public void mcc$drawForeground(int mouseX, int mouseY) {
        drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    protected void handleMouseClick(legacyconnector.v1_7_10.net.minecraft.inventory.Slot slot, int slotId, int button, int mode) {
    }

    protected void drawSlot(legacyconnector.v1_7_10.net.minecraft.inventory.Slot slot) {
    }
}
